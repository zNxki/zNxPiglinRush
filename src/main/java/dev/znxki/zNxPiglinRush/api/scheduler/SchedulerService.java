package dev.znxki.zNxPiglinRush.api.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Abstracts task scheduling across Spigot/Paper and Folia.
 *
 * <p>Folia uses a region-threaded model — tasks must be dispatched to the
 * region that owns a given chunk. Implementations of this interface hide
 * that complexity from the rest of the codebase.
 */
public interface SchedulerService {

    /**
     * Runs {@code task} on the region owning {@code location} on the next tick
     * (Folia), or on the main thread on the next tick (Spigot/Paper).
     */
    void runAtLocation(Location location, Runnable task);

    /**
     * Runs {@code task} on the region owning {@code entity}'s chunk (Folia),
     * or on the main thread (Spigot/Paper).
     */
    void runForEntity(Entity entity, Runnable task);

    /**
     * Schedules a delayed non-spatial task. Maps to
     * {@code GlobalRegionScheduler.runDelayed} on Folia and
     * {@code BukkitScheduler.runTaskLater} on Spigot/Paper.
     */
    void runDelayedGlobal(Runnable task, long delayTicks);

    /**
     * Runs {@code task} asynchronously. Safe on both Folia and Spigot/Paper.
     */
    void runAsync(Runnable task);

    /**
     * Returns {@code true} when running on a Folia server.
     */
    boolean isFolia();
}
