package me.byteful.plugin.nightmarket.shop.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.shop.item.ShopItem;
import me.byteful.plugin.nightmarket.shop.item.ShopItemRegistry;
import org.bukkit.entity.Player;
import redempt.redlib.misc.WeightedRandom;

public class PlayerShop {
    private final UUID uniqueId;
    private final Map<String, Integer> purchasedShopItems;
    private List<String> shopItems;
    private boolean pendingRotation;

    public PlayerShop(NightMarketPlugin plugin, ShopItemRegistry itemRegistry, UUID uniqueId) {
        this(plugin, itemRegistry, uniqueId, null);
    }

    public PlayerShop(NightMarketPlugin plugin, ShopItemRegistry itemRegistry, UUID uniqueId, Player player) {
        this.uniqueId = uniqueId;
        this.purchasedShopItems = new HashMap<>();
        this.shopItems = new ArrayList<>();
        if (player == null) {
            this.pendingRotation = true;
            plugin.debug("Created pending NightMarket shop for: " + this.uniqueId);
        } else {
            this.rotate(plugin, itemRegistry, player);
        }
    }

    private List<String> generateRandomShop(NightMarketPlugin plugin, ShopItemRegistry registry, Player player) {
        plugin.debug("Creating a new, random shop for: " + this.uniqueId);
        final int maxItems = registry.getMaxItems();
        final List<ShopItem> eligibleItems = new ArrayList<>(registry.getEligible(player));
        if (eligibleItems.isEmpty()) {
            plugin.getLogger().warning("No eligible NightMarket items are available for " + this.uniqueId + ".");
            return new ArrayList<>();
        }

        List<ShopItem> rollItems = eligibleItems;

        if (this.shopItems != null && plugin.getConfig().getBoolean("other.prevent_repeat_items", false)) {
            final List<ShopItem> nonRepeatingItems = new ArrayList<>(eligibleItems);
            nonRepeatingItems.removeIf(item -> this.shopItems.contains(item.id()));
            if (nonRepeatingItems.size() >= maxItems) {
                rollItems = nonRepeatingItems;
            }
        }

        WeightedRandom<ShopItem> random = WeightedRandom.fromCollection(rollItems, x -> x, ShopItem::rarity);
        final List<String> generated = new ArrayList<>();
        final int itemCount = Math.min(maxItems, random.getWeights().size());
        for (int i = 0; i < itemCount; i++) {
            final ShopItem item = random.roll();
            random.remove(item);
            generated.add(item.id());
        }

        plugin.debug("Created: " + String.join(",", generated));
        return generated;
    }

    public PlayerShop(UUID uniqueId, List<String> purchasedShopItems, List<String> shopItems) {
        this(uniqueId, purchasedShopItems, shopItems, false);
    }

    public PlayerShop(UUID uniqueId, List<String> purchasedShopItems, List<String> shopItems, boolean pendingRotation) {
        this.uniqueId = uniqueId;
        this.purchasedShopItems = deserializePurchased(purchasedShopItems);
        this.shopItems = new ArrayList<>(shopItems);
        this.pendingRotation = pendingRotation;
    }

    private static Map<String, Integer> deserializePurchased(List<String> list) {
        final Map<String, Integer> map = new HashMap<>();
        for (String data : list) {
            if (data == null || data.isEmpty()) {
                continue;
            }
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

    public boolean isPendingRotation() {
        return this.pendingRotation;
    }

    public void markPendingRotation(NightMarketPlugin plugin) {
        this.pendingRotation = true;
        plugin.debug("Marked NightMarket shop pending rotation: " + this.uniqueId);
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

    public void recordPurchase(NightMarketPlugin plugin, ShopItem item) {
        this.purchasedShopItems.merge(item.id(), 1, Integer::sum);
        plugin.getPlayerShopManager().getGlobalPurchaseCounts().merge(item.id(), 1, Integer::sum);
        plugin.debug("Player purchased item: " + item.id() + " (" + this.uniqueId + ")");
    }

    public void rotate(NightMarketPlugin plugin, ShopItemRegistry registry) {
        this.markPendingRotation(plugin);
    }

    public void rotate(NightMarketPlugin plugin, ShopItemRegistry registry, Player player) {
        if (player == null) {
            this.markPendingRotation(plugin);
            return;
        }

        plugin.debug("Rotating player shop: " + this.uniqueId);
        plugin.debug("Old shop: " + String.join(",", this.shopItems));
        this.shopItems = this.generateRandomShop(plugin, registry, player);
        plugin.debug("New shop: " + String.join(",", this.shopItems));
        this.purchasedShopItems.clear();
        this.pendingRotation = false;
    }
}
