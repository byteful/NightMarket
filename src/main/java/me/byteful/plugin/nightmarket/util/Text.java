package me.byteful.plugin.nightmarket.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import redempt.redlib.misc.FormatUtils;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

public class Text {
  private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#.##");

  public static String formatCurrency(double amount) {
    return CURRENCY_FORMAT.format(amount);
  }

  public static String color(String str) {
    return FormatUtils.color(str, true);
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
