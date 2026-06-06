package dev.znxki.zNxPiglinRush.config;

import dev.znxki.zNxPiglinRush.ZNxPiglinRush;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class PiglinRushConfig {
    private final boolean enabled;
    private final int spawnCount;
    private final int spawnCooldown;
    private final int maxNearbyEntities;
    private final int scanRadius;
    private final int extraAttempts;
    private final List<String> activeWorlds;
    private final boolean debug;
    private final boolean updateChecker;

    public PiglinRushConfig(@NonNull ZNxPiglinRush plugin) {
        var cfg = plugin.getConfig();
        this.enabled = cfg.getBoolean("enabled", true);
        this.spawnCount = Math.max(1, cfg.getInt("spawn-count", 3));
        this.spawnCooldown = Math.max(1, cfg.getInt("spawn-cooldown", 20));
        this.maxNearbyEntities = Math.max(1, cfg.getInt("max-nearby-entities", 12));
        this.scanRadius = Math.max(1, cfg.getInt("scan-radius", 10));
        this.extraAttempts = Math.max(0, cfg.getInt("extra-attempts", 1));
        this.activeWorlds = cfg.getStringList("active-worlds");
        this.debug = cfg.getBoolean("debug", false);
        this.updateChecker = cfg.getBoolean("update-checker", true);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getSpawnCount() {
        return spawnCount;
    }

    public int getSpawnCooldown() {
        return spawnCooldown;
    }

    public int getMaxNearbyEntities() {
        return maxNearbyEntities;
    }

    public int getScanRadius() {
        return scanRadius;
    }

    public int getExtraAttempts() {
        return extraAttempts;
    }

    public List<String> getActiveWorlds() {
        return activeWorlds;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isUpdateChecker() {
        return updateChecker;
    }
}
