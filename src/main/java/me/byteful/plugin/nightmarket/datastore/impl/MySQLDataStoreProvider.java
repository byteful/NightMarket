package me.byteful.plugin.nightmarket.datastore.impl;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.datastore.SQLDataStoreProvider;
import redempt.redlib.sql.SQLHelper;

import java.sql.Connection;

public class MySQLDataStoreProvider extends SQLDataStoreProvider {
  public MySQLDataStoreProvider(NightMarketPlugin plugin) {
    super(buildConnection(
      plugin.getConfig().getString("datastore.mysql.host"),
      plugin.getConfig().getInt("datastore.mysql.port"),
      plugin.getConfig().getString("datastore.mysql.user"),
      plugin.getConfig().getString("datastore.mysql.password"),
      plugin.getConfig().getString("datastore.mysql.database")
    ), "ON DUPLICATE KEY UPDATE");
  }

  private static Connection buildConnection(String host, int port, String user, String pass, String database) {
    return SQLHelper.openMySQL(host, port, user, pass, database);
//    try {
//      Class.forName("com.mysql.jdbc.Driver");
//      return DriverManager.getConnection(jdbcUrl.startsWith("jdbc") ? jdbcUrl : "jdbc:" + jdbcUrl);
//
//      //return new Driver().connect(jdbcUrl.startsWith("jdbc") ? jdbcUrl : "jdbc:" + jdbcUrl, new Properties());
//    } catch (Exception e) {
//      throw new RuntimeException(e);
//    }

//    try {
//      final Class<?> dataSourceClass = loader.loadClass("com.mysql.cj.jdbc.MysqlDataSource");
//      MysqlDataSource ds = (MysqlDataSource) dataSourceClass.getConstructor().newInstance();
//      ds.setURL(jdbcUrl.startsWith("jdbc") ? jdbcUrl : "jdbc:" + jdbcUrl);
//
//      return ds.getConnection();
//      //return ((Driver) dataSourceClass.getConstructor().newInstance()).connect(jdbcUrl.startsWith("jdbc") ? jdbcUrl : "jdbc:" + jdbcUrl, new Properties());
//    } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
//             NoSuchMethodException | SQLException e) {
//      throw new RuntimeException(e);
//    }
  }
}
