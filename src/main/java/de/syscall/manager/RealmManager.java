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

        plugin.getLogger().info("RealmManager initialized");
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
        plugin.getLogger().info("Creating realm for player " + player.getName() + ": " + name + " with template " + templateName);

        return CompletableFuture.supplyAsync(() -> {
            UUID realmId = UUID.randomUUID();
            plugin.getLogger().info("Generated realm ID: " + realmId);

            Realm realm = new Realm(realmId, player.getUniqueId(), name, templateName);

            int[] gridPos = findAvailableGridPosition();
            plugin.getLogger().info("Grid position: " + gridPos[0] + ", " + gridPos[1]);

            realm.setGridX(gridPos[0]);
            realm.setGridZ(gridPos[1]);

            Location pasteLocation = calculatePasteLocation(gridPos[0], gridPos[1]);
            plugin.getLogger().info("Paste location: " + pasteLocation);
            realm.setPasteLocation(pasteLocation);

            Location spawnLocation = pasteLocation.clone().add(128, 64, 128);
            plugin.getLogger().info("Spawn location: " + spawnLocation);
            realm.setSpawnLocation(spawnLocation);

            plugin.getRealmStorage().saveRealm(realm);
            plugin.getLogger().info("Realm saved to storage");

            return realm;
        });
    }

    public CompletableFuture<Boolean> deleteRealm(UUID realmId) {
        plugin.getLogger().info("Deleting realm: " + realmId);

        return CompletableFuture.supplyAsync(() -> {
            Realm realm = loadedRealms.get(realmId);
            if (realm == null) {
                plugin.getLogger().info("Realm not in memory, loading from storage");
                realm = plugin.getRealmStorage().loadRealm(realmId);
            }

            if (realm != null) {
                plugin.getLogger().info("Unloading and deleting realm: " + realm.getName());
                unloadRealm(realmId);
                releaseGridPosition(realm.getGridX(), realm.getGridZ());
                boolean deleted = plugin.getRealmStorage().deleteRealm(realmId);
                plugin.getLogger().info("Realm deletion result: " + deleted);
                return deleted;
            }
            plugin.getLogger().warning("Realm not found for deletion: " + realmId);
            return false;
        });
    }

    public CompletableFuture<Void> loadRealm(UUID realmId) {
        plugin.getLogger().info("Loading realm: " + realmId);

        return CompletableFuture.runAsync(() -> {
            if (loadedRealms.containsKey(realmId)) {
                plugin.getLogger().info("Realm already loaded: " + realmId);
                return;
            }

            Realm realm = plugin.getRealmStorage().loadRealm(realmId);
            if (realm == null) {
                plugin.getLogger().warning("Failed to load realm from storage: " + realmId);
                return;
            }

            plugin.getLogger().info("Loaded realm from storage: " + realm.getName());

            RealmTemplate template = plugin.getSchematicManager().getTemplate(realm.getTemplateName());
            if (template == null) {
                plugin.getLogger().warning("Template not found for realm: " + realm.getTemplateName());
                return;
            }

            plugin.getLogger().info("Found template: " + template.getDisplayName());

            plugin.getSchematicManager().pasteSchematic(template.getSchematicFile(), realm.getPasteLocation())
                    .thenRun(() -> {
                        plugin.getLogger().info("Schematic pasted successfully for realm: " + realm.getName());
                        realm.setLoaded(true);
                        loadedRealms.put(realmId, realm);
                        plugin.getRealmStorage().saveRealm(realm);
                        plugin.getLogger().info("Realm marked as loaded: " + realm.getName());
                    })
                    .exceptionally(throwable -> {
                        plugin.getLogger().severe("Failed to paste schematic for realm: " + realm.getName());
                        throwable.printStackTrace();
                        return null;
                    });
        });
    }

    public void unloadRealm(UUID realmId) {
        plugin.getLogger().info("Unloading realm: " + realmId);

        Realm realm = loadedRealms.remove(realmId);
        if (realm != null) {
            plugin.getLogger().info("Unloaded realm: " + realm.getName());
            realm.setLoaded(false);
            plugin.getRealmStorage().saveRealm(realm);

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (isPlayerInRealm(player, realmId)) {
                    plugin.getLogger().info("Teleporting player " + player.getName() + " out of unloaded realm");
                    player.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                }
            }
        }
    }

    public CompletableFuture<Void> teleportToRealm(Player player, UUID realmId) {
        plugin.getLogger().info("Teleporting player " + player.getName() + " to realm: " + realmId);

        return CompletableFuture.runAsync(() -> {
            Realm realm = loadedRealms.get(realmId);
            if (realm == null) {
                plugin.getLogger().info("Realm not loaded, loading first...");

                realm = plugin.getRealmStorage().loadRealm(realmId);
                if (realm == null) {
                    plugin.getLogger().severe("Failed to load realm from storage: " + realmId);
                    return;
                }

                plugin.getLogger().info("Checking access for player " + player.getName() + " to realm " + realm.getName());
                if (!realm.hasAccess(player.getUniqueId())) {
                    plugin.getLogger().warning("Player " + player.getName() + " has no access to realm " + realm.getName());
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§cDu hast keinen Zugriff auf dieses Realm!");
                    });
                    return;
                }

                loadRealm(realmId).thenRun(() -> {
                    Realm loadedRealm = loadedRealms.get(realmId);
                    if (loadedRealm != null) {
                        plugin.getLogger().info("Realm loaded, teleporting player");
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            player.teleport(loadedRealm.getSpawnLocation());
                            playerToRealm.put(player.getUniqueId(), realmId);
                            loadedRealm.updateLastAccessed();
                            player.sendMessage("§aWillkommen in deinem Realm: §6" + loadedRealm.getName());
                        });
                    } else {
                        plugin.getLogger().warning("Realm loading failed");
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            player.sendMessage("§cFehler beim Laden des Realms!");
                        });
                    }
                });
            } else if (realm.hasAccess(player.getUniqueId())) {
                plugin.getLogger().info("Realm already loaded, teleporting player");
                Realm finalRealm = realm;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.teleport(finalRealm.getSpawnLocation());
                    playerToRealm.put(player.getUniqueId(), realmId);
                    finalRealm.updateLastAccessed();
                    player.sendMessage("§aWillkommen in deinem Realm: §6" + finalRealm.getName());
                });
            } else {
                plugin.getLogger().warning("Player " + player.getName() + " has no access to realm " + realmId);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§cDu hast keinen Zugriff auf dieses Realm!");
                });
            }
        });
    }

    public void handlePlayerQuit(Player player) {
        UUID removedRealm = playerToRealm.remove(player.getUniqueId());
        if (removedRealm != null) {
            plugin.getLogger().info("Player " + player.getName() + " quit, removed from realm tracking");
        }
    }

    public void handleWorldChange(Player player) {
        UUID currentRealm = playerToRealm.get(player.getUniqueId());
        if (currentRealm != null && !isPlayerInRealm(player, currentRealm)) {
            playerToRealm.remove(player.getUniqueId());
            plugin.getLogger().info("Player " + player.getName() + " left realm world, removed from tracking");
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
        plugin.getLogger().info("Getting owned realms for player: " + owner);
        Collection<Realm> realms = plugin.getRealmStorage().getRealmsByOwner(owner);
        plugin.getLogger().info("Found " + realms.size() + " realms for player " + owner);
        return realms;
    }

    public Realm getRealm(UUID realmId) {
        Realm realm = loadedRealms.get(realmId);
        if (realm == null) {
            realm = plugin.getRealmStorage().loadRealm(realmId);
        }
        return realm;
    }

    public void updateRealm(Realm realm) {
        plugin.getLogger().info("Updating realm: " + realm.getName());
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
                plugin.getLogger().info("Cleaning up inactive realm: " + realm.getName());
                unloadRealm(realm.getRealmId());
                return true;
            }
            return false;
        });
    }

    public void shutdown() {
        plugin.getLogger().info("Shutting down RealmManager");
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        for (UUID realmId : new HashSet<>(loadedRealms.keySet())) {
            unloadRealm(realmId);
        }
        plugin.getLogger().info("RealmManager shutdown complete");
    }
}