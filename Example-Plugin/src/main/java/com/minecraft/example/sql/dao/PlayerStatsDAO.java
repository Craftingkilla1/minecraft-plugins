// ./Example-Plugin/src/main/java/com/minecraft/example/sql/dao/PlayerStatsDAO.java
package com.minecraft.example.sql.dao;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.sql.models.PlayerStats;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.result.ResultRow;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object for PlayerStats entities.
 */
public class PlayerStatsDAO {
    
    private final Database database;
    
    /**
     * Creates a new PlayerStatsDAO instance.
     *
     * @param database The database instance
     */
    public PlayerStatsDAO(Database database) {
        this.database = database;
    }
    
    /**
     * Maps a database row to a PlayerStats object.
     *
     * @param row The database row
     * @return The PlayerStats object
     */
    private PlayerStats mapPlayerStats(ResultRow row) throws SQLException {
        PlayerStats stats = new PlayerStats();
        stats.setId(row.getInt("id"));
        stats.setPlayerId(row.getInt("player_id"));
        stats.setName(row.getString("stat_name"));
        stats.setValue(row.getInt("stat_value"));
        stats.setLastUpdated(row.getTimestamp("last_updated").toLocalDateTime());
        return stats;
    }
    
    /**
     * Finds a statistic by ID.
     *
     * @param id The statistic ID
     * @return An Optional containing the statistic if found
     */
    public Optional<PlayerStats> findById(int id) {
        try {
            return database.queryFirst(
                "SELECT id, player_id, stat_name, stat_value, last_updated FROM stats WHERE id = ?",
                this::mapPlayerStats,
                id
            );
        } catch (SQLException e) {
            LogUtil.severe("Error finding stat by ID: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Finds a statistic by player ID and name.
     *
     * @param playerId The player ID
     * @param name The statistic name
     * @return An Optional containing the statistic if found
     */
    public Optional<PlayerStats> findByPlayerIdAndName(int playerId, String name) {
        try {
            return database.queryFirst(
                "SELECT id, player_id, stat_name, stat_value, last_updated FROM stats WHERE player_id = ? AND stat_name = ?",
                this::mapPlayerStats,
                playerId, name
            );
        } catch (SQLException e) {
            LogUtil.severe("Error finding stat by player ID and name: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Finds a statistic by player ID and name asynchronously.
     *
     * @param playerId The player ID
     * @param name The statistic name
     * @return A CompletableFuture that completes with an Optional containing the statistic if found
     */
    public CompletableFuture<Optional<PlayerStats>> findByPlayerIdAndNameAsync(int playerId, String name) {
        return database.queryFirstAsync(
            "SELECT id, player_id, stat_name, stat_value, last_updated FROM stats WHERE player_id = ? AND stat_name = ?",
            this::mapPlayerStats,
            playerId, name
        ).exceptionally(e -> {
            LogUtil.severe("Error finding stat by player ID and name asynchronously: " + e.getMessage());
            return Optional.empty();
        });
    }
    
    /**
     * Finds all statistics for a player.
     *
     * @param playerId The player ID
     * @return A list of all statistics for the player
     */
    public List<PlayerStats> findAllByPlayerId(int playerId) {
        try {
            return database.query(
                "SELECT id, player_id, stat_name, stat_value, last_updated FROM stats WHERE player_id = ?",
                this::mapPlayerStats,
                playerId
            );
        } catch (SQLException e) {
            LogUtil.severe("Error finding all stats for player: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Finds all statistics for a player asynchronously.
     *
     * @param playerId The player ID
     * @return A CompletableFuture that completes with a list of all statistics for the player
     */
    public CompletableFuture<List<PlayerStats>> findAllByPlayerIdAsync(int playerId) {
        return database.queryAsync(
            "SELECT id, player_id, stat_name, stat_value, last_updated FROM stats WHERE player_id = ?",
            this::mapPlayerStats,
            playerId
        ).exceptionally(e -> {
            LogUtil.severe("Error finding all stats for player asynchronously: " + e.getMessage());
            return new ArrayList<>();
        });
    }
    
    /**
     * Finds the top players for a statistic.
     *
     * @param statName The statistic name
     * @param limit The maximum number of results to return
     * @return A list of statistics for the top players
     */
    public List<PlayerStats> findTopByStatName(String statName, int limit) {
        try {
            return database.query(
                "SELECT id, player_id, stat_name, stat_value, last_updated FROM stats WHERE stat_name = ? ORDER BY stat_value DESC LIMIT ?",
                this::mapPlayerStats,
                statName, limit
            );
        } catch (SQLException e) {
            LogUtil.severe("Error finding top stats: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Finds the top players for a statistic asynchronously.
     *
     * @param statName The statistic name
     * @param limit The maximum number of results to return
     * @return A CompletableFuture that completes with a list of statistics for the top players
     */
    public CompletableFuture<List<PlayerStats>> findTopByStatNameAsync(String statName, int limit) {
        return database.queryAsync(
            "SELECT id, player_id, stat_name, stat_value, last_updated FROM stats WHERE stat_name = ? ORDER BY stat_value DESC LIMIT ?",
            this::mapPlayerStats,
            statName, limit
        ).exceptionally(e -> {
            LogUtil.severe("Error finding top stats asynchronously: " + e.getMessage());
            return new ArrayList<>();
        });
    }
    
    /**
     * Gets the ranking of a player for a statistic.
     *
     * @param statName The statistic name
     * @param value The statistic value
     * @return The rank of the player (1-based)
     */
    public int getStatRanking(String statName, int value) {
        try {
            return database.queryFirst(
                "SELECT COUNT(*) + 1 AS rank FROM stats WHERE stat_name = ? AND stat_value > ?",
                rs -> rs.getInt("rank"),
                statName, value
            ).orElse(0);
        } catch (SQLException e) {
            LogUtil.severe("Error getting stat ranking: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Gets the ranking of a player for a statistic asynchronously.
     *
     * @param statName The statistic name
     * @param value The statistic value
     * @return A CompletableFuture that completes with the rank of the player (1-based)
     */
    public CompletableFuture<Integer> getStatRankingAsync(String statName, int value) {
        return database.queryFirstAsync(
            "SELECT COUNT(*) + 1 AS rank FROM stats WHERE stat_name = ? AND stat_value > ?",
            rs -> rs.getInt("rank"),
            statName, value
        ).thenApply(opt -> opt.orElse(0))
        .exceptionally(e -> {
            LogUtil.severe("Error getting stat ranking asynchronously: " + e.getMessage());
            return 0;
        });
    }
    
    /**
     * Saves a statistic to the database.
     *
     * @param stats The statistic to save
     * @return The saved statistic
     */
    public PlayerStats save(PlayerStats stats) {
        try {
            if (stats.getId() == 0) {
                // Insert new stat
                int id = database.update(
                    "INSERT INTO stats (player_id, stat_name, stat_value, last_updated) VALUES (?, ?, ?, ?)",
                    stats.getPlayerId(),
                    stats.getName(),
                    stats.getValue(),
                    java.sql.Timestamp.valueOf(stats.getLastUpdated())
                );
                stats.setId(id);
            } else {
                // Update existing stat
                database.update(
                    "UPDATE stats SET stat_value = ?, last_updated = ? WHERE id = ?",
                    stats.getValue(),
                    java.sql.Timestamp.valueOf(stats.getLastUpdated()),
                    stats.getId()
                );
            }
            
            return stats;
        } catch (SQLException e) {
            LogUtil.severe("Error saving stat: " + e.getMessage());
            return stats;
        }
    }
    
    /**
     * Saves a statistic to the database asynchronously.
     *
     * @param stats The statistic to save
     * @return A CompletableFuture that completes with the saved statistic
     */
    public CompletableFuture<PlayerStats> saveAsync(PlayerStats stats) {
        if (stats.getId() == 0) {
            // Insert new stat
            return database.updateAsync(
                "INSERT INTO stats (player_id, stat_name, stat_value, last_updated) VALUES (?, ?, ?, ?)",
                stats.getPlayerId(),
                stats.getName(),
                stats.getValue(),
                java.sql.Timestamp.valueOf(stats.getLastUpdated())
            ).thenApply(id -> {
                stats.setId(id);
                return stats;
            }).exceptionally(e -> {
                LogUtil.severe("Error saving stat asynchronously: " + e.getMessage());
                return stats;
            });
        } else {
            // Update existing stat
            return database.updateAsync(
                "UPDATE stats SET stat_value = ?, last_updated = ? WHERE id = ?",
                stats.getValue(),
                java.sql.Timestamp.valueOf(stats.getLastUpdated()),
                stats.getId()
            ).thenApply(rows -> stats)
            .exceptionally(e -> {
                LogUtil.severe("Error updating stat asynchronously: " + e.getMessage());
                return stats;
            });
        }
    }
    
    /**
     * Updates a statistic in the database.
     *
     * @param stats The statistic to update
     * @return The updated statistic
     */
    public PlayerStats update(PlayerStats stats) {
        try {
            database.update(
                "UPDATE stats SET stat_value = ?, last_updated = ? WHERE id = ?",
                stats.getValue(),
                java.sql.Timestamp.valueOf(stats.getLastUpdated()),
                stats.getId()
            );
            
            return stats;
        } catch (SQLException e) {
            LogUtil.severe("Error updating stat: " + e.getMessage());
            return stats;
        }
    }
    
    /**
     * Updates a statistic in the database asynchronously.
     *
     * @param stats The statistic to update
     * @return A CompletableFuture that completes with the updated statistic
     */
    public CompletableFuture<PlayerStats> updateAsync(PlayerStats stats) {
        return database.updateAsync(
            "UPDATE stats SET stat_value = ?, last_updated = ? WHERE id = ?",
            stats.getValue(),
            java.sql.Timestamp.valueOf(stats.getLastUpdated()),
            stats.getId()
        ).thenApply(rows -> stats)
        .exceptionally(e -> {
            LogUtil.severe("Error updating stat asynchronously: " + e.getMessage());
            return stats;
        });
    }
    
    /**
     * Deletes a statistic from the database.
     *
     * @param stats The statistic to delete
     * @return True if the statistic was deleted, false otherwise
     */
    public boolean delete(PlayerStats stats) {
        try {
            int rowsAffected = database.update(
                "DELETE FROM stats WHERE id = ?",
                stats.getId()
            );
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            LogUtil.severe("Error deleting stat: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Deletes a statistic from the database asynchronously.
     *
     * @param stats The statistic to delete
     * @return A CompletableFuture that completes with true if the statistic was deleted
     */
    public CompletableFuture<Boolean> deleteAsync(PlayerStats stats) {
        return database.updateAsync(
            "DELETE FROM stats WHERE id = ?",
            stats.getId()
        ).thenApply(rowsAffected -> rowsAffected > 0)
        .exceptionally(e -> {
            LogUtil.severe("Error deleting stat asynchronously: " + e.getMessage());
            return false;
        });
    }
    
    /**
     * Deletes a statistic by player ID and name.
     *
     * @param playerId The player ID
     * @param name The statistic name
     * @return True if the statistic was deleted, false otherwise
     */
    public boolean deleteByPlayerIdAndName(int playerId, String name) {
        try {
            int rowsAffected = database.update(
                "DELETE FROM stats WHERE player_id = ? AND stat_name = ?",
                playerId, name
            );
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            LogUtil.severe("Error deleting stat by player ID and name: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Deletes a statistic by player ID and name asynchronously.
     *
     * @param playerId The player ID
     * @param name The statistic name
     * @return A CompletableFuture that completes with true if the statistic was deleted
     */
    public CompletableFuture<Boolean> deleteByPlayerIdAndNameAsync(int playerId, String name) {
        return database.updateAsync(
            "DELETE FROM stats WHERE player_id = ? AND stat_name = ?",
            playerId, name
        ).thenApply(rowsAffected -> rowsAffected > 0)
        .exceptionally(e -> {
            LogUtil.severe("Error deleting stat by player ID and name asynchronously: " + e.getMessage());
            return false;
        });
    }
    
    /**
     * Deletes all statistics for a player.
     *
     * @param playerId The player ID
     * @return True if any statistics were deleted, false otherwise
     */
    public boolean deleteAllByPlayerId(int playerId) {
        try {
            int rowsAffected = database.update(
                "DELETE FROM stats WHERE player_id = ?",
                playerId
            );
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            LogUtil.severe("Error deleting all stats for player: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Deletes all statistics for a player asynchronously.
     *
     * @param playerId The player ID
     * @return A CompletableFuture that completes with true if any statistics were deleted
     */
    public CompletableFuture<Boolean> deleteAllByPlayerIdAsync(int playerId) {
        return database.updateAsync(
            "DELETE FROM stats WHERE player_id = ?",
            playerId
        ).thenApply(rowsAffected -> rowsAffected > 0)
        .exceptionally(e -> {
            LogUtil.severe("Error deleting all stats for player asynchronously: " + e.getMessage());
            return false;
        });
    }
}