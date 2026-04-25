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
        return this.eco.look(player) >= price;
    }

    @Override
    public void withdraw(UUID player, double amount) {
        this.eco.take(player, (int) amount);
    }

    @Override
    public String getName(double amount) {
        return Text.formatCurrency(amount) + " " + (amount == 1 ? this.plugin.getConfig()
                                                                  .getString("default_currencies.playerpoints.name.singular", "Token") : this.plugin.getConfig()
                                                                                                                                         .getString(
                                                                                                                                             "default_currencies.playerpoints.name.plural",
                                                                                                                                             "Tokens"));
    }
}
