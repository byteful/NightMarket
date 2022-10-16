package me.byteful.plugin.nightmarket.datastore.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.datastore.DataStoreProvider;
import me.byteful.plugin.nightmarket.shop.player.PlayerShop;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class JSONDataStoreProvider implements DataStoreProvider {
  private final JsonObject data;
  private final File file;

  public JSONDataStoreProvider(NightMarketPlugin plugin) {
    this.file = new File(plugin.getDataFolder(), "data.json");
    if (!this.file.exists()) {
      try {
        this.file.createNewFile();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      this.data = new JsonObject();
    } else {
      try {
        this.data = DataStoreProvider.GSON.fromJson(new String(Files.readAllBytes(this.file.toPath()), StandardCharsets.UTF_8), JsonObject.class);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    if (!this.data.has("shops")) {
      this.data.add("shops", new JsonObject());
    }
  }

  @Override
  public void setPlayerShop(PlayerShop shop) {
    data.get("shops").getAsJsonObject().add(shop.getUniqueId().toString(), DataStoreProvider.GSON.toJsonTree(shop));
    save();
  }

  @Override
  public Optional<PlayerShop> getPlayerShop(UUID player) {
    final JsonElement shop = data.get("shops").getAsJsonObject().get(player.toString());
    if (shop == null) {
      return Optional.empty();
    }

    return Optional.of(DataStoreProvider.GSON.fromJson(shop, PlayerShop.class));
  }

  @Override
  public Set<PlayerShop> getAllShops() {
    final Set<PlayerShop> set = new HashSet<>();

    for (Map.Entry<String, JsonElement> entry : data.get("shops").getAsJsonObject().entrySet()) {
      set.add(DataStoreProvider.GSON.fromJson(entry.getValue(), PlayerShop.class));
    }

    return set;
  }

  private void save() {
    try {
      Files.write(file.toPath(), DataStoreProvider.GSON.toJson(data).getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {
    save();
  }
}