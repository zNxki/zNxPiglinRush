# 🔥 zNxPiglinRush

**Boost Zombified Piglin spawns on magma blocks — multiply your AFK gold farm output with zero NMS and zero lag.**

Vanilla already spawns piglins above magma blocks in the Nether, but the rate is painfully slow. zNxPiglinRush hooks into every natural spawn, detects magma blocks, and fires multiple extra spawns in the surrounding area — turning a trickle into a flood.

---

## ✨ Features

- **Spawn multiplier** — configurable number of extra Zombified Piglins per vanilla trigger
- **Spread logic** — extra mobs scatter across nearby magma blocks, not stacked on one tile
- **Lag protection** — per-block cooldown + configurable nearby-entity cap
- **World whitelist** — restrict the plugin to specific worlds (e.g. only your farm world)
- **Zero NMS** — pure Bukkit/Spigot API, compatible with Spigot and Paper 1.21.x
- **Update checker** — async Modrinth check on startup, clickable in-game notification for ops
- **Live reload** — `/piglinrush reload` applies config changes with no restart

---

## 🚀 Quick Start

1. Drop the `.jar` into your `plugins/` folder
2. Restart the server
3. Tune `plugins/zNxPiglinRush/config.yml` to your farm size
4. Run `/piglinrush reload` — done

---

## ⚙️ Configuration

```yaml
# Extra Zombified Piglins to spawn per vanilla trigger (vanilla = 1)
spawn-count: 3

# Cooldown between spawns on the same magma block (ticks, 20 = 1s)
spawn-cooldown: 20

# Max piglins within scan-radius before spawning is paused
max-nearby-entities: 12

# Radius (blocks) used for the entity cap check
scan-radius: 10

# Random extra attempts per trigger for natural variance
extra-attempts: 1

# Restrict to specific worlds — leave empty for all worlds
active-worlds: []

# Async update check on startup + op notification on join
update-checker: true
```

### Tuning guide

| Server type | `spawn-count` | `spawn-cooldown` | `max-nearby-entities` |
|---|---|---|---|
| Low-TPS / small | `2` | `40` | `8` |
| Standard | `3` | `20` | `12` |
| Dedicated farm server | `5` | `10` | `20` |

> 📉 If TPS drops, lower `spawn-count` or raise `spawn-cooldown` first.

---

## 💬 Commands & Permissions

| Command | Alias | Description |
|---|---|---|
| `/piglinrush reload` | `/pr reload` | Reload config without restart |
| `/piglinrush info` | `/pr info` | Show active settings + update status |

**Permission node:** `piglinrush.admin` (default: `op`)

---

## ❓ FAQ

**Does this affect overworld magma blocks?**
No. Zombified Piglins only spawn naturally on magma blocks in the Nether. The plugin fires only when vanilla already triggers a spawn.

**Does it break vanilla spawn rules?**
The initial spawn check (light level, mob caps) is still handled by vanilla. Extra spawns respect the `max-nearby-entities` cap in config. Global server mob caps in `spigot.yml` / `bukkit.yml` still apply.

**Compatible with other spawn plugins?**
Generally yes — zNxPiglinRush only reacts to events that already passed vanilla's own checks, with no NMS or spawner modifications.

**Folia support?**
Not yet. Planned for a future release.

---

## 📋 Requirements

- **Paper** 26.1.2+
- **Java** 25+

---

## 📄 License

This project is licensed under the **[Apache License 2.0](LICENSE)** — see the [LICENSE](LICENSE) file for details.
 
