package dev.znxki.zNxPiglinRush.stats.storage;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.api.storage.StorageBackend;
import dev.znxki.zNxPiglinRush.stats.SpawnTracker;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;

/**
 * {@link StorageBackend} implementation backed by a local SQLite database.
 *
 * <p>Schema:
 * <pre>
 * stats_total  (id PK, total_spawns INTEGER)
 * stats_hourly (hour_slot PK, spawns INTEGER)
 * </pre>
 *
 * {@link #flush()} is called from a background thread every 5 minutes and
 * once on shutdown. {@link #open()} and {@link #loadTotalSpawns()} are
 * called from the main thread during plugin enable.
 */
public final class SqliteStorageBackend implements StorageBackend {

    private final PiglinRushPlugin plugin;
    private final SpawnTracker tracker;
    private Connection connection;

    public SqliteStorageBackend(PiglinRushPlugin plugin, SpawnTracker tracker) {
        this.plugin  = plugin;
        this.tracker = tracker;
    }

    @Override
    public void open() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "stats.db");
            plugin.getDataFolder().mkdirs();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement st = connection.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS stats_total (id INTEGER PRIMARY KEY, total_spawns INTEGER NOT NULL DEFAULT 0)");
                st.execute("CREATE TABLE IF NOT EXISTS stats_hourly (hour_slot INTEGER PRIMARY KEY, spawns INTEGER NOT NULL DEFAULT 0)");
                st.execute("INSERT OR IGNORE INTO stats_total (id, total_spawns) VALUES (1, 0)");
            }

            // Seed in-memory total from persisted value
            tracker.seedTotal(loadTotalSpawns());

            // Background flush every 5 minutes
            plugin.getSchedulerService().runAsync(this::periodicFlush);

            plugin.getLogger().info("[SqliteStorageBackend] Database initialised.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[SqliteStorageBackend] Failed to initialise — stats will not persist.", e);
        }
    }

    @Override
    public void close() {
        flush();
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {}
    }

    @Override
    public void flush() {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE stats_total SET total_spawns = ? WHERE id = 1")) {
            ps.setLong(1, tracker.getTotalSpawns());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[SqliteStorageBackend] Flush failed.", e);
        }
    }

    @Override
    public long loadTotalSpawns() {
        if (connection == null) return 0;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT total_spawns FROM stats_total WHERE id = 1")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("total_spawns");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[SqliteStorageBackend] Load failed.", e);
        }
        return 0;
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
