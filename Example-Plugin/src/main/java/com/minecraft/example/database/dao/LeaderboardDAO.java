// ./Example-Plugin/src/main/java/com/minecraft/example/database/dao/LeaderboardDAO.java
package com.minecraft.example.database.dao;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.PlayerStatsPlugin;
import com.minecraft.example.data.Leaderboard;
import com.minecraft.example.data.LeaderboardEntry;
import com.minecraft.sqlbridge.api.Database;
import org.bukkit.Material;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object for leaderboards.
 */
public class LeaderboardDAO {
    
    private final PlayerStatsPlugin plugin;
    private final Database database;
    
    // Cache for leaderboard configurations
    private final Map<String, Leaderboard> leaderboardCache = new HashMap<>();
    
    /**
     * Creates a new LeaderboardDAO instance.
     *
     * @param plugin The plugin instance
     * @param database The database instance
     */
    public LeaderboardDAO(PlayerStatsPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }
    
    /**
     * Saves a leaderboard to the database.
     *
     * @param leaderboard The leaderboard to save
     * @return True if the leaderboard was saved, false otherwise
     * @throws SQLException If an error occurs
     */
    public boolean saveLeaderboard(Leaderboard leaderboard) throws SQLException {
        // Convert timestamps to SQL Timestamp
        Timestamp lastUpdated = leaderboard.getLastUpdated() != null 
                ? Timestamp.valueOf(leaderboard.getLastUpdated()) 
                : null;
        
        // Insert or update leaderboard
        int rowsAffected = database.update(
                "INSERT INTO leaderboards (leaderboard_id, display_name, stat_name, icon_material, " +
                "category, reversed, update_interval, last_updated, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (leaderboard_id) " +
                "DO UPDATE SET display_name = ?, stat_name = ?, icon_material = ?, " +
                "category = ?, reversed = ?, update_interval = ?, last_updated = ?, description = ?",
                leaderboard.getId(),
                leaderboard.getDisplayName(),
                leaderboard.getStatName(),
                leaderboard.getIcon().name(),
                leaderboard.getCategory(),
                leaderboard.isReversed() ? 1 : 0,
                leaderboard.getUpdateInterval(),
                lastUpdated,
                leaderboard.getDescription(),
                leaderboard.getDisplayName(),
                leaderboard.getStatName(),
                leaderboard.getIcon().name(),
                leaderboard.getCategory(),
                leaderboard.isReversed() ? 1 : 0,
                leaderboard.getUpdateInterval(),
                lastUpdated,
                leaderboard.getDescription()
        );
        
        if (rowsAffected > 0) {
            // Update cache
            leaderboardCache.put(leaderboard.getId(), leaderboard);
            return true;
        }
        
        return false;
    }
    
    /**
     * Saves a leaderboard to the database asynchronously.
     *
     * @param leaderboard The leaderboard to save
     * @return A CompletableFuture that completes with true if the leaderboard was saved
     */
    public CompletableFuture<Boolean> saveLeaderboardAsync(Leaderboard leaderboard) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return saveLeaderboard(leaderboard);
            } catch (SQLException e) {
                LogUtil.severe("Failed to save leaderboard: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Gets a leaderboard by its ID.
     *
     * @param id The leaderboard ID
     * @return The leaderboard, or null if not found
     * @throws SQLException If an error occurs
     */
    public Leaderboard getLeaderboard(String id) throws SQLException {
        // Check cache first
        if (leaderboardCache.containsKey(id)) {
            return leaderboardCache.get(id);
        }
        
        // Query database
        return database.queryFirst(
                "SELECT leaderboard_id, display_name, stat_name, icon_material, category, " +
                "reversed, update_interval, last_updated, description " +
                "FROM leaderboards WHERE leaderboard_id = ?",
                this::mapLeaderboard,
                id
        ).orElse(null);
    }
    
    /**
     * Gets a leaderboard by its ID asynchronously.
     *
     * @param id The leaderboard ID
     * @return A CompletableFuture that completes with the leaderboard
     */
    public CompletableFuture<Leaderboard> getLeaderboardAsync(String id) {
        // Check cache first
        if (leaderboardCache.containsKey(id)) {
            return CompletableFuture.completedFuture(leaderboardCache.get(id));
        }
        
        // Query database asynchronously
        return database.queryFirstAsync(
                "SELECT leaderboard_id, display_name, stat_name, icon_material, category, " +
                "reversed, update_interval, last_updated, description " +
                "FROM leaderboards WHERE leaderboard_id = ?",
                this::mapLeaderboard,
                id
        ).thenApply(optional -> {
            Leaderboard leaderboard = optional.orElse(null);
            if (leaderboard != null) {
                // Update cache
                leaderboardCache.put(id, leaderboard);
            }
            return leaderboard;
        });
    }
    
    /**
     * Gets all leaderboards.
     *
     * @return A list of all leaderboards
     * @throws SQLException If an error occurs
     */
    public List<Leaderboard> getAllLeaderboards() throws SQLException {
        // Check if cache is populated
        if (!leaderboardCache.isEmpty()) {
            return new ArrayList<>(leaderboardCache.values());
        }
        
        List<Leaderboard> leaderboards = database.query(
                "SELECT leaderboard_id, display_name, stat_name, icon_material, category, " +
                "reversed, update_interval, last_updated, description " +
                "FROM leaderboards",
                this::mapLeaderboard
        );
        
        // Update cache
        leaderboards.forEach(leaderboard -> leaderboardCache.put(leaderboard.getId(), leaderboard));
        
        return leaderboards;
    }
    
    /**
     * Gets all leaderboards asynchronously.
     *
     * @return A CompletableFuture that completes with a list of all leaderboards
     */
    public CompletableFuture<List<Leaderboard>> getAllLeaderboardsAsync() {
        // Check if cache is populated
        if (!leaderboardCache.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>(leaderboardCache.values()));
        }
        
        // Query database asynchronously
        return database.queryAsync(
                "SELECT leaderboard_id, display_name, stat_name, icon_material, category, " +
                "reversed, update_interval, last_updated, description " +
                "FROM leaderboards",
                this::mapLeaderboard
        ).thenApply(leaderboards -> {
            // Update cache
            leaderboards.forEach(leaderboard -> leaderboardCache.put(leaderboard.getId(), leaderboard));
            return leaderboards;
        });
    }
    
    /**
     * Gets leaderboards by category.
     *
     * @param category The category
     * @return A list of leaderboards in the category
     * @throws SQLException If an error occurs
     */
    public List<Leaderboard> getLeaderboardsByCategory(String category) throws SQLException {
        // Check if cache is populated
        if (!leaderboardCache.isEmpty()) {
            return leaderboardCache.values().stream()
                    .filter(lb -> category.equals(lb.getCategory()))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        return database.query(
                "SELECT leaderboard_id, display_name, stat_name, icon_material, category, " +
                "reversed, update_interval, last_updated, description " +
                "FROM leaderboards WHERE category = ?",
                this::mapLeaderboard,
                category
        );
    }
    
    /**
     * Deletes a leaderboard.
     *
     * @param id The leaderboard ID
     * @return True if the leaderboard was deleted, false otherwise
     * @throws SQLException If an error occurs
     */
    public boolean deleteLeaderboard(String id) throws SQLException {
        int rowsAffected = database.update(
                "DELETE FROM leaderboards WHERE leaderboard_id = ?",
                id
        );
        
        if (rowsAffected > 0) {
            // Remove from cache
            leaderboardCache.remove(id);
            return true;
        }
        
        return false;
    }
    
    /**
     * Updates the last updated timestamp for a leaderboard.
     *
     * @param id The leaderboard ID
     * @return True if the timestamp was updated, false otherwise
     * @throws SQLException If an error occurs
     */
    public boolean updateLeaderboardTimestamp(String id) throws SQLException {
        // Current timestamp
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        
        int rowsAffected = database.update(
                "UPDATE leaderboards SET last_updated = ? WHERE leaderboard_id = ?",
                now, id
        );
        
        if (rowsAffected > 0 && leaderboardCache.containsKey(id)) {
            // Update cache
            leaderboardCache.get(id).setLastUpdated(now.toLocalDateTime());
            return true;
        }
        
        return rowsAffected > 0;
    }
    
    /**
     * Updates the last updated timestamp for a leaderboard asynchronously.
     *
     * @param id The leaderboard ID
     * @return A CompletableFuture that completes with true if the timestamp was updated
     */
    public CompletableFuture<Boolean> updateLeaderboardTimestampAsync(String id) {
        // Current timestamp
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        
        return database.updateAsync(
                "UPDATE leaderboards SET last_updated = ? WHERE leaderboard_id = ?",
                now, id
        ).thenApply(rowsAffected -> {
            if (rowsAffected > 0 && leaderboardCache.containsKey(id)) {
                // Update cache
                leaderboardCache.get(id).setLastUpdated(now.toLocalDateTime());
                return true;
            }
            
            return rowsAffected > 0;
        });
    }
    
    /**
     * Gets leaderboard entries.
     *
     * @param leaderboardId The leaderboard ID
     * @param limit The maximum number of entries to return
     * @return A list of leaderboard entries
     * @throws SQLException If an error occurs
     */
    public List<LeaderboardEntry> getLeaderboardEntries(String leaderboardId, int limit) throws SQLException {
        return database.query(
                "SELECT e.id, e.leaderboard_id, e.player_id, p.name AS player_name, e.rank, e.score, " +
                "e.update_time, e.previous_rank " +
                "FROM leaderboard_entries e " +
                "JOIN players p ON e.player_id = p.id " +
                "WHERE e.leaderboard_id = ? " +
                "ORDER BY e.rank " +
                "LIMIT ?",
                this::mapLeaderboardEntry,
                leaderboardId, limit
        );
    }
    
    /**
     * Gets leaderboard entries asynchronously.
     *
     * @param leaderboardId The leaderboard ID
     * @param limit The maximum number of entries to return
     * @return A CompletableFuture that completes with a list of leaderboard entries
     */
    public CompletableFuture<List<LeaderboardEntry>> getLeaderboardEntriesAsync(String leaderboardId, int limit) {
        return database.queryAsync(
                "SELECT e.id, e.leaderboard_id, e.player_id, p.name AS player_name, e.rank, e.score, " +
                "e.update_time, e.previous_rank " +
                "FROM leaderboard_entries e " +
                "JOIN players p ON e.player_id = p.id " +
                "WHERE e.leaderboard_id = ? " +
                "ORDER BY e.rank " +
                "LIMIT ?",
                this::mapLeaderboardEntry,
                leaderboardId, limit
        );
    }
    
    /**
     * Saves a leaderboard entry.
     *
     * @param entry The entry to save
     * @return True if the entry was saved, false otherwise
     * @throws SQLException If an error occurs
     */
    public boolean saveLeaderboardEntry(LeaderboardEntry entry) throws SQLException {
        // Get the previous rank if it exists
        int previousRank = getPreviousRank(entry.getLeaderboardId(), entry.getPlayerId());
        
        // Current timestamp
        Timestamp updateTime = Timestamp.valueOf(entry.getUpdateTime());
        
        // Insert or update entry
        int rowsAffected = database.update(
                "INSERT INTO leaderboard_entries (leaderboard_id, player_id, rank, score, update_time, previous_rank) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (leaderboard_id, player_id) " +
                "DO UPDATE SET rank = ?, score = ?, update_time = ?, previous_rank = ?",
                entry.getLeaderboardId(),
                entry.getPlayerId(),
                entry.getRank(),
                entry.getScore(),
                updateTime,
                previousRank == 0 ? 0 : entry.getPreviousRank(), // Use 0 for new entries
                entry.getRank(),
                entry.getScore(),
                updateTime,
                previousRank == 0 ? 0 : previousRank // Use old rank for updates
        );
        
        return rowsAffected > 0;
    }
    
    /**
     * Saves leaderboard entries in batch.
     *
     * @param entries The entries to save
     * @return The number of entries saved
     * @throws SQLException If an error occurs
     */
    public int saveLeaderboardEntries(List<LeaderboardEntry> entries) throws SQLException {
        if (entries.isEmpty()) {
            return 0;
        }
        
        // Current timestamp
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        
        // Batch update entries
        List<Object[]> batchParams = new ArrayList<>();
        for (LeaderboardEntry entry : entries) {
            // Get the previous rank if it exists
            int previousRank = getPreviousRank(entry.getLeaderboardId(), entry.getPlayerId());
            
            batchParams.add(new Object[]{
                    entry.getLeaderboardId(),
                    entry.getPlayerId(),
                    entry.getRank(),
                    entry.getScore(),
                    now,
                    previousRank == 0 ? 0 : entry.getPreviousRank(), // Use 0 for new entries
                    entry.getRank(),
                    entry.getScore(),
                    now,
                    previousRank == 0 ? 0 : previousRank // Use old rank for updates
            });
        }
        
        // Use upsert to handle existing entries
        int[] results = database.batchUpdate(
                "INSERT INTO leaderboard_entries (leaderboard_id, player_id, rank, score, update_time, previous_rank) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (leaderboard_id, player_id) " +
                "DO UPDATE SET rank = ?, score = ?, update_time = ?, previous_rank = ?",
                batchParams
        );
        
        // Count successful updates
        int count = 0;
        for (int result : results) {
            if (result > 0) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Saves leaderboard entries in batch asynchronously.
     *
     * @param entries The entries to save
     * @return A CompletableFuture that completes with the number of entries saved
     */
    public CompletableFuture<Integer> saveLeaderboardEntriesAsync(List<LeaderboardEntry> entries) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return saveLeaderboardEntries(entries);
            } catch (SQLException e) {
                LogUtil.severe("Failed to save leaderboard entries: " + e.getMessage());
                return 0;
            }
        });
    }
    
    /**
     * Gets the rank of a player on a leaderboard.
     *
     * @param leaderboardId The leaderboard ID
     * @param playerId The player's database ID
     * @return The player's rank, or -1 if not ranked
     * @throws SQLException If an error occurs
     */
    public int getPlayerRank(String leaderboardId, int playerId) throws SQLException {
        return database.queryFirst(
                "SELECT rank FROM leaderboard_entries WHERE leaderboard_id = ? AND player_id = ?",
                rs -> rs.getInt("rank"),
                leaderboardId, playerId
        ).orElse(-1);
    }
    
    /**
     * Gets the rank of a player on a leaderboard asynchronously.
     *
     * @param leaderboardId The leaderboard ID
     * @param playerId The player's database ID
     * @return A CompletableFuture that completes with the player's rank
     */
    public CompletableFuture<Integer> getPlayerRankAsync(String leaderboardId, int playerId) {
        return database.queryFirstAsync(
                "SELECT rank FROM leaderboard_entries WHERE leaderboard_id = ? AND player_id = ?",
                rs -> rs.getInt("rank"),
                leaderboardId, playerId
        ).thenApply(optional -> optional.orElse(-1));
    }
    
    /**
     * Gets the previous rank of a player on a leaderboard.
     *
     * @param leaderboardId The leaderboard ID
     * @param playerId The player's database ID
     * @return The player's previous rank, or 0 if not previously ranked
     * @throws SQLException If an error occurs
     */
    private int getPreviousRank(String leaderboardId, int playerId) throws SQLException {
        return database.queryFirst(
                "SELECT rank FROM leaderboard_entries WHERE leaderboard_id = ? AND player_id = ?",
                rs -> rs.getInt("rank"),
                leaderboardId, playerId
        ).orElse(0);
    }
    
    /**
     * Clears all entries for a leaderboard.
     *
     * @param leaderboardId The leaderboard ID
     * @return The number of entries cleared
     * @throws SQLException If an error occurs
     */
    public int clearLeaderboard(String leaderboardId) throws SQLException {
        return database.update(
                "DELETE FROM leaderboard_entries WHERE leaderboard_id = ?",
                leaderboardId
        );
    }
    
    /**
     * Maps a ResultSet to a Leaderboard object.
     *
     * @param rs The ResultSet
     * @return The Leaderboard object.
     * @throws SQLException If an error occurs
     */
    private Leaderboard mapLeaderboard(ResultSet rs) throws SQLException {
        String id = rs.getString("leaderboard_id");
        String displayName = rs.getString("display_name");
        String statName = rs.getString("stat_name");
        Material icon = Material.valueOf(rs.getString("icon_material"));
        String category = rs.getString("category");
        boolean reversed = rs.getBoolean("reversed");
        int updateInterval = rs.getInt("update_interval");
        Timestamp lastUpdated = rs.getTimestamp("last_updated");
        String description = rs.getString("description");
        
        return Leaderboard.builder()
                .id(id)
                .displayName(displayName)
                .statName(statName)
                .icon(icon)
                .category(category)
                .reversed(reversed)
                .updateInterval(updateInterval)
                .lastUpdated(lastUpdated != null ? lastUpdated.toLocalDateTime() : null)
                .description(description)
                .build();
    }
    
    /**
     * Maps a ResultSet to a LeaderboardEntry object.
     *
     * @param rs The ResultSet
     * @return The LeaderboardEntry object
     * @throws SQLException If an error occurs
     */
    private LeaderboardEntry mapLeaderboardEntry(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String leaderboardId = rs.getString("leaderboard_id");
        int playerId = rs.getInt("player_id");
        String playerName = rs.getString("player_name");
        int rank = rs.getInt("rank");
        int score = rs.getInt("score");
        Timestamp updateTime = rs.getTimestamp("update_time");
        int previousRank = rs.getInt("previous_rank");
        
        UUID playerUUID = new UUID(0, 0); // Placeholder, will be set externally
        
        return LeaderboardEntry.builder()
                .id(id)
                .leaderboardId(leaderboardId)
                .playerId(playerId)
                .playerName(playerName)
                .rank(rank)
                .score(score)
                .updateTime(updateTime != null ? updateTime.toLocalDateTime() : LocalDateTime.now())
                .previousRank(previousRank)
                .build();
    }
    
    /**
     * Clears the leaderboard cache.
     */
    public void clearCache() {
        leaderboardCache.clear();
    }
}