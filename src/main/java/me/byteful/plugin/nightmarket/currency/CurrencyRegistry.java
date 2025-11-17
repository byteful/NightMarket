package me.byteful.plugin.nightmarket.currency;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.currency.impl.CoinsEngineCurrency;
import me.byteful.plugin.nightmarket.currency.impl.EcoBitsCurrency;
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

        register(new VaultCurrency(plugin));
        register(new PlayerPointsCurrency(plugin));
        // EcoBits has multiple possible currencies within it, so lets register all of them with the format: 'ecobits:<currency>'
        if (!new EcoBitsCurrency.EcoBitsCurrencyHandler(this).registerAll()) {
            plugin.getLogger().info("Skipped loading currency adapter: ecobits");
        }
        if (!new CoinsEngineCurrency.CoinsEngineCurrencyHandler(this).registerAll()) {
            plugin.getLogger().info("Skipped loading currency adapter: coinsengine");
        }

        isLoaded = true;
        plugin.getLogger().info("Done loading currencies!");

        plugin.getShopItemRegistry().load(); // Have to load items after currencies are loaded.
        plugin.getRotateScheduleManager().load();

        Bukkit.getOnlinePlayers().forEach(p -> plugin.getPlayerShopManager().load(p.getUniqueId()));
    }

    public Currency get(String id) {
        return currencies.get(id.toLowerCase());
    }

    public void register(Currency currency) {
        if (currency.canLoad()) {
            currency.load();
            plugin.getLogger().info("Registered currency adapter: " + currency.getId());
        } else {
            plugin.getLogger().info("Skipped loading currency adapter: " + currency.getId());
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
