// ./Example-Plugin/src/main/java/com/minecraft/example/ExamplePlugin.java
package com.minecraft.example;

import com.minecraft.core.CoreAPI;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.command.AdminCommand;
import com.minecraft.example.command.StatsCommand;
import com.minecraft.example.database.StatsDAO;
import com.minecraft.example.listener.PlayerListener;
import com.minecraft.example.model.PlayerStats;
import com.minecraft.example.service.DefaultStatsService;
import com.minecraft.example.service.StatsService;
import com.minecraft.sql.api.Database;
import com.minecraft.sql.api.DatabaseService;
import com.minecraft.sql.api.migration.Migration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ExamplePlugin extends JavaPlugin {
    
    private DatabaseService databaseService;
    private Database database;
    private StatsDAO statsDAO;
    private DefaultStatsService statsService;
    
    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Initialize Core-Utils logging
        LogUtil.init(this);
        LogUtil.setDebugMode(getConfig().getBoolean("debug", false));
        LogUtil.info("Initializing Example Plugin...");
        
        // Initialize database connection through SQL-Bridge
        initializeDatabase();
        
        // Initialize services
        initializeServices();
        
        // Register commands using Core-Utils command framework
        registerCommands();
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        LogUtil.info("Example Plugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Unregister services
        if (statsService != null) {
            CoreAPI.Services.unregister(StatsService.class);
        }
        
        LogUtil.info("Example Plugin has been disabled.");
    }
    
    private void initializeDatabase() {
        try {
            // Get database service from SQL-Bridge (via Core-Utils service registry)
            databaseService = CoreAPI.Services.get(DatabaseService.class);
            
            if (databaseService == null) {
                LogUtil.severe("Failed to get DatabaseService! Is SQL-Bridge enabled?");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Get a database instance for our plugin
            database = databaseService.getDatabaseForPlugin(this);
            
            // Register and run migrations
            List<Migration> migrations = new ArrayList<>();
            
            // Migration to create initial tables
            migrations.add(new Migration() {
                @Override
                public int getVersion() {
                    return 1;
                }
                
                @Override
                public String getDescription() {
                    return "Create player_stats table";
                }
                
                @Override
                public void migrate(Connection connection) throws SQLException {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(
                            "CREATE TABLE IF NOT EXISTS player_stats (" +
                            "  uuid VARCHAR(36) PRIMARY KEY, " +
                            "  name VARCHAR(16) NOT NULL, " +
                            "  first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "  last_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "  playtime_seconds BIGINT DEFAULT 0, " +
                            "  login_count INT DEFAULT 1, " +
                            "  blocks_broken INT DEFAULT 0, " +
                            "  blocks_placed INT DEFAULT 0" +
                            ")"
                        );
                    }
                }
                
                @Override
                public void rollback(Connection connection) throws SQLException {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("DROP TABLE IF EXISTS player_stats");
                    }
                }
            });
            
            // Register migrations with the database service
            databaseService.registerMigrations(this, migrations);
            
            // Run migrations
            int appliedMigrations = databaseService.runMigrationsSafe(this);
            if (appliedMigrations > 0) {
                LogUtil.info("Applied " + appliedMigrations + " database migrations.");
            }
            
            // Initialize the DAO
            statsDAO = new StatsDAO(database, this);
            
        } catch (Exception e) {
            LogUtil.severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    private void initializeServices() {
        // Create and register our stats service
        statsService = new DefaultStatsService(this, statsDAO);
        CoreAPI.Services.register(StatsService.class, statsService);
        LogUtil.info("Registered StatsService.");
    }
    
    private void registerCommands() {
        // Register commands using Core-Utils command framework
        CoreAPI.Commands.register(new StatsCommand(this));
        CoreAPI.Commands.register(new AdminCommand(this));
        LogUtil.info("Registered commands.");
    }
    
    /**
     * Reload the plugin configuration
     */
    public void reloadPluginConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        
        // Update debug mode
        boolean debug = config.getBoolean("debug", false);
        LogUtil.setDebugMode(debug);
        LogUtil.info("Debug mode: " + (debug ? "enabled" : "disabled"));
        
        // Update other configuration options
        // ...
    }
    
    // Getter methods
    public Database getDatabase() {
        return database;
    }
    
    public StatsDAO getStatsDAO() {
        return statsDAO;
    }
    
    public StatsService getStatsService() {
        return statsService;
    }
}