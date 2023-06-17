package me.byteful.plugin.nightmarket.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import redempt.redlib.misc.FormatUtils;

import java.util.List;
import java.util.stream.Collectors;

public class Text {
    public static String color(String str) {
        return FormatUtils.color(str);
    }

    public static List<String> color(List<String> list) {
        return list.stream().map(Text::color).collect(Collectors.toList());
    }

    public static String format(OfflinePlayer context, String string) {
        if (context != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return PlaceholderAPI.setPlaceholders(context, string);
        }

        return string;
    }
}
