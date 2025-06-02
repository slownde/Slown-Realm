package de.syscall.gui;

import de.syscall.SlownRealm;
import de.syscall.data.RealmTemplate;
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

public class RealmNameInputGUI {

    private final SlownRealm plugin;
    private final Player player;
    private final RealmTemplate template;

    public RealmNameInputGUI(SlownRealm plugin, Player player, RealmTemplate template) {
        this.plugin = plugin;
        this.player = player;
        this.template = template;
    }

    public void open() {
        Gui gui = Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# # # c # # # # #",
                        "# # # # # # # # #"
                )
                .addIngredient('#', new BackgroundItem())
                .addIngredient('c', new CreateItem())
                .build();

        Window.single()
                .setViewer(player)
                .setTitle(ColorUtil.colorize("&aRealm Name eingeben"))
                .setGui(gui)
                .build()
                .open();

        sendInputInstructions();
    }

    private void sendInputInstructions() {
        player.sendMessage(ColorUtil.component("§eGib den Namen für dein Realm in den Chat ein:"));
        player.sendMessage(ColorUtil.component("§7Schreibe §6'cancel' §7zum Abbrechen"));
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

    private class CreateItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.NAME_TAG)
                    .setDisplayName("§6Realm Name eingeben")
                    .setLegacyLore(buildCreateItemLore());
        }

        private List<String> buildCreateItemLore() {
            String costText = template.getCost() > 0 ?
                    String.format("%.2f", template.getCost()) + " Coins" :
                    "Kostenlos";

            return List.of(
                    "§7Template: §6" + template.getDisplayName(),
                    "§7Kosten: §6" + costText,
                    "",
                    "§7Schließe das Inventar und",
                    "§7gib den Namen in den Chat ein",
                    "",
                    "§eInventar schließen"
            );
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            player.closeInventory();
        }
    }
}