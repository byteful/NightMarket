package me.byteful.plugin.nightmarket.shop.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.shop.item.ShopItem;
import me.byteful.plugin.nightmarket.shop.item.ShopItemRegistry;
import me.byteful.plugin.nightmarket.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import redempt.redlib.misc.WeightedRandom;

public class PlayerShop {
    private final UUID uniqueId;
    private final Map<String, Integer> purchasedShopItems;
    private List<String> shopItems;

    public PlayerShop(NightMarketPlugin plugin, ShopItemRegistry itemRegistry, UUID uniqueId) {
        this.uniqueId = uniqueId;
        this.shopItems = this.generateRandomShop(plugin, itemRegistry);
        this.purchasedShopItems = new HashMap<>();
    }

    private List<String> generateRandomShop(NightMarketPlugin plugin, ShopItemRegistry registry) {
        plugin.debug("Creating a new, random shop for: " + this.uniqueId);
        final int maxItems = registry.getMaxItems();
        if (registry.getAll().size() < maxItems) {
            throw new RuntimeException("There are not enough items to generate shops! You need more items than slots in the GUI!");
        }

        WeightedRandom<ShopItem> random = WeightedRandom.fromCollection(registry.getAll(), x -> x, ShopItem::rarity);

        if (this.shopItems != null && plugin.getConfig().getBoolean("other.prevent_repeat_items", false)) {
            for (String currentItemId : this.shopItems) {
                ShopItem currentItem = registry.get(currentItemId);
                if (currentItem != null) {
                    random.remove(currentItem);
                }
            }
            if (random.getWeights().size() < maxItems) {
                plugin.debug("Not enough items after excluding previous items, allowing repeats.");
                random = WeightedRandom.fromCollection(registry.getAll(), x -> x, ShopItem::rarity);
            }
        }

        final List<String> generated = new ArrayList<>();
        for (int i = 0; i < maxItems; i++) {
            final ShopItem item = random.roll();
            random.remove(item);
            generated.add(item.id());
        }

        plugin.debug("Created: " + String.join(",", generated));
        return generated;
    }

    public PlayerShop(UUID uniqueId, List<String> purchasedShopItems, List<String> shopItems) {
        this.uniqueId = uniqueId;
        this.purchasedShopItems = deserializePurchased(purchasedShopItems);
        this.shopItems = shopItems;
    }

    private static Map<String, Integer> deserializePurchased(List<String> list) {
        final Map<String, Integer> map = new HashMap<>();
        for (String data : list) {
            if (!data.contains(":")) {
                map.put(data, 1);

                continue;
            }

            final String[] split = data.split(":");
            map.put(split[0], Integer.parseInt(split[1]));
        }

        return map;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public List<String> getShopItems() {
        return this.shopItems;
    }

    public void setShopItems(List<String> shopItems) {
        this.shopItems = shopItems;
    }

    public Map<String, Integer> getPurchasedShopItems() {
        return this.purchasedShopItems;
    }

    public List<String> getSerializedPurchasedShopItems() {
        final List<String> list = new ArrayList<>();
        this.purchasedShopItems.forEach((id, amt) -> list.add(id + ":" + amt));

        return list;
    }

    public int getPurchaseCount(String item) {
        return this.purchasedShopItems.getOrDefault(item, 0);
    }

    public void purchaseItem(NightMarketPlugin plugin, ShopItem item) {
        this.purchasedShopItems.merge(item.id(), 1, Integer::sum);
        plugin.getPlayerShopManager().getGlobalPurchaseCounts().merge(item.id(), 1, Integer::sum);
        final OfflinePlayer player = Bukkit.getOfflinePlayer(this.uniqueId);

        for (String cmd : item.commands()) {
            cmd = cmd.startsWith("/") ? cmd.substring(1) : cmd;
            cmd = cmd.replace("{player}", Objects.requireNonNull(player.getName()));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Text.applyPAPI(player, cmd));
        }

        item.currency().withdraw(this.uniqueId, item.amount());
        plugin.debug("Player purchased item: " + item.id() + " (" + this.uniqueId + ")");
    }

    public void rotate(NightMarketPlugin plugin, ShopItemRegistry registry) {
        plugin.debug("Rotating player shop: " + this.uniqueId);
        plugin.debug("Old shop: " + String.join(",", this.shopItems));
        this.shopItems = this.generateRandomShop(plugin, registry);
        plugin.debug("New shop: " + String.join(",", this.shopItems));
        this.purchasedShopItems.clear();
    }
}
