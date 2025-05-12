// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/impl/DefaultDatabaseService.java
package com.minecraft.sqlbridge.impl;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.DatabaseService;
import com.minecraft.sqlbridge.connection.ConnectionManager;
import com.minecraft.sqlbridge.error.DatabaseException;
import com.minecraft.sqlbridge.migration.Migration;
import com.minecraft.sqlbridge.migration.MigrationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Default implementation of the DatabaseService interface.
 */
public class DefaultDatabaseService implements DatabaseService {

    private final SqlBridgePlugin plugin;
    private final ConnectionManager connectionManager;
    private final Map<String, Database> databaseCache;
    private final Map<String, String> pluginDatabaseNames;
    private Database mainDatabase;
    private Database sharedDatabase;

    /**
     * Constructor for DefaultDatabaseService.
     *
     * @param plugin The SQL-Bridge plugin instance
     * @param connectionManager The connection manager
     */
    public DefaultDatabaseService(SqlBridgePlugin plugin, ConnectionManager connectionManager) {
        this.plugin = plugin;
        this.connectionManager = connectionManager;
        this.databaseCache = new ConcurrentHashMap<>();
        this.pluginDatabaseNames = new ConcurrentHashMap<>();
        
        initializeMainDatabase();
        
        // Initialize shared database if BungeeSupport is enabled
        if (plugin.getPluginConfig().isBungeeEnabled() && plugin.getPluginConfig().isSharedDatabaseEnabled()) {
            initializeSharedDatabase();
        }
    }

    /**
     * Initialize the main database connection.
     */
    private void initializeMainDatabase() {
        try {
            mainDatabase = new DefaultDatabase(plugin, connectionManager.getMainConnection());
            databaseCache.put("main", mainDatabase);
            LogUtil.info("Main database initialized successfully.");
        } catch (Exception e) {
            LogUtil.severe("Failed to initialize main database: " + e.getMessage());
            plugin.getErrorHandler().handleDatabaseInitError(e);
        }
    }

    /**
     * Initialize the shared database for BungeeSupport.
     */
    private void initializeSharedDatabase() {
        try {
            sharedDatabase = new DefaultDatabase(plugin, connectionManager.getSharedConnection());
            databaseCache.put("shared", sharedDatabase);
            LogUtil.info("Shared database initialized successfully.");
        } catch (Exception e) {
            LogUtil.severe("Failed to initialize shared database: " + e.getMessage());
            plugin.getErrorHandler().handleDatabaseInitError(e);
        }
    }

    @Override
    public Database getDatabase() {
        return mainDatabase;
    }

    @Override
    public Database getDatabase(String name) {
        return databaseCache.computeIfAbsent(name, this::createDatabase);
    }

    /**
     * Create a new database connection with the given name.
     *
     * @param name The name of the database connection
     * @return A new Database instance
     */
    private Database createDatabase(String name) {
        try {
            return new DefaultDatabase(plugin, connectionManager.getConnection(name));
        } catch (Exception e) {
            LogUtil.severe("Failed to create database connection '" + name + "': " + e.getMessage());
            plugin.getErrorHandler().handleDatabaseConnectionError(e);
            throw new DatabaseException("Could not create database connection: " + name, e);
        }
    }

    @Override
    public Database getDatabaseForPlugin(String pluginName) {
        String dbName = pluginDatabaseNames.computeIfAbsent(pluginName, 
                pName -> pName.toLowerCase().replace('-', '_').replace(' ', '_'));
        
        return getDatabase("plugin_" + dbName);
    }

    @Override
    public Optional<Database> getSharedDatabase() {
        return Optional.ofNullable(sharedDatabase);
    }

    @Override
    public boolean databaseExists(String name) {
        return databaseCache.containsKey(name) || connectionManager.connectionExists(name);
    }

    @Override
    public void registerMigrations(String pluginName, List<Migration> migrations) {
        try {
            MigrationManager migrationManager = plugin.getMigrationManager();
            if (migrationManager != null) {
                migrationManager.registerMigrations(pluginName, migrations);
                LogUtil.info("Registered " + migrations.size() + " migrations for plugin: " + pluginName);
            } else {
                LogUtil.warning("Migration manager is null. Could not register migrations for: " + pluginName);
            }
        } catch (Exception e) {
            LogUtil.severe("Failed to register migrations for plugin '" + pluginName + "': " + e.getMessage());
            plugin.getErrorHandler().handleMigrationError(e);
        }
    }

    @Override
    public int runMigrations(String pluginName) {
        try {
            MigrationManager migrationManager = plugin.getMigrationManager();
            if (migrationManager != null) {
                int appliedCount = migrationManager.runMigrationsForPlugin(pluginName);
                LogUtil.info("Applied " + appliedCount + " migrations for plugin: " + pluginName);
                return appliedCount;
            } else {
                LogUtil.warning("Migration manager is null. Could not run migrations for: " + pluginName);
                return 0;
            }
        } catch (Exception e) {
            LogUtil.severe("Failed to run migrations for plugin '" + pluginName + "': " + e.getMessage());
            plugin.getErrorHandler().handleMigrationError(e);
            return 0;
        }
    }

    @Override
    public CompletableFuture<Integer> runMigrationsAsync(String pluginName) {
        return CompletableFuture.supplyAsync(() -> runMigrations(pluginName))
                .exceptionally(ex -> {
                    LogUtil.severe("Error running migrations asynchronously: " + ex.getMessage());
                    plugin.getLogger().log(Level.SEVERE, "Async migration error", ex);
                    return 0;
                });
    }

    @Override
    public int getCurrentSchemaVersion(String pluginName) {
        try {
            MigrationManager migrationManager = plugin.getMigrationManager();
            if (migrationManager != null) {
                return migrationManager.getCurrentSchemaVersion(pluginName);
            } else {
                LogUtil.warning("Migration manager is null. Could not get schema version for: " + pluginName);
                return 0;
            }
        } catch (Exception e) {
            LogUtil.severe("Failed to get schema version for plugin '" + pluginName + "': " + e.getMessage());
            plugin.getErrorHandler().handleMigrationError(e);
            return 0;
        }
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Add connection pool stats
        stats.putAll(connectionManager.getStatistics());
        
        // Add performance monitoring stats if enabled
        if (plugin.getPerformanceMonitor() != null) {
            stats.putAll(plugin.getPerformanceMonitor().getStatistics());
        }
        
        // Add database cache stats
        stats.put("cachedDatabases", databaseCache.size());
        stats.put("cachedConnections", connectionManager.getActiveConnectionCount());
        
        return stats;
    }

    @Override
    public void shutdown() {
        // Close all database connections
        for (Database db : databaseCache.values()) {
            try {
                if (db instanceof DefaultDatabase) {
                    ((DefaultDatabase) db).close();
                }
            } catch (Exception e) {
                LogUtil.warning("Error closing database connection: " + e.getMessage());
            }
        }
        
        // Clear caches
        databaseCache.clear();
        pluginDatabaseNames.clear();
        
        // Shutdown connection manager
        connectionManager.close();
        
        LogUtil.info("Database service shutdown completed.");
    }
}