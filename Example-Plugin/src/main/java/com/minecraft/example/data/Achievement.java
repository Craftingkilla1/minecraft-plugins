// ./Example-Plugin/src/main/java/com/minecraft/example/data/Achievement.java
package com.minecraft.example.data;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.bukkit.Material;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an achievement that can be earned by players.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Achievement {
    
    /**
     * The unique ID of the achievement.
     */
    private String id;
    
    /**
     * The display name of the achievement.
     */
    private String name;
    
    /**
     * The description of the achievement.
     */
    private String description;
    
    /**
     * The material to use for the achievement icon.
     */
    private Material icon;
    
    /**
     * The category of the achievement.
     */
    private String category;
    
    /**
     * Whether the achievement is secret (hidden until earned).
     */
    private boolean secret;
    
    /**
     * The criteria for earning the achievement.
     * This is a map of statistic names to required values.
     */
    private Map<String, Integer> criteria;
    
    /**
     * The timestamp when the achievement was earned by a player.
     * This is only used when returning achievements for a specific player.
     */
    private LocalDateTime earnedDate;
    
    /**
     * The UUID of the player who earned the achievement.
     * This is only used when returning achievements for a specific player.
     */
    private UUID playerId;
    
    /**
     * Checks if a player has met the criteria for this achievement.
     *
     * @param playerStats The player's statistics
     * @return True if the player has met the criteria, false otherwise
     */
    public boolean checkCriteria(Map<String, Integer> playerStats) {
        if (criteria == null || criteria.isEmpty() || playerStats == null) {
            return false;
        }
        
        // Check each criterion
        for (Map.Entry<String, Integer> entry : criteria.entrySet()) {
            String statName = entry.getKey();
            int requiredValue = entry.getValue();
            int playerValue = playerStats.getOrDefault(statName, 0);
            
            if (playerValue < requiredValue) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Calculates the progress towards this achievement.
     *
     * @param playerStats The player's statistics
     * @return The progress as a value between 0.0 and 1.0
     */
    public double calculateProgress(Map<String, Integer> playerStats) {
        if (criteria == null || criteria.isEmpty() || playerStats == null) {
            return 0.0;
        }
        
        double totalProgress = 0.0;
        int criteriaCount = criteria.size();
        
        // Calculate progress for each criterion
        for (Map.Entry<String, Integer> entry : criteria.entrySet()) {
            String statName = entry.getKey();
            int requiredValue = entry.getValue();
            int playerValue = playerStats.getOrDefault(statName, 0);
            
            double criterionProgress = Math.min(1.0, (double) playerValue / requiredValue);
            totalProgress += criterionProgress;
        }
        
        return totalProgress / criteriaCount;
    }
    
    /**
     * Creates an Achievement.PlayerProgress object to track a player's progress.
     *
     * @param playerId     The player's UUID
     * @param progress     The player's progress (0.0-1.0)
     * @param isCompleted  Whether the achievement is completed
     * @param earnedDate   When the achievement was earned (null if not earned)
     * @return A new Achievement.PlayerProgress instance
     */
    public PlayerProgress createProgress(UUID playerId, double progress, boolean isCompleted, LocalDateTime earnedDate) {
        return new PlayerProgress(playerId, this.id, progress, isCompleted, earnedDate);
    }
    
    /**
     * Represents a player's progress towards an achievement.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerProgress {
        private UUID playerId;
        private String achievementId;
        private double progress;
        private boolean completed;
        private LocalDateTime earnedDate;
    }
}