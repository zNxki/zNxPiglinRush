package dev.znxki.zNxPiglinRush.stats;

import dev.znxki.zNxPiglinRush.api.stats.StatsProvider;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe, in-memory implementation of {@link StatsProvider}.
 *
 * <p>Renamed from {@code SpawnStats} to {@code SpawnTracker} — "Tracker"
 * better conveys that this class actively records data, not just holds it.
 *
 * <p>All counters use {@link AtomicLong} for lock-free concurrent writes
 * from multiple region threads (Folia) and the async update-checker thread.
 */
public final class SpawnTracker implements StatsProvider {

    private final AtomicLong totalSpawns = new AtomicLong(0);
    private final AtomicLong sessionSpawns = new AtomicLong(0);
    private final Instant sessionStart = Instant.now();

    private final ConcurrentHashMap<EntityType, AtomicLong> byMob = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicLong> blockHeatmap = new ConcurrentHashMap<>();

    private final long[] hourlyRing = new long[24];
    private int currentHourSlot;

    public SpawnTracker() {
        currentHourSlot = currentHour();
    }

    public void record(Location blockLoc, EntityType mob) {
        totalSpawns.incrementAndGet();
        sessionSpawns.incrementAndGet();
        byMob.computeIfAbsent(mob, k -> new AtomicLong()).incrementAndGet();
        advanceHourIfNeeded();
        synchronized (hourlyRing) {
            hourlyRing[currentHourSlot]++;
        }
        blockHeatmap.computeIfAbsent(packLocation(blockLoc), k -> new AtomicLong()).incrementAndGet();
    }

    /**
     * Seeds the total counter from a persisted value on startup.
     */
    public void seedTotal(long persisted) {
        totalSpawns.set(persisted);
    }

    public void resetSession() {
        sessionSpawns.set(0);
    }

    @Override
    public long getTotalSpawns() {
        return totalSpawns.get();
    }

    @Override
    public long getSessionSpawns() {
        return sessionSpawns.get();
    }

    @Override
    public Instant getSessionStart() {
        return sessionStart;
    }

    @Override
    public long getLastHours(int hours) {
        advanceHourIfNeeded();
        int cap = Math.min(hours, 24);
        long sum = 0;
        synchronized (hourlyRing) {
            for (int i = 0; i < cap; i++)
                sum += hourlyRing[((currentHourSlot - i) + 24) % 24];
        }
        return sum;
    }

    @Override
    public @NonNull @UnmodifiableView Map<EntityType, Long> getByMob() {
        Map<EntityType, Long> result = new LinkedHashMap<>();
        byMob.forEach((k, v) -> result.put(k, v.get()));
        return Collections.unmodifiableMap(result);
    }

    @Override
    public List<Map.Entry<Long, Long>> getTopBlocks(int limit) {
        List<Map.Entry<Long, Long>> list = new ArrayList<>();
        blockHeatmap.forEach((k, v) -> list.add(Map.entry(k, v.get())));
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return list.subList(0, Math.min(limit, list.size()));
    }

    @Override
    public long getBlockCount(Location loc) {
        AtomicLong v = blockHeatmap.get(packLocation(loc));
        return v == null ? 0 : v.get();
    }

    private void advanceHourIfNeeded() {
        int nowHour = currentHour();
        if (nowHour == currentHourSlot) return;
        synchronized (hourlyRing) {
            int steps = (nowHour - currentHourSlot + 24) % 24;
            for (int i = 1; i <= steps; i++)
                hourlyRing[(currentHourSlot + i) % 24] = 0;
            currentHourSlot = nowHour;
        }
    }

    private static int currentHour() {
        return (int) ((System.currentTimeMillis() / 3_600_000L) % 24);
    }

    public static long packLocation(Location loc) {
        long wx = (long) (loc.getBlockX() & 0xFFFFF);
        long wy = (long) (loc.getBlockY() & 0xFFF);
        long wz = (long) (loc.getBlockZ() & 0xFFFFF);
        long wh = (long) (loc.getWorld() != null ? loc.getWorld().hashCode() & 0xFFFF : 0);
        return (wh << 52) | (wx << 32) | (wy << 20) | wz;
    }
}
