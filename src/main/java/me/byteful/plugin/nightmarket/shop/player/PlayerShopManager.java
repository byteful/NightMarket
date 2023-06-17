package me.byteful.plugin.nightmarket.shop.player;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerShopManager implements Listener {
    private final Map<UUID, PlayerShop> loadedShops = new HashMap<>();
    private final NightMarketPlugin plugin;

    public PlayerShopManager(NightMarketPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void rotateShops() {
        plugin.debug("Rotating all shops.");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (PlayerShop shop : plugin.getDataStoreProvider().getAllShops()) {
                shop.rotate(plugin.getShopItemRegistry());
                plugin.getDataStoreProvider().setPlayerShop(shop);
            }
        });
    }

    public PlayerShop get(UUID uuid) {
        if (loadedShops.containsKey(uuid)) return loadedShops.get(uuid);

        final PlayerShop created = new PlayerShop(plugin.getShopItemRegistry(), uuid);
        loadedShops.put(uuid, created);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDataStoreProvider().setPlayerShop(created));

        return created;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> load(uuid));
    }

    public void load(UUID uuid) {
        plugin.debug("Loaded data: " + uuid);
        loadedShops.put(uuid, plugin.getDataStoreProvider().getPlayerShop(uuid).orElseGet(() -> new PlayerShop(plugin.getShopItemRegistry(), uuid)));
        plugin.debug("Loaded: " + loadedShops.size());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerShop shop = loadedShops.remove(uuid);
            if (shop == null) {
                shop = new PlayerShop(plugin.getShopItemRegistry(), uuid);
            }
            plugin.getDataStoreProvider().setPlayerShop(shop);
        });
    }
}
