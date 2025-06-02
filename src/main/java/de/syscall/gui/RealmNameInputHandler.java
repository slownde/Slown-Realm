package de.syscall.gui;

import de.syscall.SlownRealm;
import de.syscall.data.RealmTemplate;
import de.syscall.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitTask;

public class RealmNameInputHandler implements Listener {

    private final SlownRealm plugin;
    private final Player player;
    private final RealmTemplate template;
    private BukkitTask timeoutTask;

    public RealmNameInputHandler(SlownRealm plugin, Player player, RealmTemplate template) {
        this.plugin = plugin;
        this.player = player;
        this.template = template;
    }

    public void startListening() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        timeoutTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            cleanup();
            player.sendMessage(ColorUtil.component("§cZeitüberschreitung! Realm-Erstellung abgebrochen."));
        }, 600L);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().equals(player)) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();

        if (input.equalsIgnoreCase("cancel")) {
            cleanup();
            player.sendMessage(ColorUtil.component("§cRealm-Erstellung abgebrochen."));
            return;
        }

        if (input.length() > 32) {
            player.sendMessage(ColorUtil.component("§cName ist zu lang! Maximum: 32 Zeichen"));
            player.sendMessage(ColorUtil.component("§eGib einen Namen ein oder schreibe §6'cancel' §ezum Abbrechen:"));
            return;
        }

        if (input.length() < 3) {
            player.sendMessage(ColorUtil.component("§cName ist zu kurz! Minimum: 3 Zeichen"));
            player.sendMessage(ColorUtil.component("§eGib einen Namen ein oder schreibe §6'cancel' §ezum Abbrechen:"));
            return;
        }

        var ownedRealms = plugin.getRealmManager().getPlayerOwnedRealms(player.getUniqueId());
        int maxRealms = plugin.getConfig().getInt("realm.max-realms-per-player", 3);

        if (ownedRealms.size() >= maxRealms) {
            cleanup();
            player.sendMessage(ColorUtil.component("§cDu hast bereits die maximale Anzahl an Realms! (" + maxRealms + ")"));
            return;
        }

        if (template.getCost() > 0) {
            double playerCoins = plugin.getVecturAPI().getCoins(player);
            if (playerCoins < template.getCost()) {
                cleanup();
                player.sendMessage(ColorUtil.component("§cDu hast nicht genug Coins! Benötigt: §6" +
                        String.format("%.2f", template.getCost()) + " Coins"));
                return;
            }

            plugin.getVecturAPI().removeCoins(player, template.getCost());
            player.sendMessage(ColorUtil.component("§7§6" + String.format("%.2f", template.getCost()) + " Coins §7wurden abgezogen."));
        }

        cleanup();
        player.sendMessage(ColorUtil.component("§7Erstelle Realm §6" + input + "§7..."));

        plugin.getRealmManager().createRealm(player, input, template.getName()).thenAccept(realm -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ColorUtil.component("§aRealm §6" + input + " §aerfolgreich erstellt!"));
                player.sendMessage(ColorUtil.component("§7Verwende §6/realm home §7zum Teleportieren!"));
            });
        });
    }

    private void cleanup() {
        HandlerList.unregisterAll(this);
        if (timeoutTask != null) {
            timeoutTask.cancel();
        }
    }
}