package me.byteful.plugin.nightmarket.schedule.rotate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import me.byteful.plugin.nightmarket.schedule.ScheduleType;

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
        final LocalDateTime now = this.nowSupplier.get();

        for (String schedule : this.schedules) {
            LocalDateTime then = this.mode.parse(schedule);
            if (now.isAfter(then) && this.mode == ScheduleType.DATE) {
                this.scheduleManager.getPlugin().getLogger().warning("Please remove old date '" + schedule + "' from your config.");

                continue;
            }
            if (then.isBefore(now) && this.mode == ScheduleType.TIMES) {
                then = then.plusDays(1);
            }
            final long duration = Duration.between(now, then).toMillis();
            this.scheduleManager.getPlugin().debug("Rotation entry scheduled (" + schedule + ") to run at " + then + " (duration: " + duration + "ms)");
            this.scheduleManager.getScheduledTimes().add(then);
            final LocalDateTime finalThen = then;
            this.scheduleManager.getScheduler().schedule(() -> {
                this.scheduleManager.rotate();
                this.scheduleManager.getScheduledTimes().remove(finalThen);
            }, duration, TimeUnit.MILLISECONDS);
        }

        if (this.mode == ScheduleType.TIMES) {
            this.scheduleManager.scheduleTask(this.schedules, this.mode, this.nowSupplier, this.scheduleManager);
        }
    }
}
