// ./Example-Plugin/src/main/java/com/example/exampleplugin/PlayerDataManager.java
package com.example.exampleplugin;

import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.result.ResultMapper;
import com.minecraft.sqlbridge.api.result.ResultRow;
import com.minecraft.sqlbridge.api.transaction.Transaction;

import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages player data, including loading, saving, and caching.
 */
public class PlayerDataManager {
    
    private final ExamplePlugin plugin;
    private final Database database;
    private final Map<UUID, PlayerData> playerDataCache;
    private final ResultMapper<PlayerData> playerDataMapper;
    
    /**
     * Constructor for PlayerDataManager.
     *
     * @param plugin The plugin instance
     * @param database The database instance
     */
    public PlayerDataManager(ExamplePlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.playerDataCache = new ConcurrentHashMap<>();
        
        // Create a mapper for player data
        this.playerDataMapper = row -> {
            try {
                UUID uuid = UUID.fromString(row.getString("uuid"));
                String name = row.getString("name");
                int blocksBroken = row.getInt("blocks_broken");
                int blocksPlaced = row.getInt("blocks_placed");
                int mobsKilled = row.getInt("mobs_killed");
                int deaths = row.getInt("deaths");
                long lastSeen = row.getLong("last_seen");
                long firstJoin = row.getLong("first_join");
                
                return new PlayerData(uuid, name, blocksBroken, blocksPlaced, 
                                     mobsKilled, deaths, lastSeen, firstJoin);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to map player data", e);
                return null;
            }
        };
        
        // Start auto-save task (every 5 minutes)
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllPlayers, 6000, 6000);
    }
    
    /**
     * Load player data from the database.
     *
     * @param uuid The player's UUID
     * @return The player data, or null if not found
     */
    public PlayerData loadPlayerData(UUID uuid) {
        try {
            // Check if already cached
            if (playerDataCache.containsKey(uuid)) {
                return playerDataCache.get(uuid);
            }
            
            // Query the database
            Optional<PlayerData> playerData = database.queryFirst(
                "SELECT * FROM player_data WHERE uuid = ?",
                playerDataMapper,
                uuid.toString()
            );
            
            if (playerData.isPresent()) {
                // Cache and return
                PlayerData data = playerData.get();
                playerDataCache.put(uuid, data);
                return data;
            }
            
            return null;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid, e);
            return null;
        }
    }
    
    /**
     * Create or update player data.
     *
     * @param player The player
     * @return The player data
     */
    public PlayerData createOrUpdatePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Check if already cached
        PlayerData playerData = playerDataCache.get(uuid);
        
        if (playerData == null) {
            // Try to load from database
            playerData = loadPlayerData(uuid);
            
            if (playerData == null) {
                // Create new player data
                playerData = PlayerData.createNew(uuid, player.getName());
                
                // Save to database
                savePlayerData(playerData);
            } else {
                // Update name and last seen
                playerData.setLastSeen(System.currentTimeMillis());
            }
            
            // Cache
            playerDataCache.put(uuid, playerData);
        } else {
            // Update last seen
            playerData.setLastSeen(System.currentTimeMillis());
        }
        
        return playerData;
    }
    
    /**
     * Save player data to the database.
     *
     * @param playerData The player data to save
     * @return true if successful, false otherwise
     */
    public boolean savePlayerData(PlayerData playerData) {
        try {
            // Create parameters map manually instead of using Map.of since we're on Java 8
            Map<String, Object> updateParams = new HashMap<>();
            updateParams.put("blocks_broken", playerData.getBlocksBroken());
            updateParams.put("blocks_placed", playerData.getBlocksPlaced());
            updateParams.put("mobs_killed", playerData.getMobsKilled());
            updateParams.put("deaths", playerData.getDeaths());
            updateParams.put("last_seen", playerData.getLastSeen());
            
            // Use fluent query builder
            int rowsAffected = database.insertInto("player_data")
                .columns("uuid", "name", "blocks_broken", "blocks_placed", 
                         "mobs_killed", "deaths", "last_seen", "first_join")
                .values(
                    playerData.getUuid().toString(),
                    playerData.getName(),
                    playerData.getBlocksBroken(),
                    playerData.getBlocksPlaced(),
                    playerData.getMobsKilled(),
                    playerData.getDeaths(),
                    playerData.getLastSeen(),
                    playerData.getFirstJoin()
                )
                .onDuplicateKeyUpdate(updateParams)
                .executeUpdate();
            
            if (rowsAffected > 0) {
                playerData.markClean();
                return true;
            }
            
            return false;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + 
                                  playerData.getUuid(), e);
            return false;
        }
    }
    
    /**
     * Save all players with dirty data.
     */
    public void saveAllPlayers() {
        int savedCount = 0;
        
        for (PlayerData playerData : playerDataCache.values()) {
            if (playerData.isDirty()) {
                if (savePlayerData(playerData)) {
                    savedCount++;
                }
            }
        }
        
        if (savedCount > 0) {
            plugin.getLogger().info("Auto-saved data for " + savedCount + " players.");
        }
    }
    
    /**
     * Get player data from cache.
     *
     * @param uuid The player's UUID
     * @return The player data, or null if not cached
     */
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataCache.get(uuid);
    }
    
    /**
     * Remove player data from cache.
     *
     * @param uuid The player's UUID
     */
    public void removePlayerData(UUID uuid) {
        PlayerData playerData = playerDataCache.remove(uuid);
        
        if (playerData != null && playerData.isDirty()) {
            // Save before removing
            savePlayerData(playerData);
        }
    }
    
    /**
     * Reset a player's statistics.
     *
     * @param uuid The player's UUID
     * @return true if successful, false otherwise
     * @throws SQLException If a database error occurs
     */
    public boolean resetPlayerStats(UUID uuid) throws SQLException {
        // Update cached player data if exists
        PlayerData playerData = playerDataCache.get(uuid);
        if (playerData != null) {
            playerData.resetStats();
        }
        
        // Update database
        int rowsAffected = database.update(
            "UPDATE player_data SET blocks_broken = 0, blocks_placed = 0, " +
            "mobs_killed = 0, deaths = 0 WHERE uuid = ?",
            uuid.toString()
        );
        
        return rowsAffected > 0;
    }
    
    /**
     * Get top players by a specific statistic.
     *
     * @param statColumn The statistic column name
     * @param limit The maximum number of players to return
     * @return A list of player data sorted by the statistic
     */
    public List<PlayerData> getTopPlayers(String statColumn, int limit) {
        try {
            // Validate column name to prevent SQL injection
            String validColumn;
            switch (statColumn.toLowerCase()) {
                case "blocks_broken":
                    validColumn = "blocks_broken";
                    break;
                case "blocks_placed":
                    validColumn = "blocks_placed";
                    break;
                case "mobs_killed":
                    validColumn = "mobs_killed";
                    break;
                case "deaths":
                    validColumn = "deaths";
                    break;
                default:
                    throw new IllegalArgumentException("Invalid statistic column: " + statColumn);
            }
            
            // Use select builder
            return database.select()
                .columns("*")
                .from("player_data")
                .orderBy(validColumn + " DESC")
                .limit(limit)
                .executeQuery(playerDataMapper);
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get top players", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get the count of unique players.
     *
     * @return The count of unique players
     */
    public int getUniquePlayerCount() {
        try {
            // Use transaction to execute a query
            return database.executeTransaction(connection -> {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "SELECT COUNT(*) FROM player_data")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                        return 0;
                    }
                }
            });
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get unique player count", e);
            return 0;
        }
    }
    
    /**
     * Get recently active players.
     *
     * @param days The number of days to look back
     * @param limit The maximum number of players to return
     * @return A list of recently active players
     */
    public List<PlayerData> getRecentPlayers(int days, int limit) {
        try {
            long cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
            
            return database.query(
                "SELECT * FROM player_data WHERE last_seen > ? ORDER BY last_seen DESC LIMIT ?",
                playerDataMapper,
                cutoff,
                limit
            );
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get recent players", e);
            return new ArrayList<>();
        }
    }
}