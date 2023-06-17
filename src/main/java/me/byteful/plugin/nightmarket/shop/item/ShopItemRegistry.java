package me.byteful.plugin.nightmarket.shop.item;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.parser.ShopItemParser;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ShopItemRegistry {
    private final Map<String, ShopItem> items = new HashMap<>();
    private final int maxItems;

    public ShopItemRegistry(NightMarketPlugin plugin) {
        this.maxItems = plugin.getParsedGUI().getItemSlots().size();
        plugin.getConfig().getConfigurationSection("items").getValues(false).forEach((id, data) -> {
            register(ShopItemParser.parse(plugin.getCurrencyRegistry(), (ConfigurationSection) data));
            plugin.getLogger().info("Registered item: " + id);
        });
    }

    public ShopItem get(String id) {
        return items.get(id);
    }

    public Collection<ShopItem> getAll() {
        return items.values();
    }

    public void register(ShopItem item) {
        items.put(item.getId(), item);
    }

    public int getMaxItems() {
        return maxItems;
    }
}
