package me.byteful.plugin.nightmarket.parser;

import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.scheduler.ScheduledTask;
import me.byteful.plugin.nightmarket.shop.item.ShopItem;
import me.byteful.plugin.nightmarket.shop.player.PlayerShop;
import me.byteful.plugin.nightmarket.util.text.TextProvider;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import redempt.redlib.inventorygui.InventoryGUI;
import redempt.redlib.inventorygui.ItemButton;
import redempt.redlib.misc.WeightedRandom;

public class GUIParser {
    @NotNull
    public static ParsedGUI parse(@NotNull ConfigurationSection config) {
        final String title = config.getString("title", "NightMarket");
        final int rows = config.getInt("rows", 3);
        final int updateFrequency = config.getInt("update", -1);
        Preconditions.checkArgument(rows > 0 && rows < 7, "Rows needs to be greater than 0 and less than 7!");
        final IconData backgroundIcon = IconParser.parse(config.getConfigurationSection("background_icon"));
        final List<String> backgroundSlots = config.getStringList("background_slots");
        final List<String> itemSlots = config.getStringList("item_slots");
        Preconditions.checkArgument(!itemSlots.isEmpty(), "Item slots need to be greater than 0!");

        final Set<ExtraIcon> extraIcons = new HashSet<>();

        if (config.contains("extra_icons")) {
            final ConfigurationSection extraIconsConfig = config.getConfigurationSection("extra_icons");
            extraIconsConfig.getValues(false).forEach((id, data) -> {
                final ConfigurationSection iconConfig = (ConfigurationSection) data;
                final IconData icon = IconParser.parse(iconConfig);
                final List<Integer> slots = SlotNumberParser.parse(iconConfig.getStringList("slots"));

                extraIcons.add(new ExtraIcon(icon, slots));
            });
        }

        return new ParsedGUI(backgroundIcon, SlotNumberParser.parse(backgroundSlots), SlotNumberParser.parse(itemSlots), extraIcons, title, rows,
            updateFrequency);
    }

    public static class ExtraIcon {
        @NotNull
        private final IconData icon;
        @NotNull
        private final List<Integer> slots;

        private ExtraIcon(@NotNull IconData icon, @NotNull List<Integer> slots) {
            this.icon = icon;
            this.slots = slots;
        }

        public @NotNull IconData getIcon() {
            return this.icon;
        }

        public @NotNull List<Integer> getSlots() {
            return this.slots;
        }
    }

    public static class ParsedGUI {
        @NotNull
        private final IconData backgroundIcon;
        @NotNull
        private final List<Integer> backgroundSlots;
        @NotNull
        private final List<Integer> itemSlots;
        @NotNull
        private final Set<ExtraIcon> extraIcons;
        @NotNull
        private final String title;
        private final int rows;
        private final int updateFrequency;
        private String legacyTitle;

        public ParsedGUI(@NotNull IconData backgroundIcon, @NotNull List<Integer> backgroundSlots, @NotNull List<Integer> itemSlots,
                         @NotNull Set<ExtraIcon> extraIcons, @NotNull String title, int rows, int updateFrequency) {
            this.backgroundIcon = backgroundIcon;
            this.backgroundSlots = backgroundSlots;
            this.itemSlots = itemSlots;
            this.extraIcons = extraIcons;
            this.title = title;
            this.rows = rows;
            this.updateFrequency = updateFrequency;
        }

        public @NotNull List<Integer> getItemSlots() {
            return this.itemSlots;
        }

        @NotNull
        public InventoryGUI build(@NotNull PlayerShop player, @NotNull NightMarketPlugin plugin) {
            final Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
            final TextProvider tp = plugin.getTextProvider();

            plugin.debug("Building NightMarket GUI for: " + player.getUniqueId());
            plugin.debug("Items list: " + String.join(",", player.getShopItems()));
            plugin.debug("Purchased items list: " + String.join(",", player.getPurchasedShopItems().keySet()));
            final InventoryGUI gui = new InventoryGUI(this.rows * 9, this.getLegacyTitle(tp));
            final ItemButton bgButton = ItemButton.create(tp.buildItem(this.backgroundIcon), (e) -> {
            });

            final Runnable reloadBackground = () -> {
                for (Integer slot : this.backgroundSlots) {
                    gui.addButton(slot, bgButton);
                }
            };
            reloadBackground.run();

            final Runnable reloadExtraIcons = () -> {
                for (ExtraIcon extraIcon : this.extraIcons) {
                    final ItemButton iconButton = ItemButton.create(tp.buildItem(bukkitPlayer, extraIcon.getIcon()), (e) -> {
                    });

                    for (Integer slot : extraIcon.getSlots()) {
                        gui.addButton(slot, iconButton);
                    }
                }
            };
            reloadExtraIcons.run();

            final List<String> finalItems = this.validateAndResizeShopItems(player, plugin);

            final Runnable reloadItemIcons = () -> {
                final boolean globalCheck = plugin.getConfig().getBoolean("other.global_purchase_limits");
                for (int i = 0; i < this.itemSlots.size(); i++) {
                    final Integer slot = this.itemSlots.get(i);
                    final ShopItem item = plugin.getShopItemRegistry().get(finalItems.get(i));
                    gui.addButton(slot, this.createItemButton(item, player, bukkitPlayer, plugin, globalCheck));
                }
            };
            reloadItemIcons.run();

            if (this.updateFrequency > 0) {
                final AtomicReference<ScheduledTask> taskRef = new AtomicReference<>();
                final ScheduledTask updateTask = plugin.getScheduler().runGlobalTimer(() -> {
                    if (gui.getInventory() == null || gui.getInventory().isEmpty()) {
                        final ScheduledTask self = taskRef.get();
                        if (self != null) {
                            self.cancel();
                        }
                        return;
                    }
                    gui.clear();
                    reloadBackground.run();
                    reloadExtraIcons.run();
                    reloadItemIcons.run();
                    gui.update();
                }, this.updateFrequency, this.updateFrequency);
                taskRef.set(updateTask);

                gui.setOnDestroy(updateTask::cancel);
            }

            return gui;
        }

        private String getLegacyTitle(TextProvider tp) {
            String cached = this.legacyTitle;
            if (cached == null) {
                cached = tp.toLegacy(this.title);
                this.legacyTitle = cached;
            }
            return cached;
        }

        private List<String> validateAndResizeShopItems(PlayerShop player, NightMarketPlugin plugin) {
            List<String> items = player.getShopItems();
            boolean changed = items.removeIf(x -> plugin.getShopItemRegistry().get(x) == null);

            if (items.size() > this.itemSlots.size()) {
                plugin.debug("(1) GUI was previously resized. Old items list: " + String.join(",", items));
                items = items.subList(0, this.itemSlots.size());
                changed = true;
                player.setShopItems(items);
                plugin.debug("New items list: " + String.join(",", items));
            }

            if (items.size() < this.itemSlots.size()) {
                plugin.debug("(2) GUI was previously resized. Old items list: " + String.join(",", items));
                final int diff = this.itemSlots.size() - items.size();
                final WeightedRandom<ShopItem> random = WeightedRandom.fromCollection(plugin.getShopItemRegistry().getAll(), x -> x, ShopItem::rarity);
                items.forEach(x -> random.remove(plugin.getShopItemRegistry().get(x)));
                if (random.getWeights().size() < diff) {
                    throw new RuntimeException("There are not enough items to generate shops! You need more items than slots in the GUI!");
                }
                for (int i = 0; i < diff; i++) {
                    final ShopItem item = random.roll();
                    random.remove(item);
                    items.add(item.id());
                }
                changed = true;
                player.setShopItems(items);
                plugin.debug("New items list: " + String.join(",", items));
            }

            if (changed) {
                plugin.getScheduler().runAsync(() -> plugin.getDataStoreProvider().setPlayerShop(player));
                plugin.debug("Updated items to DB: " + String.join(",", items));
            }

            return items;
        }

        private ItemButton createItemButton(ShopItem item, PlayerShop player, Player bukkitPlayer, NightMarketPlugin plugin, boolean globalCheck) {
            final TextProvider tp = plugin.getTextProvider();
            final int purchased = globalCheck ? plugin.getPlayerShopManager().getGlobalPurchaseCount(item) : player.getPurchaseCount(item.id());
            final int stock = item.purchaseLimit();

            String statusText;
            if (purchased >= stock) {
                statusText = plugin.getMessageManager().get("status_bought_out");
            } else if (bukkitPlayer != null && bukkitPlayer.getInventory().firstEmpty() == -1) {
                statusText = plugin.getMessageManager().get("status_inventory_full");
            } else {
                statusText = plugin.getMessageManager().get("status_available");
            }

            ItemStack isIcon;
            if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
                isIcon = tp.buildItem(bukkitPlayer, item.icon(),
                    "{stock}", stock == Integer.MAX_VALUE ? plugin.getMessageManager().get("infinite_stock") : "" + stock,
                    "{purchase_count}", "" + purchased,
                    "{status}", statusText);
            } else {
                isIcon = tp.buildItem(item.icon());
            }

            return ItemButton.create(isIcon, e -> {
                plugin.debug("Clicked GUI button: " + e.getWhoClicked().getName());
                if (!(e.getWhoClicked() instanceof Player clicker)) {
                    return;
                }

                final boolean globalPurchaseLimits = plugin.getConfig().getBoolean("other.global_purchase_limits");
                final int purchaseCount = globalPurchaseLimits ? plugin.getPlayerShopManager().getGlobalPurchaseCount(item) : player.getPurchaseCount(
                    item.id());
                final String messageKey = globalPurchaseLimits ? "item_max_global_purchases" : "already_purchased";

                if (purchaseCount >= item.purchaseLimit()) {
                    plugin.sendMessage(clicker, clicker, messageKey);
                    clicker.closeInventory();
                    return;
                }

                if (!item.currency().canPlayerAfford(player.getUniqueId(), item.amount())) {
                    String formatted = item.currency().format(item.amount());
                    if (plugin.getConfig().getBoolean("other.lowercase_currency_names")) {
                        formatted = formatted.toLowerCase();
                    }

                    plugin.sendMessage(clicker, clicker, "cannot_afford", "{amount}", formatted, "{currency}", "");
                    clicker.closeInventory();
                    return;
                }

                player.purchaseItem(plugin, item);
                plugin.getScheduler().runAsync(() -> plugin.getDataStoreProvider().setPlayerShop(player));
                if (plugin.getConfig().getBoolean("other.close_on_buy")) {
                    clicker.closeInventory();
                }
                plugin.sendMessage(clicker, clicker, "successfully_purchased_item", "{item}", item.id());
            });
        }

        public void close(TextProvider tp) {
            final String lookFor = this.getLegacyTitle(tp);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getOpenInventory().getTitle().equals(lookFor)) {
                    player.closeInventory();
                }
            }
        }
    }
}
