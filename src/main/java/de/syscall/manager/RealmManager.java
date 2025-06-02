package de.syscall.manager;

import de.syscall.SlownRealm;
import de.syscall.data.Realm;
import de.syscall.data.RealmTemplate;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RealmManager {

    private final SlownRealm plugin;
    private final Map<UUID, Realm> loadedRealms;
    private final Map<UUID, UUID> playerToRealm;
    private final Map<String, int[]> gridPositions;
    private final Set<String> usedGridPositions;
    private BukkitTask cleanupTask;

    private static final int REALM_SIZE = 256;
    private static final int REALM_SPACING = 512;

    public RealmManager(SlownRealm plugin) {
        this.plugin = plugin;
        this.loadedRealms = new ConcurrentHashMap<>();
        this.playerToRealm = new ConcurrentHashMap<>();
        this.gridPositions = new ConcurrentHashMap<>();
        this.usedGridPositions = ConcurrentHashMap.newKeySet();
        startCleanupTask();
    }

    private void startCleanupTask() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupInactiveRealms();
            }
        }.runTaskTimerAsynchronously(plugin, 6000L, 6000L);
    }

    public CompletableFuture<Realm> createRealm(Player player, String name, String templateName) {
        return CompletableFuture.supplyAsync(() -> {
            UUID realmId = UUID.randomUUID();
            Realm realm = new Realm(realmId, player.getUniqueId(), name, templateName);

            int[] gridPos = findAvailableGridPosition();
            realm.setGridX(gridPos[0]);
            realm.setGridZ(gridPos[1]);

            Location pasteLocation = calculatePasteLocation(gridPos[0], gridPos[1]);
            realm.setPasteLocation(pasteLocation);

            Location spawnLocation = pasteLocation.clone().add(128, 64, 128);
            realm.setSpawnLocation(spawnLocation);

            plugin.getRealmStorage().saveRealm(realm);
            return realm;
        });
    }

    public CompletableFuture<Boolean> deleteRealm(UUID realmId) {
        return CompletableFuture.supplyAsync(() -> {
            Realm realm = loadedRealms.get(realmId);
            if (realm == null) {
                realm = plugin.getRealmStorage().loadRealm(realmId);
            }

            if (realm != null) {
                unloadRealm(realmId);
                releaseGridPosition(realm.getGridX(), realm.getGridZ());
                return plugin.getRealmStorage().deleteRealm(realmId);
            }
            return false;
        });
    }

    public CompletableFuture<Void> loadRealm(UUID realmId) {
        return CompletableFuture.runAsync(() -> {
            if (loadedRealms.containsKey(realmId)) {
                return;
            }

            Realm realm = plugin.getRealmStorage().loadRealm(realmId);
            if (realm == null) return;

            RealmTemplate template = plugin.getSchematicManager().getTemplate(realm.getTemplateName());
            if (template == null) return;

            plugin.getSchematicManager().pasteSchematic(template.getSchematicFile(), realm.getPasteLocation())
                    .thenRun(() -> {
                        realm.setLoaded(true);
                        loadedRealms.put(realmId, realm);
                        plugin.getRealmStorage().saveRealm(realm);
                    });
        });
    }

    public void unloadRealm(UUID realmId) {
        Realm realm = loadedRealms.remove(realmId);
        if (realm != null) {
            realm.setLoaded(false);
            plugin.getRealmStorage().saveRealm(realm);

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (isPlayerInRealm(player, realmId)) {
                    player.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                }
            }
        }
    }

    public CompletableFuture<Void> teleportToRealm(Player player, UUID realmId) {
        return CompletableFuture.runAsync(() -> {
            Realm realm = loadedRealms.get(realmId);
            if (realm == null) {
                loadRealm(realmId).thenRun(() -> {
                    Realm loadedRealm = loadedRealms.get(realmId);
                    if (loadedRealm != null && loadedRealm.hasAccess(player.getUniqueId())) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            player.teleport(loadedRealm.getSpawnLocation());
                            playerToRealm.put(player.getUniqueId(), realmId);
                            loadedRealm.updateLastAccessed();
                        });
                    }
                });
            } else if (realm.hasAccess(player.getUniqueId())) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.teleport(realm.getSpawnLocation());
                    playerToRealm.put(player.getUniqueId(), realmId);
                    realm.updateLastAccessed();
                });
            }
        });
    }

    public void handlePlayerQuit(Player player) {
        playerToRealm.remove(player.getUniqueId());
    }

    public void handleWorldChange(Player player) {
        UUID currentRealm = playerToRealm.get(player.getUniqueId());
        if (currentRealm != null && !isPlayerInRealm(player, currentRealm)) {
            playerToRealm.remove(player.getUniqueId());
        }
    }

    public boolean isPlayerInRealm(Player player, UUID realmId) {
        return playerToRealm.get(player.getUniqueId()) != null &&
                playerToRealm.get(player.getUniqueId()).equals(realmId) &&
                player.getWorld().getName().equals("realms");
    }

    public Realm getPlayerRealm(Player player) {
        UUID realmId = playerToRealm.get(player.getUniqueId());
        return realmId != null ? loadedRealms.get(realmId) : null;
    }

    public Collection<Realm> getPlayerOwnedRealms(UUID owner) {
        return plugin.getRealmStorage().getRealmsByOwner(owner);
    }

    public Realm getRealm(UUID realmId) {
        Realm realm = loadedRealms.get(realmId);
        if (realm == null) {
            realm = plugin.getRealmStorage().loadRealm(realmId);
        }
        return realm;
    }

    public void updateRealm(Realm realm) {
        loadedRealms.put(realm.getRealmId(), realm);
        plugin.getRealmStorage().saveRealm(realm);
    }

    private int[] findAvailableGridPosition() {
        int x = 0, z = 0;
        int radius = 0;

        while (true) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                        String key = dx + "," + dz;
                        if (!usedGridPositions.contains(key)) {
                            usedGridPositions.add(key);
                            gridPositions.put(key, new int[]{dx, dz});
                            return new int[]{dx, dz};
                        }
                    }
                }
            }
            radius++;
        }
    }

    private void releaseGridPosition(int x, int z) {
        String key = x + "," + z;
        usedGridPositions.remove(key);
        gridPositions.remove(key);
    }

    private Location calculatePasteLocation(int gridX, int gridZ) {
        int worldX = gridX * REALM_SPACING;
        int worldZ = gridZ * REALM_SPACING;
        return new Location(plugin.getWorldManager().getRealmWorld(), worldX, 64, worldZ);
    }

    private void cleanupInactiveRealms() {
        long currentTime = System.currentTimeMillis();
        long maxInactiveTime = plugin.getConfig().getLong("realm.max-inactive-time", 1800000);

        loadedRealms.entrySet().removeIf(entry -> {
            Realm realm = entry.getValue();
            boolean hasPlayers = plugin.getServer().getOnlinePlayers().stream()
                    .anyMatch(player -> isPlayerInRealm(player, realm.getRealmId()));

            if (!hasPlayers && (currentTime - realm.getLastAccessed()) > maxInactiveTime) {
                unloadRealm(realm.getRealmId());
                return true;
            }
            return false;
        });
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        for (UUID realmId : new HashSet<>(loadedRealms.keySet())) {
            unloadRealm(realmId);
        }
    }
}