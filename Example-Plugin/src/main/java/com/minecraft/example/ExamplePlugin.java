// ./Example-Plugin/src/main/java/com/minecraft/example/ExamplePlugin.java
package com.minecraft.example;

import com.minecraft.core.api.CoreAPI;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.config.ConfigManager;
import com.minecraft.example.core.commands.AchievementCommand;
import com.minecraft.example.core.commands.ExampleCommand;
import com.minecraft.example.core.commands.StatsCommand;
import com.minecraft.example.core.services.AchievementService;
import com.minecraft.example.core.services.NotificationService;
import com.minecraft.example.core.services.StatsService;
import com.minecraft.example.core.services.impl.ChatNotificationService;
import com.minecraft.example.core.services.impl.DefaultAchievementService;
import com.minecraft.example.core.services.impl.DefaultStatsService;
import com.minecraft.example.integration.features.AchievementManagerFeature;
import com.minecraft.example.integration.features.AdminToolsFeature;
import com.minecraft.example.integration.features.StatTrackerFeature;
import com.minecraft.example.sql.DatabaseInitializer;
import com.minecraft.example.sql.dao.AchievementDAO;
import com.minecraft.example.sql.dao.PlayerAchievementDAO;
import com.minecraft.example.sql.dao.PlayerDAO;
import com.minecraft.example.sql.dao.PlayerStatsDAO;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.DatabaseService;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main Example Plugin class demonstrating both Core-Utils and SQL-Bridge integration.
 * This plugin serves as a reference implementation for other plugin developers.
 */
public class ExamplePlugin extends JavaPlugin {
    
    private ConfigManager configManager;
    private Database database;
    
    // DAOs
    private PlayerDAO playerDAO;
    private PlayerStatsDAO playerStatsDAO;
    private AchievementDAO achievementDAO;
    private PlayerAchievementDAO playerAchievementDAO;
    
    // Features
    private StatTrackerFeature statTrackerFeature;
    private AchievementManagerFeature achievementManagerFeature;
    private AdminToolsFeature adminToolsFeature;
    
    @Override
    public void onEnable() {
        // Initialize configuration
        this.configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Initialize logging with Core-Utils
        LogUtil.init(this);
        LogUtil.info("Initializing Example Plugin");
        
        // Check for Core-Utils and SQL-Bridge
        if (!checkDependencies()) {
            LogUtil.severe("Required dependencies not found. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize database if enabled
        if (getConfig().getBoolean("features.database_integration", true)) {
            initializeDatabase();
        }
        
        // Register services with Core-Utils
        registerServices();
        
        // Register commands with Core-Utils
        registerCommands();
        
        // Initialize features
        initializeFeatures();
        
        LogUtil.info("Example Plugin enabled successfully");
    }
    
    @Override
    public void onDisable() {
        // Unregister services
        unregisterServices();
        
        // Clean up features
        if (statTrackerFeature != null) {
            statTrackerFeature.disable();
        }
        
        if (achievementManagerFeature != null) {
            achievementManagerFeature.disable();
        }
        
        if (adminToolsFeature != null) {
            adminToolsFeature.disable();
        }
        
        LogUtil.info("Example Plugin disabled successfully");
    }
    
    /**
     * Verifies that required dependencies are available.
     */
    private boolean checkDependencies() {
        Plugin coreUtils = getServer().getPluginManager().getPlugin("Core-Utils");
        Plugin sqlBridge = getServer().getPluginManager().getPlugin("Sql-Bridge");
        
        if (coreUtils == null) {
            LogUtil.severe("Core-Utils not found! This plugin requires Core-Utils to function.");
            return false;
        }
        
        if (sqlBridge == null && getConfig().getBoolean("features.database_integration", true)) {
            LogUtil.warning("SQL-Bridge not found! Database features will be disabled.");
            getConfig().set("features.database_integration", false);
            return true;
        }
        
        return true;
    }
    
    /**
     * Initializes database connection and DAOs.
     */
    private void initializeDatabase() {
        try {
            // Get the database service from SQL-Bridge via Core-Utils service registry
            DatabaseService databaseService = CoreAPI.Services.get(DatabaseService.class);
            
            if (databaseService == null) {
                LogUtil.severe("Database service not available. Database features will be disabled.");
                getConfig().set("features.database_integration", false);
                return;
            }
            
            // Get a database instance for this plugin
            this.database = databaseService.getDatabaseForPlugin(this);
            
            // Initialize database schema
            DatabaseInitializer initializer = new DatabaseInitializer(this, database);
            initializer.initialize();
            
            // Initialize DAOs
            this.playerDAO = new PlayerDAO(database, this);
            this.playerStatsDAO = new PlayerStatsDAO(database, this);
            this.achievementDAO = new AchievementDAO(database, this);
            this.playerAchievementDAO = new PlayerAchievementDAO(database, this);
            
            LogUtil.info("Database initialized successfully");
        } catch (Exception e) {
            LogUtil.severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            getConfig().set("features.database_integration", false);
        }
    }
    
    /**
     * Registers service implementations with Core-Utils service registry.
     */
    private void registerServices() {
        // Create service implementations
        NotificationService notificationService = new ChatNotificationService(this);
        StatsService statsService = new DefaultStatsService(this);
        AchievementService achievementService = new DefaultAchievementService(this);
        
        // Register services with Core-Utils
        CoreAPI.Services.register(NotificationService.class, notificationService);
        CoreAPI.Services.register(StatsService.class, statsService);
        CoreAPI.Services.register(AchievementService.class, achievementService);
        
        LogUtil.info("Services registered successfully");
    }
    
    /**
     * Unregisters services from Core-Utils service registry.
     */
    private void unregisterServices() {
        // Unregister services when the plugin is disabled
        CoreAPI.Services.unregister(NotificationService.class);
        CoreAPI.Services.unregister(StatsService.class);
        CoreAPI.Services.unregister(AchievementService.class);
        
        LogUtil.info("Services unregistered successfully");
    }
    
    /**
     * Registers commands with Core-Utils command framework.
     */
    private void registerCommands() {
        // Create command instances
        ExampleCommand exampleCommand = new ExampleCommand(this);
        StatsCommand statsCommand = new StatsCommand(this);
        AchievementCommand achievementCommand = new AchievementCommand(this);
        
        // Register commands with Core-Utils
        CoreAPI.Commands.register(exampleCommand);
        CoreAPI.Commands.register(statsCommand);
        CoreAPI.Commands.register(achievementCommand);
        
        LogUtil.info("Commands registered successfully");
    }
    
    /**
     * Initializes features based on configuration.
     */
    private void initializeFeatures() {
        // Initialize stat tracker feature
        if (getConfig().getBoolean("features.stat_tracking", true)) {
            this.statTrackerFeature = new StatTrackerFeature(this);
            statTrackerFeature.enable();
            LogUtil.info("Stat tracking feature enabled");
        }
        
        // Initialize achievement manager feature
        if (getConfig().getBoolean("features.achievements", true)) {
            this.achievementManagerFeature = new AchievementManagerFeature(this);
            achievementManagerFeature.enable();
            LogUtil.info("Achievement manager feature enabled");
        }
        
        // Initialize admin tools feature
        if (getConfig().getBoolean("features.admin_tools", true)) {
            this.adminToolsFeature = new AdminToolsFeature(this);
            adminToolsFeature.enable();
            LogUtil.info("Admin tools feature enabled");
        }
    }
    
    /**
     * Gets the configuration manager.
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Gets the database instance.
     */
    public Database getDatabase() {
        return database;
    }
    
    /**
     * Gets the player DAO.
     */
    public PlayerDAO getPlayerDAO() {
        return playerDAO;
    }
    
    /**
     * Gets the player stats DAO.
     */
    public PlayerStatsDAO getPlayerStatsDAO() {
        return playerStatsDAO;
    }
    
    /**
     * Gets the achievement DAO.
     */
    public AchievementDAO getAchievementDAO() {
        return achievementDAO;
    }
    
    /**
     * Gets the player achievement DAO.
     */
    public PlayerAchievementDAO getPlayerAchievementDAO() {
        return playerAchievementDAO;
    }
    
    /**
     * Checks if database features are enabled.
     */
    public boolean isDatabaseEnabled() {
        return getConfig().getBoolean("features.database_integration", true);
    }
}