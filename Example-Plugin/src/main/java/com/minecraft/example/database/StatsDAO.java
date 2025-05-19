// ./src/main/java/com/minecraft/example/database/StatsDAO.java
package com.minecraft.example.database;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.model.PlayerStats;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.result.ResultMapper;
import com.minecraft.sqlbridge.api.result.ResultRow;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object for PlayerStats
 * Demonstrates SQL-Bridge features
 */
public class StatsDAO {
    
    private final Database database;
    private final Plugin plugin;
    private final ResultMapper<PlayerStats> statsMapper;
    
    public StatsDAO(Database database, Plugin plugin) {
        this.database = database;
        this.plugin = plugin;
        
        // Define the mapper once as a field (SQL-Bridge best practice)
        this.statsMapper = this::mapPlayerStats;
    }
    
    /**
     * Map database row to PlayerStats object
     */
    private PlayerStats mapPlayerStats(ResultRow row) throws SQLException {
        PlayerStats stats = new PlayerStats();
        
        stats.setUuid(UUID.fromString(row.getString("uuid")));
        stats.setName(row.getString("name"));
        stats.setFirstJoin(row.getTimestamp("first_join"));
        stats.setLastJoin(row.getTimestamp("last_join"));
        stats.setPlaytimeSeconds(row.getLong("playtime_seconds"));
        stats.setLoginCount(row.getInt("login_count"));
        stats.setBlocksBroken(row.getInt("blocks_broken"));
        stats.setBlocksPlaced(row.getInt("blocks_placed"));
        
        return stats;
    }
    
    /**
     * Find PlayerStats by UUID
     */
    public Optional<PlayerStats> findByUuid(UUID uuid) {
        try {
            // Using SQL-Bridge's UUID convenience method
            return database.findByUuid(
                "SELECT * FROM player_stats WHERE uuid = ?",
                statsMapper,
                uuid
            );
        } catch (SQLException e) {
            LogUtil.severe("Error finding player stats for UUID " + uuid + ": " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Find PlayerStats by UUID asynchronously
     */
    public CompletableFuture<Optional<PlayerStats>> findByUuidAsync(UUID uuid) {
        // Using SQL-Bridge's async UUID convenience method
        return database.findByUuidAsync(
            "SELECT * FROM player_stats WHERE uuid = ?",
            statsMapper,
            uuid
        ).exceptionally(e -> {
            LogUtil.severe("Error finding player stats for UUID " + uuid + ": " + e.getMessage());
            return Optional.empty();
        });
    }
    
    /**
     * Find PlayerStats by player name
     */
    public Optional<PlayerStats> findByName(String name) {
        try {
            return database.queryFirst(
                "SELECT * FROM player_stats WHERE name = ? COLLATE NOCASE",
                statsMapper,
                name
            );
        } catch (SQLException e) {
            LogUtil.severe("Error finding player stats for name " + name + ": " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Find top players by a specific stat
     */
    public List<PlayerStats> findTopPlayersByStat(String statField, int limit) {
        try {
            // Using SQL-Bridge's query builder API
            return database.select()
                .columns("*")
                .from("player_stats")
                .orderBy(statField + " DESC")
                .limit(limit)
                .executeQuery(statsMapper);
        } catch (SQLException e) {
            LogUtil.severe("Error finding top players by " + statField + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Insert a new PlayerStats record
     */
    public boolean insert(PlayerStats stats) {
        try {
            // Using SQL-Bridge's insert builder
            int result = database.insertInto("player_stats")
                .columns("uuid", "name", "first_join", "last_join", "playtime_seconds", 
                         "login_count", "blocks_broken", "blocks_placed")
                .values(
                    stats.getUuid().toString(),
                    stats.getName(),
                    new Timestamp(stats.getFirstJoin().getTime()),
                    new Timestamp(stats.getLastJoin().getTime()),
                    stats.getPlaytimeSeconds(),
                    stats.getLoginCount(),
                    stats.getBlocksBroken(),
                    stats.getBlocksPlaced()
                )
                .executeUpdate();
            
            return result > 0;
        } catch (SQLException e) {
            LogUtil.severe("Error inserting player stats: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update an existing PlayerStats record
     */
    public boolean update(PlayerStats stats) {
        try {
            // Using SQL-Bridge's update builder
            int result = database.update("player_stats")
                .set("name", stats.getName())
                .set("last_join", new Timestamp(stats.getLastJoin().getTime()))
                .set("playtime_seconds", stats.getPlaytimeSeconds())
                .set("login_count", stats.getLoginCount())
                .set("blocks_broken", stats.getBlocksBroken())
                .set("blocks_placed", stats.getBlocksPlaced())
                .where("uuid = ?", stats.getUuid().toString())
                .executeUpdate();
            
            return result > 0;
        } catch (SQLException e) {
            LogUtil.severe("Error updating player stats: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update player playtime
     */
    public boolean updatePlaytime(UUID uuid, long additionalSeconds) {
        try {
            // Demonstrate SQL-Bridge transaction support
            return database.executeTransaction(connection -> {
                // First get current playtime with lock
                Optional<Long> currentPlaytime = database.queryFirst(
                    "SELECT playtime_seconds FROM player_stats WHERE uuid = ? FOR UPDATE",
                    row -> row.getLong("playtime_seconds"),
                    uuid.toString()
                );
                
                if (!currentPlaytime.isPresent()) {
                    return false;
                }
                
                // Update with new total
                long newTotal = currentPlaytime.get() + additionalSeconds;
                int result = database.update(
                    "UPDATE player_stats SET playtime_seconds = ? WHERE uuid = ?",
                    newTotal,
                    uuid.toString()
                );
                
                return result > 0;
            });
        } catch (SQLException e) {
            LogUtil.severe("Error updating playtime: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Increment blocks broken counter
     */
    public boolean incrementBlocksBroken(UUID uuid) {
        try {
            // Simple update query
            int result = database.update(
                "UPDATE player_stats SET blocks_broken = blocks_broken + 1 WHERE uuid = ?",
                uuid.toString()
            );
            
            return result > 0;
        } catch (SQLException e) {
            LogUtil.warning("Error incrementing blocks broken: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Increment blocks placed counter
     */
    public boolean incrementBlocksPlaced(UUID uuid) {
        try {
            int result = database.update(
                "UPDATE player_stats SET blocks_placed = blocks_placed + 1 WHERE uuid = ?",
                uuid.toString()
            );
            
            return result > 0;
        } catch (SQLException e) {
            LogUtil.warning("Error incrementing blocks placed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Reset a specific stat for a player
     */
    public boolean resetStat(UUID uuid, String statField) {
        try {
            // Using prepared statement to safely use dynamic field name
            int result = database.update(
                "UPDATE player_stats SET " + statField + " = ? WHERE uuid = ?",
                0, // Reset to zero
                uuid.toString()
            );
            
            return result > 0;
        } catch (SQLException e) {
            LogUtil.severe("Error resetting stat " + statField + ": " + e.getMessage());
            return false;
        }
    }
}