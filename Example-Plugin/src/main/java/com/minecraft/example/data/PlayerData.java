// ./Example-Plugin/src/main/java/com/minecraft/example/data/PlayerData.java
package com.minecraft.example.data;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents player data including statistics and metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerData {
    
    /**
     * The player's UUID.
     */
    private UUID uuid;
    
    /**
     * The player's name.
     */
    private String name;
    
    /**
     * When the player first joined the server.
     */
    private LocalDateTime firstJoin;
    
    /**
     * When the player was last seen on the server.
     */
    private LocalDateTime lastSeen;
    
    /**
     * The player's total playtime in seconds.
     */
    private long playtime;
    
    /**
     * The player's statistics.
     */
    private Map<String, Integer> stats;
    
    /**
     * Creates a new PlayerData instance for a player.
     *
     * @param uuid The player's UUID
     * @param name The player's name
     * @return A new PlayerData instance
     */
    public static PlayerData create(UUID uuid, String name) {
        LocalDateTime now = LocalDateTime.now();
        return PlayerData.builder()
                .uuid(uuid)
                .name(name)
                .firstJoin(now)
                .lastSeen(now)
                .playtime(0)
                .stats(new HashMap<>())
                .build();
    }
    
    /**
     * Increments a statistic by 1.
     *
     * @param statName The statistic name
     */
    public void incrementStat(String statName) {
        incrementStat(statName, 1);
    }
    
    /**
     * Increments a statistic by the specified amount.
     *
     * @param statName The statistic name
     * @param amount   The amount to increment by
     */
    public void incrementStat(String statName, int amount) {
        if (stats == null) {
            stats = new HashMap<>();
        }
        
        int currentValue = stats.getOrDefault(statName, 0);
        stats.put(statName, currentValue + amount);
    }
    
    /**
     * Sets a statistic to the specified value.
     *
     * @param statName The statistic name
     * @param value    The value to set
     */
    public void setStat(String statName, int value) {
        if (stats == null) {
            stats = new HashMap<>();
        }
        
        stats.put(statName, value);
    }
    
    /**
     * Gets the value of a statistic.
     *
     * @param statName The statistic name
     * @return The value of the statistic, or 0 if not found
     */
    public int getStat(String statName) {
        if (stats == null) {
            return 0;
        }
        
        return stats.getOrDefault(statName, 0);
    }
    
    /**
     * Updates the last seen time to now.
     */
    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
    }
    
    /**
     * Adds playtime in seconds.
     *
     * @param seconds The seconds to add
     */
    public void addPlaytime(long seconds) {
        this.playtime += seconds;
    }
}