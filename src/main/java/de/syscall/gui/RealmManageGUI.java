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

import java.util.List;

public class RealmManageGUI {

    private final SlownRealm plugin;
    private final Player player;
    private final Realm realm;

    public RealmManageGUI(SlownRealm plugin, Player player, Realm realm) {
        this.plugin = plugin;
        this.player = player;
        this.realm = realm;
    }

    public void open() {
        Gui gui = Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# a b c d e f g #",
                        "# # # # h # # # #"
                )
                .addIngredient('#', new BackgroundItem())
                .addIngredient('a', new TeleportItem())
                .addIngredient('b', new RenameItem())
                .addIngredient('c', new DescriptionItem())
                .addIngredient('d', new MembersItem())
                .addIngredient('e', new SettingsItem())
                .addIngredient('f', new ToggleActiveItem())
                .addIngredient('g', new DeleteItem())
                .addIngredient('h', new BackItem())
                .build();

        Window.single()
                .setViewer(player)
                .setTitle(ColorUtil.colorize("&6Realm: " + realm.getName()))
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

    private class TeleportItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.ENDER_PEARL)
                    .setDisplayName("§bTeleportieren")
                    .setLegacyLore(List.of(
                            "§7Teleportiere zu",
                            "§7diesem Realm",
                            "",
                            "§eKlicken zum Teleportieren"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            handleTeleport();
        }

        private void handleTeleport() {
            player.closeInventory();
            player.sendMessage(ColorUtil.component("§7Teleportiere zu §6" + realm.getName() + "§7..."));
            plugin.getRealmManager().teleportToRealm(player, realm.getRealmId());
        }
    }

    private class RenameItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.NAME_TAG)
                    .setDisplayName("§eUmbenennen")
                    .setLegacyLore(buildRenameLore());
        }

        private List<String> buildRenameLore() {
            return List.of(
                    "§7Aktueller Name:",
                    "§6" + realm.getName(),
                    "",
                    "§7Ändere den Namen",
                    "§7deines Realms",
                    "",
                    "§eKlicken zum Umbenennen"
            );
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            handleRename();
        }

        private void handleRename() {
            player.closeInventory();
            player.sendMessage(ColorUtil.component("§eGib den neuen Namen für dein Realm in den Chat ein:"));
            player.sendMessage(ColorUtil.component("§7Schreibe §6'cancel' §7zum Abbrechen"));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                new RealmRenameHandler(plugin, player, realm).startListening();
            }, 1L);
        }
    }

    private class DescriptionItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.WRITABLE_BOOK)
                    .setDisplayName("§7Beschreibung")
                    .setLegacyLore(buildDescriptionLore());
        }

        private List<String> buildDescriptionLore() {
            String currentDescription = realm.getDescription().isEmpty() ?
                    "§cKeine Beschreibung" : "§f" + realm.getDescription();

            return List.of(
                    "§7Aktuelle Beschreibung:",
                    currentDescription,
                    "",
                    "§7Ändere die Beschreibung",
                    "§7deines Realms",
                    "",
                    "§eKlicken zum Bearbeiten"
            );
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            handleDescriptionEdit();
        }

        private void handleDescriptionEdit() {
            player.closeInventory();
            player.sendMessage(ColorUtil.component("§eGib die neue Beschreibung für dein Realm in den Chat ein:"));
            player.sendMessage(ColorUtil.component("§7Schreibe §6'cancel' §7zum Abbrechen"));
            player.sendMessage(ColorUtil.component("§7Schreibe §6'clear' §7zum Löschen"));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                new RealmDescriptionHandler(plugin, player, realm).startListening();
            }, 1L);
        }
    }

    private class MembersItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setDisplayName("§aMitglieder")
                    .setLegacyLore(buildMembersLore());
        }

        private List<String> buildMembersLore() {
            return List.of(
                    "§7Anzahl Mitglieder:",
                    "§6" + realm.getMembers().size(),
                    "",
                    "§7Verwalte die Mitglieder",
                    "§7deines Realms",
                    "",
                    "§eKlicken zum Verwalten"
            );
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new RealmMembersGUI(plugin, player, realm).open();
        }
    }

    private class SettingsItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.REDSTONE)
                    .setDisplayName("§cEinstellungen")
                    .setLegacyLore(List.of(
                            "§7Ändere die Einstellungen",
                            "§7deines Realms",
                            "",
                            "§7PvP, Spawning, etc.",
                            "",
                            "§eKlicken zum Öffnen"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new RealmSettingsGUI(plugin, player, realm).open();
        }
    }

    private class ToggleActiveItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(realm.isActive() ? Material.LIME_DYE : Material.GRAY_DYE)
                    .setDisplayName(realm.isActive() ? "§aAktiv" : "§cInaktiv")
                    .setLegacyLore(buildToggleLore());
        }

        private List<String> buildToggleLore() {
            return List.of(
                    "§7Status: " + (realm.isActive() ? "§aAktiviert" : "§cDeaktiviert"),
                    "",
                    realm.isActive() ? "§7Realm ist für alle" : "§7Realm ist für niemanden",
                    realm.isActive() ? "§7zugänglich" : "§7zugänglich",
                    "",
                    "§eKlicken zum Umschalten"
            );
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            handleToggle();
        }

        private void handleToggle() {
            realm.setActive(!realm.isActive());
            plugin.getRealmManager().updateRealm(realm);

            String status = realm.isActive() ? "§aaktiviert" : "§cdeaktiviert";
            player.sendMessage(ColorUtil.component("§7Realm wurde " + status + "§7!"));
            notify();
        }
    }

    private class DeleteItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.BARRIER)
                    .setDisplayName("§cLöschen")
                    .setLegacyLore(List.of(
                            "§7Lösche dieses Realm",
                            "§7permanent",
                            "",
                            "§c§lACHTUNG:",
                            "§cDies kann nicht",
                            "§crückgängig gemacht werden!",
                            "",
                            "§eKlicken zum Löschen"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new RealmDeleteConfirmGUI(plugin, player, realm).open();
        }
    }

    private class BackItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.ARROW)
                    .setDisplayName("§eZurück")
                    .setLegacyLore(List.of(
                            "§7Zurück zur",
                            "§7Realm-Liste",
                            "",
                            "§eKlicken zum Zurückkehren"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new MyRealmsGUI(plugin, player).open();
        }
    }
}