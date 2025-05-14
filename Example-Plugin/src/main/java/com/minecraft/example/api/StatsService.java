// ./Example-Plugin/src/main/java/com/minecraft/example/api/StatsService.java
package com.minecraft.example.api;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for tracking and managing player statistics.
 */
public interface StatsService {
    
    /**
     * Increments a player statistic by 1.
     *
     * @param player    The player
     * @param statName  The name of the statistic
     */
    void incrementStat(Player player, String statName);
    
    /**
     * Increments a player statistic by the specified amount.
     *
     * @param player    The player
     * @param statName  The name of the statistic
     * @param amount    The amount to increment by
     */
    void incrementStat(Player player, String statName, int amount);
    
    /**
     * Sets a player statistic to the specified value.
     *
     * @param player    The player
     * @param statName  The name of the statistic
     * @param value     The value to set
     */
    void setStat(Player player, String statName, int value);
    
    /**
     * Sets a player statistic to the specified value asynchronously.
     *
     * @param player    The player
     * @param statName  The name of the statistic
     * @param value     The value to set
     * @return          A CompletableFuture that completes when the operation is finished
     */
    CompletableFuture<Void> setStatAsync(Player player, String statName, int value);
    
    /**
     * Gets the value of a player statistic.
     *
     * @param player    The player
     * @param statName  The name of the statistic
     * @return          The value of the statistic, or 0 if not found
     */
    int getStat(Player player, String statName);
    
    /**
     * Gets the value of a player statistic asynchronously.
     *
     * @param player    The player
     * @param statName  The name of the statistic
     * @return          A CompletableFuture that will complete with the statistic value
     */
    CompletableFuture<Integer> getStatAsync(Player player, String statName);
    
    /**
     * Gets all statistics for a player.
     *
     * @param player    The player
     * @return          A map of statistic names to values
     */
    Map<String, Integer> getAllStats(Player player);
    
    /**
     * Gets all statistics for a player asynchronously.
     *
     * @param player    The player
     * @return          A CompletableFuture that will complete with the statistics map
     */
    CompletableFuture<Map<String, Integer>> getAllStatsAsync(Player player);
    
    /**
     * Gets all statistics for a player by UUID.
     *
     * @param uuid      The player's UUID
     * @return          A map of statistic names to values
     */
    Map<String, Integer> getAllStats(UUID uuid);
    
    /**
     * Gets all statistics for a player by UUID asynchronously.
     *
     * @param uuid      The player's UUID
     * @return          A CompletableFuture that will complete with the statistics map
     */
    CompletableFuture<Map<String, Integer>> getAllStatsAsync(UUID uuid);
    
    /**
     * Resets a specific statistic for a player.
     *
     * @param player    The player
     * @param statName  The name of the statistic to reset
     * @return          True if the statistic was reset, false otherwise
     */
    boolean resetStat(Player player, String statName);
    
    /**
     * Resets all statistics for a player.
     *
     * @param player    The player
     * @return          True if the statistics were reset, false otherwise
     */
    boolean resetAllStats(Player player);
    
    /**
     * Initializes statistics for a new player.
     *
     * @param player    The player to initialize
     */
    void initializePlayer(Player player);
    
    /**
     * Updates the last seen time for a player.
     *
     * @param player    The player
     */
    void updatePlayerActivity(Player player);
    
    /**
     * Gets the top players for a specific statistic.
     *
     * @param statName  The name of the statistic
     * @param limit     The maximum number of players to return
     * @return          A map of player names to statistic values, ordered by value
     */
    Map<String, Integer> getTopPlayers(String statName, int limit);
    
    /**
     * Gets the top players for a specific statistic asynchronously.
     *
     * @param statName  The name of the statistic
     * @param limit     The maximum number of players to return
     * @return          A CompletableFuture that will complete with the top players map
     */
    CompletableFuture<Map<String, Integer>> getTopPlayersAsync(String statName, int limit);
    
    /**
     * Gets the rank of a player for a specific statistic.
     *
     * @param player    The player
     * @param statName  The name of the statistic
     * @return          The player's rank (1-based), or -1 if not ranked
     */
    int getPlayerRank(Player player, String statName);
    
    /**
     * Updates multiple statistics for a player in a single batch operation.
     *
     * @param player    The player
     * @param stats     A map of statistic names to values
     */
    void updateStats(Player player, Map<String, Integer> stats);
    
    /**
     * Updates multiple statistics for a player in a single batch operation asynchronously.
     *
     * @param player    The player
     * @param stats     A map of statistic names to values
     * @return          A CompletableFuture that completes when the operation is finished
     */
    CompletableFuture<Void> updateStatsAsync(Player player, Map<String, Integer> stats);
}