package me.byteful.plugin.nightmarket.schedule.rotate;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.schedule.ScheduleType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static me.byteful.plugin.nightmarket.util.Text.color;
import static me.byteful.plugin.nightmarket.util.Text.format;

public class RotateScheduleManager {
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final NightMarketPlugin plugin;

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

    for (String schedule : schedules) {
      final LocalDateTime then = mode.parse(schedule);
      scheduler.schedule(this::rotate, Duration.between(now, then).toMillis(), TimeUnit.MILLISECONDS);
    }
    plugin.getLogger().info("Scheduled rotating times...");
  }

  public ScheduledExecutorService getScheduler() {
    return scheduler;
  }

  public void rotate() {
    plugin.getPlayerShopManager().rotateShops();
    if(plugin.getConfig().getBoolean("other.rotate_announcement")) {
      Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(format(p, color(plugin.getMessages().get("rotate_announcement")))));
    }
  }
}