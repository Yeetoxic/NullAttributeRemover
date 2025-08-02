package gg.lakepvp.nullattributeremover;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class NullAttributeRemover extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("NullAttributeRemover has started. Crashes begone.");
    }

    @Override
    public void onDisable() {
        getLogger().info("NullAttributeRemover has shut down. May the server survive without me.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean clearedAny = false;

        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                for (AttributeModifier modifier : instance.getModifiers()) {
                    instance.removeModifier(modifier);
                    clearedAny = true;
                }
            }
        }

        if (clearedAny) {
            getLogger().info("Cleared attribute modifiers for player: " + player.getName());
        } else {
            getLogger().info("No attribute cleanup needed for player: " + player.getName());
        }
    }
}


