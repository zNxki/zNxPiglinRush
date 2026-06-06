package dev.znxki.zNxPiglinRush.commands;

import com.google.common.collect.ImmutableList;
import dev.znxki.zNxPiglinRush.ZNxPiglinRush;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class PiglinRushCommand implements CommandExecutor, TabCompleter {
    private final ZNxPiglinRush plugin;

    public PiglinRushCommand(ZNxPiglinRush plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("znxpiglinrush.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadMagmaConfig();
                sender.sendMessage(Component.text(
                        "[zNxPiglinRush] Config reloaded.", NamedTextColor.GREEN));
            }

            case "info" -> {
                var cfg = plugin.getMagmaConfig();
                sender.sendMessage(Component.text("--- zNxPiglinRush Info ---", NamedTextColor.GOLD));
                sender.sendMessage(info("Status", cfg.isEnabled() ? "ENABLED" : "DISABLED"));
                sender.sendMessage(info("Spawn count", String.valueOf(cfg.getSpawnCount())));
                sender.sendMessage(info("Cooldown", cfg.getSpawnCooldown() + " ticks"));
                sender.sendMessage(info("Max nearby", String.valueOf(cfg.getMaxNearbyEntities())));
                sender.sendMessage(info("Scan radius", cfg.getScanRadius() + " blocks"));
                sender.sendMessage(info("Extra attempts", String.valueOf(cfg.getExtraAttempts())));
                sender.sendMessage(info("Active worlds",
                        cfg.getActiveWorlds().isEmpty() ? "ALL" : String.join(", ", cfg.getActiveWorlds())));
                sender.sendMessage(info("Debug", String.valueOf(cfg.isDebug())));
            }

            case "toggle" -> {
                boolean current = plugin.getMagmaConfig().isEnabled();
                sender.sendMessage(Component.text(
                        "[zNxPiglinRush] Currently " + (current ? "ENABLED" : "DISABLED")
                                + ". Change 'enabled' in config.yml and use /ms reload.",
                        NamedTextColor.YELLOW));
            }

            default -> sendHelp(sender, label);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("znxpiglinrush.admin"))
            return List.of();

        ImmutableList.Builder<String> builder = ImmutableList.builder();
        List<String> arguments = List.of("info", "reload", "toggle");

        for (String arg : arguments) {
            if (arg.toLowerCase().startsWith(args[0].toLowerCase())) builder.add(arg);
        }

        return builder.build();
    }

    private void sendHelp(@NonNull CommandSender sender, String label) {
        sender.sendMessage(Component.text("zNxPiglinRush Commands", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/" + label + " reload", NamedTextColor.AQUA)
                .append(Component.text(" — Reload config", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " info", NamedTextColor.AQUA)
                .append(Component.text(" — Show current settings", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " toggle", NamedTextColor.AQUA)
                .append(Component.text(" — Toggle hint", NamedTextColor.GRAY)));
    }

    private @NonNull Component info(String key, String value) {
        return Component.text("  " + key + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE));
    }
}
