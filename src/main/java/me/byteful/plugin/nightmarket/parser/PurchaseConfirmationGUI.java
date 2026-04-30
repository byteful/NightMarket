package me.byteful.plugin.nightmarket.parser;

import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.shop.item.ShopItem;
import me.byteful.plugin.nightmarket.shop.player.PlayerShop;
import me.byteful.plugin.nightmarket.shop.player.PurchaseResult;
import me.byteful.plugin.nightmarket.util.Text;
import me.byteful.plugin.nightmarket.util.text.TextProvider;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import redempt.redlib.inventorygui.InventoryGUI;
import redempt.redlib.inventorygui.ItemButton;

public class PurchaseConfirmationGUI {
    private final boolean enabled;
    private final String title;
    private final int rows;
    private final IconData backgroundIcon;
    private final List<Integer> backgroundSlots;
    private final int previewSlot;
    private final IconData confirmIcon;
    private final List<Integer> confirmSlots;
    private final IconData denyIcon;
    private final List<Integer> denySlots;

    private PurchaseConfirmationGUI(boolean enabled, String title, int rows, IconData backgroundIcon, List<Integer> backgroundSlots, int previewSlot,
                                    IconData confirmIcon, List<Integer> confirmSlots, IconData denyIcon, List<Integer> denySlots) {
        this.enabled = enabled;
        this.title = title;
        this.rows = rows;
        this.backgroundIcon = backgroundIcon;
        this.backgroundSlots = backgroundSlots;
        this.previewSlot = previewSlot;
        this.confirmIcon = confirmIcon;
        this.confirmSlots = confirmSlots;
        this.denyIcon = denyIcon;
        this.denySlots = denySlots;
    }

    public static PurchaseConfirmationGUI parse(ConfigurationSection config) {
        Preconditions.checkNotNull(config, "Config for purchase_confirmation needs to not be null!");
        final boolean enabled = config.getBoolean("enabled", true);
        final String title = config.getString("title");
        Preconditions.checkNotNull(title, "purchase_confirmation.title needs to not be null!");
        final int rows = config.getInt("rows", 3);
        Preconditions.checkArgument(rows > 0 && rows < 7, "Purchase confirmation rows needs to be greater than 0 and less than 7!");

        final IconData backgroundIcon = IconParser.parse(config.getConfigurationSection("background_icon"));
        final List<Integer> backgroundSlots = SlotNumberParser.parse(config.getStringList("background_slots"));
        final int previewSlot = config.getInt("preview_slot", 13);
        final IconData confirmIcon = IconParser.parse(config.getConfigurationSection("confirm_icon"));
        final List<Integer> confirmSlots = parseActionSlots(config, "confirm_slots", "confirm_slot", 11);
        final IconData denyIcon = IconParser.parse(config.getConfigurationSection("deny_icon"));
        final List<Integer> denySlots = parseActionSlots(config, "deny_slots", "deny_slot", 15);
        validateSlot(rows, previewSlot, "preview_slot");
        validateSlots(rows, confirmSlots, "confirm slots");
        validateSlots(rows, denySlots, "deny slots");
        validateCollisions(previewSlot, confirmSlots, denySlots);

        return new PurchaseConfirmationGUI(enabled, title, rows, backgroundIcon, backgroundSlots, previewSlot, confirmIcon, confirmSlots, denyIcon,
            denySlots);
    }

    private static void validateSlot(int rows, int slot, String key) {
        Preconditions.checkArgument(slot >= 0 && slot < rows * 9, key + " must be inside the purchase confirmation inventory.");
    }

    private static List<Integer> parseActionSlots(ConfigurationSection config, String listKey, String legacyKey, int defaultSlot) {
        if (config.contains(legacyKey)) {
            return List.of(config.getInt(legacyKey, defaultSlot));
        }

        final List<Integer> parsedSlots = SlotNumberParser.parse(config.getStringList(listKey));
        if (!parsedSlots.isEmpty()) {
            return parsedSlots;
        }

        return List.of(defaultSlot);
    }

    private static void validateSlots(int rows, List<Integer> slots, String key) {
        Preconditions.checkArgument(!slots.isEmpty(), "Purchase confirmation " + key + " must contain at least one slot.");
        final Set<Integer> unique = new HashSet<>();
        for (Integer slot : slots) {
            Preconditions.checkNotNull(slot, "Purchase confirmation " + key + " cannot contain null slots.");
            validateSlot(rows, slot, key);
            Preconditions.checkArgument(unique.add(slot), "Purchase confirmation " + key + " cannot contain duplicate slots.");
        }
    }

    private static void validateCollisions(int previewSlot, List<Integer> confirmSlots, List<Integer> denySlots) {
        final Set<Integer> slots = new HashSet<>();
        slots.add(previewSlot);
        slots.addAll(confirmSlots);
        slots.addAll(denySlots);
        Preconditions.checkArgument(slots.size() == 1 + confirmSlots.size() + denySlots.size(),
            "Purchase confirmation preview, confirm, and deny slots must be unique.");
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public InventoryGUI build(Player clicker, PlayerShop shop, ShopItem item, NightMarketPlugin plugin) {
        final TextProvider tp = plugin.getTextProvider();
        final InventoryGUI gui = new InventoryGUI(this.rows * 9, this.getLegacyTitle(tp, clicker, item, plugin));
        final ItemButton backgroundButton = ItemButton.create(tp.buildItem(clicker, this.backgroundIcon,
            plugin.getScheduleReplacementService().getReplacements(clicker)), event -> {
        });
        for (Integer slot : this.backgroundSlots) {
            gui.addButton(slot, backgroundButton);
        }

        gui.addButton(this.previewSlot, ItemButton.create(tp.buildItem(clicker, item.icon(),
            plugin.getParsedGUI().getItemPreviewReplacements(item, shop, clicker, plugin)), event -> {}));
        final ItemButton confirmButton = ItemButton.create(tp.buildItem(clicker, this.confirmIcon,
            plugin.getParsedGUI().getItemReplacements(item, shop, clicker, plugin)), event -> {
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            plugin.getScheduler().runForPlayer(player, () -> {
                final PurchaseResult result = plugin.getPurchaseService().attemptPurchase(player, shop, item);
                plugin.sendMessage(player, player, result.messageKey(), result.replacements());
                if (result.success()) {
                    if (plugin.getConfig().getBoolean("other.close_on_buy")) {
                        player.closeInventory();
                    } else if (plugin.getAccessScheduleManager().isShopOpen()) {
                        plugin.getParsedGUI().build(shop, plugin).open(player);
                    } else {
                        player.closeInventory();
                    }
                }
            });
        });
        for (Integer slot : this.confirmSlots) {
            gui.addButton(slot, confirmButton);
        }

        final ItemButton denyButton = ItemButton.create(tp.buildItem(clicker, this.denyIcon,
            plugin.getParsedGUI().getItemReplacements(item, shop, clicker, plugin)), event -> {
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            plugin.getScheduler().runForPlayer(player, () -> {
                if (plugin.getAccessScheduleManager().isShopOpen() && plugin.getCurrencyRegistry().isLoaded()) {
                    plugin.getParsedGUI().build(shop, plugin).open(player);
                } else {
                    player.closeInventory();
                    plugin.sendMessage(player, player, "shop_not_open");
                }
            });
        });
        for (Integer slot : this.denySlots) {
            gui.addButton(slot, denyButton);
        }

        return gui;
    }

    private String getLegacyTitle(TextProvider tp, Player player, ShopItem item, NightMarketPlugin plugin) {
        return tp.toLegacy(Text.applyPAPIAndReplace(player, this.title, plugin.getScheduleReplacementService().append(player, "{item}", item.id())));
    }
}
