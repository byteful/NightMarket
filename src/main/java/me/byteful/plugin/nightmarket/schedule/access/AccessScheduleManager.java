package me.byteful.plugin.nightmarket.schedule.access;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.schedule.ScheduleType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AccessScheduleManager {
  private final Set<AccessSchedule> schedules = new HashSet<>();

  public AccessScheduleManager(NightMarketPlugin plugin) {
    final ConfigurationSection config = plugin.getConfig().getConfigurationSection("access_schedule");
    final ScheduleType mode = ScheduleType.fromName(config.getString("mode"));
    List<Map<?, ?>> scheduleMap;

    if (mode == ScheduleType.DATE) {
      scheduleMap = config.getMapList("dates");
    } else if (mode == ScheduleType.TIMES) {
      scheduleMap = config.getMapList("times");
    } else {
      throw new UnsupportedOperationException();
    }

    for (Map<?, ?> map : scheduleMap) {
      schedules.add(new AccessSchedule(mode, map));
    }
  }

  public boolean isShopOpen() {
    return schedules.stream().anyMatch(AccessSchedule::isNowBetween);
  }
}