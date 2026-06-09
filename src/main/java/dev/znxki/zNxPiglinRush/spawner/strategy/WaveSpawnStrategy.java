package dev.znxki.zNxPiglinRush.spawner.strategy;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.api.spawner.SpawnContext;
import dev.znxki.zNxPiglinRush.api.spawner.SpawnStrategy;
import dev.znxki.zNxPiglinRush.config.SpawnerDefinition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Wave-mode {@link SpawnStrategy}: accumulates trigger credits and fires
 * a scaled burst every {@code waveCooldownTicks} ticks.
 *
 * <p>Wave size grows with consecutive waves (bonus every 3 waves, capped
 * at {@code waveSize * 2}) to create a progressively more dramatic effect.
 */
public final class WaveSpawnStrategy implements SpawnStrategy {

    private final PiglinRushPlugin plugin;
    private final Map<UUID, WaveState> states = new HashMap<>();
    private final Random random = new Random();

    public WaveSpawnStrategy(PiglinRushPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onTrigger(SpawnContext ctx) {
        UUID key = blockKey(ctx.triggerBlock());
        WaveState state = states.computeIfAbsent(key, k -> new WaveState());
        state.credits++;

        if (!state.scheduled) {
            state.scheduled = true;
            plugin.getSchedulerService().runAtLocation(
                    ctx.spawnLocation(), () -> fire(key, ctx));
        }
    }

    @Override
    public void cleanup() {
        states.clear();
    }

    private void fire(UUID key, SpawnContext ctx) {
        WaveState state = states.get(key);
        if (state == null) return;

        SpawnerDefinition def = ctx.definition();
        int size = Math.min(state.credits * def.spawnCount(), def.waveSize());
        state.credits = 0;
        state.scheduled = false;
        state.consecutiveWaves++;

        int bonus = state.consecutiveWaves / 3;
        int scaled = Math.min(size + bonus, def.waveSize() * 2);

        World world = ctx.world();
        long nearby = world.getNearbyEntitiesByType(Entity.class, ctx.spawnLocation(), def.scanRadius())
                .stream().filter(e -> e.getType() == def.mobType()).count();
        int allowed = (int) Math.min(scaled, def.maxNearbyEntities() - nearby);
        if (allowed <= 0) return;

        for (int i = 0; i < allowed; i++) {
            Location loc = SpawnLocationResolver.find(world, ctx.triggerBlock(), def, random);
            if (loc != null) {
                world.spawnEntity(loc, def.mobType());
                plugin.getSpawnTracker().record(ctx.triggerBlock().getLocation(), def.mobType());
            }
        }
    }

    private static UUID blockKey(org.bukkit.block.Block block) {
        long packed = ((long) block.getWorld().hashCode() << 32)
                ^ ((long) block.getX() * 31 + block.getY() * 17L + block.getZ());
        return new UUID(packed, block.getWorld().getUID().getLeastSignificantBits());
    }

    private static class WaveState {
        int credits = 0;
        boolean scheduled = false;
        int consecutiveWaves = 0;
    }
}
