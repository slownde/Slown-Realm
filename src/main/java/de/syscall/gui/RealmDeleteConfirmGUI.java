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

public class RealmDeleteConfirmGUI {

    private final SlownRealm plugin;
    private final Player player;
    private final Realm realm;

    public RealmDeleteConfirmGUI(SlownRealm plugin, Player player, Realm realm) {
        this.plugin = plugin;
        this.player = player;
        this.realm = realm;
    }

    public void open() {
        Gui gui = Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# # # c # d # # #",
                        "# # # # # # # # #"
                )
                .addIngredient('#', new BackgroundItem())
                .addIngredient('c', new CancelItem())
                .addIngredient('d', new ConfirmDeleteItem())
                .build();

        Window.single()
                .setViewer(player)
                .setTitle(ColorUtil.colorize("&cRealm löschen?"))
                .setGui(gui)
                .build()
                .open();
    }

    private static class BackgroundItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                    .setDisplayName("§r");
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class CancelItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.LIME_DYE)
                    .setDisplayName("§aAbbrechen")
                    .setLegacyLore(List.of(
                            "§7Löschung abbrechen",
                            "§7und zurückkehren",
                            "",
                            "§eKlicken zum Abbrechen"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new RealmManageGUI(plugin, player, realm).open();
        }
    }

    private class ConfirmDeleteItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.BARRIER)
                    .setDisplayName("§c§lRealm löschen")
                    .setLegacyLore(buildConfirmationLore());
        }

        private List<String> buildConfirmationLore() {
            return List.of(
                    "§7Realm: §6" + realm.getName(),
                    "",
                    "§c§lACHTUNG:",
                    "§cDies löscht das Realm",
                    "§cpermanent und kann NICHT",
                    "§crückgängig gemacht werden!",
                    "",
                    "§cAlle Bauwerke gehen verloren!",
                    "",
                    "§eKlicken zum Bestätigen"
            );
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            handleRealmDeletion();
        }

        private void handleRealmDeletion() {
            player.closeInventory();
            player.sendMessage(ColorUtil.component("§7Lösche Realm §6" + realm.getName() + "§7..."));

            plugin.getRealmManager().deleteRealm(realm.getRealmId())
                    .thenAccept(this::handleDeletionResult);
        }

        private void handleDeletionResult(boolean success) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(ColorUtil.component("§aRealm §6" + realm.getName() + " §aerfolgreich gelöscht!"));
                } else {
                    player.sendMessage(ColorUtil.component("§cFehler beim Löschen des Realms!"));
                }
            });
        }
    }
}