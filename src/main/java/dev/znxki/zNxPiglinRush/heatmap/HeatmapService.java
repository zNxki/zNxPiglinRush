package dev.znxki.zNxPiglinRush.heatmap;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.api.stats.StatsProvider;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Visualises spawn-density data from {@link StatsProvider} as coloured
 * dust particles above tracked blocks.
 *
 * <p>Renamed from {@code HeatmapManager} to {@code HeatmapService} to match
 * the naming convention used for other singleton service classes.
 */
public final class HeatmapService {

    private static final int TOP_BLOCKS = 200;

    private final PiglinRushPlugin plugin;
    private final StatsProvider stats;
    private final Map<UUID, Integer> activeViewers = new HashMap<>();
    private int taskId = -1;

    public HeatmapService(PiglinRushPlugin plugin, StatsProvider stats) {
        this.plugin = plugin;
        this.stats = stats;
    }

    public boolean toggle(Player player, int radius) {
        UUID id = player.getUniqueId();
        if (activeViewers.containsKey(id)) {
            activeViewers.remove(id);
            if (activeViewers.isEmpty()) stopTask();
            return false;
        }
        activeViewers.put(id, radius);
        startTask();
        return true;
    }

    public void removeViewer(Player player) {
        activeViewers.remove(player.getUniqueId());
        if (activeViewers.isEmpty()) stopTask();
    }

    public void shutdown() {
        stopTask();
        activeViewers.clear();
    }

    private void startTask() {
        if (taskId != -1) return;
        taskId = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::renderAll, 0L, 40L).getTaskId();
    }

    private void stopTask() {
        if (taskId == -1) return;
        plugin.getServer().getScheduler().cancelTask(taskId);
        taskId = -1;
    }

    private void renderAll() {
        if (activeViewers.isEmpty()) {
            stopTask();
            return;
        }
        List<Map.Entry<Long, Long>> topBlocks = stats.getTopBlocks(TOP_BLOCKS);
        if (topBlocks.isEmpty()) return;
        long maxCount = topBlocks.get(0).getValue();
        if (maxCount == 0) return;

        Set<UUID> toRemove = new HashSet<>();
        for (Map.Entry<UUID, Integer> entry : activeViewers.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                toRemove.add(entry.getKey());
                continue;
            }

            Location playerLoc = player.getLocation();
            double r = entry.getValue();

            for (Map.Entry<Long, Long> block : topBlocks) {
                Location loc = unpack(block.getKey(), playerLoc.getWorld());
                if (loc == null || loc.distanceSquared(playerLoc) > r * r) continue;
                double heat = (double) block.getValue() / maxCount;
                Location pLoc = loc.clone().add(0.5, 1.5, 0.5);
                player.spawnParticle(Particle.DUST, pLoc, 4, 0.2, 0.1, 0.2,
                        new Particle.DustOptions(heatColor(heat), 1.5f));
                if (heat > 0.8)
                    player.spawnParticle(Particle.FLAME, pLoc.clone().add(0, 0.3, 0),
                            2, 0.05, 0.05, 0.05, 0.001);
            }
        }
        toRemove.forEach(activeViewers::remove);
    }

    private static Color heatColor(double heat) {
        if (heat < 0.5) {
            double t = heat * 2.0;
            return Color.fromRGB((int) (255 * t), (int) (255 * t), (int) (255 * (1 - t)));
        }
        double t = (heat - 0.5) * 2.0;
        return Color.fromRGB(255, (int) (255 * (1 - t)), 0);
    }

    private static Location unpack(long packed, World world) {
        if (world == null) return null;
        int z = (int) (packed & 0xFFFFF);
        int y = (int) ((packed >> 20) & 0xFFF);
        int x = (int) ((packed >> 32) & 0xFFFFF);
        if ((x & 0x80000) != 0) x |= ~0xFFFFF;
        if ((z & 0x80000) != 0) z |= ~0xFFFFF;
        if ((y & 0x800) != 0) y |= ~0xFFF;
        return new Location(world, x, y, z);
    }
}
