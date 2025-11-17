package me.byteful.plugin.nightmarket.currency.impl;

import com.willfp.ecobits.currencies.Currencies;
import com.willfp.ecobits.currencies.CurrencyUtils;
import me.byteful.plugin.nightmarket.currency.Currency;
import me.byteful.plugin.nightmarket.currency.CurrencyRegistry;
import me.byteful.plugin.nightmarket.util.Text;
import org.bukkit.Bukkit;

import java.math.BigDecimal;
import java.util.UUID;

public class EcoBitsCurrency implements Currency {
    private final com.willfp.ecobits.currencies.Currency adapter;

    public EcoBitsCurrency(com.willfp.ecobits.currencies.Currency adapter) {
        this.adapter = adapter;
    }

    @Override
    public String getId() {
        return "ecobits:" + adapter.getId();
    }

    @Override
    public void load() {

    }

    @Override
    public boolean canLoad() {
        return adapter != null && Bukkit.getPluginManager().isPluginEnabled("EcoBits");
    }

    @Override
    public boolean canPlayerAfford(UUID player, double price) {
        return CurrencyUtils.getBalance(Bukkit.getOfflinePlayer(player), adapter).doubleValue() >= price;
    }

    @Override
    public void withdraw(UUID player, double amount) {
        CurrencyUtils.adjustBalance(Bukkit.getOfflinePlayer(player), adapter, BigDecimal.valueOf(-amount));
    }

    @Override
    public String getName(double amount) {
        return Text.formatCurrency(amount) + " " + adapter.getName();
    }

    public static class EcoBitsCurrencyHandler {
        private final CurrencyRegistry registry;

        public EcoBitsCurrencyHandler(CurrencyRegistry registry) {
            this.registry = registry;
        }

        public boolean registerAll() {
            if (!Bukkit.getPluginManager().isPluginEnabled("EcoBits")) return false;

            registry.getCurrencies().keySet().removeIf(id -> id.startsWith("ecobits:"));
            boolean anyRegistered = false;
            for (com.willfp.ecobits.currencies.Currency adapter : Currencies.values()) {
                registry.register(new EcoBitsCurrency(adapter));
                anyRegistered = true;
            }

            return anyRegistered;
        }
    }
}
