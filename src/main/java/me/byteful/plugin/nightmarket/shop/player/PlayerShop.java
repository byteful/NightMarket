package me.byteful.plugin.nightmarket.shop.player;

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
    this.uniqueId = uniqueId;
    this.shopItems = generateRandomShop(itemRegistry);
    this.purchasedShopItems = new HashSet<>();
  }

  public PlayerShop(UUID uniqueId, Set<String> purchasedShopItems, List<String> shopItems) {
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
  }

  public void rotate(ShopItemRegistry registry) {
    shopItems = generateRandomShop(registry);
    purchasedShopItems.clear();
  }

  private List<String> generateRandomShop(ShopItemRegistry registry) {
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

    return generated;
  }
}