package de.syscall.gui;

import de.syscall.SlownRealm;
import de.syscall.data.Realm;
import de.syscall.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RealmMembersGUI {

    private final SlownRealm plugin;
    private final Player player;
    private final Realm realm;

    public RealmMembersGUI(SlownRealm plugin, Player player, Realm realm) {
        this.plugin = plugin;
        this.player = player;
        this.realm = realm;
    }

    public void open() {
        List<Item> memberItems = getMemberItems();

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
                .addIngredient('h', new BackToManageItem())
                .setContent(memberItems)
                .build();

        Window.single()
                .setViewer(player)
                .setTitle(ColorUtil.colorize("&aMitglieder: " + realm.getName()))
                .setGui(gui)
                .build()
                .open();
    }

    private List<Item> getMemberItems() {
        List<Item> memberItems = new ArrayList<>();

        memberItems.add(new AddMemberItem());

        realm.getMembers().forEach(memberUuid ->
                memberItems.add(new MemberItem(memberUuid)));

        if (memberItems.size() == 1) {
            memberItems.add(new NoMembersItem());
        }

        return memberItems;
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

    private class BackToManageItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.ARROW)
                    .setDisplayName("§eZurück zur Verwaltung")
                    .setLegacyLore(List.of(
                            "§7Zurück zur Realm",
                            "§7Verwaltung",
                            "",
                            "§eKlicken zum Zurückkehren"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new RealmManageGUI(plugin, player, realm).open();
        }
    }

    private class AddMemberItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.LIME_DYE)
                    .setDisplayName("§aMitglied hinzufügen")
                    .setLegacyLore(List.of(
                            "§7Füge einen neuen",
                            "§7Spieler zu deinem",
                            "§7Realm hinzu",
                            "",
                            "§7Der Spieler muss",
                            "§7online sein",
                            "",
                            "§eKlicken zum Hinzufügen"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new AddMemberGUI(plugin, player, realm).open();
        }
    }

    private class MemberItem extends AbstractItem {
        private final UUID memberUuid;

        public MemberItem(UUID memberUuid) {
            this.memberUuid = memberUuid;
        }

        @Override
        public ItemProvider getItemProvider() {
            String memberName = plugin.getServer().getOfflinePlayer(memberUuid).getName();
            boolean isOnline = plugin.getServer().getPlayer(memberUuid) != null;

            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setDisplayName("§6" + memberName)
                    .setLegacyLore(buildMemberLore(isOnline));
        }

        private List<String> buildMemberLore(boolean isOnline) {
            return List.of(
                    "§7Status: " + (isOnline ? "§aOnline" : "§7Offline"),
                    "§7Mitglied seit Realm-Erstellung",
                    "",
                    "§7Dieser Spieler kann",
                    "§7dein Realm betreten und",
                    "§7darin bauen",
                    "",
                    "§eLinksklick: §7Teleportieren (falls online)",
                    "§eRechtsklick: §7Entfernen"
            );
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            switch (clickType) {
                case LEFT -> handleTeleport();
                case RIGHT -> handleRemove();
            }
        }

        private void handleTeleport() {
            Player targetPlayer = plugin.getServer().getPlayer(memberUuid);
            if (targetPlayer == null) {
                player.sendMessage(ColorUtil.component("§cDieser Spieler ist nicht online!"));
                return;
            }

            executeTeleport(targetPlayer);
        }

        private void executeTeleport(Player targetPlayer) {
            player.closeInventory();
            player.teleport(targetPlayer.getLocation());
            player.sendMessage(ColorUtil.component("§7Du wurdest zu §6" + targetPlayer.getName() + " §7teleportiert!"));
        }

        private void handleRemove() {
            String memberName = plugin.getServer().getOfflinePlayer(memberUuid).getName();

            removeMemberFromRealm();
            kickMemberIfInRealm(memberName);

            player.sendMessage(ColorUtil.component("§6" + memberName + " §7wurde aus dem Realm entfernt!"));
            new RealmMembersGUI(plugin, player, realm).open();
        }

        private void removeMemberFromRealm() {
            realm.removeMember(memberUuid);
            plugin.getRealmManager().updateRealm(realm);
        }

        private void kickMemberIfInRealm(String memberName) {
            Player targetPlayer = plugin.getServer().getPlayer(memberUuid);
            if (targetPlayer != null && plugin.getRealmManager().isPlayerInRealm(targetPlayer, realm.getRealmId())) {
                targetPlayer.teleport(plugin.getServer().getWorlds().getFirst().getSpawnLocation());
                targetPlayer.sendMessage(ColorUtil.component("§7Du wurdest aus §6" + realm.getName() + " §7entfernt!"));
            }
        }
    }

    private static class NoMembersItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.BARRIER)
                    .setDisplayName("§cKeine Mitglieder")
                    .setLegacyLore(List.of(
                            "§7Du hast noch keine",
                            "§7Mitglieder in deinem Realm",
                            "",
                            "§7Füge Spieler hinzu,",
                            "§7damit sie dein Realm",
                            "§7besuchen können!"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }
}