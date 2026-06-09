package dev.znxki.zNxPiglinRush.command;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.api.stats.StatsProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Handles the {@code /piglinrush} command (alias {@code /pr}).
 * Subcommands: reload, info, stats, heatmap [radius], diagnostics, toggle.
 */
public final class PiglinRushCommand implements CommandExecutor, TabCompleter {

    private final PiglinRushPlugin plugin;

    public PiglinRushCommand(PiglinRushPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @Nullable Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("piglinrush.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "reload" -> {
                plugin.reloadPluginConfig();
                plugin.getSpawnerRegistry().reload();
                plugin.getDiagnosticsService().reload();
                sender.sendMessage(Component.text("[zNxPiglinRush] Config reloaded.", NamedTextColor.GREEN));
            }

            case "info" -> {
                var cfg = plugin.getPluginConfig();
                sender.sendMessage(Component.text("--- zNxPiglinRush Info ---", NamedTextColor.GOLD));
                sender.sendMessage(info("Status", cfg.isEnabled() ? "ENABLED" : "DISABLED"));
                sender.sendMessage(info("Runtime", plugin.getSchedulerService().isFolia() ? "Folia" : "Spigot/Paper"));
                sender.sendMessage(info("Spawner blocks", String.valueOf(plugin.getSpawnerRegistry().size())));
                sender.sendMessage(info("Registered",
                        plugin.getSpawnerRegistry().getRegisteredBlocks().stream()
                                .map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("none")));
                sender.sendMessage(info("Debug", String.valueOf(cfg.isDebug())));
                sender.sendMessage(Component.text("  Update: ", NamedTextColor.GRAY)
                        .append(LegacyComponentSerializer.legacySection()
                                .deserialize(plugin.getUpdateChecker().getStatusLine())));
            }

            case "stats" -> {
                StatsProvider s = plugin.getSpawnTracker();
                Duration uptime = Duration.between(s.getSessionStart(), Instant.now());
                sender.sendMessage(Component.text("--- zNxPiglinRush Stats ---", NamedTextColor.GOLD));
                sender.sendMessage(info("Total (all time)", String.format("%,d", s.getTotalSpawns())));
                sender.sendMessage(info("This session", String.format("%,d", s.getSessionSpawns())));
                sender.sendMessage(info("Session uptime", uptime.toHours() + "h " + uptime.toMinutesPart() + "m"));
                sender.sendMessage(info("Last hour", String.format("%,d", s.getLastHour())));
                sender.sendMessage(info("Last 24h", String.format("%,d", s.getLastHours(24))));
                Map<org.bukkit.entity.EntityType, Long> byMob = s.getByMob();
                if (!byMob.isEmpty()) {
                    sender.sendMessage(Component.text("  By mob:", NamedTextColor.GRAY));
                    byMob.forEach((mob, count) ->
                            sender.sendMessage(Component.text("    " + mob.name() + ": ", NamedTextColor.GRAY)
                                    .append(Component.text(String.format("%,d", count), NamedTextColor.WHITE))));
                }
            }

            case "heatmap" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Player-only command.", NamedTextColor.RED));
                    return true;
                }
                int radius = 32;
                if (args.length > 1) {
                    try {
                        radius = Math.clamp(Integer.parseInt(args[1]), 5, 100);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(Component.text("Invalid radius.", NamedTextColor.RED));
                        return true;
                    }
                }
                boolean active = plugin.getHeatmapService().toggle(player, radius);
                sender.sendMessage(Component.text("[zNxPiglinRush] Heatmap "
                                + (active ? "§aenabled" : "§cdisabled") + "§r (radius: " + radius + " blocks).",
                        NamedTextColor.YELLOW));
            }

            case "diagnostics", "diag" -> plugin.getDiagnosticsService().buildReport().forEach(sender::sendMessage);

            case "toggle" -> {
                boolean on = plugin.getPluginConfig().isEnabled();
                sender.sendMessage(Component.text("[zNxPiglinRush] Currently " + (on ? "ENABLED" : "DISABLED")
                        + ". Edit 'enabled' in config.yml and use /pr reload.", NamedTextColor.YELLOW));
            }

            default -> sendHelp(sender, label);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("piglinrush.admin")) return List.of();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return List.of("reload", "info", "stats", "heatmap", "diagnostics", "toggle")
                    .stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("heatmap"))
            return List.of("16", "32", "48", "64");
        return List.of();
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("--- zNxPiglinRush Commands ---", NamedTextColor.GOLD));
        row(sender, label, "reload", "Reload config & registry");
        row(sender, label, "info", "Show plugin settings");
        row(sender, label, "stats", "Show spawn statistics");
        row(sender, label, "heatmap [r]", "Toggle heatmap particles");
        row(sender, label, "diagnostics", "TPS & performance report");
        row(sender, label, "toggle", "Toggle hint");
    }

    private void row(CommandSender s, String label, String sub, String desc) {
        s.sendMessage(Component.text("  /" + label + " " + sub, NamedTextColor.AQUA)
                .append(Component.text(" — " + desc, NamedTextColor.GRAY)));
    }

    private Component info(String key, String value) {
        return Component.text("  " + key + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE));
    }
}
