package dev.znxki.zNxPiglinRush;

import dev.znxki.zNxPiglinRush.commands.PiglinRushCommand;
import dev.znxki.zNxPiglinRush.config.PiglinRushConfig;
import dev.znxki.zNxPiglinRush.events.PiglinRushListener;
import dev.znxki.zNxPiglinRush.updater.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZNxPiglinRush extends JavaPlugin {
    private static ZNxPiglinRush instance;
    private PiglinRushConfig config;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        config = new PiglinRushConfig(this);

        Bukkit.getServer().getPluginManager().registerEvents(new PiglinRushListener(this), this);
        var cmd = Bukkit.getPluginCommand("znxpiglinrush");
        if(cmd != null) {
            var handler = new PiglinRushCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        updateChecker = new UpdateChecker(this);
        Bukkit.getServer().getPluginManager().registerEvents(updateChecker, this);
        updateChecker.check();

        getLogger().info("zNxPiglinRush v" + getPluginMeta().getVersion() + " enabled — gold farm boost active!");
    }

    @Override
    public void onDisable() {
        getLogger().info("zNxPiglinRush disabled.");
    }

    public static ZNxPiglinRush getInstance() {
        return instance;
    }

    public PiglinRushConfig getMagmaConfig() {
        return config;
    }

    public UpdateChecker getUpdateChecker()  { return updateChecker; }

    public void reloadMagmaConfig() {
        reloadConfig();
        config = new PiglinRushConfig(this);
    }
}
