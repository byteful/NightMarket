package me.byteful.plugin.nightmarket.datastore.impl;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.datastore.DataStoreProvider;
import me.byteful.plugin.nightmarket.shop.player.PlayerShop;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class MongoDataStoreProvider implements DataStoreProvider {
    private final MongoClient client;
    private final MongoDatabase db;

    public MongoDataStoreProvider(NightMarketPlugin plugin) {
        this.client = new MongoClient(new MongoClientURI(plugin.getConfig().getString("datastore.mongo.uri")));
        this.db = this.client.getDatabase(plugin.getConfig().getString("datastore.mongo.database"));
    }

    @Override
    public void setPlayerShop(PlayerShop shop) {
        final MongoCollection<Document> col = getCollection();
        final Bson eq = Filters.eq("_id", shop.getUniqueId().toString());
        final Document doc = Document.parse(DataStoreProvider.GSON.toJson(shop));
        if (col.find(eq).cursor().hasNext()) {
            col.replaceOne(eq, doc);
        } else {
            col.insertOne(doc);
        }
    }

    @Override
    public Optional<PlayerShop> getPlayerShop(UUID player) {
        final MongoCollection<Document> col = getCollection();
        final Bson eq = Filters.eq("_id", player.toString());
        final Document doc = col.find(eq).first();
        if (doc == null) {
            return Optional.empty();
        }

        return Optional.of(DataStoreProvider.GSON.fromJson(doc.toJson(JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build()), PlayerShop.class));
    }

    @Override
    public Set<PlayerShop> getAllShops() {
        final Set<PlayerShop> set = new HashSet<>();

        getCollection().find().forEach((Consumer<? super Document>) doc -> set.add(DataStoreProvider.GSON.fromJson(doc.toJson(JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build()), PlayerShop.class)));

        return set;
    }

    @Override
    public boolean test() {
        try {
            getCollection().find(Filters.eq("test", "test")).first();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private MongoCollection<Document> getCollection() {
        return db.getCollection("NightMarket");
    }

    @Override
    public void close() {
        client.close();
    }
}
