package me.byteful.plugin.nightmarket.datastore;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.byteful.plugin.nightmarket.shop.player.PlayerShop;
import me.byteful.plugin.nightmarket.util.SQLUtils;

public abstract class SQLDataStoreProvider implements DataStoreProvider {
    protected final SQLConnectionProvider connectionProvider;
    protected final String upsertClause;
    protected final Logger logger;

    public SQLDataStoreProvider(SQLConnectionProvider connectionProvider, String upsertClause, Logger logger) {
        this.connectionProvider = connectionProvider;
        this.upsertClause = upsertClause;
        this.logger = logger;
        this.createTable();
    }

    private void createTable() {
        final String sql = "CREATE TABLE IF NOT EXISTS NightMarket (ID BINARY(16) NOT NULL, Purchased TEXT NOT NULL, Items TEXT NOT NULL, PRIMARY KEY (ID));";

        try (final Connection connection = this.connectionProvider.get(); final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException ex) {
            this.logger.log(Level.SEVERE, "Failed to create NightMarket table.", ex);
        }
    }

    @Override
    public void setPlayerShop(PlayerShop shop) {
        final String sql = "INSERT INTO NightMarket (ID, Purchased, Items) VALUES (?, ?, ?) " + this.upsertClause + " Purchased=?, Items=?;";

        try (final Connection connection = this.connectionProvider.get(); final PreparedStatement statement = connection.prepareStatement(sql)) {

            final List<String> purchased = shop.getSerializedPurchasedShopItems();
            final List<String> items = shop.getShopItems();
            final String purchasedSerialized = SQLUtils.serializeList(purchased);
            final String itemsSerialized = SQLUtils.serializeList(items);

            statement.setBytes(1, SQLUtils.serializeUUID(shop.getUniqueId()));
            statement.setString(2, purchasedSerialized);
            statement.setString(3, itemsSerialized);
            statement.setString(4, purchasedSerialized);
            statement.setString(5, itemsSerialized);

            statement.execute();
        } catch (SQLException ex) {
            this.logger.log(Level.SEVERE, "Failed to set player shop data.", ex);
        }
    }

    @Override
    public Optional<PlayerShop> getPlayerShop(UUID player) {
        final String sql = "SELECT * FROM NightMarket WHERE ID=?;";

        try (final Connection connection = this.connectionProvider.get(); final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, SQLUtils.serializeUUID(player));

            try (ResultSet set = statement.executeQuery()) {
                if (!set.next()) {
                    return Optional.empty();
                }
                final List<String> purchased = SQLUtils.deserializeList(set.getString("Purchased"));
                final List<String> items = SQLUtils.deserializeList(set.getString("Items"));

                return Optional.of(new PlayerShop(player, purchased, items));
            }
        } catch (SQLException ex) {
            this.logger.log(Level.SEVERE, "Failed to get player shop data.", ex);
        }

        return Optional.empty();
    }

    @Override
    public Set<PlayerShop> getAllShops() {
        final Set<PlayerShop> set = new HashSet<>();
        final String sql = "SELECT * FROM NightMarket;";

        try (final Connection connection = this.connectionProvider.get(); final PreparedStatement statement = connection.prepareStatement(
            sql); final ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                final UUID uuid = SQLUtils.deserializeUUID(result.getBytes("ID"));
                final List<String> purchased = SQLUtils.deserializeList(result.getString("Purchased"));
                final List<String> items = SQLUtils.deserializeList(result.getString("Items"));

                set.add(new PlayerShop(uuid, purchased, items));
            }
        } catch (SQLException ex) {
            this.logger.log(Level.SEVERE, "Failed to get all shops.", ex);
        }

        return set;
    }

    @Override
    public boolean test() {
        return this.connectionProvider.isValid();
    }

    @Override
    public void close() throws IOException {
        this.connectionProvider.close();
    }
}
