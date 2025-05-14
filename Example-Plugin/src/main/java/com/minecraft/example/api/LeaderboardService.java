// ./Example-Plugin/src/main/java/com/minecraft/example/api/LeaderboardService.java
package com.minecraft.example.api;

import com.minecraft.example.data.Leaderboard;
import com.minecraft.example.data.LeaderboardEntry;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing server leaderboards.
 */
public interface LeaderboardService {
    
    /**
     * Loads leaderboard configurations.
     */
    void loadLeaderboards();
    
    /**
     * Gets all available leaderboards.
     *
     * @return A list of all leaderboards
     */
    List<Leaderboard> getAllLeaderboards();
    
    /**
     * Gets a leaderboard by its ID.
     *
     * @param id The leaderboard ID
     * @return The leaderboard, or null if not found
     */
    Leaderboard getLeaderboard(String id);
    
    /**
     * Gets the entries for a leaderboard.
     *
     * @param id    The leaderboard ID
     * @param limit The maximum number of entries to return
     * @return A list of leaderboard entries
     */
    List<LeaderboardEntry> getLeaderboardEntries(String id, int limit);
    
    /**
     * Gets the entries for a leaderboard asynchronously.
     *
     * @param id    The leaderboard ID
     * @param limit The maximum number of entries to return
     * @return A CompletableFuture that will complete with the leaderboard entries
     */
    CompletableFuture<List<LeaderboardEntry>> getLeaderboardEntriesAsync(String id, int limit);
    
    /**
     * Updates all leaderboards with the latest data.
     */
    void updateAllLeaderboards();
    
    /**
     * Updates a specific leaderboard with the latest data.
     *
     * @param id The leaderboard ID
     * @return True if the leaderboard was updated, false otherwise
     */
    boolean updateLeaderboard(String id);
    
    /**
     * Updates a specific leaderboard with the latest data asynchronously.
     *
     * @param id The leaderboard ID
     * @return A CompletableFuture that will complete with true if the leaderboard was updated
     */
    CompletableFuture<Boolean> updateLeaderboardAsync(String id);
    
    /**
     * Gets the rank of a player on a specific leaderboard.
     *
     * @param player The player
     * @param id     The leaderboard ID
     * @return The player's rank (1-based), or -1 if not ranked
     */
    int getPlayerRank(Player player, String id);
    
    /**
     * Gets the rank of a player on a specific leaderboard asynchronously.
     *
     * @param player The player
     * @param id     The leaderboard ID
     * @return A CompletableFuture that will complete with the player's rank
     */
    CompletableFuture<Integer> getPlayerRankAsync(Player player, String id);
    
    /**
     * Gets the top players across all leaderboards.
     *
     * @param limit The maximum number of players per leaderboard
     * @return A map of leaderboard IDs to lists of entries
     */
    Map<String, List<LeaderboardEntry>> getTopPlayersAllLeaderboards(int limit);
    
    /**
     * Creates a new leaderboard.
     *
     * @param id          The leaderboard ID
     * @param displayName The display name of the leaderboard
     * @param statName    The statistic name to track
     * @return True if the leaderboard was created, false if it already exists
     */
    boolean createLeaderboard(String id, String displayName, String statName);
    
    /**
     * Deletes a leaderboard.
     *
     * @param id The leaderboard ID
     * @return True if the leaderboard was deleted, false otherwise
     */
    boolean deleteLeaderboard(String id);
    
    /**
     * Schedules automatic updates for a leaderboard.
     *
     * @param id       The leaderboard ID
     * @param interval The update interval in minutes
     * @return True if the schedule was set, false otherwise
     */
    boolean scheduleLeaderboardUpdates(String id, int interval);
}