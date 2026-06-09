package dev.znxki.zNxPiglinRush.spawner.registry;

import dev.znxki.zNxPiglinRush.PiglinRushPlugin;
import dev.znxki.zNxPiglinRush.config.SpawnerDefinition;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.logging.Level;

/**
 * Loads and caches {@link SpawnerDefinition} instances from {@code config.yml}.
 *
 * <p>Moved from {@code config.registry} to {@code spawner.registry} — the
 * registry is a runtime component of the spawner subsystem, not a config parser.
 */
public final class SpawnerRegistry {

    private final PiglinRushPlugin plugin;
    private final Map<Material, SpawnerDefinition> registry = new EnumMap<>(Material.class);

    public SpawnerRegistry(PiglinRushPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public Optional<SpawnerDefinition> get(Material material) {
        return Optional.ofNullable(registry.get(material));
    }

    public boolean isRegistered(Material material) {
        return registry.containsKey(material);
    }

    public Set<Material> getRegisteredBlocks() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    public int size() {
        return registry.size();
    }

    public void reload() {
        registry.clear();
        ConfigurationSection spawners = plugin.getConfig().getConfigurationSection("spawners");
        if (spawners == null) {
            loadDefaults();
            return;
        }

        for (String key : spawners.getKeys(false)) {
            ConfigurationSection sec = spawners.getConfigurationSection(key);
            if (sec == null) continue;

            Material block = parseMaterial(key.toUpperCase());
            if (block == null) {
                plugin.getLogger().warning("[SpawnerRegistry] Unknown material '" + key + "' — skipping.");
                continue;
            }

            EntityType mob = parseEntityType(sec.getString("mob", "ZOMBIFIED_PIGLIN"));
            if (mob == null) {
                plugin.getLogger().warning("[SpawnerRegistry] Unknown mob '" + sec.getString("mob") + "' — skipping.");
                continue;
            }

            registry.put(block, new SpawnerDefinition(
                    block, mob,
                    Math.max(1, sec.getInt("spawn-count", 3)),
                    Math.max(1, sec.getInt("spawn-cooldown", 20)),
                    Math.max(1, sec.getInt("max-nearby-entities", 12)),
                    Math.max(1, sec.getInt("scan-radius", 10)),
                    Math.max(0, sec.getInt("extra-attempts", 1)),
                    sec.getBoolean("wave-mode", false),
                    Math.max(1, sec.getInt("wave-size", 5)),
                    Math.max(1, sec.getInt("wave-cooldown", 100)),
                    parseDrops(sec, key)
            ));
            plugin.getLogger().info("[SpawnerRegistry] Registered: " + block.name() + " → " + mob.name());
        }

        if (registry.isEmpty()) {
            plugin.getLogger().warning("[SpawnerRegistry] No entries — using defaults.");
            loadDefaults();
        }
    }

    private void loadDefaults() {
        registry.put(Material.MAGMA_BLOCK, new SpawnerDefinition(
                Material.MAGMA_BLOCK, EntityType.ZOMBIFIED_PIGLIN,
                3, 20, 12, 10, 1, false, 5, 100, List.of()));
    }

    private @NonNull @UnmodifiableView List<SpawnerDefinition.CustomDrop> parseDrops(ConfigurationSection sec, String blockKey) {
        List<SpawnerDefinition.CustomDrop> drops = new ArrayList<>();
        for (Map<?, ?> raw : sec.getMapList("custom-drops")) {
            try {
                Material mat = parseMaterial(String.valueOf(raw.get("material")).toUpperCase());
                if (mat == null) continue;
                int min = raw.containsKey("min") ? ((Number) raw.get("min")).intValue() : 1;
                int max = raw.containsKey("max") ? ((Number) raw.get("max")).intValue() : 1;
                double ch = raw.containsKey("chance") ? ((Number) raw.get("chance")).doubleValue() : 1.0;
                drops.add(new SpawnerDefinition.CustomDrop(mat, min, max, Math.min(1.0, Math.max(0.0, ch))));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[SpawnerRegistry] Bad drop entry for '" + blockKey + "'", e);
            }
        }
        return Collections.unmodifiableList(drops);
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private EntityType parseEntityType(String name) {
        try {
            return EntityType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
