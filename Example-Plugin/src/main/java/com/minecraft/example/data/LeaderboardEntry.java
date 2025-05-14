// ./Example-Plugin/src/main/java/com/minecraft/example/data/LeaderboardEntry.java
package com.minecraft.example.data;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an entry in a leaderboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {
    
    /**
     * The unique ID of the entry.
     */
    private int id;
    
    /**
     * The ID of the leaderboard this entry belongs to.
     */
    private String leaderboardId;
    
    /**
     * The UUID of the player.
     */
    private UUID playerId;
    
    /**
     * The name of the player.
     */
    private String playerName;
    
    /**
     * The player's rank (1-based).
     */
    private int rank;
    
    /**
     * The player's score.
     */
    private int score;
    
    /**
     * When the entry was last updated.
     */
    private LocalDateTime updateTime;
    
    /**
     * The player's previous rank, or 0 if this is their first time on the leaderboard.
     */
    private int previousRank;
    
    /**
     * Creates a new LeaderboardEntry.
     *
     * @param leaderboardId The ID of the leaderboard
     * @param playerId      The UUID of the player
     * @param playerName    The name of the player
     * @param rank          The player's rank
     * @param score         The player's score
     * @return A new LeaderboardEntry instance
     */
    public static LeaderboardEntry create(
            String leaderboardId,
            UUID playerId,
            String playerName,
            int rank,
            int score
    ) {
        return LeaderboardEntry.builder()
                .leaderboardId(leaderboardId)
                .playerId(playerId)
                .playerName(playerName)
                .rank(rank)
                .score(score)
                .updateTime(LocalDateTime.now())
                .previousRank(0) // Default to 0, will be updated later
                .build();
    }
    
    /**
     * Checks if the player's rank has changed.
     *
     * @return True if the rank has changed, false otherwise
     */
    public boolean hasRankChanged() {
        return previousRank != 0 && previousRank != rank;
    }
    
    /**
     * Gets the rank change (positive for improvement, negative for decline).
     *
     * @return The rank change, or 0 if the rank hasn't changed
     */
    public int getRankChange() {
        if (previousRank == 0) {
            return 0; // New entry
        }
        
        return previousRank - rank; // Positive means improvement (moved up)
    }
    
    /**
     * Updates the entry with a new rank and score.
     *
     * @param newRank  The new rank
     * @param newScore The new score
     */
    public void update(int newRank, int newScore) {
        this.previousRank = this.rank;
        this.rank = newRank;
        this.score = newScore;
        this.updateTime = LocalDateTime.now();
    }
}