package me.byteful.plugin.nightmarket.datastore.impl;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.datastore.SQLDataStoreProvider;
import redempt.redlib.sql.SQLHelper;

import java.sql.Connection;

public class MySQLDataStoreProvider extends SQLDataStoreProvider {
  public MySQLDataStoreProvider(NightMarketPlugin plugin) {
    super(buildConnection(plugin.getConfig().getString("datastore.mysql.host"), plugin.getConfig().getInt("datastore.mysql.port"), plugin.getConfig().getString("datastore.mysql.user"), plugin.getConfig().getString("datastore.mysql.password"), plugin.getConfig().getString("datastore.mysql.database")), "ON DUPLICATE KEY UPDATE");
  }

  private static Connection buildConnection(String host, int port, String user, String pass, String database) {
    return SQLHelper.openMySQL(host, port, user, pass, database);
  }
}
