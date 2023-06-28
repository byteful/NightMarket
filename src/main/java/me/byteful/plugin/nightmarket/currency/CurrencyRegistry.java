package me.byteful.plugin.nightmarket.currency;

import me.byteful.plugin.nightmarket.NightMarketPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class CurrencyRegistry {
    private final Map<String, Currency> currencies = new HashMap<>();

    public CurrencyRegistry(NightMarketPlugin plugin) {
        for (String className : plugin.getConfig().getStringList("currencies")) {
            try {
                final Currency currency = (Currency) Class.forName(className).getDeclaredConstructor().newInstance();
                if (currency.canLoad()) {
                    currency.load();
                    register(currency);
                } else {
                    plugin.getLogger().warning("Failed to load currency: " + currency.getId());
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public Currency get(String id) {
        return currencies.get(id);
    }

    public void register(Currency currency) {
        currencies.put(currency.getId(), currency);
    }

    public void unregister(String id) {
        currencies.remove(id);
    }

    public Map<String, Currency> getCurrencies() {
        return currencies;
    }
}
