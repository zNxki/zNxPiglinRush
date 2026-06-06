package dev.znxki.zNxPiglinRush.events;

import com.google.common.collect.Maps;
import dev.znxki.zNxPiglinRush.ZNxPiglinRush;
import dev.znxki.zNxPiglinRush.scheduler.SchedulerAdapter;
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

import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Listens for vanilla ZombifiedPiglin spawns triggered by magma blocks
 * and fires additional spawns around the same location.
 * <p>
 * Folia notes:
 * EntitySpawnEvent is fired on the region thread that owns the chunk where
 * the entity spawns, so all block/world reads inside the handler are safe.
 * Extra spawns are deferred via {@link SchedulerAdapter#runAtLocation} which
 * routes to the correct region scheduler on Folia (or BukkitScheduler on
 * Spigot/Paper), ensuring thread safety in both environments.
 */
public class PiglinRushListener implements Listener {

    private final ZNxPiglinRush plugin;
    private final Random random = new Random();

    /**
     * Tracks last spawn time per magma block to enforce cooldown.
     * Access is always from the owning region thread (event + deferred task
     * both execute on the same region), so no synchronisation is needed on Folia.
     * On Spigot/Paper it's always the single main thread.
     */
    private final Map<UUID, Long> blockCooldowns = Maps.newHashMap();

    public PiglinRushListener(ZNxPiglinRush plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof PigZombie)) return;

        var cfg = plugin.getMagmaConfig();
        if (!cfg.isEnabled()) return;

        Location spawnLoc = event.getLocation();
        World world = spawnLoc.getWorld();
        if (world == null) return;

        // World whitelist check
        if (!cfg.getActiveWorlds().isEmpty()
                && !cfg.getActiveWorlds().contains(world.getName())) return;

        // Verify the block under the piglin is magma
        Block below = world.getBlockAt(
                spawnLoc.getBlockX(),
                spawnLoc.getBlockY() - 1,
                spawnLoc.getBlockZ()
        );
        if (below.getType() != Material.MAGMA_BLOCK) return;

        // Cooldown check
        long now = System.currentTimeMillis();
        UUID blockKey = blockLocationKey(below);
        Long lastSpawn = blockCooldowns.get(blockKey);
        long cooldownMs = (long) cfg.getSpawnCooldown() * 50L; // ticks → ms

        if (lastSpawn != null && (now - lastSpawn) < cooldownMs) {
            if (cfg.isDebug())
                plugin.getLogger().info("[debug] Cooldown active for block " + below.getLocation());
            return;
        }

        blockCooldowns.put(blockKey, now);
        if (blockCooldowns.size() > 500) cleanOldEntries(now, cooldownMs * 10);

        // Calculate how many extra piglins to spawn
        int totalExtra = (cfg.getSpawnCount() - 1) // -1: vanilla already spawned 1
                + random.nextInt(cfg.getExtraAttempts() + 1);

        if (totalExtra <= 0) return;

        // Entity cap check
        int nearbyCount = world.getNearbyEntitiesByType(
                PigZombie.class,
                spawnLoc,
                cfg.getScanRadius()
        ).size();

        if (nearbyCount >= cfg.getMaxNearbyEntities()) {
            if (cfg.isDebug())
                plugin.getLogger().info("[debug] Entity cap reached (" + nearbyCount + "), skipping.");
            return;
        }

        int allowed = Math.min(totalExtra, cfg.getMaxNearbyEntities() - nearbyCount);

        if (cfg.isDebug())
            plugin.getLogger().info("[debug] Spawning " + allowed + " extra Zombified Piglins at " + spawnLoc);

        // Snapshot values for the lambda — the location carries the world reference
        // which Folia uses to determine the owning region for the scheduled task.
        Location originSnap = spawnLoc.clone();
        Block belowSnap = below; // Block is position-only, safe to capture

        // runAtLocation schedules on the correct region thread (Folia) or
        // next main-thread tick (Spigot/Paper), deferring by 1 tick so we
        // don't interfere with the current EntitySpawnEvent.
        SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        scheduler.runAtLocation(originSnap, () -> {
            for (int i = 0; i < allowed; i++) {
                Location loc = findSpawnLocation(world, belowSnap, cfg.getScanRadius());
                if (loc == null) continue;
                world.spawnEntity(loc, EntityType.ZOMBIFIED_PIGLIN);
            }
        });
    }

    /**
     * Finds a valid spawn location on a nearby magma block.
     * Falls back to directly above the origin if no other magma block is found.
     */
    private Location findSpawnLocation(World world, Block originMagma, int radius) {
        for (int attempt = 0; attempt < 8; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int dy = random.nextInt(3) - 1; // ±1 Y

            Block candidate = world.getBlockAt(
                    originMagma.getX() + dx,
                    originMagma.getY() + dy,
                    originMagma.getZ() + dz
            );

            if (candidate.getType() == Material.MAGMA_BLOCK && isSpaceAboveClear(candidate)) {
                return candidate.getLocation().add(0.5, 1.0, 0.5);
            }
        }

        if (isSpaceAboveClear(originMagma)) {
            return originMagma.getLocation().add(0.5, 1.0, 0.5);
        }

        return null;
    }

    private boolean isSpaceAboveClear(@NonNull Block magma) {
        return magma.getRelative(0, 1, 0).isPassable()
                && magma.getRelative(0, 2, 0).isPassable();
    }

    private @NonNull UUID blockLocationKey(Block block) {
        long packed = ((long) block.getWorld().hashCode() << 32)
                ^ ((long) block.getX() * 31 + block.getY() * 17 + block.getZ());
        return new UUID(packed, block.getWorld().getUID().getLeastSignificantBits());
    }

    private void cleanOldEntries(long now, long maxAge) {
        blockCooldowns.entrySet().removeIf(e -> (now - e.getValue()) > maxAge);
    }
}
