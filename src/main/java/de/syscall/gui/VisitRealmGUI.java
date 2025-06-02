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
import java.util.Date;
import java.util.List;

public class VisitRealmGUI {

    private final SlownRealm plugin;
    private final Player player;

    public VisitRealmGUI(SlownRealm plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        List<Item> realmItems = getPublicRealms();

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
                .setTitle(ColorUtil.colorize("&bÖffentliche Realms"))
                .setGui(gui)
                .build()
                .open();
    }

    private List<Item> getPublicRealms() {
        List<Item> realmItems = new ArrayList<>();

        plugin.getRealmStorage().getAllRealms().stream()
                .filter(this::isVisitableRealm)
                .forEach(realm -> realmItems.add(new PublicRealmItem(realm)));

        if (realmItems.isEmpty()) {
            realmItems.add(new NoRealmsItem());
        }

        return realmItems;
    }

    private boolean isVisitableRealm(Realm realm) {
        return realm.getSettings().isPublic() &&
                realm.isActive() &&
                !realm.getOwner().equals(player.getUniqueId());
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

    private class PublicRealmItem extends AbstractItem {
        private final Realm realm;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

        public PublicRealmItem(Realm realm) {
            this.realm = realm;
        }

        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.ENDER_PEARL)
                    .setDisplayName("§b" + realm.getName())
                    .setLegacyLore(buildRealmLore());
        }

        private List<String> buildRealmLore() {
            String ownerName = plugin.getServer().getOfflinePlayer(realm.getOwner()).getName();
            String createdDate = dateFormat.format(new Date(realm.getCreatedTime()));

            List<String> lore = new ArrayList<>();
            addBasicRealmInfo(lore, ownerName, createdDate);
            addDescriptionIfPresent(lore);
            lore.add("§eKlicken zum Besuchen");

            return lore;
        }

        private void addBasicRealmInfo(List<String> lore, String ownerName, String createdDate) {
            lore.add("§7Besitzer: §6" + ownerName);
            lore.add("§7Template: §6" + realm.getTemplateName());
            lore.add("§7Erstellt: §6" + createdDate);
            lore.add("§7Mitglieder: §6" + realm.getMembers().size());
            lore.add("");
        }

        private void addDescriptionIfPresent(List<String> lore) {
            if (!realm.getDescription().isEmpty()) {
                lore.add("§7Beschreibung:");
                lore.add("§f" + realm.getDescription());
                lore.add("");
            }
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            handleRealmVisit();
        }

        private void handleRealmVisit() {
            player.closeInventory();
            player.sendMessage(ColorUtil.component("§7Teleportiere zu §b" + realm.getName() + "§7..."));
            plugin.getRealmManager().teleportToRealm(player, realm.getRealmId());
        }
    }

    private static class NoRealmsItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.BARRIER)
                    .setDisplayName("§cKeine öffentlichen Realms")
                    .setLegacyLore(List.of(
                            "§7Es gibt aktuell keine",
                            "§7öffentlichen Realms",
                            "§7zum Besuchen",
                            "",
                            "§7Komme später wieder!"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }
}