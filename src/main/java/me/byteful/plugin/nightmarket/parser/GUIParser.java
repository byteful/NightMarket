package me.byteful.plugin.nightmarket.parser;

import com.google.common.base.Preconditions;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.shop.item.ShopItem;
import me.byteful.plugin.nightmarket.shop.player.PlayerShop;
import me.byteful.plugin.nightmarket.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import redempt.redlib.inventorygui.InventoryGUI;
import redempt.redlib.inventorygui.ItemButton;
import redempt.redlib.misc.WeightedRandom;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static me.byteful.plugin.nightmarket.util.Text.color;

public class GUIParser {
    public static ParsedGUI parse(ConfigurationSection config) {
        final String title = config.getString("title", "NightMarket");
        final int rows = config.getInt("rows", 3);
        final int updateFrequency = config.getInt("update", -1);
        Preconditions.checkArgument(rows > 0 && rows < 7, "Rows needs to be greater than 0 and less than 7!");
        final ItemStack backgroundIcon = IconParser.parse(config.getConfigurationSection("background_icon"));
        final List<String> backgroundSlots = config.getStringList("background_slots");
        final List<String> itemSlots = config.getStringList("item_slots");
        Preconditions.checkArgument(!itemSlots.isEmpty(), "Item slots need to be greater than 0!");

        final Set<ExtraIcon> extraIcons = new HashSet<>();

        if (config.contains("extra_icons")) {
            final ConfigurationSection extraIconsConfig = config.getConfigurationSection("extra_icons");
            extraIconsConfig.getValues(false).forEach((id, data) -> {
                final ConfigurationSection iconConfig = (ConfigurationSection) data;
                final ItemStack icon = IconParser.parse(iconConfig);
                final List<Integer> slots = SlotNumberParser.parse(iconConfig.getStringList("slots"));

                extraIcons.add(new ExtraIcon(icon, slots));
            });
        }

        return new ParsedGUI(backgroundIcon, SlotNumberParser.parse(backgroundSlots), SlotNumberParser.parse(itemSlots), extraIcons, title, rows, updateFrequency);
    }

    public static class ExtraIcon {
        private final ItemStack icon;
        private final List<Integer> slots;

        private ExtraIcon(ItemStack icon, List<Integer> slots) {
            this.icon = icon;
            this.slots = slots;
        }

        public ItemStack getIcon() {
            return icon;
        }

        public List<Integer> getSlots() {
            return slots;
        }
    }

    public static class ParsedGUI {
        private final ItemStack backgroundItem;
        private final List<Integer> backgroundSlots;
        private final List<Integer> itemSlots;
        private final Set<ExtraIcon> extraIcons;
        private final String title;
        private final int rows;
        private final int updateFrequency;

        public ParsedGUI(ItemStack backgroundItem, List<Integer> backgroundSlots, List<Integer> itemSlots, Set<ExtraIcon> extraIcons, String title, int rows, int updateFrequency) {
            this.backgroundItem = backgroundItem;
            this.backgroundSlots = backgroundSlots;
            this.itemSlots = itemSlots;
            this.extraIcons = extraIcons;
            this.title = title;
            this.rows = rows;
            this.updateFrequency = updateFrequency;
        }

        public List<Integer> getItemSlots() {
            return itemSlots;
        }

        public String getTitle() {
            return color(title);
        }

        public InventoryGUI build(PlayerShop player, NightMarketPlugin plugin) {
            final Player bp = Bukkit.getPlayer(player.getUniqueId());

            plugin.debug("Building NightMarket GUI for: " + player.getUniqueId());
            plugin.debug("Items list: " + String.join(",", player.getShopItems()));
            plugin.debug("Purchased items list: " + String.join(",", player.getPurchasedShopItems().keySet()));
            final InventoryGUI gui = new InventoryGUI(rows * 9, getTitle());
            final ItemButton bgButton = ItemButton.create(backgroundItem, (e) -> {
            });

            final Runnable reloadBackground = () -> {
                for (Integer slot : backgroundSlots) {
                    gui.addButton(slot, bgButton);
                }
            };
            reloadBackground.run();

            final Runnable reloadExtraIcons = () -> {
                for (ExtraIcon extraIcon : extraIcons) {
                    final ItemButton iconButton = ItemButton.create(Text.specializeItem(bp, extraIcon.getIcon()), (e) -> {
                    });

                    for (Integer slot : extraIcon.getSlots()) {
                        gui.addButton(slot, iconButton);
                    }
                }
            };
            reloadExtraIcons.run();

            List<String> items = player.getShopItems();
            boolean changed = items.removeIf(x -> plugin.getShopItemRegistry().get(x) == null);
            if (items.size() > itemSlots.size()) {
                plugin.debug("(1) GUI was previously resized. Old items list: " + String.join(",", items));
                items = items.subList(0, itemSlots.size());
                changed = true;
                player.setShopItems(items);
                plugin.debug("New items list: " + String.join(",", items));
            }
            if (items.size() < itemSlots.size()) {
                plugin.debug("(2) GUI was previously resized. Old items list: " + String.join(",", items));
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
                player.setShopItems(items);
                plugin.debug("New items list: " + String.join(",", items));
            }

            if (changed) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDataStoreProvider().setPlayerShop(player));
                plugin.debug("Updated items to DB: " + String.join(",", items));
            }

            final List<String> finalItems = items;

            final Runnable reloadItemIcons = () -> {
                for (int i = 0; i < itemSlots.size(); i++) {
                    final Integer slot = itemSlots.get(i);
                    final ShopItem item = plugin.getShopItemRegistry().get(finalItems.get(i));

                    final boolean globalCheck = plugin.getConfig().getBoolean("other.global_purchase_limits");
                    final int purchased = globalCheck ? plugin.getPlayerShopManager().getGlobalPurchaseCount(item) : player.getPurchaseCount(item.getId());
                    final int stock = item.getPurchaseLimit();

                    ItemStack isIcon;

                    if (bp != null && bp.isOnline()) {
                        isIcon = Text.specializeItem(bp, item.getIcon(), "{stock}", stock == Integer.MAX_VALUE ? "∞" : "" + stock, "{purchase_count}", "" + purchased);
                    } else {
                        isIcon = item.getIcon();
                    }

                    gui.addButton(slot, ItemButton.create(isIcon, e -> {
                        plugin.debug("Clicked GUI button: " + e.getWhoClicked().getName());
                        if (!(e.getWhoClicked() instanceof Player)) {
                            return;
                        }

                        // Recheck these variables in case of time delay between icon creation and execution.
                        final boolean globalPurchaseLimits = plugin.getConfig().getBoolean("other.global_purchase_limits");
                        final int purchaseCount = globalPurchaseLimits ? plugin.getPlayerShopManager().getGlobalPurchaseCount(item) : player.getPurchaseCount(item.getId());
                        final String messageKey = globalPurchaseLimits ? "item_max_global_purchases" : "already_purchased";

                        if (purchaseCount >= item.getPurchaseLimit()) {
                            e.getWhoClicked().sendMessage(plugin.getMessage((Player) e.getWhoClicked(), messageKey));
                            e.getWhoClicked().closeInventory();
                            return;
                        }

                        if (!item.getCurrency().canPlayerAfford(player.getUniqueId(), item.getAmount())) {
                            String currencyName = item.getCurrency().getName(item.getAmount());
                            if (plugin.getConfig().getBoolean("other.lowercase_currency_names")) {
                                currencyName = currencyName.toLowerCase();
                            }

                            e.getWhoClicked().sendMessage(plugin.getMessage((Player) e.getWhoClicked(), "cannot_afford").replace("{amount}", Text.formatCurrency(item.getAmount())).replace("{currency}", currencyName));
                            e.getWhoClicked().closeInventory();

                            return;
                        }

                        player.purchaseItem(item);
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDataStoreProvider().setPlayerShop(player));
                        if (plugin.getConfig().getBoolean("other.close_on_buy")) {
                            e.getWhoClicked().closeInventory();
                        }
                        e.getWhoClicked().sendMessage(plugin.getMessage((Player) e.getWhoClicked(), "successfully_purchased_item").replace("{item}", item.getId()));
                    }));
                }
            };
            reloadItemIcons.run();

            if (updateFrequency > 0) {
                final BukkitTask updateTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (isCancelled() || gui.getInventory() == null || gui.getInventory().isEmpty()) {
                            cancel();
                            return;
                        }

                        gui.clear();
                        reloadBackground.run();
                        reloadExtraIcons.run();
                        reloadItemIcons.run();
                        gui.update();
                    }
                }.runTaskTimer(plugin, updateFrequency, updateFrequency);

                gui.setOnDestroy(updateTask::cancel);
            }

            return gui;
        }

        public void close() {
            final String lookFor = getTitle();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getOpenInventory().getTitle().equals(lookFor)) {
                    player.closeInventory();
                }
            }
        }
    }
}
