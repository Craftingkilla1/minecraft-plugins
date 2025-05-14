// ./Example-Plugin/src/main/java/com/minecraft/example/data/Leaderboard.java
package com.minecraft.example.data;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.bukkit.Material;

import java.time.LocalDateTime;

/**
 * Represents a leaderboard that tracks player rankings for a statistic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Leaderboard {
    
    /**
     * The unique ID of the leaderboard.
     */
    private String id;
    
    /**
     * The display name of the leaderboard.
     */
    private String displayName;
    
    /**
     * The name of the statistic being tracked.
     */
    private String statName;
    
    /**
     * The material to use for the leaderboard icon.
     */
    private Material icon;
    
    /**
     * The category of the leaderboard.
     */
    private String category;
    
    /**
     * Whether the leaderboard is reversed (smaller values are better).
     */
    private boolean reversed;
    
    /**
     * How often the leaderboard should be updated (in minutes).
     */
    private int updateInterval;
    
    /**
     * When the leaderboard was last updated.
     */
    private LocalDateTime lastUpdated;
    
    /**
     * The description of the leaderboard.
     */
    private String description;
    
    /**
     * Checks if the leaderboard needs to be updated.
     *
     * @return True if the leaderboard needs to be updated, false otherwise
     */
    public boolean needsUpdate() {
        if (lastUpdated == null) {
            return true;
        }
        
        LocalDateTime updateTime = lastUpdated.plusMinutes(updateInterval);
        return LocalDateTime.now().isAfter(updateTime);
    }
    
    /**
     * Updates the last updated timestamp to now.
     */
    public void markAsUpdated() {
        this.lastUpdated = LocalDateTime.now();
    }
}