// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/SqlBridgePlugin.java
package com.minecraft.sqlbridge;

import com.minecraft.core.CorePlugin;
import com.minecraft.core.api.service.ServiceRegistry;
import com.minecraft.core.command.CommandRegistry;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.api.DatabaseService;
import com.minecraft.sqlbridge.command.SqlBridgeCommand;
import com.minecraft.sqlbridge.config.SqlBridgeConfig;
import com.minecraft.sqlbridge.connection.ConnectionManager;
import com.minecraft.sqlbridge.error.ErrorHandler;
import com.minecraft.sqlbridge.impl.DefaultDatabaseService;
import com.minecraft.sqlbridge.migration.MigrationManager;
import com.minecraft.sqlbridge.monitoring.PerformanceMonitor;
import com.minecraft.sqlbridge.bungee.BungeeSupport;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main class for the SQL-Bridge plugin.
 */
public class SqlBridgePlugin extends JavaPlugin {
    
    private SqlBridgeConfig sqlBridgeConfig;
    private ConnectionManager connectionManager;
    private ErrorHandler errorHandler;
    private MigrationManager migrationManager;
    private PerformanceMonitor performanceMonitor;
    private DefaultDatabaseService databaseService;
    private BungeeSupport bungeeSupport;
    private boolean bungeeEnabled = false;
    
    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Initialize plugin components
        initializePlugin();
        
        // Register the database service with Core-Utils
        registerDatabaseService();
        
        // Register commands
        registerCommands();
        
        // Run migrations if configured
        runMigrations();
        
        getLogger().info("SQL-Bridge has been enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Clean up resources
        if (databaseService != null) {
            ServiceRegistry.unregister(DatabaseService.class);
        }
        
        if (connectionManager != null) {
            connectionManager.close();
        }
        
        if (performanceMonitor != null) {
            performanceMonitor.shutdown();
        }
        
        if (bungeeSupport != null && bungeeEnabled) {
            bungeeSupport.disable();
        }
        
        getLogger().info("SQL-Bridge has been disabled.");
    }
    
    /**
     * Initialize the plugin components.
     */
    private void initializePlugin() {
        // Set up logging
        LogUtil.init(this);
        
        // Load configuration
        sqlBridgeConfig = new SqlBridgeConfig(this);
        
        // Initialize error handler
        errorHandler = new ErrorHandler(this);
        
        try {
            // Initialize database connection manager based on the configuration type
            String dbType = sqlBridgeConfig.getDatabaseType().toLowerCase();
            connectionManager = new ConnectionManager(this, sqlBridgeConfig);
            
            // Initialize migration manager
            migrationManager = new MigrationManager(this, connectionManager);
            
            // Initialize performance monitor if enabled
            if (sqlBridgeConfig.isMonitoringEnabled()) {
                performanceMonitor = new PerformanceMonitor(this, connectionManager);
                performanceMonitor.start();
            }
            
            // Initialize database service
            databaseService = new DefaultDatabaseService(this, connectionManager);
            
            // Initialize BungeeSupport if enabled
            if (sqlBridgeConfig.isBungeeEnabled()) {
                bungeeSupport = new BungeeSupport(this, connectionManager);
                bungeeEnabled = bungeeSupport.enable();
            }
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize SQL-Bridge: " + e.getMessage(), e);
            errorHandler.handleCriticalError(e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    /**
     * Register the database service with the CoreUtils service registry.
     */
    private void registerDatabaseService() {
        if (databaseService != null) {
            try {
                // Register the database service with Core-Utils
                ServiceRegistry.register(DatabaseService.class, databaseService);
                getLogger().info("Database service registered successfully.");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to register database service: " + e.getMessage(), e);
                errorHandler.handleServiceRegistrationError(e);
            }
        }
    }
    
    /**
     * Register plugin commands with Core-Utils command registry.
     */
    private void registerCommands() {
        try {
            // Get CorePlugin instance
            Plugin corePlugin = getServer().getPluginManager().getPlugin("CoreUtils");
            if (corePlugin instanceof CorePlugin) {
                CorePlugin core = (CorePlugin) corePlugin;
                
                // Get command registry
                CommandRegistry commandRegistry = core.getCommandRegistry();
                
                // Register SQL-Bridge command
                SqlBridgeCommand sqlBridgeCommand = new SqlBridgeCommand(this);
                commandRegistry.registerCommand(sqlBridgeCommand);
                
                getLogger().info("SQL-Bridge commands registered successfully.");
            } else {
                getLogger().warning("CoreUtils plugin not found or is not an instance of CorePlugin. Commands will not be registered.");
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to register commands: " + e.getMessage(), e);
            errorHandler.handleCommandRegistrationError(e);
        }
    }
    
    /**
     * Run database migrations if enabled in configuration.
     */
    private void runMigrations() {
        if (sqlBridgeConfig.isAutoMigrateEnabled() && migrationManager != null) {
            try {
                migrationManager.runMigrations();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to run migrations: " + e.getMessage(), e);
                errorHandler.handleMigrationError(e);
            }
        }
    }
    
    /**
     * Get the plugin's configuration manager.
     * 
     * @return The SqlBridgeConfig instance
     */
    public SqlBridgeConfig getPluginConfig() {
        return sqlBridgeConfig;
    }
    
    /**
     * Get the connection manager.
     * 
     * @return The ConnectionManager instance
     */
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }
    
    /**
     * Get the error handler.
     * 
     * @return The ErrorHandler instance
     */
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }
    
    /**
     * Get the migration manager.
     * 
     * @return The MigrationManager instance
     */
    public MigrationManager getMigrationManager() {
        return migrationManager;
    }
    
    /**
     * Get the performance monitor.
     * 
     * @return The PerformanceMonitor instance
     */
    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
    
    /**
     * Get the database service.
     * 
     * @return The DatabaseService instance
     */
    public DatabaseService getDatabaseService() {
        return databaseService;
    }
    
    /**
     * Get the BungeeSupport instance.
     * 
     * @return The BungeeSupport instance
     */
    public BungeeSupport getBungeeSupport() {
        return bungeeSupport;
    }
}