package dev.znxki.zNxPiglinRush.stats.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.api.storage.StorageBackend;
import dev.znxki.zNxPiglinRush.stats.SpawnTracker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * {@link StorageBackend} that persists spawn statistics in a JSON file
 * ({@code plugins/zNxPiglinRush/stats.json}).
 *
 * <p>Uses Gson, which is already provided at runtime by Paper (no extra shading).
 */
public final class JsonStorageBackend implements StorageBackend {
    private static final Gson GSON = new Gson();

    private final PiglinRushPlugin plugin;
    private final SpawnTracker tracker;
    private File statsFile;

    public JsonStorageBackend(PiglinRushPlugin plugin, SpawnTracker tracker) {
        this.plugin = plugin;
        this.tracker = tracker;
    }

    @Override
    public void open() {
        plugin.getDataFolder().mkdirs();
        statsFile = new File(plugin.getDataFolder(), "stats.json");

        if (!statsFile.exists()) write(0L);

        tracker.seedTotal(loadTotalSpawns());
        plugin.getSchedulerService().runAsync(this::periodicFlush);
        plugin.getLogger().info("[JsonStorageBackend] Initialised.");
    }

    @Override
    public void close() {
        flush();
    }

    @Override
    public void flush() {
        if (statsFile == null) return;
        write(tracker.getTotalSpawns());
    }

    @Override
    public long loadTotalSpawns() {
        if (statsFile == null || !statsFile.exists()) return 0;

        try (Reader r = new InputStreamReader(new FileInputStream(statsFile), StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            return obj.has("total_spawns") ? obj.get("total_spawns").getAsLong() : 0L;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[JsonStorageBackend] Failed to read stats.json.", e);
            return 0;
        }
    }

    private void write(long total) {
        JsonObject obj = new JsonObject();
        obj.addProperty("total_spawns", total);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(statsFile), StandardCharsets.UTF_8)) {
            GSON.toJson(obj, w);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[JsonStorageBackend] Failed to write stats.json.", e);
        }
    }

    private void periodicFlush() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(300_000L);
                flush();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
