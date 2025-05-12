// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/migration/MigrationManager.java
package com.minecraft.sqlbridge.migration;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.connection.ConnectionManager;
import com.minecraft.sqlbridge.error.DatabaseException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages database migrations for plugins.
 */
public class MigrationManager {
    
    private static final String SCHEMA_VERSION_TABLE = "sql_bridge_schema_versions";
    
    private final SqlBridgePlugin plugin;
    private final ConnectionManager connectionManager;
    private final Map<String, List<Migration>> pluginMigrations;
    private final Map<String, Integer> currentVersionCache;
    private final boolean rollbackFailedMigrations;
    
    /**
     * Constructor for MigrationManager.
     *
     * @param plugin The SQL-Bridge plugin instance
     * @param connectionManager The connection manager
     */
    public MigrationManager(SqlBridgePlugin plugin, ConnectionManager connectionManager) {
        this.plugin = plugin;
        this.connectionManager = connectionManager;
        this.pluginMigrations = new ConcurrentHashMap<>();
        this.currentVersionCache = new ConcurrentHashMap<>();
        this.rollbackFailedMigrations = plugin.getPluginConfig().isRollbackFailedMigrationsEnabled();
        
        // Initialize schema version table
        initializeSchemaVersionTable();
    }
    
    /**
     * Initialize the schema version table.
     */
    private void initializeSchemaVersionTable() {
        try {
            DataSource dataSource = connectionManager.getMainConnection();
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                String createTableSQL = "CREATE TABLE IF NOT EXISTS " + SCHEMA_VERSION_TABLE + " (" +
                                       "plugin_name VARCHAR(100) NOT NULL, " +
                                       "version INT NOT NULL, " +
                                       "applied_at TIMESTAMP NOT NULL, " +
                                       "description VARCHAR(255), " +
                                       "PRIMARY KEY (plugin_name, version)" +
                                       ")";
                
                stmt.execute(createTableSQL);
                LogUtil.info("Schema version table initialized.");
            }
        } catch (SQLException e) {
            LogUtil.severe("Failed to initialize schema version table: " + e.getMessage());
            plugin.getErrorHandler().handleMigrationError(e);
        }
    }
    
    /**
     * Register migrations for a plugin.
     *
     * @param pluginName The name of the plugin
     * @param migrations The list of migrations to register
     */
    public void registerMigrations(String pluginName, List<Migration> migrations) {
        // Sort migrations by version
        migrations.sort(Comparator.comparingInt(Migration::getVersion));
        
        // Check for duplicate versions
        for (int i = 0; i < migrations.size() - 1; i++) {
            if (migrations.get(i).getVersion() == migrations.get(i + 1).getVersion()) {
                LogUtil.warning("Duplicate migration version " + migrations.get(i).getVersion() +
                               " for plugin " + pluginName);
            }
        }
        
        pluginMigrations.put(pluginName, migrations);
        LogUtil.info("Registered " + migrations.size() + " migrations for plugin: " + pluginName);
    }
    
    /**
     * Run all pending migrations for all registered plugins.
     *
     * @return The total number of migrations that were applied
     */
    public int runMigrations() {
        int totalApplied = 0;
        
        for (String pluginName : pluginMigrations.keySet()) {
            totalApplied += runMigrationsForPlugin(pluginName);
        }
        
        return totalApplied;
    }
    
    /**
     * Run all pending migrations for a specific plugin.
     *
     * @param pluginName The name of the plugin
     * @return The number of migrations that were applied
     */
    public int runMigrationsForPlugin(String pluginName) {
        List<Migration> migrations = pluginMigrations.get(pluginName);
        if (migrations == null || migrations.isEmpty()) {
            LogUtil.warning("No migrations registered for plugin: " + pluginName);
            return 0;
        }
        
        int currentVersion = getCurrentSchemaVersion(pluginName);
        int applied = 0;
        
        LogUtil.info("Running migrations for plugin " + pluginName + ", current version: " + currentVersion);
        
        DataSource dataSource = connectionManager.getMainConnection();
        try (Connection conn = dataSource.getConnection()) {
            // Disable auto-commit for transaction management
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            try {
                for (Migration migration : migrations) {
                    if (migration.getVersion() > currentVersion) {
                        LogUtil.info("Applying migration v" + migration.getVersion() + " for plugin " + pluginName +
                                   ": " + migration.getDescription());
                        
                        try {
                            // Apply the migration
                            migration.migrate(conn);
                            
                            // Record the new version
                            recordMigration(conn, pluginName, migration);
                            
                            applied++;
                            currentVersion = migration.getVersion();
                        } catch (SQLException e) {
                            LogUtil.severe("Migration v" + migration.getVersion() + " for plugin " + pluginName +
                                          " failed: " + e.getMessage());
                            
                            if (rollbackFailedMigrations) {
                                LogUtil.info("Rolling back migration v" + migration.getVersion());
                                try {
                                    migration.rollback(conn);
                                } catch (SQLException rollbackEx) {
                                    LogUtil.severe("Rollback of migration v" + migration.getVersion() +
                                                  " failed: " + rollbackEx.getMessage());
                                }
                            }
                            
                            // Rollback the transaction
                            conn.rollback();
                            throw e;
                        }
                    }
                }
                
                // Commit the transaction if we get here
                conn.commit();
                
                // Update the cache
                currentVersionCache.put(pluginName, currentVersion);
                
                LogUtil.info("Applied " + applied + " migrations for plugin " + pluginName +
                           ", new version: " + currentVersion);
            } finally {
                // Restore original auto-commit state
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            LogUtil.severe("Error running migrations for plugin " + pluginName + ": " + e.getMessage());
            plugin.getErrorHandler().handleMigrationError(e);
        }
        
        return applied;
    }
    
    /**
     * Record a migration in the schema version table.
     *
     * @param conn The database connection
     * @param pluginName The name of the plugin
     * @param migration The migration that was applied
     * @throws SQLException If an error occurs while recording the migration
     */
    private void recordMigration(Connection conn, String pluginName, Migration migration) throws SQLException {
        String sql = "INSERT INTO " + SCHEMA_VERSION_TABLE +
                    " (plugin_name, version, applied_at, description) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pluginName);
            stmt.setInt(2, migration.getVersion());
            stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            stmt.setString(4, migration.getDescription());
            stmt.executeUpdate();
        }
    }
    
    /**
     * Get the current schema version for a plugin.
     *
     * @param pluginName The name of the plugin
     * @return The current schema version, or 0 if no migrations have been applied
     */
    public int getCurrentSchemaVersion(String pluginName) {
        // Check cache first
        if (currentVersionCache.containsKey(pluginName)) {
            return currentVersionCache.get(pluginName);
        }
        
        int version = 0;
        DataSource dataSource = connectionManager.getMainConnection();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT MAX(version) FROM " + SCHEMA_VERSION_TABLE + " WHERE plugin_name = ?")) {
            
            stmt.setString(1, pluginName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    version = rs.getInt(1);
                }
            }
            
            // Cache the result
            currentVersionCache.put(pluginName, version);
        } catch (SQLException e) {
            LogUtil.warning("Error getting current schema version for plugin " + pluginName +
                           ": " + e.getMessage());
        }
        
        return version;
    }
    
    /**
     * Get all schema versions for a plugin.
     *
     * @param pluginName The name of the plugin
     * @return A list of schema versions
     */
    public List<SchemaVersion> getSchemaVersions(String pluginName) {
        List<SchemaVersion> versions = new ArrayList<>();
        DataSource dataSource = connectionManager.getMainConnection();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT version, applied_at, description FROM " + SCHEMA_VERSION_TABLE +
                 " WHERE plugin_name = ? ORDER BY version")) {
            
            stmt.setString(1, pluginName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SchemaVersion version = new SchemaVersion(
                        pluginName,
                        rs.getInt("version"),
                        rs.getTimestamp("applied_at"),
                        rs.getString("description")
                    );
                    versions.add(version);
                }
            }
        } catch (SQLException e) {
            LogUtil.warning("Error getting schema versions for plugin " + pluginName +
                           ": " + e.getMessage());
        }
        
        return versions;
    }
    
    /**
     * Get all schema versions for all plugins.
     *
     * @return A map of plugin names to lists of schema versions
     */
    public Map<String, List<SchemaVersion>> getAllSchemaVersions() {
        Map<String, List<SchemaVersion>> allVersions = new HashMap<>();
        DataSource dataSource = connectionManager.getMainConnection();
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT plugin_name, version, applied_at, description FROM " + SCHEMA_VERSION_TABLE +
                 " ORDER BY plugin_name, version")) {
            
            String currentPlugin = null;
            List<SchemaVersion> versions = null;
            
            while (rs.next()) {
                String pluginName = rs.getString("plugin_name");
                
                if (!pluginName.equals(currentPlugin)) {
                    if (currentPlugin != null) {
                        allVersions.put(currentPlugin, versions);
                    }
                    
                    currentPlugin = pluginName;
                    versions = new ArrayList<>();
                }
                
                SchemaVersion version = new SchemaVersion(
                    pluginName,
                    rs.getInt("version"),
                    rs.getTimestamp("applied_at"),
                    rs.getString("description")
                );
                
                versions.add(version);
            }
            
            if (currentPlugin != null) {
                allVersions.put(currentPlugin, versions);
            }
        } catch (SQLException e) {
            LogUtil.warning("Error getting all schema versions: " + e.getMessage());
            plugin.getLogger().log(Level.WARNING, "SQL exception", e);
        }
        
        return allVersions;
    }
    
    /**
     * Clear the schema version cache.
     */
    public void clearCache() {
        currentVersionCache.clear();
        LogUtil.info("Schema version cache cleared.");
    }
}