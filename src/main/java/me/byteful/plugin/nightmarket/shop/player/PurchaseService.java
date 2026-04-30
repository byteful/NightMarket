package me.byteful.plugin.nightmarket.shop.player;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.shop.item.ShopItem;
import me.byteful.plugin.nightmarket.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PurchaseService {
    private static final long PURCHASE_COOLDOWN_MS = 750L;

    private final Set<String> inProgress = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Long> lastPurchaseAt = new ConcurrentHashMap<>();
    private final NightMarketPlugin plugin;

    public PurchaseService(NightMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public PurchaseResult attemptPurchase(Player buyer, PlayerShop shop, ShopItem item) {
        final PlayerShop currentShop = this.plugin.getPlayerShopManager().getLoaded(buyer.getUniqueId());
        if (currentShop == null || currentShop != shop) {
            return PurchaseResult.failure("item_not_available");
        }

        final boolean globalPurchaseLimits = this.plugin.getConfig().getBoolean("other.global_purchase_limits");
        final String guardKey = globalPurchaseLimits ? "global:" + item.id() : buyer.getUniqueId() + ":" + item.id();
        final String cooldownKey = buyer.getUniqueId() + ":" + item.id();
        if (!this.inProgress.add(guardKey)) {
            return PurchaseResult.failure("purchase_in_progress");
        }

        try {
            final long now = System.currentTimeMillis();
            final Long lastPurchase = this.lastPurchaseAt.get(cooldownKey);
            if (lastPurchase != null && now - lastPurchase < PURCHASE_COOLDOWN_MS) {
                return PurchaseResult.failure("purchase_in_progress");
            }

            PurchaseResult validation = this.validate(buyer, currentShop, item);
            if (!validation.success()) {
                return validation;
            }

            item.currency().withdraw(currentShop.getUniqueId(), item.amount());
            currentShop.recordPurchase(this.plugin, item);
            this.lastPurchaseAt.put(cooldownKey, now);
            this.plugin.getScheduler().runAsync(() -> this.plugin.getDataStoreProvider().setPlayerShop(currentShop));
            this.dispatchCommands(buyer, item);
            return PurchaseResult.success("successfully_purchased_item", "{item}", item.id());
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to process NightMarket purchase for " + buyer.getUniqueId() + " item " + item.id(), e);
            return PurchaseResult.failure("purchase_failed");
        } finally {
            this.inProgress.remove(guardKey);
        }
    }

    public PurchaseResult validate(Player buyer, PlayerShop shop, ShopItem item) {
        if (!shop.getShopItems().contains(item.id())) {
            return PurchaseResult.failure("item_not_available");
        }

        if (!this.plugin.getShopItemRegistry().isEligible(buyer, item)) {
            return PurchaseResult.failure("no_item_permission");
        }

        final boolean globalPurchaseLimits = this.plugin.getConfig().getBoolean("other.global_purchase_limits");
        final int purchaseCount = globalPurchaseLimits
                                  ? this.plugin.getPlayerShopManager().getGlobalPurchaseCount(item)
                                  : shop.getPurchaseCount(item.id());
        if (purchaseCount >= item.purchaseLimit()) {
            return PurchaseResult.failure(globalPurchaseLimits ? "item_max_global_purchases" : "already_purchased");
        }

        if (buyer.getInventory().firstEmpty() == -1) {
            return PurchaseResult.failure("inventory_full");
        }

        if (!item.currency().canPlayerAfford(shop.getUniqueId(), item.amount())) {
            final String formatted = Text.formatPrice(this.plugin.getConfig(), item);
            return PurchaseResult.failure("cannot_afford", "{amount}", formatted, "{currency}", "");
        }

        return PurchaseResult.success("purchase_available");
    }

    private void dispatchCommands(Player buyer, ShopItem item) {
        final OfflinePlayer offlineBuyer = Bukkit.getOfflinePlayer(buyer.getUniqueId());
        final String playerName = Objects.requireNonNullElse(offlineBuyer.getName(), buyer.getName());
        for (String configuredCommand : item.commands()) {
            String command = configuredCommand.startsWith("/") ? configuredCommand.substring(1) : configuredCommand;
            command = command.replace("{player}", playerName);
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Text.applyPAPI(offlineBuyer, command));
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to dispatch NightMarket purchase command for " + buyer.getUniqueId() + ": " + command,
                    e);
            }
        }
    }
}
