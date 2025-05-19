// ./src/main/java/com/minecraft/example/service/StatsService.java
package com.minecraft.example.service;

import com.minecraft.example.model.PlayerStats;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for player statistics operations
 * Demonstrates Core-Utils service registry pattern
 */
public interface StatsService {
    
    /**
     * Get statistics for a player
     * @param playerUuid Player UUID
     * @return Optional containing PlayerStats if found
     */
    Optional<PlayerStats> getPlayerStats(UUID playerUuid);
    
    /**
     * Save player statistics
     * @param stats PlayerStats to save
     * @return true if successful
     */
    boolean savePlayerStats(PlayerStats stats);
    
    /**
     * Get statistics for a player by name
     * @param playerName Player name
     * @return Optional containing PlayerStats if found
     */
    Optional<PlayerStats> getPlayerStatsByName(String playerName);
    
    /**
     * Update a specific statistic for a player
     * @param playerUuid Player UUID
     * @param statName Name of the statistic to update
     * @param value New value
     * @return true if successful
     */
    boolean updateStat(UUID playerUuid, String statName, int value);
    
    /**
     * Increment a specific statistic for a player
     * @param playerUuid Player UUID
     * @param statName Name of the statistic to increment
     * @return true if successful
     */
    boolean incrementStat(UUID playerUuid, String statName);
    
    /**
     * Get top players for a specific statistic
     * @param statName Name of the statistic to rank by
     * @param limit Maximum number of players to return
     * @return List of PlayerStats ordered by the specified statistic
     */
    List<PlayerStats> getTopPlayers(String statName, int limit);
    
    /**
     * Reset a player's statistics
     * @param playerUuid Player UUID
     * @return true if successful
     */
    boolean resetStats(UUID playerUuid);
    
    /**
     * Update player's time played
     * Uses a transaction to ensure atomicity
     * @param playerUuid Player UUID
     * @param additionalSeconds Seconds to add to time played
     * @return true if successful
     */
    boolean updateTimePlayed(UUID playerUuid, int additionalSeconds);
}