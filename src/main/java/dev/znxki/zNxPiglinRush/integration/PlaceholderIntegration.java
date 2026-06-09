package dev.znxki.zNxPiglinRush.integration;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.api.stats.StatsProvider;
import dev.znxki.zNxPiglinRush.diagnostics.DiagnosticsService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for zNxPiglinRush.
 *
 * <p>Moved from {@code hook} to {@code integration} — "integration" is the
 * standard package name for optional third-party hooks in plugin codebases.
 *
 * <p>Available placeholders: {@code %piglinrush_total%}, {@code _session},
 * {@code _last_hour}, {@code _last_24h}, {@code _status}, {@code _tps},
 * {@code _spawners}, {@code _version}.
 */
public final class PlaceholderIntegration extends PlaceholderExpansion {

    private final PiglinRushPlugin plugin;
    private final StatsProvider stats;
    private final DiagnosticsService diagnostics;

    public PlaceholderIntegration(PiglinRushPlugin plugin, StatsProvider stats, DiagnosticsService diagnostics) {
        this.plugin = plugin;
        this.stats = stats;
        this.diagnostics = diagnostics;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "piglinrush";
    }

    @Override
    public @NotNull String getAuthor() {
        return "zNxki_";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return switch (params.toLowerCase()) {
            case "total" -> String.valueOf(stats.getTotalSpawns());
            case "session" -> String.valueOf(stats.getSessionSpawns());
            case "last_hour" -> String.valueOf(stats.getLastHour());
            case "last_24h" -> String.valueOf(stats.getLastHours(24));
            case "status" -> plugin.getPluginConfig().isEnabled() ? "ENABLED" : "DISABLED";
            case "tps" -> String.format("%.1f", diagnostics.getAverageTps());
            case "spawners" -> String.valueOf(plugin.getSpawnerRegistry().size());
            case "version" -> plugin.getPluginMeta().getVersion();
            default -> null;
        };
    }
}
