package me.byteful.plugin.nightmarket.parser;

import me.byteful.plugin.nightmarket.currency.Currency;
import me.byteful.plugin.nightmarket.currency.CurrencyRegistry;
import me.byteful.plugin.nightmarket.shop.item.ShopItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class ShopItemParser {
  public static ShopItem parse(CurrencyRegistry registry, ConfigurationSection config) {
    final ItemStack icon = IconParser.parse(null, config.getConfigurationSection("icon"));
    final String command = config.getString("command");
    final boolean multiplePurchase = config.getBoolean("multiple_purchase");
    final double amount = config.getDouble("price.amount");
    final Currency currency = registry.get(config.getString("price.currency"));

    if(config.getName().contains(",")) {
      throw new RuntimeException("Item config '" + config.getName() + "' CANNOT contain ',' characters.");
    }

    return new ShopItem(config.getName(), icon, command, currency, amount, multiplePurchase);
  }
}