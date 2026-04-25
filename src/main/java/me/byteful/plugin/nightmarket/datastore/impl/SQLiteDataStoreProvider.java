package me.byteful.plugin.nightmarket.datastore.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.datastore.SQLConnectionProvider;
import me.byteful.plugin.nightmarket.datastore.SQLDataStoreProvider;
import me.byteful.plugin.nightmarket.util.dependency.IsolatedClassLoader;

public class SQLiteDataStoreProvider extends SQLDataStoreProvider {
    public SQLiteDataStoreProvider(IsolatedClassLoader loader, NightMarketPlugin plugin) {
        super(new SQLiteConnectionProvider(loader, plugin), "ON CONFLICT (ID) DO UPDATE SET", plugin.getLogger());
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

            return (Connection) connectionClass.getConstructor(String.class, String.class, Properties.class)
                .newInstance("jdbc:sqlite:" + path.toString(), path.toString(), properties);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class SQLiteConnectionProvider implements SQLConnectionProvider {
        private final IsolatedClassLoader loader;
        private final NightMarketPlugin plugin;
        private SQLiteConnection connection = null;

        private SQLiteConnectionProvider(IsolatedClassLoader loader, NightMarketPlugin plugin) {
            this.loader = loader;
            this.plugin = plugin;
            this.get();
        }

        @Override
        public Connection get() {
            try {
                if (this.connection == null || this.connection.isClosed()) {
                    final Connection realConnection = buildConnection(this.loader, this.plugin.getDataFolder().toPath().resolve("data.db"));
                    this.connection = new SQLiteConnection(realConnection);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to connect to SQLite database!", e);
            }

            return this.connection;
        }

        @Override
        public boolean isValid() {
            try {
                return !this.connection.isClosed() && this.connection.isValid(5);
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to validate SQLite connection.", e);
                return false;
            }
        }

        @Override
        public void close() throws IOException {
            try {
                this.connection.actuallyClose();
            } catch (SQLException e) {
                throw new IOException("Failed to close SQLite connection!", e);
            }
        }
    }

    private record SQLiteConnection(Connection handle) implements Connection {

        @Override
            public Statement createStatement() throws SQLException {
                return this.handle.createStatement();
            }

        @Override
            public void close() throws SQLException {
                // Do nothing
            }

            @Override
            public PreparedStatement prepareStatement(String sql) throws SQLException {
                return this.handle.prepareStatement(sql);
            }

            @Override
            public CallableStatement prepareCall(String sql) throws SQLException {
                return this.handle.prepareCall(sql);
            }

            @Override
            public String nativeSQL(String sql) throws SQLException {
                return this.handle.nativeSQL(sql);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                if (iface.isInstance(this.handle)) {
                    return (T) this.handle;
                }
                return this.handle.unwrap(iface);
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return iface.isInstance(this.handle) || this.handle.isWrapperFor(iface);
            }

            public void actuallyClose() throws SQLException {
                this.handle.close();
            }

        @Override
            public boolean getAutoCommit() throws SQLException {
                return this.handle.getAutoCommit();
            }

            @Override
            public void setAutoCommit(boolean autoCommit) throws SQLException {
                this.handle.setAutoCommit(autoCommit);
            }

            @Override
            public void commit() throws SQLException {
                this.handle.commit();
            }

            @Override
            public void rollback() throws SQLException {
                this.handle.rollback();
            }

            @Override
            public boolean isClosed() throws SQLException {
                return this.handle.isClosed();
            }

            @Override
            public DatabaseMetaData getMetaData() throws SQLException {
                return this.handle.getMetaData();
            }

            @Override
            public boolean isReadOnly() throws SQLException {
                return this.handle.isReadOnly();
            }

            @Override
            public void setReadOnly(boolean readOnly) throws SQLException {
                this.handle.setReadOnly(readOnly);
            }

            @Override
            public String getCatalog() throws SQLException {
                return this.handle.getCatalog();
            }

            @Override
            public void setCatalog(String catalog) throws SQLException {
                this.handle.setCatalog(catalog);
            }

            @Override
            public int getTransactionIsolation() throws SQLException {
                return this.handle.getTransactionIsolation();
            }

            @Override
            public void setTransactionIsolation(int level) throws SQLException {
                this.handle.setTransactionIsolation(level);
            }

            @Override
            public SQLWarning getWarnings() throws SQLException {
                return this.handle.getWarnings();
            }

            @Override
            public void clearWarnings() throws SQLException {
                this.handle.clearWarnings();
            }

            @Override
            public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
                return this.handle.createStatement(resultSetType, resultSetConcurrency);
            }

            @Override
            public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
                return this.handle.prepareStatement(sql, resultSetType, resultSetConcurrency);
            }

            @Override
            public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
                return this.handle.prepareCall(sql, resultSetType, resultSetConcurrency);
            }

            @Override
            public Map<String, Class<?>> getTypeMap() throws SQLException {
                return this.handle.getTypeMap();
            }

            @Override
            public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
                this.handle.setTypeMap(map);
            }

            @Override
            public int getHoldability() throws SQLException {
                return this.handle.getHoldability();
            }

            @Override
            public void setHoldability(int holdability) throws SQLException {
                this.handle.setHoldability(holdability);
            }

            @Override
            public Savepoint setSavepoint() throws SQLException {
                return this.handle.setSavepoint();
            }

            @Override
            public Savepoint setSavepoint(String name) throws SQLException {
                return this.handle.setSavepoint(name);
            }

            @Override
            public void rollback(Savepoint savepoint) throws SQLException {
                this.handle.rollback(savepoint);
            }

            @Override
            public void releaseSavepoint(Savepoint savepoint) throws SQLException {
                this.handle.releaseSavepoint(savepoint);
            }

            @Override
            public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
                return this.handle.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
            }

            @Override
            public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
                return this.handle.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
            }

            @Override
            public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
                return this.handle.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
            }

            @Override
            public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
                return this.handle.prepareStatement(sql, autoGeneratedKeys);
            }

            @Override
            public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
                return this.handle.prepareStatement(sql, columnIndexes);
            }

            @Override
            public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
                return this.handle.prepareStatement(sql, columnNames);
            }

            @Override
            public Clob createClob() throws SQLException {
                return this.handle.createClob();
            }

            @Override
            public Blob createBlob() throws SQLException {
                return this.handle.createBlob();
            }

            @Override
            public NClob createNClob() throws SQLException {
                return this.handle.createNClob();
            }

            @Override
            public SQLXML createSQLXML() throws SQLException {
                return this.handle.createSQLXML();
            }

            @Override
            public boolean isValid(int timeout) throws SQLException {
                return this.handle.isValid(timeout);
            }

            @Override
            public void setClientInfo(String name, String value) throws SQLClientInfoException {
                this.handle.setClientInfo(name, value);
            }

            @Override
            public String getClientInfo(String name) throws SQLException {
                return this.handle.getClientInfo(name);
            }

            @Override
            public Properties getClientInfo() throws SQLException {
                return this.handle.getClientInfo();
            }

            @Override
            public void setClientInfo(Properties properties) throws SQLClientInfoException {
                this.handle.setClientInfo(properties);
            }

            @Override
            public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
                return this.handle.createArrayOf(typeName, elements);
            }

            @Override
            public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
                return this.handle.createStruct(typeName, attributes);
            }

            @Override
            public String getSchema() throws SQLException {
                return this.handle.getSchema();
            }

            @Override
            public void setSchema(String schema) throws SQLException {
                this.handle.setSchema(schema);
            }

            @Override
            public void abort(Executor executor) throws SQLException {
                this.handle.abort(executor);
            }

            @Override
            public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
                this.handle.setNetworkTimeout(executor, milliseconds);
            }

            @Override
            public int getNetworkTimeout() throws SQLException {
                return this.handle.getNetworkTimeout();
            }

    }
}
