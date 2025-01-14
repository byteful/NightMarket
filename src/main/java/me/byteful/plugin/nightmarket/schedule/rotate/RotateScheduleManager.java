package me.byteful.plugin.nightmarket.schedule.rotate;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.schedule.ScheduleType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static me.byteful.plugin.nightmarket.schedule.ScheduleUtils.getDateNearest;
import static me.byteful.plugin.nightmarket.util.Text.color;
import static me.byteful.plugin.nightmarket.util.Text.format;

public class RotateScheduleManager {
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    final NightMarketPlugin plugin;
    final Set<LocalDateTime> scheduledTimes = new HashSet<>();

    public RotateScheduleManager(NightMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    private static @NotNull List<String> getSchedules(ScheduleType mode, ConfigurationSection config) {
        List<String> schedules;

        if (mode == ScheduleType.DATE) {
            schedules = config.getStringList("dates");
        } else if (mode == ScheduleType.TIMES) {
            schedules = config.getStringList("times");
        } else {
            throw new UnsupportedOperationException();
        }

        return schedules;
    }

    public void rotate() {
        plugin.getPlayerShopManager().rotateShops();
        if (plugin.getConfig().getBoolean("other.rotate_announcement")) {
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(format(p, color(plugin.getMessage(p, "rotate_announcement")))));
        }
    }

    public LocalDateTime getNextTime() {
        final LocalDateTime now = LocalDateTime.now(NightMarketPlugin.getInstance().getTimezone());

        final ConfigurationSection config = plugin.getConfig().getConfigurationSection("rotate_schedule");
        final ScheduleType mode = ScheduleType.fromName(config.getString("mode"));
        final List<String> schedules = getSchedules(mode, config);

        final Set<LocalDateTime> scheduledTimesNow = new HashSet<>();

        if (mode == ScheduleType.TIMES) {
            for (String schedule : schedules) {
                LocalDateTime parsed = ScheduleType.TIMES.parse(schedule);
                if (parsed.isBefore(now)) {
                    parsed = parsed.plusDays(1);
                }

                scheduledTimesNow.add(parsed);
            }
        } else if (mode == ScheduleType.DATE) {
            for (String schedule : schedules) {
                scheduledTimesNow.add(ScheduleType.DATE.parse(schedule));
            }
        }

        return getDateNearest(scheduledTimesNow.stream().filter(x -> x.isAfter(now)).collect(Collectors.toList()), now);
    }

    public void load() {
        final ConfigurationSection config = plugin.getConfig().getConfigurationSection("rotate_schedule");
        final ScheduleType mode = ScheduleType.fromName(config.getString("mode"));
        final List<String> schedules = getSchedules(mode, config);

        new ScheduleTask(schedules, mode, () -> LocalDateTime.now(NightMarketPlugin.getInstance().getTimezone()), this).run();
        plugin.getLogger().info("Scheduled rotating times...");
    }

    public void scheduleTask(List<String> schedules, ScheduleType mode, Supplier<LocalDateTime> now, RotateScheduleManager scheduleManager) {
        Bukkit.getScheduler().runTaskLater(plugin, new ScheduleTask(schedules, mode, now, scheduleManager), 20L * TimeUnit.DAYS.toSeconds(1));
    }
}
