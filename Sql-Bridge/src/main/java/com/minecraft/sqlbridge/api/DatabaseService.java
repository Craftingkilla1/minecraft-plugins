// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/DatabaseService.java
package com.minecraft.sqlbridge.api;

import com.minecraft.sqlbridge.migration.Migration;

import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service interface for database operations.
 * This is the main entry point for plugins to interact with SQL-Bridge.
 */
public interface DatabaseService {

    /**
     * Get the main database connection.
     *
     * @return The Database interface for the main database
     */
    Database getDatabase();
    
    /**
     * Get a named database connection.
     * If the named connection doesn't exist, it will be created using default settings.
     *
     * @param name The name of the database connection
     * @return The Database interface for the named database
     */
    Database getDatabase(String name);
    
    /**
     * Get a database connection for a specific plugin.
     * This creates a database connection with a database name or table prefix
     * specific to the plugin.
     *
     * @param pluginName The name of the plugin
     * @return The Database interface for the plugin's database
     */
    Database getDatabaseForPlugin(String pluginName);
    
    /**
     * Get a database connection for a specific plugin.
     *
     * @param plugin The plugin instance
     * @return The Database interface for the plugin's database
     */
    default Database getDatabaseForPlugin(Plugin plugin) {
        return getDatabaseForPlugin(plugin.getName());
    }
    
    /**
     * Get a shared database connection for use across a BungeeCord network.
     * This is only available if BungeeSupport is enabled.
     *
     * @return An Optional containing the shared Database, or empty if BungeeSupport is disabled
     */
    Optional<Database> getSharedDatabase();
    
    /**
     * Check if a named database exists.
     *
     * @param name The name of the database
     * @return true if the database exists, false otherwise
     */
    boolean databaseExists(String name);
    
    /**
     * Register migrations for a plugin.
     * These migrations will be run automatically during server startup if auto-migration is enabled.
     *
     * @param pluginName The name of the plugin
     * @param migrations The list of migrations to register
     */
    void registerMigrations(String pluginName, List<Migration> migrations);
    
    /**
     * Register migrations for a plugin.
     *
     * @param plugin The plugin instance
     * @param migrations The list of migrations to register
     */
    default void registerMigrations(Plugin plugin, List<Migration> migrations) {
        registerMigrations(plugin.getName(), migrations);
    }
    
    /**
     * Run all pending migrations for a plugin.
     *
     * @param pluginName The name of the plugin
     * @return The number of migrations that were applied
     */
    int runMigrations(String pluginName);
    
    /**
     * Run all pending migrations for a plugin.
     *
     * @param plugin The plugin instance
     * @return The number of migrations that were applied
     */
    default int runMigrations(Plugin plugin) {
        return runMigrations(plugin.getName());
    }
    
    /**
     * Run all pending migrations for a plugin asynchronously.
     *
     * @param pluginName The name of the plugin
     * @return A CompletableFuture containing the number of migrations that were applied
     */
    CompletableFuture<Integer> runMigrationsAsync(String pluginName);
    
    /**
     * Run all pending migrations for a plugin asynchronously.
     *
     * @param plugin The plugin instance
     * @return A CompletableFuture containing the number of migrations that were applied
     */
    default CompletableFuture<Integer> runMigrationsAsync(Plugin plugin) {
        return runMigrationsAsync(plugin.getName());
    }
    
    /**
     * Run all pending migrations for a plugin with safe error handling.
     *
     * @param pluginName The name of the plugin
     * @param logger The logger to use for error logging
     * @return The number of migrations that were applied, or 0 if migrations fail
     */
    default int runMigrationsSafe(String pluginName, Logger logger) {
        try {
            return runMigrations(pluginName);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to run migrations for plugin '" + pluginName + "'", e);
            return 0;
        }
    }
    
    /**
     * Run all pending migrations for a plugin with safe error handling.
     *
     * @param plugin The plugin instance
     * @return The number of migrations that were applied, or 0 if migrations fail
     */
    default int runMigrationsSafe(Plugin plugin) {
        return runMigrationsSafe(plugin.getName(), plugin.getLogger());
    }
    
    /**
     * Get the current schema version for a plugin.
     *
     * @param pluginName The name of the plugin
     * @return The current schema version
     */
    int getCurrentSchemaVersion(String pluginName);
    
    /**
     * Get the current schema version for a plugin.
     *
     * @param plugin The plugin instance
     * @return The current schema version
     */
    default int getCurrentSchemaVersion(Plugin plugin) {
        return getCurrentSchemaVersion(plugin.getName());
    }
    
    /**
     * Initialize the database for a plugin. This creates the necessary tables
     * and runs migrations if needed. This is a convenience method for standard setup.
     *
     * @param plugin The plugin instance
     * @param createTableStatements SQL statements to create tables if they don't exist
     * @return true if the database was initialized successfully, false otherwise
     */
    default boolean initializeDatabase(Plugin plugin, String... createTableStatements) {
        try {
            // Get database for this plugin
            Database db = getDatabaseForPlugin(plugin);
            
            // Create tables
            boolean success = true;
            for (String sql : createTableStatements) {
                try {
                    db.update(sql);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to execute: " + sql, e);
                    success = false;
                }
            }
            
            // Run migrations if any are registered
            if (getCurrentSchemaVersion(plugin) >= 0 || runMigrationsSafe(plugin) > 0) {
                plugin.getLogger().info("Database schema is up to date.");
            }
            
            return success;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }
    
    /**
     * Get database statistics.
     *
     * @return Database statistics and metrics as key-value pairs
     */
    java.util.Map<String, Object> getStatistics();
    
    /**
     * Close all database connections and clean up resources.
     * This is typically called when the plugin is disabled.
     */
    void shutdown();
}