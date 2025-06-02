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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RealmInfoGUI {

    private final SlownRealm plugin;
    private final Player player;
    private final Realm realm;

    public RealmInfoGUI(SlownRealm plugin, Player player, Realm realm) {
        this.plugin = plugin;
        this.player = player;
        this.realm = realm;
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
                .addIngredient('a', new BasicInfoItem())
                .addIngredient('b', new OwnerInfoItem())
                .addIngredient('c', new TemplateInfoItem())
                .addIngredient('d', new MembersInfoItem())
                .addIngredient('e', new SettingsInfoItem())
                .addIngredient('f', new StatusInfoItem())
                .addIngredient('g', new LocationInfoItem())
                .addIngredient('h', new SizeInfoItem())
                .addIngredient('i', new CreationInfoItem())
                .addIngredient('j', new AccessInfoItem())
                .addIngredient('k', new WorldInfoItem())
                .addIngredient('l', new StatsInfoItem())
                .addIngredient('m', new PermissionInfoItem())
                .addIngredient('n', new TeleportItem())
                .addIngredient('o', new BackItem())
                .build();

        Window window = Window.single()
                .setViewer(player)
                .setTitle(ColorUtil.colorize("&9Info: " + realm.getName()))
                .setGui(gui)
                .build();

        window.open();
    }

    private static class BackgroundItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE)
                    .setDisplayName("§r");
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class BasicInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            String createdDate = dateFormat.format(new Date(realm.getCreatedTime()));
            String lastAccessed = dateFormat.format(new Date(realm.getLastAccessed()));

            List<String> lore = new ArrayList<>();
            lore.add("§7Name: §6" + realm.getName());
            lore.add("§7ID: §e" + realm.getRealmId().toString().substring(0, 8) + "...");
            lore.add("§7Erstellt: §6" + createdDate);
            lore.add("§7Zuletzt besucht: §6" + lastAccessed);
            lore.add("");

            if (realm.getDescription().isEmpty()) {
                lore.add("§7Keine Beschreibung");
            } else {
                lore.add("§7Beschreibung:");
                String[] lines = realm.getDescription().split(" ");
                StringBuilder currentLine = new StringBuilder();
                for (String word : lines) {
                    if (currentLine.length() + word.length() > 30) {
                        lore.add("§f" + currentLine.toString());
                        currentLine = new StringBuilder(word);
                    } else {
                        if (!currentLine.isEmpty()) currentLine.append(" ");
                        currentLine.append(word);
                    }
                }
                if (!currentLine.isEmpty()) {
                    lore.add("§f" + currentLine.toString());
                }
            }

            return new ItemBuilder(Material.BOOK)
                    .setDisplayName("§6Grundinformationen")
                    .setLegacyLore(lore);
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class OwnerInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            String ownerName = plugin.getServer().getOfflinePlayer(realm.getOwner()).getName();
            boolean isOwner = realm.getOwner().equals(player.getUniqueId());
            boolean ownerOnline = plugin.getServer().getPlayer(realm.getOwner()) != null;

            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setDisplayName("§eBesitzer")
                    .setLegacyLore(List.of(
                            "§7Besitzer: §6" + ownerName,
                            "§7Status: " + (ownerOnline ? "§aOnline" : "§7Offline"),
                            "§7Deine Rolle: " + (isOwner ? "§aBesitzer" : realm.isMember(player.getUniqueId()) ? "§6Mitglied" : "§7Besucher"),
                            "",
                            isOwner ? "§7Du hast volle Kontrolle" : "§7Du kannst " + (realm.hasAccess(player.getUniqueId()) ? "§abauen" : "§cnur schauen"),
                            isOwner ? "§7über dieses Realm" : "§7in diesem Realm"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class TemplateInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            String templateDisplayName = getTemplateDisplayName();
            var template = plugin.getSchematicManager().getTemplate(realm.getTemplateName());

            return new ItemBuilder(template != null ? template.getIcon() : Material.MAP)
                    .setDisplayName("§aTemplate")
                    .setLegacyLore(List.of(
                            "§7Template: §6" + templateDisplayName,
                            "§7Interne ID: §e" + realm.getTemplateName(),
                            template != null ? "§7Kosten: §6" + String.format("%.2f", template.getCost()) + " Coins" : "§7Kosten: §cUnbekannt",
                            "",
                            "§7Das Template bestimmt",
                            "§7das Aussehen und Layout",
                            "§7des Realms bei der Erstellung"
                    ));
        }

        private String getTemplateDisplayName() {
            var template = plugin.getSchematicManager().getTemplate(realm.getTemplateName());
            return template != null ? template.getDisplayName() : realm.getTemplateName();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class MembersInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            List<String> lore = new ArrayList<>();
            lore.add("§7Mitglieder: §6" + realm.getMembers().size());
            lore.add("§7Besucher: §6" + realm.getVisitors().size());
            lore.add("§7Gesamt-Zugriff: §6" + (realm.getMembers().size() + realm.getVisitors().size() + 1));
            lore.add("");

            if (realm.getMembers().isEmpty()) {
                lore.add("§7Keine Mitglieder");
            } else {
                lore.add("§7Mitglieder-Liste:");
                int count = 0;
                for (var memberUuid : realm.getMembers()) {
                    if (count >= 5) {
                        lore.add("§7... und " + (realm.getMembers().size() - 5) + " weitere");
                        break;
                    }
                    String memberName = plugin.getServer().getOfflinePlayer(memberUuid).getName();
                    boolean memberOnline = plugin.getServer().getPlayer(memberUuid) != null;
                    lore.add("§6- " + memberName + " " + (memberOnline ? "§a●" : "§7●"));
                    count++;
                }
            }

            return new ItemBuilder(Material.GOLDEN_HELMET)
                    .setDisplayName("§aMitglieder")
                    .setLegacyLore(lore);
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (realm.getOwner().equals(player.getUniqueId())) {
                new RealmMembersGUI(plugin, player, realm).open();
            }
        }
    }

    private class SettingsInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            var settings = realm.getSettings();

            return new ItemBuilder(Material.REDSTONE)
                    .setDisplayName("§cEinstellungen")
                    .setLegacyLore(List.of(
                            "§7Öffentlich: " + (settings.isPublic() ? "§aJa" : "§cNein"),
                            "§7PvP: " + (settings.isPvpEnabled() ? "§aAktiv" : "§cInaktiv"),
                            "§7Monster: " + (settings.isMobSpawning() ? "§aAktiv" : "§cInaktiv"),
                            "§7Tiere: " + (settings.isAnimalSpawning() ? "§aAktiv" : "§cInaktiv"),
                            "§7Feuer: " + (settings.isFireSpread() ? "§aAktiv" : "§cInaktiv"),
                            "§7Explosionen: " + (settings.isExplosions() ? "§aAktiv" : "§cInaktiv"),
                            "§7Wetter: " + (settings.isWeatherChanges() ? "§aAktiv" : "§cInaktiv"),
                            "§7Tag/Nacht: " + (settings.isDayNightCycle() ? "§aAktiv" : "§cInaktiv"),
                            "§7Zeit: §6" + getTimeString(settings.getTimeOfDay())
                    ));
        }

        private String getTimeString(int time) {
            int hours = (time / 1000 + 6) % 24;
            return String.format("%02d:00", hours);
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (realm.getOwner().equals(player.getUniqueId())) {
                new RealmSettingsGUI(plugin, player, realm).open();
            }
        }
    }

    private class StatusInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(realm.isActive() ? Material.LIME_DYE : Material.GRAY_DYE)
                    .setDisplayName("§eStatus")
                    .setLegacyLore(List.of(
                            "§7Realm Status: " + (realm.isActive() ? "§aAktiv" : "§cInaktiv"),
                            "§7Geladen: " + (realm.isLoaded() ? "§aJa" : "§7Nein"),
                            "§7Im Speicher: " + (realm.isLoaded() ? "§a✓" : "§7✗"),
                            "",
                            realm.isActive() ? "§7Realm ist für berechtigte" : "§7Realm ist komplett",
                            realm.isActive() ? "§7Spieler zugänglich" : "§7gesperrt",
                            realm.isLoaded() ? "§7Realm ist sofort begehbar" : "§7Realm wird bei Bedarf geladen"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class LocationInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            List<String> lore = new ArrayList<>();
            lore.add("§7Grid Position:");
            lore.add("§6X: " + realm.getGridX() + " Z: " + realm.getGridZ());
            lore.add("");
            lore.add("§7Welt-Koordinaten:");

            if (realm.getPasteLocation() != null) {
                lore.add("§6X: " + (int)realm.getPasteLocation().getX() +
                        " Z: " + (int)realm.getPasteLocation().getZ());
                lore.add("§7Y: §6" + (int)realm.getPasteLocation().getY());
            } else {
                lore.add("§cNicht verfügbar");
            }

            lore.add("");
            lore.add("§7Spawn-Position:");

            if (realm.getSpawnLocation() != null) {
                lore.add("§6X: " + (int)realm.getSpawnLocation().getX() +
                        " Y: " + (int)realm.getSpawnLocation().getY() +
                        " Z: " + (int)realm.getSpawnLocation().getZ());
                lore.add("§7Blickrichtung: §6" + Math.round(realm.getSpawnLocation().getYaw()) + "°");
            } else {
                lore.add("§cNicht verfügbar");
            }

            return new ItemBuilder(Material.COMPASS)
                    .setDisplayName("§bStandort")
                    .setLegacyLore(lore);
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private static class SizeInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.FILLED_MAP)
                    .setDisplayName("§2Größe")
                    .setLegacyLore(List.of(
                            "§7Realm-Größe: §6256x256 Blöcke",
                            "§7Baubare Fläche: §665.536 Blöcke",
                            "§7Höhe: §6Vollständig (Y: -64 bis 320)",
                            "§7Abstand zu anderen: §6512 Blöcke",
                            "",
                            "§7Das entspricht etwa",
                            "§716 Chunks pro Seite",
                            "§7oder 256 Chunk-Bereichen"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class CreationInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            Date createdDate = new Date(realm.getCreatedTime());
            Date lastAccessDate = new Date(realm.getLastAccessed());

            long daysSinceCreation = (System.currentTimeMillis() - realm.getCreatedTime()) / (1000 * 60 * 60 * 24);
            long daysSinceAccess = (System.currentTimeMillis() - realm.getLastAccessed()) / (1000 * 60 * 60 * 24);

            return new ItemBuilder(Material.CLOCK)
                    .setDisplayName("§eZeitdaten")
                    .setLegacyLore(List.of(
                            "§7Erstellt am:",
                            "§6" + dateFormat.format(createdDate) + " um " + timeFormat.format(createdDate),
                            "§7Vor §6" + daysSinceCreation + " Tagen",
                            "",
                            "§7Zuletzt besucht:",
                            "§6" + dateFormat.format(lastAccessDate) + " um " + timeFormat.format(lastAccessDate),
                            "§7Vor §6" + daysSinceAccess + " Tagen",
                            "",
                            "§7Realm-Alter: §6" + daysSinceCreation + " Tage"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class AccessInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            boolean hasAccess = realm.hasAccess(player.getUniqueId());
            boolean isOwner = realm.getOwner().equals(player.getUniqueId());
            boolean isMember = realm.isMember(player.getUniqueId());

            String role;
            if (isOwner) role = "§aBesitzer";
            else if (isMember) role = "§6Mitglied";
            else if (realm.getSettings().isPublic()) role = "§7Besucher";
            else role = "§cKein Zugriff";

            return new ItemBuilder(hasAccess ? Material.LIME_CONCRETE : Material.RED_CONCRETE)
                    .setDisplayName("§dZugriffsberechtigung")
                    .setLegacyLore(List.of(
                            "§7Deine Rolle: " + role,
                            "§7Zugriff: " + (hasAccess ? "§aErlaubt" : "§cVerweigert"),
                            "",
                            "§7Berechtigungen:",
                            isOwner ? "§a✓ Vollzugriff" : hasAccess ? "§a✓ Bauen erlaubt" : "§c✗ Kein Bauzugriff",
                            isOwner ? "§a✓ Einstellungen ändern" : "§c✗ Keine Verwaltung",
                            isOwner ? "§a✓ Mitglieder verwalten" : "§c✗ Keine Mitgliederverwaltung",
                            hasAccess ? "§a✓ Realm betreten" : "§c✗ Kein Zugang"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private static class WorldInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.GRASS_BLOCK)
                    .setDisplayName("§aWelt-Information")
                    .setLegacyLore(List.of(
                            "§7Welt: §6realms",
                            "§7Dimension: §6Void/Custom",
                            "§7Generator: §6Leer (Void)",
                            "§7Schwierigkeit: §6Normal",
                            "",
                            "§7Alle Realms befinden sich",
                            "§7in der speziellen 'realms'",
                            "§7Welt, die automatisch",
                            "§7vom Plugin verwaltet wird"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class StatsInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            int totalRealms = plugin.getRealmStorage().getAllRealms().size();
            int playerRealms = plugin.getRealmManager().getPlayerOwnedRealms(realm.getOwner()).size();
            boolean isActive = realm.isActive();
            boolean isLoaded = realm.isLoaded();

            return new ItemBuilder(Material.PAPER)
                    .setDisplayName("§9Statistiken")
                    .setLegacyLore(List.of(
                            "§7Gesamt Realms im System: §6" + totalRealms,
                            "§7Realms des Besitzers: §6" + playerRealms,
                            "§7Aktuell geladene Realms: §6" + plugin.getRealmManager().getClass().getDeclaredFields().length,
                            "",
                            "§7Dieses Realm:",
                            "§7Status: " + (isActive ? "§aAktiv" : "§cInaktiv"),
                            "§7Speicher: " + (isLoaded ? "§aGeladen" : "§7Entladen"),
                            "§7Sichtbarkeit: " + (realm.getSettings().isPublic() ? "§aÖffentlich" : "§7Privat")
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class PermissionInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            var template = plugin.getSchematicManager().getTemplate(realm.getTemplateName());
            String templatePerm = template != null && template.getPermission() != null ? template.getPermission() : "§7Keine";

            return new ItemBuilder(Material.IRON_INGOT)
                    .setDisplayName("§cPermissions")
                    .setLegacyLore(List.of(
                            "§7Template-Permission:",
                            "§6" + templatePerm,
                            "",
                            "§7Realm-Permissions:",
                            "§7Besitzer: §aAutomatisch",
                            "§7Mitglieder: §6Eingeladen",
                            "§7Besucher: " + (realm.getSettings().isPublic() ? "§aÖffentlich" : "§cPrivat"),
                            "",
                            "§7Verwende §6/realm invite §7um",
                            "§7Spieler einzuladen"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class TeleportItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            boolean canTeleport = realm.hasAccess(player.getUniqueId()) && realm.isActive();

            return new ItemBuilder(canTeleport ? Material.ENDER_PEARL : Material.BARRIER)
                    .setDisplayName(canTeleport ? "§bZum Realm teleportieren" : "§cTeleport nicht möglich")
                    .setLegacyLore(canTeleport ? List.of(
                            "§7Teleportiere direkt",
                            "§7zu diesem Realm",
                            "",
                            "§7Spawn-Position:",
                            realm.getSpawnLocation() != null ? "§6" + (int)realm.getSpawnLocation().getX() +
                                    " " + (int)realm.getSpawnLocation().getY() +
                                    " " + (int)realm.getSpawnLocation().getZ() : "§cNicht verfügbar",
                            "",
                            "§eKlicken zum Teleportieren"
                    ) : List.of(
                            "§7Du kannst nicht zu",
                            "§7diesem Realm teleportieren",
                            "",
                            "§cGründe:",
                            !realm.hasAccess(player.getUniqueId()) ? "§c- Keine Berechtigung" : "§a- Berechtigung OK",
                            !realm.isActive() ? "§c- Realm inaktiv" : "§a- Realm aktiv"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (!realm.hasAccess(player.getUniqueId())) {
                player.sendMessage(ColorUtil.component("§cDu hast keine Berechtigung für dieses Realm!"));
                return;
            }

            if (!realm.isActive()) {
                player.sendMessage(ColorUtil.component("§cDieses Realm ist inaktiv!"));
                return;
            }

            player.closeInventory();
            player.sendMessage(ColorUtil.component("§7Teleportiere zu §6" + realm.getName() + "§7..."));
            plugin.getRealmManager().teleportToRealm(player, realm.getRealmId());
        }
    }

    private class BackItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.ARROW)
                    .setDisplayName("§eZurück")
                    .setLegacyLore(List.of(
                            "§7Zurück zum",
                            "§7Hauptmenü",
                            "",
                            "§eKlicken zum Zurückkehren"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new RealmMainGUI(plugin, player).open();
        }
    }
}