// ./Example-Plugin/src/main/java/com/minecraft/example/sql/dao/PlayerDAO.java
package com.minecraft.example.sql.dao;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.sql.models.Player;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.result.ResultRow;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object for Player entities.
 */
public class PlayerDAO {
    
    private final Database database;
    
    /**
     * Creates a new PlayerDAO instance.
     *
     * @param database The database instance
     */
    public PlayerDAO(Database database) {
        this.database = database;
    }
    
    /**
     * Maps a database row to a Player object.
     *
     * @param row The database row
     * @return The Player object
     */
    private Player mapPlayer(ResultRow row) throws SQLException {
        Player player = new Player();
        player.setId(row.getInt("id"));
        player.setUuid(UUID.fromString(row.getString("uuid")));
        player.setName(row.getString("name"));
        player.setFirstJoin(row.getTimestamp("first_join").toLocalDateTime());
        player.setLastSeen(row.getTimestamp("last_seen").toLocalDateTime());
        return player;
    }
    
    /**
     * Finds a player by their ID.
     *
     * @param id The player ID
     * @return An Optional containing the player if found
     */
    public Optional<Player> findById(int id) {
        try {
            return database.queryFirst(
                "SELECT id, uuid, name, first_join, last_seen FROM players WHERE id = ?",
                this::mapPlayer,
                id
            );
        } catch (SQLException e) {
            LogUtil.severe("Error finding player by ID: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Finds a player by their ID asynchronously.
     *
     * @param id The player ID
     * @return A CompletableFuture that completes with an Optional containing the player if found
     */
    public CompletableFuture<Optional<Player>> findByIdAsync(int id) {
        return database.queryFirstAsync(
            "SELECT id, uuid, name, first_join, last_seen FROM players WHERE id = ?",
            this::mapPlayer,
            id
        ).exceptionally(e -> {
            LogUtil.severe("Error finding player by ID asynchronously: " + e.getMessage());
            return Optional.empty();
        });
    }
    
    /**
     * Finds a player by their UUID.
     *
     * @param uuid The player UUID
     * @return An Optional containing the player if found
     */
    public Optional<Player> findByUuid(UUID uuid) {
        try {
            return database.findByUuid(
                "SELECT id, uuid, name, first_join, last_seen FROM players WHERE uuid = ?",
                this::mapPlayer,
                uuid
            );
        } catch (SQLException e) {
            LogUtil.severe("Error finding player by UUID: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Finds a player by their UUID asynchronously.
     *
     * @param uuid The player UUID
     * @return A CompletableFuture that completes with an Optional containing the player if found
     */
    public CompletableFuture<Optional<Player>> findByUuidAsync(UUID uuid) {
        return database.findByUuidAsync(
            "SELECT id, uuid, name, first_join, last_seen FROM players WHERE uuid = ?",
            this::mapPlayer,
            uuid
        ).exceptionally(e -> {
            LogUtil.severe("Error finding player by UUID asynchronously: " + e.getMessage());
            return Optional.empty();
        });
    }
    
    /**
     * Finds a player by their name.
     *
     * @param name The player name
     * @return An Optional containing the player if found
     */
    public Optional<Player> findByName(String name) {
        try {
            return database.queryFirst(
                "SELECT id, uuid, name, first_join, last_seen FROM players WHERE name = ? COLLATE NOCASE",
                this::mapPlayer,
                name
            );
        } catch (SQLException e) {
            LogUtil.severe("Error finding player by name: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Finds a player by their player ID (same as findById - this was missing).
     *
     * @param playerId The player ID
     * @return An Optional containing the player if found
     */
    public Optional<Player> findByPlayerId(int playerId) {
        return findById(playerId);
    }
    
    /**
     * Finds a player by their name asynchronously.
     *
     * @param name The player name
     * @return A CompletableFuture that completes with an Optional containing the player if found
     */
    public CompletableFuture<Optional<Player>> findByNameAsync(String name) {
        return database.queryFirstAsync(
            "SELECT id, uuid, name, first_join, last_seen FROM players WHERE name = ? COLLATE NOCASE",
            this::mapPlayer,
            name
        ).exceptionally(e -> {
            LogUtil.severe("Error finding player by name asynchronously: " + e.getMessage());
            return Optional.empty();
        });
    }
    
    /**
     * Finds all players.
     *
     * @return A list of all players
     */
    public List<Player> findAll() {
        try {
            return database.query(
                "SELECT id, uuid, name, first_join, last_seen FROM players",
                this::mapPlayer
            );
        } catch (SQLException e) {
            LogUtil.severe("Error finding all players: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Finds all players asynchronously.
     *
     * @return A CompletableFuture that completes with a list of all players
     */
    public CompletableFuture<List<Player>> findAllAsync() {
        return database.queryAsync(
            "SELECT id, uuid, name, first_join, last_seen FROM players",
            this::mapPlayer
        ).exceptionally(e -> {
            LogUtil.severe("Error finding all players asynchronously: " + e.getMessage());
            return new ArrayList<>();
        });
    }
    
    /**
     * Saves a player to the database.
     *
     * @param player The player to save
     * @return The saved player
     */
    public Player save(Player player) {
        try {
            if (player.getId() == 0) {
                // Insert new player
                int id = database.update(
                    "INSERT INTO players (uuid, name, first_join, last_seen) VALUES (?, ?, ?, ?)",
                    player.getUuid().toString(),
                    player.getName(),
                    java.sql.Timestamp.valueOf(player.getFirstJoin()),
                    java.sql.Timestamp.valueOf(player.getLastSeen())
                );
                player.setId(id);
            } else {
                // Update existing player
                database.update(
                    "UPDATE players SET name = ?, last_seen = ? WHERE id = ?",
                    player.getName(),
                    java.sql.Timestamp.valueOf(player.getLastSeen()),
                    player.getId()
                );
            }
            
            return player;
        } catch (SQLException e) {
            LogUtil.severe("Error saving player: " + e.getMessage());
            return player;
        }
    }
    
    /**
     * Saves a player to the database asynchronously.
     *
     * @param player The player to save
     * @return A CompletableFuture that completes with the saved player
     */
    public CompletableFuture<Player> saveAsync(Player player) {
        if (player.getId() == 0) {
            // Insert new player
            return database.updateAsync(
                "INSERT INTO players (uuid, name, first_join, last_seen) VALUES (?, ?, ?, ?)",
                player.getUuid().toString(),
                player.getName(),
                java.sql.Timestamp.valueOf(player.getFirstJoin()),
                java.sql.Timestamp.valueOf(player.getLastSeen())
            ).thenApply(id -> {
                player.setId(id);
                return player;
            }).exceptionally(e -> {
                LogUtil.severe("Error saving player asynchronously: " + e.getMessage());
                return player;
            });
        } else {
            // Update existing player
            return database.updateAsync(
                "UPDATE players SET name = ?, last_seen = ? WHERE id = ?",
                player.getName(),
                java.sql.Timestamp.valueOf(player.getLastSeen()),
                player.getId()
            ).thenApply(rows -> player)
            .exceptionally(e -> {
                LogUtil.severe("Error updating player asynchronously: " + e.getMessage());
                return player;
            });
        }
    }
    
    /**
     * Updates a player in the database.
     *
     * @param player The player to update
     * @return The updated player
     */
    public Player update(Player player) {
        try {
            database.update(
                "UPDATE players SET name = ?, last_seen = ? WHERE id = ?",
                player.getName(),
                java.sql.Timestamp.valueOf(player.getLastSeen()),
                player.getId()
            );
            
            return player;
        } catch (SQLException e) {
            LogUtil.severe("Error updating player: " + e.getMessage());
            return player;
        }
    }
    
    /**
     * Updates a player in the database asynchronously.
     *
     * @param player The player to update
     * @return A CompletableFuture that completes with the updated player
     */
    public CompletableFuture<Player> updateAsync(Player player) {
        return database.updateAsync(
            "UPDATE players SET name = ?, last_seen = ? WHERE id = ?",
            player.getName(),
            java.sql.Timestamp.valueOf(player.getLastSeen()),
            player.getId()
        ).thenApply(rows -> player)
        .exceptionally(e -> {
            LogUtil.severe("Error updating player asynchronously: " + e.getMessage());
            return player;
        });
    }
    
    /**
     * Deletes a player from the database.
     *
     * @param player The player to delete
     * @return True if the player was deleted, false otherwise
     */
    public boolean delete(Player player) {
        try {
            int rowsAffected = database.update(
                "DELETE FROM players WHERE id = ?",
                player.getId()
            );
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            LogUtil.severe("Error deleting player: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Deletes a player from the database asynchronously.
     *
     * @param player The player to delete
     * @return A CompletableFuture that completes with true if the player was deleted
     */
    public CompletableFuture<Boolean> deleteAsync(Player player) {
        return database.updateAsync(
            "DELETE FROM players WHERE id = ?",
            player.getId()
        ).thenApply(rowsAffected -> rowsAffected > 0)
        .exceptionally(e -> {
            LogUtil.severe("Error deleting player asynchronously: " + e.getMessage());
            return false;
        });
    }
}