package com.minecraft.sqlbridge.migrations;

import com.minecraft.sqlbridge.api.Database;

/**
 * Interface for database migrations.
 * Migrations are used to apply and revert schema changes.
 */
public interface Migration {

    /**
     * Get the migration version number
     * 
     * @return The version number
     */
    int getVersion();
    
    /**
     * Get the migration description
     * 
     * @return The description
     */
    String getDescription();
    
    /**
     * Apply the migration
     * 
     * @param database The database
     * @return True if the migration was applied successfully
     */
    boolean up(Database database);
    
    /**
     * Revert the migration
     * 
     * @param database The database
     * @return True if the migration was reverted successfully
     */
    boolean down(Database database);
}