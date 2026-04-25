package me.byteful.plugin.nightmarket.currency.impl;

import java.util.UUID;
import me.byteful.plugin.nightmarket.currency.Currency;
import me.byteful.plugin.nightmarket.currency.CurrencyRegistry;
import org.bukkit.Bukkit;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;

public class CoinsEngineCurrency implements Currency {
    private final su.nightexpress.coinsengine.api.currency.Currency adapter;

    public CoinsEngineCurrency(su.nightexpress.coinsengine.api.currency.Currency adapter) {
        this.adapter = adapter;
    }

    @Override
    public String getId() {
        return "coinsengine:" + this.adapter.getId();
    }

    @Override
    public void load() {

    }

    @Override
    public boolean canLoad() {
        return this.adapter != null && Bukkit.getPluginManager().isPluginEnabled("CoinsEngine");
    }

    @Override
    public boolean canPlayerAfford(UUID player, double price) {
        return CoinsEngineAPI.getBalance(player, this.adapter) >= price;
    }

    @Override
    public void withdraw(UUID player, double amount) {
        CoinsEngineAPI.removeBalance(player, this.adapter, amount);
    }

    @Override
    public String getName(double amount) {
        return this.adapter.format(amount);
    }

    public static class CoinsEngineCurrencyHandler {
        private final CurrencyRegistry registry;

        public CoinsEngineCurrencyHandler(CurrencyRegistry registry) {
            this.registry = registry;
        }

        public boolean registerAll() {
            if (!Bukkit.getPluginManager().isPluginEnabled("CoinsEngine")) {
                return false;
            }

            this.registry.getCurrencies().keySet().removeIf(id -> id.startsWith("coinsengine:"));
            boolean anyRegistered = false;
            for (su.nightexpress.coinsengine.api.currency.Currency adapter : CoinsEngineAPI.getCurrencies()) {
                this.registry.register(new CoinsEngineCurrency(adapter));
                anyRegistered = true;
            }

            return anyRegistered;
        }
    }
}
