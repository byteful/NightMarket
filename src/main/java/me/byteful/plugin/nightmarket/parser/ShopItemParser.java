package me.byteful.plugin.nightmarket.parser;

import me.byteful.plugin.nightmarket.currency.Currency;
import me.byteful.plugin.nightmarket.currency.CurrencyRegistry;
import me.byteful.plugin.nightmarket.shop.item.ShopItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class ShopItemParser {
    public static ShopItem parse(CurrencyRegistry registry, ConfigurationSection config) {
        final ItemStack icon = IconParser.parse(config.getConfigurationSection("icon"));
        final String command = config.getString("command");
        final boolean multiplePurchase = config.getBoolean("multiple_purchase");
        final double amount = config.getDouble("price.amount");
        final double rarity = config.getDouble("rarity");
        final Currency currency = registry.get(config.getString("price.currency"));

        if (config.getName().contains(",")) {
            throw new RuntimeException("Item config '" + config.getName() + "' id/name CANNOT contain ',' characters.");
        }

        if (currency == null || !currency.canLoad()) {
            throw new RuntimeException("Failed to find a valid currency adapter for: " + config.getString("price.currency"));
        }

        return new ShopItem(config.getName(), icon, command, currency, amount, rarity, multiplePurchase);
    }
}
