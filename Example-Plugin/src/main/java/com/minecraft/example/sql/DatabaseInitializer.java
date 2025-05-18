// ./Example-Plugin/src/main/java/com/minecraft/example/sql/DatabaseInitializer.java
package com.minecraft.example.sql;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.ExamplePlugin;
import com.minecraft.example.sql.migrations.CreateAchievementsTableMigration;
import com.minecraft.example.sql.migrations.CreatePlayerAchievementsTableMigration;
import com.minecraft.example.sql.migrations.CreatePlayerStatsTableMigration;
import com.minecraft.example.sql.migrations.CreatePlayersTableMigration;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.DatabaseService;
import com.minecraft.sqlbridge.api.migration.Migration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database initialization and migrations.
 * Demonstrates SQL-Bridge migration system.
 */
public class DatabaseInitializer {

    private final ExamplePlugin plugin;
    private final Database database;
    
    /**
     * Constructs a new DatabaseInitializer.
     *
     * @param plugin The plugin instance
     * @param database The database instance
     */
    public DatabaseInitializer(ExamplePlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }
    
    /**
     * Initializes the database schema.
     */
    public void initialize() {
        try {
            // Register migrations
            List<Migration> migrations = createMigrations();
            
            DatabaseService databaseService = plugin.getServer().getServicesManager()
                    .getRegistration(DatabaseService.class).getProvider();
            
            // Register migrations with the database service
            databaseService.registerMigrations(plugin, migrations);
            
            // Run migrations
            int appliedCount = databaseService.runMigrationsSafe(plugin);
            LogUtil.info("Applied " + appliedCount + " database migrations");
            
            // Check current schema version
            int currentVersion = databaseService.getCurrentSchemaVersion(plugin);
            LogUtil.info("Current database schema version: " + currentVersion);
            
        } catch (Exception e) {
            LogUtil.severe("Failed to initialize database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates the list of migrations.
     *
     * @return The list of migrations
     */
    private List<Migration> createMigrations() {
        List<Migration> migrations = new ArrayList<>();
        
        // Add migrations in order
        migrations.add(new CreatePlayersTableMigration());
        migrations.add(new CreatePlayerStatsTableMigration());
        migrations.add(new CreateAchievementsTableMigration());
        migrations.add(new CreatePlayerAchievementsTableMigration());
        
        return migrations;
    }
    
    /**
     * Creates initial database data if needed.
     */
    public void createInitialData() {
        try {
            createDefaultAchievements();
        } catch (SQLException e) {
            LogUtil.severe("Failed to create initial data: " + e.getMessage());
        }
    }
    
    /**
     * Creates default achievements in the database.
     */
    private void createDefaultAchievements() throws SQLException {
        // Check if achievements table is empty
        boolean hasAchievements = database.queryFirst(
                "SELECT COUNT(*) as count FROM achievements",
                row -> row.getInt("count") > 0
        ).orElse(false);
        
        // If no achievements exist, create default ones
        if (!hasAchievements) {
            LogUtil.info("Creating default achievements...");
            
            // Insert default achievements using batch update
            List<Object[]> achievements = new ArrayList<>();
            
            // Add default achievements from messages.yml
            achievements.add(new Object[]{"first_join", "First Steps", "Join the server for the first time"});
            achievements.add(new Object[]{"blocks_broken_100", "Demolition Expert", "Break 100 blocks"});
            achievements.add(new Object[]{"blocks_broken_1000", "Block Destroyer", "Break 1,000 blocks"});
            achievements.add(new Object[]{"blocks_placed_100", "Constructor", "Place 100 blocks"});
            achievements.add(new Object[]{"blocks_placed_1000", "Master Builder", "Place 1,000 blocks"});
            achievements.add(new Object[]{"player_kills_10", "PvP Novice", "Defeat 10 players"});
            achievements.add(new Object[]{"mob_kills_100", "Monster Hunter", "Defeat 100 mobs"});
            achievements.add(new Object[]{"playtime_1_hour", "Dedicated", "Play for 1 hour"});
            achievements.add(new Object[]{"playtime_10_hours", "Veteran", "Play for 10 hours"});
            achievements.add(new Object[]{"distance_1_km", "Explorer", "Travel 1 kilometer"});
            achievements.add(new Object[]{"distance_10_km", "Globe Trotter", "Travel 10 kilometers"});
            
            // Execute batch insert
            database.batchUpdate(
                    "INSERT INTO achievements (identifier, name, description) VALUES (?, ?, ?)",
                    achievements
            );
            
            LogUtil.info("Created " + achievements.size() + " default achievements");
        }
    }
}