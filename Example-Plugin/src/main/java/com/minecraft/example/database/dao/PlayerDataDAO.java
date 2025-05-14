// ./Example-Plugin/src/main/java/com/minecraft/example/database/dao/PlayerDataDAO.java
package com.minecraft.example.database.dao;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.PlayerStatsPlugin;
import com.minecraft.example.data.PlayerData;
import com.minecraft.sqlbridge.api.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object for player data.
 */
public class PlayerDataDAO {
    
    private final PlayerStatsPlugin plugin;
    private final Database database;
    
    // Cache for frequently accessed player data
    private final Map<UUID, PlayerData> playerCache = new HashMap<>();
    
    /**
     * Creates a new PlayerDataDAO instance.
     *
     * @param plugin The plugin instance
     * @param database The database instance
     */
    public PlayerDataDAO(PlayerStatsPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }
    
    /**
     * Creates a new player in the database.
     *
     * @param playerData The player data to create
     * @return True if the player was created, false otherwise
     * @throws SQLException If an error occurs
     */
    public boolean createPlayer(PlayerData playerData) throws SQLException {
        // Insert into players table
        int playerId = database.update(
                "INSERT INTO players (uuid, name, first_join, last_seen, playtime) VALUES (?, ?, ?, ?, ?)",
                playerData.getUuid().toString(),
                playerData.getName(),
                Timestamp.valueOf(playerData.getFirstJoin()),
                Timestamp.valueOf(playerData.getLastSeen()),
                playerData.getPlaytime()
        );
        
        if (playerId > 0) {
            // Update cache
            playerCache.put(playerData.getUuid(), playerData);
            return true;
        }
        
        return false;
    }
    
    /**
     * Creates a new player in the database asynchronously.
     *
     * @param playerData The player data to create
     * @return A CompletableFuture that completes with true if the player was created
     */
    public CompletableFuture<Boolean> createPlayerAsync(PlayerData playerData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return createPlayer(playerData);
            } catch (SQLException e) {
                LogUtil.severe("Failed to create player: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Gets a player by UUID.
     *
     * @param uuid The player's UUID
     * @return The player data, or null if not found
     * @throws SQLException If an error occurs
     */
    public PlayerData getPlayer(UUID uuid) throws SQLException {
        // Check cache first
        if (playerCache.containsKey(uuid)) {
            return playerCache.get(uuid);
        }
        
        // Query database
        return database.queryFirst(
                "SELECT id, uuid, name, first_join, last_seen, playtime FROM players WHERE uuid = ?",
                this::mapPlayer,
                uuid.toString()
        ).orElse(null);
    }
    
    /**
     * Gets a player by UUID asynchronously.
     *
     * @param uuid The player's UUID
     * @return A CompletableFuture that completes with the player data
     */
    public CompletableFuture<PlayerData> getPlayerAsync(UUID uuid) {
        // Check cache first
        if (playerCache.containsKey(uuid)) {
            PlayerData cachedData = playerCache.get(uuid);
            return CompletableFuture.completedFuture(cachedData);
        }
        
        // Query database asynchronously
        return database.queryFirstAsync(
                "SELECT id, uuid, name, first_join, last_seen, playtime FROM players WHERE uuid = ?",
                this::mapPlayer,
                uuid.toString()
        ).thenApply(optional -> {
            PlayerData playerData = optional.orElse(null);
            if (playerData != null) {
                // Update cache
                playerCache.put(uuid, playerData);
                
                // Load stats
                loadPlayerStats(playerData);
            }
            return playerData;
        });
    }
    
    /**
     * Gets a player by name.
     *
     * @param name The player's name
     * @return The player data, or null if not found
     * @throws SQLException If an error occurs
     */
    public PlayerData getPlayerByName(String name) throws SQLException {
        return database.queryFirst(
                "SELECT id, uuid, name, first_join, last_seen, playtime FROM players WHERE name = ? COLLATE NOCASE",
                this::mapPlayer,
                name
        ).orElse(null);
    }
    
    /**
     * Updates player data in the database.
     *
     * @param playerData The player data to update
     * @return True if the player was updated, false otherwise
     * @throws SQLException If an error occurs
     */
    public boolean updatePlayer(PlayerData playerData) throws SQLException {
        // Update players table
        int rowsAffected = database.update(
                "UPDATE players SET name = ?, last_seen = ?, playtime = ? WHERE uuid = ?",
                playerData.getName(),
                Timestamp.valueOf(playerData.getLastSeen()),
                playerData.getPlaytime(),
                playerData.getUuid().toString()
        );
        
        if (rowsAffected > 0) {
            // Update cache
            playerCache.put(playerData.getUuid(), playerData);
            
            // Update stats if they exist
            if (playerData.getStats() != null && !playerData.getStats().isEmpty()) {
                updatePlayerStats(playerData);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Updates player data in the database asynchronously.
     *
     * @param playerData The player data to update
     * @return A CompletableFuture that completes with true if the player was updated
     */
    public CompletableFuture<Boolean> updatePlayerAsync(PlayerData playerData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return updatePlayer(playerData);
            } catch (SQLException e) {
                LogUtil.severe("Failed to update player: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Loads player statistics from the database.
     *
     * @param playerData The player data to load stats for
     * @throws SQLException If an error occurs
     */
    public void loadPlayerStats(PlayerData playerData) throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        
        // Get player ID
        int playerId = getPlayerId(playerData.getUuid());
        if (playerId == -1) {
            return; // Player not found
        }
        
        // Query stats
        database.executeQuery(
                "SELECT stat_name, stat_value FROM stats WHERE player_id = ?",
                rs -> {
                    String statName = rs.getString("stat_name");
                    int statValue = rs.getInt("stat_value");
                    stats.put(statName, statValue);
                },
                playerId
        );
        
        // Update player data
        playerData.setStats(stats);
    }
    
    /**
     * Updates player statistics in the database.
     *
     * @param playerData The player data containing stats to update
     * @throws SQLException If an error occurs
     */
    public void updatePlayerStats(PlayerData playerData) throws SQLException {
        // Get player ID
        int playerId = getPlayerId(playerData.getUuid());
        if (playerId == -1) {
            return; // Player not found
        }
        
        // Current timestamp
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        
        // Batch update stats
        List<Object[]> batchParams = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : playerData.getStats().entrySet()) {
            batchParams.add(new Object[]{
                    playerId,
                    entry.getKey(),
                    entry.getValue(),
                    now,
                    playerId,
                    entry.getKey()
            });
        }
        
        // Use upsert (INSERT OR REPLACE) to handle existing stats
        database.batchUpdate(
                "INSERT INTO stats (player_id, stat_name, stat_value, last_updated) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (player_id, stat_name) " +
                "DO UPDATE SET stat_value = ?, last_updated = ?",
                batchParams
        );
    }
    
    /**
     * Updates a specific player statistic in the database.
     *
     * @param uuid The player's UUID
     * @param statName The statistic name
     * @param value The statistic value
     * @return True if the statistic was updated, false otherwise
     * @throws SQLException If an error occurs
     */
    public boolean updateStat(UUID uuid, String statName, int value) throws SQLException {
        // Get player ID
        int playerId = getPlayerId(uuid);
        if (playerId == -1) {
            return false; // Player not found
        }
        
        // Current timestamp
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        
        // Update stat using upsert
        int rowsAffected = database.update(
                "INSERT INTO stats (player_id, stat_name, stat_value, last_updated) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (player_id, stat_name) " +
                "DO UPDATE SET stat_value = ?, last_updated = ?",
                playerId, statName, value, now, value, now
        );
        
        // Update cache if it exists
        if (rowsAffected > 0 && playerCache.containsKey(uuid)) {
            PlayerData cachedData = playerCache.get(uuid);
            cachedData.setStat(statName, value);
        }
        
        return rowsAffected > 0;
    }
    
    /**
     * Updates a specific player statistic in the database asynchronously.
     *
     * @param uuid The player's UUID
     * @param statName The statistic name
     * @param value The statistic value
     * @return A CompletableFuture that completes with true if the statistic was updated
     */
    public CompletableFuture<Boolean> updateStatAsync(UUID uuid, String statName, int value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return updateStat(uuid, statName, value);
            } catch (SQLException e) {
                LogUtil.severe("Failed to update stat: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Gets the value of a specific player statistic.
     *
     * @param uuid The player's UUID
     * @param statName The statistic name
     * @return The statistic value, or 0 if not found
     * @throws SQLException If an error occurs
     */
    public int getStat(UUID uuid, String statName) throws SQLException {
        // Check cache first
        if (playerCache.containsKey(uuid)) {
            PlayerData cachedData = playerCache.get(uuid);
            return cachedData.getStat(statName);
        }
        
        // Get player ID
        int playerId = getPlayerId(uuid);
        if (playerId == -1) {
            return 0; // Player not found
        }
        
        // Query stat
        return database.queryFirst(
                "SELECT stat_value FROM stats WHERE player_id = ? AND stat_name = ?",
                rs -> rs.getInt("stat_value"),
                playerId, statName
        ).orElse(0);
    }
    
    /**
     * Gets the value of a specific player statistic asynchronously.
     *
     * @param uuid The player's UUID
     * @param statName The statistic name
     * @return A CompletableFuture that completes with the statistic value
     */
    public CompletableFuture<Integer> getStatAsync(UUID uuid, String statName) {
        // Check cache first
        if (playerCache.containsKey(uuid)) {
            PlayerData cachedData = playerCache.get(uuid);
            return CompletableFuture.completedFuture(cachedData.getStat(statName));
        }
        
        // Query database asynchronously
        return getPlayerIdAsync(uuid).thenCompose(playerId -> {
            if (playerId == -1) {
                return CompletableFuture.completedFuture(0); // Player not found
            }
            
            return database.queryFirstAsync(
                    "SELECT stat_value FROM stats WHERE player_id = ? AND stat_name = ?",
                    rs -> rs.getInt("stat_value"),
                    playerId, statName
            ).thenApply(optional -> optional.orElse(0));
        });
    }
    
    /**
     * Gets all statistics for a player.
     *
     * @param uuid The player's UUID
     * @return A map of statistic names to values
     * @throws SQLException If an error occurs
     */
    public Map<String, Integer> getAllStats(UUID uuid) throws SQLException {
        // Check cache first
        if (playerCache.containsKey(uuid)) {
            PlayerData cachedData = playerCache.get(uuid);
            if (cachedData.getStats() != null) {
                return new HashMap<>(cachedData.getStats());
            }
        }
        
        Map<String, Integer> stats = new HashMap<>();
        
        // Get player ID
        int playerId = getPlayerId(uuid);
        if (playerId == -1) {
            return stats; // Player not found
        }
        
        // Query stats
        database.executeQuery(
                "SELECT stat_name, stat_value FROM stats WHERE player_id = ?",
                rs -> {
                    String statName = rs.getString("stat_name");
                    int statValue = rs.getInt("stat_value");
                    stats.put(statName, statValue);
                },
                playerId
        );
        
        return stats;
    }
    
    /**
     * Gets all statistics for a player asynchronously.
     *
     * @param uuid The player's UUID
     * @return A CompletableFuture that completes with a map of statistic names to values
     */
    public CompletableFuture<Map<String, Integer>> getAllStatsAsync(UUID uuid) {
        // Check cache first
        if (playerCache.containsKey(uuid)) {
            PlayerData cachedData = playerCache.get(uuid);
            if (cachedData.getStats() != null) {
                return CompletableFuture.completedFuture(new HashMap<>(cachedData.getStats()));
            }
        }
        
        // Query database asynchronously
        return getPlayerIdAsync(uuid).thenCompose(playerId -> {
            if (playerId == -1) {
                return CompletableFuture.completedFuture(new HashMap<>()); // Player not found
            }
            
            Map<String, Integer> stats = new HashMap<>();
            
            return database.executeQueryAsync(
                    "SELECT stat_name, stat_value FROM stats WHERE player_id = ?",
                    rs -> {
                        String statName = rs.getString("stat_name");
                        int statValue = rs.getInt("stat_value");
                        stats.put(statName, statValue);
                    },
                    playerId
            ).thenApply(v -> stats);
        });
    }
    
    /**
     * Gets the top players for a specific statistic.
     *
     * @param statName The statistic name
     * @param limit The maximum number of players to return
     * @return A map of player names to statistic values, ordered by value
     * @throws SQLException If an error occurs
     */
    public Map<String, Integer> getTopPlayers(String statName, int limit) throws SQLException {
        Map<String, Integer> topPlayers = new LinkedHashMap<>(); // Preserve order
        
        database.executeQuery(
                "SELECT p.name, s.stat_value " +
                "FROM stats s " +
                "JOIN players p ON s.player_id = p.id " +
                "WHERE s.stat_name = ? " +
                "ORDER BY s.stat_value DESC " +
                "LIMIT ?",
                rs -> {
                    String playerName = rs.getString("name");
                    int statValue = rs.getInt("stat_value");
                    topPlayers.put(playerName, statValue);
                },
                statName, limit
        );
        
        return topPlayers;
    }
    
    /**
     * Gets the top players for a specific statistic asynchronously.
     *
     * @param statName The statistic name
     * @param limit The maximum number of players to return
     * @return A CompletableFuture that completes with a map of player names to statistic values
     */
    public CompletableFuture<Map<String, Integer>> getTopPlayersAsync(String statName, int limit) {
        Map<String, Integer> topPlayers = new LinkedHashMap<>(); // Preserve order
        
        return database.executeQueryAsync(
                "SELECT p.name, s.stat_value " +
                "FROM stats s " +
                "JOIN players p ON s.player_id = p.id " +
                "WHERE s.stat_name = ? " +
                "ORDER BY s.stat_value DESC " +
                "LIMIT ?",
                rs -> {
                    String playerName = rs.getString("name");
                    int statValue = rs.getInt("stat_value");
                    topPlayers.put(playerName, statValue);
                },
                statName, limit
        ).thenApply(v -> topPlayers);
    }
    
    /**
     * Gets the player ID from the database.
     *
     * @param uuid The player's UUID
     * @return The player ID, or -1 if not found
     * @throws SQLException If an error occurs
     */
    private int getPlayerId(UUID uuid) throws SQLException {
        return database.queryFirst(
                "SELECT id FROM players WHERE uuid = ?",
                rs -> rs.getInt("id"),
                uuid.toString()
        ).orElse(-1);
    }
    
    /**
     * Gets the player ID from the database asynchronously.
     *
     * @param uuid The player's UUID
     * @return A CompletableFuture that completes with the player ID
     */
    private CompletableFuture<Integer> getPlayerIdAsync(UUID uuid) {
        return database.queryFirstAsync(
                "SELECT id FROM players WHERE uuid = ?",
                rs -> rs.getInt("id"),
                uuid.toString()
        ).thenApply(optional -> optional.orElse(-1));
    }
    
    /**
     * Maps a ResultSet to a PlayerData object.
     *
     * @param rs The ResultSet
     * @return The PlayerData object
     * @throws SQLException If an error occurs
     */
    private PlayerData mapPlayer(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        String name = rs.getString("name");
        LocalDateTime firstJoin = rs.getTimestamp("first_join").toLocalDateTime();
        LocalDateTime lastSeen = rs.getTimestamp("last_seen").toLocalDateTime();
        long playtime = rs.getLong("playtime");
        
        PlayerData playerData = PlayerData.builder()
                .uuid(uuid)
                .name(name)
                .firstJoin(firstJoin)
                .lastSeen(lastSeen)
                .playtime(playtime)
                .stats(new HashMap<>())
                .build();
        
        try {
            // Load player stats
            loadPlayerStats(playerData);
        } catch (SQLException e) {
            LogUtil.warning("Failed to load stats for player " + name + ": " + e.getMessage());
        }
        
        // Update cache
        playerCache.put(uuid, playerData);
        
        return playerData;
    }
    
    /**
     * Clears the player cache.
     */
    public void clearCache() {
        playerCache.clear();
    }
    
    /**
     * Removes a player from the cache.
     *
     * @param uuid The player's UUID
     */
    public void removeFromCache(UUID uuid) {
        playerCache.remove(uuid);
    }
}