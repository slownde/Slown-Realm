package de.syscall.data;

import org.bukkit.Material;

public class RealmTemplate {

    private final String name;
    private final String displayName;
    private final String description;
    private final String schematicFile;
    private final Material icon;
    private final double cost;
    private final String permission;
    private final boolean enabled;

    public RealmTemplate(String name, String displayName, String description, String schematicFile,
                         Material icon, double cost, String permission, boolean enabled) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.schematicFile = schematicFile;
        this.icon = icon;
        this.cost = cost;
        this.permission = permission;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getSchematicFile() {
        return schematicFile;
    }

    public Material getIcon() {
        return icon;
    }

    public double getCost() {
        return cost;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isEnabled() {
        return enabled;
    }
}