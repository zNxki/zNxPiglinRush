package dev.znxki.zNxPiglinRush.update;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.logging.Level;

/**
 * Orchestrates the async update check and in-game notifications.
 * HTTP logic lives in {@link ModrinthUpdateSource}.
 */
public final class UpdateChecker implements Listener {

    private static final String PROJECT_ID = "znxpiglinrush";
    private static final String MODRINTH_URL = "https://modrinth.com/plugin/" + PROJECT_ID;

    private final PiglinRushPlugin plugin;
    private final ModrinthUpdateSource source;
    private final String currentVersion;

    private volatile UpdateStatus status = UpdateStatus.PENDING;
    private volatile String latestVersion;
    private volatile String latestVersionUrl;

    public UpdateChecker(PiglinRushPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getPluginMeta().getVersion();
        this.source = new ModrinthUpdateSource(PROJECT_ID,
                "zNxPiglinRush/" + currentVersion + " (zNxki_; " + MODRINTH_URL + ")");
    }

    public void check() {
        if (!plugin.getPluginConfig().isUpdateCheckerEnabled()) return;
        plugin.getSchedulerService().runAsync(() -> {
            try {
                var result = source.fetchLatest(plugin.getServer().getMinecraftVersion());
                switch (result.status()) {
                    case FOUND -> {
                        latestVersion = result.versionNumber();
                        latestVersionUrl = result.versionUrl();
                        if (isNewer(latestVersion, currentVersion)) {
                            status = UpdateStatus.UPDATE_AVAILABLE;
                            plugin.getLogger().warning("[UpdateChecker] Update available! "
                                    + currentVersion + " → " + latestVersion);
                        } else {
                            status = UpdateStatus.UP_TO_DATE;
                            plugin.getLogger().info("[UpdateChecker] Up to date (" + currentVersion + ").");
                        }
                    }
                    case NOT_FOUND -> {
                        status = UpdateStatus.ERROR;
                        plugin.getLogger().warning("[UpdateChecker] Project not found on Modrinth. Set the correct PROJECT_ID.");
                    }
                    case NO_RESULTS -> status = UpdateStatus.UP_TO_DATE;
                    case ERROR -> {
                        status = UpdateStatus.ERROR;
                        plugin.getLogger().warning("[UpdateChecker] " + result.errorMessage());
                    }
                }
            } catch (Exception e) {
                status = UpdateStatus.ERROR;
                plugin.getLogger().log(Level.WARNING, "[UpdateChecker] Request failed.", e);
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getSchedulerService().runDelayedGlobal(() -> notifyPlayer(event.getPlayer()), 40L);
    }

    public void notifyPlayer(Player player) {
        if (!plugin.getPluginConfig().isUpdateCheckerEnabled()) return;
        if (!player.hasPermission("piglinrush.admin")) return;
        if (status != UpdateStatus.UPDATE_AVAILABLE) return;

        String url = latestVersionUrl != null ? latestVersionUrl : MODRINTH_URL;
        player.sendMessage(Component.empty());
        player.sendMessage(prefix().append(Component.text("A new version is available!", NamedTextColor.YELLOW)));
        player.sendMessage(prefix()
                .append(Component.text("Current: ", NamedTextColor.GRAY))
                .append(Component.text(currentVersion, NamedTextColor.RED))
                .append(Component.text("  →  Latest: ", NamedTextColor.GRAY))
                .append(Component.text(latestVersion, NamedTextColor.GREEN)));
        player.sendMessage(prefix()
                .append(Component.text("[Click to open Modrinth]", NamedTextColor.AQUA)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(url))
                        .hoverEvent(HoverEvent.showText(Component.text(url, NamedTextColor.GRAY)))));
        player.sendMessage(Component.empty());
    }

    public String getStatusLine() {
        return switch (status) {
            case PENDING -> "§7Checking...";
            case UP_TO_DATE -> "§aUp to date (" + currentVersion + ")";
            case UPDATE_AVAILABLE -> "§cUpdate available: " + currentVersion + " → §a" + latestVersion;
            case ERROR -> "§cCheck failed (see console)";
        };
    }

    public UpdateStatus getStatus() {
        return status;
    }

    private static boolean isNewer(String remote, String local) {
        if (remote.equals(local)) return false;
        try {
            int[] r = semver(remote), l = semver(local);
            for (int i = 0; i < Math.max(r.length, l.length); i++) {
                int rv = i < r.length ? r[i] : 0, lv = i < l.length ? l[i] : 0;
                if (rv != lv) return rv > lv;
            }
        } catch (NumberFormatException ignored) {
        }
        return false;
    }

    private static int[] semver(String v) {
        String[] p = v.split("[\\-+]")[0].split("\\.");
        int[] n = new int[p.length];
        for (int i = 0; i < p.length; i++) n[i] = Integer.parseInt(p[i]);
        return n;
    }

    private Component prefix() {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("zNxPiglinRush", NamedTextColor.GOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY));
    }

    public enum UpdateStatus {PENDING, UP_TO_DATE, UPDATE_AVAILABLE, ERROR}
}
