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

public class RealmRenameHandler implements Listener {

    private final SlownRealm plugin;
    private final Player player;
    private final Realm realm;
    private BukkitTask timeoutTask;

    public RealmRenameHandler(SlownRealm plugin, Player player, Realm realm) {
        this.plugin = plugin;
        this.player = player;
        this.realm = realm;
    }

    public void startListening() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        timeoutTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            cleanup();
            player.sendMessage(ColorUtil.component("§cZeitüberschreitung! Umbenennung abgebrochen."));
        }, 600L);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().equals(player)) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();

        if (input.equalsIgnoreCase("cancel")) {
            cleanup();
            player.sendMessage(ColorUtil.component("§cUmbenennung abgebrochen."));
            return;
        }

        if (input.length() > 32) {
            player.sendMessage(ColorUtil.component("§cName ist zu lang! Maximum: 32 Zeichen"));
            player.sendMessage(ColorUtil.component("§eGib einen neuen Namen ein oder schreibe §6'cancel' §ezum Abbrechen:"));
            return;
        }

        if (input.length() < 3) {
            player.sendMessage(ColorUtil.component("§cName ist zu kurz! Minimum: 3 Zeichen"));
            player.sendMessage(ColorUtil.component("§eGib einen neuen Namen ein oder schreibe §6'cancel' §ezum Abbrechen:"));
            return;
        }

        String oldName = realm.getName();
        realm.setName(input);
        plugin.getRealmManager().updateRealm(realm);

        cleanup();
        player.sendMessage(ColorUtil.component("§aRealm erfolgreich von §6" + oldName + " §azu §6" + input + " §aumbenannt!"));

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