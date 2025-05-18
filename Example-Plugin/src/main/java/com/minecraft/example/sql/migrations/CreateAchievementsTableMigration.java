// ./Example-Plugin/src/main/java/com/minecraft/example/sql/migrations/CreateAchievementsTableMigration.java
package com.minecraft.example.sql.migrations;

import com.minecraft.sqlbridge.api.migration.Migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Migration to create the achievements table.
 * Demonstrates SQL-Bridge migration system.
 */
public class CreateAchievementsTableMigration implements Migration {

    @Override
    public int getVersion() {
        return 3;
    }

    @Override
    public String getDescription() {
        return "Create achievements table";
    }

    @Override
    public void migrate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Create achievements table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS achievements (" +
                    "  id INTEGER PRIMARY KEY " + getAutoIncrementSyntax(connection) + ", " +
                    "  identifier VARCHAR(50) NOT NULL UNIQUE, " +
                    "  name VARCHAR(100) NOT NULL, " +
                    "  description VARCHAR(255) NOT NULL, " +
                    "  icon_material VARCHAR(50) DEFAULT 'DIAMOND', " +
                    "  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")"
            );
            
            // Create index on identifier
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_achievements_identifier ON achievements (identifier)");
        }
    }

    @Override
    public void rollback(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Drop index first
            try {
                statement.execute("DROP INDEX IF EXISTS idx_achievements_identifier");
            } catch (SQLException e) {
                // Ignore - some databases handle this differently
            }
            
            // Drop table
            statement.execute("DROP TABLE IF EXISTS achievements");
        }
    }
    
    /**
     * Gets the auto-increment syntax for the current database type.
     * Different databases have different syntax for auto-increment columns.
     */
    private String getAutoIncrementSyntax(Connection connection) throws SQLException {
        String dbType = connection.getMetaData().getDatabaseProductName().toLowerCase();
        
        if (dbType.contains("mysql") || dbType.contains("mariadb")) {
            return "AUTO_INCREMENT";
        } else if (dbType.contains("postgresql")) {
            return "SERIAL";
        } else if (dbType.contains("sqlite")) {
            return "AUTOINCREMENT";
        } else if (dbType.contains("h2")) {
            return "AUTO_INCREMENT";
        } else {
            // Default to standard SQL syntax
            return "AUTO_INCREMENT";
        }
    }
}