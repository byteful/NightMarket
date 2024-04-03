package me.byteful.plugin.nightmarket.parser;

import java.util.ArrayList;
import java.util.List;

public class SlotNumberParser {
  public static List<Integer> parse(List<String> data) {
    final List<Integer> list = new ArrayList<>();

    for (String s : data) {
      if (s.contains("-")) {
        final String[] split = s.split("-");
        final Integer start = parseInt(split[0]);
        final Integer end = parseInt(split[1]);
        if (start == null || end == null) {
          continue;
        }
        for (int i = start; i <= end; i++) {
          list.add(i);
        }
      } else {
        final Integer parsed = parseInt(s);
        if (parsed != null) {
          list.add(parsed);
        }
      }
    }

    return list;
  }

  private static Integer parseInt(String str) {
    try {
      return Integer.parseInt(str);
    } catch (Exception ignored) {
      return null;
    }
  }
}
