package me.byteful.plugin.nightmarket.currency.impl;

import me.byteful.plugin.nightmarket.currency.Currency;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;

import java.util.UUID;

public class PlayerPointsCurrency implements Currency {
    private transient PlayerPointsAPI eco;

    @Override
    public String getId() {
        return "playerpoints";
    }

    @Override
    public void load() {
        this.eco = PlayerPoints.getInstance().getAPI();
    }

    @Override
    public boolean canLoad() {
        return Bukkit.getPluginManager().isPluginEnabled("PlayerPoints");
    }

    @Override
    public boolean canPlayerAfford(UUID player, double price) {
        return eco.look(player) >= price;
    }

    @Override
    public void withdraw(UUID player, double amount) {
        eco.take(player, (int) amount);
    }

    @Override
    public String getName(double amount) {
        return amount == 1 ? "Token" : "Tokens";
    }
}
