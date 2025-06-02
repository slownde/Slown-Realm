package de.syscall;

import de.syscall.api.RealmAPI;
import de.syscall.api.VecturAPI;
import de.syscall.command.RealmCommand;
import de.syscall.listener.PlayerJoinListener;
import de.syscall.listener.PlayerQuitListener;
import de.syscall.listener.PlayerWorldChangeListener;
import de.syscall.listener.RealmProtectionListener;
import de.syscall.manager.RealmManager;
import de.syscall.manager.SchematicManager;
import de.syscall.manager.WorldManager;
import de.syscall.storage.RealmStorage;
import org.bukkit.plugin.java.JavaPlugin;

public class SlownRealm extends JavaPlugin {

    private static SlownRealm instance;
    private static RealmAPI api;
    private VecturAPI vecturAPI;
    private RealmStorage realmStorage;
    private SchematicManager schematicManager;
    private WorldManager worldManager;
    private RealmManager realmManager;

    @Override
    public void onEnable() {
        instance = this;

        if (!setupVecturAPI()) {
            getLogger().severe("Slown-Vectur Plugin nicht gefunden!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        this.realmStorage = new RealmStorage(this);
        this.schematicManager = new SchematicManager(this);
        this.worldManager = new WorldManager(this);
        this.realmManager = new RealmManager(this);

        worldManager.createRealmWorld();

        api = new RealmAPI(this);

        registerListeners();
        registerCommands();

        getLogger().info("Slown-Realm erfolgreich gestartet!");
    }

    @Override
    public void onDisable() {
        if (realmManager != null) {
            realmManager.shutdown();
        }

        getLogger().info("Slown-Realm gestoppt!");
    }

    private boolean setupVecturAPI() {
        try {
            this.vecturAPI = VecturAPI.getInstance();
            return vecturAPI != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerWorldChangeListener(this), this);
        getServer().getPluginManager().registerEvents(new RealmProtectionListener(this), this);
    }

    private void registerCommands() {
        RealmCommand realmCommand = new RealmCommand(this);
        getCommand("realm").setExecutor(realmCommand);
        getCommand("realm").setTabCompleter(realmCommand);
    }

    public static SlownRealm getInstance() {
        return instance;
    }

    public static RealmAPI getAPI() {
        return api;
    }

    public VecturAPI getVecturAPI() {
        return vecturAPI;
    }

    public RealmStorage getRealmStorage() {
        return realmStorage;
    }

    public SchematicManager getSchematicManager() {
        return schematicManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public RealmManager getRealmManager() {
        return realmManager;
    }
}