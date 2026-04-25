package me.byteful.plugin.nightmarket.schedule.access;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import me.byteful.plugin.nightmarket.schedule.ScheduleType;

public class AccessSchedule {
    private final ScheduleType type;
    private final String startRaw, endRaw;
    private final ZoneId timezone;

    public AccessSchedule(ScheduleType type, Map<?, ?> data, ZoneId timezone) {
        this.type = type;
        this.startRaw = (String) data.get("start");
        this.endRaw = (String) data.get("end");
        this.timezone = timezone;
    }

    public boolean isNowBetween() {
        final LocalDateTime now = LocalDateTime.now(this.timezone);

        if (this.type == ScheduleType.DATE) {
            return now.isAfter(this.getStart(now)) && now.isBefore(this.getEnd(now));
        } else {
            final LocalDateTime start = this.getStart(now);
            final LocalDateTime end = this.getEnd(now);
            if (now.isAfter(start) && now.isBefore(end)) {
                return true;
            }

            final LocalDateTime prevStart = this.getStart(now.minusDays(1));
            final LocalDateTime prevEnd = this.getEnd(now.minusDays(1));
            return now.isAfter(prevStart) && now.isBefore(prevEnd);
        }
    }

    public LocalDateTime getStart(LocalDateTime from) {
        if (this.type == ScheduleType.TIMES) {
            return this.type.parseTime(this.startRaw).atDate(from.toLocalDate());
        } else {
            return this.type.parse(this.startRaw);
        }
    }

    public LocalDateTime getEnd(LocalDateTime from) {
        if (this.type == ScheduleType.TIMES) {
            final LocalTime startTime = this.type.parseTime(this.startRaw);
            final LocalTime endTime = this.type.parseTime(this.endRaw);
            LocalDateTime end = endTime.atDate(from.toLocalDate());
            if (endTime.isBefore(startTime)) {
                end = end.plusDays(1);
            }

            return end;
        } else {
            return this.type.parse(this.endRaw);
        }
    }

    public LocalDateTime getStart() {
        return this.getStart(LocalDateTime.now(this.timezone));
    }
}
