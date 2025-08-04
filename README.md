# NullAttributeRemover

A lightweight Bukkit/Spigot plugin that scans and removes **broken** or **invalid attribute modifiers** from players to prevent crashes and instability.  
Originally created to fix `java.lang.NullPointerException` in corrupted player data.

## 🔍 What It Does

Scans all online players for modifiers with:
- ❌ `null` or duplicate UUIDs
- ❌ `NaN` or `Infinity` amounts
- ❌ `null`, blank, or UUID-like names
- ❌ invalid operations
- ❌ completely null modifier entries

## ✅ New in v1.2

- `/nar scan [player]` — manually scan a specific player or all online players  
- `/nar debug <player>` — injects known-broken modifiers for testing
- **Event-based scanning:** automatic checks on teleport, world change, etc.
- **Config support** (`config.yml`) for:
  - Silent mode (no chat messages to players)
  - Console logging (on/off)
  - Log file output (e.g. `logs/nar.log`)
- Colored console logs for easy visibility
- Detects UUID-style names and removes junky 0.0-amount filler modifiers

## 🔧 Configuration

```yaml
# config.yml
silent: false              # If true, suppresses chat messages sent to players
log-to-console: true       # If true, log modifier actions to console
log-to-file: true          # If true, also log to plugins/NullAttributeRemover/nar.log
```

## 📦 Commands

| Command | Description |
|--------|-------------|
| `/nar scan [player]` | Scan a player or all online players for bad attributes |
| `/nar debug <player>` | Injects known broken modifiers for testing your setup |
| `/nar reset <player>` | Resets a player’s attributes to vanilla defaults (optional) |

## 🧪 Compatibility

- Requires **Java 17+**
- Works with **Minecraft 1.17+**
- Designed for **Paper** but may work on Spigot

## 📦 Building

Use Maven to build:

```bash
mvn clean package
```

The output `.jar` will be in `target/`.

## ⚖️ License

[MIT](./LICENSE)
