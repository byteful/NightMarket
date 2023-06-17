package me.byteful.plugin.nightmarket.datastore;

import me.byteful.plugin.nightmarket.shop.player.PlayerShop;
import me.byteful.plugin.nightmarket.util.SQLUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public abstract class SQLDataStoreProvider implements DataStoreProvider {
    protected final Connection connection;
    protected final String upsertClause;

    public SQLDataStoreProvider(Connection connection, String upsertClause) {
        this.connection = connection;
        this.upsertClause = upsertClause;
        createTable();
    }

    @Override
    public void setPlayerShop(PlayerShop shop) {
        final String sql = "INSERT INTO NightMarket (ID, Purchased, Items) VALUES (?, ?, ?) " + upsertClause + " Purchased=?, Items=?;";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, SQLUtils.serializeUUID(shop.getUniqueId()));
            statement.setString(2, SQLUtils.serializeList(new ArrayList<>(shop.getPurchasedShopItems())));
            statement.setString(3, SQLUtils.serializeList(shop.getShopItems()));
            statement.setString(4, SQLUtils.serializeList(new ArrayList<>(shop.getPurchasedShopItems())));
            statement.setString(5, SQLUtils.serializeList(shop.getShopItems()));

            statement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Optional<PlayerShop> getPlayerShop(UUID player) {
        final String sql = "SELECT * FROM NightMarket WHERE ID=?;";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, SQLUtils.serializeUUID(player));

            try (ResultSet set = statement.executeQuery()) {
                if (!set.next()) {
                    return Optional.empty();
                }
                final List<String> purchased = SQLUtils.deserializeList(set.getString("Purchased"));
                final List<String> items = SQLUtils.deserializeList(set.getString("Items"));

                return Optional.of(new PlayerShop(player, new HashSet<>(purchased), items));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Set<PlayerShop> getAllShops() {
        final Set<PlayerShop> set = new HashSet<>();
        final String sql = "SELECT * FROM NightMarket;";

        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                final UUID uuid = SQLUtils.deserializeUUID(result.getBytes("ID"));
                final List<String> purchased = SQLUtils.deserializeList(result.getString("Purchased"));
                final List<String> items = SQLUtils.deserializeList(result.getString("Items"));

                set.add(new PlayerShop(uuid, new HashSet<>(purchased), items));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return set;
    }

    @Override
    public boolean test() {
        try {
            return !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createTable() {
        final String sql = "CREATE TABLE IF NOT EXISTS NightMarket (ID BINARY(16) NOT NULL, Purchased TEXT NOT NULL, Items TEXT NOT NULL, PRIMARY KEY (ID));";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
