package me.byteful.plugin.nightmarket;

import me.byteful.plugin.nightmarket.shop.player.PlayerShop;
import org.bukkit.Bukkit;
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
        if (!sender.hasPermission("nightmarket.use")) {
            sender.sendMessage(plugin.getMessage(sender, "no_permission"));

            return;
        }

        if (!plugin.getAccessScheduleManager().isShopOpen()) {
            sender.sendMessage(plugin.getMessage(sender, "shop_not_open"));

            return;
        }
        if (!plugin.getCurrencyRegistry().isLoaded()) {
            sender.sendMessage(plugin.getMessage(sender, "shop_loading"));

            return;
        }

        final PlayerShop shop = plugin.getPlayerShopManager().get(sender.getUniqueId());
        plugin.getParsedGUI().build(shop, plugin).open(sender);
    }

    @CommandHook("reload")
    public void onReload(CommandSender sender) {
        if (!sender.hasPermission("nightmarket.admin")) {
            sender.sendMessage(plugin.getMessage(null, "no_permission"));

            return;
        }

        plugin.getParsedGUI().close();
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
        plugin.reloadUpdateChecker();
//    plugin.getPlayerShopManager().updateGlobalPurchaseCount();
        sender.sendMessage(plugin.getMessage(null, "reload_success"));
    }

    @CommandHook("forcerotate")
    public void onForceRotate(CommandSender sender, Player player) {
        if (!sender.hasPermission("nightmarket.admin")) {
            sender.sendMessage(plugin.getMessage(null, "no_permission"));

            return;
        }

        plugin.getPlayerShopManager().get(player.getUniqueId()).rotate(plugin.getShopItemRegistry());
        sender.sendMessage(plugin.getMessage(null, "force_rotated").replace("{player}", player.getName()));
    }

    @CommandHook("forceglobalrotate")
    public void onForceGlobalRotate(CommandSender sender) {
        if (!sender.hasPermission("nightmarket.admin")) {
            sender.sendMessage(plugin.getMessage(null, "no_permission"));

            return;
        }

        plugin.getPlayerShopManager().rotateShops();
        sender.sendMessage(plugin.getMessage(null, "force_rotated_all"));
    }

    @CommandHook("debug")
    public void onDebug(CommandSender sender) {
        if (!sender.hasPermission("nightmarket.admin")) {
            sender.sendMessage(plugin.getMessage(null, "no_permission"));

            return;
        }

        plugin.getUpdateChecker().check();
        sender.sendMessage("NightMarket Debug Information:");
        sender.sendMessage("- Server Version: " + Bukkit.getVersion());
        sender.sendMessage("- Server Type: " + Bukkit.getBukkitVersion());
        sender.sendMessage("- Plugin Version: " + plugin.getDescription().getVersion());
        sender.sendMessage("- Latest Version: " + plugin.getUpdateChecker().getLastCheckedVersion());
        sender.sendMessage("- DataStore Type: " + plugin.getDataStoreProvider().getClass().getSimpleName());
        sender.sendMessage("- Buyer: %%__USER__%%");
        sender.sendMessage("- Resource ID: %%__RESOURCE__%%");
        sender.sendMessage("- MC-Market?: %%__BUILTBYBIT__%%");
        sender.sendMessage("{!} Please include your configuration with this when asking for help. You MAY OMIT credentials. Please COPY AND PASTE configuration into discord server. {!}");
    }
}
