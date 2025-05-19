// ./src/main/java/com/minecraft/example/service/DefaultStatsService.java
package com.minecraft.example.service;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.ExamplePlugin;
import com.minecraft.example.model.PlayerStats;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.result.ResultMapper;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of StatsService
 * Demonstrates SQL-Bridge database operations
 */
public class DefaultStatsService implements StatsService {
    
    private final ExamplePlugin plugin;
    private final Database database;
    private final ResultMapper<PlayerStats> playerStatsMapper;
    
    /**
     * Constructor
     * @param plugin Plugin instance
     * @param database Database instance
     */
    public DefaultStatsService(ExamplePlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        
        // Define the mapper once for reuse
        this.playerStatsMapper = row -> {
            PlayerStats stats = new PlayerStats();
            stats.setId(row.getInt("id"));
            stats.setPlayerUuid(UUID.fromString(row.getString("uuid")));
            stats.setPlayerName(row.getString("name"));
            stats.setKills(row.getInt("kills"));
            stats.setDeaths(row.getInt("deaths"));
            stats.setBlocksPlaced(row.getInt("blocks_placed"));
            stats.setBlocksBroken(row.getInt("blocks_broken"));
            stats.setTimePlayed(row.getInt("time_played"));
            stats.setLastSeen(row.getTimestamp("last_seen"));
            return stats;
        };
    }
    
    @Override
    public Optional<PlayerStats> getPlayerStats(UUID playerUuid) {
        try {
            // Use SQL-Bridge's convenience method for UUID
            return database.findByUuid(
                "SELECT * FROM player_stats WHERE uuid = ?",
                playerStatsMapper,
                playerUuid
            );
        } catch (SQLException e) {
            LogUtil.severe("Failed to get player stats: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public boolean savePlayerStats(PlayerStats stats) {
        try {
            // Check if the player already exists
            boolean exists = database.queryFirst(
                "SELECT 1 FROM player_stats WHERE uuid = ?",
                row -> true,
                stats.getPlayerUuid().toString()
            ).isPresent();
            
            if (exists) {
                // Update existing player
                int updated = database.update(
                    "UPDATE player_stats SET " +
                    "name = ?, " +
                    "kills = ?, " +
                    "deaths = ?, " +
                    "blocks_placed = ?, " +
                    "blocks_broken = ?, " +
                    "time_played = ?, " +
                    "last_seen = ? " +
                    "WHERE uuid = ?",
                    stats.getPlayerName(),
                    stats.getKills(),
                    stats.getDeaths(),
                    stats.getBlocksPlaced(),
                    stats.getBlocksBroken(),
                    stats.getTimePlayed(),
                    new Timestamp(System.currentTimeMillis()),
                    stats.getPlayerUuid().toString()
                );
                
                return updated > 0;
            } else {
                // Insert new player
                int inserted = database.update(
                    "INSERT INTO player_stats " +
                    "(uuid, name, kills, deaths, blocks_placed, blocks_broken, time_played, last_seen) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    stats.getPlayerUuid().toString(),
                    stats.getPlayerName(),
                    stats.getKills(),
                    stats.getDeaths(),
                    stats.getBlocksPlaced(),
                    stats.getBlocksBroken(),
                    stats.getTimePlayed(),
                    new Timestamp(System.currentTimeMillis())
                );
                
                return inserted > 0;
            }
        } catch (SQLException e) {
            LogUtil.severe("Failed to save player stats: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public Optional<PlayerStats> getPlayerStatsByName(String playerName) {
        try {
            return database.queryFirst(
                "SELECT * FROM player_stats WHERE name = ?",
                playerStatsMapper,
                playerName
            );
        } catch (SQLException e) {
            LogUtil.severe("Failed to get player stats by name: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public boolean updateStat(UUID playerUuid, String statName, int value) {
        // Map stat name to database column
        String column;
        switch (statName.toLowerCase()) {
            case "kills":
                column = "kills";
                break;
            case "deaths":
                column = "deaths";
                break;
            case "blocks_placed":
            case "blocksplaced":
                column = "blocks_placed";
                break;
            case "blocks_broken":
            case "blocksbroken":
                column = "blocks_broken";
                break;
            case "time_played":
            case "timeplayed":
                column = "time_played";
                break;
            default:
                LogUtil.warning("Invalid stat name: " + statName);
                return false;
        }
        
        try {
            // Demonstrate SQL-Bridge's query builder API
            int updated = database.update("player_stats")
                .set(column, value)
                .set("last_seen", new Timestamp(System.currentTimeMillis()))
                .where("uuid = ?", playerUuid.toString())
                .executeUpdate();
            
            return updated > 0;
        } catch (SQLException e) {
            LogUtil.severe("Failed to update stat: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean incrementStat(UUID playerUuid, String statName) {
        // Map stat name to database column
        String column;
        switch (statName.toLowerCase()) {
            case "kills":
                column = "kills";
                break;
            case "deaths":
                column = "deaths";
                break;
            case "blocks_placed":
            case "blocksplaced":
                column = "blocks_placed";
                break;
            case "blocks_broken":
            case "blocksbroken":
                column = "blocks_broken";
                break;
            default:
                LogUtil.warning("Invalid stat name: " + statName);
                return false;
        }
        
        try {
            // Use SQL UPDATE with increment
            int updated = database.update(
                "UPDATE player_stats SET " + column + " = " + column + " + 1, last_seen = ? WHERE uuid = ?",
                new Timestamp(System.currentTimeMillis()),
                playerUuid.toString()
            );
            
            return updated > 0;
        } catch (SQLException e) {
            LogUtil.severe("Failed to increment stat: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<PlayerStats> getTopPlayers(String statName, int limit) {
        // Map stat name to database column
        String column;
        switch (statName.toLowerCase()) {
            case "kills":
                column = "kills";
                break;
            case "deaths":
                column = "deaths";
                break;
            case "blocks_placed":
            case "blocksplaced":
                column = "blocks_placed";
                break;
            case "blocks_broken":
            case "blocksbroken":
                column = "blocks_broken";
                break;
            case "time_played":
            case "timeplayed":
                column = "time_played";
                break;
            default:
                LogUtil.warning("Invalid stat name: " + statName);
                return new ArrayList<>();
        }
        
        try {
            // Use SQL-Bridge's query builder API
            return database.select()
                .all()
                .from("player_stats")
                .orderBy(column + " DESC")
                .limit(limit)
                .executeQuery(playerStatsMapper);
        } catch (SQLException e) {
            LogUtil.severe("Failed to get top players: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean resetStats(UUID playerUuid) {
        try {
            // Reset all stats to zero
            int updated = database.update(
                "UPDATE player_stats SET " +
                "kills = 0, " +
                "deaths = 0, " +
                "blocks_placed = 0, " +
                "blocks_broken = 0, " +
                "time_played = 0, " +
                "last_seen = ? " +
                "WHERE uuid = ?",
                new Timestamp(System.currentTimeMillis()),
                playerUuid.toString()
            );
            
            return updated > 0;
        } catch (SQLException e) {
            LogUtil.severe("Failed to reset stats: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean updateTimePlayed(UUID playerUuid, int additionalSeconds) {
        try {
            // Demonstrate SQL-Bridge's transaction support
            return database.executeTransaction(connection -> {
                // Get current time played
                Optional<Integer> timePlayedOpt = database.queryFirst(
                    "SELECT time_played FROM player_stats WHERE uuid = ? FOR UPDATE",
                    row -> row.getInt("time_played"),
                    playerUuid.toString()
                );
                
                if (!timePlayedOpt.isPresent()) {
                    return false; // Player not found
                }
                
                int currentTimePlayed = timePlayedOpt.get();
                int newTimePlayed = currentTimePlayed + additionalSeconds;
                
                // Update time played
                int updated = database.update(
                    "UPDATE player_stats SET time_played = ?, last_seen = ? WHERE uuid = ?",
                    newTimePlayed,
                    new Timestamp(System.currentTimeMillis()),
                    playerUuid.toString()
                );
                
                return updated > 0;
            });
        } catch (SQLException e) {
            LogUtil.severe("Failed to update time played: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Asynchronously get player stats
     * Demonstrates SQL-Bridge's async operations
     * @param playerUuid Player UUID
     * @param callback Callback to handle the result
     */
    public void getPlayerStatsAsync(UUID playerUuid, java.util.function.Consumer<Optional<PlayerStats>> callback) {
        // Use SQL-Bridge's async API
        database.findByUuidAsync(
            "SELECT * FROM player_stats WHERE uuid = ?",
            playerStatsMapper,
            playerUuid
        ).thenAccept(result -> {
            // Run callback on main thread for thread safety with Bukkit API
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        }).exceptionally(e -> {
            LogUtil.severe("Failed to get player stats asynchronously: " + e.getMessage());
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(Optional.empty()));
            return null;
        });
    }
}