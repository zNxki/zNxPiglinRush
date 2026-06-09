package dev.znxki.zNxPiglinRush.spawner.strategy;

import dev.znxki.zNxPiglinRush.config.SpawnerDefinition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Random;

/**
 * Utility that finds a valid spawn location near a trigger block.
 * Shared between {@link StandardSpawnStrategy} and {@link WaveSpawnStrategy}.
 */
public final class SpawnLocationResolver {

    private SpawnLocationResolver() {
    }

    /**
     * Searches for a passable 2-block-tall slot on a matching block within
     * {@code definition.scanRadius()}, falling back to directly above
     * {@code origin} if nothing suitable is found.
     *
     * @return a centred spawn location, or {@code null} if all positions are blocked
     */
    public static Location find(World world, Block origin, SpawnerDefinition definition, Random random) {
        int radius = definition.scanRadius();
        for (int attempt = 0; attempt < 8; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int dy = random.nextInt(3) - 1;
            Block candidate = world.getBlockAt(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
            if (candidate.getType() == definition.block() && isClear(candidate))
                return candidate.getLocation().add(0.5, 1.0, 0.5);
        }
        return isClear(origin) ? origin.getLocation().add(0.5, 1.0, 0.5) : null;
    }

    private static boolean isClear(Block b) {
        return b.getRelative(0, 1, 0).isPassable() && b.getRelative(0, 2, 0).isPassable();
    }
}
