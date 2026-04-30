package me.byteful.plugin.nightmarket.currency;

import java.util.HashMap;
import java.util.Map;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.currency.impl.CoinsEngineCurrency;
import me.byteful.plugin.nightmarket.currency.impl.EcoBitsCurrency;
import me.byteful.plugin.nightmarket.currency.impl.PlayerPointsCurrency;
import me.byteful.plugin.nightmarket.currency.impl.VaultCurrency;
import org.bukkit.Bukkit;

public class CurrencyRegistry {
    private final Map<String, Currency> currencies = new HashMap<>();
    private final NightMarketPlugin plugin;
    private boolean isLoaded = false;

    public CurrencyRegistry(NightMarketPlugin plugin) {
        this.plugin = plugin;
        plugin.getScheduler().runGlobalDelayed(this::load, 20L);
    }

    private void load() {
        this.plugin.getLogger().info("Loading NightMarket currency adapters...");
        // Let other external plugins know. We delay by a second loading these so other plugins get a chance to handle this and depend on NightMarket properly.
        Bukkit.getPluginManager().callEvent(new CurrencyRegisterEvent(this));

        this.register(new VaultCurrency(this.plugin));
        this.register(new PlayerPointsCurrency(this.plugin));
        // EcoBits has multiple possible currencies within it, so lets register all of them with the format: 'ecobits:<currency>'
        if (!new EcoBitsCurrency.EcoBitsCurrencyHandler(this).registerAll()) {
            this.plugin.getLogger().info("Skipped loading currency adapter: ecobits");
        }
        if (!new CoinsEngineCurrency.CoinsEngineCurrencyHandler(this).registerAll()) {
            this.plugin.getLogger().info("Skipped loading currency adapter: coinsengine");
        }

        this.isLoaded = true;
        this.plugin.getLogger().info("Done loading currencies!");

        this.plugin.getShopItemRegistry().load(); // Have to load items after currencies are loaded.
        this.plugin.getRotateScheduleManager().load();

        Bukkit.getOnlinePlayers().forEach(p -> this.plugin.getPlayerShopManager().load(p));
    }

    public void register(Currency currency) {
        if (currency.canLoad()) {
            currency.load();
            this.plugin.getLogger().info("Registered currency adapter: " + currency.getId());
        } else {
            this.plugin.getLogger().info("Skipped loading currency adapter: " + currency.getId());
        }

        this.currencies.put(currency.getId().toLowerCase(), currency);
    }

    public Currency get(String id) {
        return this.currencies.get(id.toLowerCase());
    }

    public void unregister(Currency currency) {
        this.unregister(currency.getId());
    }

    public void unregister(String id) {
        this.currencies.remove(id.toLowerCase());
    }

    public Map<String, Currency> getCurrencies() {
        return this.currencies;
    }

    public boolean isLoaded() {
        return this.isLoaded;
    }
}
