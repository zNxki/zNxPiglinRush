package dev.znxki.zNxPiglinRush.scheduler;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.api.scheduler.SchedulerService;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * {@link SchedulerService} implementation for Spigot and Paper servers.
 * Delegates to the standard {@code BukkitScheduler}.
 */
public final class BukkitSchedulerService implements SchedulerService {

    private final PiglinRushPlugin plugin;

    public BukkitSchedulerService(PiglinRushPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAtLocation(Location location, Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    @Override
    public void runForEntity(Entity entity, Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    @Override
    public void runDelayedGlobal(Runnable task, long delayTicks) {
        plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public void runAsync(Runnable task) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public boolean isFolia() {
        return false;
    }
}
