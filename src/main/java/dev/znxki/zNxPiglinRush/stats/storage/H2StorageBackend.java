package dev.znxki.zNxPiglinRush.stats.storage;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.api.storage.StorageBackend;
import dev.znxki.zNxPiglinRush.stats.SpawnTracker;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;

/**
 * {@link StorageBackend} implementation backed by an embedded H2 database.
 *
 * <p>Replaces {@code SqliteStorageBackend} — identical schema and behaviour,
 * but H2 ships without platform-native binaries so the shaded jar stays
 * under ~2 MB instead of ~13 MB.
 *
 * <p>The database file is stored at
 * {@code plugins/zNxPiglinRush/stats} (H2 appends {@code .mv.db}).
 *
 * <p>Schema:
 * <pre>
 * stats_total (id INT PK, total_spawns BIGINT NOT NULL DEFAULT 0)
 * </pre>
 *
 * <p>{@link #flush()} is called from a background thread every 5 minutes
 * and once on shutdown. {@link #open()} and {@link #loadTotalSpawns()} run
 * on the main thread during plugin enable.
 */
public final class H2StorageBackend implements StorageBackend {

    private final PiglinRushPlugin plugin;
    private final SpawnTracker tracker;
    private Connection connection;

    public H2StorageBackend(PiglinRushPlugin plugin, SpawnTracker tracker) {
        this.plugin  = plugin;
        this.tracker = tracker;
    }

    @Override
    public void open() {
        try {
            plugin.getDataFolder().mkdirs();
            File dbFile = new File(plugin.getDataFolder(), "stats");

            String url = "jdbc:h2:file:" + dbFile.getAbsolutePath()
                    + ";AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE";

            connection = DriverManager.getConnection(url, "sa", "");

            try (Statement st = connection.createStatement()) {
                st.execute(
                        "CREATE TABLE IF NOT EXISTS stats_total (" +
                                "  id         INT         PRIMARY KEY," +
                                "  total_spawns BIGINT NOT NULL DEFAULT 0" +
                                ")"
                );

                st.execute(
                        "MERGE INTO stats_total (id, total_spawns) KEY(id) VALUES (1, 0)"
                );
            }

            tracker.seedTotal(loadTotalSpawns());

            // Periodic background flush every 5 minutes
            plugin.getSchedulerService().runAsync(this::periodicFlush);

            plugin.getLogger().info("[H2StorageBackend] Database initialised.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "[H2StorageBackend] Failed to initialise — stats will not persist.", e);
        }
    }

    @Override
    public void close() {
        flush();
        try {
            if (connection != null && !connection.isClosed()) {
                try (Statement st = connection.createStatement()) {
                    st.execute("SHUTDOWN COMPACT");
                } catch (SQLException ignored) {}
                connection.close();
            }
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
            plugin.getLogger().log(Level.WARNING, "[H2StorageBackend] Flush failed.", e);
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
            plugin.getLogger().log(Level.WARNING, "[H2StorageBackend] Load failed.", e);
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
