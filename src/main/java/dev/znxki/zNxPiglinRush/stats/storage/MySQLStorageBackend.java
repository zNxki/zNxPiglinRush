package dev.znxki.zNxPiglinRush.stats.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.api.storage.StorageBackend;
import dev.znxki.zNxPiglinRush.config.PluginConfig;
import dev.znxki.zNxPiglinRush.stats.SpawnTracker;

import java.sql.*;
import java.util.logging.Level;

/**
 * {@link StorageBackend} backed by a remote MySQL or MariaDB database.
 *
 * <p>Uses HikariCP as the connection pool, downloaded at startup via the
 * Paper {@code LibraryLoader} (see {@link dev.znxki.zNxPiglinRush.storage.StorageLibraryLoader}).
 * No JDBC driver or Hikari jars are shaded into the plugin jar.
 *
 * <p>The JDBC driver ({@code mysql-connector-j} or {@code mariadb-java-client})
 * is also resolved at runtime through the same mechanism.
 */
public final class MySQLStorageBackend implements StorageBackend {
    private final PiglinRushPlugin plugin;
    private final SpawnTracker tracker;
    private final boolean isMariaDB;
    private HikariDataSource dataSource;

    public MySQLStorageBackend(PiglinRushPlugin plugin, SpawnTracker tracker, boolean isMariaDB) {
        this.plugin = plugin;
        this.tracker = tracker;
        this.isMariaDB = isMariaDB;
    }

    @Override
    public void open() {
        PluginConfig cfg = plugin.getPluginConfig();
        String label = isMariaDB ? "MariaDB" : "MySQL";

        try {
            HikariConfig hk = new HikariConfig();

            if (isMariaDB) {
                hk.setDriverClassName("org.mariadb.jdbc.Driver");
                hk.setJdbcUrl("jdbc:mariadb://" + cfg.getDbHost() + ":" + cfg.getDbPort()
                        + "/" + cfg.getDbName()
                        + "?useSSL=false&characterEncoding=utf8");
            } else {
                hk.setDriverClassName("com.mysql.cj.jdbc.Driver");
                hk.setJdbcUrl("jdbc:mysql://" + cfg.getDbHost() + ":" + cfg.getDbPort()
                        + "/" + cfg.getDbName()
                        + "?useSSL=false&characterEncoding=utf8&serverTimezone=UTC");
            }

            hk.setUsername(cfg.getDbUsername());
            hk.setPassword(cfg.getDbPassword());
            hk.setMaximumPoolSize(cfg.getDbPoolSize());
            hk.setConnectionTimeout(cfg.getDbConnectionTimeout());
            hk.setPoolName("zNxPiglinRush-" + label);

            hk.addDataSourceProperty("cachePrepStmts", "true");
            hk.addDataSourceProperty("prepStmtCacheSize", "10");
            hk.addDataSourceProperty("prepStmtCacheSqlLimit", "256");
            hk.addDataSourceProperty("useServerPrepStmts", "true");

            dataSource = new HikariDataSource(hk);

            try (Connection conn = dataSource.getConnection();
                 Statement st = conn.createStatement()) {
                st.execute(
                        "CREATE TABLE IF NOT EXISTS stats_total (" +
                                "  id           INT    PRIMARY KEY," +
                                "  total_spawns BIGINT NOT NULL DEFAULT 0" +
                                ")"
                );
                st.execute(
                        "INSERT IGNORE INTO stats_total (id, total_spawns) VALUES (1, 0)"
                );
            }

            tracker.seedTotal(loadTotalSpawns());
            plugin.getSchedulerService().runAsync(this::periodicFlush);
            plugin.getLogger().info("[" + label + "StorageBackend] Connected and initialised.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                    "[" + label + "StorageBackend] Failed to initialise — stats will not persist.", e);
        }
    }

    @Override
    public void close() {
        flush();
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public void flush() {
        if (dataSource == null || dataSource.isClosed()) return;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE stats_total SET total_spawns = ? WHERE id = 1")) {
            ps.setLong(1, tracker.getTotalSpawns());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[MySQLStorageBackend] Flush failed.", e);
        }
    }

    @Override
    public long loadTotalSpawns() {
        if (dataSource == null || dataSource.isClosed()) return 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT total_spawns FROM stats_total WHERE id = 1")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("total_spawns");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[MySQLStorageBackend] Load failed.", e);
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
