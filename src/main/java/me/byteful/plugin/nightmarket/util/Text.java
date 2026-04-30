package me.byteful.plugin.nightmarket.util;

import java.text.DecimalFormat;
import java.util.Locale;
import me.clip.placeholderapi.PlaceholderAPI;
import me.byteful.plugin.nightmarket.shop.item.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

public class Text {
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#.##");

    public static String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    public static String formatPrice(ConfigurationSection config, ShopItem item) {
        String formatted = item.currency().format(item.amount());
        if (config != null && config.getBoolean("other.lowercase_currency_names")) {
            formatted = formatted.toLowerCase(Locale.ROOT);
        }
        return formatted;
    }

    public static String applyPAPIAndReplace(OfflinePlayer player, String text, String... replacements) {
        return replacements(applyPAPI(player, text), replacements);
    }

    public static String applyPAPI(OfflinePlayer context, String string) {
        if (context != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return PlaceholderAPI.setPlaceholders(context, string);
        }
        return string;
    }

    public static String replacements(String str, String... replacements) {
        if (replacements.length % 2 != 0) {
            return str;
        }

        for (int i = 0; i < replacements.length; i += 2) {
            str = str.replace(replacements[i], replacements[i + 1]);
        }
        return str;
    }

}
