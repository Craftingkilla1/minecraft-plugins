// ./Example-Plugin/src/main/java/com/minecraft/example/core/services/impl/DefaultAchievementService.java
package com.minecraft.example.core.services.impl;

import com.minecraft.core.api.CoreAPI;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.ExamplePlugin;
import com.minecraft.example.core.services.AchievementService;
import com.minecraft.example.core.services.NotificationService;
import com.minecraft.example.core.services.StatsService;
import com.minecraft.example.sql.dao.AchievementDAO;
import com.minecraft.example.sql.dao.PlayerAchievementDAO;
import com.minecraft.example.sql.dao.PlayerDAO;
import com.minecraft.example.sql.models.Achievement;
import com.minecraft.example.sql.models.PlayerAchievement;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Default implementation of the AchievementService.
 * Demonstrates Core-Utils service implementation.
 */
public class DefaultAchievementService implements AchievementService {

    private final ExamplePlugin plugin;
    private final Map<String, Map<String, Object>> achievementDefinitions;
    
    /**
     * Constructs a new DefaultAchievementService.
     *
     * @param plugin The plugin instance
     */
    public DefaultAchievementService(ExamplePlugin plugin) {
        this.plugin = plugin;
        this.achievementDefinitions = loadAchievementDefinitions();
    }
    
    /**
     * Loads achievement definitions from configuration.
     *
     * @return A map of achievement IDs to their definitions
     */
    private Map<String, Map<String, Object>> loadAchievementDefinitions() {
        Map<String, Map<String, Object>> definitions = new HashMap<>();
        FileConfiguration config = plugin.getConfig();
        
        // Load from messages.yml
        Map<String, Object> achievementsSection = plugin.getConfigManager().getMessages().getConfig().getConfigurationSection("achievements").getValues(false);
        
        for (Map.Entry<String, Object> entry : achievementsSection.entrySet()) {
            String achievementId = entry.getKey();
            
            // Skip non-achievement entries
            if (achievementId.equals("unlocked") || achievementId.equals("global_announce")) {
                continue;
            }
            
            // Get achievement details
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> details = (Map<String, Object>) entry.getValue();
                definitions.put(achievementId, details);
            }
        }
        
        return definitions;
    }
    
    @Override
    public boolean awardAchievement(Player player, String achievementId) {
        UUID uuid = player.getUniqueId();
        
        // Check if the achievement exists
        if (!achievementDefinitions.containsKey(achievementId)) {
            LogUtil.warning("Attempted to award non-existent achievement: " + achievementId);
            return false;
        }
        
        // If database is enabled, award in database
        if (plugin.isDatabaseEnabled()) {
            try {
                // Get achievement DAO
                PlayerAchievementDAO achievementDAO = plugin.getPlayerAchievementDAO();
                
                // Check if player already has this achievement
                if (achievementDAO.hasAchievement(uuid, achievementId)) {
                    return false; // Already has it
                }
                
                // Award the achievement
                boolean awarded = achievementDAO.awardAchievement(uuid, achievementId);
                
                if (awarded) {
                    // Send notification
                    Map<String, Object> details = achievementDefinitions.get(achievementId);
                    String name = (String) details.get("name");
                    String description = (String) details.get("description");
                    
                    NotificationService notificationService = CoreAPI.Services.get(NotificationService.class);
                    if (notificationService != null) {
                        notificationService.sendAchievementNotification(player, name, description);
                        notificationService.broadcastAchievement(player, name);
                    }
                    
                    // Give rewards if enabled
                    if (plugin.getConfig().getBoolean("achievements.enable_rewards", true)) {
                        giveAchievementReward(player, achievementId);
                    }
                }
                
                return awarded;
            } catch (Exception e) {
                LogUtil.severe("Error awarding achievement to player: " + e.getMessage());
                return false;
            }
        } else {
            // If database is disabled, just send notification
            Map<String, Object> details = achievementDefinitions.get(achievementId);
            String name = (String) details.get("name");
            String description = (String) details.get("description");
            
            NotificationService notificationService = CoreAPI.Services.get(NotificationService.class);
            if (notificationService != null) {
                notificationService.sendAchievementNotification(player, name, description);
                notificationService.broadcastAchievement(player, name);
            }
            
            return true;
        }
    }
    
    /**
     * Gives a reward to a player for achieving an achievement.
     *
     * @param player The player
     * @param achievementId The achievement ID
     */
    private void giveAchievementReward(Player player, String achievementId) {
        // This would typically give an item, money, experience, etc.
        // For now, just give a small amount of experience
        player.giveExp(50);
    }
    
    @Override
    public boolean awardAchievement(UUID uuid, String achievementId) {
        // Check if the achievement exists
        if (!achievementDefinitions.containsKey(achievementId)) {
            LogUtil.warning("Attempted to award non-existent achievement: " + achievementId);
            return false;
        }
        
        // If player is online, use the player version
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            return awardAchievement(player, achievementId);
        }
        
        // Otherwise, if database is enabled, award in database
        if (plugin.isDatabaseEnabled()) {
            try {
                // Get achievement DAO
                PlayerAchievementDAO achievementDAO = plugin.getPlayerAchievementDAO();
                
                // Check if player already has this achievement
                if (achievementDAO.hasAchievement(uuid, achievementId)) {
                    return false; // Already has it
                }
                
                // Award the achievement
                return achievementDAO.awardAchievement(uuid, achievementId);
            } catch (Exception e) {
                LogUtil.severe("Error awarding achievement to player: " + e.getMessage());
                return false;
            }
        }
        
        return false;
    }
    
    @Override
    public CompletableFuture<Boolean> awardAchievementAsync(Player player, String achievementId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        UUID uuid = player.getUniqueId();
        
        // Check if the achievement exists
        if (!achievementDefinitions.containsKey(achievementId)) {
            LogUtil.warning("Attempted to award non-existent achievement: " + achievementId);
            future.complete(false);
            return future;
        }
        
        // If database is enabled, award in database
        if (plugin.isDatabaseEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    // Get achievement DAO
                    PlayerAchievementDAO achievementDAO = plugin.getPlayerAchievementDAO();
                    
                    // Check if player already has this achievement
                    if (achievementDAO.hasAchievement(uuid, achievementId)) {
                        future.complete(false); // Already has it
                        return;
                    }
                    
                    // Award the achievement
                    boolean awarded = achievementDAO.awardAchievement(uuid, achievementId);
                    
                    if (awarded) {
                        // Back to main thread for Bukkit API
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            // Send notification
                            Map<String, Object> details = achievementDefinitions.get(achievementId);
                            String name = (String) details.get("name");
                            String description = (String) details.get("description");
                            
                            NotificationService notificationService = CoreAPI.Services.get(NotificationService.class);
                            if (notificationService != null) {
                                notificationService.sendAchievementNotification(player, name, description);
                                notificationService.broadcastAchievement(player, name);
                            }
                            
                            // Give rewards if enabled
                            if (plugin.getConfig().getBoolean("achievements.enable_rewards", true)) {
                                giveAchievementReward(player, achievementId);
                            }
                            
                            future.complete(true);
                        });
                    } else {
                        future.complete(false);
                    }
                } catch (Exception e) {
                    LogUtil.severe("Error awarding achievement to player async: " + e.getMessage());
                    future.complete(false);
                }
            });
        } else {
            // If database is disabled, just send notification on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                Map<String, Object> details = achievementDefinitions.get(achievementId);
                String name = (String) details.get("name");
                String description = (String) details.get("description");
                
                NotificationService notificationService = CoreAPI.Services.get(NotificationService.class);
                if (notificationService != null) {
                    notificationService.sendAchievementNotification(player, name, description);
                    notificationService.broadcastAchievement(player, name);
                }
                
                future.complete(true);
            });
        }
        
        return future;
    }
    
    @Override
    public boolean hasAchievement(Player player, String achievementId) {
        UUID uuid = player.getUniqueId();
        return hasAchievement(uuid, achievementId);
    }
    
    @Override
    public boolean hasAchievement(UUID uuid, String achievementId) {
        // If database is enabled, check in database
        if (plugin.isDatabaseEnabled()) {
            try {
                // Get achievement DAO
                PlayerAchievementDAO achievementDAO = plugin.getPlayerAchievementDAO();
                return achievementDAO.hasAchievement(uuid, achievementId);
            } catch (Exception e) {
                LogUtil.severe("Error checking if player has achievement: " + e.getMessage());
                return false;
            }
        }
        
        return false;
    }
    
    @Override
    public List<String> getPlayerAchievements(Player player) {
        UUID uuid = player.getUniqueId();
        return getPlayerAchievements(uuid);
    }
    
    @Override
    public List<String> getPlayerAchievements(UUID uuid) {
        List<String> achievements = new ArrayList<>();
        
        // If database is enabled, get from database
        if (plugin.isDatabaseEnabled()) {
            try {
                // Get achievement DAO
                PlayerAchievementDAO achievementDAO = plugin.getPlayerAchievementDAO();
                List<PlayerAchievement> playerAchievements = achievementDAO.findByPlayerUuid(uuid);
                
                for (PlayerAchievement achievement : playerAchievements) {
                    achievements.add(achievement.getAchievementIdentifier());
                }
            } catch (Exception e) {
                LogUtil.severe("Error getting player achievements: " + e.getMessage());
            }
        }
        
        return achievements;
    }
    
    @Override
    public Map<String, String> getPlayerAchievementsWithNames(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, String> achievements = new LinkedHashMap<>();
        
        // If database is enabled, get from database
        if (plugin.isDatabaseEnabled()) {
            try {
                // Get achievement DAO
                PlayerAchievementDAO achievementDAO = plugin.getPlayerAchievementDAO();
                List<PlayerAchievement> playerAchievements = achievementDAO.findByPlayerUuid(uuid);
                
                for (PlayerAchievement achievement : playerAchievements) {
                    achievements.put(achievement.getAchievementIdentifier(), achievement.getAchievementName());
                }
            } catch (Exception e) {
                LogUtil.severe("Error getting player achievements with names: " + e.getMessage());
            }
        }
        
        return achievements;
    }
    
    @Override
    public Map<String, Object> getAchievementDetails(String achievementId) {
        // Check local definitions first
        if (achievementDefinitions.containsKey(achievementId)) {
            return new HashMap<>(achievementDefinitions.get(achievementId));
        }
        
        // If database is enabled, try to get from database
        if (plugin.isDatabaseEnabled()) {
            try {
                // Get achievement DAO
                AchievementDAO achievementDAO = plugin.getAchievementDAO();
                Optional<Achievement> achievement = achievementDAO.findByIdentifier(achievementId);
                
                if (achievement.isPresent()) {
                    Map<String, Object> details = new HashMap<>();
                    Achievement a = achievement.get();
                    details.put("name", a.getName());
                    details.put("description", a.getDescription());
                    details.put("icon_material", a.getIconMaterial());
                    return details;
                }
            } catch (Exception e) {
                LogUtil.severe("Error getting achievement details: " + e.getMessage());
            }
        }
        
        return new HashMap<>();
    }
    
    @Override
    public Map<String, String> getAllAchievements() {
        Map<String, String> achievements = new LinkedHashMap<>();
        
        // Add from local definitions
        for (Map.Entry<String, Map<String, Object>> entry : achievementDefinitions.entrySet()) {
            String achievementId = entry.getKey();
            String name = (String) entry.getValue().get("name");
            achievements.put(achievementId, name);
        }
        
        // If database is enabled, add from database
        if (plugin.isDatabaseEnabled()) {
            try {
                // Get achievement DAO
                AchievementDAO achievementDAO = plugin.getAchievementDAO();
                List<Achievement> databaseAchievements = achievementDAO.findAll();
                
                for (Achievement achievement : databaseAchievements) {
                    // Only add if not already in the map (local definitions take precedence)
                    if (!achievements.containsKey(achievement.getIdentifier())) {
                        achievements.put(achievement.getIdentifier(), achievement.getName());
                    }
                }
            } catch (Exception e) {
                LogUtil.severe("Error getting all achievements: " + e.getMessage());
            }
        }
        
        return achievements;
    }
    
    @Override
    public int getAchievementCount(Player player) {
        UUID uuid = player.getUniqueId();
        
        // If database is enabled, get count from database
        if (plugin.isDatabaseEnabled()) {
            try {
                // Get achievement DAO
                PlayerAchievementDAO achievementDAO = plugin.getPlayerAchievementDAO();
                return achievementDAO.countAchievements(uuid);
            } catch (Exception e) {
                LogUtil.severe("Error getting achievement count: " + e.getMessage());
                return 0;
            }
        }
        
        return 0;
    }
    
    @Override
    public Map<String, Boolean> getAchievementProgress(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, Boolean> progress = new LinkedHashMap<>();
        
        // Get all achievement IDs
        Set<String> allAchievementIds = getAllAchievements().keySet();
        
        // Initialize all as not earned
        for (String achievementId : allAchievementIds) {
            progress.put(achievementId, false);
        }
        
        // If database is enabled, get earned achievements from database
        if (plugin.isDatabaseEnabled()) {
            try {
                // Get achievement DAO
                PlayerAchievementDAO achievementDAO = plugin.getPlayerAchievementDAO();
                List<PlayerAchievement> playerAchievements = achievementDAO.findByPlayerUuid(uuid);
                
                for (PlayerAchievement achievement : playerAchievements) {
                    progress.put(achievement.getAchievementIdentifier(), true);
                }
            } catch (Exception e) {
                LogUtil.severe("Error getting achievement progress: " + e.getMessage());
            }
        }
        
        return progress;
    }
    
    @Override
    public boolean removeAchievement(Player player, String achievementId) {
        UUID uuid = player.getUniqueId();
        
        // If database is enabled, remove from database
        if (plugin.isDatabaseEnabled()) {
            try {
                // Get achievement DAO
                PlayerAchievementDAO achievementDAO = plugin.getPlayerAchievementDAO();
                return achievementDAO.removeAchievement(uuid, achievementId);
            } catch (Exception e) {
                LogUtil.severe("Error removing achievement from player: " + e.getMessage());
                return false;
            }
        }
        
        return false;
    }
    
    @Override
    public boolean removeAllAchievements(Player player) {
        UUID uuid = player.getUniqueId();
        
        // If database is enabled, remove all from database
        if (plugin.isDatabaseEnabled()) {
            try {
                // Get player and achievement DAOs
                PlayerDAO playerDAO = plugin.getPlayerDAO();
                Optional<Integer> playerId = playerDAO.getPlayerIdByUuid(uuid);
                
                if (!playerId.isPresent()) {
                    return false;
                }
                
                // Delete all player achievements using a direct query
                int deleted = plugin.getDatabase().update(
                        "DELETE FROM player_achievements WHERE player_id = ?",
                        playerId.get()
                );
                
                return deleted >= 0;
            } catch (Exception e) {
                LogUtil.severe("Error removing all achievements from player: " + e.getMessage());
                return false;
            }
        }
        
        return false;
    }
    
    @Override
    public boolean createAchievement(String achievementId, String name, String description, String iconMaterial) {
        // If database is enabled, create in database
        if (plugin.isDatabaseEnabled()) {
            try {
                // Get achievement DAO
                AchievementDAO achievementDAO = plugin.getAchievementDAO();
                
                // Check if achievement already exists
                if (achievementDAO.exists(achievementId)) {
                    return false; // Already exists
                }
                
                // Create achievement
                Achievement achievement = new Achievement(
                        achievementId,
                        name,
                        description,
                        iconMaterial
                );
                
                return achievementDAO.save(achievement);
            } catch (Exception e) {
                LogUtil.severe("Error creating achievement: " + e.getMessage());
                return false;
            }
        }
        
        return false;
    }
    
    @Override
    public List<String> getRecentAchievements(Player player, int limit) {
        UUID uuid = player.getUniqueId();
        List<String> achievements = new ArrayList<>();
        
        // If database is enabled, get from database
        if (plugin.isDatabaseEnabled()) {
            try {
                // Get achievement DAO
                PlayerAchievementDAO achievementDAO = plugin.getPlayerAchievementDAO();
                List<PlayerAchievement> recentAchievements = achievementDAO.getRecentAchievements(uuid, limit);
                
                for (PlayerAchievement achievement : recentAchievements) {
                    achievements.add(achievement.getAchievementIdentifier());
                }
            } catch (Exception e) {
                LogUtil.severe("Error getting recent achievements: " + e.getMessage());
            }
        }
        
        return achievements;
    }
    
    @Override
    public Map<String, String> getGlobalRecentAchievements(int limit) {
        Map<String, String> achievements = new LinkedHashMap<>();
        
        // If database is enabled, get from database
        if (plugin.isDatabaseEnabled()) {
            try {
                // Get achievement DAO
                PlayerAchievementDAO achievementDAO = plugin.getPlayerAchievementDAO();
                List<PlayerAchievement> recentAchievements = achievementDAO.getGlobalRecentAchievements(limit);
                
                // Get player DAO
                PlayerDAO playerDAO = plugin.getPlayerDAO();
                
                for (PlayerAchievement achievement : recentAchievements) {
                    // Get player name
                    Optional<com.minecraft.example.sql.models.Player> player = playerDAO.findByPlayerId(achievement.getPlayerId());
                    if (!player.isPresent()) {
                        continue;
                    }
                    
                    String playerName = player.get().getName();
                    achievements.put(playerName, achievement.getAchievementIdentifier());
                }
            } catch (Exception e) {
                LogUtil.severe("Error getting global recent achievements: " + e.getMessage());
            }
        }
        
        return achievements;
    }
    
    @Override
    public List<String> checkAndAwardAchievements(Player player) {
        List<String> awardedAchievements = new ArrayList<>();
        
        // Get stats service
        StatsService statsService = CoreAPI.Services.get(StatsService.class);
        if (statsService == null) {
            return awardedAchievements;
        }
        
        // Get player stats
        Map<String, Integer> stats = statsService.getAllStats(player);
        
        // Check each achievement condition
        
        // Blocks broken achievements
        int blocksBroken = stats.getOrDefault("blocks_broken", 0);
        if (blocksBroken >= 1000 && !hasAchievement(player, "blocks_broken_1000")) {
            if (awardAchievement(player, "blocks_broken_1000")) {
                awardedAchievements.add("blocks_broken_1000");
            }
        } else if (blocksBroken >= 100 && !hasAchievement(player, "blocks_broken_100")) {
            if (awardAchievement(player, "blocks_broken_100")) {
                awardedAchievements.add("blocks_broken_100");
            }
        }
        
        // Blocks placed achievements
        int blocksPlaced = stats.getOrDefault("blocks_placed", 0);
        if (blocksPlaced >= 1000 && !hasAchievement(player, "blocks_placed_1000")) {
            if (awardAchievement(player, "blocks_placed_1000")) {
                awardedAchievements.add("blocks_placed_1000");
            }
        } else if (blocksPlaced >= 100 && !hasAchievement(player, "blocks_placed_100")) {
            if (awardAchievement(player, "blocks_placed_100")) {
                awardedAchievements.add("blocks_placed_100");
            }
        }
        
        // Player kills achievement
        int playerKills = stats.getOrDefault("player_kills", 0);
        if (playerKills >= 10 && !hasAchievement(player, "player_kills_10")) {
            if (awardAchievement(player, "player_kills_10")) {
                awardedAchievements.add("player_kills_10");
            }
        }
        
        // Mob kills achievement
        int mobKills = stats.getOrDefault("mob_kills", 0);
        if (mobKills >= 100 && !hasAchievement(player, "mob_kills_100")) {
            if (awardAchievement(player, "mob_kills_100")) {
                awardedAchievements.add("mob_kills_100");
            }
        }
        
        // Playtime achievements
        int playtimeSeconds = stats.getOrDefault("playtime_seconds", 0);
        int playtimeHours = playtimeSeconds / 3600;
        
        if (playtimeHours >= 10 && !hasAchievement(player, "playtime_10_hours")) {
            if (awardAchievement(player, "playtime_10_hours")) {
                awardedAchievements.add("playtime_10_hours");
            }
        } else if (playtimeHours >= 1 && !hasAchievement(player, "playtime_1_hour")) {
            if (awardAchievement(player, "playtime_1_hour")) {
                awardedAchievements.add("playtime_1_hour");
            }
        }
        
        // Distance achievements
        double distance = statsService.getDistance(player);
        double distanceKm = distance / 1000.0;
        
        if (distanceKm >= 10 && !hasAchievement(player, "distance_10_km")) {
            if (awardAchievement(player, "distance_10_km")) {
                awardedAchievements.add("distance_10_km");
            }
        } else if (distanceKm >= 1 && !hasAchievement(player, "distance_1_km")) {
            if (awardAchievement(player, "distance_1_km")) {
                awardedAchievements.add("distance_1_km");
            }
        }
        
        return awardedAchievements;
    }
    
    @Override
    public void handlePlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Award "first_join" achievement if it's their first time
        if (plugin.isDatabaseEnabled()) {
            try {
                if (!hasAchievement(player, "first_join")) {
                    // Run async to avoid lag on join
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        awardAchievement(player, "first_join");
                    });
                }
                
                // Run an async task to check achievements
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    // Wait a short time for stats to load properly
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // Back to main thread for Bukkit API
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        checkAndAwardAchievements(player);
                    });
                });
            } catch (Exception e) {
                LogUtil.severe("Error handling player join (achievements): " + e.getMessage());
            }
        }
    }
}