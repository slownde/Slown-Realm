package de.syscall.listener;

import de.syscall.SlownRealm;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final SlownRealm plugin;

    public PlayerQuitListener(SlownRealm plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getRealmManager().handlePlayerQuit(player);
    }
}