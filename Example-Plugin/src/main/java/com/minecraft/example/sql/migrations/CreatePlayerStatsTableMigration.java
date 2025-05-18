// ./Example-Plugin/src/main/java/com/minecraft/example/sql/migrations/CreatePlayerStatsTableMigration.java
package com.minecraft.example.sql.migrations;

import com.minecraft.sqlbridge.api.migration.Migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Migration to create the player_stats table.
 * Demonstrates SQL-Bridge migration system.
 */
public class CreatePlayerStatsTableMigration implements Migration {

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public String getDescription() {
        return "Create player_stats table";
    }

    @Override
    public void migrate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Create player_stats table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS player_stats (" +
                    "  player_id INTEGER NOT NULL, " +
                    "  blocks_broken INTEGER NOT NULL DEFAULT 0, " +
                    "  blocks_placed INTEGER NOT NULL DEFAULT 0, " +
                    "  player_kills INTEGER NOT NULL DEFAULT 0, " +
                    "  mob_kills INTEGER NOT NULL DEFAULT 0, " +
                    "  deaths INTEGER NOT NULL DEFAULT 0, " +
                    "  playtime_seconds INTEGER NOT NULL DEFAULT 0, " +
                    "  distance_traveled DOUBLE NOT NULL DEFAULT 0, " +
                    "  last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "  PRIMARY KEY (player_id), " +
                    "  FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE" +
                    ")"
            );
            
            // Create index on last_updated
            statement.execute("CREATE INDEX IF NOT EXISTS idx_player_stats_last_updated ON player_stats (last_updated)");
        }
    }

    @Override
    public void rollback(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Drop index first
            try {
                statement.execute("DROP INDEX IF EXISTS idx_player_stats_last_updated");
            } catch (SQLException e) {
                // Ignore - some databases handle this differently
            }
            
            // Drop table
            statement.execute("DROP TABLE IF EXISTS player_stats");
        }
    }
}