package dev.znxki.zNxPiglinRush;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.znxki.zNxPiglinRush.api.scheduler.SchedulerService;
import dev.znxki.zNxPiglinRush.api.storage.StorageBackend;
import dev.znxki.zNxPiglinRush.command.PiglinRushCommand;
import dev.znxki.zNxPiglinRush.config.ConfigUpdater;
import dev.znxki.zNxPiglinRush.config.PluginConfig;
import dev.znxki.zNxPiglinRush.diagnostics.DiagnosticsService;
import dev.znxki.zNxPiglinRush.heatmap.HeatmapService;
import dev.znxki.zNxPiglinRush.integration.PlaceholderIntegration;
import dev.znxki.zNxPiglinRush.listener.SpawnListener;
import dev.znxki.zNxPiglinRush.scheduler.SchedulerServiceFactory;
import dev.znxki.zNxPiglinRush.spawner.loot.LootInjector;
import dev.znxki.zNxPiglinRush.spawner.registry.SpawnerRegistry;
import dev.znxki.zNxPiglinRush.stats.SpawnTracker;
import dev.znxki.zNxPiglinRush.stats.storage.H2StorageBackend;
import dev.znxki.zNxPiglinRush.storage.StorageBackendFactory;
import dev.znxki.zNxPiglinRush.update.UpdateChecker;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class PiglinRushPlugin extends JavaPlugin {

    private static PiglinRushPlugin instance;

    // Services
    private PluginConfig pluginConfig;
    private SchedulerService schedulerService;
    private SpawnerRegistry spawnerRegistry;
    private SpawnTracker spawnTracker;
    private StorageBackend storageBackend;
    private HeatmapService heatmapService;
    private LootInjector lootInjector;
    private DiagnosticsService diagnosticsService;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        new ConfigUpdater(this).update();

        pluginConfig = new PluginConfig(this);
        schedulerService = SchedulerServiceFactory.create(this);
        spawnTracker = new SpawnTracker();
        spawnerRegistry = new SpawnerRegistry(this);
        heatmapService = new HeatmapService(this, spawnTracker);
        lootInjector = new LootInjector(this, spawnerRegistry);
        diagnosticsService = new DiagnosticsService(this, spawnTracker);

        PluginConfig.StorageType storageType = pluginConfig.getStorageType();
        storageBackend = StorageBackendFactory.create(this, spawnTracker, storageType);
        storageBackend.open();

        var pm = getServer().getPluginManager();
        pm.registerEvents(new SpawnListener(this), this);
        pm.registerEvents(lootInjector, this);

        updateChecker = new UpdateChecker(this);
        pm.registerEvents(updateChecker, this);
        updateChecker.check();

        registerCommand();
        diagnosticsService.start();

        if (pm.getPlugin("PlaceholderAPI") != null) {
            new PlaceholderIntegration(this, spawnTracker, diagnosticsService).register();
            getLogger().info("[PiglinRushPlugin] PlaceholderAPI expansion registered.");
        }

        StartupBanner.print(this, spawnerRegistry.size(), storageType, schedulerService.isFolia());
    }

    @Override
    public void onDisable() {
        heatmapService.shutdown();
        diagnosticsService.stop();
        storageBackend.close();
        getLogger().info("zNxPiglinRush disabled.");
    }

    private void registerCommand() {
        PiglinRushCommand handler = new PiglinRushCommand(this);
        try {
            LifecycleEventManager<Plugin> lifecycle = getLifecycleManager();
            lifecycle.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                Commands cmds = event.registrar();
                cmds.register(
                        cmds.getDispatcher().register(brigadier(handler)),
                        "zNxPiglinRush main command",
                        List.of("pr")
                );
            });
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            var cmd = getCommand("piglinrush");
            if (cmd != null) {
                cmd.setExecutor(handler);
                cmd.setTabCompleter(handler);
            }
        }
    }

    private LiteralArgumentBuilder<CommandSourceStack> brigadier(PiglinRushCommand handler) {
        var root = lit("piglinrush")
                .requires(src -> src.getSender().hasPermission("piglinrush.admin"))
                .executes(ctx -> {
                    handler.onCommand(ctx.getSource().getSender(), null, "piglinrush", new String[0]);
                    return 1;
                });
        for (String sub : new String[]{"reload", "info", "stats", "heatmap", "diagnostics", "toggle"}) {
            root.then(lit(sub).executes(ctx -> {
                handler.onCommand(ctx.getSource().getSender(), null, "piglinrush", new String[]{sub});
                return 1;
            }));
        }
        return root;
    }

    @Contract(value = "_ -> new", pure = true)
    private static @NonNull LiteralArgumentBuilder<CommandSourceStack> lit(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static PiglinRushPlugin getInstance() {
        return instance;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public SchedulerService getSchedulerService() {
        return schedulerService;
    }

    public SpawnerRegistry getSpawnerRegistry() {
        return spawnerRegistry;
    }

    public SpawnTracker getSpawnTracker() {
        return spawnTracker;
    }

    public HeatmapService getHeatmapService() {
        return heatmapService;
    }

    public DiagnosticsService getDiagnosticsService() {
        return diagnosticsService;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        pluginConfig = new PluginConfig(this);
        spawnTracker.resetSession();
    }
}
