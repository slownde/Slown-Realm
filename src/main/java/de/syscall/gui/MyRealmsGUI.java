package de.syscall.gui;

import de.syscall.SlownRealm;
import de.syscall.data.Realm;
import de.syscall.util.ColorUtil;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;
import xyz.xenondevs.invui.window.Window;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class MyRealmsGUI {

    private final SlownRealm plugin;
    private final Player player;

    public MyRealmsGUI(SlownRealm plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        plugin.getLogger().info("Opening MyRealmsGUI for player: " + player.getName());
        List<Item> realmItems = getPlayerRealms();
        plugin.getLogger().info("Realm items count: " + realmItems.size());

        Gui gui = PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# < # # h # # > #"
                )
                .addIngredient('#', new BackgroundItem())
                .addIngredient('<', new PreviousPageItem())
                .addIngredient('>', new NextPageItem())
                .addIngredient('h', new BackToMainItem())
                .setContent(realmItems)
                .build();

        Window.single()
                .setViewer(player)
                .setTitle(ColorUtil.colorize("&6Meine Realms"))
                .setGui(gui)
                .build()
                .open();

        plugin.getLogger().info("MyRealmsGUI opened successfully");
    }

    private List<Item> getPlayerRealms() {
        plugin.getLogger().info("Getting player realms for: " + player.getName());
        List<Item> realmItems = new ArrayList<>();

        Collection<Realm> playerRealms = plugin.getRealmManager().getPlayerOwnedRealms(player.getUniqueId());
        plugin.getLogger().info("Player owned realms count: " + playerRealms.size());

        for (Realm realm : playerRealms) {
            plugin.getLogger().info("Adding realm to GUI: " + realm.getName());
            realmItems.add(new RealmItem(realm));
        }

        if (realmItems.isEmpty()) {
            plugin.getLogger().info("No realms found, adding NoRealmsItem");
            realmItems.add(new NoRealmsItem());
        }

        return realmItems;
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

    private static class PreviousPageItem extends PageItem {
        public PreviousPageItem() {
            super(false);
        }

        @Override
        public ItemProvider getItemProvider(PagedGui<?> pagedGui) {
            return new ItemBuilder(Material.ARROW)
                    .setDisplayName("§7Vorherige Seite")
                    .setLegacyLore(List.of("§8Klicke zum Zurückblättern"));
        }
    }

    private static class NextPageItem extends PageItem {
        public NextPageItem() {
            super(true);
        }

        @Override
        public ItemProvider getItemProvider(PagedGui<?> pagedGui) {
            return new ItemBuilder(Material.ARROW)
                    .setDisplayName("§7Nächste Seite")
                    .setLegacyLore(List.of("§8Klicke zum Weiterblättern"));
        }
    }

    private class BackToMainItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.ARROW)
                    .setDisplayName("§eZurück zum Hauptmenü")
                    .setLegacyLore(List.of(
                            "§7Zurück zum Realm",
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

    private class RealmItem extends AbstractItem {
        private final Realm realm;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

        public RealmItem(Realm realm) {
            this.realm = realm;
            plugin.getLogger().info("Created RealmItem for: " + realm.getName());
        }

        @Override
        public ItemProvider getItemProvider() {
            plugin.getLogger().info("Getting ItemProvider for realm: " + realm.getName());
            List<String> lore = buildRealmLore();

            return new ItemBuilder(Material.GRASS_BLOCK)
                    .setDisplayName("§6" + realm.getName())
                    .setLegacyLore(lore);
        }

        private List<String> buildRealmLore() {
            List<String> lore = new ArrayList<>();

            addBasicRealmInfo(lore);
            addDescriptionIfPresent(lore);
            addInteractionInstructions(lore);

            return lore;
        }

        private void addBasicRealmInfo(List<String> lore) {
            String createdDate = dateFormat.format(new Date(realm.getCreatedTime()));
            String lastAccessed = dateFormat.format(new Date(realm.getLastAccessed()));

            lore.add("§7Template: §6" + realm.getTemplateName());
            lore.add("§7Status: " + (realm.isActive() ? "§aAktiv" : "§cInaktiv"));
            lore.add("§7Geladen: " + (realm.isLoaded() ? "§aJa" : "§7Nein"));
            lore.add("§7Mitglieder: §6" + realm.getMembers().size());
            lore.add("§7Erstellt: §6" + createdDate);
            lore.add("§7Zuletzt besucht: §6" + lastAccessed);
            lore.add("");
        }

        private void addDescriptionIfPresent(List<String> lore) {
            if (!realm.getDescription().isEmpty()) {
                lore.add("§7Beschreibung:");
                lore.add("§f" + realm.getDescription());
                lore.add("");
            }
        }

        private void addInteractionInstructions(List<String> lore) {
            lore.add("§eLinksklick: §7Teleportieren");
            lore.add("§eRechtsklick: §7Verwalten");
            lore.add("§eShift+Rechtsklick: §7Löschen");
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            plugin.getLogger().info("Realm clicked: " + realm.getName() + " | Click type: " + clickType);

            switch (clickType) {
                case LEFT -> handleTeleport();
                case RIGHT -> handleManage();
                case SHIFT_RIGHT -> handleDelete();
            }
        }

        private void handleTeleport() {
            plugin.getLogger().info("Teleporting to realm: " + realm.getName());
            player.closeInventory();
            player.sendMessage(ColorUtil.component("§7Teleportiere zu Realm §6" + realm.getName() + "§7..."));
            plugin.getRealmManager().teleportToRealm(player, realm.getRealmId());
        }

        private void handleManage() {
            plugin.getLogger().info("Opening management for realm: " + realm.getName());
            new RealmManageGUI(plugin, player, realm).open();
        }

        private void handleDelete() {
            plugin.getLogger().info("Opening delete confirmation for realm: " + realm.getName());
            new RealmDeleteConfirmGUI(plugin, player, realm).open();
        }
    }

    private static class NoRealmsItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.BARRIER)
                    .setDisplayName("§cKeine Realms")
                    .setLegacyLore(List.of(
                            "§7Du hast noch keine",
                            "§7Realms erstellt",
                            "",
                            "§7Verwende das Hauptmenü",
                            "§7um dein erstes Realm",
                            "§7zu erstellen!",
                            "",
                            "§eZurück zum Hauptmenü"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            player.sendMessage(ColorUtil.component("§7Du hast noch keine Realms. Erstelle dein erstes Realm!"));
        }
    }
}