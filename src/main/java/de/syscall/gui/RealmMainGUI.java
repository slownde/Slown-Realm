package de.syscall.gui;

import de.syscall.SlownRealm;
import de.syscall.data.Realm;
import de.syscall.util.ColorUtil;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RealmMainGUI {

    private final SlownRealm plugin;
    private final Player player;

    public RealmMainGUI(SlownRealm plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        Gui gui = Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# a b c d e f g #",
                        "# h i j k l m n #",
                        "# # # # o # # # #"
                )
                .addIngredient('#', new BackgroundItem())
                .addIngredient('a', new MyRealmsItem())
                .addIngredient('b', new CreateRealmItem())
                .addIngredient('c', new VisitRealmItem())
                .addIngredient('d', new RealmHomeItem())
                .addIngredient('e', new RealmSettingsItem())
                .addIngredient('f', new RealmInfoItem())
                .addIngredient('g', new QuickTeleportItem())
                .addIngredient('h', new PlayerRealmsItem())
                .addIngredient('i', new PublicRealmsItem())
                .addIngredient('j', new RealmStatsItem())
                .addIngredient('k', new HelpItem())
                .addIngredient('l', new AdminItem())
                .addIngredient('m', new FavoritesItem())
                .addIngredient('n', new RecentRealmsItem())
                .addIngredient('o', new CloseItem())
                .build();

        Window.single()
                .setViewer(player)
                .setTitle(ColorUtil.colorize("&6&lRealm Menü"))
                .setGui(gui)
                .build()
                .open();
    }

    private static class BackgroundItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .setDisplayName("§r");
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class MyRealmsItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            Collection<Realm> ownedRealms = plugin.getRealmManager().getPlayerOwnedRealms(player.getUniqueId());
            int maxRealms = plugin.getConfig().getInt("realm.max-realms-per-player", 3);

            return new ItemBuilder(Material.GRASS_BLOCK)
                    .setDisplayName("§6Meine Realms")
                    .setLegacyLore(buildMyRealmsLore(ownedRealms, maxRealms));
        }

        private List<String> buildMyRealmsLore(Collection<Realm> ownedRealms, int maxRealms) {
            return List.of(
                    "§7Verwalte deine eigenen Realms",
                    "§7Anzahl: §6" + ownedRealms.size() + "§7/§6" + maxRealms,
                    "",
                    "§7Hier kannst du:",
                    "§a• Realms ansehen",
                    "§a• Realms verwalten",
                    "§a• Einstellungen ändern",
                    "§a• Mitglieder hinzufügen",
                    "",
                    ownedRealms.isEmpty() ? "§7Du hast noch keine Realms!" : "§eKlicken zum Öffnen"
            );
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new MyRealmsGUI(plugin, player).open();
        }
    }

    private class CreateRealmItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            Collection<Realm> ownedRealms = plugin.getRealmManager().getPlayerOwnedRealms(player.getUniqueId());
            int maxRealms = plugin.getConfig().getInt("realm.max-realms-per-player", 3);
            boolean canCreate = ownedRealms.size() < maxRealms;

            return new ItemBuilder(canCreate ? Material.EMERALD : Material.BARRIER)
                    .setDisplayName(canCreate ? "§aRealm Erstellen" : "§cMaximum erreicht")
                    .setLegacyLore(buildCreateRealmLore(canCreate, ownedRealms.size(), maxRealms));
        }

        private List<String> buildCreateRealmLore(boolean canCreate, int ownedCount, int maxRealms) {
            if (canCreate) {
                return List.of(
                        "§7Erstelle ein neues Realm",
                        "§7mit verschiedenen Templates",
                        "",
                        "§7Verfügbare Templates:",
                        "§6• Basic Island (Kostenlos)",
                        "§6• Desert Oasis (100 Coins)",
                        "§6• Forest Grove (200 Coins)",
                        "§6• Mountain Peak (500 Coins)",
                        "§6• Ocean Deep (750 Coins)",
                        "",
                        "§eKlicken zum Erstellen"
                );
            } else {
                return List.of(
                        "§7Du hast bereits die maximale",
                        "§7Anzahl an Realms erstellt",
                        "",
                        "§7Aktuell: §c" + ownedCount + "§7/§c" + maxRealms,
                        "",
                        "§7Lösche ein Realm um",
                        "§7Platz für ein neues zu schaffen"
                );
            }
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (!canCreateRealm()) {
                player.sendMessage(ColorUtil.component("§cDu hast bereits die maximale Anzahl an Realms!"));
                return;
            }

            new CreateRealmGUI(plugin, player).open();
        }

        private boolean canCreateRealm() {
            Collection<Realm> ownedRealms = plugin.getRealmManager().getPlayerOwnedRealms(player.getUniqueId());
            int maxRealms = plugin.getConfig().getInt("realm.max-realms-per-player", 3);
            return ownedRealms.size() < maxRealms;
        }
    }

    private class VisitRealmItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            long publicRealms = getPublicRealmsCount();

            return new ItemBuilder(Material.ENDER_PEARL)
                    .setDisplayName("§bRealm Besuchen")
                    .setLegacyLore(buildVisitRealmLore(publicRealms));
        }

        private long getPublicRealmsCount() {
            return plugin.getRealmStorage().getAllRealms().stream()
                    .filter(realm -> realm.getSettings().isPublic() && realm.isActive())
                    .count();
        }

        private List<String> buildVisitRealmLore(long publicRealms) {
            return List.of(
                    "§7Besuche öffentliche Realms",
                    "§7anderer Spieler",
                    "",
                    "§7Verfügbare öffentliche Realms:",
                    "§6" + publicRealms + " Realms",
                    "",
                    "§7Du kannst nur öffentliche",
                    "§7Realms besuchen, außer du",
                    "§7wurdest als Mitglied hinzugefügt",
                    "",
                    publicRealms > 0 ? "§eKlicken zum Durchsuchen" : "§7Keine öffentlichen Realms verfügbar"
            );
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new VisitRealmGUI(plugin, player).open();
        }
    }

    private class RealmHomeItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            Collection<Realm> ownedRealms = plugin.getRealmManager().getPlayerOwnedRealms(player.getUniqueId());
            boolean hasRealm = !ownedRealms.isEmpty();
            Realm homeRealm = hasRealm ? ownedRealms.iterator().next() : null;

            return new ItemBuilder(hasRealm ? Material.RED_BED : Material.BARRIER)
                    .setDisplayName(hasRealm ? "§6Realm Home" : "§cKein Realm")
                    .setLegacyLore(buildRealmHomeLore(hasRealm, homeRealm));
        }

        private List<String> buildRealmHomeLore(boolean hasRealm, Realm homeRealm) {
            if (hasRealm && homeRealm != null) {
                return List.of(
                        "§7Teleportiere zu deinem",
                        "§7Haupt-Realm",
                        "",
                        "§7Dein Haupt-Realm:",
                        "§6" + homeRealm.getName(),
                        "§7Status: " + (homeRealm.isActive() ? "§aAktiv" : "§cInaktiv"),
                        "§7Template: §6" + homeRealm.getTemplateName(),
                        "§7Mitglieder: §6" + homeRealm.getMembers().size(),
                        "",
                        homeRealm.isActive() ? "§eKlicken zum Teleportieren" : "§cRealm ist inaktiv!"
                );
            } else {
                return List.of(
                        "§7Du hast noch kein Realm",
                        "§7Erstelle erst eines!",
                        "",
                        "§7Verwende den §6'Realm Erstellen'",
                        "§7Button um dein erstes",
                        "§7Realm zu erschaffen",
                        "",
                        "§cKein Realm vorhanden"
                );
            }
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            Collection<Realm> ownedRealms = plugin.getRealmManager().getPlayerOwnedRealms(player.getUniqueId());
            if (ownedRealms.isEmpty()) {
                player.sendMessage(ColorUtil.component("§cDu hast noch kein Realm! Erstelle erst eines."));
                return;
            }

            Realm homeRealm = ownedRealms.iterator().next();
            if (!homeRealm.isActive()) {
                player.sendMessage(ColorUtil.component("§cDein Haupt-Realm ist inaktiv!"));
                return;
            }

            teleportToHomeRealm(homeRealm);
        }

        private void teleportToHomeRealm(Realm homeRealm) {
            player.closeInventory();
            player.sendMessage(ColorUtil.component("§7Teleportiere zu deinem Realm..."));
            plugin.getRealmManager().teleportToRealm(player, homeRealm.getRealmId());
        }
    }

    private class RealmSettingsItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            Realm currentRealm = plugin.getRealmManager().getPlayerRealm(player);
            boolean inOwnRealm = currentRealm != null && currentRealm.getOwner().equals(player.getUniqueId());

            return new ItemBuilder(inOwnRealm ? Material.REDSTONE : Material.BARRIER)
                    .setDisplayName(inOwnRealm ? "§cRealm Einstellungen" : "§cNicht verfügbar")
                    .setLegacyLore(buildRealmSettingsLore(inOwnRealm, currentRealm));
        }

        private List<String> buildRealmSettingsLore(boolean inOwnRealm, Realm currentRealm) {
            if (inOwnRealm && currentRealm != null) {
                return List.of(
                        "§7Ändere die Einstellungen",
                        "§7deines aktuellen Realms",
                        "",
                        "§7Aktuelles Realm:",
                        "§6" + currentRealm.getName(),
                        "",
                        "§7Einstellungen:",
                        "§a• PvP, Monster, Tiere",
                        "§a• Feuer, Explosionen",
                        "§a• Wetter, Tag/Nacht-Zyklus",
                        "§a• Öffentlich/Privat",
                        "",
                        "§eKlicken zum Öffnen"
                );
            } else {
                return List.of(
                        "§7Du musst in deinem",
                        "§7eigenen Realm sein um",
                        "§7Einstellungen zu ändern!",
                        "",
                        "§cAnforderungen:",
                        "§c• In der 'realms' Welt sein",
                        "§c• Besitzer des Realms sein",
                        "§c• Realm muss geladen sein",
                        "",
                        "§cNicht verfügbar"
                );
            }
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            Realm currentRealm = plugin.getRealmManager().getPlayerRealm(player);
            if (currentRealm == null || !currentRealm.getOwner().equals(player.getUniqueId())) {
                player.sendMessage(ColorUtil.component("§cDu musst in deinem eigenen Realm sein!"));
                return;
            }

            new RealmSettingsGUI(plugin, player, currentRealm).open();
        }
    }

    private class RealmInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            Realm currentRealm = plugin.getRealmManager().getPlayerRealm(player);
            boolean inRealm = currentRealm != null;

            return new ItemBuilder(inRealm ? Material.BOOK : Material.BARRIER)
                    .setDisplayName(inRealm ? "§9Realm Information" : "§cNicht im Realm")
                    .setLegacyLore(buildRealmInfoLore(inRealm, currentRealm));
        }

        private List<String> buildRealmInfoLore(boolean inRealm, Realm currentRealm) {
            if (inRealm && currentRealm != null) {
                String ownerName = plugin.getServer().getOfflinePlayer(currentRealm.getOwner()).getName();
                return List.of(
                        "§7Zeige detaillierte Informationen",
                        "§7über das aktuelle Realm",
                        "",
                        "§7Aktuelles Realm:",
                        "§6" + currentRealm.getName(),
                        "§7Besitzer: §6" + ownerName,
                        "§7Template: §6" + currentRealm.getTemplateName(),
                        "§7Status: " + (currentRealm.isActive() ? "§aAktiv" : "§cInaktiv"),
                        "",
                        "§7Zeigt alle Details über:",
                        "§a• Besitzer & Mitglieder",
                        "§a• Einstellungen & Permissions",
                        "§a• Standort & Größe",
                        "§a• Erstellungsdatum & Statistiken",
                        "",
                        "§eKlicken für Details"
                );
            } else {
                return List.of(
                        "§7Du befindest dich nicht",
                        "§7in einem Realm!",
                        "",
                        "§7Um Realm-Informationen",
                        "§7anzuzeigen, musst du dich",
                        "§7in der 'realms' Welt befinden",
                        "",
                        "§7Teleportiere zu einem Realm",
                        "§7und versuche es erneut",
                        "",
                        "§cNicht verfügbar"
                );
            }
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            Realm currentRealm = plugin.getRealmManager().getPlayerRealm(player);
            if (currentRealm == null) {
                player.sendMessage(ColorUtil.component("§cDu befindest dich nicht in einem Realm!"));
                return;
            }

            new RealmInfoGUI(plugin, player, currentRealm).open();
        }
    }

    private class QuickTeleportItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            Collection<Realm> ownedRealms = plugin.getRealmManager().getPlayerOwnedRealms(player.getUniqueId());

            return new ItemBuilder(Material.COMPASS)
                    .setDisplayName("§5Quick Teleport")
                    .setLegacyLore(buildQuickTeleportLore(ownedRealms));
        }

        private List<String> buildQuickTeleportLore(Collection<Realm> ownedRealms) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Schnell-Teleport zu deinen");
            lore.add("§7Realms ohne Umwege");
            lore.add("");
            lore.add("§7Deine Realms: §6" + ownedRealms.size());
            lore.add("");

            if (ownedRealms.isEmpty()) {
                lore.add("§7Keine Realms vorhanden");
            } else {
                lore.add("§7Verfügbare Realms:");
                addRealmPreview(lore, ownedRealms);
            }
            lore.add("");
            lore.add(ownedRealms.isEmpty() ? "§cKeine Realms" : "§eKlicken für Quick-Access");

            return lore;
        }

        private void addRealmPreview(List<String> lore, Collection<Realm> ownedRealms) {
            int count = 0;
            for (Realm realm : ownedRealms) {
                if (count >= 3) break;
                lore.add("§6• " + realm.getName() + " " + (realm.isActive() ? "§a●" : "§c●"));
                count++;
            }
            if (ownedRealms.size() > 3) {
                lore.add("§7... und " + (ownedRealms.size() - 3) + " weitere");
            }
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            Collection<Realm> ownedRealms = plugin.getRealmManager().getPlayerOwnedRealms(player.getUniqueId());
            if (ownedRealms.isEmpty()) {
                player.sendMessage(ColorUtil.component("§cDu hast keine Realms!"));
                return;
            }

            handleQuickTeleport(ownedRealms);
        }

        private void handleQuickTeleport(Collection<Realm> ownedRealms) {
            if (ownedRealms.size() == 1) {
                Realm realm = ownedRealms.iterator().next();
                if (!realm.isActive()) {
                    player.sendMessage(ColorUtil.component("§cDein Realm ist inaktiv!"));
                    return;
                }
                executeQuickTeleport(realm);
            } else {
                new MyRealmsGUI(plugin, player).open();
            }
        }

        private void executeQuickTeleport(Realm realm) {
            player.closeInventory();
            player.sendMessage(ColorUtil.component("§7Quick-Teleport zu §6" + realm.getName() + "§7..."));
            plugin.getRealmManager().teleportToRealm(player, realm.getRealmId());
        }
    }

    private class PlayerRealmsItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            int onlinePlayers = plugin.getServer().getOnlinePlayers().size();
            int playersWithRealms = countPlayersWithRealms();

            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setDisplayName("§aSpieler Realms")
                    .setLegacyLore(List.of(
                            "§7Entdecke Realms von",
                            "§7anderen Spielern",
                            "",
                            "§7Online Spieler: §6" + onlinePlayers,
                            "§7Mit eigenen Realms: §6" + playersWithRealms,
                            "",
                            "§7Hier findest du:",
                            "§a• Öffentliche Realms",
                            "§a• Featured Builds",
                            "§a• Community Projekte",
                            "",
                            "§eKlicken zum Durchsuchen"
                    ));
        }

        private int countPlayersWithRealms() {
            return (int) plugin.getServer().getOnlinePlayers().stream()
                    .filter(p -> !plugin.getRealmManager().getPlayerOwnedRealms(p.getUniqueId()).isEmpty())
                    .count();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new VisitRealmGUI(plugin, player).open();
        }
    }

    private class PublicRealmsItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            long publicCount = getPublicRealmsCount();

            return new ItemBuilder(Material.BEACON)
                    .setDisplayName("§eÖffentliche Realms")
                    .setLegacyLore(buildPublicRealmsLore(publicCount));
        }

        private long getPublicRealmsCount() {
            return plugin.getRealmStorage().getAllRealms().stream()
                    .filter(realm -> realm.getSettings().isPublic() && realm.isActive())
                    .count();
        }

        private List<String> buildPublicRealmsLore(long publicCount) {
            return List.of(
                    "§7Alle öffentlich zugänglichen",
                    "§7Realms im Netzwerk",
                    "",
                    "§7Verfügbar: §6" + publicCount + " Realms",
                    "",
                    "§7Kategorien:",
                    "§a• Kreative Builds",
                    "§a• Survival Projekte",
                    "§a• Mini-Games",
                    "§a• Showcase Realms",
                    "",
                    publicCount > 0 ? "§eKlicken zum Erkunden" : "§7Keine öffentlichen Realms"
            );
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new VisitRealmGUI(plugin, player).open();
        }
    }

    private class RealmStatsItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.PAPER)
                    .setDisplayName("§9Realm Statistiken")
                    .setLegacyLore(buildRealmStatsLore());
        }

        private List<String> buildRealmStatsLore() {
            Collection<Realm> allRealms = plugin.getRealmStorage().getAllRealms();
            Collection<Realm> playerRealms = plugin.getRealmManager().getPlayerOwnedRealms(player.getUniqueId());
            long activeRealms = allRealms.stream().filter(Realm::isActive).count();
            long publicRealms = allRealms.stream().filter(realm -> realm.getSettings().isPublic()).count();

            return List.of(
                    "§7Übersicht über alle",
                    "§7Realms im System",
                    "",
                    "§7§lGlobal:",
                    "§7Gesamt Realms: §6" + allRealms.size(),
                    "§7Aktive Realms: §6" + activeRealms,
                    "§7Öffentliche: §6" + publicRealms,
                    "§7Private: §6" + (allRealms.size() - publicRealms),
                    "",
                    "§7§lDeine Statistiken:",
                    "§7Deine Realms: §6" + playerRealms.size(),
                    "§7Davon aktiv: §6" + playerRealms.stream().filter(Realm::isActive).count(),
                    "§7Davon öffentlich: §6" + playerRealms.stream().filter(r -> r.getSettings().isPublic()).count(),
                    "",
                    "§eKlicken für Details"
            );
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            player.sendMessage(ColorUtil.component("§6═══ Realm Statistiken ═══"));
            player.sendMessage(ColorUtil.component("§7Detaillierte Statistiken werden in einem zukünftigen Update verfügbar sein!"));
        }
    }

    private class HelpItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.ENCHANTED_BOOK)
                    .setDisplayName("§eHilfe & Anleitungen")
                    .setLegacyLore(List.of(
                            "§7Benötigst du Hilfe mit",
                            "§7dem Realm-System?",
                            "",
                            "§7Verfügbare Hilfen:",
                            "§a• Erste Schritte Guide",
                            "§a• Command Übersicht",
                            "§a• FAQ & Tipps",
                            "§a• Troubleshooting",
                            "",
                            "§7Commands:",
                            "§6/realm home §7- Zum Realm",
                            "§6/realm invite <spieler> §7- Einladen",
                            "§6/realm visit <spieler> §7- Besuchen",
                            "",
                            "§eKlicken für Hilfe"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            sendHelpInformation();
        }

        private void sendHelpInformation() {
            player.closeInventory();
            player.sendMessage(ColorUtil.component("§6═══ Realm System Hilfe ═══"));
            player.sendMessage(ColorUtil.component("§7"));
            player.sendMessage(ColorUtil.component("§e§lCommands:"));
            player.sendMessage(ColorUtil.component("§6/realm §7- Hauptmenü öffnen"));
            player.sendMessage(ColorUtil.component("§6/realm home §7- Zu deinem Realm"));
            player.sendMessage(ColorUtil.component("§6/realm create <n> §7- Neues Realm"));
            player.sendMessage(ColorUtil.component("§6/realm invite <spieler> §7- Spieler einladen"));
            player.sendMessage(ColorUtil.component("§6/realm visit <spieler> §7- Realm besuchen"));
            player.sendMessage(ColorUtil.component("§7"));
            player.sendMessage(ColorUtil.component("§e§lTipps:"));
            player.sendMessage(ColorUtil.component("§7• Verwende das GUI für einfache Verwaltung"));
            player.sendMessage(ColorUtil.component("§7• Öffentliche Realms können von allen besucht werden"));
            player.sendMessage(ColorUtil.component("§7• Mitglieder können in deinem Realm bauen"));
            player.sendMessage(ColorUtil.component("§7• Einstellungen gelten nur für dein Realm"));
        }
    }

    private class AdminItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            boolean isAdmin = player.hasPermission("slownrealm.admin");

            return new ItemBuilder(isAdmin ? Material.NETHER_STAR : Material.BARRIER)
                    .setDisplayName(isAdmin ? "§cAdmin Panel" : "§cKeine Berechtigung")
                    .setLegacyLore(buildAdminLore(isAdmin));
        }

        private List<String> buildAdminLore(boolean isAdmin) {
            if (isAdmin) {
                return List.of(
                        "§7Administrator Funktionen",
                        "§7für das Realm-System",
                        "",
                        "§7Admin Befehle:",
                        "§c• Realm Management",
                        "§c• Template Verwaltung",
                        "§c• System Statistiken",
                        "§c• Performance Monitoring",
                        "",
                        "§eKlicken für Admin Panel"
                );
            } else {
                return List.of(
                        "§7Du hast keine Administrator",
                        "§7Berechtigung für das",
                        "§7Realm-System",
                        "",
                        "§cBenötigte Permission:",
                        "§cslownrealm.admin"
                );
            }
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (!player.hasPermission("slownrealm.admin")) {
                player.sendMessage(ColorUtil.component("§cDu hast keine Administrator-Berechtigung!"));
                return;
            }

            player.closeInventory();
            player.sendMessage(ColorUtil.component("§cAdmin Panel wird in einem zukünftigen Update verfügbar sein!"));
        }
    }

    private static class FavoritesItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.HEART_OF_THE_SEA)
                    .setDisplayName("§dFavoriten")
                    .setLegacyLore(List.of(
                            "§7Deine favorisierten Realms",
                            "§7für schnellen Zugriff",
                            "",
                            "§7Favoriten: §60",
                            "",
                            "§7Füge Realms zu deinen",
                            "§7Favoriten hinzu um sie",
                            "§7schneller zu finden",
                            "",
                            "§7Feature kommt bald!"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            player.sendMessage(ColorUtil.component("§7Das Favoriten-System wird in einem zukünftigen Update verfügbar sein!"));
        }
    }

    private static class RecentRealmsItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.RECOVERY_COMPASS)
                    .setDisplayName("§bZuletzt besucht")
                    .setLegacyLore(List.of(
                            "§7Realms die du kürzlich",
                            "§7besucht hast",
                            "",
                            "§7Verlauf: §60 Einträge",
                            "",
                            "§7Hier findest du:",
                            "§a• Kürzlich besuchte Realms",
                            "§a• Chronologische Reihenfolge",
                            "§a• Quick-Access Buttons",
                            "",
                            "§7Feature kommt bald!"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            player.sendMessage(ColorUtil.component("§7Das Verlauf-System wird in einem zukünftigen Update verfügbar sein!"));
        }
    }

    private static class CloseItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.BARRIER)
                    .setDisplayName("§cMenü schließen")
                    .setLegacyLore(List.of(
                            "§7Schließe das Realm-Menü",
                            "§7und kehre zum Spiel zurück",
                            "",
                            "§7Du kannst jederzeit mit",
                            "§6/realm §7das Menü wieder öffnen",
                            "",
                            "§eKlicken zum Schließen"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            player.closeInventory();
            player.sendMessage(ColorUtil.component("§7Realm-Menü geschlossen. Verwende §6/realm §7zum erneuten Öffnen."));
        }
    }
}