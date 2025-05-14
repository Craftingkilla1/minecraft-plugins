// ./Example-Plugin/src/main/java/com/minecraft/example/service/DefaultLeaderboardService.java
package com.minecraft.example.service;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.PlayerStatsPlugin;
import com.minecraft.example.api.LeaderboardService;
import com.minecraft.example.api.StatsService;
import com.minecraft.example.data.Leaderboard;
import com.minecraft.example.data.LeaderboardEntry;
import com.minecraft.example.database.DatabaseManager;
import com.minecraft.example.database.dao.LeaderboardDAO;
import com.minecraft.example.database.dao.PlayerDataDAO;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of the LeaderboardService interface.
 */
public class DefaultLeaderboardService implements LeaderboardService {
    
    private final PlayerStatsPlugin plugin;
    private final DatabaseManager databaseManager;
    private final LeaderboardDAO leaderboardDAO;
    private final PlayerDataDAO playerDataDAO;
    private final StatsService statsService;
    
    // In-memory cache of leaderboards
    private final Map<String, Leaderboard> leaderboardCache = new ConcurrentHashMap<>();
    
    // Map of leaderboard update tasks
    private final Map<String, BukkitTask> updateTasks = new HashMap<>();
    
    /**
     * Creates a new DefaultLeaderboardService instance.
     *
     * @param plugin The plugin instance
     * @param databaseManager The database manager
     * @param statsService The stats service
     */
    public DefaultLeaderboardService(PlayerStatsPlugin plugin, DatabaseManager databaseManager, StatsService statsService) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.leaderboardDAO = databaseManager.getLeaderboardDAO();
        this.playerDataDAO = databaseManager.getPlayerDataDAO();
        this.statsService = statsService;
        
        // Load leaderboards
        loadLeaderboards();
        
        // Schedule automatic leaderboard updates
        scheduleLeaderboardUpdates();
    }
    
    @Override
    public void loadLeaderboards() {
        // Load from configuration
        ConfigurationSection leaderboardsSection = plugin.getConfig().getConfigurationSection("leaderboards.default");
        if (leaderboardsSection == null) {
            LogUtil.warning("No default leaderboards found in configuration.");
            return;
        }
        
        // Process each leaderboard
        for (String id : leaderboardsSection.getKeys(false)) {
            ConfigurationSection section = leaderboardsSection.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            
            String displayName = section.getString("display-name", id);
            String statName = section.getString("stat-name", "");
            String category = section.getString("category", "general");
            String iconName = section.getString("icon", "PAPER");
            boolean reversed = section.getBoolean("reversed", false);
            int updateInterval = section.getInt("update-interval", 
                    plugin.getPluginConfig().getLeaderboardUpdateInterval());
            
            Material icon;
            try {
                icon = Material.valueOf(iconName.toUpperCase());
            } catch (IllegalArgumentException e) {
                LogUtil.warning("Invalid material for leaderboard " + id + ": " + iconName);
                icon = Material.PAPER;
            }
            
            // Create leaderboard
            Leaderboard leaderboard = Leaderboard.builder()
                    .id(id)
                    .displayName(displayName)
                    .statName(statName)
                    .icon(icon)
                    .category(category)
                    .reversed(reversed)
                    .updateInterval(updateInterval)
                    .description("Top players for " + displayName)
                    .build();
            
            // Save to database
            try {
                leaderboardDAO.saveLeaderboard(leaderboard);
                leaderboardCache.put(id, leaderboard);
                LogUtil.debug("Loaded leaderboard: " + id);
            } catch (SQLException e) {
                LogUtil.severe("Failed to save leaderboard to database: " + e.getMessage());
            }
        }
        
        // Load any additional leaderboards from the database
        try {
            List<Leaderboard> dbLeaderboards = leaderboardDAO.getAllLeaderboards();
            for (Leaderboard leaderboard : dbLeaderboards) {
                if (!leaderboardCache.containsKey(leaderboard.getId())) {
                    leaderboardCache.put(leaderboard.getId(), leaderboard);
                    LogUtil.debug("Loaded additional leaderboard from database: " + leaderboard.getId());
                }
            }
        } catch (SQLException e) {
            LogUtil.severe("Failed to load leaderboards from database: " + e.getMessage());
        }
        
        LogUtil.info("Loaded " + leaderboardCache.size() + " leaderboards.");
    }
    
    /**
     * Schedules automatic leaderboard updates.
     */
    private void scheduleLeaderboardUpdates() {
        // Schedule updates for each leaderboard
        for (Leaderboard leaderboard : leaderboardCache.values()) {
            scheduleLeaderboardUpdates(leaderboard.getId(), leaderboard.getUpdateInterval());
        }
    }
    
    @Override
    public List<Leaderboard> getAllLeaderboards() {
        if (leaderboardCache.isEmpty()) {
            try {
                List<Leaderboard> leaderboards = leaderboardDAO.getAllLeaderboards();
                for (Leaderboard leaderboard : leaderboards) {
                    leaderboardCache.put(leaderboard.getId(), leaderboard);
                }
                return leaderboards;
            } catch (SQLException e) {
                LogUtil.severe("Failed to get leaderboards: " + e.getMessage());
                return new ArrayList<>();
            }
        }
        
        return new ArrayList<>(leaderboardCache.values());
    }
    
    @Override
    public Leaderboard getLeaderboard(String id) {
        // Check cache first
        if (leaderboardCache.containsKey(id)) {
            return leaderboardCache.get(id);
        }
        
        // Try to get from database
        try {
            Leaderboard leaderboard = leaderboardDAO.getLeaderboard(id);
            if (leaderboard != null) {
                leaderboardCache.put(id, leaderboard);
            }
            return leaderboard;
        } catch (SQLException e) {
            LogUtil.severe("Failed to get leaderboard: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public List<LeaderboardEntry> getLeaderboardEntries(String id, int limit) {
        try {
            return leaderboardDAO.getLeaderboardEntries(id, limit);
        } catch (SQLException e) {
            LogUtil.severe("Failed to get leaderboard entries: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @Override
    public CompletableFuture<List<LeaderboardEntry>> getLeaderboardEntriesAsync(String id, int limit) {
        return leaderboardDAO.getLeaderboardEntriesAsync(id, limit)
                .exceptionally(e -> {
                    LogUtil.severe("Failed to get leaderboard entries asynchronously: " + e.getMessage());
                    return new ArrayList<>();
                });
    }
    
    @Override
    public void updateAllLeaderboards() {
        // Update each leaderboard
        for (String id : leaderboardCache.keySet()) {
            updateLeaderboard(id);
        }
    }
    
    @Override
    public boolean updateLeaderboard(String id) {
        // Get leaderboard
        Leaderboard leaderboard = getLeaderboard(id);
        if (leaderboard == null) {
            LogUtil.warning("Tried to update non-existent leaderboard: " + id);
            return false;
        }
        
        // Get statistic name
        String statName = leaderboard.getStatName();
        if (statName == null || statName.isEmpty()) {
            LogUtil.warning("Leaderboard " + id + " has no stat name");
            return false;
        }
        
        try {
            // Get top players for the statistic
            Map<String, Integer> topPlayers = statsService.getTopPlayers(statName, 100);
            
            // Convert to leaderboard entries
            List<LeaderboardEntry> entries = new ArrayList<>();
            int rank = 1;
            
            for (Map.Entry<String, Integer> entry : topPlayers.entrySet()) {
                String playerName = entry.getKey();
                int score = entry.getValue();
                
                // Get player ID
                int playerId = getPlayerIdByName(playerName);
                if (playerId == -1) {
                    continue; // Skip if player not found
                }
                
                // Create entry
                LeaderboardEntry leaderboardEntry = LeaderboardEntry.create(
                        id,
                        new UUID(0, 0), // Placeholder, will be set by DAO
                        playerName,
                        rank,
                        score
                );
                
                entries.add(leaderboardEntry);
                rank++;
            }
            
            // Save entries
            if (!entries.isEmpty()) {
                // Clear existing entries
                leaderboardDAO.clearLeaderboard(id);
                
                // Save new entries
                leaderboardDAO.saveLeaderboardEntries(entries);
            }
            
            // Update leaderboard timestamp
            leaderboardDAO.updateLeaderboardTimestamp(id);
            
            LogUtil.debug("Updated leaderboard: " + id + " with " + entries.size() + " entries");
            return true;
        } catch (Exception e) {
            LogUtil.severe("Failed to update leaderboard: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public CompletableFuture<Boolean> updateLeaderboardAsync(String id) {
        return CompletableFuture.supplyAsync(() -> updateLeaderboard(id));
    }
    
    @Override
    public int getPlayerRank(Player player, String id) {
        try {
            // Get player ID
            int playerId = getPlayerId(player.getUniqueId());
            if (playerId == -1) {
                return -1;
            }
            
            return leaderboardDAO.getPlayerRank(id, playerId);
        } catch (SQLException e) {
            LogUtil.severe("Failed to get player rank: " + e.getMessage());
            return -1;
        }
    }
    
    @Override
    public CompletableFuture<Integer> getPlayerRankAsync(Player player, String id) {
        return getPlayerIdAsync(player.getUniqueId())
                .thenCompose(playerId -> {
                    if (playerId == -1) {
                        return CompletableFuture.completedFuture(-1);
                    }
                    
                    return leaderboardDAO.getPlayerRankAsync(id, playerId);
                })
                .exceptionally(e -> {
                    LogUtil.severe("Failed to get player rank asynchronously: " + e.getMessage());
                    return -1;
                });
    }
    
    @Override
    public Map<String, List<LeaderboardEntry>> getTopPlayersAllLeaderboards(int limit) {
        Map<String, List<LeaderboardEntry>> result = new HashMap<>();
        
        for (String id : leaderboardCache.keySet()) {
            List<LeaderboardEntry> entries = getLeaderboardEntries(id, limit);
            result.put(id, entries);
        }
        
        return result;
    }
    
    @Override
    public boolean createLeaderboard(String id, String displayName, String statName) {
        // Check if leaderboard already exists
        if (leaderboardCache.containsKey(id)) {
            return false;
        }
        
        // Create leaderboard
        Leaderboard leaderboard = Leaderboard.builder()
                .id(id)
                .displayName(displayName)
                .statName(statName)
                .icon(Material.PAPER) // Default icon
                .category("custom")
                .reversed(false)
                .updateInterval(plugin.getPluginConfig().getLeaderboardUpdateInterval())
                .description("Custom leaderboard for " + displayName)
                .build();
        
        try {
            // Save to database
            boolean saved = leaderboardDAO.saveLeaderboard(leaderboard);
            
            if (saved) {
                // Add to cache
                leaderboardCache.put(id, leaderboard);
                
                // Schedule updates
                scheduleLeaderboardUpdates(id, leaderboard.getUpdateInterval());
                
                // Update immediately
                updateLeaderboardAsync(id);
            }
            
            return saved;
        } catch (SQLException e) {
            LogUtil.severe("Failed to create leaderboard: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean deleteLeaderboard(String id) {
        try {
            // Delete from database
            boolean deleted = leaderboardDAO.deleteLeaderboard(id);
            
            if (deleted) {
                // Remove from cache
                leaderboardCache.remove(id);
                
                // Cancel update task
                BukkitTask task = updateTasks.remove(id);
                if (task != null) {
                    task.cancel();
                }
            }
            
            return deleted;
        } catch (SQLException e) {
            LogUtil.severe("Failed to delete leaderboard: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean scheduleLeaderboardUpdates(String id, int interval) {
        // Cancel existing task
        BukkitTask existingTask = updateTasks.get(id);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Create new task
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                () -> updateLeaderboard(id),
                interval * 20 * 60, // Convert to ticks
                interval * 20 * 60  // Convert to ticks
        );
        
        updateTasks.put(id, task);
        
        // Update leaderboard interval in database
        try {
            Leaderboard leaderboard = getLeaderboard(id);
            if (leaderboard != null) {
                leaderboard.setUpdateInterval(interval);
                leaderboardDAO.saveLeaderboard(leaderboard);
            }
        } catch (SQLException e) {
            LogUtil.severe("Failed to update leaderboard interval: " + e.getMessage());
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets the player ID by UUID from the database.
     *
     * @param uuid The player's UUID
     * @return The player ID, or -1 if not found
     */
    private int getPlayerId(UUID uuid) throws SQLException {
        return playerDataDAO.queryFirst(
                "SELECT id FROM players WHERE uuid = ?",
                rs -> rs.getInt("id"),
                uuid.toString()
        ).orElse(-1);
    }
    
    /**
     * Gets the player ID by UUID from the database asynchronously.
     *
     * @param uuid The player's UUID
     * @return A CompletableFuture that completes with the player ID
     */
    private CompletableFuture<Integer> getPlayerIdAsync(UUID uuid) {
        return playerDataDAO.queryFirstAsync(
                "SELECT id FROM players WHERE uuid = ?",
                rs -> rs.getInt("id"),
                uuid.toString()
        ).thenApply(optional -> optional.orElse(-1));
    }
    
    /**
     * Gets the player ID by name from the database.
     *
     * @param name The player's name
     * @return The player ID, or -1 if not found
     */
    private int getPlayerIdByName(String name) throws SQLException {
        return playerDataDAO.queryFirst(
                "SELECT id FROM players WHERE name = ? COLLATE NOCASE",
                rs -> rs.getInt("id"),
                name
        ).orElse(-1);
    }
    
    /**
     * Shuts down the service, canceling all update tasks.
     */
    public void shutdown() {
        // Cancel all update tasks
        for (BukkitTask task : updateTasks.values()) {
            task.cancel();
        }
        
        updateTasks.clear();
    }
}