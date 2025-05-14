// ./Example-Plugin/src/main/java/com/minecraft/example/PlayerStatsPlugin.java
package com.minecraft.example;

import com.minecraft.core.CorePlugin;
import com.minecraft.core.api.service.ServiceRegistry;
import com.minecraft.core.command.CommandRegistry;
import com.minecraft.core.config.ConfigManager;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.api.AchievementService;
import com.minecraft.example.api.LeaderboardService;
import com.minecraft.example.api.StatsService;
import com.minecraft.example.command.AdminCommand;
import com.minecraft.example.command.LeaderboardCommand;
import com.minecraft.example.command.StatsCommand;
import com.minecraft.example.config.MessageConfig;
import com.minecraft.example.config.PluginConfig;
import com.minecraft.example.database.DatabaseManager;
import com.minecraft.example.listener.AchievementListener;
import com.minecraft.example.listener.PlayerListener;
import com.minecraft.example.listener.StatTrackerListener;
import com.minecraft.example.service.DefaultAchievementService;
import com.minecraft.example.service.DefaultLeaderboardService;
import com.minecraft.example.service.DefaultStatsService;
import com.minecraft.sqlbridge.api.DatabaseService;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class PlayerStatsPlugin extends JavaPlugin {
    
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PluginConfig pluginConfig;
    private MessageConfig messageConfig;
    
    // Service implementations
    private DefaultStatsService statsService;
    private DefaultAchievementService achievementService;
    private DefaultLeaderboardService leaderboardService;
    
    @Override
    public void onEnable() {
        // Initialize LogUtil
        LogUtil.init(this);
        LogUtil.info("Initializing PlayerStats plugin...");
        
        // Check for dependencies
        if (!checkDependencies()) {
            return;
        }
        
        // Load configurations
        this.configManager = new ConfigManager(this);
        this.pluginConfig = new PluginConfig(this, configManager);
        this.messageConfig = new MessageConfig(this, configManager);
        
        // Initialize database
        initDatabase();
        
        // Register services
        registerServices();
        
        // Register commands
        registerCommands();
        
        // Register event listeners
        registerEventListeners();
        
        LogUtil.info("PlayerStats plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Unregister services
        if (statsService != null) {
            ServiceRegistry.unregister(StatsService.class);
        }
        if (achievementService != null) {
            ServiceRegistry.unregister(AchievementService.class);
        }
        if (leaderboardService != null) {
            ServiceRegistry.unregister(LeaderboardService.class);
        }
        
        // Close database connections
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        
        LogUtil.info("PlayerStats plugin disabled.");
    }
    
    private boolean checkDependencies() {
        // Check for Core-Utils
        Plugin corePlugin = getServer().getPluginManager().getPlugin("CoreUtils");
        if (corePlugin == null || !(corePlugin instanceof CorePlugin)) {
            LogUtil.severe("CoreUtils not found! This plugin requires CoreUtils to function.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        
        // Check for SQL-Bridge
        Plugin sqlBridgePlugin = getServer().getPluginManager().getPlugin("Sql-Bridge");
        if (sqlBridgePlugin == null) {
            LogUtil.severe("Sql-Bridge not found! This plugin requires Sql-Bridge to function.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        
        return true;
    }
    
    private void initDatabase() {
        try {
            // Get the database service from SQL-Bridge
            DatabaseService databaseService = ServiceRegistry.getService(DatabaseService.class);
            if (databaseService == null) {
                LogUtil.severe("Failed to get DatabaseService from Sql-Bridge.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Create our database manager
            this.databaseManager = new DatabaseManager(this, databaseService);
            this.databaseManager.initialize();
            
            LogUtil.info("Database initialized successfully.");
        } catch (Exception e) {
            LogUtil.severe("Failed to initialize database: " + e.getMessage());
            getServer().getLogger().log(Level.SEVERE, "Database initialization error", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    private void registerServices() {
        // Create service implementations
        this.statsService = new DefaultStatsService(this, databaseManager);
        this.achievementService = new DefaultAchievementService(this, databaseManager, statsService);
        this.leaderboardService = new DefaultLeaderboardService(this, databaseManager, statsService);
        
        // Register with the service registry
        ServiceRegistry.register(StatsService.class, statsService);
        ServiceRegistry.register(AchievementService.class, achievementService);
        ServiceRegistry.register(LeaderboardService.class, leaderboardService);
        
        LogUtil.info("Services registered successfully.");
    }
    
    private void registerCommands() {
        try {
            // Get CorePlugin instance
            Plugin corePlugin = getServer().getPluginManager().getPlugin("CoreUtils");
            CorePlugin core = (CorePlugin) corePlugin;
            
            // Get command registry
            CommandRegistry commandRegistry = core.getCommandRegistry();
            
            // Register commands
            commandRegistry.registerCommand(new StatsCommand(this, statsService));
            commandRegistry.registerCommand(new AdminCommand(this, statsService, achievementService, leaderboardService));
            commandRegistry.registerCommand(new LeaderboardCommand(this, leaderboardService));
            
            LogUtil.info("Commands registered successfully.");
        } catch (Exception e) {
            LogUtil.severe("Failed to register commands: " + e.getMessage());
            getServer().getLogger().log(Level.SEVERE, "Command registration error", e);
        }
    }
    
    private void registerEventListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this, statsService), this);
        getServer().getPluginManager().registerEvents(new StatTrackerListener(this, statsService), this);
        getServer().getPluginManager().registerEvents(new AchievementListener(this, achievementService, statsService), this);
        
        LogUtil.info("Event listeners registered successfully.");
    }
    
    // Getter methods
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }
    
    public MessageConfig getMessageConfig() {
        return messageConfig;
    }
    
    public StatsService getStatsService() {
        return statsService;
    }
    
    public AchievementService getAchievementService() {
        return achievementService;
    }
    
    public LeaderboardService getLeaderboardService() {
        return leaderboardService;
    }
}