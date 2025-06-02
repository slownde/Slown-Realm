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
        plugin.getLogger().info("Schematics folder path: " + schematicsFolder.getAbsolutePath());
    }

    private void loadTemplates() {
        plugin.reloadConfig();
        ConfigurationSection templatesSection = plugin.getConfig().getConfigurationSection("realm.templates");

        plugin.getLogger().info("Loading templates from config...");
        plugin.getLogger().info("Templates section found: " + (templatesSection != null));

        if (templatesSection == null) {
            plugin.getLogger().warning("No templates section found in config, creating defaults");
            createDefaultTemplates();
            return;
        }

        plugin.getLogger().info("Template keys in config: " + templatesSection.getKeys(false));

        for (String templateName : templatesSection.getKeys(false)) {
            ConfigurationSection section = templatesSection.getConfigurationSection(templateName);
            if (section == null) continue;

            plugin.getLogger().info("Loading template: " + templateName);

            try {
                String displayName = section.getString("display-name", templateName);
                String description = section.getString("description", "");
                String schematicFile = section.getString("schematic-file", templateName + ".schem");

                String iconString = section.getString("icon", "GRASS_BLOCK");
                Material icon = Material.GRASS_BLOCK;
                try {
                    icon = Material.valueOf(iconString.toUpperCase());
                    plugin.getLogger().info("  Icon: " + iconString + " -> " + icon);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("  Invalid icon material: " + iconString + ", using GRASS_BLOCK");
                }

                double cost = section.getDouble("cost", 0.0);
                String permission = section.getString("permission", null);
                boolean enabled = section.getBoolean("enabled", true);

                plugin.getLogger().info("  Display Name: " + displayName);
                plugin.getLogger().info("  Description: " + description);
                plugin.getLogger().info("  Schematic File: " + schematicFile);
                plugin.getLogger().info("  Cost: " + cost);
                plugin.getLogger().info("  Permission: " + permission);
                plugin.getLogger().info("  Enabled: " + enabled);

                RealmTemplate template = new RealmTemplate(templateName, displayName, description,
                        schematicFile, icon, cost, permission, enabled);
                templates.put(templateName, template);
                plugin.getLogger().info("Successfully loaded template: " + templateName);

            } catch (Exception e) {
                plugin.getLogger().severe("Error loading template " + templateName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Total templates loaded: " + templates.size());
        validateSchematicFiles();
    }

    private void validateSchematicFiles() {
        plugin.getLogger().info("=== SCHEMATIC VALIDATION ===");
        plugin.getLogger().info("Total templates loaded: " + templates.size());

        File[] files = schematicsFolder.listFiles();
        if (files != null) {
            plugin.getLogger().info("Files in schematics folder:");
            for (File file : files) {
                if (file.isFile()) {
                    plugin.getLogger().info("  - " + file.getName() + " (" + file.length() + " bytes)");
                }
            }
        }

        for (RealmTemplate template : templates.values()) {
            plugin.getLogger().info("Template: " + template.getName() + " | Enabled: " + template.isEnabled() + " | File: " + template.getSchematicFile());

            if (!template.isEnabled()) {
                plugin.getLogger().info("  -> Template disabled, skipping file check");
                continue;
            }

            File schematicFile = new File(schematicsFolder, template.getSchematicFile());
            if (schematicFile.exists()) {
                plugin.getLogger().info("  -> Found schematic file: " + template.getSchematicFile() + " (" + schematicFile.length() + " bytes)");
            } else {
                plugin.getLogger().warning("  -> Missing schematic file: " + template.getSchematicFile());
            }
        }
        plugin.getLogger().info("=== END VALIDATION ===");
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
                plugin.getLogger().info("Attempting to paste schematic: " + schematicName);
                plugin.getLogger().info("Schematic file path: " + schematicFile.getAbsolutePath());

                if (!schematicFile.exists()) {
                    plugin.getLogger().warning("Schematic file not found: " + schematicName);
                    return false;
                }

                ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
                if (format == null) {
                    plugin.getLogger().warning("Unknown schematic format for file: " + schematicName);
                    return false;
                }

                plugin.getLogger().info("Using format: " + format.getName());

                Clipboard clipboard;
                try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                    clipboard = reader.read();
                    plugin.getLogger().info("Clipboard loaded, dimensions: " + clipboard.getDimensions());
                }

                CompletableFuture<Boolean> result = new CompletableFuture<>();

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
                            editSession.setFastMode(true);

                            Operation operation = new ClipboardHolder(clipboard)
                                    .createPaste(editSession)
                                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                                    .ignoreAirBlocks(false)
                                    .build();

                            Operations.complete(operation);
                            editSession.flushSession();

                            plugin.getLogger().info("Schematic pasted successfully: " + schematicName);
                            result.complete(true);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error pasting schematic " + schematicName + " in main thread: " + e.getMessage());
                        e.printStackTrace();
                        result.complete(false);
                    }
                });

                return result.get();

            } catch (Exception e) {
                plugin.getLogger().severe("Error preparing schematic " + schematicName + ": " + e.getMessage());
                e.printStackTrace();
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