// ./Example-Plugin/src/main/java/com/example/exampleplugin/ExamplePlugin.java
package com.example.exampleplugin;

import com.example.exampleplugin.database.DatabaseManager;
import com.minecraft.core.CorePlugin;
import com.minecraft.core.api.service.ServiceLocator;
import com.minecraft.core.command.CommandRegistry;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.DatabaseService;
import com.minecraft.sqlbridge.migration.Migration;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Example plugin demonstrating the use of SQL-Bridge.
 */
public class ExamplePlugin extends JavaPlugin {

    private DatabaseService databaseService;
    private Database database;
    private PlayerDataManager playerDataManager;
    private DatabaseManager databaseManager;
    private ExampleService exampleService;
    private Plugin sqlBridge;
    private Plugin coreUtils;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Initialize core dependencies
        initializeDependencies();
        
        // Initialize example service
        exampleService = new DefaultExampleService(this);
        
        // Initialize database
        initializeDatabase();
        
        // Register commands
        registerCommands();
        
        getLogger().info("ExamplePlugin has been enabled!");
    }
    
    /**
     * Initialize core dependencies
     */
    private void initializeDependencies() {
        // Get CoreUtils plugin
        coreUtils = Bukkit.getPluginManager().getPlugin("CoreUtils");
        if (coreUtils == null) {
            getLogger().severe("CoreUtils not found! This plugin requires CoreUtils to function.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Get SQL-Bridge plugin
        sqlBridge = Bukkit.getPluginManager().getPlugin("SqlBridge");
        if (sqlBridge == null) {
            getLogger().warning("SqlBridge not found! Database functionality will be disabled.");
        }
    }
    
    /**
     * Initialize database connection and features
     */
    private void initializeDatabase() {
        if (sqlBridge != null) {
            // Get the DatabaseService from SQL-Bridge
            databaseService = ServiceLocator.getService(DatabaseService.class);
            
            if (databaseService == null) {
                getLogger().severe("Could not find DatabaseService. Database functionality will be disabled.");
                return;
            }
            
            // Get a database connection for this plugin
            database = databaseService.getDatabaseForPlugin(getName());
            
            // Register migrations
            List<Migration> migrations = Arrays.asList(
                new CreateTablesV1Migration(),
                new AddIndexesV2Migration()
            );
            
            databaseService.registerMigrations(getName(), migrations);
            
            // Run migrations
            try {
                int appliedCount = databaseService.runMigrations(getName());
                getLogger().info("Applied " + appliedCount + " migrations.");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to run migrations", e);
                return;
            }
            
            // Initialize player data manager
            playerDataManager = new PlayerDataManager(this, database);
            
            // Initialize database manager
            databaseManager = new DatabaseManager(this);
            
            // Register events
            getServer().getPluginManager().registerEvents(new PlayerListener(this, playerDataManager), this);
        } else {
            getLogger().warning("SQL-Bridge not available. Database features are disabled.");
        }
    }
    
    /**
     * Register plugin commands
     */
    private void registerCommands() {
        // If using Core-Utils command framework
        if (coreUtils instanceof CorePlugin) {
            CommandRegistry commandRegistry = ((CorePlugin) coreUtils).getCommandRegistry();
            
            // Register example command
            commandRegistry.registerCommand(new ExampleCommand(this));
            
            // Register database command if database is available
            if (databaseManager != null && databaseManager.isInitialized()) {
                commandRegistry.registerCommand(new DatabaseCommand(this, databaseManager));
            }
        } else {
            getLogger().warning("CoreUtils command registry not available. Commands will not be registered.");
        }
    }

    @Override
    public void onDisable() {
        // Save any pending data
        if (playerDataManager != null) {
            playerDataManager.saveAllPlayers();
        }
        
        getLogger().info("ExamplePlugin has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (command.getName().equalsIgnoreCase("stats")) {
            // Handle stats command
            handleStatsCommand(player, args);
            return true;
        } else if (command.getName().equalsIgnoreCase("resetstats")) {
            // Handle resetstats command
            handleResetStatsCommand(player);
            return true;
        }
        
        return false;
    }
    
    /**
     * Handle the /stats command.
     *
     * @param player The player who executed the command
     * @param args The command arguments
     */
    private void handleStatsCommand(Player player, String[] args) {
        PlayerData playerData = playerDataManager.getPlayerData(player.getUniqueId());
        
        if (playerData != null) {
            player.sendMessage("§6=== Your Stats ===");
            player.sendMessage("§7Blocks Broken: §a" + playerData.getBlocksBroken());
            player.sendMessage("§7Blocks Placed: §a" + playerData.getBlocksPlaced());
            player.sendMessage("§7Mobs Killed: §a" + playerData.getMobsKilled());
            player.sendMessage("§7Deaths: §a" + playerData.getDeaths());
            player.sendMessage("§7Last Seen: §a" + new java.util.Date(playerData.getLastSeen()));
        } else {
            player.sendMessage("§cNo stats available. Please reconnect.");
        }
    }
    
    /**
     * Handle the /resetstats command.
     *
     * @param player The player who executed the command
     */
    private void handleResetStatsCommand(Player player) {
        // Check permission
        if (!player.hasPermission("exampleplugin.resetstats")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return;
        }
        
        // Reset stats asynchronously
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                playerDataManager.resetPlayerStats(player.getUniqueId());
                player.sendMessage("§aYour stats have been reset.");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to reset stats for " + player.getName(), e);
                player.sendMessage("§cFailed to reset stats. Please try again later.");
            }
        });
    }
    
    /**
     * Get the database instance.
     *
     * @return The database instance
     */
    public Database getDatabase() {
        return database;
    }
    
    /**
     * Get the player data manager.
     *
     * @return The player data manager
     */
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    /**
     * Get the database manager.
     *
     * @return The database manager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    /**
     * Get the example service.
     *
     * @return The example service
     */
    public ExampleService getService() {
        return exampleService;
    }
    
    /**
     * Get the SQL-Bridge plugin reference.
     *
     * @return The SQL-Bridge plugin reference, or null if not available
     */
    public Plugin getSqlBridge() {
        return sqlBridge;
    }
    
    /**
     * Get the Core-Utils plugin reference.
     *
     * @return The Core-Utils plugin reference
     */
    public Plugin getCoreUtils() {
        return coreUtils;
    }
    
    /**
     * Migration to create initial tables.
     */
    private class CreateTablesV1Migration implements Migration {
        
        @Override
        public int getVersion() {
            return 1;
        }
        
        @Override
        public String getDescription() {
            return "Create initial tables";
        }
        
        @Override
        public void migrate(Connection connection) throws SQLException {
            try (Statement stmt = connection.createStatement()) {
                // Create player_data table
                String createPlayerDataTable = 
                    "CREATE TABLE player_data (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "name VARCHAR(16) NOT NULL," +
                    "blocks_broken INT NOT NULL DEFAULT 0," +
                    "blocks_placed INT NOT NULL DEFAULT 0," +
                    "mobs_killed INT NOT NULL DEFAULT 0," +
                    "deaths INT NOT NULL DEFAULT 0," +
                    "last_seen BIGINT NOT NULL," +
                    "first_join BIGINT NOT NULL" +
                    ")";
                
                stmt.executeUpdate(createPlayerDataTable);
            }
        }
        
        @Override
        public void rollback(Connection connection) throws SQLException {
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DROP TABLE IF EXISTS player_data");
            }
        }
    }
    
    /**
     * Migration to add indexes to tables.
     */
    private class AddIndexesV2Migration implements Migration {
        
        @Override
        public int getVersion() {
            return 2;
        }
        
        @Override
        public String getDescription() {
            return "Add indexes to tables";
        }
        
        @Override
        public void migrate(Connection connection) throws SQLException {
            try (Statement stmt = connection.createStatement()) {
                // Add index on player_data.name
                String createNameIndex = 
                    "CREATE INDEX idx_player_data_name ON player_data (name)";
                
                stmt.executeUpdate(createNameIndex);
                
                // Add index on player_data.last_seen
                String createLastSeenIndex = 
                    "CREATE INDEX idx_player_data_last_seen ON player_data (last_seen)";
                
                stmt.executeUpdate(createLastSeenIndex);
            }
        }
        
        @Override
        public void rollback(Connection connection) throws SQLException {
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DROP INDEX IF EXISTS idx_player_data_name");
                stmt.executeUpdate("DROP INDEX IF EXISTS idx_player_data_last_seen");
            }
        }
    }
}