// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/example/SqlBridgeExample.java
package com.minecraft.sqlbridge.example;

import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.DatabaseService;
import com.minecraft.sqlbridge.api.callback.DatabaseCallback;
import com.minecraft.sqlbridge.api.callback.DatabaseResultCallback;
import com.minecraft.sqlbridge.api.query.DeleteBuilder;
import com.minecraft.sqlbridge.api.query.InsertBuilder;
import com.minecraft.sqlbridge.api.query.SelectBuilder;
import com.minecraft.sqlbridge.api.query.UpdateBuilder;
import com.minecraft.sqlbridge.api.result.ResultMapper;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Example plugin showing how to use the SQL-Bridge API.
 * This class demonstrates all the new features of the enhanced API.
 */
public class SqlBridgeExample extends JavaPlugin implements Listener {
    
    private DatabaseService databaseService;
    private Database database;
    
    @Override
    public void onEnable() {
        // Get the database service from your service registry
        // This is just an example - how you get the service depends on your implementation
        databaseService = ServiceRegistry.getService(DatabaseService.class);
        
        if (databaseService == null) {
            getLogger().severe("Could not find DatabaseService!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize the database
        initializeDatabase();
        
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    /**
     * Initialize the database with tables.
     */
    private void initializeDatabase() {
        // Get a database connection specifically for this plugin
        database = databaseService.getDatabaseForPlugin(this);
        
        // Initialize database with tables using the enhanced API
        boolean success = databaseService.initializeDatabase(this,
            "CREATE TABLE IF NOT EXISTS players (" +
            "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
            "uuid VARCHAR(36) NOT NULL UNIQUE, " +
            "name VARCHAR(16) NOT NULL, " +
            "last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "balance DOUBLE DEFAULT 0.0)",
            
            "CREATE TABLE IF NOT EXISTS player_stats (" +
            "player_id INTEGER PRIMARY KEY, " +
            "deaths INTEGER DEFAULT 0, " +
            "kills INTEGER DEFAULT 0, " +
            "FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE)"
        );
        
        if (success) {
            getLogger().info("Database initialized successfully!");
        } else {
            getLogger().warning("There were issues initializing the database.");
        }
    }
    
    /**
     * Example of the player join event handler.
     * This demonstrates different ways to interact with the database.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String name = player.getName();
        
        // Example 1: Safe method with direct SQL
        // Check if player exists
        ResultMapper<Integer> idMapper = row -> row.getInt("id");
        Optional<Integer> playerId = database.queryFirstSafe(
            "SELECT id FROM players WHERE uuid = ?",
            idMapper, 
            getLogger(),
            uuid
        );
        
        if (playerId.isPresent()) {
            // Update existing player
            updateExistingPlayer(playerId.get(), name);
        } else {
            // Create new player
            createNewPlayer(uuid, name);
        }
        
        // Example 2: Query builder with safe execution
        loadPlayerStats(playerId.orElse(null), player);
    }
    
    /**
     * Update an existing player's record.
     * Shows different ways to update data.
     */
    private void updateExistingPlayer(int playerId, String name) {
        // Example 1: Safe direct SQL update
        database.updateSafe(
            "UPDATE players SET name = ?, last_login = CURRENT_TIMESTAMP WHERE id = ?",
            getLogger(),
            name, playerId
        );
        
        // Example 2: Using the UpdateBuilder
        UpdateBuilder updateBuilder = database.update("players")
            .set("name", name)
            .set("last_login", java.sql.Timestamp.from(java.time.Instant.now()))
            .where("id = ?", playerId);
        
        // Execute safely
        updateBuilder.executeUpdateSafe(getLogger());
        
        // Example 3: Using callbacks for async operations
        database.updateWithCallback(
            "UPDATE players SET name = ? WHERE id = ?",
            new DatabaseResultCallback<Integer>() {
                @Override
                public void onSuccess(Integer rowsAffected) {
                    getLogger().info("Updated " + rowsAffected + " player records.");
                }
                
                @Override
                public void onError(Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to update player", e);
                }
            },
            name, playerId
        );
    }
    
    /**
     * Create a new player record.
     * Shows different ways to insert data.
     */
    private void createNewPlayer(String uuid, String name) {
        // Example 1: Safe direct SQL insert
        database.updateSafe(
            "INSERT INTO players (uuid, name) VALUES (?, ?)",
            getLogger(),
            uuid, name
        );
        
        // Example 2: Using the InsertBuilder
        InsertBuilder insertBuilder = database.insertInto("players")
            .columns("uuid", "name")
            .values(uuid, name);
        
        // Execute safely
        insertBuilder.executeUpdateSafe(getLogger());
        
        // Example 3: Using columnValues for cleaner code
        Map<String, Object> playerData = new HashMap<>();
        playerData.put("uuid", uuid);
        playerData.put("name", name);
        
        database.insertInto("players")
            .columnValues(playerData)
            .executeUpdateSafe(getLogger());
        
        // Get the player ID after insert
        Optional<Integer> newPlayerId = database.queryFirstSafe(
            "SELECT id FROM players WHERE uuid = ?",
            row -> row.getInt("id"),
            getLogger(),
            uuid
        );
        
        // Initialize player stats
        newPlayerId.ifPresent(id -> {
            database.insertInto("player_stats")
                .columns("player_id")
                .values(id)
                .executeUpdateSafe(getLogger());
        });
    }
    
    /**
     * Load a player's stats.
     * Shows how to use the SelectBuilder and process results.
     */
    private void loadPlayerStats(Integer playerId, Player player) {
        if (playerId == null) {
            getLogger().warning("Cannot load stats for player with null ID: " + player.getName());
            return;
        }
        
        // Example: Using the SelectBuilder
        SelectBuilder selectBuilder = database.select()
            .columns("deaths", "kills")
            .from("player_stats")
            .where("player_id = ?", playerId);
        
        // Define a mapper to convert result rows to a PlayerStats object
        ResultMapper<PlayerStats> statsMapper = row -> new PlayerStats(
            row.getInt("deaths"),
            row.getInt("kills")
        );
        
        // Execute the query safely
        Optional<PlayerStats> stats = selectBuilder.executeQueryFirstSafe(statsMapper, getLogger());
        
        // Process the results
        stats.ifPresent(s -> {
            player.sendMessage("Welcome back! You have " + s.getKills() + " kills and " + 
                              s.getDeaths() + " deaths.");
            
            // Example: Calculate K/D ratio
            double kdRatio = s.getDeaths() > 0 ? (double) s.getKills() / s.getDeaths() : s.getKills();
            player.sendMessage("Your K/D ratio is: " + String.format("%.2f", kdRatio));
        });
    }
    
    /**
     * Delete a player and all their data.
     * Shows how to use the DeleteBuilder.
     */
    public void deletePlayer(String uuid) {
        // Example: Using the DeleteBuilder
        DeleteBuilder deleteBuilder = database.deleteFrom("players")
            .where("uuid = ?", uuid);
        
        // Execute safely with logging
        int rowsDeleted = deleteBuilder.executeUpdateSafe(getLogger());
        
        if (rowsDeleted > 0) {
            getLogger().info("Deleted player with UUID: " + uuid);
        } else {
            getLogger().info("No player found with UUID: " + uuid);
        }
    }
    
    /**
     * Batch update example.
     * Shows how to perform batch operations.
     */
    public void updatePlayerBalances(Map<String, Double> playerBalances) {
        // Prepare batch parameter sets
        List<Object[]> parameterSets = new java.util.ArrayList<>();
        
        for (Map.Entry<String, Double> entry : playerBalances.entrySet()) {
            parameterSets.add(new Object[] { entry.getValue(), entry.getKey() });
        }
        
        // Perform batch update
        database.batchUpdateSafe(
            "UPDATE players SET balance = ? WHERE uuid = ?",
            parameterSets,
            getLogger()
        );
    }
    
    /**
     * Convenience method example.
     * Shows how to check if tables exist.
     */
    public void checkDatabaseTables() {
        // Check if tables exist
        boolean playersTableExists = database.tableExistsSafe("players", getLogger());
        boolean statsTableExists = database.tableExistsSafe("player_stats", getLogger());
        
        getLogger().info("Players table exists: " + playersTableExists);
        getLogger().info("Player stats table exists: " + statsTableExists);
        
        // Create tables if they don't exist
        if (!playersTableExists) {
            database.createTableIfNotExistsSafe("players",
                "CREATE TABLE players (" +
                "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "uuid VARCHAR(36) NOT NULL UNIQUE, " +
                "name VARCHAR(16) NOT NULL)",
                getLogger()
            );
        }
    }
    
    /**
     * Simple class to hold player stats.
     */
    private static class PlayerStats {
        private final int deaths;
        private final int kills;
        
        public PlayerStats(int deaths, int kills) {
            this.deaths = deaths;
            this.kills = kills;
        }
        
        public int getDeaths() {
            return deaths;
        }
        
        public int getKills() {
            return kills;
        }
    }
    
    /**
     * Mock service registry for the example.
     */
    private static class ServiceRegistry {
        public static <T> T getService(Class<T> serviceClass) {
            // This is just a mock implementation
            return null;
        }
    }
}