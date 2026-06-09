package dev.znxki.zNxPiglinRush;

import dev.znxki.zNxPiglinRush.config.PluginConfig;
import org.jspecify.annotations.NonNull;

import java.util.logging.Logger;

public class StartupBanner {
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";

    private static final String GOLD = "\u001B[38;2;255;170;0m";
    private static final String ORANGE = "\u001B[38;2;255;100;0m";
    private static final String GRAY = "\u001B[38;2;180;180;180m";
    private static final String WHITE = "\u001B[38;2;240;240;240m";

    private StartupBanner() {
    }

    public static void print(@NonNull PiglinRushPlugin plugin, int spawnerCount,
                             PluginConfig.@NonNull StorageType storageType, boolean isFolia) {
        Logger log = plugin.getLogger();
        String version = plugin.getPluginMeta().getVersion();

        String[] art = {
                GOLD + BOLD + "  ____  _       _ _       ____            _     " + RESET,
                GOLD + BOLD + " |  _ \\(_) __ _| (_)_ __ |  _ \\ _   _ ___| |__  " + RESET,
                GOLD + BOLD + " | |_) | |/ _` | | | '_ \\| |_) | | | / __| '_ \\ " + RESET,
                GOLD + BOLD + " |  __/| | (_| | | | | | |  _ <| |_| \\__ \\ | | |" + RESET,
                GOLD + BOLD + " |_|   |_|\\__, |_|_|_| |_|_| \\_\\\\__,_|___/_| |_|" + RESET,
                GOLD + BOLD + "          |___/                                   " + RESET,
        };

        String sep = ORANGE + "  " + "━".repeat(51) + RESET;

        String versionLine  = pill("Version",  version,      GOLD);
        String platformLine = pill("Platform", isFolia ? "Folia" : "Paper", ORANGE);
        String storageLine  = pill("Storage",  storageType.name(), ORANGE);
        String spawnerLine  = pill("Spawners", spawnerCount + " loaded", ORANGE);
        String authorLine   = pill("Author",   "zNxki_", GRAY);

        log.info("");
        for (String line : art)  log.info(line);
        log.info(sep);
        log.info(versionLine);
        log.info(platformLine);
        log.info(storageLine);
        log.info(spawnerLine);
        log.info(authorLine);
        log.info(sep);
        log.info("");
    }

    private static @NonNull String pill(String label, String value, String valueColor) {
        return GRAY + "  " + WHITE + BOLD + String.format("%-10s", label)
                + RESET + GRAY + " » "
                + valueColor + BOLD + value
                + RESET;
    }
}
