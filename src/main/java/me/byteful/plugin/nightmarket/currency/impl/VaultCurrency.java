package me.byteful.plugin.nightmarket.currency.impl;

import me.byteful.plugin.nightmarket.currency.Currency;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

public class VaultCurrency implements Currency {
    private transient Economy eco;

    @Override
    public String getId() {
        return "vault";
    }

    @Override
    public void load() {
        final RegisteredServiceProvider<Economy> ecoP = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (ecoP == null) {
            throw new RuntimeException("Failed to find Vault binding for Economy.");
        }

        this.eco = ecoP.getProvider();
    }

    @Override
    public boolean canLoad() {
        return Bukkit.getPluginManager().isPluginEnabled("Vault");
    }

    @Override
    public boolean canPlayerAfford(UUID player, double price) {
        return eco.has(Bukkit.getOfflinePlayer(player), Math.abs(price));
    }

    @Override
    public void withdraw(UUID player, double amount) {
        eco.withdrawPlayer(Bukkit.getOfflinePlayer(player), Math.abs(amount));
    }
}
