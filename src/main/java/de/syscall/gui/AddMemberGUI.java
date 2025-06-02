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

public class AddMemberGUI {

    private final SlownRealm plugin;
    private final Player player;
    private final Realm realm;

    public AddMemberGUI(SlownRealm plugin, Player player, Realm realm) {
        this.plugin = plugin;
        this.player = player;
        this.realm = realm;
    }

    public void open() {
        List<Item> playerItems = getAvailablePlayers();

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
                .addIngredient('h', new BackToMembersItem())
                .setContent(playerItems)
                .build();

        Window.single()
                .setViewer(player)
                .setTitle(ColorUtil.colorize("&aSpieler hinzufügen"))
                .setGui(gui)
                .build()
                .open();
    }

    private List<Item> getAvailablePlayers() {
        List<Item> playerItems = new ArrayList<>();

        plugin.getServer().getOnlinePlayers().stream()
                .filter(this::canAddPlayer)
                .forEach(onlinePlayer -> playerItems.add(new OnlinePlayerItem(onlinePlayer)));

        if (playerItems.isEmpty()) {
            playerItems.add(new NoPlayersItem());
        }

        return playerItems;
    }

    private boolean canAddPlayer(Player onlinePlayer) {
        return !onlinePlayer.equals(player) &&
                !realm.isMember(onlinePlayer.getUniqueId()) &&
                !realm.getOwner().equals(onlinePlayer.getUniqueId());
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

    private class BackToMembersItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.ARROW)
                    .setDisplayName("§eZurück zu Mitgliedern")
                    .setLegacyLore(List.of(
                            "§7Zurück zur",
                            "§7Mitglieder-Verwaltung",
                            "",
                            "§eKlicken zum Zurückkehren"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new RealmMembersGUI(plugin, player, realm).open();
        }
    }

    private class OnlinePlayerItem extends AbstractItem {
        private final Player targetPlayer;

        public OnlinePlayerItem(Player targetPlayer) {
            this.targetPlayer = targetPlayer;
        }

        @Override
        public ItemProvider getItemProvider() {
            String group = plugin.getVecturAPI().getGroupDisplayName(targetPlayer);

            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setDisplayName("§6" + targetPlayer.getName())
                    .setLegacyLore(List.of(
                            "§7Rang: " + group,
                            "§7Status: §aOnline",
                            "",
                            "§7Füge diesen Spieler",
                            "§7zu deinem Realm hinzu",
                            "",
                            "§7Mitglieder können:",
                            "§a- Dein Realm betreten",
                            "§a- Im Realm bauen",
                            "§a- Items verwenden",
                            "",
                            "§eKlicken zum Hinzufügen"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (realm.isMember(targetPlayer.getUniqueId())) {
                player.sendMessage(ColorUtil.component("§cDieser Spieler ist bereits Mitglied!"));
                return;
            }

            addPlayerToRealm();
        }

        private void addPlayerToRealm() {
            realm.addMember(targetPlayer.getUniqueId());
            plugin.getRealmManager().updateRealm(realm);

            player.sendMessage(ColorUtil.component("§a" + targetPlayer.getName() + " §7wurde zu deinem Realm hinzugefügt!"));
            targetPlayer.sendMessage(ColorUtil.component("§7Du wurdest zu §6" + player.getName() + "§7s Realm §6" + realm.getName() + " §7hinzugefügt!"));

            new RealmMembersGUI(plugin, player, realm).open();
        }
    }

    private static class NoPlayersItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.BARRIER)
                    .setDisplayName("§cKeine Spieler verfügbar")
                    .setLegacyLore(List.of(
                            "§7Es sind keine Spieler",
                            "§7online, die du hinzufügen",
                            "§7könntest",
                            "",
                            "§7Spieler müssen online",
                            "§7sein, um hinzugefügt",
                            "§7zu werden"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }
}