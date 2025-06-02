package de.syscall.manager;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import de.syscall.SlownRealm;
import de.syscall.data.RealmTemplate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SchematicManager {

    private final SlownRealm plugin;
    private final Map<String, RealmTemplate> templates;
    private File schematicsFolder;

    public SchematicManager(SlownRealm plugin) {
        this.plugin = plugin;
        this.templates = new HashMap<>();
        initializeSchematicsFolder();
        loadTemplates();
    }

    private void initializeSchematicsFolder() {
        schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }
    }

    private void loadTemplates() {
        plugin.reloadConfig();
        ConfigurationSection templatesSection = plugin.getConfig().getConfigurationSection("realm.templates");

        if (templatesSection == null) {
            createDefaultTemplates();
            return;
        }

        for (String templateName : templatesSection.getKeys(false)) {
            ConfigurationSection section = templatesSection.getConfigurationSection(templateName);
            if (section == null) continue;

            String displayName = section.getString("display-name", templateName);
            String description = section.getString("description", "");
            String schematicFile = section.getString("schematic-file", templateName + ".schem");
            Material icon = Material.valueOf(section.getString("icon", "GRASS_BLOCK"));
            double cost = section.getDouble("cost", 0.0);
            String permission = section.getString("permission", null);
            boolean enabled = section.getBoolean("enabled", true);

            RealmTemplate template = new RealmTemplate(templateName, displayName, description,
                    schematicFile, icon, cost, permission, enabled);
            templates.put(templateName, template);
        }
    }

    private void createDefaultTemplates() {
        plugin.getConfig().set("realm.templates.basic.display-name", "Basic Island");
        plugin.getConfig().set("realm.templates.basic.description", "A simple island to start with");
        plugin.getConfig().set("realm.templates.basic.schematic-file", "basic.schem");
        plugin.getConfig().set("realm.templates.basic.icon", "GRASS_BLOCK");
        plugin.getConfig().set("realm.templates.basic.cost", 0.0);
        plugin.getConfig().set("realm.templates.basic.permission", null);
        plugin.getConfig().set("realm.templates.basic.enabled", true);

        plugin.getConfig().set("realm.templates.desert.display-name", "Desert Oasis");
        plugin.getConfig().set("realm.templates.desert.description", "A desert themed realm");
        plugin.getConfig().set("realm.templates.desert.schematic-file", "desert.schem");
        plugin.getConfig().set("realm.templates.desert.icon", "SAND");
        plugin.getConfig().set("realm.templates.desert.cost", 100.0);
        plugin.getConfig().set("realm.templates.desert.permission", null);
        plugin.getConfig().set("realm.templates.desert.enabled", true);

        plugin.saveConfig();
        loadTemplates();
    }

    public CompletableFuture<Boolean> pasteSchematic(String schematicName, Location location) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File schematicFile = new File(schematicsFolder, schematicName);
                if (!schematicFile.exists()) {
                    plugin.getLogger().warning("Schematic file not found: " + schematicName);
                    return false;
                }

                ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
                if (format == null) {
                    plugin.getLogger().warning("Unknown schematic format: " + schematicName);
                    return false;
                }

                try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                    Clipboard clipboard = reader.read();

                    try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
                        Operation operation = new ClipboardHolder(clipboard)
                                .createPaste(editSession)
                                .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                                .ignoreAirBlocks(false)
                                .build();

                        Operations.complete(operation);
                        return true;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error pasting schematic: " + e.getMessage());
                return false;
            }
        });
    }

    public RealmTemplate getTemplate(String name) {
        return templates.get(name);
    }

    public Map<String, RealmTemplate> getAllTemplates() {
        return new HashMap<>(templates);
    }

    public void reloadTemplates() {
        templates.clear();
        loadTemplates();
    }
}