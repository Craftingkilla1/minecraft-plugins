// ./Example-Plugin/src/main/java/com/minecraft/example/database/DatabaseManager.java
package com.minecraft.example.database;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.PlayerStatsPlugin;
import com.minecraft.example.database.dao.AchievementDAO;
import com.minecraft.example.database.dao.LeaderboardDAO;
import com.minecraft.example.database.dao.PlayerDataDAO;
import com.minecraft.example.database.migration.CreateInitialTables;
import com.minecraft.example.database.migration.AddIndexesMigration;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.DatabaseService;
import com.minecraft.sqlbridge.api.migration.Migration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages database connections and data access objects.
 */
public class DatabaseManager {
    
    private final PlayerStatsPlugin plugin;
    private final DatabaseService databaseService;
    private Database database;
    
    // Data Access Objects
    private PlayerDataDAO playerDataDAO;
    private AchievementDAO achievementDAO;
    private LeaderboardDAO leaderboardDAO;
    
    /**
     * Creates a new DatabaseManager instance.
     *
     * @param plugin The plugin instance
     * @param databaseService The database service from SQL-Bridge
     */
    public DatabaseManager(PlayerStatsPlugin plugin, DatabaseService databaseService) {
        this.plugin = plugin;
        this.databaseService = databaseService;
    }
    
    /**
     * Initializes the database and DAOs.
     *
     * @throws SQLException If an error occurs during initialization
     */
    public void initialize() throws SQLException {
        // Get a database connection for our plugin
        this.database = databaseService.getDatabaseForPlugin(plugin.getName());
        
        if (database == null) {
            throw new SQLException("Failed to get database connection from SQL-Bridge");
        }
        
        // Register database migrations
        registerMigrations();
        
        // Create DAOs
        this.playerDataDAO = new PlayerDataDAO(plugin, database);
        this.achievementDAO = new AchievementDAO(plugin, database);
        this.leaderboardDAO = new LeaderboardDAO(plugin, database);
        
        // Run database migrations
        runMigrations();
    }
    
    /**
     * Registers database migrations.
     */
    private void registerMigrations() {
        List<Migration> migrations = new ArrayList<>();
        migrations.add(new CreateInitialTables());
        migrations.add(new AddIndexesMigration());
        
        databaseService.registerMigrations(plugin.getName(), migrations);
        LogUtil.info("Registered " + migrations.size() + " database migrations");
    }
    
    /**
     * Runs database migrations.
     */
    private void runMigrations() {
        databaseService.runMigrationsAsync(plugin.getName())
                .thenAccept(count -> {
                    LogUtil.info("Applied " + count + " database migrations");
                })
                .exceptionally(e -> {
                    LogUtil.severe("Failed to run database migrations: " + e.getMessage());
                    plugin.getLogger().log(Level.SEVERE, "Migration error", e);
                    return null;
                });
    }
    
    /**
     * Executes a query to test database connectivity.
     *
     * @return A CompletableFuture that completes with true if the test succeeds
     */
    public CompletableFuture<Boolean> testConnection() {
        return database.updateAsync("CREATE TABLE IF NOT EXISTS test_connection (id INT)")
                .thenCompose(result -> database.updateAsync("DROP TABLE IF EXISTS test_connection"))
                .thenApply(result -> true)
                .exceptionally(e -> {
                    LogUtil.severe("Database connection test failed: " + e.getMessage());
                    return false;
                });
    }
    
    /**
     * Shuts down the database manager and closes connections.
     */
    public void shutdown() {
        LogUtil.info("Shutting down database manager");
        // Nothing to do here as SQL-Bridge manages connections
    }
    
    /**
     * Gets the player data DAO.
     *
     * @return The player data DAO
     */
    public PlayerDataDAO getPlayerDataDAO() {
        return playerDataDAO;
    }
    
    /**
     * Gets the achievement DAO.
     *
     * @return The achievement DAO
     */
    public AchievementDAO getAchievementDAO() {
        return achievementDAO;
    }
    
    /**
     * Gets the leaderboard DAO.
     *
     * @return The leaderboard DAO
     */
    public LeaderboardDAO getLeaderboardDAO() {
        return leaderboardDAO;
    }
    
    /**
     * Gets the database instance.
     *
     * @return The database instance
     */
    public Database getDatabase() {
        return database;
    }
}