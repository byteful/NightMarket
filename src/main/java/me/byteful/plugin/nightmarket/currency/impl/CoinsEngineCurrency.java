package me.byteful.plugin.nightmarket.currency.impl;

import me.byteful.plugin.nightmarket.currency.Currency;
import me.byteful.plugin.nightmarket.currency.CurrencyRegistry;
import org.bukkit.Bukkit;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;

import java.util.UUID;

public class CoinsEngineCurrency implements Currency {
    private final su.nightexpress.coinsengine.api.currency.Currency adapter;

    public CoinsEngineCurrency(su.nightexpress.coinsengine.api.currency.Currency adapter) {
        this.adapter = adapter;
    }

    @Override
    public String getId() {
        return "coinsengine:" + adapter.getId();
    }

    @Override
    public void load() {

    }

    @Override
    public boolean canLoad() {
        return adapter != null && Bukkit.getPluginManager().isPluginEnabled("CoinsEngine");
    }

    @Override
    public boolean canPlayerAfford(UUID player, double price) {
        return CoinsEngineAPI.getBalance(player, adapter) >= price;
    }

    @Override
    public void withdraw(UUID player, double amount) {
        CoinsEngineAPI.removeBalance(player, adapter, amount);
    }

    @Override
    public String getName(double amount) {
        return adapter.format(amount);
    }

    public static class CoinsEngineCurrencyHandler {
        private final CurrencyRegistry registry;

        public CoinsEngineCurrencyHandler(CurrencyRegistry registry) {
            this.registry = registry;
        }

        public boolean registerAll() {
            if (!Bukkit.getPluginManager().isPluginEnabled("CoinsEngine")) return false;

            registry.getCurrencies().keySet().removeIf(id -> id.startsWith("coinsengine:"));
            boolean anyRegistered = false;
            for (su.nightexpress.coinsengine.api.currency.Currency adapter : CoinsEngineAPI.getCurrencies()) {
                registry.register(new CoinsEngineCurrency(adapter));
                anyRegistered = true;
            }

            return anyRegistered;
        }
    }
}
