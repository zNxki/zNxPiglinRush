package dev.znxki.zNxPiglinRush.spawner.strategy;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.api.spawner.SpawnContext;
import dev.znxki.zNxPiglinRush.api.spawner.SpawnStrategy;
import dev.znxki.zNxPiglinRush.config.SpawnerDefinition;
import dev.znxki.zNxPiglinRush.spawner.loot.LootInjector;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.Random;

/**
 * Default {@link SpawnStrategy}: immediately spawns {@code spawnCount - 1}
 * extra mobs (the vanilla trigger already spawned one) scattered across
 * nearby matching blocks.
 */
public final class StandardSpawnStrategy implements SpawnStrategy {

    private final PiglinRushPlugin plugin;
    private final Random random = new Random();

    public StandardSpawnStrategy(PiglinRushPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onTrigger(SpawnContext ctx) {
        SpawnerDefinition def = ctx.definition();
        World world = ctx.world();

        int totalExtra = (def.spawnCount() - 1) + random.nextInt(def.extraAttempts() + 1);
        if (totalExtra <= 0) return;

        long nearby = world.getNearbyEntitiesByType(Entity.class, ctx.spawnLocation(), def.scanRadius())
                .stream().filter(e -> e.getType() == def.mobType()).count();
        if (nearby >= def.maxNearbyEntities()) return;

        int allowed = (int) Math.min(totalExtra, def.maxNearbyEntities() - nearby);

        plugin.getSchedulerService().runAtLocation(ctx.spawnLocation(), () -> {
            for (int i = 0; i < allowed; i++) {
                Location loc = SpawnLocationResolver.find(world, ctx.triggerBlock(), def, random);
                if (loc == null) continue;
                Entity spawned = world.spawnEntity(loc, def.mobType());
                LootInjector.tag(spawned, def.block(), plugin);
                plugin.getSpawnTracker().record(ctx.triggerBlock().getLocation(), def.mobType());
            }
        });
    }
}
