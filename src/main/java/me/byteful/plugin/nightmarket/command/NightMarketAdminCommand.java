package me.byteful.plugin.nightmarket.command;

import java.io.IOException;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Dependency;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command({"nightmarketadmin", "adminnightmarket", "nmarketadmin", "nightmarket-admin"})
@CommandPermission("nightmarket.admin")
public class NightMarketAdminCommand {
    @Dependency
    private NightMarketPlugin plugin;

    @Subcommand("reload")
    @Description("Reloads the NightMarket configuration.")
    public void reload(CommandSender sender) {
        this.plugin.getParsedGUI().close(this.plugin.getTextProvider());
        this.plugin.getRotateScheduleManager().getScheduler().shutdownNow();
        try {
            this.plugin.getDataStoreProvider().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.plugin.reloadConfig();
        this.plugin.reloadMessages();
        this.plugin.reloadCurrencyManager();
        this.plugin.reloadRotateSchedules();
        this.plugin.reloadAccessSchedules();
        this.plugin.reloadParsedGUI();
        this.plugin.reloadShopItems();
        this.plugin.loadDataStore();
        this.plugin.reloadGlobalPurchaseTask();
        this.plugin.reloadUpdateChecker();
        this.plugin.sendMessage(sender, null, "reload_success");
    }

    @Subcommand("forcerotate")
    @Description("Forcefully rotate provided player's NightMarket shop.")
    public void forceRotate(CommandSender sender, Player player) {
        this.plugin.getPlayerShopManager().get(player.getUniqueId()).rotate(this.plugin, this.plugin.getShopItemRegistry());
        this.plugin.sendMessage(sender, null, "force_rotated", "{player}", player.getName());
    }

    @Subcommand("forceglobalrotate")
    @Description("Forcefully rotates all shops.")
    public void forceGlobalRotate(CommandSender sender) {
        this.plugin.getPlayerShopManager().rotateShops();
        this.plugin.sendMessage(sender, null, "force_rotated_all");
    }

    @Subcommand("debug")
    @Description("Provides debug information to give to support.")
    public void debug(CommandSender sender) {
        this.plugin.getUpdateChecker().check();
        sender.sendMessage("NightMarket Debug Information:");
        sender.sendMessage("- Server Version: " + Bukkit.getVersion());
        sender.sendMessage("- Server Type: " + Bukkit.getBukkitVersion());
        sender.sendMessage("- Plugin Version: " + this.plugin.getDescription().getVersion());
        sender.sendMessage("- Latest Version: " + this.plugin.getUpdateChecker().getLastCheckedVersion());
        sender.sendMessage("- DataStore Type: " + this.plugin.getDataStoreProvider().getClass().getSimpleName());
        sender.sendMessage("- Buyer: %%__USER__%%");
        sender.sendMessage("- Resource ID: %%__RESOURCE__%%");
        sender.sendMessage("- MC-Market?: %%__BUILTBYBIT__%%");
        sender.sendMessage(
            "{!} Please include your configuration with this when asking for help. You MAY OMIT credentials. Please COPY AND PASTE configuration into discord server. {!}");
    }
}
