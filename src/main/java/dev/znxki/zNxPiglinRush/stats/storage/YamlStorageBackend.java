package dev.znxki.zNxPiglinRush.stats.storage;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.api.storage.StorageBackend;
import dev.znxki.zNxPiglinRush.stats.SpawnTracker;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * {@link StorageBackend} that persists spawn statistics in a simple YAML file
 * ({@code plugins/zNxPiglinRush/stats.yml}).
 *
 * <p>No extra libraries required — uses the Bukkit YAML API that is already
 * on the classpath via paper-api.
 */
public final class YamlStorageBackend implements StorageBackend {

    private final PiglinRushPlugin plugin;
    private final SpawnTracker tracker;
    private File statsFile;
    private YamlConfiguration yaml;

    public YamlStorageBackend(PiglinRushPlugin plugin, SpawnTracker tracker) {
        this.plugin = plugin;
        this.tracker = tracker;
    }

    @Override
    public void open() {
        plugin.getDataFolder().mkdirs();
        statsFile = new File(plugin.getDataFolder(), "stats.yml");

        if (!statsFile.exists()) {
            yaml = new YamlConfiguration();
            yaml.set("total_spawns", 0L);
            save();
        } else {
            yaml = YamlConfiguration.loadConfiguration(statsFile);
        }

        tracker.seedTotal(loadTotalSpawns());
        plugin.getSchedulerService().runAsync(this::periodicFlush);
        plugin.getLogger().info("[YamlStorageBackend] Initialised.");
    }

    @Override
    public void close() {
        flush();
    }

    @Override
    public void flush() {
        if (yaml == null) return;
        yaml.set("total_spawns", tracker.getTotalSpawns());
        save();
    }

    @Override
    public long loadTotalSpawns() {
        if (yaml == null) return 0;
        return yaml.getLong("total_spawns", 0L);
    }

    private void save() {
        try {
            yaml.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[YamlStorageBackend] Failed to save stats.yml.", e);
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
