package dev.znxki.zNxPiglinRush.scheduler;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.api.scheduler.SchedulerService;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * {@link SchedulerService} implementation for Folia servers.
 *
 * <p>Routes each task to the appropriate Folia scheduler:
 * <ul>
 *   <li>Spatial tasks → {@code RegionScheduler} (chunk-owning region)</li>
 *   <li>Entity tasks → {@code EntityScheduler} (entity-owning region)</li>
 *   <li>Non-spatial delayed → {@code GlobalRegionScheduler}</li>
 *   <li>Async I/O → {@code AsyncScheduler}</li>
 * </ul>
 */
public final class FoliaSchedulerService implements SchedulerService {

    private final PiglinRushPlugin plugin;

    public FoliaSchedulerService(PiglinRushPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAtLocation(Location location, Runnable task) {
        plugin.getServer().getRegionScheduler().run(plugin, location, t -> task.run());
    }

    @Override
    public void runForEntity(Entity entity, Runnable task) {
        entity.getScheduler().run(plugin, t -> task.run(), null);
    }

    @Override
    public void runDelayedGlobal(Runnable task, long delayTicks) {
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), delayTicks);
    }

    @Override
    public void runAsync(Runnable task) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, t -> task.run());
    }

    @Override
    public boolean isFolia() {
        return true;
    }
}
