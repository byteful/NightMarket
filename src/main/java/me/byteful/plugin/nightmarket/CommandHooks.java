package me.byteful.plugin.nightmarket;

import me.byteful.plugin.nightmarket.shop.player.PlayerShop;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import redempt.redlib.commandmanager.CommandHook;

import java.io.IOException;

public class CommandHooks {
  private final NightMarketPlugin plugin;

  public CommandHooks(NightMarketPlugin plugin) {
    this.plugin = plugin;
  }

  @CommandHook("open")
  public void onOpen(Player sender) {
    if (!plugin.getAccessScheduleManager().isShopOpen()) {
      sender.sendMessage(plugin.getMessages().get("shop_not_open"));

      return;
    }

    final PlayerShop shop = plugin.getPlayerShopManager().get(sender.getUniqueId());
    plugin.getParsedGUI().build(shop, plugin).open(sender);
  }

  @CommandHook("reload")
  public void onReload(CommandSender sender) {
    plugin.getRotateScheduleManager().getScheduler().shutdownNow();
    try {
      plugin.getDataStoreProvider().close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    plugin.reloadConfig();
    plugin.reloadMessages();
    plugin.reloadCurrencyManager();
    plugin.reloadRotateSchedules();
    plugin.reloadAccessSchedules();
    plugin.reloadParsedGUI();
    plugin.reloadShopItems();
    plugin.loadDataStore();
    sender.sendMessage(plugin.getMessages().get("reload_success"));
  }

  @CommandHook("forcerotate")
  public void onForceRotate(CommandSender sender, Player player) {
    plugin.getPlayerShopManager().get(player.getUniqueId()).rotate(plugin.getShopItemRegistry());
    sender.sendMessage(plugin.getMessages().get("force_rotated").replace("{player}", player.getName()));
  }
}