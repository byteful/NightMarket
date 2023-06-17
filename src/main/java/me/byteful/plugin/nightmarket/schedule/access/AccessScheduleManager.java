package me.byteful.plugin.nightmarket.schedule.access;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.schedule.ScheduleType;
import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static me.byteful.plugin.nightmarket.schedule.ScheduleUtils.getDateNearest;

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

    public LocalDateTime getNextTime() {
        final LocalDateTime now = LocalDateTime.now();

        if (isShopOpen()) {
            return now;
        }

        System.out.println(schedules.stream().map(AccessSchedule::getStart).collect(Collectors.toList()));

        return getDateNearest(schedules.stream().map(AccessSchedule::getStart).collect(Collectors.toList()), now);
    }
}
