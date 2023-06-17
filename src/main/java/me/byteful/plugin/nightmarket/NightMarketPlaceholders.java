package me.byteful.plugin.nightmarket;

import me.byteful.plugin.nightmarket.shop.player.PlayerShop;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class NightMarketPlaceholders extends PlaceholderExpansion {
    private final NightMarketPlugin plugin;

    public NightMarketPlaceholders(NightMarketPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "nightmarket";
    }

    @Override
    public String getAuthor() {
        return "byteful";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return null;
        }

        final PlayerShop shop = plugin.getPlayerShopManager().get(player.getUniqueId());
        if (shop == null) {
            return null;
        }

        switch (params.toLowerCase().trim().replace(" ", "_")) {
            case "purchased_items_count": {
                return "" + shop.getPurchasedShopItems().size();
            }

            case "available_items_count": {
                return "" + shop.getShopItems().size();
            }

            case "rotate": {
                return "" + plugin.getRotateScheduleManager().getNextTime().format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a", Locale.US));
            }

            case "open": {
                return "" + plugin.getAccessScheduleManager().getNextTime().format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a", Locale.US));
            }

            default: {
                return null;
            }
        }
    }
}
