package me.byteful.plugin.nightmarket.parser;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.scheduler.ScheduledTask;
import me.byteful.plugin.nightmarket.shop.item.ConfirmPurchaseMode;
import me.byteful.plugin.nightmarket.shop.item.ShopItem;
import me.byteful.plugin.nightmarket.shop.player.PlayerShop;
import me.byteful.plugin.nightmarket.shop.player.PurchaseResult;
import me.byteful.plugin.nightmarket.util.Text;
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
        final IconData unavailableIcon = config.contains("unavailable_icon")
                                         ? IconParser.parse(config.getConfigurationSection("unavailable_icon"))
                                         : backgroundIcon;
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

        return new ParsedGUI(backgroundIcon, unavailableIcon, SlotNumberParser.parse(backgroundSlots),
            SlotNumberParser.parse(itemSlots), extraIcons, title, rows, updateFrequency);
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
        private final IconData unavailableIcon;
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

        public ParsedGUI(@NotNull IconData backgroundIcon, @NotNull IconData unavailableIcon, @NotNull List<Integer> backgroundSlots, @NotNull List<Integer> itemSlots,
                         @NotNull Set<ExtraIcon> extraIcons, @NotNull String title, int rows, int updateFrequency) {
            this.backgroundIcon = backgroundIcon;
            this.unavailableIcon = unavailableIcon;
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
            return this.build(player, plugin, Bukkit.getPlayer(player.getUniqueId()), Bukkit.getPlayer(player.getUniqueId()), false, true, this.title);
        }

        @NotNull
        public InventoryGUI buildReadOnly(@NotNull PlayerShop shop, @NotNull NightMarketPlugin plugin, @NotNull Player viewer, Player targetPlayer,
                                          @NotNull String viewedName) {
            final String configuredTitle = plugin.getConfig().getString("admin_view.title");
            Preconditions.checkNotNull(configuredTitle, "admin_view.title needs to not be null!");
            final String title = configuredTitle.replace("{player}", viewedName);

            return this.build(shop, plugin, viewer, targetPlayer, true, false, title);
        }

        @NotNull
        private InventoryGUI build(@NotNull PlayerShop player, @NotNull NightMarketPlugin plugin, Player viewer, Player targetPlayer, boolean readOnly,
                                   boolean persistRepairs, String title) {
            final Player bukkitPlayer = viewer;
            final TextProvider tp = plugin.getTextProvider();

            plugin.debug("Building NightMarket GUI for: " + player.getUniqueId());
            plugin.debug("Items list: " + String.join(",", player.getShopItems()));
            plugin.debug("Purchased items list: " + String.join(",", player.getPurchasedShopItems().keySet()));
            final InventoryGUI gui = new InventoryGUI(this.rows * 9, this.getLegacyTitle(tp, bukkitPlayer, plugin, title));
            final ItemButton bgButton = ItemButton.create(tp.buildItem(bukkitPlayer, this.backgroundIcon,
                plugin.getScheduleReplacementService().getReplacements(bukkitPlayer)), (e) -> {
            });

            final Runnable reloadBackground = () -> {
                for (Integer slot : this.backgroundSlots) {
                    gui.addButton(slot, bgButton);
                }
            };
            reloadBackground.run();

            final Runnable reloadExtraIcons = () -> {
                for (ExtraIcon extraIcon : this.extraIcons) {
                    final ItemButton iconButton = ItemButton.create(tp.buildItem(bukkitPlayer, extraIcon.getIcon(),
                        plugin.getScheduleReplacementService().getReplacements(bukkitPlayer)), (e) -> {
                    });

                    for (Integer slot : extraIcon.getSlots()) {
                        gui.addButton(slot, iconButton);
                    }
                }
            };
            reloadExtraIcons.run();

            final List<String> finalItems = this.validateAndResizeShopItems(player, plugin, targetPlayer, persistRepairs);

            final Runnable reloadItemIcons = () -> {
                final boolean globalCheck = plugin.getConfig().getBoolean("other.global_purchase_limits");
                for (int i = 0; i < this.itemSlots.size(); i++) {
                    final Integer slot = this.itemSlots.get(i);
                    if (i >= finalItems.size()) {
                        gui.addButton(slot, ItemButton.create(tp.buildItem(bukkitPlayer, this.unavailableIcon,
                            plugin.getScheduleReplacementService().getReplacements(bukkitPlayer)), event -> {
                        }));
                        continue;
                    }
                    final ShopItem item = plugin.getShopItemRegistry().get(finalItems.get(i));
                    if (item == null) {
                        gui.addButton(slot, ItemButton.create(tp.buildItem(bukkitPlayer, this.unavailableIcon,
                            plugin.getScheduleReplacementService().getReplacements(bukkitPlayer)), event -> {
                        }));
                        continue;
                    }
                    gui.addButton(slot, this.createItemButton(item, player, bukkitPlayer, plugin, globalCheck, readOnly));
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

        private String getLegacyTitle(TextProvider tp, Player player, NightMarketPlugin plugin, String title) {
            return tp.toLegacy(Text.applyPAPIAndReplace(player, title, plugin.getScheduleReplacementService().getReplacements(player)));
        }

        private List<String> validateAndResizeShopItems(PlayerShop player, NightMarketPlugin plugin, Player targetPlayer, boolean persistRepairs) {
            if (!persistRepairs) {
                final List<String> displayItems = new ArrayList<>(player.getShopItems());
                if (displayItems.size() > this.itemSlots.size()) {
                    return new ArrayList<>(displayItems.subList(0, this.itemSlots.size()));
                }
                return displayItems;
            }

            List<String> items = player.getShopItems();
            boolean changed = items.removeIf(x -> {
                final ShopItem item = plugin.getShopItemRegistry().get(x);
                return item == null || !plugin.getShopItemRegistry().isEligible(targetPlayer, item);
            });

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
                final List<ShopItem> eligibleItems = targetPlayer == null
                                                     ? plugin.getShopItemRegistry().getUngated()
                                                     : plugin.getShopItemRegistry().getEligible(targetPlayer);
                if (eligibleItems.isEmpty()) {
                    player.setShopItems(items);
                    plugin.debug("No eligible items available to fill resized GUI.");
                    return items;
                }
                final WeightedRandom<ShopItem> random = WeightedRandom.fromCollection(eligibleItems, x -> x, ShopItem::rarity);
                items.forEach(x -> random.remove(plugin.getShopItemRegistry().get(x)));
                final int fillCount = Math.min(diff, random.getWeights().size());
                for (int i = 0; i < fillCount; i++) {
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

        public String[] getItemReplacements(ShopItem item, PlayerShop player, Player bukkitPlayer, NightMarketPlugin plugin) {
            final boolean globalCheck = plugin.getConfig().getBoolean("other.global_purchase_limits");
            final int purchased = globalCheck ? plugin.getPlayerShopManager().getGlobalPurchaseCount(item) : player.getPurchaseCount(item.id());
            return this.getItemReplacements(item, player, bukkitPlayer, plugin, this.getStatusText(item, player, bukkitPlayer, plugin, purchased),
                purchased);
        }

        public String[] getItemPreviewReplacements(ShopItem item, PlayerShop player, Player bukkitPlayer, NightMarketPlugin plugin) {
            final boolean globalCheck = plugin.getConfig().getBoolean("other.global_purchase_limits");
            final int purchased = globalCheck ? plugin.getPlayerShopManager().getGlobalPurchaseCount(item) : player.getPurchaseCount(item.id());
            return this.getItemReplacements(item, player, bukkitPlayer, plugin, plugin.getMessageManager().get("status_item_preview"), purchased);
        }

        private String[] getItemReplacements(ShopItem item, PlayerShop player, Player bukkitPlayer, NightMarketPlugin plugin, String status,
                                             int purchased) {
            final int stock = item.purchaseLimit();
            return plugin.getScheduleReplacementService().append(bukkitPlayer,
                "{stock}", stock == Integer.MAX_VALUE ? plugin.getMessageManager().get("infinite_stock") : "" + stock,
                "{purchase_count}", "" + purchased,
                "{status}", status,
                "{item}", item.id(),
                "{price}", Text.formatPrice(plugin.getConfig(), item));
        }

        private String getStatusText(ShopItem item, PlayerShop player, Player bukkitPlayer, NightMarketPlugin plugin, int purchased) {
            final int stock = item.purchaseLimit();
            if (purchased >= stock) {
                return plugin.getMessageManager().get("status_bought_out");
            } else if (bukkitPlayer != null && bukkitPlayer.getInventory().firstEmpty() == -1) {
                return plugin.getMessageManager().get("status_inventory_full");
            } else if (bukkitPlayer != null && !plugin.getShopItemRegistry().isEligible(bukkitPlayer, item)) {
                return plugin.getMessageManager().get("status_no_permission");
            }
            return plugin.getMessageManager().get("status_available");
        }

        private ItemButton createItemButton(ShopItem item, PlayerShop player, Player bukkitPlayer, NightMarketPlugin plugin, boolean globalCheck,
                                            boolean readOnly) {
            final TextProvider tp = plugin.getTextProvider();
            final String[] replacements = this.getItemReplacements(item, player, bukkitPlayer, plugin);

            ItemStack isIcon;
            if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
                isIcon = tp.buildItem(bukkitPlayer, item.icon(), replacements);
            } else {
                isIcon = tp.buildItem(item.icon());
            }

            return ItemButton.create(isIcon, e -> {
                plugin.debug("Clicked GUI button: " + e.getWhoClicked().getName());
                if (!(e.getWhoClicked() instanceof Player clicker)) {
                    return;
                }

                if (readOnly) {
                    plugin.sendMessage(clicker, clicker, "admin_view_read_only");
                    return;
                }

                plugin.getScheduler().runForPlayer(clicker, () -> this.handlePurchaseClick(item, player, clicker, plugin));
            });
        }

        private void handlePurchaseClick(ShopItem item, PlayerShop player, Player clicker, NightMarketPlugin plugin) {
            final PurchaseResult validation = plugin.getPurchaseService().validate(clicker, player, item);
            if (!validation.success()) {
                plugin.sendMessage(clicker, clicker, validation.messageKey(), validation.replacements());
                clicker.closeInventory();
                return;
            }

            final boolean globalConfirm = plugin.getPurchaseConfirmationGUI().isEnabled();
            final boolean shouldConfirm = item.confirmPurchaseMode() == ConfirmPurchaseMode.ENABLED
                                          || item.confirmPurchaseMode() == ConfirmPurchaseMode.DEFAULT && globalConfirm;
            if (shouldConfirm) {
                plugin.getPurchaseConfirmationGUI().build(clicker, player, item, plugin).open(clicker);
                return;
            }

            final PurchaseResult result = plugin.getPurchaseService().attemptPurchase(clicker, player, item);
            plugin.sendMessage(clicker, clicker, result.messageKey(), result.replacements());
            if (result.success() && plugin.getConfig().getBoolean("other.close_on_buy")) {
                clicker.closeInventory();
            }
        }

        public void close(TextProvider tp) {
            final String lookFor = tp.toLegacy(this.title);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getOpenInventory().getTitle().equals(lookFor)) {
                    player.closeInventory();
                }
            }
        }
    }
}
