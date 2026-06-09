package dev.znxki.zNxPiglinRush.listener;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.api.spawner.SpawnContext;
import dev.znxki.zNxPiglinRush.api.spawner.SpawnStrategy;
import dev.znxki.zNxPiglinRush.config.SpawnerDefinition;
import dev.znxki.zNxPiglinRush.spawner.registry.SpawnerRegistry;
import dev.znxki.zNxPiglinRush.spawner.strategy.StandardSpawnStrategy;
import dev.znxki.zNxPiglinRush.spawner.strategy.WaveSpawnStrategy;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Core event listener. Renamed from {@code PiglinRushListener} to
 * {@code SpawnListener} — describes what it listens to, not the plugin name.
 *
 * <p>Delegates actual spawn logic to the appropriate {@link SpawnStrategy}
 * (standard or wave-mode) so this class stays thin.
 */
public final class SpawnListener implements Listener {

    private final PiglinRushPlugin plugin;
    private final SpawnerRegistry registry;
    private final SpawnStrategy standardStrategy;
    private final SpawnStrategy waveStrategy;

    private final Map<UUID, Long> blockCooldowns = new HashMap<>();

    public SpawnListener(PiglinRushPlugin plugin) {
        this.plugin = plugin;
        this.registry = plugin.getSpawnerRegistry();
        this.standardStrategy = new StandardSpawnStrategy(plugin);
        this.waveStrategy = new WaveSpawnStrategy(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!plugin.getPluginConfig().isEnabled()) return;

        Location spawnLoc = event.getLocation();
        World world = spawnLoc.getWorld();
        if (world == null) return;

        var activeWorlds = plugin.getPluginConfig().getActiveWorlds();
        if (!activeWorlds.isEmpty() && !activeWorlds.contains(world.getName())) return;

        Block below = world.getBlockAt(spawnLoc.getBlockX(), spawnLoc.getBlockY() - 1, spawnLoc.getBlockZ());
        Optional<SpawnerDefinition> defOpt = registry.get(below.getType());
        if (defOpt.isEmpty()) return;

        SpawnerDefinition def = defOpt.get();
        if (event.getEntity().getType() != def.mobType()) return;

        // Cooldown
        UUID key = blockKey(below);
        long now = System.currentTimeMillis();
        long cooldownMs = (long) def.spawnCooldown() * 50L;
        Long last = blockCooldowns.get(key);
        if (last != null && (now - last) < cooldownMs) return;
        blockCooldowns.put(key, now);
        if (blockCooldowns.size() > 500) cleanOldEntries(now, cooldownMs * 10);

        plugin.getSpawnTracker().record(below.getLocation(), def.mobType());

        SpawnContext ctx = new SpawnContext(below, spawnLoc.clone(), world, def);
        (def.waveMode() ? waveStrategy : standardStrategy).onTrigger(ctx);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getHeatmapService().removeViewer(event.getPlayer());
    }

    private UUID blockKey(Block block) {
        long packed = ((long) block.getWorld().hashCode() << 32)
                ^ ((long) block.getX() * 31 + block.getY() * 17 + block.getZ());
        return new UUID(packed, block.getWorld().getUID().getLeastSignificantBits());
    }

    private void cleanOldEntries(long now, long maxAge) {
        blockCooldowns.entrySet().removeIf(e -> (now - e.getValue()) > maxAge);
    }
}
