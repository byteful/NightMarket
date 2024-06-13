package me.byteful.plugin.nightmarket;

import me.byteful.plugin.nightmarket.currency.CurrencyRegistry;
import me.byteful.plugin.nightmarket.datastore.DataStoreProvider;
import me.byteful.plugin.nightmarket.datastore.impl.JSONDataStoreProvider;
import me.byteful.plugin.nightmarket.datastore.impl.MongoDataStoreProvider;
import me.byteful.plugin.nightmarket.datastore.impl.MySQLDataStoreProvider;
import me.byteful.plugin.nightmarket.datastore.impl.SQLiteDataStoreProvider;
import me.byteful.plugin.nightmarket.parser.GUIParser;
import me.byteful.plugin.nightmarket.schedule.access.AccessScheduleManager;
import me.byteful.plugin.nightmarket.schedule.rotate.RotateScheduleManager;
import me.byteful.plugin.nightmarket.shop.item.ShopItemRegistry;
import me.byteful.plugin.nightmarket.shop.player.PlayerShopManager;
import me.byteful.plugin.nightmarket.util.UpdateChecker;
import me.byteful.plugin.nightmarket.util.dependency.IsolatedClassLoader;
import me.byteful.plugin.nightmarket.util.dependency.LibraryLoader;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import redempt.redlib.commandmanager.CommandParser;
import redempt.redlib.commandmanager.Messages;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

public final class NightMarketPlugin extends JavaPlugin {
  private static NightMarketPlugin instance;

  private final UpdateChecker updateChecker = new UpdateChecker(this);

  private CurrencyRegistry currencyRegistry;
  private DataStoreProvider dataStoreProvider;
  private PlayerShopManager playerShopManager;
  private RotateScheduleManager rotateScheduleManager;
  private AccessScheduleManager accessScheduleManager;
  private ShopItemRegistry shopItemRegistry;
  private GUIParser.ParsedGUI parsedGUI;
  private Messages messages;
  private BukkitTask updateCheckingTask;
  private ZoneId timezone;

  public static NightMarketPlugin getInstance() {
    return instance;
  }

  @Override
  public void onEnable() {
    instance = this;

    saveDefaultConfig();
    getLogger().info("Loaded config...");
    reloadMessages();
    getLogger().info("Loaded messages...");
    reloadCurrencyManager();
    getLogger().info("Loaded currencies...");
    playerShopManager = new PlayerShopManager(this);
    getLogger().info("Loaded player shops...");
    reloadRotateSchedules();
    getLogger().info("Loaded rotate schedules...");
    reloadAccessSchedules();
    getLogger().info("Loaded access schedules...");
    reloadParsedGUI();
    getLogger().info("Loaded GUI...");
    reloadShopItems();
    getLogger().info("Loaded shop items...");
    loadDataStore();
    reloadUpdateChecker();

    new CommandParser(getResource("commands.rdcml")).parse().register(this, "nightmarket", new CommandHooks(this));
    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      new NightMarketPlaceholders(this).register();
    }
  }

  void reloadUpdateChecker() {
    if (updateCheckingTask != null) {
      updateCheckingTask.cancel();
      updateCheckingTask = null;
    }

    if (getConfig().getBoolean("other.update")) {
      updateCheckingTask = Bukkit.getScheduler().runTaskTimer(this, updateChecker::check, 0L, 20L * TimeUnit.DAYS.toSeconds(1));
    }
  }

  void loadDataStore() {
    switch (getConfig().getString("datastore.type").toLowerCase().trim().replace(" ", "_")) {
      case "mysql":
      case "mariadb": {
        LibraryLoader.loadWithInject(this, "com.mysql", "mysql-connector-j", "8.4.0");
        dataStoreProvider = new MySQLDataStoreProvider(this);
        getLogger().info("Detected data store type: MySQL-remote");

        break;
      }

      case "mongo":
      case "mongodb": {
        LibraryLoader.loadWithInject(this, "org.mongodb", "mongo-java-driver", "3.12.14");
        dataStoreProvider = new MongoDataStoreProvider(this);
        getLogger().info("Detected data store type: MongoDB-remote");

        break;
      }

      case "json":
      case "file": {
        dataStoreProvider = new JSONDataStoreProvider(this);
        getLogger().info("Detected data store type: JSON-file");

        break;
      }

      case "sqlite":
      case "flatfile": {
        final IsolatedClassLoader loader = LibraryLoader.load(this, "org.xerial", "sqlite-jdbc", "3.42.0.0"); // Use older version of SQLite for compatibility
        dataStoreProvider = new SQLiteDataStoreProvider(loader, this);
        getLogger().info("Detected data store type: SQLite-file");

        break;
      }
    }

    if (!dataStoreProvider.test()) {
      getLogger().info("Failed DataStore testing... Plugin shutting down.");
      dataStoreProvider = null;
      Bukkit.getPluginManager().disablePlugin(this);

      return;
    }

    LibraryLoader.clearUnusedJars(this);
    getLogger().info("Loaded data store...");
  }

  @Override
  public void onDisable() {
    if (rotateScheduleManager != null) {
      getRotateScheduleManager().getScheduler().shutdownNow();
    }
    if (dataStoreProvider != null) {
      try {
        getDataStoreProvider().close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    instance = null;
  }

  public RotateScheduleManager getRotateScheduleManager() {
    return rotateScheduleManager;
  }

  public AccessScheduleManager getAccessScheduleManager() {
    return accessScheduleManager;
  }

  public DataStoreProvider getDataStoreProvider() {
    return dataStoreProvider;
  }

  public CurrencyRegistry getCurrencyRegistry() {
    return currencyRegistry;
  }

  public PlayerShopManager getPlayerShopManager() {
    return playerShopManager;
  }

  public ShopItemRegistry getShopItemRegistry() {
    return shopItemRegistry;
  }

  public GUIParser.ParsedGUI getParsedGUI() {
    return parsedGUI;
  }

  public String getMessage(Player context, String key) {
    return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") && context != null ? PlaceholderAPI.setPlaceholders(context, messages.get(key)) : messages.get(key);
  }

  void reloadMessages() {
    messages = Messages.load(this);
  }

  void reloadParsedGUI() {
    parsedGUI = GUIParser.parse(getConfig().getConfigurationSection("gui"));
  }

  void reloadRotateSchedules() {
    final String read = getConfig().getString("timezone");
    if (read == null || read.isEmpty()) {
      timezone = ZoneOffset.systemDefault();
    } else {
      timezone = ZoneOffset.of(read, ZoneOffset.SHORT_IDS);
    }

    rotateScheduleManager = new RotateScheduleManager(this);
  }

  void reloadAccessSchedules() {
    accessScheduleManager = new AccessScheduleManager(this);
  }

  void reloadShopItems() {
    shopItemRegistry = new ShopItemRegistry(this);
  }

  void reloadCurrencyManager() {
    currencyRegistry = new CurrencyRegistry(this);
  }

  public UpdateChecker getUpdateChecker() {
    return updateChecker;
  }

  public void debug(String message) {
    if (getDescription().getVersion().contains("BETA")) {
      getLogger().info("[DEBUG] " + message);
    }
  }

  public ZoneId getTimezone() {
    return timezone;
  }
}
