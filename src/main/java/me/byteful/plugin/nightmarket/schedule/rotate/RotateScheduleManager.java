package me.byteful.plugin.nightmarket.schedule.rotate;

import static me.byteful.plugin.nightmarket.schedule.ScheduleUtils.getDateNearest;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.schedule.ScheduleType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class RotateScheduleManager {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final NightMarketPlugin plugin;
    private final Set<LocalDateTime> scheduledTimes = new HashSet<>();

    public RotateScheduleManager(NightMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public ScheduledExecutorService getScheduler() {
        return this.scheduler;
    }

    public NightMarketPlugin getPlugin() {
        return this.plugin;
    }

    public Set<LocalDateTime> getScheduledTimes() {
        return this.scheduledTimes;
    }

    public void rotate() {
        this.plugin.getPlayerShopManager().rotateShops();
        if (this.plugin.getConfig().getBoolean("other.rotate_announcement")) {
            Bukkit.getOnlinePlayers().forEach(p -> this.plugin.sendMessage(p, p, "rotate_announcement"));
        }
    }

    public LocalDateTime getNextTime() {
        final LocalDateTime now = LocalDateTime.now(this.plugin.getTimezone());

        final ConfigurationSection config = this.plugin.getConfig().getConfigurationSection("rotate_schedule");
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

        return getDateNearest(scheduledTimesNow.stream().filter(x -> x.isAfter(now)).collect(Collectors.toList()), now, this.plugin.getTimezone());
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

    public void load() {
        final ConfigurationSection config = this.plugin.getConfig().getConfigurationSection("rotate_schedule");
        final ScheduleType mode = ScheduleType.fromName(config.getString("mode"));
        final List<String> schedules = getSchedules(mode, config);

        new ScheduleTask(schedules, mode, () -> LocalDateTime.now(this.plugin.getTimezone()), this).run();
        this.plugin.getLogger().info("Scheduled rotating times...");
    }

    public void scheduleTask(List<String> schedules, ScheduleType mode, Supplier<LocalDateTime> now, RotateScheduleManager scheduleManager) {
        this.plugin.getScheduler().runGlobalDelayed(new ScheduleTask(schedules, mode, now, scheduleManager), 20L * TimeUnit.DAYS.toSeconds(1));
    }
}
