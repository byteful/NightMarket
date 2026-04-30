package me.byteful.plugin.nightmarket.scheduler.impl;

import me.byteful.plugin.nightmarket.scheduler.ScheduledTask;
import me.byteful.plugin.nightmarket.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class FoliaSchedulerImpl implements Scheduler {
    private final Plugin plugin;

    public FoliaSchedulerImpl(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable runnable) {
        Bukkit.getAsyncScheduler().runNow(this.plugin, t -> runnable.run());
    }

    @Override
    public void runGlobal(Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().run(this.plugin, t -> runnable.run());
    }

    @Override
    public void runForPlayer(Player player, Runnable runnable) {
        player.getScheduler().run(this.plugin, t -> runnable.run(), null);
    }

    @Override
    public ScheduledTask runGlobalTimer(Runnable runnable, long delayTicks, long periodTicks) {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(this.plugin, t -> runnable.run(), delayTicks, periodTicks)::cancel;
    }

    @Override
    public void runGlobalDelayed(Runnable runnable, long delayTicks) {
        Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, t -> runnable.run(), delayTicks);
    }
}
