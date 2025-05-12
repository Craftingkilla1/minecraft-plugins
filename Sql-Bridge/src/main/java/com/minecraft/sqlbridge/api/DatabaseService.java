// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/DatabaseService.java
package com.minecraft.sqlbridge.api;

import com.minecraft.sqlbridge.migration.Migration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
     * Run all pending migrations for a plugin.
     *
     * @param pluginName The name of the plugin
     * @return The number of migrations that were applied
     */
    int runMigrations(String pluginName);
    
    /**
     * Run all pending migrations for a plugin asynchronously.
     *
     * @param pluginName The name of the plugin
     * @return A CompletableFuture containing the number of migrations that were applied
     */
    CompletableFuture<Integer> runMigrationsAsync(String pluginName);
    
    /**
     * Get the current schema version for a plugin.
     *
     * @param pluginName The name of the plugin
     * @return The current schema version
     */
    int getCurrentSchemaVersion(String pluginName);
    
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