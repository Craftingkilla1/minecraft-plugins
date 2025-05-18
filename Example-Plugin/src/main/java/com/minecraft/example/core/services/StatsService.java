// ./Example-Plugin/src/main/java/com/minecraft/example/core/services/StatsService.java
package com.minecraft.example.core.services;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for player statistics management.
 * Demonstrates Core-Utils service registry system.
 */
public interface StatsService {
    
    /**
     * Increments a specific statistic for a player.
     *
     * @param player The player
     * @param statName The name of the stat
     * @param value The amount to increment by
     * @return True if successful, false otherwise
     */
    boolean incrementStat(Player player, String statName, int value);
    
    /**
     * Increments a specific statistic for a player by UUID.
     *
     * @param uuid The player's UUID
     * @param statName The name of the stat
     * @param value The amount to increment by
     * @return True if successful, false otherwise
     */
    boolean incrementStat(UUID uuid, String statName, int value);
    
    /**
     * Increments a specific statistic for a player asynchronously.
     *
     * @param player The player
     * @param statName The name of the stat
     * @param value The amount to increment by
     * @return A CompletableFuture that will contain true if successful
     */
    CompletableFuture<Boolean> incrementStatAsync(Player player, String statName, int value);
    
    /**
     * Gets the value of a specific statistic for a player.
     *
     * @param player The player
     * @param statName The name of the stat
     * @return The stat value, or 0 if not found
     */
    int getStat(Player player, String statName);
    
    /**
     * Gets the value of a specific statistic for a player by UUID.
     *
     * @param uuid The player's UUID
     * @param statName The name of the stat
     * @return The stat value, or 0 if not found
     */
    int getStat(UUID uuid, String statName);
    
    /**
     * Gets all statistics for a player.
     *
     * @param player The player
     * @return A map of stat names to their values
     */
    Map<String, Integer> getAllStats(Player player);
    
    /**
     * Gets all statistics for a player by UUID.
     *
     * @param uuid The player's UUID
     * @return A map of stat names to their values
     */
    Map<String, Integer> getAllStats(UUID uuid);
    
    /**
     * Gets all statistics for a player asynchronously.
     *
     * @param player The player
     * @return A CompletableFuture that will contain a map of stat names to their values
     */
    CompletableFuture<Map<String, Integer>> getAllStatsAsync(Player player);
    
    /**
     * Sets a specific statistic for a player.
     *
     * @param player The player
     * @param statName The name of the stat
     * @param value The new value
     * @return True if successful, false otherwise
     */
    boolean setStat(Player player, String statName, int value);
    
    /**
     * Resets all statistics for a player.
     *
     * @param player The player
     * @return True if successful, false otherwise
     */
    boolean resetStats(Player player);
    
    /**
     * Gets the top players for a specific statistic.
     *
     * @param statName The name of the stat
     * @param limit The maximum number of players to return
     * @return A map of player names to their stat values, sorted by value in descending order
     */
    Map<String, Integer> getTopPlayers(String statName, int limit);
    
    /**
     * Gets the total playtime for a player in minutes.
     *
     * @param player The player
     * @return The total playtime in minutes
     */
    int getPlaytimeMinutes(Player player);
    
    /**
     * Gets the formatted playtime for a player (e.g., "2h 30m").
     *
     * @param player The player
     * @return The formatted playtime
     */
    String getFormattedPlaytime(Player player);
    
    /**
     * Increments the distance traveled for a player.
     *
     * @param player The player
     * @param distance The distance to add in blocks
     * @return True if successful, false otherwise
     */
    boolean incrementDistance(Player player, double distance);
    
    /**
     * Gets the total distance traveled by a player in blocks.
     *
     * @param player The player
     * @return The total distance traveled
     */
    double getDistance(Player player);
    
    /**
     * Gets the formatted distance for a player (e.g., "1.5 km").
     *
     * @param player The player
     * @return The formatted distance
     */
    String getFormattedDistance(Player player);
    
    /**
     * Saves all pending statistics for a player.
     *
     * @param player The player
     * @return True if successful, false otherwise
     */
    boolean saveStats(Player player);
    
    /**
     * Loads statistics for a player from the database.
     *
     * @param player The player
     * @return True if successful, false otherwise
     */
    boolean loadStats(Player player);
    
    /**
     * Handles a player joining the server.
     *
     * @param player The player
     */
    void handlePlayerJoin(Player player);
    
    /**
     * Handles a player quitting the server.
     *
     * @param player The player
     */
    void handlePlayerQuit(Player player);
}