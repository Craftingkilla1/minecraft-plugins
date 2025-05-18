// ./Example-Plugin/src/main/java/com/minecraft/example/sql/migrations/CreatePlayersTableMigration.java
package com.minecraft.example.sql.migrations;

import com.minecraft.sqlbridge.api.migration.Migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Migration to create the players table.
 * Demonstrates SQL-Bridge migration system.
 */
public class CreatePlayersTableMigration implements Migration {

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public String getDescription() {
        return "Create players table";
    }

    @Override
    public void migrate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Create players table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS players (" +
                    "  id INTEGER PRIMARY KEY " + getAutoIncrementSyntax(connection) + ", " +
                    "  uuid VARCHAR(36) NOT NULL UNIQUE, " +
                    "  name VARCHAR(16) NOT NULL, " +
                    "  first_join TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "  last_join TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")"
            );
            
            // Create index on uuid
            statement.execute("CREATE INDEX IF NOT EXISTS idx_players_uuid ON players (uuid)");
            
            // Create index on name
            statement.execute("CREATE INDEX IF NOT EXISTS idx_players_name ON players (name)");
        }
    }

    @Override
    public void rollback(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Drop indexes first
            try {
                statement.execute("DROP INDEX IF EXISTS idx_players_name");
                statement.execute("DROP INDEX IF EXISTS idx_players_uuid");
            } catch (SQLException e) {
                // Ignore - some databases handle this differently
            }
            
            // Drop table
            statement.execute("DROP TABLE IF EXISTS players");
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