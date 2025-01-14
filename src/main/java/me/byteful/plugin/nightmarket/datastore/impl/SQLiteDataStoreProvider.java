package me.byteful.plugin.nightmarket.datastore.impl;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.datastore.SQLConnectionProvider;
import me.byteful.plugin.nightmarket.datastore.SQLDataStoreProvider;
import me.byteful.plugin.nightmarket.util.dependency.IsolatedClassLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class SQLiteDataStoreProvider extends SQLDataStoreProvider {
    public SQLiteDataStoreProvider(IsolatedClassLoader loader, NightMarketPlugin plugin) {
        super(new SQLiteConnectionProvider(loader, plugin), "ON CONFLICT (ID) DO UPDATE SET");
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
        private final IsolatedClassLoader loader;
        private final NightMarketPlugin plugin;
        private SQLiteConnection connection = null;

        private SQLiteConnectionProvider(IsolatedClassLoader loader, NightMarketPlugin plugin) {
            this.loader = loader;
            this.plugin = plugin;
            get();
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
                connection.actuallyClose();
            } catch (SQLException e) {
                throw new IOException("Failed to close SQLite connection!", e);
            }
        }

        @Override
        public Connection get() {
            try {
                if (connection == null || connection.isClosed()) {
                    final Connection realConnection = buildConnection(loader, plugin.getDataFolder().toPath().resolve("data.db"));
                    connection = new SQLiteConnection(realConnection);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to connect to SQLite database!", e);
            }

            return connection;
        }
    }

    private static final class SQLiteConnection implements Connection {
        private final Connection handle;

        private SQLiteConnection(Connection handle) {
            this.handle = handle;
        }

        @Override
        public void close() throws SQLException {
            // Do nothing
        }

        public void actuallyClose() throws SQLException {
            handle.close();
        }

        @Override
        public Statement createStatement() throws SQLException {
            return handle.createStatement();
        }

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return handle.prepareStatement(sql);
        }

        @Override
        public CallableStatement prepareCall(String sql) throws SQLException {
            return handle.prepareCall(sql);
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return handle.nativeSQL(sql);
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return handle.getAutoCommit();
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            handle.setAutoCommit(autoCommit);
        }

        @Override
        public void commit() throws SQLException {
            handle.commit();
        }

        @Override
        public void rollback() throws SQLException {
            handle.rollback();
        }

        @Override
        public boolean isClosed() throws SQLException {
            return handle.isClosed();
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return handle.getMetaData();
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return handle.isReadOnly();
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            handle.setReadOnly(readOnly);
        }

        @Override
        public String getCatalog() throws SQLException {
            return handle.getCatalog();
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
            handle.setCatalog(catalog);
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return handle.getTransactionIsolation();
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
            handle.setTransactionIsolation(level);
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return handle.getWarnings();
        }

        @Override
        public void clearWarnings() throws SQLException {
            handle.clearWarnings();
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return handle.createStatement(resultSetType, resultSetConcurrency);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return handle.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return handle.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return handle.getTypeMap();
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            handle.setTypeMap(map);
        }

        @Override
        public int getHoldability() throws SQLException {
            return handle.getHoldability();
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
            handle.setHoldability(holdability);
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return handle.setSavepoint();
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            return handle.setSavepoint(name);
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException {
            handle.rollback(savepoint);
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
            handle.releaseSavepoint(savepoint);
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return handle.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return handle.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return handle.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return handle.prepareStatement(sql, autoGeneratedKeys);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return handle.prepareStatement(sql, columnIndexes);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return handle.prepareStatement(sql, columnNames);
        }

        @Override
        public Clob createClob() throws SQLException {
            return handle.createClob();
        }

        @Override
        public Blob createBlob() throws SQLException {
            return handle.createBlob();
        }

        @Override
        public NClob createNClob() throws SQLException {
            return handle.createNClob();
        }

        @Override
        public SQLXML createSQLXML() throws SQLException {
            return handle.createSQLXML();
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            return handle.isValid(timeout);
        }

        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException {
            handle.setClientInfo(name, value);
        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return handle.getClientInfo(name);
        }

        @Override
        public Properties getClientInfo() throws SQLException {
            return handle.getClientInfo();
        }

        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException {
            handle.setClientInfo(properties);
        }

        @Override
        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return handle.createArrayOf(typeName, elements);
        }

        @Override
        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return handle.createStruct(typeName, attributes);
        }

        @Override
        public String getSchema() throws SQLException {
            return handle.getSchema();
        }

        @Override
        public void setSchema(String schema) throws SQLException {
            handle.setSchema(schema);
        }

        @Override
        public void abort(Executor executor) throws SQLException {
            handle.abort(executor);
        }

        @Override
        public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
            handle.setNetworkTimeout(executor, milliseconds);
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return handle.getNetworkTimeout();
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return iface.isInstance(handle) || handle.isWrapperFor(iface);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(handle)) {
                return (T) handle;
            }
            return handle.unwrap(iface);
        }
    }
}
