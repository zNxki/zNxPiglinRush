package dev.znxki.zNxPiglinRush.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Queries the Modrinth API v2 for the latest version of a project.
 *
 * <p>Extracted from {@code UpdateChecker} into its own class so that
 * alternative update sources (Hangar, GitHub Releases) can be added
 * later without touching the checker logic.
 */
public final class ModrinthUpdateSource {

    private static final String API =
            "https://api.modrinth.com/v2/project/%s/version?loaders=[%%22paper%%22,%%22spigot%%22]&game_versions=[%%22%s%%22]";

    private final String projectId;
    private final String userAgent;

    public ModrinthUpdateSource(String projectId, String userAgent) {
        this.projectId = projectId;
        this.userAgent = userAgent;
    }

    /**
     * Fetches the latest version for the given Minecraft version.
     *
     * @return a {@link VersionResult} — never {@code null}
     * @throws IOException if the HTTP request fails
     */
    public VersionResult fetchLatest(String mcVersion) throws IOException {
        String url = String.format(API, projectId, mcVersion);
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(5_000);
        conn.setRequestProperty("User-Agent", userAgent);

        int status = conn.getResponseCode();
        if (status == 404) return VersionResult.notFound();
        if (status != 200) return VersionResult.error("HTTP " + status);

        try (var reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
            JsonArray versions = JsonParser.parseReader(reader).getAsJsonArray();
            if (versions.isEmpty()) return VersionResult.noResults();

            JsonObject latest = versions.get(0).getAsJsonObject();
            String versionNumber = latest.get("version_number").getAsString();
            String versionId = latest.has("id") ? latest.get("id").getAsString() : null;
            String versionUrl = versionId != null
                    ? "https://modrinth.com/plugin/" + projectId + "/version/" + versionId
                    : "https://modrinth.com/plugin/" + projectId;
            return VersionResult.found(versionNumber, versionUrl);
        }
    }

    public record VersionResult(Status status, String versionNumber, String versionUrl, String errorMessage) {

        public enum Status {FOUND, NOT_FOUND, NO_RESULTS, ERROR}

        @Contract("_, _ -> new")
        static @NonNull VersionResult found(String ver, String url) {
            return new VersionResult(Status.FOUND, ver, url, null);
        }

        @Contract(" -> new")
        static @NonNull VersionResult notFound() {
            return new VersionResult(Status.NOT_FOUND, null, null, null);
        }

        @Contract(" -> new")
        static @NonNull VersionResult noResults() {
            return new VersionResult(Status.NO_RESULTS, null, null, null);
        }

        @Contract("_ -> new")
        static @NonNull VersionResult error(String msg) {
            return new VersionResult(Status.ERROR, null, null, msg);
        }
    }
}
