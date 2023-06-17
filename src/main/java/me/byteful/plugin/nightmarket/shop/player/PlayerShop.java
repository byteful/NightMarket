package me.byteful.plugin.nightmarket.shop.player;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.shop.item.ShopItem;
import me.byteful.plugin.nightmarket.shop.item.ShopItemRegistry;
import me.byteful.plugin.nightmarket.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import redempt.redlib.misc.WeightedRandom;

import java.util.*;

public class PlayerShop {
    private final UUID uniqueId;
    private final Set<String> purchasedShopItems;
    private List<String> shopItems;

    public PlayerShop(ShopItemRegistry itemRegistry, UUID uniqueId) {
        new RuntimeException("Create load").printStackTrace();
        this.uniqueId = uniqueId;
        this.shopItems = generateRandomShop(itemRegistry);
        this.purchasedShopItems = new HashSet<>();
    }

    public PlayerShop(UUID uniqueId, Set<String> purchasedShopItems, List<String> shopItems) {
        new RuntimeException("DB load").printStackTrace();
        this.uniqueId = uniqueId;
        this.purchasedShopItems = purchasedShopItems;
        this.shopItems = shopItems;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public List<String> getShopItems() {
        return shopItems;
    }

    public void setShopItems(List<String> shopItems) {
        this.shopItems = shopItems;
    }

    public Set<String> getPurchasedShopItems() {
        return purchasedShopItems;
    }

    public boolean hasPurchasedItem(String item) {
        return purchasedShopItems.contains(item);
    }

    public void purchaseItem(ShopItem item) {
        purchasedShopItems.add(item.getId());
        final OfflinePlayer player = Bukkit.getOfflinePlayer(uniqueId);
        final String cmd = item.getCommand().startsWith("/") ? item.getCommand().substring(1) : item.getCommand();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Text.format(player, cmd.replace("{player}", Objects.requireNonNull(player.getName()))));
        item.getCurrency().withdraw(uniqueId, item.getAmount());
        NightMarketPlugin.getInstance().debug("Player purchased item: " + item.getId() + " (" + uniqueId + ")");
    }

    public void rotate(ShopItemRegistry registry) {
        NightMarketPlugin.getInstance().debug("Rotating player shop: " + uniqueId);
        NightMarketPlugin.getInstance().debug("Old shop: " + String.join(",", shopItems));
        shopItems = generateRandomShop(registry);
        NightMarketPlugin.getInstance().debug("New shop: " + String.join(",", shopItems));
        purchasedShopItems.clear();
    }

    private List<String> generateRandomShop(ShopItemRegistry registry) {
        NightMarketPlugin.getInstance().debug("Creating a new, random shop for: " + uniqueId);
        final int maxItems = registry.getMaxItems();
        if (registry.getAll().size() < maxItems) {
            throw new RuntimeException("There are not enough items to generate shops! You need more items than slots in the GUI!");
        }

        final WeightedRandom<ShopItem> random = WeightedRandom.fromCollection(registry.getAll(), x -> x, ShopItem::getRarity);

        final List<String> generated = new ArrayList<>();
        for (int i = 0; i < maxItems; i++) {
            final ShopItem item = random.roll();
            random.remove(item);
            generated.add(item.getId());
        }

        NightMarketPlugin.getInstance().debug("Created: " + String.join(",", generated));
        return generated;
    }
}
