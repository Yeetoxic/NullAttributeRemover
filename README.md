# NullAttributeRemover

A tiny Paper plugin that prevents server crashes caused by corrupted player attribute modifiers.  
It automatically clears all attribute modifiers from players when they join.

## 💡 Why?

Some malformed or plugin-generated items can leave behind null or invalid attribute data in a player's NBT.  
This can crash Paper servers with errors like:

```
Cannot invoke "ObjectArrayList.get(int)" because "this.wrapped" is null
```

This plugin clears all `AttributeInstance` modifiers before the player ticks — sidestepping the crash entirely.

## ✅ Features

- Clears all attribute modifiers on player join
- Logs to console when a player is cleaned
- Logs startup and shutdown events
- Lightweight, no config, no commands

## 🛠️ Requirements

- Minecraft 1.21+
- Paper (not Spigot or Bukkit — it uses Paper’s attribute API)
- Java 21

## 📦 Building

Use Maven to build:

```bash
mvn clean package
```

The output `.jar` will be in `target/`.

## ⚖️ License

[MIT](./LICENSE)
