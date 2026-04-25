package me.byteful.plugin.nightmarket.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public enum ScheduleType {
    DATE {
        private final DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a", Locale.US);

        @Override
        public LocalDateTime parse(String str) {
            return LocalDateTime.parse(str, this.format);
        }

        @Override
        public LocalTime parseTime(String str) {
            throw new UnsupportedOperationException("Date schematics do not support parsing only time!");
        }
    },
    TIMES {
        private final DateTimeFormatter format = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.US);

        @Override
        public LocalDateTime parse(String str) {
            return LocalTime.parse(str, this.format).atDate(LocalDate.now());
        }

        @Override
        public LocalTime parseTime(String str) {
            return LocalTime.parse(str, this.format);
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
