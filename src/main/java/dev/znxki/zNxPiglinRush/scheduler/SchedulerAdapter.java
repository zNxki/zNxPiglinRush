package dev.znxki.zNxPiglinRush.scheduler;

import dev.znxki.zNxPiglinRush.ZNxPiglinRush;
import org.bukkit.Location;
import org.bukkit.entity.Entity;


/**
 * Thin scheduler abstraction that routes task scheduling to the correct
 * implementation depending on whether the server is running Folia or
 * Spigot/Paper.
 *
 * <p>Folia uses a region-threaded model where tasks must be scheduled
 * on the region that owns a given chunk. Using the global BukkitScheduler
 * on Folia results in a crash or a thread-safety violation, so we must
 * use:
 * <ul>
 *   <li>{@code entity.getScheduler().run()} for entity-relative work</li>
 *   <li>{@code server.getRegionScheduler().run()} for location-relative work</li>
 *   <li>{@code server.getGlobalRegionScheduler().runDelayed()} for non-spatial delayed tasks</li>
 *   <li>{@code server.getAsyncScheduler().runNow()} for truly async work</li>
 * </ul>
 *
 * <p>On Spigot/Paper we simply delegate to the familiar BukkitScheduler.
 */
public class SchedulerAdapter {
    private final ZNxPiglinRush plugin;
    private final boolean isFolia;

    public SchedulerAdapter(ZNxPiglinRush plugin) {
        this.plugin = plugin;
        this.isFolia = detectFolia();
    }

    /**
     * Schedules a task to run on the region that owns {@code location} on
     * the next tick (Folia) or on the main thread next tick (Spigot/Paper).
     */
    public void runAtLocation(Location location, Runnable task) {
        if (isFolia) {
            plugin.getServer().getRegionScheduler().run(plugin, location, t -> task.run());
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Schedules a task bound to a specific entity's region (Folia) or the
     * main thread (Spigot/Paper). Falls back to location-based scheduling
     * if the entity is invalid.
     */
    public void runForEntity(Entity entity, Runnable task) {
        if (isFolia) {
            entity.getScheduler().run(plugin, t -> task.run(), null);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Schedules a delayed task on the global region (Folia) or the main
     * thread (Spigot/Paper). Suitable for non-spatial work like sending
     * a message to a joining player.
     *
     * @param task       the task to run
     * @param delayTicks delay in ticks
     */
    public void runDelayedGlobal(Runnable task, long delayTicks) {
        if (isFolia) {
            plugin.getServer().getGlobalRegionScheduler()
                    .runDelayed(plugin, t -> task.run(), delayTicks);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Runs a task asynchronously. Safe on both Folia and Spigot/Paper —
     * used for network I/O (update checker).
     */
    public void runAsync(Runnable task) {
        if (isFolia) {
            plugin.getServer().getAsyncScheduler()
                    .runNow(plugin, t -> task.run());
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public boolean isFolia() {
        return isFolia;
    }

    /**
     * Detects Folia at runtime via reflection. We check for the presence of
     * {@code io.papermc.paper.threadedregions.RegionizedServer}, which only
     * exists on Folia builds.
     */
    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
