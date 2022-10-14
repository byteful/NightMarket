package me.byteful.plugin.nightmarket.datastore.impl;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.datastore.SQLDataStoreProvider;
import me.byteful.plugin.nightmarket.util.dependency.IsolatedClassLoader;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.util.Properties;

public class MySQLDataStoreProvider extends SQLDataStoreProvider {
  public MySQLDataStoreProvider(IsolatedClassLoader loader, NightMarketPlugin plugin) {
    super(buildConnection(loader, plugin.getConfig().getString("mysql_uri")), "ON DUPLICATE KEY UPDATE");
  }

  private static Connection buildConnection(IsolatedClassLoader loader, String jdbcUrl) {
//    try {
//      return new Driver().connect(jdbcUrl, new Properties());
//    } catch (Exception e) {
//      throw new RuntimeException(e);
//    }

    try {
      final Class<?> driverClass = loader.loadClass("com.mysql.jdbc.Driver");
      return (Connection) driverClass.getConstructor(String.class, Properties.class).newInstance(jdbcUrl.startsWith("jdbc") ? jdbcUrl : "jdbc:" + jdbcUrl, new Properties());
    } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
             IllegalAccessException | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
}