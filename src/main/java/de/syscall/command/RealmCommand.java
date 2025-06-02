package de.syscall.command;

import de.syscall.SlownRealm;
import de.syscall.data.Realm;
import de.syscall.gui.RealmMainGUI;
import de.syscall.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RealmCommand implements CommandExecutor, TabCompleter {

    private final SlownRealm plugin;
    private final List<String> subCommands = Arrays.asList("menu", "create", "delete", "visit", "home", "invite", "kick", "settings", "list", "info", "reload");

    public RealmCommand(SlownRealm plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.component("&cDieser Command kann nur von Spielern ausgeführt werden!"));
            return true;
        }

        if (args.length == 0) {
            new RealmMainGUI(plugin, player).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "menu" -> {
                new RealmMainGUI(plugin, player).open();
                return true;
            }

            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage(ColorUtil.component("&cVerwendung: /realm create <name>"));
                    return true;
                }

                String realmName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                handleCreateRealm(player, realmName);
                return true;
            }

            case "delete" -> {
                if (args.length < 2) {
                    player.sendMessage(ColorUtil.component("&cVerwendung: /realm delete <name>"));
                    return true;
                }

                String realmName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                handleDeleteRealm(player, realmName);
                return true;
            }

            case "visit" -> {
                if (args.length < 2) {
                    player.sendMessage(ColorUtil.component("&cVerwendung: /realm visit <spieler>"));
                    return true;
                }

                String targetPlayerName = args[1];
                handleVisitRealm(player, targetPlayerName);
                return true;
            }

            case "home" -> {
                handleRealmHome(player);
                return true;
            }

            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage(ColorUtil.component("&cVerwendung: /realm invite <spieler>"));
                    return true;
                }

                String targetPlayerName = args[1];
                handleInvitePlayer(player, targetPlayerName);
                return true;
            }

            case "kick" -> {
                if (args.length < 2) {
                    player.sendMessage(ColorUtil.component("&cVerwendung: /realm kick <spieler>"));
                    return true;
                }

                String targetPlayerName = args[1];
                handleKickPlayer(player, targetPlayerName);
                return true;
            }

            case "list" -> {
                handleListRealms(player);
                return true;
            }

            case "info" -> {
                handleRealmInfo(player);
                return true;
            }

            case "reload" -> {
                if (!player.hasPermission("slownrealm.admin")) {
                    player.sendMessage(ColorUtil.component("&cDu hast keine Berechtigung für diesen Command!"));
                    return true;
                }

                plugin.getSchematicManager().reloadTemplates();
                player.sendMessage(ColorUtil.component("&aSlown-Realm Konfiguration neu geladen!"));
                return true;
            }

            default -> {
                player.sendMessage(ColorUtil.component("&cUnbekannter Subcommand! Verwende &6/realm menu &cfür das Hauptmenü."));
                return true;
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    if (subCommand.equals("reload") && !sender.hasPermission("slownrealm.admin")) {
                        continue;
                    }
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("visit") || subCommand.equals("invite") || subCommand.equals("kick")) {
                String input = args[1].toLowerCase();
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(input)) {
                        completions.add(player.getName());
                    }
                }
            }
        }

        return completions;
    }

    private void handleCreateRealm(Player player, String name) {
        if (name.length() > 32) {
            player.sendMessage(ColorUtil.component("&cRealm-Name ist zu lang! Maximum: 32 Zeichen"));
            return;
        }

        var ownedRealms = plugin.getRealmManager().getPlayerOwnedRealms(player.getUniqueId());
        int maxRealms = plugin.getConfig().getInt("realm.max-realms-per-player", 3);

        if (ownedRealms.size() >= maxRealms) {
            player.sendMessage(ColorUtil.component("&cDu hast bereits die maximale Anzahl an Realms! (" + maxRealms + ")"));
            return;
        }

        player.sendMessage(ColorUtil.component("&7Erstelle Realm..."));
        plugin.getRealmManager().createRealm(player, name, "basic").thenAccept(realm -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ColorUtil.component("&aRealm &6" + name + " &aerfolgreich erstellt!"));
            });
        });
    }

    private void handleDeleteRealm(Player player, String name) {
        var ownedRealms = plugin.getRealmManager().getPlayerOwnedRealms(player.getUniqueId());
        Realm targetRealm = ownedRealms.stream()
                .filter(realm -> realm.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);

        if (targetRealm == null) {
            player.sendMessage(ColorUtil.component("&cRealm nicht gefunden oder du bist nicht der Besitzer!"));
            return;
        }

        plugin.getRealmManager().deleteRealm(targetRealm.getRealmId()).thenAccept(success -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(ColorUtil.component("&aRealm &6" + name + " &aerfolgreich gelöscht!"));
                } else {
                    player.sendMessage(ColorUtil.component("&cFehler beim Löschen des Realms!"));
                }
            });
        });
    }

    private void handleVisitRealm(Player player, String targetPlayerName) {
        Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(ColorUtil.component("&cSpieler nicht gefunden!"));
            return;
        }

        var targetRealms = plugin.getRealmManager().getPlayerOwnedRealms(targetPlayer.getUniqueId());
        if (targetRealms.isEmpty()) {
            player.sendMessage(ColorUtil.component("&cDieser Spieler hat keine Realms!"));
            return;
        }

        Realm publicRealm = targetRealms.stream()
                .filter(realm -> realm.getSettings().isPublic())
                .findFirst()
                .orElse(null);

        if (publicRealm == null) {
            player.sendMessage(ColorUtil.component("&cDieser Spieler hat keine öffentlichen Realms!"));
            return;
        }

        player.sendMessage(ColorUtil.component("&7Teleportiere zu " + targetPlayerName + "s Realm..."));
        plugin.getRealmManager().teleportToRealm(player, publicRealm.getRealmId());
    }

    private void handleRealmHome(Player player) {
        var ownedRealms = plugin.getRealmManager().getPlayerOwnedRealms(player.getUniqueId());
        if (ownedRealms.isEmpty()) {
            player.sendMessage(ColorUtil.component("&cDu hast noch kein Realm! Verwende &6/realm create <name>"));
            return;
        }

        Realm homeRealm = ownedRealms.iterator().next();
        player.sendMessage(ColorUtil.component("&7Teleportiere zu deinem Realm..."));
        plugin.getRealmManager().teleportToRealm(player, homeRealm.getRealmId());
    }

    private void handleInvitePlayer(Player player, String targetPlayerName) {
        Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(ColorUtil.component("&cSpieler nicht gefunden!"));
            return;
        }

        Realm currentRealm = plugin.getRealmManager().getPlayerRealm(player);
        if (currentRealm == null || !currentRealm.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ColorUtil.component("&cDu musst in deinem eigenen Realm sein!"));
            return;
        }

        if (currentRealm.isMember(targetPlayer.getUniqueId())) {
            player.sendMessage(ColorUtil.component("&cDieser Spieler ist bereits Mitglied!"));
            return;
        }

        currentRealm.addMember(targetPlayer.getUniqueId());
        plugin.getRealmManager().updateRealm(currentRealm);

        player.sendMessage(ColorUtil.component("&a" + targetPlayerName + " wurde zu deinem Realm hinzugefügt!"));
        targetPlayer.sendMessage(ColorUtil.component("&7Du wurdest zu &6" + player.getName() + "&7s Realm eingeladen!"));
    }

    private void handleKickPlayer(Player player, String targetPlayerName) {
        Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(ColorUtil.component("&cSpieler nicht gefunden!"));
            return;
        }

        Realm currentRealm = plugin.getRealmManager().getPlayerRealm(player);
        if (currentRealm == null || !currentRealm.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ColorUtil.component("&cDu musst in deinem eigenen Realm sein!"));
            return;
        }

        if (!currentRealm.isMember(targetPlayer.getUniqueId())) {
            player.sendMessage(ColorUtil.component("&cDieser Spieler ist kein Mitglied!"));
            return;
        }

        currentRealm.removeMember(targetPlayer.getUniqueId());
        plugin.getRealmManager().updateRealm(currentRealm);

        if (plugin.getRealmManager().isPlayerInRealm(targetPlayer, currentRealm.getRealmId())) {
            targetPlayer.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
        }

        player.sendMessage(ColorUtil.component("&a" + targetPlayerName + " wurde aus deinem Realm entfernt!"));
        targetPlayer.sendMessage(ColorUtil.component("&7Du wurdest aus &6" + player.getName() + "&7s Realm entfernt!"));
    }

    private void handleListRealms(Player player) {
        var ownedRealms = plugin.getRealmManager().getPlayerOwnedRealms(player.getUniqueId());

        if (ownedRealms.isEmpty()) {
            player.sendMessage(ColorUtil.component("&7Du hast keine Realms."));
            return;
        }

        player.sendMessage(ColorUtil.component("&6Deine Realms:"));
        for (Realm realm : ownedRealms) {
            String status = realm.isActive() ? "&a✓" : "&c✗";
            String loaded = realm.isLoaded() ? "&a[Geladen]" : "&7[Ungeladen]";
            player.sendMessage(ColorUtil.component("&7- " + status + " &6" + realm.getName() + " " + loaded));
        }
    }

    private void handleRealmInfo(Player player) {
        Realm currentRealm = plugin.getRealmManager().getPlayerRealm(player);
        if (currentRealm == null) {
            player.sendMessage(ColorUtil.component("&cDu befindest dich nicht in einem Realm!"));
            return;
        }

        String ownerName = plugin.getServer().getOfflinePlayer(currentRealm.getOwner()).getName();
        player.sendMessage(ColorUtil.component("&6&l◆ Realm Information ◆"));
        player.sendMessage(ColorUtil.component("&7&m─────────────────────"));
        player.sendMessage(ColorUtil.component("&7Name: &6" + currentRealm.getName()));
        player.sendMessage(ColorUtil.component("&7Besitzer: &6" + ownerName));
        player.sendMessage(ColorUtil.component("&7Template: &6" + currentRealm.getTemplateName()));
        player.sendMessage(ColorUtil.component("&7Mitglieder: &6" + currentRealm.getMembers().size()));
        player.sendMessage(ColorUtil.component("&7Status: " + (currentRealm.isActive() ? "&aAktiv" : "&cInaktiv")));
        player.sendMessage(ColorUtil.component("&7&m─────────────────────"));
    }
}