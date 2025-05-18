// ./Example-Plugin/src/main/java/com/minecraft/example/core/services/AchievementService.java
package com.minecraft.example.core.services;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for achievement management.
 * Demonstrates Core-Utils service registry system.
 */
public interface AchievementService {
    
    /**
     * Awards an achievement to a player.
     *
     * @param player The player
     * @param achievementId The achievement identifier
     * @return True if awarded successfully, false if already awarded or error
     */
    boolean awardAchievement(Player player, String achievementId);
    
    /**
     * Awards an achievement to a player by UUID.
     *
     * @param uuid The player's UUID
     * @param achievementId The achievement identifier
     * @return True if awarded successfully, false if already awarded or error
     */
    boolean awardAchievement(UUID uuid, String achievementId);
    
    /**
     * Awards an achievement to a player asynchronously.
     *
     * @param player The player
     * @param achievementId The achievement identifier
     * @return A CompletableFuture that will contain true if awarded successfully
     */
    CompletableFuture<Boolean> awardAchievementAsync(Player player, String achievementId);
    
    /**
     * Checks if a player has a specific achievement.
     *
     * @param player The player
     * @param achievementId The achievement identifier
     * @return True if the player has the achievement, false otherwise
     */
    boolean hasAchievement(Player player, String achievementId);
    
    /**
     * Checks if a player has a specific achievement by UUID.
     *
     * @param uuid The player's UUID
     * @param achievementId The achievement identifier
     * @return True if the player has the achievement, false otherwise
     */
    boolean hasAchievement(UUID uuid, String achievementId);
    
    /**
     * Gets all achievements earned by a player.
     *
     * @param player The player
     * @return A list of achievement identifiers
     */
    List<String> getPlayerAchievements(Player player);
    
    /**
     * Gets all achievements earned by a player by UUID.
     *
     * @param uuid The player's UUID
     * @return A list of achievement identifiers
     */
    List<String> getPlayerAchievements(UUID uuid);
    
    /**
     * Gets all achievements with their details earned by a player.
     *
     * @param player The player
     * @return A map of achievement identifiers to their names
     */
    Map<String, String> getPlayerAchievementsWithNames(Player player);
    
    /**
     * Gets the details of an achievement.
     *
     * @param achievementId The achievement identifier
     * @return A map containing the achievement details (name, description, etc.)
     */
    Map<String, Object> getAchievementDetails(String achievementId);
    
    /**
     * Gets all available achievements.
     *
     * @return A map of achievement identifiers to their names
     */
    Map<String, String> getAllAchievements();
    
    /**
     * Gets the total number of achievements a player has earned.
     *
     * @param player The player
     * @return The number of achievements earned
     */
    int getAchievementCount(Player player);
    
    /**
     * Gets the progress of a player towards all achievements.
     *
     * @param player The player
     * @return A map of achievement identifiers to boolean values (true if earned)
     */
    Map<String, Boolean> getAchievementProgress(Player player);
    
    /**
     * Removes an achievement from a player.
     *
     * @param player The player
     * @param achievementId The achievement identifier
     * @return True if removed successfully, false otherwise
     */
    boolean removeAchievement(Player player, String achievementId);
    
    /**
     * Removes all achievements from a player.
     *
     * @param player The player
     * @return True if removed successfully, false otherwise
     */
    boolean removeAllAchievements(Player player);
    
    /**
     * Creates a new achievement.
     *
     * @param achievementId The achievement identifier
     * @param name The achievement name
     * @param description The achievement description
     * @param iconMaterial The icon material name
     * @return True if created successfully, false otherwise
     */
    boolean createAchievement(String achievementId, String name, String description, String iconMaterial);
    
    /**
     * Gets the most recently earned achievements by a player.
     *
     * @param player The player
     * @param limit The maximum number of achievements to return
     * @return A list of achievement identifiers, most recent first
     */
    List<String> getRecentAchievements(Player player, int limit);
    
    /**
     * Gets the most recently awarded achievements across all players.
     *
     * @param limit The maximum number of achievements to return
     * @return A map of player names to achievement identifiers, most recent first
     */
    Map<String, String> getGlobalRecentAchievements(int limit);
    
    /**
     * Checks for and awards achievements based on player stats.
     *
     * @param player The player
     * @return A list of achievement identifiers that were awarded
     */
    List<String> checkAndAwardAchievements(Player player);
    
    /**
     * Handles a player joining the server.
     *
     * @param player The player
     */
    void handlePlayerJoin(Player player);
}