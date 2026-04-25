package me.byteful.plugin.nightmarket.currency;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CurrencyRegisterEvent extends Event {
    private final CurrencyRegistry registry;
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public CurrencyRegisterEvent(CurrencyRegistry registry) {
        this.registry = registry;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public void register(Currency currency) {
        this.getRegistry().register(currency);
    }

    public CurrencyRegistry getRegistry() {
        return this.registry;
    }
}
