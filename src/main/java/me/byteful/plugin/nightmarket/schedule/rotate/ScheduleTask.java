package me.byteful.plugin.nightmarket.schedule.rotate;

import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.schedule.ScheduleType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ScheduleTask implements Runnable {
    private final List<String> schedules;
    private final ScheduleType mode;
    private final Supplier<LocalDateTime> nowSupplier;
    private final RotateScheduleManager scheduleManager;

    public ScheduleTask(List<String> schedules, ScheduleType mode, Supplier<LocalDateTime> nowSupplier, RotateScheduleManager scheduleManager) {
        this.schedules = schedules;
        this.mode = mode;
        this.nowSupplier = nowSupplier;
        this.scheduleManager = scheduleManager;
    }

    @Override
    public void run() {
        final LocalDateTime now = nowSupplier.get();

        for (String schedule : schedules) {
            LocalDateTime then = mode.parse(schedule);
            if (now.isAfter(then) && mode == ScheduleType.DATE) {
                scheduleManager.plugin.getLogger().warning("Please remove old date '" + schedule + "' from your config.");

                continue;
            }
            if (then.isBefore(now) && mode == ScheduleType.TIMES) {
                then = then.plusDays(1);
            }
            final long duration = Duration.between(now, then).toMillis();
            NightMarketPlugin.getInstance().debug("Rotation entry scheduled (" + schedule + ") to run at " + then + " (duration: " + duration + "ms)");
            scheduleManager.scheduledTimes.add(then);
            final LocalDateTime finalThen = then;
            scheduleManager.scheduler.schedule(() -> {
                scheduleManager.rotate();
                scheduleManager.scheduledTimes.remove(finalThen);
            }, duration, TimeUnit.MILLISECONDS);
        }

        if (mode == ScheduleType.TIMES) {
            scheduleManager.scheduleTask(schedules, mode, nowSupplier, scheduleManager);
        }
    }
}
