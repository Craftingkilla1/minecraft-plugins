// ./Example-Plugin/src/main/java/com/minecraft/example/sql/migrations/CreatePlayerAchievementsTableMigration.java
package com.minecraft.example.sql.migrations;

import com.minecraft.sqlbridge.api.migration.Migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Migration to create the player_achievements table.
 * Demonstrates SQL-Bridge migration system.
 */
public class CreatePlayerAchievementsTableMigration implements Migration {

    @Override
    public int getVersion() {
        return 4;
    }

    @Override
    public String getDescription() {
        return "Create player_achievements table";
    }

    @Override
    public void migrate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Create player_achievements table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS player_achievements (" +
                    "  player_id INTEGER NOT NULL, " +
                    "  achievement_id INTEGER NOT NULL, " +
                    "  earned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "  PRIMARY KEY (player_id, achievement_id), " +
                    "  FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE, " +
                    "  FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE" +
                    ")"
            );
            
            // Create index on earned_at
            statement.execute("CREATE INDEX IF NOT EXISTS idx_player_achievements_earned_at ON player_achievements (earned_at)");
        }
    }

    @Override
    public void rollback(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Drop index first
            try {
                statement.execute("DROP INDEX IF EXISTS idx_player_achievements_earned_at");
            } catch (SQLException e) {
                // Ignore - some databases handle this differently
            }
            
            // Drop table
            statement.execute("DROP TABLE IF EXISTS player_achievements");
        }
    }
}