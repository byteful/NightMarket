package me.byteful.plugin.nightmarket.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import org.bukkit.entity.Player;

public class ScheduleReplacementService {
    private final NightMarketPlugin plugin;

    public ScheduleReplacementService(NightMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public String[] getReplacements(Player player) {
        final LocalDateTime openTime = this.plugin.getAccessScheduleManager().getNextTime();
        final LocalDateTime rotateTime = this.plugin.getRotateScheduleManager().getNextTime();

        return new String[]{
            "{open_time}", this.formatDate(openTime),
            "{open_countdown}", this.formatCountdown(openTime),
            "{rotate_time}", this.formatDate(rotateTime),
            "{rotate_countdown}", this.formatCountdown(rotateTime),
            "{timezone}", this.plugin.getTimezone().toString()
        };
    }

    public String[] append(Player player, String... replacements) {
        final String[] schedule = this.getReplacements(player);
        final String[] combined = new String[schedule.length + replacements.length];
        System.arraycopy(schedule, 0, combined, 0, schedule.length);
        System.arraycopy(replacements, 0, combined, schedule.length, replacements.length);
        return combined;
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) {
            return this.plugin.getMessageManager().get("schedule_time_unavailable");
        }

        final String pattern = this.plugin.getConfig().getString("formats.date_time", "MM/dd/yyyy hh:mm:ss a");
        return date.format(DateTimeFormatter.ofPattern(pattern, Locale.US));
    }

    private String formatCountdown(LocalDateTime date) {
        if (date == null) {
            return this.plugin.getMessageManager().get("schedule_countdown_unavailable");
        }

        Duration duration = Duration.between(LocalDateTime.now(this.plugin.getTimezone()), date);
        if (duration.isNegative()) {
            duration = Duration.ZERO;
        }

        final long seconds = duration.getSeconds();
        final long days = seconds / 86400;
        final long hours = seconds / 3600;
        final long minutes = (seconds % 3600) / 60;
        final long remainingSeconds = seconds % 60;
        return this.plugin.getConfig().getString("formats.countdown", "{hours}h, {minutes}m, {seconds}s")
            .replace("{days}", String.valueOf(days))
            .replace("{hours}", String.valueOf(hours))
            .replace("{minutes}", String.valueOf(minutes))
            .replace("{seconds}", String.valueOf(remainingSeconds));
    }
}
