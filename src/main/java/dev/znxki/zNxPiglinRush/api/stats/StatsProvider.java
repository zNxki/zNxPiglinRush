package dev.znxki.zNxPiglinRush.api.stats;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Read-only view of plugin spawn statistics.
 * Consumed by {@link dev.znxki.zNxPiglinRush.integration.PlaceholderIntegration},
 * {@link dev.znxki.zNxPiglinRush.diagnostics.DiagnosticsService}, and commands.
 */
public interface StatsProvider {

    long getTotalSpawns();

    long getSessionSpawns();

    Instant getSessionStart();

    long getLastHours(int hours);

    default long getLastHour() {
        return getLastHours(1);
    }

    Map<EntityType, Long> getByMob();

    List<Map.Entry<Long, Long>> getTopBlocks(int limit);

    long getBlockCount(Location loc);
}
