package dev.znxki.zNxPiglinRush;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.znxki.zNxPiglinRush.commands.PiglinRushCommand;
import dev.znxki.zNxPiglinRush.config.PiglinRushConfig;
import dev.znxki.zNxPiglinRush.events.PiglinRushListener;
import dev.znxki.zNxPiglinRush.scheduler.SchedulerAdapter;
import dev.znxki.zNxPiglinRush.updater.UpdateChecker;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class ZNxPiglinRush extends JavaPlugin {
    private static ZNxPiglinRush instance;

    private PiglinRushConfig config;
    private UpdateChecker updateChecker;
    private SchedulerAdapter schedulerAdapter;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        config = new PiglinRushConfig(this);
        schedulerAdapter = new SchedulerAdapter(this);

        Bukkit.getServer().getPluginManager().registerEvents(new PiglinRushListener(this), this);

        registerCommand();

        updateChecker = new UpdateChecker(this);
        Bukkit.getServer().getPluginManager().registerEvents(updateChecker, this);
        updateChecker.check();

        getLogger().info("zNxPiglinRush v" + getPluginMeta().getVersion()
                + " enabled" + (schedulerAdapter.isFolia() ? " [Folia]" : "") + " — gold farm boost active!");
    }

    @Override
    public void onDisable() {
        getLogger().info("zNxPiglinRush disabled.");
    }

    /**
     * Registers /piglinrush (alias /pr) using the Paper LifecycleEventManager
     * when running on Paper or Folia. On Spigot, falls back to the legacy
     * getCommand() approach declared in plugin.yml.
     * <p>
     * The Lifecycle API guarantees command registration happens before the server
     * finishes loading, fixing the "commands registered too late" issue that
     * plagued the old onEnable() + PluginCommand approach on Paper 1.20.6+.
     */
    private void registerCommand() {
        PiglinRushCommand handler = new PiglinRushCommand(this);

        try {
            LifecycleEventManager<Plugin> lifecycle = getLifecycleManager();
            lifecycle.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                Commands commands = event.registrar();
                commands.register(
                        commands.getDispatcher()
                                .register(LiteralArgumentBuilder.<CommandSourceStack>literal("piglinrush")
                                        .requires(src -> src.getSender().hasPermission("piglinrush.admin"))
                                        .executes(ctx -> {
                                            handler.onCommand(ctx.getSource().getSender(), null, "piglinrush", new String[0]);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("reload")
                                                .executes(ctx -> {
                                                    handler.onCommand(ctx.getSource().getSender(), null, "piglinrush", new String[]{"reload"});
                                                    return Command.SINGLE_SUCCESS;
                                                }))
                                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("info")
                                                .executes(ctx -> {
                                                    handler.onCommand(ctx.getSource().getSender(), null, "piglinrush", new String[]{"info"});
                                                    return Command.SINGLE_SUCCESS;
                                                }))
                                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("toggle")
                                                .executes(ctx -> {
                                                    handler.onCommand(ctx.getSource().getSender(), null, "piglinrush", new String[]{"toggle"});
                                                    return Command.SINGLE_SUCCESS;
                                                }))),
                        "zNxPiglinRush main command",
                        List.of("pr")
                );
            });
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            var cmd = Bukkit.getPluginCommand("znxpiglinrush");
            if (cmd != null) {
                cmd.setExecutor(handler);
                cmd.setTabCompleter(handler);
            }
        }
    }

    public static ZNxPiglinRush getInstance() {
        return instance;
    }

    public PiglinRushConfig getMagmaConfig() {
        return config;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public SchedulerAdapter getSchedulerAdapter() {
        return schedulerAdapter;
    }

    public void reloadMagmaConfig() {
        reloadConfig();
        config = new PiglinRushConfig(this);
    }
}
