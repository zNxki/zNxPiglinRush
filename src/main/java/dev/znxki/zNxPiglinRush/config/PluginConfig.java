package dev.znxki.zNxPiglinRush.config;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;

import java.util.List;

/**
 * Typed wrapper for the global (non-per-block) plugin settings.
 * Per-block settings are held in {@link SpawnerDefinition} instances
 * managed by {@link dev.znxki.zNxPiglinRush.spawner.registry.SpawnerRegistry}.
 */
public final class PluginConfig {

    private final boolean enabled;
    private final List<String> activeWorlds;
    private final boolean debug;
    private final boolean updateCheckerEnabled;

    public PluginConfig(PiglinRushPlugin plugin) {
        var cfg = plugin.getConfig();
        this.enabled = cfg.getBoolean("enabled", true);
        this.activeWorlds = cfg.getStringList("active-worlds");
        this.debug = cfg.getBoolean("debug", false);
        this.updateCheckerEnabled = cfg.getBoolean("update-checker", true);
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
}
