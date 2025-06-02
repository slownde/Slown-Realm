package de.syscall.listener;

import de.syscall.SlownRealm;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final SlownRealm plugin;

    public PlayerJoinListener(SlownRealm plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.getWorld().getName().equals("realms")) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getRealmManager().getPlayerRealm(player) == null) {
                    player.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                }
            }, 20L);
        }
    }
}