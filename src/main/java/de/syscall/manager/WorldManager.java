package de.syscall.manager;

import de.syscall.SlownRealm;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

public class WorldManager {

    private final SlownRealm plugin;
    private World realmWorld;

    public WorldManager(SlownRealm plugin) {
        this.plugin = plugin;
    }

    public void createRealmWorld() {
        if (plugin.getServer().getWorld("realms") != null) {
            realmWorld = plugin.getServer().getWorld("realms");
            return;
        }

        WorldCreator creator = new WorldCreator("realms");
        creator.type(WorldType.FLAT);
        creator.generator(new VoidGenerator());
        creator.generateStructures(false);

        realmWorld = creator.createWorld();
        if (realmWorld != null) {
            realmWorld.setSpawnFlags(false, false);
            realmWorld.setKeepSpawnInMemory(false);
            realmWorld.setAutoSave(false);
            plugin.getLogger().info("Realm world created successfully!");
        }
    }

    public World getRealmWorld() {
        return realmWorld;
    }

    private static class VoidGenerator extends ChunkGenerator {
        @Override
        public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
            return createChunkData(world);
        }

        @Override
        public boolean canSpawn(World world, int x, int z) {
            return true;
        }
    }
}