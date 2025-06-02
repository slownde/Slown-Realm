package de.syscall.gui;

import de.syscall.SlownRealm;
import de.syscall.data.RealmTemplate;
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

import java.util.ArrayList;
import java.util.List;

public class CreateRealmGUI {

    private final SlownRealm plugin;
    private final Player player;

    public CreateRealmGUI(SlownRealm plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        List<Item> templateItems = getAvailableTemplates();

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
                .setContent(templateItems)
                .build();

        Window.single()
                .setViewer(player)
                .setTitle(ColorUtil.colorize("&aRealm Template wählen"))
                .setGui(gui)
                .build()
                .open();
    }

    private List<Item> getAvailableTemplates() {
        List<Item> templateItems = new ArrayList<>();

        plugin.getSchematicManager().getAllTemplates().values().stream()
                .filter(this::canUseTemplate)
                .forEach(template -> templateItems.add(new TemplateItem(template)));

        return templateItems;
    }

    private boolean canUseTemplate(RealmTemplate template) {
        return template.isEnabled() &&
                (template.getPermission() == null || player.hasPermission(template.getPermission()));
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

    private class TemplateItem extends AbstractItem {
        private final RealmTemplate template;

        public TemplateItem(RealmTemplate template) {
            this.template = template;
        }

        @Override
        public ItemProvider getItemProvider() {
            List<String> lore = buildTemplateLore();

            return new ItemBuilder(template.getIcon())
                    .setDisplayName("§6" + template.getDisplayName())
                    .setLegacyLore(lore);
        }

        private List<String> buildTemplateLore() {
            List<String> lore = new ArrayList<>();
            lore.add("§7" + template.getDescription());
            lore.add("");

            addCostInformation(lore);
            addRealmLimitInformation(lore);

            return lore;
        }

        private void addCostInformation(List<String> lore) {
            if (template.getCost() > 0) {
                double playerCoins = plugin.getVecturAPI().getCoins(player);
                boolean canAfford = playerCoins >= template.getCost();
                lore.add("§7Kosten: " + (canAfford ? "§a" : "§c") + String.format("%.2f", template.getCost()) + " Coins");
                lore.add("§7Deine Coins: §6" + String.format("%.2f", playerCoins));
                lore.add("");
            } else {
                lore.add("§7Kosten: §aKostenlos");
                lore.add("");
            }
        }

        private void addRealmLimitInformation(List<String> lore) {
            int ownedRealms = plugin.getRealmManager().getPlayerOwnedRealms(player.getUniqueId()).size();
            int maxRealms = plugin.getConfig().getInt("realm.max-realms-per-player", 3);
            boolean hasSpace = ownedRealms < maxRealms;

            if (!hasSpace) {
                lore.add("§cMaximale Anzahl Realms erreicht!");
                lore.add("§c(" + ownedRealms + "/" + maxRealms + ")");
            } else {
                lore.add("§eKlicken zum Erstellen");
            }
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (!canCreateRealm()) {
                return;
            }

            if (!canAffordTemplate()) {
                return;
            }

            new RealmNameInputGUI(plugin, player, template).open();
        }

        private boolean canCreateRealm() {
            int ownedRealms = plugin.getRealmManager().getPlayerOwnedRealms(player.getUniqueId()).size();
            int maxRealms = plugin.getConfig().getInt("realm.max-realms-per-player", 3);

            if (ownedRealms >= maxRealms) {
                player.sendMessage(ColorUtil.component("§cDu hast bereits die maximale Anzahl an Realms! (" + maxRealms + ")"));
                return false;
            }
            return true;
        }

        private boolean canAffordTemplate() {
            if (template.getCost() > 0) {
                double playerCoins = plugin.getVecturAPI().getCoins(player);
                if (playerCoins < template.getCost()) {
                    player.sendMessage(ColorUtil.component("§cDu hast nicht genug Coins! Benötigt: §6" +
                            String.format("%.2f", template.getCost()) + " Coins"));
                    return false;
                }
            }
            return true;
        }
    }
}