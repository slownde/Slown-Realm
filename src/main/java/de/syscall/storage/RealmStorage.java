package de.syscall.storage;

import de.syscall.SlownRealm;
import de.syscall.data.Realm;
import de.syscall.data.RealmSettings;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RealmStorage {

    private final SlownRealm plugin;
    private final Map<UUID, Realm> realmCache;
    private File realmsFolder;

    public RealmStorage(SlownRealm plugin) {
        this.plugin = plugin;
        this.realmCache = new ConcurrentHashMap<>();
        initializeStorage();
    }

    private void initializeStorage() {
        realmsFolder = new File(plugin.getDataFolder(), "realms");
        if (!realmsFolder.exists()) {
            realmsFolder.mkdirs();
        }
    }

    public void saveRealm(Realm realm) {
        File realmFile = new File(realmsFolder, realm.getRealmId().toString() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(realmFile);

        config.set("id", realm.getRealmId().toString());
        config.set("owner", realm.getOwner().toString());
        config.set("name", realm.getName());
        config.set("description", realm.getDescription());
        config.set("template", realm.getTemplateName());
        config.set("active", realm.isActive());
        config.set("loaded", realm.isLoaded());
        config.set("created-time", realm.getCreatedTime());
        config.set("last-accessed", realm.getLastAccessed());
        config.set("grid.x", realm.getGridX());
        config.set("grid.z", realm.getGridZ());

        if (realm.getSpawnLocation() != null) {
            saveLocation(config, "spawn", realm.getSpawnLocation());
        }

        if (realm.getPasteLocation() != null) {
            saveLocation(config, "paste", realm.getPasteLocation());
        }

        List<String> membersList = new ArrayList<>();
        for (UUID member : realm.getMembers()) {
            membersList.add(member.toString());
        }
        config.set("members", membersList);

        List<String> visitorsList = new ArrayList<>();
        for (UUID visitor : realm.getVisitors()) {
            visitorsList.add(visitor.toString());
        }
        config.set("visitors", visitorsList);

        saveRealmSettings(config, realm.getSettings());

        try {
            config.save(realmFile);
            realmCache.put(realm.getRealmId(), realm);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save realm " + realm.getRealmId() + ": " + e.getMessage());
        }
    }

    public Realm loadRealm(UUID realmId) {
        if (realmCache.containsKey(realmId)) {
            return realmCache.get(realmId);
        }

        File realmFile = new File(realmsFolder, realmId.toString() + ".yml");
        if (!realmFile.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(realmFile);

        UUID owner = UUID.fromString(config.getString("owner"));
        String name = config.getString("name");
        String templateName = config.getString("template");

        Realm realm = new Realm(realmId, owner, name, templateName);
        realm.setDescription(config.getString("description", ""));
        realm.setActive(config.getBoolean("active", true));
        realm.setLoaded(config.getBoolean("loaded", false));
        realm.setCreatedTime(config.getLong("created-time", System.currentTimeMillis()));
        realm.setLastAccessed(config.getLong("last-accessed", System.currentTimeMillis()));
        realm.setGridX(config.getInt("grid.x", 0));
        realm.setGridZ(config.getInt("grid.z", 0));

        Location spawnLocation = loadLocation(config, "spawn");
        if (spawnLocation != null) {
            realm.setSpawnLocation(spawnLocation);
        }

        Location pasteLocation = loadLocation(config, "paste");
        if (pasteLocation != null) {
            realm.setPasteLocation(pasteLocation);
        }

        List<String> membersList = config.getStringList("members");
        for (String memberStr : membersList) {
            try {
                realm.addMember(UUID.fromString(memberStr));
            } catch (IllegalArgumentException ignored) {}
        }

        List<String> visitorsList = config.getStringList("visitors");
        for (String visitorStr : visitorsList) {
            try {
                realm.addVisitor(UUID.fromString(visitorStr));
            } catch (IllegalArgumentException ignored) {}
        }

        RealmSettings settings = loadRealmSettings(config);
        realm.setSettings(settings);

        realmCache.put(realmId, realm);
        return realm;
    }

    public boolean deleteRealm(UUID realmId) {
        File realmFile = new File(realmsFolder, realmId.toString() + ".yml");
        realmCache.remove(realmId);
        return realmFile.delete();
    }

    public Collection<Realm> getAllRealms() {
        List<Realm> realms = new ArrayList<>();
        File[] files = realmsFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files != null) {
            for (File file : files) {
                String fileName = file.getName().replace(".yml", "");
                try {
                    UUID realmId = UUID.fromString(fileName);
                    Realm realm = loadRealm(realmId);
                    if (realm != null) {
                        realms.add(realm);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        return realms;
    }

    public Collection<Realm> getRealmsByOwner(UUID owner) {
        return getAllRealms().stream()
                .filter(realm -> realm.getOwner().equals(owner))
                .toList();
    }

    private void saveLocation(FileConfiguration config, String path, Location location) {
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
    }

    private Location loadLocation(FileConfiguration config, String path) {
        if (!config.isConfigurationSection(path)) {
            return null;
        }

        String worldName = config.getString(path + ".world");
        if (worldName == null) {
            return null;
        }

        return new Location(
                plugin.getServer().getWorld(worldName),
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw"),
                (float) config.getDouble(path + ".pitch")
        );
    }

    private void saveRealmSettings(FileConfiguration config, RealmSettings settings) {
        config.set("settings.public", settings.isPublic());
        config.set("settings.pvp", settings.isPvpEnabled());
        config.set("settings.mob-spawning", settings.isMobSpawning());
        config.set("settings.animal-spawning", settings.isAnimalSpawning());
        config.set("settings.fire-spread", settings.isFireSpread());
        config.set("settings.explosions", settings.isExplosions());
        config.set("settings.leaf-decay", settings.isLeafDecay());
        config.set("settings.weather-changes", settings.isWeatherChanges());
        config.set("settings.day-night-cycle", settings.isDayNightCycle());
        config.set("settings.time-of-day", settings.getTimeOfDay());
        config.set("settings.biome", settings.getBiome());
    }

    private RealmSettings loadRealmSettings(FileConfiguration config) {
        RealmSettings settings = new RealmSettings();

        if (config.isConfigurationSection("settings")) {
            ConfigurationSection settingsSection = config.getConfigurationSection("settings");
            settings.setPublic(settingsSection.getBoolean("public", false));
            settings.setPvpEnabled(settingsSection.getBoolean("pvp", false));
            settings.setMobSpawning(settingsSection.getBoolean("mob-spawning", true));
            settings.setAnimalSpawning(settingsSection.getBoolean("animal-spawning", true));
            settings.setFireSpread(settingsSection.getBoolean("fire-spread", false));
            settings.setExplosions(settingsSection.getBoolean("explosions", false));
            settings.setLeafDecay(settingsSection.getBoolean("leaf-decay", true));
            settings.setWeatherChanges(settingsSection.getBoolean("weather-changes", true));
            settings.setDayNightCycle(settingsSection.getBoolean("day-night-cycle", true));
            settings.setTimeOfDay(settingsSection.getInt("time-of-day", 6000));
            settings.setBiome(settingsSection.getString("biome", "PLAINS"));
        }

        return settings;
    }
}