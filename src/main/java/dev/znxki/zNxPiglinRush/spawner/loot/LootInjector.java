package dev.znxki.zNxPiglinRush.spawner.loot;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.config.SpawnerDefinition;
import dev.znxki.zNxPiglinRush.spawner.registry.SpawnerRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.Optional;
import java.util.Random;

/**
 * Listens for {@link EntityDeathEvent} and injects custom drops for
 * entities that were spawned by this plugin (identified via metadata).
 *
 * <p>Moved from {@code loot} to {@code spawner.loot} — loot injection is
 * a sub-concern of the spawner subsystem.
 */
public final class LootInjector implements Listener {

    public static final String META_KEY = "piglinrush_block";

    private final PiglinRushPlugin plugin;
    private final SpawnerRegistry registry;
    private final Random random = new Random();

    public LootInjector(PiglinRushPlugin plugin, SpawnerRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public static void tag(Entity entity, Material spawnerBlock, PiglinRushPlugin plugin) {
        entity.setMetadata(META_KEY, new FixedMetadataValue(plugin, spawnerBlock.name()));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasMetadata(META_KEY)) return;

        String blockName = entity.getMetadata(META_KEY).stream()
                .filter(m -> m.getOwningPlugin() == plugin)
                .findFirst()
                .map(MetadataValue::asString)
                .orElse(null);
        if (blockName == null) return;

        Material mat;
        try {
            mat = Material.valueOf(blockName);
        } catch (IllegalArgumentException e) {
            return;
        }

        Optional<SpawnerDefinition> defOpt = registry.get(mat);
        if (defOpt.isEmpty() || defOpt.get().customDrops().isEmpty()) return;

        for (SpawnerDefinition.CustomDrop drop : defOpt.get().customDrops()) {
            ItemStack item = drop.roll(random);
            if (item != null) event.getDrops().add(item);
        }
    }
}
