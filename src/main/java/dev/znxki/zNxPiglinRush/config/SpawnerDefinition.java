package dev.znxki.zNxPiglinRush.config;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

/**
 * Immutable definition for a single spawner block type.
 * Loaded from the {@code spawners:} section of {@code config.yml} by
 * {@link dev.znxki.zNxPiglinRush.spawner.registry.SpawnerRegistry}.
 *
 * <p>Replaces the old {@code BlockSpawnerConfig} record. The rename makes
 * the purpose clearer — this is a <em>definition</em> (configuration data),
 * not a runtime config object.
 */
public record SpawnerDefinition(
        Material block,
        EntityType mobType,
        int spawnCount,
        int spawnCooldown,
        int maxNearbyEntities,
        int scanRadius,
        int extraAttempts,
        boolean waveMode,
        int waveSize,
        int waveCooldownTicks,
        List<CustomDrop> customDrops
) {

    /**
     * A single custom-drop rule associated with this spawner's mob.
     */
    public record CustomDrop(Material material, int minAmount, int maxAmount, double chance) {

        /**
         * Rolls this drop rule against {@code rng}.
         *
         * @return the resulting {@link ItemStack}, or {@code null} if the roll failed.
         */
        public ItemStack roll(Random rng) {
            if (rng.nextDouble() > chance) return null;
            int amount = minAmount + rng.nextInt(Math.max(1, maxAmount - minAmount + 1));
            return new ItemStack(material, amount);
        }
    }
}
