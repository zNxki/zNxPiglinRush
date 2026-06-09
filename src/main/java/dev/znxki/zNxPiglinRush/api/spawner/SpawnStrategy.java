package dev.znxki.zNxPiglinRush.api.spawner;

/**
 * Strategy interface for deciding how to respond to a vanilla spawn trigger.
 *
 * <p>Two built-in strategies ship with the plugin:
 * <ul>
 *   <li>{@link dev.znxki.zNxPiglinRush.spawner.strategy.StandardSpawnStrategy}</li>
 *   <li>{@link dev.znxki.zNxPiglinRush.spawner.strategy.WaveSpawnStrategy}</li>
 * </ul>
 */
public interface SpawnStrategy {

    /**
     * Called when a vanilla spawn triggers on a block managed by this strategy.
     * Must be Folia-safe — defer any work through {@link dev.znxki.zNxPiglinRush.api.scheduler.SchedulerService}.
     */
    void onTrigger(SpawnContext context);

    default void cleanup() {
    }
}
