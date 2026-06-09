package dev.znxki.zNxPiglinRush.storage;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.api.storage.StorageBackend;
import dev.znxki.zNxPiglinRush.config.PluginConfig;
import dev.znxki.zNxPiglinRush.stats.SpawnTracker;
import dev.znxki.zNxPiglinRush.stats.storage.H2StorageBackend;
import dev.znxki.zNxPiglinRush.stats.storage.JsonStorageBackend;
import dev.znxki.zNxPiglinRush.stats.storage.MySQLStorageBackend;
import dev.znxki.zNxPiglinRush.stats.storage.YamlStorageBackend;
import org.jspecify.annotations.NonNull;

/**
 * Creates the appropriate {@link StorageBackend} based on the configured
 * {@link PluginConfig.StorageType}.
 *
 * <p>HikariCP and JDBC drivers are already on the classpath when this method
 * is called because {@link StorageLibraryLoader} (declared via {@code loader:}
 * in {@code paper-plugin.yml}) is executed by Paper before {@code onEnable()}.
 */
public final class StorageBackendFactory {
    private StorageBackendFactory() {
    }

    /**
     * @param plugin  plugin instance
     * @param tracker spawn tracker to seed / flush
     * @param type    storage type from config
     * @return a ready-to-use backend for {@link StorageBackend#open()}
     */
    public static @NonNull StorageBackend create(PiglinRushPlugin plugin, SpawnTracker tracker, PluginConfig.@NonNull StorageType type) {
        return switch (type) {
            case YAML -> new YamlStorageBackend(plugin, tracker);
            case JSON -> new JsonStorageBackend(plugin, tracker);
            case H2 -> new H2StorageBackend(plugin, tracker);
            case MYSQL -> new MySQLStorageBackend(plugin, tracker, false);
            case MARIADB -> new MySQLStorageBackend(plugin, tracker, true);
        };
    }
}
