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
  }

  public void rotateShops() {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      for (PlayerShop shop : plugin.getDataStoreProvider().getAllShops()) {
        shop.rotate(plugin.getShopItemRegistry());
        plugin.getDataStoreProvider().setPlayerShop(shop);
      }
    });
  }

  public PlayerShop get(UUID uuid) {
    return loadedShops.getOrDefault(uuid, new PlayerShop(plugin.getShopItemRegistry(), uuid));
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    final UUID uuid = event.getPlayer().getUniqueId();
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadedShops.put(uuid, plugin.getDataStoreProvider().getPlayerShop(uuid).orElse(new PlayerShop(plugin.getShopItemRegistry(), uuid))));
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