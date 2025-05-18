// ./Example-Plugin/src/main/java/com/minecraft/example/sql/dao/PlayerAchievementDAO.java
package com.minecraft.example.sql.dao;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.sql.models.PlayerAchievement;
import com.minecraft.sqlbridge.api.Database;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object for PlayerAchievement entities.
 * Demonstrates SQL-Bridge DAO pattern implementation.
 */
public class PlayerAchievementDAO {
    
    private final Database database;
    private final Plugin plugin;
    private final PlayerDAO playerDAO;
    private final AchievementDAO achievementDAO;
    
    /**
     * Constructs a new PlayerAchievementDAO.
     *
     * @param database The database instance
     * @param plugin The plugin instance
     */
    public PlayerAchievementDAO(Database database, Plugin plugin) {
        this.database = database;
        this.plugin = plugin;
        this.playerDAO = new PlayerDAO(database, plugin);
        this.achievementDAO = new AchievementDAO(database, plugin);
    }
    
    /**
     * Creates the player achievement mapper.
     *
     * @return The player achievement result mapper
     */
    private com.minecraft.sqlbridge.api.result.ResultMapper<PlayerAchievement> createPlayerAchievementMapper() {
        return row -> {
            PlayerAchievement playerAchievement = new PlayerAchievement();
            playerAchievement.setPlayerId(row.getInt("player_id"));
            playerAchievement.setAchievementId(row.getInt("achievement_id"));
            playerAchievement.setEarnedAt(row.getTimestamp("earned_at"));
            return playerAchievement;
        };
    }
    
    /**
     * Creates a joined achievement mapper with detailed achievement info.
     *
     * @return The joined player achievement result mapper
     */
    private com.minecraft.sqlbridge.api.result.ResultMapper<PlayerAchievement> createJoinedPlayerAchievementMapper() {
        return row -> {
            PlayerAchievement playerAchievement = new PlayerAchievement();
            playerAchievement.setPlayerId(row.getInt("player_id"));
            playerAchievement.setAchievementId(row.getInt("achievement_id"));
            playerAchievement.setEarnedAt(row.getTimestamp("earned_at"));
            playerAchievement.setAchievementIdentifier(row.getString("identifier"));
            playerAchievement.setAchievementName(row.getString("name"));
            playerAchievement.setAchievementDescription(row.getString("description"));
            playerAchievement.setIconMaterial(row.getString("icon_material"));
            return playerAchievement;
        };
    }
    
    /**
     * Gets all achievements earned by a player.
     *
     * @param uuid The player's UUID
     * @return A list of player achievements
     */
    public List<PlayerAchievement> findByPlayerUuid(UUID uuid) {
        try {
            Optional<Integer> playerId = playerDAO.getPlayerIdByUuid(uuid);
            if (!playerId.isPresent()) {
                return new ArrayList<>();
            }
            
            return database.query(
                    "SELECT pa.*, a.identifier, a.name, a.description, a.icon_material " +
                    "FROM player_achievements pa " +
                    "JOIN achievements a ON pa.achievement_id = a.id " +
                    "WHERE pa.player_id = ? " +
                    "ORDER BY pa.earned_at DESC",
                    createJoinedPlayerAchievementMapper(),
                    playerId.get()
            );
        } catch (SQLException e) {
            LogUtil.severe("Error finding player achievements by UUID: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets all achievements earned by a player asynchronously.
     *
     * @param uuid The player's UUID
     * @return A CompletableFuture that will contain a list of player achievements
     */
    public CompletableFuture<List<PlayerAchievement>> findByPlayerUuidAsync(UUID uuid) {
        return playerDAO.getPlayerIdByUuid(uuid)
                .map(playerId -> database.queryAsync(
                        "SELECT pa.*, a.identifier, a.name, a.description, a.icon_material " +
                        "FROM player_achievements pa " +
                        "JOIN achievements a ON pa.achievement_id = a.id " +
                        "WHERE pa.player_id = ? " +
                        "ORDER BY pa.earned_at DESC",
                        createJoinedPlayerAchievementMapper(),
                        playerId
                ))
                .orElse(CompletableFuture.completedFuture(new ArrayList<>()))
                .exceptionally(e -> {
                    LogUtil.severe("Error finding player achievements by UUID async: " + e.getMessage());
                    return new ArrayList<>();
                });
    }
    
    /**
     * Gets all players who have earned a specific achievement.
     *
     * @param achievementIdentifier The achievement identifier
     * @return A list of player achievements
     */
    public List<PlayerAchievement> findByAchievementIdentifier(String achievementIdentifier) {
        try {
            Optional<Integer> achievementId = achievementDAO.getIdByIdentifier(achievementIdentifier);
            if (!achievementId.isPresent()) {
                return new ArrayList<>();
            }
            
            return database.query(
                    "SELECT pa.*, a.identifier, a.name, a.description, a.icon_material " +
                    "FROM player_achievements pa " +
                    "JOIN achievements a ON pa.achievement_id = a.id " +
                    "WHERE pa.achievement_id = ? " +
                    "ORDER BY pa.earned_at DESC",
                    createJoinedPlayerAchievementMapper(),
                    achievementId.get()
            );
        } catch (SQLException e) {
            LogUtil.severe("Error finding player achievements by achievement identifier: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Checks if a player has earned a specific achievement.
     *
     * @param uuid The player's UUID
     * @param achievementIdentifier The achievement identifier
     * @return True if the player has earned the achievement, false otherwise
     */
    public boolean hasAchievement(UUID uuid, String achievementIdentifier) {
        try {
            Optional<Integer> playerId = playerDAO.getPlayerIdByUuid(uuid);
            Optional<Integer> achievementId = achievementDAO.getIdByIdentifier(achievementIdentifier);
            
            if (!playerId.isPresent() || !achievementId.isPresent()) {
                return false;
            }
            
            return database.queryFirst(
                    "SELECT 1 FROM player_achievements WHERE player_id = ? AND achievement_id = ?",
                    row -> true,
                    playerId.get(),
                    achievementId.get()
            ).isPresent();
        } catch (SQLException e) {
            LogUtil.severe("Error checking if player has achievement: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if a player has earned a specific achievement asynchronously.
     *
     * @param uuid The player's UUID
     * @param achievementIdentifier The achievement identifier
     * @return A CompletableFuture that will contain true if the player has earned the achievement
     */
    public CompletableFuture<Boolean> hasAchievementAsync(UUID uuid, String achievementIdentifier) {
        CompletableFuture<Optional<Integer>> playerIdFuture = CompletableFuture.completedFuture(
                playerDAO.getPlayerIdByUuid(uuid));
        CompletableFuture<Optional<Integer>> achievementIdFuture = CompletableFuture.completedFuture(
                achievementDAO.getIdByIdentifier(achievementIdentifier));
        
        return CompletableFuture.allOf(playerIdFuture, achievementIdFuture)
                .thenCompose(v -> {
                    Optional<Integer> playerId = playerIdFuture.join();
                    Optional<Integer> achievementId = achievementIdFuture.join();
                    
                    if (!playerId.isPresent() || !achievementId.isPresent()) {
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    return database.queryFirstAsync(
                            "SELECT 1 FROM player_achievements WHERE player_id = ? AND achievement_id = ?",
                            row -> true,
                            playerId.get(),
                            achievementId.get()
                    ).thenApply(Optional::isPresent);
                })
                .exceptionally(e -> {
                    LogUtil.severe("Error checking if player has achievement async: " + e.getMessage());
                    return false;
                });
    }
    
    /**
     * Awards an achievement to a player.
     *
     * @param uuid The player's UUID
     * @param achievementIdentifier The achievement identifier
     * @return True if successful, false otherwise
     */
    public boolean awardAchievement(UUID uuid, String achievementIdentifier) {
        try {
            return database.executeTransaction(connection -> {
                Optional<Integer> playerId = playerDAO.getPlayerIdByUuid(uuid);
                Optional<Integer> achievementId = achievementDAO.getIdByIdentifier(achievementIdentifier);
                
                if (!playerId.isPresent() || !achievementId.isPresent()) {
                    return false;
                }
                
                // Check if the player already has this achievement
                boolean hasAchievement = database.queryFirst(
                        "SELECT 1 FROM player_achievements WHERE player_id = ? AND achievement_id = ? FOR UPDATE",
                        row -> true,
                        playerId.get(),
                        achievementId.get()
                ).isPresent();
                
                if (hasAchievement) {
                    return false; // Player already has this achievement
                }
                
                // Award the achievement with current timestamp
                Timestamp now = new Timestamp(System.currentTimeMillis());
                int inserted = database.update(
                        "INSERT INTO player_achievements (player_id, achievement_id, earned_at) VALUES (?, ?, ?)",
                        playerId.get(),
                        achievementId.get(),
                        now
                );
                
                return inserted > 0;
            });
        } catch (SQLException e) {
            LogUtil.severe("Error awarding achievement to player: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Awards an achievement to a player asynchronously.
     *
     * @param uuid The player's UUID
     * @param achievementIdentifier The achievement identifier
     * @return A CompletableFuture that will contain true if successful
     */
    public CompletableFuture<Boolean> awardAchievementAsync(UUID uuid, String achievementIdentifier) {
        CompletableFuture<Optional<Integer>> playerIdFuture = CompletableFuture.completedFuture(
                playerDAO.getPlayerIdByUuid(uuid));
        CompletableFuture<Optional<Integer>> achievementIdFuture = CompletableFuture.completedFuture(
                achievementDAO.getIdByIdentifier(achievementIdentifier));
        
        return CompletableFuture.allOf(playerIdFuture, achievementIdFuture)
                .thenCompose(v -> {
                    Optional<Integer> playerId = playerIdFuture.join();
                    Optional<Integer> achievementId = achievementIdFuture.join();
                    
                    if (!playerId.isPresent() || !achievementId.isPresent()) {
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    return hasAchievementAsync(uuid, achievementIdentifier)
                            .thenCompose(hasAchievement -> {
                                if (hasAchievement) {
                                    return CompletableFuture.completedFuture(false); // Player already has this achievement
                                }
                                
                                // Award the achievement with current timestamp
                                Timestamp now = new Timestamp(System.currentTimeMillis());
                                return database.updateAsync(
                                        "INSERT INTO player_achievements (player_id, achievement_id, earned_at) VALUES (?, ?, ?)",
                                        playerId.get(),
                                        achievementId.get(),
                                        now
                                ).thenApply(inserted -> inserted > 0);
                            });
                })
                .exceptionally(e -> {
                    LogUtil.severe("Error awarding achievement to player async: " + e.getMessage());
                    return false;
                });
    }
    
    /**
     * Removes an achievement from a player.
     *
     * @param uuid The player's UUID
     * @param achievementIdentifier The achievement identifier
     * @return True if successful, false otherwise
     */
    public boolean removeAchievement(UUID uuid, String achievementIdentifier) {
        try {
            Optional<Integer> playerId = playerDAO.getPlayerIdByUuid(uuid);
            Optional<Integer> achievementId = achievementDAO.getIdByIdentifier(achievementIdentifier);
            
            if (!playerId.isPresent() || !achievementId.isPresent()) {
                return false;
            }
            
            int deleted = database.update(
                    "DELETE FROM player_achievements WHERE player_id = ? AND achievement_id = ?",
                    playerId.get(),
                    achievementId.get()
            );
            
            return deleted > 0;
        } catch (SQLException e) {
            LogUtil.severe("Error removing achievement from player: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Counts the number of achievements earned by a player.
     *
     * @param uuid The player's UUID
     * @return The number of achievements earned
     */
    public int countAchievements(UUID uuid) {
        try {
            Optional<Integer> playerId = playerDAO.getPlayerIdByUuid(uuid);
            if (!playerId.isPresent()) {
                return 0;
            }
            
            return database.queryFirst(
                    "SELECT COUNT(*) as count FROM player_achievements WHERE player_id = ?",
                    row -> row.getInt("count"),
                    playerId.get()
            ).orElse(0);
        } catch (SQLException e) {
            LogUtil.severe("Error counting player achievements: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Gets the most recent achievements earned by a player.
     *
     * @param uuid The player's UUID
     * @param limit The maximum number of achievements to return
     * @return A list of recent player achievements
     */
    public List<PlayerAchievement> getRecentAchievements(UUID uuid, int limit) {
        try {
            Optional<Integer> playerId = playerDAO.getPlayerIdByUuid(uuid);
            if (!playerId.isPresent()) {
                return new ArrayList<>();
            }
            
            return database.query(
                    "SELECT pa.*, a.identifier, a.name, a.description, a.icon_material " +
                    "FROM player_achievements pa " +
                    "JOIN achievements a ON pa.achievement_id = a.id " +
                    "WHERE pa.player_id = ? " +
                    "ORDER BY pa.earned_at DESC " +
                    "LIMIT ?",
                    createJoinedPlayerAchievementMapper(),
                    playerId.get(),
                    limit
            );
        } catch (SQLException e) {
            LogUtil.severe("Error getting recent player achievements: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets the most recently awarded achievements across all players.
     *
     * @param limit The maximum number of achievements to return
     * @return A list of recent player achievements
     */
    public List<PlayerAchievement> getGlobalRecentAchievements(int limit) {
        try {
            return database.query(
                    "SELECT pa.*, a.identifier, a.name, a.description, a.icon_material " +
                    "FROM player_achievements pa " +
                    "JOIN achievements a ON pa.achievement_id = a.id " +
                    "ORDER BY pa.earned_at DESC " +
                    "LIMIT ?",
                    createJoinedPlayerAchievementMapper(),
                    limit
            );
        } catch (SQLException e) {
            LogUtil.severe("Error getting global recent player achievements: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}