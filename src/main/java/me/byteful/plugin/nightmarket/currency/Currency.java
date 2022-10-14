package me.byteful.plugin.nightmarket.currency;

import java.util.UUID;

public interface Currency {
  String getId();

  void load();

  boolean canLoad();

  boolean canPlayerAfford(UUID player, double price);

  void withdraw(UUID player, double amount);
}