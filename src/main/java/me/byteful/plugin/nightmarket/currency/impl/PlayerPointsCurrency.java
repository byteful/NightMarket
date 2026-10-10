package me.byteful.plugin.nightmarket.currency.impl;

import java.util.UUID;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.currency.Currency;
import me.byteful.plugin.nightmarket.util.Text;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;

public class PlayerPointsCurrency implements Currency {
    private final NightMarketPlugin plugin;
    private PlayerPointsAPI eco;

    public PlayerPointsCurrency(NightMarketPlugin plugin) {
        this.plugin = plugin;
    }

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
        return this.eco.look(player) >= toPoints(price);
    }

    @Override
    public void withdraw(UUID player, double amount) {
        this.tryWithdraw(player, amount);
    }

    @Override
    public boolean tryWithdraw(UUID player, double amount) {
        return this.eco.take(player, toPoints(amount));
    }

    // PlayerPoints balances are whole numbers. Round fractional prices up so they are never undercharged,
    // and use the same value for the afford check and the displayed price.
    private static int toPoints(double amount) {
        return (int) Math.ceil(Math.abs(amount) - 1.0E-9);
    }

    @Override
    public String getName(double amount) {
        final int points = toPoints(amount);
        return Text.formatCurrency(points) + " " + (points == 1 ? this.plugin.getConfig()
                                                                  .getString("default_currencies.playerpoints.name.singular", "Token") : this.plugin.getConfig()
                                                                                                                                         .getString(
                                                                                                                                             "default_currencies.playerpoints.name.plural",
                                                                                                                                             "Tokens"));
    }
}
