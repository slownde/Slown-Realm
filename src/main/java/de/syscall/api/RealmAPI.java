package de.syscall.api;

import de.syscall.SlownRealm;
import de.syscall.data.Realm;
import de.syscall.data.RealmTemplate;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RealmAPI {

    private static RealmAPI instance;
    private final SlownRealm plugin;

    public RealmAPI(SlownRealm plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static RealmAPI getInstance() {
        return instance;
    }

    public CompletableFuture<Realm> createRealm(Player player, String name, String templateName) {
        return plugin.getRealmManager().createRealm(player, name, templateName);
    }

    public CompletableFuture<Boolean> deleteRealm(UUID realmId) {
        return plugin.getRealmManager().deleteRealm(realmId);
    }

    public CompletableFuture<Void> teleportToRealm(Player player, UUID realmId) {
        return plugin.getRealmManager().teleportToRealm(player, realmId);
    }

    public Realm getPlayerRealm(Player player) {
        return plugin.getRealmManager().getPlayerRealm(player);
    }

    public Collection<Realm> getPlayerOwnedRealms(UUID owner) {
        return plugin.getRealmManager().getPlayerOwnedRealms(owner);
    }

    public Realm getRealm(UUID realmId) {
        return plugin.getRealmManager().getRealm(realmId);
    }

    public void updateRealm(Realm realm) {
        plugin.getRealmManager().updateRealm(realm);
    }

    public Collection<Realm> getAllRealms() {
        return plugin.getRealmStorage().getAllRealms();
    }

    public Map<String, RealmTemplate> getAllTemplates() {
        return plugin.getSchematicManager().getAllTemplates();
    }

    public RealmTemplate getTemplate(String name) {
        return plugin.getSchematicManager().getTemplate(name);
    }

    public boolean isPlayerInRealm(Player player, UUID realmId) {
        return plugin.getRealmManager().isPlayerInRealm(player, realmId);
    }

    public CompletableFuture<Void> loadRealm(UUID realmId) {
        return plugin.getRealmManager().loadRealm(realmId);
    }

    public void unloadRealm(UUID realmId) {
        plugin.getRealmManager().unloadRealm(realmId);
    }

    public CompletableFuture<Boolean> pasteSchematic(String schematicName, org.bukkit.Location location) {
        return plugin.getSchematicManager().pasteSchematic(schematicName, location);
    }
}