package me.byteful.plugin.nightmarket.shop.player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.shop.item.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerShopManager implements Listener {
    private final Map<UUID, PlayerShop> loadedShops = new ConcurrentHashMap<>();
    private final Map<String, Integer> globalPurchaseCount = new ConcurrentHashMap<>();
    private final NightMarketPlugin plugin;

    public PlayerShopManager(NightMarketPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void rotateShops() {
        this.plugin.debug("Rotating all shops.");
        this.plugin.getScheduler().runAsync(() -> {
            for (PlayerShop shop : this.plugin.getDataStoreProvider().getAllShops()) {
                shop.rotate(this.plugin, this.plugin.getShopItemRegistry());
                this.plugin.getDataStoreProvider().setPlayerShop(shop);
                if (Bukkit.getPlayer(shop.getUniqueId()) != null) {
                    this.loadedShops.put(shop.getUniqueId(), shop);
                }
            }
            this.updateGlobalPurchaseCount();
        });
    }

    public void updateGlobalPurchaseCount() {
        if (!this.plugin.getConfig().getBoolean("other.global_purchase_limits")) {
            return;
        }

        this.plugin.getScheduler().runAsync(() -> {
            final Map<String, Integer> copy = new ConcurrentHashMap<>();
            final Set<PlayerShop> shops = this.plugin.getDataStoreProvider().getAllShops();
            for (ShopItem item : this.plugin.getShopItemRegistry().getAll()) {
                final int totalPurchases = shops.stream().mapToInt(shop -> shop.getPurchaseCount(item.id())).sum();
                copy.put(item.id(), totalPurchases);
            }

            this.plugin.getScheduler().runGlobal(() -> {
                this.globalPurchaseCount.clear();
                this.globalPurchaseCount.putAll(copy);
            });
        });
    }

    public PlayerShop get(UUID uuid) {
        return this.loadedShops.computeIfAbsent(uuid, key -> {
            final PlayerShop created = this.plugin.getDataStoreProvider()
                .getPlayerShop(key)
                .orElseGet(() -> new PlayerShop(this.plugin, this.plugin.getShopItemRegistry(), key));
            this.plugin.getScheduler().runAsync(() -> this.plugin.getDataStoreProvider().setPlayerShop(created));
            return created;
        });
    }

    public int getGlobalPurchaseCount(ShopItem item) {
        return this.globalPurchaseCount.getOrDefault(item.id(), 0);
    }

    public Map<String, Integer> getGlobalPurchaseCounts() {
        return this.globalPurchaseCount;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        this.plugin.getScheduler().runAsync(() -> this.load(uuid));
    }

    public void load(UUID uuid) {
        this.plugin.debug("Loaded data: " + uuid);
        this.loadedShops.put(uuid,
            this.plugin.getDataStoreProvider().getPlayerShop(uuid).orElseGet(() -> new PlayerShop(this.plugin, this.plugin.getShopItemRegistry(), uuid)));
        this.plugin.debug("Loaded: " + this.loadedShops.size());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        this.plugin.getScheduler().runAsync(() -> {
            PlayerShop shop = this.loadedShops.remove(uuid);
            if (shop == null) {
                shop = new PlayerShop(this.plugin, this.plugin.getShopItemRegistry(), uuid);
            }
            this.plugin.getDataStoreProvider().setPlayerShop(shop);
        });
    }
}
