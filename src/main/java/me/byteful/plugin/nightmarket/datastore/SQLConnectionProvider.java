package me.byteful.plugin.nightmarket.datastore;

import java.io.Closeable;
import java.sql.Connection;
import java.util.function.Supplier;

public interface SQLConnectionProvider extends Closeable, Supplier<Connection> {
  boolean isValid();
}
