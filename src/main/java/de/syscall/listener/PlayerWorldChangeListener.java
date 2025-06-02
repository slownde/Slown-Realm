package de.syscall.listener;

import de.syscall.SlownRealm;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class PlayerWorldChangeListener implements Listener {

    private final SlownRealm plugin;

    public PlayerWorldChangeListener(SlownRealm plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        plugin.getRealmManager().handleWorldChange(player);
    }
}