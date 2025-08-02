package gg.lakepvp.nullattributeremover;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class NullAttributeRemover extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("NullAttributeRemover has started. Crashes begone.");

        // Schedule repeating cleanup every 30 seconds
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                cleanAttributes(player, true); // true = silent logging
            }
        }, 20L * 30, 20L * 30); // Delay, then repeat every 30 seconds
    }

    @Override
    public void onDisable() {
        getLogger().info("NullAttributeRemover has shut down. May the server survive without me.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        cleanAttributes(event.getPlayer(), false);
    }

    public void cleanAttributes(Player player, boolean silent) {
        boolean clearedAny = false;
        int clearedCount = 0;
        boolean failedToRemove = false;

        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                List<AttributeModifier> toRemove = new ArrayList<>();
                for (AttributeModifier modifier : instance.getModifiers()) {
                    if (modifier == null || modifier.getName() == null) {
                        toRemove.add(modifier);
                    }
                }
                for (AttributeModifier mod : toRemove) {
                    try {
                        instance.removeModifier(mod);
                        clearedAny = true;
                        clearedCount++;
                        if (!silent) {
                            getLogger().warning("Removed modifier from " + player.getName() +
                                    " [Attribute: " + attribute.name() +
                                    ", Name: " + mod.getName() +
                                    ", UUID: " + mod.getUniqueId() +
                                    ", Operation: " + mod.getOperation() + "]");
                        }
                    } catch (Exception e) {
                        failedToRemove = true;
                        getLogger().severe("Failed to remove modifier from " + player.getName() +
                                " [Attribute: " + attribute.name() + "]");
                        e.printStackTrace();
                    }
                }
            }
        }

        if (failedToRemove) {
            player.kickPlayer("Attribute data was broken and couldn't be repaired. Please rejoin.");
            getLogger().severe("Kicked " + player.getName() + " due to broken attributes.");
        } else if (clearedAny) {
            getLogger().info("Cleaned " + clearedCount + " invalid modifiers from " + player.getName());
        } else if (!clearedAny && !silent) {
            getLogger().info("No attribute issues found for " + player.getName() + ". All clear!");
        }
    }
}


