package dev.znxki.zNxPiglinRush.diagnostics;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.api.stats.StatsProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Monitors server TPS and alerts ops when performance degrades.
 * Renamed from {@code DiagnosticsMonitor} to {@code DiagnosticsService}
 * for consistency with other service-layer classes.
 */
public final class DiagnosticsService {

    private final PiglinRushPlugin plugin;
    private final StatsProvider stats;

    private int taskId = -1;
    private long lastAlertMillis = 0L;
    private final Deque<Double> tpsSamples = new ArrayDeque<>(5);

    private double tpsAlertThreshold;
    private int alertCooldownSeconds;

    public DiagnosticsService(PiglinRushPlugin plugin, StatsProvider stats) {
        this.plugin = plugin;
        this.stats = stats;
        reload();
    }

    public void start() {
        if (taskId != -1) return;
        taskId = plugin.getServer().getScheduler()
                .runTaskTimerAsynchronously(plugin, this::sample, 200L, 600L).getTaskId();
    }

    public void stop() {
        if (taskId == -1) return;
        plugin.getServer().getScheduler().cancelTask(taskId);
        taskId = -1;
    }

    public void reload() {
        tpsAlertThreshold = plugin.getConfig().getDouble("diagnostics.tps-alert-threshold", 18.0);
        alertCooldownSeconds = plugin.getConfig().getInt("diagnostics.alert-cooldown-seconds", 300);
    }

    public double getAverageTps() {
        if (tpsSamples.isEmpty()) return 20.0;
        return tpsSamples.stream().mapToDouble(d -> d).average().orElse(20.0);
    }

    public List<Component> buildReport() {
        List<Component> lines = new ArrayList<>();
        double tps = getAverageTps();
        NamedTextColor tpsColor = tps >= 19.0 ? NamedTextColor.GREEN : tps >= 16.0 ? NamedTextColor.YELLOW : NamedTextColor.RED;

        lines.add(Component.text("--- zNxPiglinRush Diagnostics ---", NamedTextColor.GOLD));
        lines.add(Component.text("  TPS (avg): ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f", tps), tpsColor)));
        lines.add(Component.text("  Session spawns: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(stats.getSessionSpawns()), NamedTextColor.WHITE)));
        lines.add(Component.text("  Last hour: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(stats.getLastHour()), NamedTextColor.WHITE)));
        lines.add(Component.text("  Suggestions:", NamedTextColor.AQUA));
        lines.add(tps < tpsAlertThreshold
                ? Component.text("  ⚠ TPS low — raise spawn-cooldown or lower spawn-count.", NamedTextColor.YELLOW)
                : Component.text("  ✔ TPS healthy.", NamedTextColor.GREEN));
        return lines;
    }

    private void sample() {
        double tps = sampleTps();
        synchronized (tpsSamples) {
            if (tpsSamples.size() >= 5) tpsSamples.poll();
            tpsSamples.offer(tps);
        }
        long now = System.currentTimeMillis();
        if (tps < tpsAlertThreshold && (now - lastAlertMillis) > (long) alertCooldownSeconds * 1000L) {
            lastAlertMillis = now;
            plugin.getSchedulerService().runDelayedGlobal(() -> notifyOps(tps), 1L);
        }
    }

    private void notifyOps(double tps) {
        Component msg = Component.text("[zNxPiglinRush] ", NamedTextColor.GOLD)
                .append(Component.text("⚠ TPS dropped to " + String.format("%.1f", tps)
                        + " — consider tuning spawn-cooldown or spawn-count.", NamedTextColor.YELLOW));
        for (Player p : Bukkit.getOnlinePlayers())
            if (p.hasPermission("piglinrush.admin")) p.sendMessage(msg);
    }

    private double sampleTps() {
        try {
            Method m = plugin.getServer().getClass().getMethod("getTPS");
            double[] arr = (double[]) m.invoke(plugin.getServer());
            return arr.length > 0 ? arr[0] : 20.0;
        } catch (Exception ignored) {
            return 20.0;
        }
    }
}
