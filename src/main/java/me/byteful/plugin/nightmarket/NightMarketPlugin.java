package me.byteful.plugin.nightmarket;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import me.byteful.plugin.nightmarket.command.NightMarketAdminCommand;
import me.byteful.plugin.nightmarket.command.NightMarketCommand;
import me.byteful.plugin.nightmarket.currency.CurrencyRegistry;
import me.byteful.plugin.nightmarket.datastore.DataStoreProvider;
import me.byteful.plugin.nightmarket.datastore.impl.JSONDataStoreProvider;
import me.byteful.plugin.nightmarket.datastore.impl.MongoDataStoreProvider;
import me.byteful.plugin.nightmarket.datastore.impl.MySQLDataStoreProvider;
import me.byteful.plugin.nightmarket.datastore.impl.SQLiteDataStoreProvider;
import me.byteful.plugin.nightmarket.parser.GUIParser;
import me.byteful.plugin.nightmarket.schedule.access.AccessScheduleManager;
import me.byteful.plugin.nightmarket.schedule.rotate.RotateScheduleManager;
import me.byteful.plugin.nightmarket.scheduler.impl.BukkitSchedulerImpl;
import me.byteful.plugin.nightmarket.scheduler.impl.FoliaSchedulerImpl;
import me.byteful.plugin.nightmarket.scheduler.ScheduledTask;
import me.byteful.plugin.nightmarket.scheduler.Scheduler;
import me.byteful.plugin.nightmarket.shop.item.ShopItemRegistry;
import me.byteful.plugin.nightmarket.shop.player.PlayerShopManager;
import me.byteful.plugin.nightmarket.util.ConfigUpdater;
import me.byteful.plugin.nightmarket.util.MessageManager;
import me.byteful.plugin.nightmarket.util.Text;
import me.byteful.plugin.nightmarket.util.UpdateChecker;
import me.byteful.plugin.nightmarket.util.dependency.IsolatedClassLoader;
import me.byteful.plugin.nightmarket.util.dependency.LibraryLoader;
import me.byteful.plugin.nightmarket.util.text.LegacyTextProvider;
import me.byteful.plugin.nightmarket.util.text.TextProvider;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitCommandHandler;

public final class NightMarketPlugin extends JavaPlugin {
    private final UpdateChecker updateChecker = new UpdateChecker(this);

    private Scheduler scheduler;
    private TextProvider textProvider;
    private CurrencyRegistry currencyRegistry;
    private DataStoreProvider dataStoreProvider;
    private PlayerShopManager playerShopManager;
    private RotateScheduleManager rotateScheduleManager;
    private AccessScheduleManager accessScheduleManager;
    private ShopItemRegistry shopItemRegistry;
    private GUIParser.ParsedGUI parsedGUI;
    private MessageManager messageManager;
    private ScheduledTask updateCheckingTask;
    private ScheduledTask globalPurchaseTask;
    private ZoneId timezone;
    private Metrics metrics;

    @Override
    public void onDisable() {
        if (this.metrics != null) {
            this.metrics.shutdown();
        }

        if (this.updateCheckingTask != null) {
            this.updateCheckingTask.cancel();
        }
        if (this.globalPurchaseTask != null) {
            this.globalPurchaseTask.cancel();
        }
        if (this.rotateScheduleManager != null) {
            this.getRotateScheduleManager().getScheduler().shutdownNow();
        }
        if (this.dataStoreProvider != null) {
            try {
                this.getDataStoreProvider().close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onEnable() {
        this.scheduler = this.createScheduler();
        this.getLogger().info("Using scheduler: " + this.scheduler.getClass().getSimpleName());

        this.textProvider = this.detectTextProvider();
        this.getLogger().info("Using text provider: " + this.textProvider.getClass().getSimpleName());

        this.saveDefaultConfig();
        ConfigUpdater.update(this);
        this.reloadConfig();
        this.getLogger().info("Loaded config...");

        this.reloadMessages();
        this.getLogger().info("Loaded messages...");

        this.reloadCurrencyManager();
        this.getLogger().info("Loaded currencies...");

        this.playerShopManager = new PlayerShopManager(this);
        this.getLogger().info("Loaded player shops...");

        this.reloadRotateSchedules();
        this.getLogger().info("Loaded rotate schedules...");

        this.reloadAccessSchedules();
        this.getLogger().info("Loaded access schedules...");

        this.reloadParsedGUI();
        this.getLogger().info("Loaded GUI...");

        this.reloadShopItems();
        this.getLogger().info("Loaded shop items...");

        this.loadDataStore();
        this.reloadUpdateChecker();
        this.reloadGlobalPurchaseTask();

        final BukkitCommandHandler commandHandler = BukkitCommandHandler.create(this);
        commandHandler.registerDependency(NightMarketPlugin.class, this);
        commandHandler.register(new NightMarketCommand());
        commandHandler.register(new NightMarketAdminCommand());
        commandHandler.registerBrigadier();
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new NightMarketPlaceholders(this).register();
        }

        this.metrics = new Metrics(this, 22301);
        this.getLogger().info("Successfully started " + this.getDescription().getFullName() + "!");
    }

    public void reloadGlobalPurchaseTask() {
        if (this.globalPurchaseTask != null) {
            this.globalPurchaseTask.cancel();
            this.globalPurchaseTask = null;
        }
        if (this.getConfig().getBoolean("other.global_purchase_limits")) {
            this.globalPurchaseTask = this.scheduler.runGlobalTimer(() -> this.playerShopManager.updateGlobalPurchaseCount(), 1L, 20L);
        }
    }

    public void reloadUpdateChecker() {
        if (this.updateCheckingTask != null) {
            this.updateCheckingTask.cancel();
            this.updateCheckingTask = null;
        }

        if (this.getConfig().getBoolean("other.update")) {
            this.updateCheckingTask = this.scheduler.runGlobalTimer(this.updateChecker::check, 1L, 20L * TimeUnit.DAYS.toSeconds(1));
        }
    }

    public void loadDataStore() {
        switch (this.getConfig().getString("datastore.type").toLowerCase().trim().replace(" ", "_")) {
            case "mysql":
            case "mariadb": {
                LibraryLoader.loadWithInject(this, "com.mysql", "mysql-connector-j", "8.4.0");
                this.dataStoreProvider = new MySQLDataStoreProvider(this);
                this.getLogger().info("Detected data store type: MySQL-remote");

                break;
            }

            case "mongo":
            case "mongodb": {
                LibraryLoader.loadWithInject(this, "org.mongodb", "mongo-java-driver", "3.12.14");
                this.dataStoreProvider = new MongoDataStoreProvider(this);
                this.getLogger().info("Detected data store type: MongoDB-remote");

                break;
            }

            case "json":
            case "file": {
                this.dataStoreProvider = new JSONDataStoreProvider(this);
                this.getLogger().info("Detected data store type: JSON-file");

                break;
            }

            case "sqlite":
            case "flatfile": {
                final IsolatedClassLoader loader = LibraryLoader.load(this, "org.xerial", "sqlite-jdbc",
                    "3.42.0.0"); // Use older version of SQLite for compatibility
                this.dataStoreProvider = new SQLiteDataStoreProvider(loader, this);
                this.getLogger().info("Detected data store type: SQLite-file");

                break;
            }
        }

        if (!this.dataStoreProvider.test()) {
            this.getLogger().info("Failed DataStore testing... Plugin shutting down.");
            this.dataStoreProvider = null;
            Bukkit.getPluginManager().disablePlugin(this);

            return;
        }

        LibraryLoader.clearUnusedJars(this);
        this.getLogger().info("Loaded data store...");
    }

    public void reloadMessages() {
        this.messageManager = new MessageManager(this);
    }

    public void reloadParsedGUI() {
        this.parsedGUI = GUIParser.parse(Objects.requireNonNull(this.getConfig().getConfigurationSection("gui")));
    }

    public void reloadRotateSchedules() {
        final String read = this.getConfig().getString("timezone");
        if (read == null || read.isEmpty()) {
            this.timezone = ZoneOffset.systemDefault();
        } else {
            this.timezone = ZoneOffset.of(read, ZoneOffset.SHORT_IDS);
        }

        this.rotateScheduleManager = new RotateScheduleManager(this);
    }

    public void reloadAccessSchedules() {
        this.accessScheduleManager = new AccessScheduleManager(this);
    }

    public void reloadShopItems() {
        this.shopItemRegistry = new ShopItemRegistry(this);
    }

    public void reloadCurrencyManager() {
        this.currencyRegistry = new CurrencyRegistry(this);
    }

    private Scheduler createScheduler() {
        return isFolia() ? new FoliaSchedulerImpl(this) : new BukkitSchedulerImpl(this);
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private TextProvider detectTextProvider() {
        try {
            Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            CommandSender.class.getMethod("sendMessage", componentClass);
            return (TextProvider) Class.forName("me.byteful.plugin.nightmarket.util.text.PaperTextProvider")
                .getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            return new LegacyTextProvider();
        }
    }

    public RotateScheduleManager getRotateScheduleManager() {
        return this.rotateScheduleManager;
    }

    public DataStoreProvider getDataStoreProvider() {
        return this.dataStoreProvider;
    }

    public AccessScheduleManager getAccessScheduleManager() {
        return this.accessScheduleManager;
    }

    public CurrencyRegistry getCurrencyRegistry() {
        return this.currencyRegistry;
    }

    public PlayerShopManager getPlayerShopManager() {
        return this.playerShopManager;
    }

    public ShopItemRegistry getShopItemRegistry() {
        return this.shopItemRegistry;
    }

    public GUIParser.ParsedGUI getParsedGUI() {
        return this.parsedGUI;
    }

    public void sendMessage(CommandSender sender, Player papiContext, String key, String... replacements) {
        String raw = Text.applyPAPIAndReplace(papiContext, this.messageManager.get(key), replacements);
        this.textProvider.sendMessage(sender, raw);
    }

    public TextProvider getTextProvider() {
        return this.textProvider;
    }

    public MessageManager getMessageManager() {
        return this.messageManager;
    }

    public UpdateChecker getUpdateChecker() {
        return this.updateChecker;
    }

    public Scheduler getScheduler() {
        return this.scheduler;
    }

    public void debug(String message) {
        if (this.getConfig().getBoolean("other.debug", false)) {
            this.getLogger().info("[DEBUG] " + message);
        }
    }

    public ZoneId getTimezone() {
        return this.timezone;
    }
}
