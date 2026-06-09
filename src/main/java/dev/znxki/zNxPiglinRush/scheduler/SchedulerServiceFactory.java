package dev.znxki.zNxPiglinRush.scheduler;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.api.scheduler.SchedulerService;

/**
 * Creates the appropriate {@link SchedulerService} implementation at runtime
 * by probing for the Folia-specific class {@code RegionizedServer}.
 */
public final class SchedulerServiceFactory {

    private SchedulerServiceFactory() {
    }

    public static SchedulerService create(PiglinRushPlugin plugin) {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return new FoliaSchedulerService(plugin);
        } catch (ClassNotFoundException e) {
            return new BukkitSchedulerService(plugin);
        }
    }
}
