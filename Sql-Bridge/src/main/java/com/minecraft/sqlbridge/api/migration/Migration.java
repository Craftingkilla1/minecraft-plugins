// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/migration/Migration.java
package com.minecraft.sqlbridge.api.migration;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface representing a database migration.
 * Migrations are used to manage changes to the database schema over time.
 */
public interface Migration {
    
    /**
     * Get the version number of this migration.
     * Each migration should have a unique, sequential version number.
     *
     * @return The version number
     */
    int getVersion();
    
    /**
     * Get a description of this migration.
     *
     * @return The migration description
     */
    default String getDescription() {
        return "Migration v" + getVersion();
    }
    
    /**
     * Apply the migration to the database.
     *
     * @param connection The database connection
     * @throws SQLException If an error occurs during migration
     */
    void migrate(Connection connection) throws SQLException;
    
    /**
     * Rollback the migration if it fails.
     * This method is called when a migration fails and rollback is enabled.
     *
     * @param connection The database connection
     * @throws SQLException If an error occurs during rollback
     */
    void rollback(Connection connection) throws SQLException;
}