package me.byteful.plugin.nightmarket.currency.impl;

import me.byteful.plugin.nightmarket.currency.Currency;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;

import java.util.Objects;
import java.util.UUID;

public class VaultCurrency implements Currency {
  private transient Economy eco;

  @Override
  public String getId() {
    return "vault";
  }

  @Override
  public void load() {
    this.eco = Objects.requireNonNull(Bukkit.getServicesManager().getRegistration(Economy.class), "Failed to find a valid Vault currency adapter.").getProvider();
  }

  @Override
  public boolean canLoad() {
    return Bukkit.getPluginManager().isPluginEnabled("Vault") && Bukkit.getServicesManager().getRegistration(Economy.class) != null;
  }

  @Override
  public boolean canPlayerAfford(UUID player, double price) {
    return eco.has(Bukkit.getOfflinePlayer(player), Math.abs(price));
  }

  @Override
  public void withdraw(UUID player, double amount) {
    eco.withdrawPlayer(Bukkit.getOfflinePlayer(player), Math.abs(amount));
  }

  @Override
  public String getName(double amount) {
    return amount == 1 ? "Dollar" : "Dollars";
  }
}
