// ./src/main/java/com/minecraft/example/ExamplePlugin.java
package com.minecraft.example;

import com.minecraft.core.CorePlugin;
import com.minecraft.core.api.service.ServiceRegistry;
import com.minecraft.core.command.CommandRegistry;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.command.ExampleCommand;
import com.minecraft.example.command.PlayerStatsCommand;
import com.minecraft.example.config.ConfigManager;
import com.minecraft.example.listener.PlayerListener;
import com.minecraft.example.model.PlayerStats;
import com.minecraft.example.service.DefaultStatsService;
import com.minecraft.example.service.StatsService;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.DatabaseService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

/**
 * Main class for Example-Plugin
 * Demonstrates Core-Utils and SQL-Bridge functionality
 */
public class ExamplePlugin extends JavaPlugin {
    
    private ConfigManager configManager;
    private Database database;
    private StatsService statsService;
    
    @Override
    public void onEnable() {
        // Initialize LogUtil
        LogUtil.init(this);
        LogUtil.info("Initializing Example-Plugin...");
        
        // Check for required plugins
        if (!checkRequiredPlugins()) {
            return;
        }
        
        // Load configuration
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Initialize database
        initializeDatabase();
        
        // Register services
        registerServices();
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        LogUtil.info("Example-Plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Unregister services
        if (statsService != null) {
            ServiceRegistry.unregister(StatsService.class);
            LogUtil.info("Unregistered StatsService");
        }
        
        LogUtil.info("Example-Plugin disabled successfully!");
    }
    
    /**
     * Check if required plugins are available
     * @return true if all required plugins are available
     */
    private boolean checkRequiredPlugins() {
        // Check for Core-Utils
        Plugin corePlugin = getServer().getPluginManager().getPlugin("CoreUtils");
        if (!(corePlugin instanceof CorePlugin)) {
            LogUtil.severe("Core-Utils not found! This plugin requires Core-Utils to function.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        
        // Check for SQL-Bridge
        Plugin sqlPlugin = getServer().getPluginManager().getPlugin("SQL-Bridge");
        if (sqlPlugin == null) {
            LogUtil.severe("SQL-Bridge not found! This plugin requires SQL-Bridge to function.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        
        return true;
    }
    
    /**
     * Initialize database connection and tables
     */
    private void initializeDatabase() {
        try {
            // Get database service from SQL-Bridge
            DatabaseService databaseService = ServiceRegistry.getService(DatabaseService.class);
            if (databaseService == null) {
                LogUtil.severe("Could not find DatabaseService! Is SQL-Bridge enabled?");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Get database connection for this plugin
            database = databaseService.getDatabaseForPlugin(this);
            LogUtil.info("Database connection established");
            
            // Create tables
            createTables();
            
        } catch (Exception e) {
            LogUtil.severe("Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    /**
     * Create database tables
     */
    private void createTables() {
        try {
            // Create player_stats table if it doesn't exist
            database.update(
                "CREATE TABLE IF NOT EXISTS player_stats (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  uuid VARCHAR(36) NOT NULL UNIQUE, " +
                "  name VARCHAR(16) NOT NULL, " +
                "  kills INTEGER NOT NULL DEFAULT 0, " +
                "  deaths INTEGER NOT NULL DEFAULT 0, " +
                "  blocks_placed INTEGER NOT NULL DEFAULT 0, " +
                "  blocks_broken INTEGER NOT NULL DEFAULT 0, " +
                "  time_played INTEGER NOT NULL DEFAULT 0, " +
                "  last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            
            LogUtil.info("Database tables initialized");
        } catch (SQLException e) {
            LogUtil.severe("Failed to create tables: " + e.getMessage());
        }
    }
    
    /**
     * Register plugin services
     */
    private void registerServices() {
        // Create and register StatsService
        statsService = new DefaultStatsService(this, database);
        ServiceRegistry.register(StatsService.class, statsService);
        LogUtil.info("Registered StatsService");
    }
    
    /**
     * Register plugin commands
     */
    private void registerCommands() {
        // Get CorePlugin instance
        CorePlugin corePlugin = (CorePlugin) getServer().getPluginManager().getPlugin("CoreUtils");
        CommandRegistry commandRegistry = corePlugin.getCommandRegistry();
        
        // Register commands
        commandRegistry.registerCommand(new ExampleCommand(this));
        commandRegistry.registerCommand(new PlayerStatsCommand(this));
        
        LogUtil.info("Registered commands");
    }
    
    /**
     * Register event listeners
     */
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        LogUtil.info("Registered event listeners");
    }
    
    /**
     * Get the configuration manager
     * @return ConfigManager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Get the database instance
     * @return Database instance
     */
    public Database getDatabase() {
        return database;
    }
    
    /**
     * Get the stats service
     * @return StatsService instance
     */
    public StatsService getStatsService() {
        return statsService;
    }
}