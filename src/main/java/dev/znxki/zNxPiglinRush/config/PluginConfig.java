package dev.znxki.zNxPiglinRush.config;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;

import java.util.List;

/**
 * Typed wrapper for the global (non-per-block) plugin settings.
 * Per-block settings are held in {@link SpawnerDefinition} instances
 * managed by {@link dev.znxki.zNxPiglinRush.spawner.registry.SpawnerRegistry}.
 */
public final class PluginConfig {
    public enum StorageType {
        YAML, JSON, H2, MYSQL, MARIADB;

        public static StorageType parse(String raw) {
            try {
                return valueOf(raw.toUpperCase());
            } catch (IllegalArgumentException e) {
                return H2;
            }
        }

        /**
         * Returns true if this type requires a remote JDBC connection + HikariCP.
         */
        public boolean isRemote() {
            return this == MYSQL || this == MARIADB;
        }
    }

    private final boolean enabled;
    private final List<String> activeWorlds;
    private final boolean debug;
    private final boolean updateCheckerEnabled;

    private final StorageType storageType;

    private final String dbHost;
    private final int dbPort;
    private final String dbName;
    private final String dbUsername;
    private final String dbPassword;
    private final int dbPoolSize;
    private final int dbConnectionTimeout;

    public PluginConfig(PiglinRushPlugin plugin) {
        var cfg = plugin.getConfig();
        this.enabled = cfg.getBoolean("enabled", true);
        this.activeWorlds = cfg.getStringList("active-worlds");
        this.debug = cfg.getBoolean("debug", false);
        this.updateCheckerEnabled = cfg.getBoolean("update-checker", true);

        this.storageType = StorageType.parse(cfg.getString("storage.type", "H2"));

        this.dbHost = cfg.getString("storage.database.host", "localhost");
        this.dbPort = cfg.getInt("storage.database.port", 3306);
        this.dbName = cfg.getString("storage.database.name", "minecraft");
        this.dbUsername = cfg.getString("storage.database.username", "root");
        this.dbPassword = cfg.getString("storage.database.password", "");
        this.dbPoolSize = cfg.getInt("storage.database.pool-size", 5);
        this.dbConnectionTimeout = cfg.getInt("storage.database.connection-timeout", 5000);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getActiveWorlds() {
        return activeWorlds;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isUpdateCheckerEnabled() {
        return updateCheckerEnabled;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public String getDbHost() {
        return dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public int getDbPoolSize() {
        return dbPoolSize;
    }

    public int getDbConnectionTimeout() {
        return dbConnectionTimeout;
    }
}
