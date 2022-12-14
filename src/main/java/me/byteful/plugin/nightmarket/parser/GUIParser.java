package me.byteful.plugin.nightmarket.parser;

import com.google.common.base.Preconditions;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.shop.item.ShopItem;
import me.byteful.plugin.nightmarket.shop.player.PlayerShop;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import redempt.redlib.inventorygui.InventoryGUI;
import redempt.redlib.inventorygui.ItemButton;
import redempt.redlib.misc.WeightedRandom;

import java.util.List;

import static me.byteful.plugin.nightmarket.util.Text.color;

public class GUIParser {
  public static ParsedGUI parse(ConfigurationSection config) {
    final String title = config.getString("title");
    final int rows = config.getInt("rows");
    Preconditions.checkArgument(rows > 0 && rows < 7, "Rows needs to be greater than 0 and less than 7!");
    final ItemStack backgroundIcon = IconParser.parse(config.getConfigurationSection("background_icon"));
    final List<String> backgroundSlots = config.getStringList("background_slots");
    final List<String> itemSlots = config.getStringList("item_slots");
    Preconditions.checkArgument(itemSlots.size() > 0, "Item slots need to be greater than 0!");

    return new ParsedGUI(backgroundIcon, SlotNumberParser.parse(backgroundSlots), SlotNumberParser.parse(itemSlots), title, rows);
  }

  public static class ParsedGUI {
    private final ItemStack backgroundItem;
    private final List<Integer> backgroundSlots;
    private final List<Integer> itemSlots;
    private final String title;
    private final int rows;

    public ParsedGUI(ItemStack backgroundItem, List<Integer> backgroundSlots, List<Integer> itemSlots, String title, int rows) {
      this.backgroundItem = backgroundItem;
      this.backgroundSlots = backgroundSlots;
      this.itemSlots = itemSlots;
      this.title = title;
      this.rows = rows;
    }

    public List<Integer> getItemSlots() {
      return itemSlots;
    }

    public String getTitle() {
      return color(title);
    }

    public InventoryGUI build(PlayerShop player, NightMarketPlugin plugin) {
      final InventoryGUI gui = new InventoryGUI(rows * 9, getTitle());
      final ItemButton bgButton = ItemButton.create(backgroundItem, (e) -> {
      });
      for (Integer slot : backgroundSlots) {
        gui.addButton(slot, bgButton);
      }
      List<String> items = player.getShopItems();
      if (items.size() > itemSlots.size()) {
        items = items.subList(0, itemSlots.size());
      }
      boolean changed = items.removeIf(x -> plugin.getShopItemRegistry().get(x) == null);
      if (items.size() < itemSlots.size()) {
        final int diff = itemSlots.size() - items.size();
        final WeightedRandom<ShopItem> random = WeightedRandom.fromCollection(plugin.getShopItemRegistry().getAll(), x -> x, ShopItem::getRarity);
        items.forEach(x -> random.remove(plugin.getShopItemRegistry().get(x)));
        if (random.getWeights().size() < diff) {
          throw new RuntimeException("There are not enough items to generate shops! You need more items than slots in the GUI!");
        }
        for (int i = 0; i < diff; i++) {
          final ShopItem item = random.roll();
          random.remove(item);
          items.add(item.getId());
        }
        changed = true;
      }

      if (changed) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDataStoreProvider().setPlayerShop(player));
      }
      for (int i = 0; i < itemSlots.size(); i++) {
        final Integer slot = itemSlots.get(i);
        final ShopItem item = plugin.getShopItemRegistry().get(items.get(i));

        gui.addButton(slot, ItemButton.create(item.getIcon(), e -> {
          if (!(e.getWhoClicked() instanceof Player)) {
            return;
          }

          if (!item.isMultiplePurchase() && player.hasPurchasedItem(item.getId())) {
            e.getWhoClicked().sendMessage(plugin.getMessage((Player) e.getWhoClicked(), "already_purchased"));
            e.getWhoClicked().closeInventory();

            return;
          }

          if (!item.getCurrency().canPlayerAfford(player.getUniqueId(), item.getAmount())) {
            e.getWhoClicked().sendMessage(plugin.getMessage((Player) e.getWhoClicked(), "cannot_afford"));
            e.getWhoClicked().closeInventory();

            return;
          }

          player.purchaseItem(item);
          Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDataStoreProvider().setPlayerShop(player));
          if (plugin.getConfig().getBoolean("other.close_on_buy")) {
            e.getWhoClicked().closeInventory();
          }
          e.getWhoClicked().sendMessage(plugin.getMessage((Player) e.getWhoClicked(), "successfully_purchased_item"));
        }));
      }

      return gui;
    }
  }
}
