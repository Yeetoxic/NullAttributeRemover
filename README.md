# NullAttributeRemover

A lightweight Bukkit/Spigot plugin that scans and removes **broken** or **invalid attribute modifiers** from players to prevent crashes and instability.  
Originally created to fix `java.lang.NullPointerException` in corrupted player data.

## 🔍 What It Does

This plugin scans player attributes for:

- ❌ `null` or duplicate UUIDs (even across attributes)
- ❌ `NaN` or `Infinity` values
- ❌ Invalid or missing operations
- ❌ `null`, blank, or UUID-looking modifier names
- ❌ Fully broken modifiers (null entries)

If any are found, they’re **removed automatically**.  
The scan can be triggered manually or on events like teleport, respawn, world change, etc.

## ⚙️ How It Works

- Checks every attribute on a player (`GENERIC_MAX_HEALTH`, etc.)
- Inspects each modifier’s:
  - UUID validity and uniqueness
  - Name validity (including if it looks like a UUID)
  - Value sanity (no `NaN`, `Infinity`, or zero junk)
  - Operation type
- If any part is broken or suspicious, it’s removed
- If a modifier’s UUID is reused in multiple places, all duplicates are removed
- Optional logs and messages are configurable

## 🔧 Configuration

```yaml
# config.yml
silent: false                # Suppresses player chat messages
kickOnFailure: true          # Kick player if bad modifiers can’t be removed
log-to-console: true         # Show scan results in console
log-to-file: true            # Save scan logs to plugins/NullAttributeRemover/nar.log
logUUIDLikeNames: true       # Warn if modifier name looks like a UUID
log-invalid-uuid-names: true    # If false, suppress logs like "Skipped bad UUID: armor.boots"
debugLogs: true              # Enable detailed internal debug logs
```

## 📦 Commands

| Command | Description |
|--------|-------------|
| `/nar scan [player]` | Scan a specific player or all online players |
| `/nar debug <player>` | Injects broken modifiers for testing purposes |
| `/nar reset <player>` | Clears all attributes and resets them to vanilla |

> The plugin uses tab completion for all commands.

## 🧪 Compatibility

- Requires **Java 17 or newer**
- Works with **Minecraft 1.17+**
- Designed for **Paper**, may also work on Spigot and forks

## 🛠 Building From Source

Clone and build with Maven:

```bash
mvn clean package
```

The resulting `.jar` will be in the `target/` folder.

## ⚖️ License

[MIT](./LICENSE)