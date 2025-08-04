package net.slimelabs.nullattributeremover;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public final class NullAttributeRemover extends JavaPlugin implements Listener, TabExecutor {

    // Color scheme
    private final ChatColor GREEN = ChatColor.of("#88ff88");
    private final ChatColor BLUE = ChatColor.of("#88bbff");
    private final ChatColor GRAY = ChatColor.GRAY;
    private final ChatColor YELLOW = ChatColor.of("#ffee88");
    private final ChatColor RED = ChatColor.RED;
    private final ChatColor PREFIX = ChatColor.of("#55ffaa");

    private final String prefix = PREFIX + "[" + ChatColor.BOLD + "NAR" + ChatColor.RESET + PREFIX + "] " + ChatColor.RESET;

    // Config options
    private boolean silentMode;
    private boolean logWhenClean;
    private boolean kickOnFailure;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        getCommand("nar").setExecutor(this);
        getCommand("nar").setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);

        log("Started. Watching attributes like a hawk.", true);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                cleanAttributes(player, true);
            }
        }, 20L * 30, 20L * 30);
    }

    @Override
    public void onDisable() {
        log("Shut down. If the server crashes now, it's not on me.", true);
    }

    private void loadSettings() {
        FileConfiguration config = getConfig();
        silentMode = config.getBoolean("silent-mode", false);
        logWhenClean = config.getBoolean("log-when-clean", true);
        kickOnFailure = config.getBoolean("kick-on-failure", true);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        cleanAttributes(event.getPlayer(), false);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        cleanAttributes(event.getPlayer(), true);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        cleanAttributes(event.getPlayer(), true);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        cleanAttributes(event.getPlayer(), true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nullattributeremover.use")) {
            sender.sendMessage(prefix + RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(prefix + GRAY + "Usage: /nar <scan|reload|version|debug|reset> [player]");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("scan")) {
            if (args.length == 1) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    boolean fixed = cleanAttributes(p, false);
                    sender.sendMessage(prefix + BLUE + p.getName() + ": " + (fixed ? GREEN + "Fixed." : GRAY + "No issues."));
                }
            } else {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null && target.isOnline()) {
                    boolean fixed = cleanAttributes(target, false);
                    sender.sendMessage(prefix + BLUE + target.getName() + ": " + (fixed ? GREEN + "Fixed." : GRAY + "No issues."));
                } else {
                    sender.sendMessage(prefix + RED + "Player not found or offline.");
                }
            }
            return true;
        }

        if (sub.equals("reload")) {
            reloadConfig();
            loadSettings();
            sender.sendMessage(prefix + GREEN + "Configuration reloaded.");
            return true;
        }

        if (sub.equals("version")) {
            sender.sendMessage(prefix + YELLOW + "NullAttributeRemover v" + getDescription().getVersion());
            return true;
        }

        if (sub.equals("debug")) {
            if (args.length == 2) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null && target.isOnline()) {
                    debugInjectModifier(target);
                    sender.sendMessage(prefix + GREEN + "Injected debug modifiers into " + target.getName());
                } else {
                    sender.sendMessage(prefix + RED + "Player not found or offline.");
                }
            } else {
                sender.sendMessage(prefix + GRAY + "Usage: /nar debug <player>");
            }
            return true;
        }

        if (sub.equals("reset")) {
            if (args.length == 2) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null && target.isOnline()) {
                    for (Attribute attr : Attribute.values()) {
                        AttributeInstance instance = target.getAttribute(attr);
                        if (instance == null) continue;

                        for (AttributeModifier mod : new ArrayList<>(instance.getModifiers())) {
                            instance.removeModifier(mod);
                        }

                        // Reset to default base value if possible
                        switch (attr) {
                            case GENERIC_MAX_HEALTH -> instance.setBaseValue(20.0);
                            case GENERIC_MOVEMENT_SPEED -> instance.setBaseValue(0.1);
                            case GENERIC_ATTACK_DAMAGE -> instance.setBaseValue(1.0);
                            case GENERIC_ATTACK_SPEED -> instance.setBaseValue(4.0);
                            case GENERIC_ARMOR -> instance.setBaseValue(0.0);
                            case GENERIC_ARMOR_TOUGHNESS -> instance.setBaseValue(0.0);
                            case GENERIC_LUCK -> instance.setBaseValue(0.0);
                            case GENERIC_FLYING_SPEED -> instance.setBaseValue(0.05);
                            case GENERIC_KNOCKBACK_RESISTANCE -> instance.setBaseValue(0.0);
                            case GENERIC_FOLLOW_RANGE -> instance.setBaseValue(32.0);
                            case GENERIC_ATTACK_KNOCKBACK -> instance.setBaseValue(0.0);
                            default -> {} // Other attributes likely handled internally by Mojang
                        }
                    }

                    target.setHealth(Math.min(20.0, target.getMaxHealth())); // Just in case
                    sender.sendMessage(prefix + GREEN + "Reset attributes for " + target.getName());
                } else {
                    sender.sendMessage(prefix + RED + "Player not found or offline.");
                }
            } else {
                sender.sendMessage(prefix + GRAY + "Usage: /nar reset <player>");
            }
            return true;
        }

        sender.sendMessage(prefix + GRAY + "Usage: /nar <scan|reload|version|debug|reset> [player]");
        return true;
    }

    public boolean cleanAttributes(Player player, boolean silent) {
        boolean clearedAny = false;
        int clearedCount = 0;
        boolean failedToRemove = false;

        Map<UUID, List<String>> globalUUIDMap = new HashMap<>();
        Map<Attribute, List<AttributeModifier>> removeQueue = new HashMap<>();
        Set<String> loggedNamesThisScan = new HashSet<>();

        boolean logUUIDNames = getConfig().getBoolean("logUUIDLikeNames", false);
        boolean enableDebug = getConfig().getBoolean("debugLogs", false);

        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;

            Map<UUID, List<AttributeModifier>> localUUIDMap = new HashMap<>();
            List<AttributeModifier> toRemove = new ArrayList<>();

            for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
                boolean shouldRemove = false;
                String reason = "";

                if (modifier == null) {
                    shouldRemove = true;
                    reason = "null modifier";
                } else {
                    UUID uuid = null;
                    String name = modifier.getName();
                    boolean isBlankName = (name == null || name.codePoints().allMatch(Character::isWhitespace));
                    boolean looksLikeUUID = false;

                    if (name != null) {
                        try {
                            UUID.fromString(name);
                            looksLikeUUID = true;
                        } catch (IllegalArgumentException ignored) {}

                        if (looksLikeUUID && logUUIDNames && !silent) {
                            getLogger().warning("[NAR] UUID-like name detected: '" + name + "' on attribute [" + attribute.name() + "] for player " + player.getName());
                        }
                    }

                    if (isBlankName) {
                        shouldRemove = true;
                        reason = "blank name";
                    } else {
                        try {
                            uuid = modifier.getUniqueId();
                            if (uuid == null) {
                                shouldRemove = true;
                                reason = "null UUID";
                            } else {
                                globalUUIDMap.computeIfAbsent(uuid, k -> new ArrayList<>()).add(attribute.name());
                                localUUIDMap.computeIfAbsent(uuid, k -> new ArrayList<>()).add(modifier);
                            }
                        } catch (IllegalArgumentException ex) {
                            if (enableDebug && loggedNamesThisScan.add(name)) {
                                getLogger().warning("[DEBUG] Modifier has invalid UUID string: " + name);
                            }
                            shouldRemove = true;
                            reason = "invalid UUID string: " + ex.getMessage();
                        }
                    }

                    double amt = modifier.getAmount();
                    if (Double.isNaN(amt) || Double.isInfinite(amt)) {
                        shouldRemove = true;
                        reason = "invalid amount: " + amt;
                    }

                    AttributeModifier.Operation op = modifier.getOperation();
                    if (op == null || !(op == AttributeModifier.Operation.ADD_NUMBER
                            || op == AttributeModifier.Operation.ADD_SCALAR
                            || op == AttributeModifier.Operation.MULTIPLY_SCALAR_1)) {
                        shouldRemove = true;
                        reason = "invalid operation";
                    }

                    if (amt == 0 && (isBlankName || looksLikeUUID)) {
                        shouldRemove = true;
                        reason = "blank or UUID name and zero amount";
                    }
                }

                if (shouldRemove) {
                    toRemove.add(modifier);
                    logRemoved(player, attribute, modifier, reason, silent);
                    clearedAny = true;
                    clearedCount++;
                }
            }

            for (Map.Entry<UUID, List<AttributeModifier>> entry : localUUIDMap.entrySet()) {
                if (entry.getValue().size() > 1) {
                    for (AttributeModifier dupe : entry.getValue()) {
                        if (!toRemove.contains(dupe)) {
                            toRemove.add(dupe);
                            logRemoved(player, attribute, dupe, "duplicate UUID (local)", silent);
                            clearedAny = true;
                            clearedCount++;
                        }
                    }
                }
            }

            if (!toRemove.isEmpty()) {
                removeQueue.put(attribute, toRemove);
            }
        }

        for (Map.Entry<UUID, List<String>> entry : globalUUIDMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                UUID dupeUUID = entry.getKey();
                for (Attribute attribute : Attribute.values()) {
                    AttributeInstance instance = player.getAttribute(attribute);
                    if (instance == null) continue;
                    for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
                        if (modifier != null && dupeUUID.equals(modifier.getUniqueId())) {
                            if (!removeQueue.getOrDefault(attribute, List.of()).contains(modifier)) {
                                removeQueue.computeIfAbsent(attribute, k -> new ArrayList<>()).add(modifier);
                                logRemoved(player, attribute, modifier, "duplicate UUID (global)", silent);
                                clearedAny = true;
                                clearedCount++;
                            }
                        }
                    }
                }
            }
        }

        for (Map.Entry<Attribute, List<AttributeModifier>> entry : removeQueue.entrySet()) {
            AttributeInstance instance = player.getAttribute(entry.getKey());
            if (instance == null) continue;

            for (AttributeModifier mod : entry.getValue()) {
                try {
                    instance.removeModifier(mod);
                    if (instance.getModifiers().contains(mod)) {
                        failedToRemove = true;
                        log(player.getName() + ": Modifier stuck after attempted removal! [" + entry.getKey().name() + "]", true);
                    }
                } catch (Exception e) {
                    failedToRemove = true;
                    getLogger().severe("Failed to remove modifier from " + player.getName() + " [" + entry.getKey().name() + "]");
                    e.printStackTrace();
                }
            }
        }

        if (failedToRemove && kickOnFailure) {
            player.kickPlayer("Attribute data was broken and couldn't be repaired. Please rejoin.");
            log("Kicked " + player.getName() + " due to unrepairable attribute modifiers.", true);
        } else if (clearedAny) {
            log(player.getName() + ": Removed " + clearedCount + " invalid modifiers.", !silent);
        } else if (!clearedAny && logWhenClean && !silent && !silentMode) {
            log(player.getName() + ": No attribute issues found.", true);
        }

        return clearedAny;
    }

    private void logRemoved(Player player, Attribute attribute, AttributeModifier modifier, String reason, boolean silent) {
        StringBuilder detail = new StringBuilder();
        detail.append(GRAY).append("• ").append(BLUE).append(attribute.name()).append(GRAY)
                .append(" → ").append(RED).append(reason);

        if (modifier != null) {
            String modName = modifier.getName();
            String uuidStr;
            try {
                uuidStr = modifier.getUniqueId().toString();
            } catch (IllegalArgumentException e) {
                uuidStr = "invalid-uuid";
                if (getConfig().getBoolean("log-invalid-uuid-names", true)) {
                    getLogger().warning("[logRemoved] Skipped bad UUID: " + modName);
                }
            }

            double amt = modifier.getAmount();
            String op = (modifier.getOperation() != null) ? modifier.getOperation().name() : "null";

            detail.append(GRAY).append(" [name=").append(modName)
                    .append(", uuid=").append(uuidStr)
                    .append(", amt=").append(amt)
                    .append(", op=").append(op).append("]");
        }

        if (!silent && !silentMode) {
            player.sendMessage(detail.toString());
        }

        log(player.getName() + ": Removed [" + attribute.name() + "] modifier due to " + reason, true);
    }

    private void debugInjectModifier(Player player) {
        try {
            AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            AttributeInstance moveSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            AttributeInstance knockback = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);

            // 1. NaN amount
            if (maxHealth != null) {
                maxHealth.addModifier(new AttributeModifier(
                        UUID.randomUUID(),
                        "debug_nan",
                        Double.NaN,
                        AttributeModifier.Operation.ADD_NUMBER
                ));
            }

            // 2. Infinity amount
            if (moveSpeed != null) {
                moveSpeed.addModifier(new AttributeModifier(
                        UUID.randomUUID(),
                        "debug_infinity",
                        Double.POSITIVE_INFINITY,
                        AttributeModifier.Operation.ADD_NUMBER
                ));
            }

            // 3. Blank name (via reflection, safe)
            if (knockback != null) {
                knockback.addModifier(new AttributeModifier(
                        UUID.randomUUID(),
                        " ", // intentionally blank
                        0.0,
                        AttributeModifier.Operation.ADD_NUMBER
                ));
            }

            // 4. Name null + amount 0.0 (detects both conditions together)
//            if (knockback != null) {
//                AttributeModifier mod = new AttributeModifier(
//                        UUID.randomUUID(),
//                        "to_be_nulled",
//                        0.0,
//                        AttributeModifier.Operation.ADD_NUMBER
//                );
//                Field nameField = AttributeModifier.class.getDeclaredField("name");
//                nameField.setAccessible(true);
//                nameField.set(mod, null);
//                knockback.addModifier(mod);
//            }

            // 5. Duplicate UUID (global)
            UUID dupeUUID = UUID.fromString("deadbeef-dead-beef-dead-beef00000001");
            if (maxHealth != null) {
                maxHealth.addModifier(new AttributeModifier(
                        dupeUUID,
                        "dupe_uuid1",
                        1.0,
                        AttributeModifier.Operation.ADD_NUMBER
                ));
            }
            if (moveSpeed != null) {
                moveSpeed.addModifier(new AttributeModifier(
                        dupeUUID,
                        "dupe_uuid2",
                        1.0,
                        AttributeModifier.Operation.ADD_NUMBER
                ));
            }

            player.sendMessage(ChatColor.DARK_AQUA + "[NAR] Injected debug modifiers into " + ChatColor.GRAY + player.getName());
        } catch (Throwable t) {
            getLogger().severe("[NAR] Failed to inject cursed modifiers into " + player.getName());
            t.printStackTrace();
        }
    }

    private void log(String msg, boolean important) {
        if (!silentMode || important) {
            String colored = "\u001B[36m[NAR]\u001B[0m ";
            if (msg.toLowerCase().contains("removed")) {
                colored += "\u001B[33m" + msg; // Yellow
            } else if (msg.toLowerCase().contains("kicked") || msg.toLowerCase().contains("fail")) {
                colored += "\u001B[31m" + msg; // Red
            } else {
                colored += "\u001B[32m" + msg; // Green
            }
            colored += "\u001B[0m";
            getServer().getConsoleSender().sendMessage(colored);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.addAll(List.of("scan", "reload", "version", "debug", "reset"));
        } else if (args.length == 2 && List.of("scan", "debug", "reset").contains(args[0].toLowerCase())) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                suggestions.add(p.getName());
            }
        }
        return suggestions;
    }
}