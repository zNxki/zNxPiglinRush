package dev.znxki.zNxPiglinRush.config;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/**
 * Detects outdated {@code config.yml} files and migrates them to the current
 * schema version without overwriting settings the user has already configured.
 *
 * <h3>Versioning scheme</h3>
 * <ul>
 *   <li><b>Version absent / 1</b> — flat keys ({@code spawn-count},
 *       {@code spawn-cooldown}, …). Migrated to v2 by moving those values
 *       into {@code spawners.magma_block.*}.</li>
 *   <li><b>Version 2</b> — current schema with {@code spawners:} section and
 *       {@code diagnostics:} section. Any missing keys are filled from the
 *       bundled default config.</li>
 * </ul>
 *
 * <h3>Strategy</h3>
 * <ol>
 *   <li>Read the user's live {@code config.yml} from disk.</li>
 *   <li>Determine its version from {@code config-version}.</li>
 *   <li>If migration is needed, back up the file as
 *       {@code config_backup_<timestamp>.yml}.</li>
 *   <li>Apply only the transformations required for the detected version gap.</li>
 *   <li>Fill any still-missing keys from the bundled default config.</li>
 *   <li>Write the result back to disk and reload into memory.</li>
 * </ol>
 *
 * <p>User values are <em>never</em> overwritten — only missing keys are added.
 */
public final class ConfigUpdater {
    /**
     * Latest config schema version. Bump this whenever the schema changes.
     */
    public static final int CURRENT_VERSION = 2;

    private static final DateTimeFormatter BACKUP_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final PiglinRushPlugin plugin;
    private final File configFile;

    public ConfigUpdater(PiglinRushPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    /**
     * Runs the migration check. Call this once in {@code onEnable()} after
     * {@code saveDefaultConfig()}, before constructing {@link PluginConfig}.
     *
     * @return {@code true} if the config was modified (and needs re-reading)
     */
    public boolean update() {
        if (!configFile.exists()) return false;

        FileConfiguration live = YamlConfiguration.loadConfiguration(configFile);
        int version = live.getInt("config-version", 1);

        if (version >= CURRENT_VERSION) return fillMissingKeys(live);

        plugin.getLogger().info("[ConfigUpdate] Detected config version " + version + ", migrating to version " + CURRENT_VERSION + "...");
        backup();

        if (version < 2) migrateV1toV2(live);
        fillMissingKeys(live);
        live.set("config-version", CURRENT_VERSION);

        try {
            live.save(configFile);
            plugin.getLogger().info("[ConfigUpdater] Migration complete. Original saved as config_backup_*.yml");
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[ConfigUpdater] Failed to save migrated config.", e);
            return false;
        }
    }

    /**
     * v1 → v2: The old flat keys are moved into {@code spawners.magma_block.*}.
     * Global keys ({@code enabled}, {@code active-worlds}, {@code debug},
     * {@code update-checker}) stay at the root — they existed in v1 too.
     */
    private void migrateV1toV2(FileConfiguration live) {
        plugin.getLogger().info("[ConfigUpdater] Applying v1 → v2: moving flat spawn keys into spawners.magma_block");

        String base = "spawners.magma_block.";

        // Only migrate if the key existed in v1 and the new path is not already set
        migrateKey(live, "spawn-count", base + "spawn-count", 3);
        migrateKey(live, "spawn-cooldown", base + "spawn-cooldown", 20);
        migrateKey(live, "max-nearby-entities", base + "max-nearby-entities", 12);
        migrateKey(live, "scan-radius", base + "scan-radius", 10);
        migrateKey(live, "extra-attempts", base + "extra-attempts", 1);

        // Set defaults for keys that didn't exist in v1
        setIfAbsent(live, base + "mob", "ZOMBIFIED_PIGLIN");
        setIfAbsent(live, base + "wave-mode", false);
        setIfAbsent(live, base + "wave-size", 5);
        setIfAbsent(live, base + "wave-cooldown", 100);

        // Remove the old flat keys (they're now under spawners.*)
        for (String old : new String[]{"spawn-count", "spawn-cooldown", "max-nearby-entities", "scan-radius", "extra-attempts"}) {
            live.set(old, null);
        }
    }

    /**
     * Reads the bundled default config and sets any key that is present in
     * the default but absent in {@code live}. This ensures new config options
     * added in future versions are automatically picked up without a full
     * migration step.
     *
     * @return {@code true} if at least one key was added
     */
    private boolean fillMissingKeys(FileConfiguration live) {
        var defaultStream = plugin.getResource("config.yml");
        if (defaultStream == null) return false;

        FileConfiguration defaults = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(defaultStream, java.nio.charset.StandardCharsets.UTF_8));

        boolean changed = false;
        for (String key : defaults.getKeys(true)) {
            // Skip section nodes (only copy leaf values)
            if (defaults.isConfigurationSection(key)) continue;
            if (!live.contains(key)) {
                live.set(key, defaults.get(key));
                plugin.getLogger().info("[ConfigUpdater] Added missing key: " + key);
                changed = true;
            }
        }

        if (changed) {
            try {
                live.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "[ConfigUpdater] Failed to save after filling missing keys.", e);
            }
        }

        return changed;
    }

    /**
     * Moves a value from {@code oldKey} to {@code newKey}, preserving the
     * user's value. If {@code oldKey} is absent, writes {@code fallback} to
     * {@code newKey} only if {@code newKey} is also absent.
     */
    private void migrateKey(FileConfiguration cfg, String oldKey, String newKey, Object fallback) {
        if (cfg.contains(oldKey)) {
            if (!cfg.contains(newKey)) cfg.set(newKey, cfg.get(oldKey));
        } else {
            setIfAbsent(cfg, newKey, fallback);
        }
    }

    private void setIfAbsent(FileConfiguration cfg, String key, Object value) {
        if (!cfg.contains(key)) cfg.set(key, value);
    }

    /**
     * Creates a timestamped backup of the current config file.
     */
    private void backup() {
        String timestamp = LocalDateTime.now().format(BACKUP_FMT);
        File backup = new File(plugin.getDataFolder(), "config_backup_" + timestamp + ".yml");
        try {
            Files.copy(configFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("[ConfigUpdater] Backup saved: " + backup.getName());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[ConfigUpdater] Could not create backup — proceeding anyway.", e);
        }
    }
}
