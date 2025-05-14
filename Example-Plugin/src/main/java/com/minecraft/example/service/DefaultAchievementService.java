// ./Example-Plugin/src/main/java/com/minecraft/example/service/DefaultAchievementService.java
package com.minecraft.example.service;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.PlayerStatsPlugin;
import com.minecraft.example.api.AchievementService;
import com.minecraft.example.api.StatsService;
import com.minecraft.example.data.Achievement;
import com.minecraft.example.database.DatabaseManager;
import com.minecraft.example.database.dao.AchievementDAO;
import com.minecraft.example.database.dao.PlayerDataDAO;
import com.minecraft.example.listener.AchievementListener;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of the AchievementService interface.
 */
public class DefaultAchievementService implements AchievementService {
    
    private final PlayerStatsPlugin plugin;
    private final DatabaseManager databaseManager;
    private final AchievementDAO achievementDAO;
    private final PlayerDataDAO playerDataDAO;
    private final StatsService statsService;
    
    // In-memory cache of achievements
    private final Map<String, Achievement> achievementCache = new ConcurrentHashMap<>();
    
    // In-memory cache of player achievements
    private final Map<UUID, Set<String>> playerAchievementsCache = new ConcurrentHashMap<>();
    
    /**
     * Creates a new DefaultAchievementService instance.
     *
     * @param plugin The plugin instance
     * @param databaseManager The database manager
     * @param statsService The stats service
     */
    public DefaultAchievementService(PlayerStatsPlugin plugin, DatabaseManager databaseManager, StatsService statsService) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.achievementDAO = databaseManager.getAchievementDAO();
        this.playerDataDAO = databaseManager.getPlayerDataDAO();
        this.statsService = statsService;
        
        // Load achievements
        loadAchievements();
    }
    
    @Override
    public void loadAchievements() {
        // Load from configuration file
        File achievementsFile = new File(plugin.getDataFolder(), "achievements.yml");
        if (!achievementsFile.exists()) {
            plugin.saveResource("achievements.yml", false);
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(achievementsFile);
        ConfigurationSection achievementsSection = config.getConfigurationSection("achievements");
        
        if (achievementsSection == null) {
            LogUtil.warning("No achievements found in configuration.");
            return;
        }
        
        // Clear cache
        achievementCache.clear();
        
        // Process each achievement
        for (String id : achievementsSection.getKeys(false)) {
            ConfigurationSection section = achievementsSection.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            
            String name = section.getString("name", id);
            String description = section.getString("description", "");
            String category = section.getString("category", "general");
            boolean secret = section.getBoolean("secret", false);
            String iconName = section.getString("icon", "PAPER");
            
            Material icon;
            try {
                icon = Material.valueOf(iconName.toUpperCase());
            } catch (IllegalArgumentException e) {
                LogUtil.warning("Invalid material for achievement " + id + ": " + iconName);
                icon = Material.PAPER;
            }
            
            // Parse criteria
            Map<String, Integer> criteria = new HashMap<>();
            ConfigurationSection criteriaSection = section.getConfigurationSection("criteria");
            if (criteriaSection != null) {
                for (String statName : criteriaSection.getKeys(false)) {
                    int value = criteriaSection.getInt(statName);
                    criteria.put(statName, value);
                }
            }
            
            // Create achievement
            Achievement achievement = Achievement.builder()
                    .id(id)
                    .name(name)
                    .description(description)
                    .icon(icon)
                    .category(category)
                    .secret(secret)
                    .criteria(criteria)
                    .build();
            
            // Add to cache
            achievementCache.put(id, achievement);
            
            // Save to database
            try {
                achievementDAO.saveAchievement(achievement);
            } catch (SQLException e) {
                LogUtil.severe("Failed to save achievement to database: " + e.getMessage());
            }
        }
        
        LogUtil.info("Loaded " + achievementCache.size() + " achievements.");
    }
    
    @Override
    public List<Achievement> getAllAchievements() {
        if (achievementCache.isEmpty()) {
            try {
                List<Achievement> achievements = achievementDAO.getAllAchievements();
                for (Achievement achievement : achievements) {
                    achievementCache.put(achievement.getId(), achievement);
                }
                return achievements;
            } catch (SQLException e) {
                LogUtil.severe("Failed to get achievements: " + e.getMessage());
                return new ArrayList<>();
            }
        }
        
        return new ArrayList<>(achievementCache.values());
    }
    
    @Override
    public Achievement getAchievement(String id) {
        // Check cache first
        if (achievementCache.containsKey(id)) {
            return achievementCache.get(id);
        }
        
        // Try to get from database
        try {
            Achievement achievement = achievementDAO.getAchievement(id);
            if (achievement != null) {
                achievementCache.put(id, achievement);
            }
            return achievement;
        } catch (SQLException e) {
            LogUtil.severe("Failed to get achievement: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public boolean hasAchievement(Player player, String id) {
        UUID uuid = player.getUniqueId();
        
        // Check cache first
        if (playerAchievementsCache.containsKey(uuid)) {
            return playerAchievementsCache.get(uuid).contains(id);
        }
        
        // Get player ID
        int playerId;
        try {
            playerId = getPlayerId(player.getUniqueId());
            if (playerId == -1) {
                return false;
            }
        } catch (SQLException e) {
            LogUtil.severe("Failed to get player ID: " + e.getMessage());
            return false;
        }
        
        // Check database
        try {
            return achievementDAO.hasAchievement(playerId, id);
        } catch (SQLException e) {
            LogUtil.severe("Failed to check if player has achievement: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public CompletableFuture<Boolean> hasAchievementAsync(Player player, String id) {
        UUID uuid = player.getUniqueId();
        
        // Check cache first
        if (playerAchievementsCache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(playerAchievementsCache.get(uuid).contains(id));
        }
        
        // Get player ID asynchronously, then check achievement
        return getPlayerIdAsync(player.getUniqueId())
                .thenCompose(playerId -> {
                    if (playerId == -1) {
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    return achievementDAO.hasAchievementAsync(playerId, id);
                })
                .exceptionally(e -> {
                    LogUtil.severe("Failed to check if player has achievement: " + e.getMessage());
                    return false;
                });
    }
    
    @Override
    public List<Achievement> getPlayerAchievements(Player player) {
        return getPlayerAchievements(player.getUniqueId());
    }
    
    @Override
    public CompletableFuture<List<Achievement>> getPlayerAchievementsAsync(Player player) {
        return getPlayerIdAsync(player.getUniqueId())
                .thenCompose(playerId -> {
                    if (playerId == -1) {
                        return CompletableFuture.completedFuture(new ArrayList<Achievement>());
                    }
                    
                    return achievementDAO.getPlayerAchievementsAsync(playerId)
                            .thenApply(achievements -> {
                                // Update player UUID (it's a placeholder in the DAO)
                                for (Achievement achievement : achievements) {
                                    achievement.setPlayerId(player.getUniqueId());
                                }
                                
                                // Update cache
                                Set<String> achievementIds = achievements.stream()
                                        .map(Achievement::getId)
                                        .collect(Collectors.toSet());
                                playerAchievementsCache.put(player.getUniqueId(), achievementIds);
                                
                                return achievements;
                            });
                })
                .exceptionally(e -> {
                    LogUtil.severe("Failed to get player achievements: " + e.getMessage());
                    return new ArrayList<>();
                });
    }
    
    @Override
    public List<Achievement> getPlayerAchievements(UUID uuid) {
        // Get player ID
        int playerId;
        try {
            playerId = getPlayerId(uuid);
            if (playerId == -1) {
                return new ArrayList<>();
            }
        } catch (SQLException e) {
            LogUtil.severe("Failed to get player ID: " + e.getMessage());
            return new ArrayList<>();
        }
        
        // Get achievements from database
        try {
            List<Achievement> achievements = achievementDAO.getPlayerAchievements(playerId);
            
            // Update player UUID (it's a placeholder in the DAO)
            for (Achievement achievement : achievements) {
                achievement.setPlayerId(uuid);
            }
            
            // Update cache
            Set<String> achievementIds = achievements.stream()
                    .map(Achievement::getId)
                    .collect(Collectors.toSet());
            playerAchievementsCache.put(uuid, achievementIds);
            
            return achievements;
        } catch (SQLException e) {
            LogUtil.severe("Failed to get player achievements: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean awardAchievement(Player player, String id) {
        // Get achievement
        Achievement achievement = getAchievement(id);
        if (achievement == null) {
            LogUtil.warning("Attempted to award non-existent achievement: " + id);
            return false;
        }
        
        // Check if player already has the achievement
        if (hasAchievement(player, id)) {
            return false;
        }
        
        // Get player ID
        int playerId;
        try {
            playerId = getPlayerId(player.getUniqueId());
            if (playerId == -1) {
                return false;
            }
        } catch (SQLException e) {
            LogUtil.severe("Failed to get player ID: " + e.getMessage());
            return false;
        }
        
        // Award achievement
        try {
            boolean awarded = achievementDAO.awardAchievement(playerId, id);
            
            if (awarded) {
                // Update cache
                UUID uuid = player.getUniqueId();
                Set<String> achievements = playerAchievementsCache.computeIfAbsent(uuid, k -> new HashSet<>());
                achievements.add(id);
                
                // Notify achievement listener
                if (plugin.getServer().getPluginManager().isPluginEnabled(plugin)) {
                    AchievementListener listener = findAchievementListener();
                    if (listener != null) {
                        listener.onAchievementEarned(player, id, achievement.getName(), achievement.isSecret());
                    }
                }
                
                LogUtil.info("Player " + player.getName() + " earned achievement: " + achievement.getName());
                return true;
            }
        } catch (SQLException e) {
            LogUtil.severe("Failed to award achievement: " + e.getMessage());
        }
        
        return false;
    }
    
    @Override
    public CompletableFuture<Boolean> awardAchievementAsync(Player player, String id) {
        // Get achievement
        Achievement achievement = getAchievement(id);
        if (achievement == null) {
            LogUtil.warning("Attempted to award non-existent achievement: " + id);
            return CompletableFuture.completedFuture(false);
        }
        
        // Check if player already has the achievement
        return hasAchievementAsync(player, id)
                .thenCompose(hasAchievement -> {
                    if (hasAchievement) {
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    // Get player ID and award achievement
                    return getPlayerIdAsync(player.getUniqueId())
                            .thenCompose(playerId -> {
                                if (playerId == -1) {
                                    return CompletableFuture.completedFuture(false);
                                }
                                
                                return achievementDAO.awardAchievementAsync(playerId, id)
                                        .thenApply(awarded -> {
                                            if (awarded) {
                                                // Update cache
                                                UUID uuid = player.getUniqueId();
                                                Set<String> achievements = playerAchievementsCache
                                                        .computeIfAbsent(uuid, k -> new HashSet<>());
                                                achievements.add(id);
                                                
                                                // Notify achievement listener
                                                if (plugin.getServer().getPluginManager().isPluginEnabled(plugin)) {
                                                    AchievementListener listener = findAchievementListener();
                                                    if (listener != null) {
                                                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                                                            listener.onAchievementEarned(player, id, 
                                                                    achievement.getName(), achievement.isSecret());
                                                        });
                                                    }
                                                }
                                                
                                                LogUtil.info("Player " + player.getName() + 
                                                        " earned achievement: " + achievement.getName());
                                            }
                                            return awarded;
                                        });
                            });
                })
                .exceptionally(e -> {
                    LogUtil.severe("Failed to award achievement asynchronously: " + e.getMessage());
                    return false;
                });
    }
    
    @Override
    public void checkAchievements(Player player) {
        // Get player's stats
        Map<String, Integer> stats = statsService.getAllStats(player);
        
        // Get all achievements
        List<Achievement> achievements = getAllAchievements();
        
        // Check each achievement
        for (Achievement achievement : achievements) {
            String id = achievement.getId();
            
            // Skip if player already has the achievement
            if (hasAchievement(player, id)) {
                continue;
            }
            
            // Check if criteria are met
            if (achievement.checkCriteria(stats)) {
                // Award achievement asynchronously
                awardAchievementAsync(player, id);
            } else {
                // Update progress
                updateProgress(player, achievement, stats);
            }
        }
    }
    
    @Override
    public double getAchievementProgress(Player player, String id) {
        // Get achievement
        Achievement achievement = getAchievement(id);
        if (achievement == null) {
            return 0.0;
        }
        
        // Check if player already has the achievement
        if (hasAchievement(player, id)) {
            return 1.0;
        }
        
        // Get player's stats
        Map<String, Integer> stats = statsService.getAllStats(player);
        
        // Calculate progress
        return achievement.calculateProgress(stats);
    }
    
    @Override
    public Map<String, Double> getAllAchievementProgress(Player player) {
        Map<String, Double> progress = new HashMap<>();
        
        // Get player's stats
        Map<String, Integer> stats = statsService.getAllStats(player);
        
        // Get all achievements
        List<Achievement> achievements = getAllAchievements();
        
        // Get player ID
        int playerId;
        try {
            playerId = getPlayerId(player.getUniqueId());
            if (playerId == -1) {
                return progress;
            }
        } catch (SQLException e) {
            LogUtil.severe("Failed to get player ID: " + e.getMessage());
            return progress;
        }
        
        // Get all progress from database
        Map<String, Double> dbProgress;
        try {
            dbProgress = achievementDAO.getAllProgress(playerId);
        } catch (SQLException e) {
            LogUtil.severe("Failed to get achievement progress: " + e.getMessage());
            dbProgress = new HashMap<>();
        }
        
        // Calculate progress for each achievement
        for (Achievement achievement : achievements) {
            String id = achievement.getId();
            
            // Check if player already has the achievement
            if (hasAchievement(player, id)) {
                progress.put(id, 1.0);
                continue;
            }
            
            // Get progress from database or calculate
            double achievementProgress = dbProgress.getOrDefault(id, achievement.calculateProgress(stats));
            progress.put(id, achievementProgress);
        }
        
        return progress;
    }
    
    @Override
    public int getAchievementCount(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Check cache first
        if (playerAchievementsCache.containsKey(uuid)) {
            return playerAchievementsCache.get(uuid).size();
        }
        
        // Get player achievements
        List<Achievement> achievements = getPlayerAchievements(player);
        return achievements.size();
    }
    
    @Override
    public double getCompletionPercentage(Player player) {
        int total = getAllAchievements().size();
        if (total == 0) {
            return 0.0;
        }
        
        int earned = getAchievementCount(player);
        return (double) earned / total * 100.0;
    }
    
    @Override
    public boolean resetAchievements(Player player) {
        // Get player ID
        int playerId;
        try {
            playerId = getPlayerId(player.getUniqueId());
            if (playerId == -1) {
                return false;
            }
        } catch (SQLException e) {
            LogUtil.severe("Failed to get player ID: " + e.getMessage());
            return false;
        }
        
        // Reset achievements
        try {
            int count = achievementDAO.resetAchievements(playerId);
            
            // Update cache
            playerAchievementsCache.remove(player.getUniqueId());
            
            LogUtil.info("Reset " + count + " achievements for player " + player.getName());
            return count > 0;
        } catch (SQLException e) {
            LogUtil.severe("Failed to reset achievements: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<Achievement> getRecentAchievements(Player player, int limit) {
        // Get player ID
        int playerId;
        try {
            playerId = getPlayerId(player.getUniqueId());
            if (playerId == -1) {
                return new ArrayList<>();
            }
        } catch (SQLException e) {
            LogUtil.severe("Failed to get player ID: " + e.getMessage());
            return new ArrayList<>();
        }
        
        // Get recent achievements
        try {
            List<Achievement> achievements = achievementDAO.getRecentAchievements(playerId, limit);
            
            // Update player UUID
            for (Achievement achievement : achievements) {
                achievement.setPlayerId(player.getUniqueId());
            }
            
            return achievements;
        } catch (SQLException e) {
            LogUtil.severe("Failed to get recent achievements: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Updates the progress for an achievement.
     *
     * @param player The player
     * @param achievement The achievement
     * @param stats The player's stats
     */
    private void updateProgress(Player player, Achievement achievement, Map<String, Integer> stats) {
        // Get player ID
        int playerId;
        try {
            playerId = getPlayerId(player.getUniqueId());
            if (playerId == -1) {
                return;
            }
        } catch (SQLException e) {
            LogUtil.severe("Failed to get player ID: " + e.getMessage());
            return;
        }
        
        // Calculate progress
        double progress = achievement.calculateProgress(stats);
        
        // Update progress in database
        try {
            achievementDAO.updateProgress(playerId, achievement.getId(), progress);
        } catch (SQLException e) {
            LogUtil.severe("Failed to update achievement progress: " + e.getMessage());
        }
    }
    
    /**
     * Gets the player ID from the database.
     *
     * @param uuid The player's UUID
     * @return The player ID, or -1 if not found
     * @throws SQLException If an error occurs
     */
    private int getPlayerId(UUID uuid) throws SQLException {
        return playerDataDAO.queryFirst(
                "SELECT id FROM players WHERE uuid = ?",
                rs -> rs.getInt("id"),
                uuid.toString()
        ).orElse(-1);
    }
    
    /**
     * Gets the player ID from the database asynchronously.
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
     * Finds the registered AchievementListener.
     *
     * @return The AchievementListener instance, or null if not found
     */
    private AchievementListener findAchievementListener() {
        for (org.bukkit.event.Listener listener : plugin.getPluginManager().getListeners()) {
            if (listener instanceof AchievementListener) {
                return (AchievementListener) listener;
            }
        }
        return null;
    }
    
    /**
     * Clears the achievement cache.
     */
    public void clearCache() {
        achievementCache.clear();
        playerAchievementsCache.clear();
    }
}