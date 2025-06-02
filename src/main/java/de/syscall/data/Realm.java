package de.syscall.data;

import org.bukkit.Location;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Realm {

    private final UUID realmId;
    private final UUID owner;
    private String name;
    private String description;
    private String templateName;
    private boolean active;
    private boolean loaded;
    private long createdTime;
    private long lastAccessed;
    private Location spawnLocation;
    private Location pasteLocation;
    private int gridX;
    private int gridZ;
    private final Set<UUID> members;
    private final Set<UUID> visitors;
    private RealmSettings settings;

    public Realm(UUID realmId, UUID owner, String name, String templateName) {
        this.realmId = realmId;
        this.owner = owner;
        this.name = name;
        this.templateName = templateName;
        this.description = "";
        this.active = true;
        this.loaded = false;
        this.createdTime = System.currentTimeMillis();
        this.lastAccessed = System.currentTimeMillis();
        this.members = ConcurrentHashMap.newKeySet();
        this.visitors = ConcurrentHashMap.newKeySet();
        this.settings = new RealmSettings();
    }

    public UUID getRealmId() {
        return realmId;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public Location getSpawnLocation() {
        return spawnLocation != null ? spawnLocation.clone() : null;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation != null ? spawnLocation.clone() : null;
    }

    public Location getPasteLocation() {
        return pasteLocation != null ? pasteLocation.clone() : null;
    }

    public void setPasteLocation(Location pasteLocation) {
        this.pasteLocation = pasteLocation != null ? pasteLocation.clone() : null;
    }

    public int getGridX() {
        return gridX;
    }

    public void setGridX(int gridX) {
        this.gridX = gridX;
    }

    public int getGridZ() {
        return gridZ;
    }

    public void setGridZ(int gridZ) {
        this.gridZ = gridZ;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public void addMember(UUID member) {
        members.add(member);
    }

    public void removeMember(UUID member) {
        members.remove(member);
    }

    public boolean isMember(UUID player) {
        return members.contains(player);
    }

    public Set<UUID> getVisitors() {
        return visitors;
    }

    public void addVisitor(UUID visitor) {
        visitors.add(visitor);
    }

    public void removeVisitor(UUID visitor) {
        visitors.remove(visitor);
    }

    public boolean hasAccess(UUID player) {
        return owner.equals(player) || members.contains(player) || (settings.isPublic() && visitors.contains(player));
    }

    public RealmSettings getSettings() {
        return settings;
    }

    public void setSettings(RealmSettings settings) {
        this.settings = settings;
    }

    public void updateLastAccessed() {
        this.lastAccessed = System.currentTimeMillis();
    }
}