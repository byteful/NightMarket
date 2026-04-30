package me.byteful.plugin.nightmarket.shop.player;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
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
import org.bukkit.entity.Player;

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
            final Set<PlayerShop> shops = this.plugin.getDataStoreProvider().getAllShops();
            this.plugin.getScheduler().runGlobal(() -> {
                for (PlayerShop shop : shops) {
                    final Player onlinePlayer = Bukkit.getPlayer(shop.getUniqueId());
                    if (onlinePlayer == null) {
                        this.plugin.getScheduler().runAsync(() -> {
                            shop.markPendingRotation(this.plugin);
                            this.plugin.getDataStoreProvider().setPlayerShop(shop);
                        });
                        continue;
                    }

                    this.plugin.getScheduler().runForPlayer(onlinePlayer, () -> {
                        shop.rotate(this.plugin, this.plugin.getShopItemRegistry(), onlinePlayer);
                        this.loadedShops.put(shop.getUniqueId(), shop);
                        this.plugin.getScheduler().runAsync(() -> this.plugin.getDataStoreProvider().setPlayerShop(shop));
                    });
                }
                this.resetGlobalPurchaseCounts();
            });
        });
    }

    public void updateGlobalPurchaseCount() {
        if (!this.plugin.getConfig().getBoolean("other.global_purchase_limits")) {
            return;
        }

        this.plugin.getScheduler().runAsync(() -> {
            final Set<PlayerShop> shops = this.plugin.getDataStoreProvider().getAllShops();

            this.plugin.getScheduler().runGlobal(() -> {
                final Map<String, Integer> refreshedCounts = new ConcurrentHashMap<>();
                final Set<UUID> countedPlayers = new HashSet<>();
                for (PlayerShop storedShop : shops) {
                    final PlayerShop loadedShop = this.loadedShops.get(storedShop.getUniqueId());
                    this.addGlobalPurchaseCounts(refreshedCounts, loadedShop == null ? storedShop : loadedShop);
                    countedPlayers.add(storedShop.getUniqueId());
                }
                for (Map.Entry<UUID, PlayerShop> loadedEntry : this.loadedShops.entrySet()) {
                    if (countedPlayers.add(loadedEntry.getKey())) {
                        this.addGlobalPurchaseCounts(refreshedCounts, loadedEntry.getValue());
                    }
                }

                this.globalPurchaseCount.clear();
                for (ShopItem item : this.plugin.getShopItemRegistry().getAll()) {
                    final String itemId = item.id();
                    this.globalPurchaseCount.put(itemId, refreshedCounts.getOrDefault(itemId, 0));
                }
            });
        });
    }

    private void addGlobalPurchaseCounts(Map<String, Integer> counts, PlayerShop shop) {
        if (shop.isPendingRotation()) {
            return;
        }

        for (ShopItem item : this.plugin.getShopItemRegistry().getAll()) {
            counts.merge(item.id(), shop.getPurchaseCount(item.id()), Integer::sum);
        }
    }

    public PlayerShop get(UUID uuid) {
        return this.loadedShops.computeIfAbsent(uuid, key -> {
            final PlayerShop created = this.plugin.getDataStoreProvider()
                .getPlayerShop(key)
                .orElseGet(() -> new PlayerShop(this.plugin, this.plugin.getShopItemRegistry(), key, null));
            this.plugin.getScheduler().runAsync(() -> this.plugin.getDataStoreProvider().setPlayerShop(created));
            return created;
        });
    }

    public PlayerShop get(Player player) {
        final UUID uuid = player.getUniqueId();
        final boolean[] createdShop = new boolean[]{false};
        final PlayerShop shop = this.loadedShops.computeIfAbsent(uuid, key -> {
            final Optional<PlayerShop> stored = this.plugin.getDataStoreProvider().getPlayerShop(key);
            createdShop[0] = stored.isEmpty();
            return stored.orElseGet(() -> new PlayerShop(this.plugin, this.plugin.getShopItemRegistry(), key, player));
        });
        final boolean wasPending = shop.isPendingRotation();
        this.refreshPendingShop(shop, player);
        if (createdShop[0] || wasPending) {
            this.plugin.getScheduler().runAsync(() -> this.plugin.getDataStoreProvider().setPlayerShop(shop));
        }
        return shop;
    }

    public int getGlobalPurchaseCount(ShopItem item) {
        return this.globalPurchaseCount.getOrDefault(item.id(), 0);
    }

    public Map<String, Integer> getGlobalPurchaseCounts() {
        return this.globalPurchaseCount;
    }

    public PlayerShop getLoaded(UUID uuid) {
        return this.loadedShops.get(uuid);
    }

    public void resetGlobalPurchaseCounts() {
        this.globalPurchaseCount.clear();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        this.load(player);
    }

    public void load(UUID uuid) {
        this.plugin.debug("Loaded data: " + uuid);
        this.loadedShops.put(uuid,
            this.plugin.getDataStoreProvider().getPlayerShop(uuid).orElseGet(() -> new PlayerShop(this.plugin, this.plugin.getShopItemRegistry(), uuid)));
        this.plugin.debug("Loaded: " + this.loadedShops.size());
    }

    public void load(Player player) {
        final UUID uuid = player.getUniqueId();
        this.plugin.debug("Loaded data: " + uuid);
        this.plugin.getScheduler().runAsync(() -> {
            final Optional<PlayerShop> loaded = this.plugin.getDataStoreProvider().getPlayerShop(uuid);
            this.plugin.getScheduler().runForPlayer(player, () -> {
                final PlayerShop shop = loaded.orElseGet(() -> new PlayerShop(this.plugin, this.plugin.getShopItemRegistry(), uuid, player));
                final boolean shouldPersist = loaded.isEmpty() || shop.isPendingRotation();
                this.refreshPendingShop(shop, player);
                this.loadedShops.put(uuid, shop);
                if (shouldPersist) {
                    this.plugin.getScheduler().runAsync(() -> this.plugin.getDataStoreProvider().setPlayerShop(shop));
                }
                this.plugin.debug("Loaded: " + this.loadedShops.size());
            });
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        this.plugin.getScheduler().runAsync(() -> {
            PlayerShop shop = this.loadedShops.remove(uuid);
            if (shop == null) {
                shop = new PlayerShop(this.plugin, this.plugin.getShopItemRegistry(), uuid, null);
            }
            this.plugin.getDataStoreProvider().setPlayerShop(shop);
        });
    }

    private void refreshPendingShop(PlayerShop shop, Player player) {
        if (!shop.isPendingRotation()) {
            return;
        }

        shop.rotate(this.plugin, this.plugin.getShopItemRegistry(), player);
    }
}
