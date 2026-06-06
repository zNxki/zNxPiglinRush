package dev.znxki.zNxPiglinRush.events;

import com.google.common.collect.Maps;
import dev.znxki.zNxPiglinRush.ZNxPiglinRush;
import dev.znxki.zNxPiglinRush.config.PiglinRushConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.PigZombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Listens for vanilla ZombifiedPiglin spawns triggered by magma blocks
 * and fires additional spawns around the same location, boosting the farm rate.
 * <p>
 * Vanilla mechanism (still present in 1.21.x):
 * NetherPortal → RandomTick on a MagmaBlock → spawn attempt on the block above.
 * We hook EntitySpawnEvent, check that the spawned mob is a ZombifiedPiglin
 * standing on a magma block, then queue extra spawns at nearby valid positions.
 */
public class PiglinRushListener implements Listener {
    private final ZNxPiglinRush plugin;
    private final Random random = new Random();

    /**
     * Tracks last spawn time per magma block to enforce cooldown
     */
    private final Map<UUID, Long> blockCooldowns = Maps.newHashMap();

    public PiglinRushListener(ZNxPiglinRush plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(@NonNull EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof PigZombie)) return;

        PiglinRushConfig cfg = plugin.getMagmaConfig();
        if (!cfg.isEnabled()) return;

        Location spawnLoc = event.getLocation();
        World world = spawnLoc.getWorld();
        if (world == null) return;

        if (!cfg.getActiveWorlds().isEmpty() &&
                !cfg.getActiveWorlds().contains(world.getName())) return;

        Block below = world.getBlockAt(
                spawnLoc.getBlockX(),
                spawnLoc.getBlockY() - 1,
                spawnLoc.getBlockZ()
        );
        if (below.getType() != Material.MAGMA_BLOCK) return;

        long now = System.currentTimeMillis();
        UUID blockKey = blockLocationKey(below);
        Long lastSpawn = blockCooldowns.get(blockKey);
        long cooldownMs = (long) cfg.getSpawnCooldown() * 50L;

        if (lastSpawn != null && (now - lastSpawn) < cooldownMs) {
            if (cfg.isDebug()) plugin.getLogger().info("[debug] Cooldown active for block " + below.getLocation());
            return;
        }

        blockCooldowns.put(blockKey, now);
        if (blockCooldowns.size() > 500) cleanOldEntries(now, cooldownMs * 10);

        int totalExtra = (cfg.getSpawnCount() - 1)
                + random.nextInt(cfg.getExtraAttempts() + 1);
        if (totalExtra <= 0) return;

        int nearbyCount = world.getNearbyEntitiesByType(
                PigZombie.class,
                spawnLoc,
                cfg.getScanRadius()
        ).size();

        if (nearbyCount >= cfg.getMaxNearbyEntities()) {
            if (cfg.isDebug())
                plugin.getLogger().info("[debug] Entity cap reached (" + nearbyCount + "), skipping extra spawns.");
            return;
        }

        int allowed = Math.min(totalExtra, cfg.getMaxNearbyEntities() - nearbyCount);
        if (cfg.isDebug())
            plugin.getLogger().info("[debug] Spawning " + allowed + " extra Zombified Piglins at " + spawnLoc);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (int i = 0; i < allowed; i++) {
                Location loc = findSpawnLocation(world, below, cfg.getScanRadius());
                if (loc == null) continue;
                world.spawnEntity(loc, EntityType.ZOMBIFIED_PIGLIN);
            }
        });
    }

    /**
     * Finds a valid spawn location on a magma block within the given radius.
     * Falls back to the original block if no other magma block is found.
     */
    private @Nullable Location findSpawnLocation(World world, Block originMagma, int radius) {
        for (int attempts = 0; attempts < 8; attempts++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int dy = random.nextInt(3) - 1; // ±1 Y

            Block candidate = world.getBlockAt(
                    originMagma.getX() + dx,
                    originMagma.getY() + dy,
                    originMagma.getZ() + dz
            );

            if (candidate.getType() == Material.MAGMA_BLOCK
                    && isSpaceAboveClear(candidate)) {
                return candidate.getLocation().add(0.5, 1.0, 0.5);
            }
        }

        if (isSpaceAboveClear(originMagma)) {
            return originMagma.getLocation().add(0.5, 1.0, 0.5);
        }

        return null;
    }

    /**
     * Checks that the two blocks above a magma block are passable (air/non-solid).
     */
    private boolean isSpaceAboveClear(@NonNull Block magma) {
        Block above1 = magma.getRelative(0, 1, 0);
        Block above2 = magma.getRelative(0, 2, 0);
        return above1.isPassable() && above2.isPassable();
    }

    /**
     * Generates a stable UUID-like key for a block location
     */
    private @NonNull UUID blockLocationKey(@NonNull Block block) {
        long packed = ((long) block.getWorld().hashCode() << 32)
                ^ ((long) block.getX() * 31 + block.getY() * 17L + block.getZ());
        return new UUID(packed, block.getWorld().getUID().getLeastSignificantBits());
    }

    /**
     * Removes cooldown entries older than maxAge ms to keep the map small.
     */
    private void cleanOldEntries(long now, long maxAge) {
        blockCooldowns.entrySet().removeIf(e -> (now - e.getValue()) > maxAge);
    }
}
