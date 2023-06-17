package me.byteful.plugin.nightmarket.schedule.rotate;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.schedule.ScheduleType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static me.byteful.plugin.nightmarket.schedule.ScheduleUtils.getDateNearest;
import static me.byteful.plugin.nightmarket.util.Text.color;
import static me.byteful.plugin.nightmarket.util.Text.format;

public class RotateScheduleManager {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final NightMarketPlugin plugin;
    private final Set<LocalDateTime> scheduledTimes = new HashSet<>();

    public RotateScheduleManager(NightMarketPlugin plugin) {
        this.plugin = plugin;
        final ConfigurationSection config = plugin.getConfig().getConfigurationSection("rotate_schedule");
        final ScheduleType mode = ScheduleType.fromName(config.getString("mode"));
        List<String> schedules;

        if (mode == ScheduleType.DATE) {
            schedules = config.getStringList("dates");
        } else if (mode == ScheduleType.TIMES) {
            schedules = config.getStringList("times");
        } else {
            throw new UnsupportedOperationException();
        }

        final LocalDateTime now = LocalDateTime.now();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (String schedule : schedules) {
                    final LocalDateTime then = mode.parse(schedule);
                    if (now.isAfter(then) && mode == ScheduleType.DATE) {
                        plugin.getLogger().warning("Please remove old date '" + schedule + "' from your config.");

                        continue;
                    }
                    scheduledTimes.add(then);
                    scheduler.schedule(() -> {
                        rotate();
                        scheduledTimes.remove(then);
                    }, Duration.between(now, then).toMillis(), TimeUnit.MILLISECONDS);
                }

                if (mode == ScheduleType.TIMES) {
                    runTaskLater(plugin, 20L * TimeUnit.DAYS.toSeconds(1));
                }
            }
        }.run();
        plugin.getLogger().info("Scheduled rotating times...");
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public LocalDateTime getNextTime() {
        final LocalDateTime now = LocalDateTime.now();

        return getDateNearest(scheduledTimes.stream().filter(x -> x.isAfter(now)).collect(Collectors.toList()), now);
    }

    public void rotate() {
        plugin.getPlayerShopManager().rotateShops();
        if (plugin.getConfig().getBoolean("other.rotate_announcement")) {
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(format(p, color(plugin.getMessage(p, "rotate_announcement")))));
        }
    }
}
