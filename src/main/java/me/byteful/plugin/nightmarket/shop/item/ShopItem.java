package me.byteful.plugin.nightmarket.shop.item;

import java.util.List;
import java.util.Objects;
import me.byteful.plugin.nightmarket.currency.Currency;
import me.byteful.plugin.nightmarket.parser.IconData;

public record ShopItem(String id, IconData icon, List<String> commands, Currency currency, double amount, double rarity, int purchaseLimit,
                       String permission, ConfirmPurchaseMode confirmPurchaseMode) {

    public boolean hasPermissionRequirement() {
        return this.permission != null && !this.permission.trim().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ShopItem shopItem = (ShopItem) o;
        return Double.compare(shopItem.amount, this.amount) == 0 && Double.compare(shopItem.rarity,
            this.rarity) == 0 && this.purchaseLimit == shopItem.purchaseLimit && Objects.equals(
            this.id, shopItem.id) && Objects.equals(this.icon, shopItem.icon) && Objects.equals(
            this.commands, shopItem.commands) && Objects.equals(this.currency, shopItem.currency) && Objects.equals(
            this.permission, shopItem.permission) && this.confirmPurchaseMode == shopItem.confirmPurchaseMode;
    }

    @Override
    public String toString() {
        return "ShopItem{" + "id='" + this.id + '\'' + ", icon=" + this.icon + ", command='" + this.commands + '\'' + ", currency=" + this.currency + ", amount=" + this.amount + ", rarity=" + this.rarity + ", purchaseLimit=" + this.purchaseLimit + ", permission='" + this.permission + '\'' + ", confirmPurchaseMode=" + this.confirmPurchaseMode + '}';
    }
}
