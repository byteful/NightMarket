package me.byteful.plugin.nightmarket.scheduler.impl;

import me.byteful.plugin.nightmarket.scheduler.ScheduledTask;
import me.byteful.plugin.nightmarket.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BukkitSchedulerImpl implements Scheduler {
    private final Plugin plugin;

    public BukkitSchedulerImpl(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, runnable);
    }

    @Override
    public void runGlobal(Runnable runnable) {
        Bukkit.getScheduler().runTask(this.plugin, runnable);
    }

    @Override
    public void runForPlayer(Player player, Runnable runnable) {
        Bukkit.getScheduler().runTask(this.plugin, runnable);
    }

    @Override
    public ScheduledTask runGlobalTimer(Runnable runnable, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(this.plugin, runnable, delayTicks, periodTicks)::cancel;
    }

    @Override
    public void runGlobalDelayed(Runnable runnable, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(this.plugin, runnable, delayTicks);
    }
}
