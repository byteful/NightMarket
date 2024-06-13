package me.byteful.plugin.nightmarket.datastore.impl;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.datastore.SQLDataStoreProvider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLDataStoreProvider extends SQLDataStoreProvider {
  public MySQLDataStoreProvider(NightMarketPlugin plugin) {
    super(buildConnection(plugin.getConfig().getString("datastore.mysql.host"), plugin.getConfig().getInt("datastore.mysql.port"), plugin.getConfig().getString("datastore.mysql.user"), plugin.getConfig().getString("datastore.mysql.password"), plugin.getConfig().getString("datastore.mysql.database")), "ON DUPLICATE KEY UPDATE");
  }

  private static Connection buildConnection(String host, int port, String user, String pass, String database) {
    try {
      Class.forName("com.mysql.jdbc.Driver");

      final Connection connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/?user=" + user + "&password=" + pass);
      connection.createStatement().execute("CREATE DATABASE IF NOT EXISTS " + database + ";");
      connection.createStatement().execute("USE " + database + ";");

      return connection;
    } catch (SQLException | ClassNotFoundException e) {
      throw new RuntimeException("Failed to connect to MySQL!", e);
    }
  }
}
