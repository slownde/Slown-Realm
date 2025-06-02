package de.syscall.gui;

import de.syscall.SlownRealm;
import de.syscall.data.Realm;
import de.syscall.data.RealmSettings;
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

public class RealmSettingsGUI {

    private final SlownRealm plugin;
    private final Player player;
    private final Realm realm;

    public RealmSettingsGUI(SlownRealm plugin, Player player, Realm realm) {
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
                .addIngredient('a', new PublicToggleItem())
                .addIngredient('b', new PvpToggleItem())
                .addIngredient('c', new MobSpawningItem())
                .addIngredient('d', new AnimalSpawningItem())
                .addIngredient('e', new FireSpreadItem())
                .addIngredient('f', new ExplosionsItem())
                .addIngredient('g', new LeafDecayItem())
                .addIngredient('h', new WeatherItem())
                .addIngredient('i', new DayNightItem())
                .addIngredient('j', new TimeSetItem())
                .addIngredient('k', new BiomeItem())
                .addIngredient('l', new SaveItem())
                .addIngredient('m', new ResetItem())
                .addIngredient('n', new InfoItem())
                .addIngredient('o', new BackItem())
                .build();

        Window.single()
                .setViewer(player)
                .setTitle(ColorUtil.colorize("&cRealm Einstellungen"))
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

    private class PublicToggleItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            RealmSettings settings = realm.getSettings();
            return new ItemBuilder(settings.isPublic() ? Material.LIME_DYE : Material.GRAY_DYE)
                    .setDisplayName("§6Öffentlich")
                    .setLegacyLore(buildToggleLore(settings.isPublic(), "Andere Spieler können", "dein Realm besuchen"));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            RealmSettings settings = realm.getSettings();
            settings.setPublic(!settings.isPublic());
            notify();
        }
    }

    private class PvpToggleItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            RealmSettings settings = realm.getSettings();
            return new ItemBuilder(settings.isPvpEnabled() ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD)
                    .setDisplayName("§cPvP")
                    .setLegacyLore(buildToggleLore(settings.isPvpEnabled(), "Spieler können sich", "gegenseitig angreifen"));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            RealmSettings settings = realm.getSettings();
            settings.setPvpEnabled(!settings.isPvpEnabled());
            notify();
        }
    }

    private class MobSpawningItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            RealmSettings settings = realm.getSettings();
            return new ItemBuilder(settings.isMobSpawning() ? Material.ZOMBIE_HEAD : Material.BARRIER)
                    .setDisplayName("§4Monster Spawning")
                    .setLegacyLore(buildToggleLore(settings.isMobSpawning(), "Monster können in", "deinem Realm spawnen"));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            RealmSettings settings = realm.getSettings();
            settings.setMobSpawning(!settings.isMobSpawning());
            notify();
        }
    }

    private class AnimalSpawningItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            RealmSettings settings = realm.getSettings();
            return new ItemBuilder(settings.isAnimalSpawning() ? Material.PIG_SPAWN_EGG : Material.BARRIER)
                    .setDisplayName("§2Tier Spawning")
                    .setLegacyLore(buildToggleLore(settings.isAnimalSpawning(), "Tiere können in", "deinem Realm spawnen"));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            RealmSettings settings = realm.getSettings();
            settings.setAnimalSpawning(!settings.isAnimalSpawning());
            notify();
        }
    }

    private class FireSpreadItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            RealmSettings settings = realm.getSettings();
            return new ItemBuilder(settings.isFireSpread() ? Material.FIRE_CHARGE : Material.WATER_BUCKET)
                    .setDisplayName("§6Feuer Ausbreitung")
                    .setLegacyLore(buildToggleLore(settings.isFireSpread(), "Feuer kann sich", "ausbreiten"));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            RealmSettings settings = realm.getSettings();
            settings.setFireSpread(!settings.isFireSpread());
            notify();
        }
    }

    private class ExplosionsItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            RealmSettings settings = realm.getSettings();
            return new ItemBuilder(settings.isExplosions() ? Material.TNT : Material.OBSIDIAN)
                    .setDisplayName("§4Explosionen")
                    .setLegacyLore(buildToggleLore(settings.isExplosions(), "Explosionen können", "Blöcke zerstören"));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            RealmSettings settings = realm.getSettings();
            settings.setExplosions(!settings.isExplosions());
            notify();
        }
    }

    private class LeafDecayItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            RealmSettings settings = realm.getSettings();
            return new ItemBuilder(settings.isLeafDecay() ? Material.OAK_LEAVES : Material.OAK_LOG)
                    .setDisplayName("§2Blätter Zerfall")
                    .setLegacyLore(buildToggleLore(settings.isLeafDecay(), "Blätter verschwinden", "ohne Holz"));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            RealmSettings settings = realm.getSettings();
            settings.setLeafDecay(!settings.isLeafDecay());
            notify();
        }
    }

    private class WeatherItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            RealmSettings settings = realm.getSettings();
            return new ItemBuilder(settings.isWeatherChanges() ? Material.WATER_BUCKET : Material.BUCKET)
                    .setDisplayName("§9Wetter Änderungen")
                    .setLegacyLore(buildToggleLore(settings.isWeatherChanges(), "Wetter kann sich", "automatisch ändern"));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            RealmSettings settings = realm.getSettings();
            settings.setWeatherChanges(!settings.isWeatherChanges());
            notify();
        }
    }

    private class DayNightItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            RealmSettings settings = realm.getSettings();
            return new ItemBuilder(settings.isDayNightCycle() ? Material.CLOCK : Material.REDSTONE_BLOCK)
                    .setDisplayName("§eTag/Nacht Zyklus")
                    .setLegacyLore(buildToggleLore(settings.isDayNightCycle(), "Zeit vergeht", "automatisch"));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            RealmSettings settings = realm.getSettings();
            settings.setDayNightCycle(!settings.isDayNightCycle());
            notify();
        }
    }

    private class TimeSetItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            RealmSettings settings = realm.getSettings();
            return new ItemBuilder(Material.COMPASS)
                    .setDisplayName("§eZeit Setzen")
                    .setLegacyLore(buildTimeSetLore(settings));
        }

        private List<String> buildTimeSetLore(RealmSettings settings) {
            String timeString = getTimeString(settings.getTimeOfDay());
            return List.of(
                    "§7Aktuelle Zeit: §6" + timeString,
                    "§7Setze eine feste Zeit",
                    "§7für dein Realm",
                    "",
                    "§eLinks: §7Morgen (6:00)",
                    "§eRechts: §7Mittag (12:00)",
                    "§eShift+Links: §7Abend (18:00)",
                    "§eShift+Rechts: §7Mitternacht (0:00)"
            );
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            RealmSettings settings = realm.getSettings();

            switch (clickType) {
                case LEFT -> settings.setTimeOfDay(0);
                case RIGHT -> settings.setTimeOfDay(6000);
                case SHIFT_LEFT -> settings.setTimeOfDay(12000);
                case SHIFT_RIGHT -> settings.setTimeOfDay(18000);
            }

            notify();
        }

        private String getTimeString(int time) {
            int hours = (time / 1000 + 6) % 24;
            return String.format("%02d:00", hours);
        }
    }

    private class BiomeItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            RealmSettings settings = realm.getSettings();
            return new ItemBuilder(Material.GRASS_BLOCK)
                    .setDisplayName("§2Biom")
                    .setLegacyLore(buildBiomeLore(settings));
        }

        private List<String> buildBiomeLore(RealmSettings settings) {
            return List.of(
                    "§7Aktuelles Biom: §6" + settings.getBiome(),
                    "§7Ändere das Biom",
                    "§7deines Realms",
                    "",
                    "§eKlicken für Optionen"
            );
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            player.sendMessage(ColorUtil.component("§7Biom-Änderung wird in einem zukünftigen Update verfügbar sein!"));
        }
    }

    private class SaveItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.EMERALD)
                    .setDisplayName("§aSpeichern")
                    .setLegacyLore(List.of(
                            "§7Speichere alle",
                            "§7Einstellungen",
                            "",
                            "§eKlicken zum Speichern"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            handleSave();
        }

        private void handleSave() {
            plugin.getRealmManager().updateRealm(realm);
            player.sendMessage(ColorUtil.component("§aEinstellungen gespeichert!"));
        }
    }

    private class ResetItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.REDSTONE)
                    .setDisplayName("§cZurücksetzen")
                    .setLegacyLore(List.of(
                            "§7Setze alle Einstellungen",
                            "§7auf Standardwerte zurück",
                            "",
                            "§eKlicken zum Zurücksetzen"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            handleReset();
        }

        private void handleReset() {
            realm.setSettings(new RealmSettings());
            player.sendMessage(ColorUtil.component("§cEinstellungen zurückgesetzt!"));
            new RealmSettingsGUI(plugin, player, realm).open();
        }
    }

    private static class InfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.BOOK)
                    .setDisplayName("§9Information")
                    .setLegacyLore(List.of(
                            "§7Hier kannst du alle",
                            "§7Einstellungen für dein",
                            "§7Realm anpassen",
                            "",
                            "§7Vergiss nicht zu",
                            "§7speichern!"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
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

    private List<String> buildToggleLore(boolean enabled, String description1, String description2) {
        return List.of(
                "§7Status: " + (enabled ? "§aAktiviert" : "§cDeaktiviert"),
                "§7" + description1,
                "§7" + description2,
                "",
                "§eKlicken zum Umschalten"
        );
    }
}