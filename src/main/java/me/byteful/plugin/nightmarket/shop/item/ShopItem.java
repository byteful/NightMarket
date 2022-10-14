package me.byteful.plugin.nightmarket.shop.item;

import me.byteful.plugin.nightmarket.currency.Currency;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class ShopItem {
  private final String id;
  private final ItemStack icon;
  private final String command;
  private final Currency currency;
  private final double amount;
  private final boolean isMultiplePurchase;

  public ShopItem(String id, ItemStack icon, String command, Currency currency, double amount, boolean isMultiplePurchase) {
    this.id = id;
    this.icon = icon;
    this.command = command;
    this.currency = currency;
    this.amount = amount;
    this.isMultiplePurchase = isMultiplePurchase;
  }

  public String getId() {
    return id;
  }

  public ItemStack getIcon() {
    return icon;
  }

  public String getCommand() {
    return command;
  }

  public Currency getCurrency() {
    return currency;
  }

  public double getAmount() {
    return amount;
  }

  public boolean isMultiplePurchase() {
    return isMultiplePurchase;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ShopItem shopItem = (ShopItem) o;
    return Double.compare(shopItem.amount, amount) == 0 && isMultiplePurchase == shopItem.isMultiplePurchase && Objects.equals(id, shopItem.id) && Objects.equals(icon, shopItem.icon) && Objects.equals(command, shopItem.command) && Objects.equals(currency, shopItem.currency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, icon, command, currency, amount, isMultiplePurchase);
  }

  @Override
  public String toString() {
    return "ShopItem{" +
      "id='" + id + '\'' +
      ", icon=" + icon +
      ", command='" + command + '\'' +
      ", currency=" + currency +
      ", amount=" + amount +
      ", isMultiplePurchase=" + isMultiplePurchase +
      '}';
  }
}