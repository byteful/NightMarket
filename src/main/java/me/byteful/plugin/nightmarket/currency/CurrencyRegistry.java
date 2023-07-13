package me.byteful.plugin.nightmarket.currency;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.currency.impl.PlayerPointsCurrency;
import me.byteful.plugin.nightmarket.currency.impl.VaultCurrency;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

public class CurrencyRegistry {
    private final Map<String, Currency> currencies = new HashMap<>();
    private final NightMarketPlugin plugin;
    private boolean isLoaded = false;

    public CurrencyRegistry(NightMarketPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskLater(plugin, this::load, 20L);
    }

    private void load() {
        plugin.getLogger().info("Loading NightMarket currency adapters...");
        // Let other external plugins know. We delay by a second loading these so other plugins get a chance to handle this and depend on NightMarket properly.
        Bukkit.getPluginManager().callEvent(new CurrencyRegisterEvent(this));

        register(new VaultCurrency());
        register(new PlayerPointsCurrency());

        isLoaded = true;
        plugin.getLogger().info("Done loading currencies!");

        plugin.getShopItemRegistry().load(); // Have to load items after currencies are loaded.
    }

    public Currency get(String id) {
        return currencies.get(id.toLowerCase());
    }

    public void register(Currency currency) {
        if (currency.canLoad()) {
            currency.load();
            plugin.getLogger().info("Registered currency adapter: " + currency.getId());
        } else {
            plugin.getLogger().warning("Skipped loading currency adapter: " + currency.getId());
        }

        currencies.put(currency.getId().toLowerCase(), currency);
    }

    public void unregister(Currency currency) {
        unregister(currency.getId());
    }

    public void unregister(String id) {
        currencies.remove(id.toLowerCase());
    }

    public Map<String, Currency> getCurrencies() {
        return currencies;
    }

    public boolean isLoaded() {
        return isLoaded;
    }
}
