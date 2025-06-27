package me.byteful.plugin.nightmarket.datastore.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.datastore.SQLConnectionProvider;
import me.byteful.plugin.nightmarket.datastore.SQLDataStoreProvider;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQLDataStoreProvider extends SQLDataStoreProvider {
    public MySQLDataStoreProvider(NightMarketPlugin plugin) {
        super(buildConnectionProvider(plugin.getConfig().getString("datastore.mysql.host"), plugin.getConfig().getInt("datastore.mysql.port"), plugin.getConfig().getString("datastore.mysql.user"), plugin.getConfig().getString("datastore.mysql.password"), plugin.getConfig().getString("datastore.mysql.database")), "ON DUPLICATE KEY UPDATE");
    }

    private static MySQLConnectionProvider buildConnectionProvider(String host, int port, String user, String pass, String database) {
        return new MySQLConnectionProvider(String.format("jdbc:mysql://%s:%s/%s", host, port, database), user, pass);
    }

    private static final class MySQLConnectionProvider implements SQLConnectionProvider {
        private final HikariDataSource hikari;

        private MySQLConnectionProvider(String jdbcURL, String user, String pass) {
            final HikariConfig config = new HikariConfig();
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl(jdbcURL);
            config.setUsername(user);
            config.setPassword(pass);

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            config.addDataSourceProperty("alwaysSendSetIsolation", "false");
            config.addDataSourceProperty("cacheCallableStmts", "true");

            this.hikari = new HikariDataSource(config);
        }

        @Override
        public boolean isValid() {
            return hikari.isRunning();
        }

        @Override
        public void close() {
            hikari.close();
        }

        @Override
        public Connection get() {
            try {
                return hikari.getConnection();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
