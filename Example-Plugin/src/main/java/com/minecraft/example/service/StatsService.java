// ./src/main/java/com/minecraft/example/service/StatsService.java
package com.minecraft.example.service;

import com.minecraft.example.model.PlayerStats;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing player statistics
 */
public interface StatsService {
    
    /**
     * Get player stats by UUID (synchronous)
     * 
     * @param uuid Player UUID
     * @return Optional with player stats if found
     */
    Optional<PlayerStats> getPlayerStats(UUID uuid);
    
    /**
     * Get player stats by UUID (asynchronous)
     * 
     * @param uuid Player UUID
     * @return CompletableFuture with Optional player stats
     */
    CompletableFuture<Optional<PlayerStats>> getPlayerStatsAsync(UUID uuid);
    
    /**
     * Get player stats by name (synchronous)
     * 
     * @param name Player name
     * @return Optional with player stats if found
     */
    Optional<PlayerStats> getPlayerStatsByName(String name);
    
    /**
     * Get top players for a specific stat
     * 
     * @param statField The name of the field to sort by (playtime_seconds, blocks_broken, etc.)
     * @param limit Maximum number of players to return
     * @return List of player stats ordered by the specified stat
     */
    List<PlayerStats> getTopPlayers(String statField, int limit);
    
    /**
     * Create or update player stats when a player joins
     * 
     * @param player The player who joined
     */
    void recordPlayerJoin(Player player);
    
    /**
     * Update player playtime when a player quits
     * 
     * @param player The player who quit
     * @param sessionSeconds The length of the player's session in seconds
     */
    void updatePlaytime(Player player, long sessionSeconds);
    
    /**
     * Increment the blocks broken counter for a player
     * 
     * @param player The player who broke a block
     */
    void incrementBlocksBroken(Player player);
    
    /**
     * Increment the blocks placed counter for a player
     * 
     * @param player The player who placed a block
     */
    void incrementBlocksPlaced(Player player);
    
    /**
     * Reset a specific stat for a player
     * 
     * @param uuid Player UUID
     * @param statField The name of the field to reset
     * @return true if successful, false otherwise
     */
    boolean resetStat(UUID uuid, String statField);
}