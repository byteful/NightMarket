package me.byteful.plugin.nightmarket.schedule;

import me.byteful.plugin.nightmarket.NightMarketPlugin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public enum ScheduleType {
    DATE {
        private final DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a", Locale.US).withZone(NightMarketPlugin.getInstance().getTimezone());

        @Override
        public LocalDateTime parse(String str) {
            return LocalDateTime.parse(str, format);
            //return format.parse(str, Instant::from);
        }

        @Override
        public LocalTime parseTime(String str) {
            throw new UnsupportedOperationException("Date schematics do not support parsing only time!");
        }
    },
    TIMES {
        private final DateTimeFormatter format = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.US).withZone(NightMarketPlugin.getInstance().getTimezone());

        @Override
        public LocalDateTime parse(String str) {
            return LocalTime.parse(str, format).atDate(LocalDate.now());
            //return format.parse(str, Instant::from);
        }

        @Override
        public LocalTime parseTime(String str) {
            return LocalTime.parse(str, format);
        }
    };

    public static ScheduleType fromName(String str) {
        try {
            return valueOf(str.toUpperCase().trim().replace(" ", "_"));
        } catch (Exception e) {
            throw new RuntimeException("Please use either DATE or TIMES for your schedule modes!", e);
        }
    }

    public LocalTime parseTime(String str) {
        throw new UnsupportedOperationException("Time parsing is not supported for all schedule types!");
    }

    public abstract LocalDateTime parse(String str);
}
