package me.byteful.plugin.nightmarket.schedule.rotate;

import me.byteful.plugin.nightmarket.schedule.ScheduleType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ScheduleTask implements Runnable {
  private final List<String> schedules;
  private final ScheduleType mode;
  private final LocalDateTime now;
  private final RotateScheduleManager scheduleManager;

  public ScheduleTask(List<String> schedules, ScheduleType mode, LocalDateTime now, RotateScheduleManager scheduleManager) {
    this.schedules = schedules;
    this.mode = mode;
    this.now = now;
    this.scheduleManager = scheduleManager;
  }

  @Override
  public void run() {
    for (String schedule : schedules) {
      final LocalDateTime then = mode.parse(schedule);
      if (now.isAfter(then) && mode == ScheduleType.DATE) {
        scheduleManager.plugin.getLogger().warning("Please remove old date '" + schedule + "' from your config.");

        continue;
      }
      scheduleManager.scheduledTimes.add(then);
      scheduleManager.scheduler.schedule(() -> {
        scheduleManager.rotate();
        scheduleManager.scheduledTimes.remove(then);
      }, Duration.between(now, then).toMillis(), TimeUnit.MILLISECONDS);
    }

    if (mode == ScheduleType.TIMES) {
      scheduleManager.scheduleTask(schedules, mode, now, scheduleManager);
    }
  }
}
