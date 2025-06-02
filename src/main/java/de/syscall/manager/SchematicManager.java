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
        validateSchematicFiles();
    }

    private void initializeSchematicsFolder() {
        schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
            plugin.getLogger().info("Created schematics folder: " + schematicsFolder.getAbsolutePath());
        }
        plugin.getLogger().info("Schematics folder path: " + schematicsFolder.getAbsolutePath());
    }

    private void validateSchematicFiles() {
        plugin.getLogger().info("=== SCHEMATIC VALIDATION ===");
        plugin.getLogger().info("Total templates loaded: " + templates.size());

        File[] files = schematicsFolder.listFiles();
        plugin.getLogger().info("Files in schematics folder:");
        if (files != null && files.length > 0) {
            for (File file : files) {
                plugin.getLogger().info("  - " + file.getName() + " (" + file.length() + " bytes)");
            }
        } else {
            plugin.getLogger().warning("No files found in schematics folder!");
        }

        for (RealmTemplate template : templates.values()) {
            plugin.getLogger().info("Template: " + template.getName() +
                    " | Enabled: " + template.isEnabled() +
                    " | File: " + template.getSchematicFile());

            if (!template.isEnabled()) {
                plugin.getLogger().info("  -> Template disabled, skipping file check");
                continue;
            }

            File schematicFile = new File(schematicsFolder, template.getSchematicFile());
            if (!schematicFile.exists()) {
                plugin.getLogger().warning("  -> MISSING schematic file: " + template.getSchematicFile());
                plugin.getLogger().warning("     Expected location: " + schematicFile.getAbsolutePath());
            } else {
                plugin.getLogger().info("  -> Found schematic file: " + template.getSchematicFile() + " (" + schematicFile.length() + " bytes)");
            }
        }
        plugin.getLogger().info("=== END VALIDATION ===");
    }

    private void loadTemplates() {
        plugin.getLogger().info("Loading templates from config...");
        plugin.reloadConfig();

        ConfigurationSection templatesSection = plugin.getConfig().getConfigurationSection("realm.templates");
        plugin.getLogger().info("Templates section found: " + (templatesSection != null));

        if (templatesSection == null) {
            plugin.getLogger().warning("No templates section found in config, creating default templates");
            createDefaultTemplates();
            return;
        }

        plugin.getLogger().info("Template keys in config: " + templatesSection.getKeys(false));

        for (String templateName : templatesSection.getKeys(false)) {
            plugin.getLogger().info("Loading template: " + templateName);
            ConfigurationSection section = templatesSection.getConfigurationSection(templateName);
            if (section == null) {
                plugin.getLogger().warning("Section is null for template: " + templateName);
                continue;
            }

            String displayName = section.getString("display-name", templateName);
            String description = section.getString("description", "");
            String schematicFile = section.getString("schematic-file", templateName + ".schem");

            Material icon;
            try {
                String iconName = section.getString("icon", "GRASS_BLOCK");
                icon = Material.valueOf(iconName);
                plugin.getLogger().info("  Icon: " + iconName + " -> " + icon);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid icon material for template " + templateName + ", using GRASS_BLOCK");
                icon = Material.GRASS_BLOCK;
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
        }

        plugin.getLogger().info("Total templates loaded: " + templates.size());
    }

    private void createDefaultTemplates() {
        plugin.getLogger().info("Creating default template configuration...");

        plugin.getConfig().set("realm.templates.basic.display-name", "Basic Island");
        plugin.getConfig().set("realm.templates.basic.description", "A simple island to start with");
        plugin.getConfig().set("realm.templates.basic.schematic-file", "basic.schem");
        plugin.getConfig().set("realm.templates.basic.icon", "GRASS_BLOCK");
        plugin.getConfig().set("realm.templates.basic.cost", 0.0);
        plugin.getConfig().set("realm.templates.basic.permission", null);
        plugin.getConfig().set("realm.templates.basic.enabled", true);

        plugin.saveConfig();
        plugin.getLogger().info("Default template configuration saved, reloading...");
        loadTemplates();
    }

    public CompletableFuture<Boolean> pasteSchematic(String schematicName, Location location) {
        return CompletableFuture.supplyAsync(() -> {
            plugin.getLogger().info("Attempting to paste schematic: " + schematicName);
            try {
                File schematicFile = new File(schematicsFolder, schematicName);
                plugin.getLogger().info("Schematic file path: " + schematicFile.getAbsolutePath());

                if (!schematicFile.exists()) {
                    plugin.getLogger().severe("Schematic file not found: " + schematicName);
                    plugin.getLogger().severe("Expected location: " + schematicFile.getAbsolutePath());
                    plugin.getLogger().severe("Available files in schematics folder:");
                    File[] files = schematicsFolder.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            plugin.getLogger().severe("  - " + file.getName());
                        }
                    } else {
                        plugin.getLogger().severe("  No files found or folder doesn't exist");
                    }
                    return false;
                }

                ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
                if (format == null) {
                    plugin.getLogger().severe("Unknown schematic format: " + schematicName);
                    return false;
                }

                plugin.getLogger().info("Using format: " + format.getName());

                try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                    Clipboard clipboard = reader.read();
                    plugin.getLogger().info("Clipboard loaded, dimensions: " + clipboard.getDimensions());

                    try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
                        editSession.setFastMode(true);

                        Operation operation = new ClipboardHolder(clipboard)
                                .createPaste(editSession)
                                .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                                .ignoreAirBlocks(false)
                                .build();

                        Operations.complete(operation);
                        plugin.getLogger().info("Successfully pasted schematic: " + schematicName + " at " +
                                location.getX() + ", " + location.getY() + ", " + location.getZ());
                        return true;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error pasting schematic " + schematicName + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    public boolean hasSchematicFile(String templateName) {
        RealmTemplate template = templates.get(templateName);
        if (template == null) {
            plugin.getLogger().warning("Template not found: " + templateName);
            return false;
        }

        File schematicFile = new File(schematicsFolder, template.getSchematicFile());
        boolean exists = schematicFile.exists();
        plugin.getLogger().info("Schematic file check for " + templateName + ": " + exists + " (" + schematicFile.getAbsolutePath() + ")");
        return exists;
    }

    public RealmTemplate getTemplate(String name) {
        RealmTemplate template = templates.get(name);
        plugin.getLogger().info("Get template " + name + ": " + (template != null ? "found" : "not found"));
        return template;
    }

    public Map<String, RealmTemplate> getAllTemplates() {
        plugin.getLogger().info("Getting all templates, count: " + templates.size());
        return new HashMap<>(templates);
    }

    public Map<String, RealmTemplate> getAvailableTemplates() {
        plugin.getLogger().info("Getting available templates...");
        Map<String, RealmTemplate> available = new HashMap<>();

        for (Map.Entry<String, RealmTemplate> entry : templates.entrySet()) {
            String name = entry.getKey();
            RealmTemplate template = entry.getValue();

            plugin.getLogger().info("Checking template " + name + ":");
            plugin.getLogger().info("  Enabled: " + template.isEnabled());

            boolean hasFile = hasSchematicFile(name);
            plugin.getLogger().info("  Has file: " + hasFile);

            if (template.isEnabled() && hasFile) {
                available.put(name, template);
                plugin.getLogger().info("  -> Added to available templates");
            } else {
                plugin.getLogger().info("  -> Skipped (not enabled or missing file)");
            }
        }

        plugin.getLogger().info("Available templates count: " + available.size());
        return available;
    }

    public void reloadTemplates() {
        plugin.getLogger().info("Reloading templates...");
        templates.clear();
        loadTemplates();
        validateSchematicFiles();
    }
}