package me.byteful.plugin.nightmarket.command;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.shop.player.PlayerShop;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Dependency;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command({"nightmarket", "nmarket", "nightm"})
@CommandPermission("nightmarket.use")
public class NightMarketCommand {
    @Dependency
    private NightMarketPlugin plugin;

    @DefaultFor({"nightmarket", "nmarket", "nightm"})
    @Description("Opens the NightMarket GUI.")
    public void open(Player sender) {
        if (!this.plugin.getAccessScheduleManager().isShopOpen()) {
            this.plugin.sendMessage(sender, sender, "shop_not_open");
            return;
        }

        if (!this.plugin.getCurrencyRegistry().isLoaded()) {
            this.plugin.sendMessage(sender, sender, "shop_loading");
            return;
        }

        final PlayerShop shop = this.plugin.getPlayerShopManager().get(sender);
        this.plugin.getParsedGUI().build(shop, this.plugin).open(sender);
    }
}
