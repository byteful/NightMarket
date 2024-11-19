package me.byteful.plugin.nightmarket.shop.item;

import me.byteful.plugin.nightmarket.currency.Currency;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

public class ShopItem {
    private final String id;
    private final ItemStack icon;
    private final List<String> commands;
    private final Currency currency;
    private final double amount;
    private final double rarity;
    private final int purchaseLimit;

    public ShopItem(String id, ItemStack icon, List<String> commands, Currency currency, double amount, double rarity, int purchaseLimit) {
        this.id = id;
        this.icon = icon;
        this.commands = commands;
        this.currency = currency;
        this.amount = amount;
        this.rarity = rarity;
        this.purchaseLimit = purchaseLimit;
    }

    public String getId() {
        return id;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public List<String> getCommands() {
        return commands;
    }

    public Currency getCurrency() {
        return currency;
    }

    public double getAmount() {
        return amount;
    }

    public int getPurchaseLimit() {
        return purchaseLimit;
    }

    public double getRarity() {
        return rarity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShopItem shopItem = (ShopItem) o;
        return Double.compare(shopItem.amount, amount) == 0 && Double.compare(shopItem.rarity, rarity) == 0 && purchaseLimit == shopItem.purchaseLimit && Objects.equals(id, shopItem.id) && Objects.equals(icon, shopItem.icon) && Objects.equals(commands, shopItem.commands) && Objects.equals(currency, shopItem.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, icon, commands, currency, amount, rarity, purchaseLimit);
    }

    @Override
    public String toString() {
        return "ShopItem{" + "id='" + id + '\'' + ", icon=" + icon + ", command='" + commands + '\'' + ", currency=" + currency + ", amount=" + amount + ", rarity=" + rarity + ", isMultiplePurchase=" + purchaseLimit + '}';
    }
}
