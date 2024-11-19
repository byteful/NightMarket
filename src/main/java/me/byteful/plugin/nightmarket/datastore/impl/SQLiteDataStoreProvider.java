package me.byteful.plugin.nightmarket.datastore.impl;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.datastore.SQLConnectionProvider;
import me.byteful.plugin.nightmarket.datastore.SQLDataStoreProvider;
import me.byteful.plugin.nightmarket.util.dependency.IsolatedClassLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class SQLiteDataStoreProvider extends SQLDataStoreProvider {
    public SQLiteDataStoreProvider(IsolatedClassLoader loader, NightMarketPlugin plugin) {
        super(new SQLiteConnectionProvider(buildConnection(loader, plugin.getDataFolder().toPath().resolve("data.db"))), "ON CONFLICT (ID) DO UPDATE SET");
    }

    private static Connection buildConnection(IsolatedClassLoader loader, Path path) {
        if (loader == null) {
            // We are on a newer version where Spigot loads the library for us.
            try {
                Class.forName("org.sqlite.JDBC");

                final Properties properties = new Properties();
                properties.setProperty("foreign_keys", "on");
                properties.setProperty("busy_timeout", "1000");

                return DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath(), properties);
            } catch (SQLException | ClassNotFoundException e) {
                throw new RuntimeException("Failed to connect to SQLite!", e);
            }
        }

        try {
            final Class<?> connectionClass = loader.loadClass("org.sqlite.jdbc4.JDBC4Connection");
            final Properties properties = new Properties();
            properties.setProperty("foreign_keys", "on");
            properties.setProperty("busy_timeout", "1000");

            return (Connection) connectionClass.getConstructor(String.class, String.class, Properties.class).newInstance("jdbc:sqlite:" + path.toString(), path.toString(), properties);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class SQLiteConnectionProvider implements SQLConnectionProvider {
        private final Connection connection;

        private SQLiteConnectionProvider(Connection connection) {
            this.connection = connection;
        }

        @Override
        public boolean isValid() {
            try {
                return !connection.isClosed() && connection.isValid(5);
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public void close() throws IOException {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new IOException("Failed to close SQLite connection!", e);
            }
        }

        @Override
        public Connection get() {
            return connection;
        }
    }
}
