package me.byteful.plugin.nightmarket.util;

import java.nio.ByteBuffer;
import java.util.*;

public class SQLUtils {
  public static byte[] serializeUUID(UUID uuid) {
    final ByteBuffer buffer = ByteBuffer.allocate(16);
    buffer.putLong(uuid.getMostSignificantBits());
    buffer.putLong(uuid.getLeastSignificantBits());

    return buffer.array();
  }

  public static UUID deserializeUUID(byte[] data) {
    final ByteBuffer buffer = ByteBuffer.wrap(data);

    return new UUID(buffer.getLong(), buffer.getLong());
  }

  public static String serializeList(List<String> list) {
    return String.join(",", list);
  }

  public static List<String> deserializeList(String data) {
    return new ArrayList<>(Arrays.asList(data.split(",")));
  }
}