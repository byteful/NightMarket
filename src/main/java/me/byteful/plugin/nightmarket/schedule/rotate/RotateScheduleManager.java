package me.byteful.plugin.nightmarket.schedule.rotate;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.schedule.ScheduleType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

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
  final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  final NightMarketPlugin plugin;
  final Set<LocalDateTime> scheduledTimes = new HashSet<>();

  public RotateScheduleManager(NightMarketPlugin plugin) {
    this.plugin = plugin;
  }

  public ScheduledExecutorService getScheduler() {
    return scheduler;
  }

  public LocalDateTime getNextTime() {
    final LocalDateTime now = LocalDateTime.now(NightMarketPlugin.getInstance().getTimezone());

    return getDateNearest(scheduledTimes.stream().filter(x -> x.isAfter(now)).collect(Collectors.toList()), now);
  }

  public void rotate() {
    plugin.getPlayerShopManager().rotateShops();
    if (plugin.getConfig().getBoolean("other.rotate_announcement")) {
      Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(format(p, color(plugin.getMessage(p, "rotate_announcement")))));
    }
  }

  public void load() {
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

    final LocalDateTime now = LocalDateTime.now(NightMarketPlugin.getInstance().getTimezone());

    new ScheduleTask(schedules, mode, now, this).run();
    plugin.getLogger().info("Scheduled rotating times...");
  }

  public void scheduleTask(List<String> schedules, ScheduleType mode, LocalDateTime now, RotateScheduleManager scheduleManager) {
    Bukkit.getScheduler().runTaskLater(plugin, new ScheduleTask(schedules, mode, now, scheduleManager), 20L * TimeUnit.DAYS.toSeconds(1));
  }
}
