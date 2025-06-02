package de.syscall.listener;

import de.syscall.SlownRealm;
import de.syscall.data.Realm;
import de.syscall.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;

public class RealmProtectionListener implements Listener {

    private final SlownRealm plugin;

    public RealmProtectionListener(SlownRealm plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.getPlayer().getWorld().getName().equals("realms")) return;

        Player player = event.getPlayer();
        Realm realm = plugin.getRealmManager().getPlayerRealm(player);

        if (realm == null) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.component("§cDu befindest dich nicht in einem gültigen Realm!"));
            return;
        }

        if (!realm.hasAccess(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.component("§cDu hast keine Berechtigung in diesem Realm zu bauen!"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.getPlayer().getWorld().getName().equals("realms")) return;

        Player player = event.getPlayer();
        Realm realm = plugin.getRealmManager().getPlayerRealm(player);

        if (realm == null) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.component("§cDu befindest dich nicht in einem gültigen Realm!"));
            return;
        }

        if (!realm.hasAccess(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.component("§cDu hast keine Berechtigung in diesem Realm zu bauen!"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getPlayer().getWorld().getName().equals("realms")) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Realm realm = plugin.getRealmManager().getPlayerRealm(player);

        if (realm == null) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.component("§cDu befindest dich nicht in einem gültigen Realm!"));
            return;
        }

        if (!realm.hasAccess(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.component("§cDu hast keine Berechtigung in diesem Realm!"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!event.getEntity().getWorld().getName().equals("realms")) return;

        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player victim) {
            Realm realm = plugin.getRealmManager().getPlayerRealm(attacker);

            if (realm != null && !realm.getSettings().isPvpEnabled()) {
                event.setCancelled(true);
                attacker.sendMessage(ColorUtil.component("§cPvP ist in diesem Realm deaktiviert!"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!event.getLocation().getWorld().getName().equals("realms")) return;

        // Find realm at spawn location
        for (Realm realm : plugin.getRealmStorage().getAllRealms()) {
            if (realm.isLoaded() && isInRealmBounds(event.getLocation(), realm)) {
                if (event.getEntity().getType().name().contains("MONSTER") && !realm.getSettings().isMobSpawning()) {
                    event.setCancelled(true);
                    return;
                }

                if (isAnimal(event.getEntity().getType().name()) && !realm.getSettings().isAnimalSpawning()) {
                    event.setCancelled(true);
                    return;
                }
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!event.getWorld().getName().equals("realms")) return;

        // Check if any loaded realm has weather changes disabled
        for (Realm realm : plugin.getRealmStorage().getAllRealms()) {
            if (realm.isLoaded() && !realm.getSettings().isWeatherChanges()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTimeSkip(TimeSkipEvent event) {
        if (!event.getWorld().getName().equals("realms")) return;

        // Check if any loaded realm has day/night cycle disabled
        for (Realm realm : plugin.getRealmStorage().getAllRealms()) {
            if (realm.isLoaded() && !realm.getSettings().isDayNightCycle()) {
                event.setCancelled(true);
                event.getWorld().setTime(realm.getSettings().getTimeOfDay());
                return;
            }
        }
    }

    private boolean isInRealmBounds(org.bukkit.Location location, Realm realm) {
        if (realm.getPasteLocation() == null) return false;

        org.bukkit.Location paste = realm.getPasteLocation();
        double distance = location.distance(paste.clone().add(128, 0, 128));
        return distance <= 181; // sqrt(128^2 + 128^2) = ~181
    }

    private boolean isAnimal(String entityType) {
        return entityType.contains("COW") || entityType.contains("PIG") || entityType.contains("SHEEP") ||
                entityType.contains("CHICKEN") || entityType.contains("HORSE") || entityType.contains("WOLF") ||
                entityType.contains("CAT") || entityType.contains("RABBIT") || entityType.contains("LLAMA");
    }
}