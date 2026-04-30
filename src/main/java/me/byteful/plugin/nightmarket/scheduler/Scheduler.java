package me.byteful.plugin.nightmarket.scheduler;

import org.bukkit.entity.Player;

public interface Scheduler {
    void runAsync(Runnable runnable);

    void runGlobal(Runnable runnable);

    void runForPlayer(Player player, Runnable runnable);

    ScheduledTask runGlobalTimer(Runnable runnable, long delayTicks, long periodTicks);

    void runGlobalDelayed(Runnable runnable, long delayTicks);
}
