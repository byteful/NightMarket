package me.byteful.plugin.nightmarket;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import me.byteful.plugin.nightmarket.shop.item.ShopItem;
import me.byteful.plugin.nightmarket.shop.player.PlayerShop;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class NightMarketPlaceholders extends PlaceholderExpansion {
    private final NightMarketPlugin plugin;

    public NightMarketPlaceholders(NightMarketPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "nightmarket";
    }

    @Override
    public String getAuthor() {
        return "byteful";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return null;
        }

        final PlayerShop shop = this.plugin.getPlayerShopManager().get(player.getUniqueId());
        if (shop == null) {
            return null;
        }

        params = params.trim().replace(" ", "_");

        if (params.startsWith("stock_")) {
            final String itemId = params.substring(6);
            final ShopItem item = this.plugin.getShopItemRegistry().get(itemId);

            if (item == null) {
                return null;
            }

            final int limit = item.purchaseLimit();
            if (limit == Integer.MAX_VALUE) {
                return this.plugin.getMessageManager().get("infinite_stock");
            }

            final boolean globalCheck = this.plugin.getConfig().getBoolean("other.global_purchase_limits");
            final int purchased = globalCheck
                                  ? this.plugin.getPlayerShopManager().getGlobalPurchaseCount(item)
                                  : shop.getPurchasedShopItems().getOrDefault(itemId, 0);

            final int remaining = Math.max(0, limit - purchased);
            return String.valueOf(remaining);
        }

        switch (params.toLowerCase()) {
            case "purchased_items_count": {
                return String.valueOf(shop.getPurchasedShopItems().size());
            }

            case "available_items_count": {
                return String.valueOf(shop.getShopItems().size());
            }

            case "rotate": {
                return this.plugin.getRotateScheduleManager().getNextTime().format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a", Locale.US));
            }

            case "open": {
                return this.plugin.getAccessScheduleManager().getNextTime().format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a", Locale.US));
            }

            case "rotate_countdown": {
                return this.getCountdown(this.plugin.getRotateScheduleManager().getNextTime());
            }

            case "open_countdown": {
                return this.getCountdown(this.plugin.getAccessScheduleManager().getNextTime());
            }

            case "timezone": {
                return this.plugin.getTimezone().toString();
            }

            default: {
                return null;
            }
        }
    }

    private String getCountdown(LocalDateTime end) {
        final Duration between = Duration.between(LocalDateTime.now(this.plugin.getTimezone()), end);
        return this.convertDurationToString(between);
    }

    private String convertDurationToString(Duration duration) {
        final long seconds = duration.getSeconds();
        final long h = seconds / 3600;
        final long m = (seconds % 3600) / 60;
        final long s = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (h > 0) {
            sb.append(h).append("h, ");
        }
        if (h > 0 || m > 0) {
            sb.append(m).append("m, ");
        }
        sb.append(s).append("s");
        return sb.toString();
    }
}
