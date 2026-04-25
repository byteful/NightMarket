package me.byteful.plugin.nightmarket.scheduler;

public interface Scheduler {
    void runAsync(Runnable runnable);

    void runGlobal(Runnable runnable);

    ScheduledTask runGlobalTimer(Runnable runnable, long delayTicks, long periodTicks);

    void runGlobalDelayed(Runnable runnable, long delayTicks);
}
