// ./Example-Plugin/src/main/java/com/minecraft/example/sql/dao/AchievementDAO.java
package com.minecraft.example.sql.dao;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.sql.models.Achievement;
import com.minecraft.sqlbridge.api.Database;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object for Achievement entities.
 * Demonstrates SQL-Bridge DAO pattern implementation.
 */
public class AchievementDAO {
    
    private final Database database;
    private final Plugin plugin;
    
    /**
     * Constructs a new AchievementDAO.
     *
     * @param database The database instance
     * @param plugin The plugin instance
     */
    public AchievementDAO(Database database, Plugin plugin) {
        this.database = database;
        this.plugin = plugin;
    }
    
    /**
     * Creates the achievement mapper.
     *
     * @return The achievement result mapper
     */
    private com.minecraft.sqlbridge.api.result.ResultMapper<Achievement> createAchievementMapper() {
        return row -> {
            Achievement achievement = new Achievement();
            achievement.setId(row.getInt("id"));
            achievement.setIdentifier(row.getString("identifier"));
            achievement.setName(row.getString("name"));
            achievement.setDescription(row.getString("description"));
            achievement.setIconMaterial(row.getString("icon_material"));
            achievement.setCreatedAt(row.getTimestamp("created_at"));
            return achievement;
        };
    }
    
    /**
     * Finds an achievement by its unique identifier.
     *
     * @param identifier The achievement identifier
     * @return An Optional containing the achievement if found
     */
    public Optional<Achievement> findByIdentifier(String identifier) {
        try {
            return database.queryFirst(
                    "SELECT * FROM achievements WHERE identifier = ?",
                    createAchievementMapper(),
                    identifier
            );
        } catch (SQLException e) {
            LogUtil.severe("Error finding achievement by identifier: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Finds an achievement by its database ID.
     *
     * @param id The achievement ID
     * @return An Optional containing the achievement if found
     */
    public Optional<Achievement> findById(int id) {
        try {
            return database.queryFirst(
                    "SELECT * FROM achievements WHERE id = ?",
                    createAchievementMapper(),
                    id
            );
        } catch (SQLException e) {
            LogUtil.severe("Error finding achievement by ID: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Gets all achievements.
     *
     * @return A list of all achievements
     */
    public List<Achievement> findAll() {
        try {
            return database.query(
                    "SELECT * FROM achievements ORDER BY identifier",
                    createAchievementMapper()
            );
        } catch (SQLException e) {
            LogUtil.severe("Error retrieving all achievements: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets all achievements asynchronously.
     *
     * @return A CompletableFuture that will contain a list of all achievements
     */
    public CompletableFuture<List<Achievement>> findAllAsync() {
        return database.queryAsync(
                "SELECT * FROM achievements ORDER BY identifier",
                createAchievementMapper()
        ).exceptionally(e -> {
            LogUtil.severe("Error retrieving all achievements async: " + e.getMessage());
            return new ArrayList<>();
        });
    }
    
    /**
     * Creates or updates an achievement.
     *
     * @param achievement The achievement to save
     * @return True if successful, false otherwise
     */
    public boolean save(Achievement achievement) {
        try {
            // Check if achievement exists
            Optional<Achievement> existing = findByIdentifier(achievement.getIdentifier());
            
            if (existing.isPresent()) {
                // Update existing achievement
                int updated = database.update(
                        "UPDATE achievements SET " +
                        "name = ?, " +
                        "description = ?, " +
                        "icon_material = ? " +
                        "WHERE identifier = ?",
                        achievement.getName(),
                        achievement.getDescription(),
                        achievement.getIconMaterial(),
                        achievement.getIdentifier()
                );
                
                if (updated > 0) {
                    // Set ID from existing achievement
                    achievement.setId(existing.get().getId());
                }
                
                return updated > 0;
            } else {
                // Insert new achievement
                int inserted = database.update(
                        "INSERT INTO achievements (identifier, name, description, icon_material) " +
                        "VALUES (?, ?, ?, ?)",
                        achievement.getIdentifier(),
                        achievement.getName(),
                        achievement.getDescription(),
                        achievement.getIconMaterial()
                );
                
                if (inserted > 0) {
                    // Get the generated ID and set it in the achievement object
                    Optional<Integer> id = database.queryFirst(
                            "SELECT id FROM achievements WHERE identifier = ?",
                            row -> row.getInt("id"),
                            achievement.getIdentifier()
                    );
                    id.ifPresent(achievement::setId);
                }
                
                return inserted > 0;
            }
        } catch (SQLException e) {
            LogUtil.severe("Error saving achievement: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Creates or updates an achievement asynchronously.
     *
     * @param achievement The achievement to save
     * @return A CompletableFuture that will contain true if successful
     */
    public CompletableFuture<Boolean> saveAsync(Achievement achievement) {
        return database.queryFirstAsync(
                "SELECT * FROM achievements WHERE identifier = ?",
                createAchievementMapper(),
                achievement.getIdentifier()
        ).thenCompose(existing -> {
            if (existing.isPresent()) {
                // Update existing achievement
                return database.updateAsync(
                        "UPDATE achievements SET " +
                        "name = ?, " +
                        "description = ?, " +
                        "icon_material = ? " +
                        "WHERE identifier = ?",
                        achievement.getName(),
                        achievement.getDescription(),
                        achievement.getIconMaterial(),
                        achievement.getIdentifier()
                ).thenApply(updated -> {
                    if (updated > 0) {
                        // Set ID from existing achievement
                        achievement.setId(existing.get().getId());
                    }
                    return updated > 0;
                });
            } else {
                // Insert new achievement
                return database.updateAsync(
                        "INSERT INTO achievements (identifier, name, description, icon_material) " +
                        "VALUES (?, ?, ?, ?)",
                        achievement.getIdentifier(),
                        achievement.getName(),
                        achievement.getDescription(),
                        achievement.getIconMaterial()
                ).thenCompose(inserted -> {
                    if (inserted > 0) {
                        // Get the generated ID and set it in the achievement object
                        return database.queryFirstAsync(
                                "SELECT id FROM achievements WHERE identifier = ?",
                                row -> row.getInt("id"),
                                achievement.getIdentifier()
                        ).thenApply(id -> {
                            id.ifPresent(achievement::setId);
                            return true;
                        });
                    }
                    return CompletableFuture.completedFuture(inserted > 0);
                });
            }
        }).exceptionally(e -> {
            LogUtil.severe("Error saving achievement async: " + e.getMessage());
            return false;
        });
    }
    
    /**
     * Deletes an achievement by its identifier.
     *
     * @param identifier The achievement identifier
     * @return True if successful, false otherwise
     */
    public boolean deleteByIdentifier(String identifier) {
        try {
            int deleted = database.update(
                    "DELETE FROM achievements WHERE identifier = ?",
                    identifier
            );
            return deleted > 0;
        } catch (SQLException e) {
            LogUtil.severe("Error deleting achievement: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if an achievement exists by its identifier.
     *
     * @param identifier The achievement identifier
     * @return True if the achievement exists, false otherwise
     */
    public boolean exists(String identifier) {
        try {
            return database.queryFirst(
                    "SELECT 1 FROM achievements WHERE identifier = ?",
                    row -> true,
                    identifier
            ).isPresent();
        } catch (SQLException e) {
            LogUtil.severe("Error checking if achievement exists: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the achievement ID by identifier.
     *
     * @param identifier The achievement identifier
     * @return An Optional containing the achievement ID if found
     */
    public Optional<Integer> getIdByIdentifier(String identifier) {
        try {
            return database.queryFirst(
                    "SELECT id FROM achievements WHERE identifier = ?",
                    row -> row.getInt("id"),
                    identifier
            );
        } catch (SQLException e) {
            LogUtil.severe("Error getting achievement ID by identifier: " + e.getMessage());
            return Optional.empty();
        }
    }
}