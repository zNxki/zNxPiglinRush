package dev.znxki.zNxPiglinRush.api.spawner;

import dev.znxki.zNxPiglinRush.config.SpawnerDefinition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Immutable snapshot of the context in which a spawn event occurred.
 * Passed to {@link SpawnStrategy} implementations so they have all
 * information they need without reaching back into global state.
 */
public record SpawnContext(
        Block triggerBlock,
        Location spawnLocation,
        World world,
        SpawnerDefinition definition
) {
}
