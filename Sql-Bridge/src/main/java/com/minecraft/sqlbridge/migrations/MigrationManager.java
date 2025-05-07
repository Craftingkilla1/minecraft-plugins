package com.minecraft.sqlbridge.migrations;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.connection.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages database migrations for plugins.
 * Handles creating, tracking, and applying migrations.
 */
public class MigrationManager {

    private final SqlBridgePlugin plugin;
    private final ConnectionManager connectionManager;
    private final Map<String, List<Migration>> pluginMigrations = new HashMap<>();
    private static final String MIGRATIONS_TABLE = "sql_bridge_migrations";

    /**
     * Create a new migration manager
     *
     * @param plugin The SQL-Bridge plugin
     * @param connectionManager The connection manager
     */
    public MigrationManager(SqlBridgePlugin plugin, ConnectionManager connectionManager) {
        this.plugin = plugin;
        this.connectionManager = connectionManager;
        
        // Initialize migrations table
        initMigrationsTable();
    }

    /**
     * Register migrations for a plugin
     *
     * @param pluginName The plugin name
     * @param migrations The migrations to register
     */
    public void registerMigrations(String pluginName, List<Migration> migrations) {
        // Sort migrations by version
        Collections.sort(migrations, (a, b) -> Integer.compare(a.getVersion(), b.getVersion()));
        
        // Register the migrations
        pluginMigrations.put(pluginName, migrations);
        
        LogUtil.info("Registered " + migrations.size() + " migrations for " + pluginName);
    }

    /**
     * Run all pending migrations for a plugin
     *
     * @param pluginName The plugin name
     * @return True if all migrations were applied successfully
     */
    public boolean runMigrations(String pluginName) {
        List<Migration> migrations = pluginMigrations.get(pluginName);
        
        if (migrations == null || migrations.isEmpty()) {
            LogUtil.info("No migrations found for " + pluginName);
            return true;
        }
        
        int current = getCurrentVersion(pluginName);
        boolean success = true;
        
        LogUtil.info("Current migration version for " + pluginName + ": " + current);
        
        for (Migration migration : migrations) {
            if (migration.getVersion() > current) {
                LogUtil.info("Running migration " + migration.getVersion() + ": " + migration.getDescription());
                
                try {
                    // Get the database from the connection manager
                    Database database = plugin.getServer().getServicesManager()
                            .getRegistration(Database.class).getProvider();
                    
                    // Apply the migration
                    if (migration.up(database)) {
                        // Update the version in the database
                        recordMigration(pluginName, migration);
                        LogUtil.info("Migration " + migration.getVersion() + " applied successfully");
                    } else {
                        LogUtil.severe("Migration " + migration.getVersion() + " failed");
                        success = false;
                        break;
                    }
                } catch (Exception e) {
                    LogUtil.severe("Error applying migration " + migration.getVersion() + ": " + e.getMessage());
                    e.printStackTrace();
                    success = false;
                    break;
                }
            }
        }
        
        return success;
    }

    /**
     * Rollback migrations to a specific version
     *
     * @param pluginName The plugin name
     * @param targetVersion The target version to rollback to
     * @return True if all migrations were rolled back successfully
     */
    public boolean rollback(String pluginName, int targetVersion) {
        List<Migration> migrations = pluginMigrations.get(pluginName);
        
        if (migrations == null || migrations.isEmpty()) {
            LogUtil.info("No migrations found for " + pluginName);
            return true;
        }
        
        // Get all applied migrations
        List<Migration> appliedMigrations = getAppliedMigrations(pluginName);
        boolean success = true;
        
        // Reverse the order of applied migrations for rollback
        Collections.reverse(appliedMigrations);
        
        for (Migration migration : appliedMigrations) {
            if (migration.getVersion() > targetVersion) {
                LogUtil.info("Rolling back migration " + migration.getVersion() + ": " + migration.getDescription());
                
                try {
                    // Get the database from the connection manager
                    Database database = plugin.getServer().getServicesManager()
                            .getRegistration(Database.class).getProvider();
                    
                    // Rollback the migration
                    if (migration.down(database)) {
                        // Remove the migration from the database
                        removeMigration(pluginName, migration);
                        LogUtil.info("Migration " + migration.getVersion() + " rolled back successfully");
                    } else {
                        LogUtil.severe("Rolling back migration " + migration.getVersion() + " failed");
                        success = false;
                        break;
                    }
                } catch (Exception e) {
                    LogUtil.severe("Error rolling back migration " + migration.getVersion() + ": " + e.getMessage());
                    e.printStackTrace();
                    success = false;
                    break;
                }
            }
        }
        
        return success;
    }

    /**
     * Get the current migration version for a plugin
     *
     * @param pluginName The plugin name
     * @return The current version
     */
    public int getCurrentVersion(String pluginName) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = connectionManager.getConnection();
            
            String sql = "SELECT MAX(version) FROM " + MIGRATIONS_TABLE + " WHERE plugin = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, pluginName);
            
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int version = resultSet.getInt(1);
                return resultSet.wasNull() ? 0 : version;
            }
            
            return 0;
        } catch (SQLException e) {
            LogUtil.warning("Error getting current migration version: " + e.getMessage());
            return 0;
        } finally {
            closeResources(connection, statement, resultSet);
        }
    }

    /**
     * Check if a plugin has any pending migrations
     *
     * @param pluginName The plugin name
     * @return True if there are pending migrations
     */
    public boolean hasPendingMigrations(String pluginName) {
        List<Migration> migrations = pluginMigrations.get(pluginName);
        
        if (migrations == null || migrations.isEmpty()) {
            return false;
        }
        
        int current = getCurrentVersion(pluginName);
        
        for (Migration migration : migrations) {
            if (migration.getVersion() > current) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Get all applied migrations for a plugin
     *
     * @param pluginName The plugin name
     * @return List of applied migrations
     */
    private List<Migration> getAppliedMigrations(String pluginName) {
        List<Migration> applied = new ArrayList<>();
        List<Migration> migrations = pluginMigrations.get(pluginName);
        
        if (migrations == null || migrations.isEmpty()) {
            return applied;
        }
        
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = connectionManager.getConnection();
            
            String sql = "SELECT version FROM " + MIGRATIONS_TABLE + " WHERE plugin = ? ORDER BY version";
            statement = connection.prepareStatement(sql);
            statement.setString(1, pluginName);
            
            resultSet = statement.executeQuery();
            
            // Create a map of version to migration
            Map<Integer, Migration> migrationMap = new HashMap<>();
            for (Migration migration : migrations) {
                migrationMap.put(migration.getVersion(), migration);
            }
            
            // Add applied migrations to the list
            while (resultSet.next()) {
                int version = resultSet.getInt("version");
                Migration migration = migrationMap.get(version);
                
                if (migration != null) {
                    applied.add(migration);
                }
            }
            
            return applied;
        } catch (SQLException e) {
            LogUtil.warning("Error getting applied migrations: " + e.getMessage());
            return applied;
        } finally {
            closeResources(connection, statement, resultSet);
        }
    }

    /**
     * Record a migration in the database
     *
     * @param pluginName The plugin name
     * @param migration The migration
     */
    private void recordMigration(String pluginName, Migration migration) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = connectionManager.getConnection();
            
            String sql = "INSERT INTO " + MIGRATIONS_TABLE + " (plugin, version, description, applied_at) VALUES (?, ?, ?, NOW())";
            statement = connection.prepareStatement(sql);
            statement.setString(1, pluginName);
            statement.setInt(2, migration.getVersion());
            statement.setString(3, migration.getDescription());
            
            statement.executeUpdate();
        } catch (SQLException e) {
            LogUtil.severe("Error recording migration: " + e.getMessage());
        } finally {
            closeResources(connection, statement, null);
        }
    }

    /**
     * Remove a migration from the database
     *
     * @param pluginName The plugin name
     * @param migration The migration
     */
    private void removeMigration(String pluginName, Migration migration) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = connectionManager.getConnection();
            
            String sql = "DELETE FROM " + MIGRATIONS_TABLE + " WHERE plugin = ? AND version = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, pluginName);
            statement.setInt(2, migration.getVersion());
            
            statement.executeUpdate();
        } catch (SQLException e) {
            LogUtil.severe("Error removing migration: " + e.getMessage());
        } finally {
            closeResources(connection, statement, null);
        }
    }

    /**
     * Initialize the migrations table
     */
    private void initMigrationsTable() {
        Connection connection = null;
        Statement statement = null;
        
        try {
            connection = connectionManager.getConnection();
            statement = connection.createStatement();
            
            // Create the migrations table
            String sql = "CREATE TABLE IF NOT EXISTS " + MIGRATIONS_TABLE + " (" +
                    "id INTEGER PRIMARY KEY " + 
                    (connectionManager.getType() == com.minecraft.sqlbridge.api.DatabaseType.SQLITE ? "AUTOINCREMENT" : "AUTO_INCREMENT") + ", " +
                    "plugin VARCHAR(100) NOT NULL, " +
                    "version INTEGER NOT NULL, " +
                    "description VARCHAR(255) NOT NULL, " +
                    "applied_at TIMESTAMP NOT NULL, " +
                    "UNIQUE(plugin, version)" +
                    ")";
            
            statement.executeUpdate(sql);
            
            LogUtil.info("Migrations table initialized");
        } catch (SQLException e) {
            LogUtil.severe("Error initializing migrations table: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(connection, statement, null);
        }
    }

    /**
     * Close database resources
     *
     * @param connection The connection
     * @param statement The statement
     * @param resultSet The result set
     */
    private void closeResources(Connection connection, Statement statement, ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                // Ignore
            }
        }
        
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                // Ignore
            }
        }
        
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // Ignore
            }
        }
    }
}