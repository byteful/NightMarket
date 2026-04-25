package me.byteful.plugin.nightmarket.schedule.access;

import static me.byteful.plugin.nightmarket.schedule.ScheduleUtils.getDateNearest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.schedule.ScheduleType;
import org.bukkit.configuration.ConfigurationSection;

public class AccessScheduleManager {
    private final Set<AccessSchedule> schedules = new HashSet<>();
    private final ScheduleType mode;
    private final NightMarketPlugin plugin;

    public AccessScheduleManager(NightMarketPlugin plugin) {
        this.plugin = plugin;
        final ConfigurationSection config = plugin.getConfig().getConfigurationSection("access_schedule");
        this.mode = ScheduleType.fromName(config.getString("mode"));
        List<Map<?, ?>> scheduleMap;

        if (this.mode == ScheduleType.DATE) {
            scheduleMap = config.getMapList("dates");
        } else if (this.mode == ScheduleType.TIMES) {
            scheduleMap = config.getMapList("times");
        } else {
            throw new UnsupportedOperationException();
        }

        for (Map<?, ?> map : scheduleMap) {
            this.schedules.add(new AccessSchedule(this.mode, map, plugin.getTimezone()));
        }
    }

    public LocalDateTime getNextTime() {
        final LocalDateTime now = LocalDateTime.now(this.plugin.getTimezone());

        if (this.isShopOpen()) {
            return now;
        }

        List<LocalDateTime> startTimes = this.schedules.stream()
            .map(s -> s.getStart(now))
            .collect(Collectors.toList());

        if (this.mode == ScheduleType.TIMES) {
            final List<LocalDateTime> adjusted = new ArrayList<>();
            for (final LocalDateTime start : startTimes) {
                if (start.isBefore(now)) {
                    adjusted.add(start.plusDays(1));
                } else {
                    adjusted.add(start);
                }
            }
            startTimes = adjusted;
        }

        final List<LocalDateTime> futureStarts = startTimes.stream().filter(s -> s.isAfter(now)).collect(Collectors.toList());

        return getDateNearest(futureStarts, now, this.plugin.getTimezone());
    }

    public boolean isShopOpen() {
        return this.schedules.stream().anyMatch(AccessSchedule::isNowBetween);
    }
}
