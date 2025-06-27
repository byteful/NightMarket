package me.byteful.plugin.nightmarket.schedule.access;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.schedule.ScheduleType;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

public class AccessSchedule {
    private final ScheduleType type;
    private final String startRaw, endRaw;

    public AccessSchedule(ScheduleType type, Map<?, ?> data) {
        this.type = type;
        this.startRaw = (String) data.get("start");
        this.endRaw = (String) data.get("end");
    }

    public boolean isNowBetween() {
        final LocalDateTime now = LocalDateTime.now(NightMarketPlugin.getInstance().getTimezone());

        if (type == ScheduleType.DATE) {
            return now.isAfter(getStart(now)) && now.isBefore(getEnd(now));
        } else {
            // Check current day's schedule window
            final LocalDateTime start = getStart(now);
            final LocalDateTime end = getEnd(now);
            if (now.isAfter(start) && now.isBefore(end)) {
                return true;
            }

            // Check previous day's schedule window (for overnight schedules)
            final LocalDateTime prevStart = getStart(now.minusDays(1));
            final LocalDateTime prevEnd = getEnd(now.minusDays(1));
            return now.isAfter(prevStart) && now.isBefore(prevEnd);
        }
    }

    public LocalDateTime getStart(LocalDateTime from) {
        if (type == ScheduleType.TIMES) {
            return type.parseTime(startRaw).atDate(from.toLocalDate());
        } else {
            return type.parse(startRaw);
        }
    }

    public LocalDateTime getEnd(LocalDateTime from) {
        if (type == ScheduleType.TIMES) {
            final LocalTime startTime = type.parseTime(startRaw);
            final LocalTime endTime = type.parseTime(endRaw);
            LocalDateTime end = endTime.atDate(from.toLocalDate());
            if (endTime.isBefore(startTime)) {
                end = end.plusDays(1);
            }

            return end;
        } else {
            return type.parse(endRaw);
        }
    }

    public LocalDateTime getStart() {
        return getStart(LocalDateTime.now(NightMarketPlugin.getInstance().getTimezone()));
    }
}
