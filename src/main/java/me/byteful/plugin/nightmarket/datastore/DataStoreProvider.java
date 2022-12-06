package me.byteful.plugin.nightmarket.datastore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.byteful.plugin.nightmarket.shop.player.PlayerShop;

import java.io.Closeable;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface DataStoreProvider extends Closeable {
  Gson GSON = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();

  void setPlayerShop(PlayerShop shop);

  Optional<PlayerShop> getPlayerShop(UUID player);

  Set<PlayerShop> getAllShops();

  boolean test();
}
