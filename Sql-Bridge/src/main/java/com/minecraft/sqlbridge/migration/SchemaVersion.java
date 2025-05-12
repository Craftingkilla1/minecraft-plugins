// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/migration/SchemaVersion.java
package com.minecraft.sqlbridge.migration;

import java.sql.Timestamp;

/**
 * Represents a database schema version for a specific plugin.
 */
public class SchemaVersion {
    
    private final String pluginName;
    private final int version;
    private final Timestamp appliedAt;
    private final String description;
    
    /**
     * Constructor for SchemaVersion.
     *
     * @param pluginName The name of the plugin
     * @param version The schema version
     * @param appliedAt The timestamp when the migration was applied
     * @param description The migration description
     */
    public SchemaVersion(String pluginName, int version, Timestamp appliedAt, String description) {
        this.pluginName = pluginName;
        this.version = version;
        this.appliedAt = appliedAt;
        this.description = description;
    }
    
    /**
     * Get the name of the plugin.
     *
     * @return The plugin name
     */
    public String getPluginName() {
        return pluginName;
    }
    
    /**
     * Get the schema version.
     *
     * @return The version number
     */
    public int getVersion() {
        return version;
    }
    
    /**
     * Get the timestamp when the migration was applied.
     *
     * @return The applied timestamp
     */
    public Timestamp getAppliedAt() {
        return appliedAt;
    }
    
    /**
     * Get the migration description.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return "SchemaVersion{" +
               "pluginName='" + pluginName + '\'' +
               ", version=" + version +
               ", appliedAt=" + appliedAt +
               ", description='" + description + '\'' +
               '}';
    }
}