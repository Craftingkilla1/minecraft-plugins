// ./Example-Plugin/src/main/java/com/minecraft/example/service/DefaultStatsService.java
package com.minecraft.example.service;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.PlayerStatsPlugin;
import com.minecraft.example.api.StatsService;
import com.minecraft.example.data.PlayerData;
import com.minecraft.example.database.DatabaseManager;
import com.minecraft.example.database.dao.PlayerDataDAO;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of the StatsService interface.
 */
public class DefaultStatsService implements StatsService {
    
    private final PlayerStatsPlugin plugin;
    private final DatabaseManager databaseManager;
    private final PlayerDataDAO playerDataDAO;
    
    // In-memory cache of active player data
    private final Map<UUID, PlayerData> activePlayerCache = new ConcurrentHashMap<>();
    
    // In-memory cache of last activity time for each player
    private final Map<UUID, LocalDateTime> lastActivityTime = new ConcurrentHashMap<>();
    
    /**
     * Creates a new DefaultStatsService instance.
     *
     * @param plugin The plugin instance
     * @param databaseManager The database manager
     */
    public DefaultStatsService(PlayerStatsPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.playerDataDAO = databaseManager.getPlayerDataDAO();
        
        // Schedule periodic cache flushing
        schedulePeriodicFlush();
    }
    
    /**
     * Schedules periodic flushing of cached player data to the database.
     */
    private void schedulePeriodicFlush() {
        int flushInterval = plugin.getPluginConfig().get("performance.cache-flush-interval", 5) * 20;
        
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            LogUtil.debug("Flushing player data cache to database...");
            flushCache();
            
            // Clean up inactive player data from memory
            cleanupInactivePlayerData();
        }, flushInterval, flushInterval);
    }
    
    /**
     * Flushes cached player data to the database.
     */
    private void flushCache() {
        for (PlayerData playerData : activePlayerCache.values()) {
            try {
                playerDataDAO.updatePlayer(playerData);
            } catch (SQLException e) {
                LogUtil.severe("Failed to flush player data: " + e.getMessage());
            }
        }
    }
    
    /**
     * Cleans up inactive player data from memory.
     */
    private void cleanupInactivePlayerData() {
        int expiryTime = plugin.getPluginConfig().getCacheExpiryTime();
        LocalDateTime now = LocalDateTime.now();
        
        // Check each cached player
        for (Map.Entry<UUID, LocalDateTime> entry : lastActivityTime.entrySet()) {
            UUID uuid = entry.getKey();
            LocalDateTime lastActive = entry.getValue();
            
            // Calculate how long since last activity
            long minutesSinceActivity = ChronoUnit.MINUTES.between(lastActive, now);
            
            // If inactive for too long, remove from cache
            if (minutesSinceActivity >= expiryTime) {
                // Flush data to database before removing
                PlayerData playerData = activePlayerCache.get(uuid);
                if (playerData != null) {
                    try {
                        playerDataDAO.updatePlayer(playerData);
                    } catch (SQLException e) {
                        LogUtil.severe("Failed to save player data before cache cleanup: " + e.getMessage());
                    }
                }
                
                // Remove from cache
                activePlayerCache.remove(uuid);
                lastActivityTime.remove(uuid);
                
                LogUtil.debug("Removed inactive player data from cache: " + uuid);
            }
        }
    }
    
    @Override
    public void incrementStat(Player player, String statName) {
        incrementStat(player, statName, 1);
    }
    
    @Override
    public void incrementStat(Player player, String statName, int amount) {
        UUID uuid = player.getUniqueId();
        
        // Update memory cache
        PlayerData playerData = getOrLoadPlayerData(player);
        playerData.incrementStat(statName, amount);
        
        // Update last activity time
        lastActivityTime.put(uuid, LocalDateTime.now());
        
        // If auto-save is enabled, save to database immediately
        if (plugin.getPluginConfig().get("performance.auto-save-stats", false)) {
            savePlayerDataAsync(playerData);
        }
    }
    
    @Override
    public void setStat(Player player, String statName, int value) {
        UUID uuid = player.getUniqueId();
        
        // Update memory cache
        PlayerData playerData = getOrLoadPlayerData(player);
        playerData.setStat(statName, value);
        
        // Update last activity time
        lastActivityTime.put(uuid, LocalDateTime.now());
        
        // If auto-save is enabled, save to database immediately
        if (plugin.getPluginConfig().get("performance.auto-save-stats", false)) {
            savePlayerDataAsync(playerData);
        }
    }
    
    @Override
    public CompletableFuture<Void> setStatAsync(Player player, String statName, int value) {
        setStat(player, statName, value);
        
        // Save to database asynchronously
        return saveStatAsync(player.getUniqueId(), statName, value).thenApply(result -> null);
    }
    
    @Override
    public int getStat(Player player, String statName) {
        // Get from memory cache if available
        PlayerData playerData = activePlayerCache.get(player.getUniqueId());
        if (playerData != null) {
            return playerData.getStat(statName);
        }
        
        // Get from database
        try {
            return playerDataDAO.getStat(player.getUniqueId(), statName);
        } catch (SQLException e) {
            LogUtil.severe("Failed to get stat: " + e.getMessage());
            return 0;
        }
    }
    
    @Override
    public CompletableFuture<Integer> getStatAsync(Player player, String statName) {
        // Get from memory cache if available
        PlayerData playerData = activePlayerCache.get(player.getUniqueId());
        if (playerData != null) {
            return CompletableFuture.completedFuture(playerData.getStat(statName));
        }
        
        // Get from database asynchronously
        return playerDataDAO.getStatAsync(player.getUniqueId(), statName);
    }
    
    @Override
    public Map<String, Integer> getAllStats(Player player) {
        return getAllStats(player.getUniqueId());
    }
    
    @Override
    public CompletableFuture<Map<String, Integer>> getAllStatsAsync(Player player) {
        return getAllStatsAsync(player.getUniqueId());
    }
    
    @Override
    public Map<String, Integer> getAllStats(UUID uuid) {
        // Get from memory cache if available
        PlayerData playerData = activePlayerCache.get(uuid);
        if (playerData != null && playerData.getStats() != null) {
            return new HashMap<>(playerData.getStats());
        }
        
        // Get from database
        try {
            return playerDataDAO.getAllStats(uuid);
        } catch (SQLException e) {
            LogUtil.severe("Failed to get all stats: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    @Override
    public CompletableFuture<Map<String, Integer>> getAllStatsAsync(UUID uuid) {
        // Get from memory cache if available
        PlayerData playerData = activePlayerCache.get(uuid);
        if (playerData != null && playerData.getStats() != null) {
            return CompletableFuture.completedFuture(new HashMap<>(playerData.getStats()));
        }
        
        // Get from database asynchronously
        return playerDataDAO.getAllStatsAsync(uuid);
    }
    
    @Override
    public boolean resetStat(Player player, String statName) {
        UUID uuid = player.getUniqueId();
        
        // Update memory cache
        PlayerData playerData = getOrLoadPlayerData(player);
        playerData.setStat(statName, 0);
        
        // Save to database
        try {
            return playerDataDAO.updateStat(uuid, statName, 0);
        } catch (SQLException e) {
            LogUtil.severe("Failed to reset stat: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean resetAllStats(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Clear stats in memory cache
        PlayerData playerData = getOrLoadPlayerData(player);
        playerData.setStats(new HashMap<>());
        
        // Save to database
        try {
            return playerDataDAO.updatePlayer(playerData);
        } catch (SQLException e) {
            LogUtil.severe("Failed to reset all stats: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void initializePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        
        try {
            // Check if player exists in database
            PlayerData existingData = playerDataDAO.getPlayer(uuid);
            
            if (existingData == null) {
                // Create new player data
                PlayerData newPlayerData = PlayerData.create(uuid, name);
                playerDataDAO.createPlayer(newPlayerData);
                
                // Add to cache
                activePlayerCache.put(uuid, newPlayerData);
                lastActivityTime.put(uuid, LocalDateTime.now());
                
                LogUtil.info("Initialized new player: " + name);
            } else {
                // Update existing player's name if needed
                if (!existingData.getName().equals(name)) {
                    existingData.setName(name);
                    playerDataDAO.updatePlayer(existingData);
                }
                
                // Add to cache
                activePlayerCache.put(uuid, existingData);
                lastActivityTime.put(uuid, LocalDateTime.now());
                
                LogUtil.debug("Loaded existing player data: " + name);
            }
        } catch (SQLException e) {
            LogUtil.severe("Failed to initialize player: " + e.getMessage());
        }
    }
    
    @Override
    public void updatePlayerActivity(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Update memory cache
        PlayerData playerData = getOrLoadPlayerData(player);
        playerData.updateLastSeen();
        
        // Update last activity time
        lastActivityTime.put(uuid, LocalDateTime.now());
        
        // Save to database
        try {
            playerDataDAO.updatePlayer(playerData);
        } catch (SQLException e) {
            LogUtil.severe("Failed to update player activity: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Integer> getTopPlayers(String statName, int limit) {
        try {
            return playerDataDAO.getTopPlayers(statName, limit);
        } catch (SQLException e) {
            LogUtil.severe("Failed to get top players: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    @Override
    public CompletableFuture<Map<String, Integer>> getTopPlayersAsync(String statName, int limit) {
        return playerDataDAO.getTopPlayersAsync(statName, limit);
    }
    
    @Override
    public int getPlayerRank(Player player, String statName) {
        // Get the player's stat value
        int playerValue = getStat(player, statName);
        
        // Get top players for the stat
        Map<String, Integer> topPlayers = getTopPlayers(statName, Integer.MAX_VALUE);
        
        // Count players with higher stats
        int rank = 1;
        for (Map.Entry<String, Integer> entry : topPlayers.entrySet()) {
            if (entry.getValue() > playerValue) {
                rank++;
            } else if (entry.getValue() == playerValue && !entry.getKey().equals(player.getName())) {
                // Tiebreaker: alphabetical order
                if (entry.getKey().compareTo(player.getName()) < 0) {
                    rank++;
                }
            }
        }
        
        return rank;
    }
    
    @Override
    public void updateStats(Player player, Map<String, Integer> stats) {
        UUID uuid = player.getUniqueId();
        
        // Update memory cache
        PlayerData playerData = getOrLoadPlayerData(player);
        
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            playerData.setStat(entry.getKey(), entry.getValue());
        }
        
        // Update last activity time
        lastActivityTime.put(uuid, LocalDateTime.now());
        
        // Save to database in batch
        try {
            playerDataDAO.updatePlayerStats(playerData);
        } catch (SQLException e) {
            LogUtil.severe("Failed to update stats: " + e.getMessage());
        }
    }
    
    @Override
    public CompletableFuture<Void> updateStatsAsync(Player player, Map<String, Integer> stats) {
        // Update memory cache
        updateStats(player, stats);
        
        // Save to database asynchronously
        return CompletableFuture.runAsync(() -> {
            try {
                PlayerData playerData = activePlayerCache.get(player.getUniqueId());
                if (playerData != null) {
                    playerDataDAO.updatePlayerStats(playerData);
                }
            } catch (SQLException e) {
                LogUtil.severe("Failed to update stats asynchronously: " + e.getMessage());
            }
        });
    }
    
    /**
     * Gets existing player data from cache or loads it from the database.
     *
     * @param player The player
     * @return The player data
     */
    private PlayerData getOrLoadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Check cache first
        PlayerData playerData = activePlayerCache.get(uuid);
        
        if (playerData == null) {
            try {
                // Load from database
                playerData = playerDataDAO.getPlayer(uuid);
                
                // If still null, initialize new player data
                if (playerData == null) {
                    playerData = PlayerData.create(uuid, player.getName());
                    playerDataDAO.createPlayer(playerData);
                }
                
                // Add to cache
                activePlayerCache.put(uuid, playerData);
                lastActivityTime.put(uuid, LocalDateTime.now());
            } catch (SQLException e) {
                LogUtil.severe("Failed to load player data: " + e.getMessage());
                
                // Create temporary player data
                playerData = PlayerData.create(uuid, player.getName());
                activePlayerCache.put(uuid, playerData);
                lastActivityTime.put(uuid, LocalDateTime.now());
            }
        } else {
            // Update last activity time
            lastActivityTime.put(uuid, LocalDateTime.now());
        }
        
        return playerData;
    }
    
    /**
     * Saves player data to the database asynchronously.
     *
     * @param playerData The player data to save
     * @return A CompletableFuture that completes when the operation is finished
     */
    private CompletableFuture<Boolean> savePlayerDataAsync(PlayerData playerData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return playerDataDAO.updatePlayer(playerData);
            } catch (SQLException e) {
                LogUtil.severe("Failed to save player data: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Saves a specific stat to the database asynchronously.
     *
     * @param uuid The player's UUID
     * @param statName The statistic name
     * @param value The statistic value
     * @return A CompletableFuture that completes with true if the stat was saved
     */
    private CompletableFuture<Boolean> saveStatAsync(UUID uuid, String statName, int value) {
        return playerDataDAO.updateStatAsync(uuid, statName, value);
    }
    
    /**
     * Called when a player logs out.
     *
     * @param player The player
     */
    public void onPlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Get player data from cache
        PlayerData playerData = activePlayerCache.get(uuid);
        
        if (playerData != null) {
            // Update last seen time
            playerData.updateLastSeen();
            
            // Save to database
            try {
                playerDataDAO.updatePlayer(playerData);
            } catch (SQLException e) {
                LogUtil.severe("Failed to save player data on logout: " + e.getMessage());
            }
            
            // Remove from cache
            activePlayerCache.remove(uuid);
            lastActivityTime.remove(uuid);
        }
    }
    
    /**
     * Shuts down the service, saving all cached player data.
     */
    public void shutdown() {
        LogUtil.info("Saving all player data before shutdown...");
        flushCache();
        activePlayerCache.clear();
        lastActivityTime.clear();
    }
}