package me.byteful.plugin.nightmarket.currency;

import java.util.UUID;

public interface Currency {
    /**
     * Gets the currency's unique ID that is used in the config.yml file.
     *
     * @return unique id for configuration uses
     */
    String getId();

    /**
     * Runs any code if stuff is required to be loaded before other methods can be used.
     */
    void load();

    /**
     * Checks if this currency has all requirements met and is able to be loaded.
     *
     * @return true if this currency is able to be loaded
     */
    boolean canLoad();

    /**
     * Runs code with external currency API to see if this player can afford the provided price.
     *
     * @param player the player's unique ID
     * @param price  the decimal price
     * @return true if this player can afford the price
     */
    boolean canPlayerAfford(UUID player, double price);

    /**
     * Takes the provided amount/price from the player's balance in this currency.
     *
     * @param player the player's unique ID
     * @param amount the decimal price
     */
    void withdraw(UUID player, double amount);

    /**
     * Returns a singular/plural name depending on the currency amount.
     * Ex: [amount] dollar(s)
     *
     * @param amount the amount of this currency
     * @return the formatted currency name
     */
    String getName(double amount);
}
