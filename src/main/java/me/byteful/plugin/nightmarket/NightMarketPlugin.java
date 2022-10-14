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
import me.byteful.plugin.nightmarket.util.dependency.IsolatedClassLoader;
import me.byteful.plugin.nightmarket.util.dependency.LibraryLoader;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.commandmanager.CommandParser;
import redempt.redlib.commandmanager.Messages;

import java.io.IOException;

public final class NightMarketPlugin extends JavaPlugin {
  private CurrencyRegistry currencyRegistry;
  private DataStoreProvider dataStoreProvider;
  private PlayerShopManager playerShopManager;
  private RotateScheduleManager rotateScheduleManager;
  private AccessScheduleManager accessScheduleManager;
  private ShopItemRegistry shopItemRegistry;
  private GUIParser.ParsedGUI parsedGUI;
  private Messages messages;

  @Override
  public void onEnable() {
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

    new CommandParser(getResource("commands.rdcml")).parse().register(this, "nightmarket", new CommandHooks(this));
  }

  void loadDataStore() {
    switch (getConfig().getString("datastore.type").toLowerCase().trim().replace(" ", "_")) {
      case "mysql":
      case "mariadb": {
        final IsolatedClassLoader loader = LibraryLoader.load(this, "mysql", "mysql-connector-java", "8.0.30");
        dataStoreProvider = new MySQLDataStoreProvider(loader, this);
        getLogger().info("Detected data store type: MySQL-remote");

        break;
      }

      case "mongo":
      case "mongodb": {
        LibraryLoader.load(this, "org.mongodb", "mongo-java-driver", "3.12.11");
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
        final IsolatedClassLoader loader = LibraryLoader.load(this, "org.xerial", "sqlite-jdbc", "3.39.3.0");
        dataStoreProvider = new SQLiteDataStoreProvider(loader, this);
        getLogger().info("Detected data store type: SQLite-file");

        break;
      }
    }

    getLogger().info("Loaded data store...");
  }

  @Override
  public void onDisable() {
    getRotateScheduleManager().getScheduler().shutdownNow();
    try {
      getDataStoreProvider().close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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

  public Messages getMessages() {
    return messages;
  }

  void reloadMessages() {
    messages = Messages.load(this);
  }

  void reloadParsedGUI() {
    parsedGUI = GUIParser.parse(getConfig().getConfigurationSection("gui"));
  }

  void reloadRotateSchedules() {
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
}
