package net.danh.sincePet.utils;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Routes work through Paper's region-aware schedulers so the plugin can run on Paper and Folia.
 */
public final class SchedulerUtils {
    private SchedulerUtils() {
    }

    public static ScheduledTask runAsync(Plugin plugin, Runnable task) {
        return Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
    }

    public static ScheduledTask runAsyncTimer(Plugin plugin, Runnable task, long initialDelaySeconds, long periodSeconds) {
        return Bukkit.getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);
    }

    public static ScheduledTask runEntityDelayed(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        return entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, delayTicks);
    }

    public static ScheduledTask runEntityTimer(Plugin plugin, Entity entity, Runnable task, long initialDelayTicks, long periodTicks) {
        return entity.getScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), null, initialDelayTicks, periodTicks);
    }

    public static void cancelAsyncTasks(Plugin plugin) {
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
    }
}
