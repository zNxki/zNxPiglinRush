package dev.znxki.zNxPiglinRush.updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.znxki.zNxPiglinRush.ZNxPiglinRush;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class UpdateChecker implements Listener {
    private static final String MODRINTH_PROJECT_ID = "znxpiglinrush";
    private static final String MODRINTH_API =
            "https://api.modrinth.com/v2/project/%s/version?loaders=[%%22paper%%22]&game_versions=[%%22%s%%22]";
    private static final String MODRINTH_URL =
            "https://modrinth.com/plugin/" + MODRINTH_PROJECT_ID;

    private final ZNxPiglinRush plugin;
    private final String currentVersion;

    /**
     * Result state — set once the async check completes.
     */
    private UpdateResult result = UpdateResult.PENDING;
    private String latestVersion = null;
    private String latestVersionUrl = null;

    public UpdateChecker(@NonNull ZNxPiglinRush plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getPluginMeta().getVersion();
    }

    public void check() {
        if (!plugin.getMagmaConfig().isUpdateChecker()) return;

        CompletableFuture.runAsync(() -> {
            try {
                fetchLatestVersion();
            } catch (Exception e) {
                result = UpdateResult.ERROR;
                plugin.getLogger().log(Level.WARNING,
                        "[UpdateChecker] Failed to contact Modrinth: " + e.getMessage());
            }
        });
    }


    /**
     * Notify a player if an update is available (called on join for ops).
     */
    public void notifyPlayer(Player player) {
        if (!plugin.getMagmaConfig().isUpdateChecker()) return;
        if (!player.hasPermission("magmaspawner.admin")) return;

        switch (result) {
            case UPDATE_AVAILABLE -> {
                player.sendMessage(Component.empty());
                player.sendMessage(prefix()
                        .append(Component.text("A new version is available!", NamedTextColor.YELLOW)));
                player.sendMessage(prefix()
                        .append(Component.text("Current: ", NamedTextColor.GRAY))
                        .append(Component.text(currentVersion, NamedTextColor.RED))
                        .append(Component.text("  →  Latest: ", NamedTextColor.GRAY))
                        .append(Component.text(latestVersion, NamedTextColor.GREEN)));
                player.sendMessage(prefix()
                        .append(Component.text("[Click to open Modrinth]", NamedTextColor.AQUA)
                                .decorate(TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.openUrl(
                                        latestVersionUrl != null ? latestVersionUrl : MODRINTH_URL))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text(latestVersionUrl != null
                                                        ? latestVersionUrl : MODRINTH_URL,
                                                NamedTextColor.GRAY)))));
                player.sendMessage(Component.empty());
            }
            case UP_TO_DATE -> plugin.getLogger().fine("[UpdateChecker] " + player.getName()
                    + " joined — plugin is up to date.");
            case PENDING, ERROR -> { /* silent */ }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> notifyPlayer(event.getPlayer()), 40L);
    }

    /**
     * Returns a one-line status string for /ms info.
     */
    public String getStatusLine() {
        return switch (result) {
            case PENDING -> "§7Checking...";
            case UP_TO_DATE -> "§aUp to date (" + currentVersion + ")";
            case UPDATE_AVAILABLE -> "§cUpdate available: " + currentVersion + " → §a" + latestVersion;
            case ERROR -> "§cCheck failed (see console)";
        };
    }

    public UpdateResult getResult() {
        return result;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    private void fetchLatestVersion() throws IOException {
        String gameVersion = plugin.getServer().getMinecraftVersion();
        String url = String.format(MODRINTH_API, MODRINTH_PROJECT_ID, gameVersion);

        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(5_000);
        conn.setRequestProperty("User-Agent",
                "zNxPiglinRush/" + currentVersion + " (zNxki_; modrinth.com/plugin/" + MODRINTH_PROJECT_ID + ")");

        int status = conn.getResponseCode();
        if (status == 404) {
            result = UpdateResult.ERROR;
            plugin.getLogger().warning(
                    "[UpdateChecker] Project '" + MODRINTH_PROJECT_ID + "' not found on Modrinth. " +
                            "Update MODRINTH_PROJECT_ID in UpdateChecker.java before publishing.");
            return;
        }

        if (status != 200) {
            result = UpdateResult.ERROR;
            plugin.getLogger().warning(
                    "[UpdateChecker] Modrinth returned HTTP " + status);
            return;
        }

        try (var reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
            JsonArray versions = JsonParser.parseReader(reader).getAsJsonArray();

            if (versions.isEmpty()) {
                result = UpdateResult.UP_TO_DATE;
                return;
            }

            JsonObject latest = versions.get(0).getAsJsonObject();
            latestVersion = latest.get("version_number").getAsString();

            if (latest.has("id")) {
                latestVersionUrl = "https://modrinth.com/plugin/"
                        + MODRINTH_PROJECT_ID + "/version/" + latest.get("id").getAsString();
            }

            if (isNewer(latestVersion, currentVersion)) {
                result = UpdateResult.UPDATE_AVAILABLE;
                plugin.getLogger().warning(
                        "[UpdateChecker] Update available! " + currentVersion
                                + " → " + latestVersion + " — " + MODRINTH_URL);
            } else {
                result = UpdateResult.UP_TO_DATE;
                plugin.getLogger().info(
                        "[UpdateChecker] Plugin is up to date (" + currentVersion + ").");
            }
        }
    }

    static boolean isNewer(String remote, String local) {
        if (remote.equals(local)) return false;
        try {
            int[] r = parseSemver(remote);
            int[] l = parseSemver(local);
            for (int i = 0; i < Math.max(r.length, l.length); i++) {
                int rv = i < r.length ? r[i] : 0;
                int lv = i < l.length ? l[i] : 0;
                if (rv != lv) return rv > lv;
            }
            return false;
        } catch (NumberFormatException e) {
            return !remote.equals(local);
        }
    }

    private static int[] parseSemver(String version) {
        String clean = version.split("[\\-+]")[0];
        String[] parts = clean.split("\\.");
        int[] nums = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            nums[i] = Integer.parseInt(parts[i]);
        }
        return nums;
    }

    private Component prefix() {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("zNxPiglinRush", NamedTextColor.GOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY));
    }

    public enum UpdateResult {
        PENDING,
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        ERROR
    }
}
