// ./Example-Plugin/src/main/java/com/minecraft/example/database/migration/CreateInitialTables.java
package com.minecraft.example.database.migration;

import com.minecraft.sqlbridge.api.Migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Migration to create the initial database tables.
 */
public class CreateInitialTables implements Migration {
    
    @Override
    public int getVersion() {
        return 1;
    }
    
    @Override
    public String getDescription() {
        return "Create initial tables for PlayerStats plugin";
    }
    
    @Override
    public void migrate(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Create players table
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS players (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36) NOT NULL UNIQUE, " +
                    "name VARCHAR(16) NOT NULL, " +
                    "first_join TIMESTAMP NOT NULL, " +
                    "last_seen TIMESTAMP NOT NULL, " +
                    "playtime BIGINT NOT NULL DEFAULT 0" +
                    ")"
            );
            
            // Create stats table
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS stats (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "player_id INTEGER NOT NULL, " +
                    "stat_name VARCHAR(64) NOT NULL, " +
                    "stat_value INTEGER NOT NULL DEFAULT 0, " +
                    "last_updated TIMESTAMP NOT NULL, " +
                    "FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE, " +
                    "UNIQUE (player_id, stat_name)" +
                    ")"
            );
            
            // Create achievements table
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS achievements (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "achievement_id VARCHAR(64) NOT NULL UNIQUE, " +
                    "name VARCHAR(64) NOT NULL, " +
                    "description TEXT, " +
                    "icon_material VARCHAR(64) NOT NULL, " +
                    "category VARCHAR(32), " +
                    "secret BOOLEAN NOT NULL DEFAULT 0, " +
                    "criteria_json TEXT" +
                    ")"
            );
            
            // Create player_achievements table
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS player_achievements (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "player_id INTEGER NOT NULL, " +
                    "achievement_id VARCHAR(64) NOT NULL, " +
                    "earned_date TIMESTAMP NOT NULL, " +
                    "progress FLOAT NOT NULL DEFAULT 0.0, " +
                    "FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (achievement_id) REFERENCES achievements(achievement_id) ON DELETE CASCADE, " +
                    "UNIQUE (player_id, achievement_id)" +
                    ")"
            );
            
            // Create leaderboards table
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS leaderboards (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "leaderboard_id VARCHAR(64) NOT NULL UNIQUE, " +
                    "display_name VARCHAR(64) NOT NULL, " +
                    "stat_name VARCHAR(64) NOT NULL, " +
                    "icon_material VARCHAR(64) NOT NULL, " +
                    "category VARCHAR(32), " +
                    "reversed BOOLEAN NOT NULL DEFAULT 0, " +
                    "update_interval INTEGER NOT NULL DEFAULT 30, " +
                    "last_updated TIMESTAMP, " +
                    "description TEXT" +
                    ")"
            );
            
            // Create leaderboard_entries table
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS leaderboard_entries (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "leaderboard_id VARCHAR(64) NOT NULL, " +
                    "player_id INTEGER NOT NULL, " +
                    "rank INTEGER NOT NULL, " +
                    "score INTEGER NOT NULL, " +
                    "update_time TIMESTAMP NOT NULL, " +
                    "previous_rank INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY (leaderboard_id) REFERENCES leaderboards(leaderboard_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE, " +
                    "UNIQUE (leaderboard_id, player_id)" +
                    ")"
            );
        }
    }
    
    @Override
    public void rollback(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Drop tables in reverse order to avoid foreign key constraints
            stmt.execute("DROP TABLE IF EXISTS leaderboard_entries");
            stmt.execute("DROP TABLE IF EXISTS leaderboards");
            stmt.execute("DROP TABLE IF EXISTS player_achievements");
            stmt.execute("DROP TABLE IF EXISTS achievements");
            stmt.execute("DROP TABLE IF EXISTS stats");
            stmt.execute("DROP TABLE IF EXISTS players");
        }
    }
}