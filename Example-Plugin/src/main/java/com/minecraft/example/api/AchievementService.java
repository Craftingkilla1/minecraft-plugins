// ./Example-Plugin/src/main/java/com/minecraft/example/api/AchievementService.java
package com.minecraft.example.api;

import com.minecraft.example.data.Achievement;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing player achievements.
 */
public interface AchievementService {
    
    /**
     * Loads all achievements from the configuration file.
     */
    void loadAchievements();
    
    /**
     * Gets all available achievements.
     *
     * @return A list of all achievements
     */
    List<Achievement> getAllAchievements();
    
    /**
     * Gets an achievement by its unique ID.
     *
     * @param id The achievement ID
     * @return The achievement, or null if not found
     */
    Achievement getAchievement(String id);
    
    /**
     * Checks if a player has earned a specific achievement.
     *
     * @param player The player
     * @param id     The achievement ID
     * @return True if the player has earned the achievement, false otherwise
     */
    boolean hasAchievement(Player player, String id);
    
    /**
     * Checks if a player has earned a specific achievement asynchronously.
     *
     * @param player The player
     * @param id     The achievement ID
     * @return A CompletableFuture that will complete with true if the player has the achievement
     */
    CompletableFuture<Boolean> hasAchievementAsync(Player player, String id);
    
    /**
     * Gets all achievements earned by a player.
     *
     * @param player The player
     * @return A list of achievements earned by the player
     */
    List<Achievement> getPlayerAchievements(Player player);
    
    /**
     * Gets all achievements earned by a player asynchronously.
     *
     * @param player The player
     * @return A CompletableFuture that will complete with the player's achievements
     */
    CompletableFuture<List<Achievement>> getPlayerAchievementsAsync(Player player);
    
    /**
     * Gets all achievements earned by a player by UUID.
     *
     * @param uuid The player's UUID
     * @return A list of achievements earned by the player
     */
    List<Achievement> getPlayerAchievements(UUID uuid);
    
    /**
     * Awards an achievement to a player.
     *
     * @param player The player
     * @param id     The achievement ID
     * @return True if the achievement was awarded, false if the player already had it
     */
    boolean awardAchievement(Player player, String id);
    
    /**
     * Awards an achievement to a player asynchronously.
     *
     * @param player The player
     * @param id     The achievement ID
     * @return A CompletableFuture that will complete with true if the achievement was awarded
     */
    CompletableFuture<Boolean> awardAchievementAsync(Player player, String id);
    
    /**
     * Checks if a player should earn any achievements based on their current statistics.
     *
     * @param player The player
     */
    void checkAchievements(Player player);
    
    /**
     * Gets the progress of a player towards an achievement.
     *
     * @param player The player
     * @param id     The achievement ID
     * @return The progress as a value between 0.0 and 1.0
     */
    double getAchievementProgress(Player player, String id);
    
    /**
     * Gets the progress of a player towards all achievements.
     *
     * @param player The player
     * @return A map of achievement IDs to progress values
     */
    Map<String, Double> getAllAchievementProgress(Player player);
    
    /**
     * Gets the total number of achievements earned by a player.
     *
     * @param player The player
     * @return The number of achievements earned
     */
    int getAchievementCount(Player player);
    
    /**
     * Gets the completion percentage for all achievements for a player.
     *
     * @param player The player
     * @return The percentage of achievements completed (0-100)
     */
    double getCompletionPercentage(Player player);
    
    /**
     * Resets all achievements for a player.
     *
     * @param player The player
     * @return True if the achievements were reset, false otherwise
     */
    boolean resetAchievements(Player player);
    
    /**
     * Gets the most recently earned achievements for a player.
     *
     * @param player The player
     * @param limit  The maximum number of achievements to return
     * @return A list of recently earned achievements
     */
    List<Achievement> getRecentAchievements(Player player, int limit);
}