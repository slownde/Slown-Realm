package de.syscall.gui;

import de.syscall.SlownRealm;
import de.syscall.data.Realm;
import de.syscall.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitTask;

public class RealmDescriptionHandler implements Listener {

    private final SlownRealm plugin;
    private final Player player;
    private final Realm realm;
    private BukkitTask timeoutTask;

    public RealmDescriptionHandler(SlownRealm plugin, Player player, Realm realm) {
        this.plugin = plugin;
        this.player = player;
        this.realm = realm;
    }

    public void startListening() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        timeoutTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            cleanup();
            player.sendMessage(ColorUtil.component("§cZeitüberschreitung! Beschreibung abgebrochen."));
        }, 600L);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().equals(player)) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();

        if (input.equalsIgnoreCase("cancel")) {
            cleanup();
            player.sendMessage(ColorUtil.component("§cBeschreibung abgebrochen."));
            return;
        }

        if (input.equalsIgnoreCase("clear")) {
            realm.setDescription("");
            plugin.getRealmManager().updateRealm(realm);
            cleanup();
            player.sendMessage(ColorUtil.component("§aBeschreibung wurde gelöscht!"));

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                new RealmManageGUI(plugin, player, realm).open();
            });
            return;
        }

        if (input.length() > 128) {
            player.sendMessage(ColorUtil.component("§cBeschreibung ist zu lang! Maximum: 128 Zeichen"));
            player.sendMessage(ColorUtil.component("§eGib eine neue Beschreibung ein, schreibe §6'clear' §ezum Löschen oder §6'cancel' §ezum Abbrechen:"));
            return;
        }

        realm.setDescription(input);
        plugin.getRealmManager().updateRealm(realm);

        cleanup();
        player.sendMessage(ColorUtil.component("§aBeschreibung erfolgreich geändert!"));

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            new RealmManageGUI(plugin, player, realm).open();
        });
    }

    private void cleanup() {
        HandlerList.unregisterAll(this);
        if (timeoutTask != null) {
            timeoutTask.cancel();
        }
    }
}