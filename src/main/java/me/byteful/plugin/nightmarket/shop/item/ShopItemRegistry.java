package me.byteful.plugin.nightmarket.shop.item;

import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.parser.ShopItemParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class ShopItemRegistry {
    private final Map<String, ShopItem> items = new HashMap<>();
    private final NightMarketPlugin plugin;

    public ShopItemRegistry(NightMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        final int[] total = new int[]{0};
        this.plugin.getConfig().getConfigurationSection("items").getValues(false).forEach((id, data) -> {
            ShopItem parsed;
            try {
                parsed = ShopItemParser.parse(this.plugin.getLogger(), this.plugin.getCurrencyRegistry(), (ConfigurationSection) data);
            } catch (Exception e) {
                this.plugin.getLogger().warning("Skipped loading ShopItem: " + id + " (" + e.getMessage() + ")");
                return;
            }

            this.register(parsed);
            total[0]++;
        });
        this.plugin.getLogger().info("Registered " + total[0] + " items...");
    }

    public void register(ShopItem item) {
        this.items.put(item.id(), item);
    }

    public ShopItem get(String id) {
        return this.items.get(id);
    }

    public Collection<ShopItem> getAll() {
        return this.items.values();
    }

    public List<ShopItem> getEligible(Player player) {
        return this.items.values().stream()
            .filter(item -> this.isEligible(player, item))
            .collect(Collectors.toList());
    }

    public List<ShopItem> getUngated() {
        return this.items.values().stream()
            .filter(item -> !item.hasPermissionRequirement())
            .collect(Collectors.toList());
    }

    public boolean isEligible(Player player, ShopItem item) {
        return !item.hasPermissionRequirement() || player != null && player.hasPermission(item.permission());
    }

    public int getMaxItems() {
        return this.plugin.getParsedGUI().getItemSlots().size();
    }
}
