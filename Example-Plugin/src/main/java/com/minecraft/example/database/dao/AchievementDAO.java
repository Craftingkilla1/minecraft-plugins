// ./Example-Plugin/src/main/java/com/minecraft/example/database/dao/AchievementDAO.java
package com.minecraft.example.database.dao;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.PlayerStatsPlugin;
import com.minecraft.example.data.Achievement;
import com.minecraft.sqlbridge.api.Database;
import org.bukkit.Material;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object for achievements.
 */
public class AchievementDAO {
    
    private final PlayerStatsPlugin plugin;
    private final Database database;
    
    // Cache for all achievements
    private final Map<String, Achievement> achievementCache = new HashMap<>();
    
    /**
     * Creates a new AchievementDAO instance.
     *
     * @param plugin The plugin instance
     * @param database The database instance
     */
    public AchievementDAO(PlayerStatsPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }
    
    /**
     * Saves an achievement to the database.
     *
     * @param achievement The achievement to save
     * @return True if the achievement was saved, false otherwise
     * @throws SQLException If an error occurs
     */
    public boolean saveAchievement(Achievement achievement) throws SQLException {
        // Convert criteria map to JSON string
        String criteriaJson = mapToJson(achievement.getCriteria());
        
        // Insert or update achievement
        int rowsAffected = database.update(
                "INSERT INTO achievements (achievement_id, name, description, icon_material, category, secret, criteria_json) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (achievement_id) " +
                "DO UPDATE SET name = ?, description = ?, icon_material = ?, category = ?, secret = ?, criteria_json = ?",
                achievement.getId(),
                achievement.getName(),
                achievement.getDescription(),
                achievement.getIcon().name(),
                achievement.getCategory(),
                achievement.isSecret() ? 1 : 0,
                criteriaJson,
                achievement.getName(),
                achievement.getDescription(),
                achievement.getIcon().name(),
                achievement.getCategory(),
                achievement.isSecret() ? 1 : 0,
                criteriaJson
        );
        
        if (rowsAffected > 0) {
            // Update cache
            achievementCache.put(achievement.getId(), achievement);
            return true;
        }
        
        return false;
    }
    
    /**
     * Saves an achievement to the database asynchronously.
     *
     * @param achievement The achievement to save
     * @return A CompletableFuture that completes with true if the achievement was saved
     */
    public CompletableFuture<Boolean> saveAchievementAsync(Achievement achievement) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return saveAchievement(achievement);
            } catch (SQLException e) {
                LogUtil.severe("Failed to save achievement: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Gets an achievement by its ID.
     *
     * @param id The achievement ID
     * @return The achievement, or null if not found
     * @throws SQLException If an error occurs
     */
    public Achievement getAchievement(String id) throws SQLException {
        // Check cache first
        if (achievementCache.containsKey(id)) {
            return achievementCache.get(id);
        }
        
        // Query database
        return database.queryFirst(
                "SELECT achievement_id, name, description, icon_material, category, secret, criteria_json " +
                "FROM achievements WHERE achievement_id = ?",
                this::mapAchievement,
                id
        ).orElse(null);
    }
    
    /**
     * Gets an achievement by its ID asynchronously.
     *
     * @param id The achievement ID
     * @return A CompletableFuture that completes with the achievement
     */
    public CompletableFuture<Achievement> getAchievementAsync(String id) {
        // Check cache first
        if (achievementCache.containsKey(id)) {
            return CompletableFuture.completedFuture(achievementCache.get(id));
        }
        
        // Query database asynchronously
        return database.queryFirstAsync(
                "SELECT achievement_id, name, description, icon_material, category, secret, criteria_json " +
                "FROM achievements WHERE achievement_id = ?",
                this::mapAchievement,
                id
        ).thenApply(optional -> {
            Achievement achievement = optional.orElse(null);
            if (achievement != null) {
                // Update cache
                achievementCache.put(id, achievement);
            }
            return achievement;
        });
    }
    
    /**
     * Gets all achievements.
     *
     * @return A list of all achievements
     * @throws SQLException If an error occurs
     */
    public List<Achievement> getAllAchievements() throws SQLException {
        // Check if cache is populated
        if (!achievementCache.isEmpty()) {
            return new ArrayList<>(achievementCache.values());
        }
        
        List<Achievement> achievements = database.query(
                "SELECT achievement_id, name, description, icon_material, category, secret, criteria_json " +
                "FROM achievements",
                this::mapAchievement
        );
        
        // Update cache
        achievements.forEach(achievement -> achievementCache.put(achievement.getId(), achievement));
        
        return achievements;
    }
    
    /**
     * Gets all achievements asynchronously.
     *
     * @return A CompletableFuture that completes with a list of all achievements
     */
    public CompletableFuture<List<Achievement>> getAllAchievementsAsync() {
        // Check if cache is populated
        if (!achievementCache.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>(achievementCache.values()));
        }
        
        // Query database asynchronously
        return database.queryAsync(
                "SELECT achievement_id, name, description, icon_material, category, secret, criteria_json " +
                "FROM achievements",
                this::mapAchievement
        ).thenApply(achievements -> {
            // Update cache
            achievements.forEach(achievement -> achievementCache.put(achievement.getId(), achievement));
            return achievements;
        });
    }
    
    /**
     * Gets achievements by category.
     *
     * @param category The category
     * @return A list of achievements in the category
     * @throws SQLException If an error occurs
     */
    public List<Achievement> getAchievementsByCategory(String category) throws SQLException {
        // Check if cache is populated
        if (!achievementCache.isEmpty()) {
            return achievementCache.values().stream()
                    .filter(a -> category.equals(a.getCategory()))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        return database.query(
                "SELECT achievement_id, name, description, icon_material, category, secret, criteria_json " +
                "FROM achievements WHERE category = ?",
                this::mapAchievement,
                category
        );
    }
    
    /**
     * Checks if a player has earned an achievement.
     *
     * @param playerId The player's database ID
     * @param achievementId The achievement ID
     * @return True if the player has earned the achievement, false otherwise
     * @throws SQLException If an error occurs
     */
    public boolean hasAchievement(int playerId, String achievementId) throws SQLException {
        return database.queryFirst(
                "SELECT 1 FROM player_achievements WHERE player_id = ? AND achievement_id = ?",
                rs -> true,
                playerId, achievementId
        ).orElse(false);
    }
    
    /**
     * Checks if a player has earned an achievement asynchronously.
     *
     * @param playerId The player's database ID
     * @param achievementId The achievement ID
     * @return A CompletableFuture that completes with true if the player has earned the achievement
     */
    public CompletableFuture<Boolean> hasAchievementAsync(int playerId, String achievementId) {
        return database.queryFirstAsync(
                "SELECT 1 FROM player_achievements WHERE player_id = ? AND achievement_id = ?",
                rs -> true,
                playerId, achievementId
        ).thenApply(optional -> optional.orElse(false));
    }
    
    /**
     * Awards an achievement to a player.
     *
     * @param playerId The player's database ID
     * @param achievementId The achievement ID
     * @return True if the achievement was awarded, false otherwise
     * @throws SQLException If an error occurs
     */
    public boolean awardAchievement(int playerId, String achievementId) throws SQLException {
        // Check if player already has the achievement
        if (hasAchievement(playerId, achievementId)) {
            return false;
        }
        
        // Current timestamp
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        
        // Award achievement
        int rowsAffected = database.update(
                "INSERT INTO player_achievements (player_id, achievement_id, earned_date, progress) " +
                "VALUES (?, ?, ?, 1.0)",
                playerId, achievementId, now
        );
        
        return rowsAffected > 0;
    }
    
    /**
     * Awards an achievement to a player asynchronously.
     *
     * @param playerId The player's database ID
     * @param achievementId The achievement ID
     * @return A CompletableFuture that completes with true if the achievement was awarded
     */
    public CompletableFuture<Boolean> awardAchievementAsync(int playerId, String achievementId) {
        return hasAchievementAsync(playerId, achievementId).thenCompose(hasAchievement -> {
            if (hasAchievement) {
                return CompletableFuture.completedFuture(false);
            }
            
            // Current timestamp
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            
            // Award achievement
            return database.updateAsync(
                    "INSERT INTO player_achievements (player_id, achievement_id, earned_date, progress) " +
                    "VALUES (?, ?, ?, 1.0)",
                    playerId, achievementId, now
            ).thenApply(rowsAffected -> rowsAffected > 0);
        });
    }
    
    /**
     * Gets all achievements earned by a player.
     *
     * @param playerId The player's database ID
     * @return A list of achievements earned by the player
     * @throws SQLException If an error occurs
     */
    public List<Achievement> getPlayerAchievements(int playerId) throws SQLException {
        return database.query(
                "SELECT a.achievement_id, a.name, a.description, a.icon_material, a.category, a.secret, " +
                "a.criteria_json, pa.earned_date " +
                "FROM achievements a " +
                "JOIN player_achievements pa ON a.achievement_id = pa.achievement_id " +
                "WHERE pa.player_id = ?",
                rs -> {
                    Achievement achievement = mapAchievement(rs);
                    achievement.setEarnedDate(rs.getTimestamp("earned_date").toLocalDateTime());
                    achievement.setPlayerId(UUID.randomUUID()); // Placeholder, will be set externally
                    return achievement;
                },
                playerId
        );
    }
    
    /**
     * Gets all achievements earned by a player asynchronously.
     *
     * @param playerId The player's database ID
     * @return A CompletableFuture that completes with a list of achievements earned by the player
     */
    public CompletableFuture<List<Achievement>> getPlayerAchievementsAsync(int playerId) {
        return database.queryAsync(
                "SELECT a.achievement_id, a.name, a.description, a.icon_material, a.category, a.secret, " +
                "a.criteria_json, pa.earned_date " +
                "FROM achievements a " +
                "JOIN player_achievements pa ON a.achievement_id = pa.achievement_id " +
                "WHERE pa.player_id = ?",
                rs -> {
                    Achievement achievement = mapAchievement(rs);
                    achievement.setEarnedDate(rs.getTimestamp("earned_date").toLocalDateTime());
                    achievement.setPlayerId(UUID.randomUUID()); // Placeholder, will be set externally
                    return achievement;
                },
                playerId
        );
    }
    
    /**
     * Gets the most recently earned achievements for a player.
     *
     * @param playerId The player's database ID
     * @param limit The maximum number of achievements to return
     * @return A list of recently earned achievements
     * @throws SQLException If an error occurs
     */
    public List<Achievement> getRecentAchievements(int playerId, int limit) throws SQLException {
        return database.query(
                "SELECT a.achievement_id, a.name, a.description, a.icon_material, a.category, a.secret, " +
                "a.criteria_json, pa.earned_date " +
                "FROM achievements a " +
                "JOIN player_achievements pa ON a.achievement_id = pa.achievement_id " +
                "WHERE pa.player_id = ? " +
                "ORDER BY pa.earned_date DESC " +
                "LIMIT ?",
                rs -> {
                    Achievement achievement = mapAchievement(rs);
                    achievement.setEarnedDate(rs.getTimestamp("earned_date").toLocalDateTime());
                    achievement.setPlayerId(UUID.randomUUID()); // Placeholder, will be set externally
                    return achievement;
                },
                playerId, limit
        );
    }
    
    /**
     * Updates achievement progress for a player.
     *
     * @param playerId The player's database ID
     * @param achievementId The achievement ID
     * @param progress The progress value (0.0-1.0)
     * @return True if the progress was updated, false otherwise
     * @throws SQLException If an error occurs
     */
    public boolean updateProgress(int playerId, String achievementId, double progress) throws SQLException {
        // Check if player already has the achievement
        if (hasAchievement(playerId, achievementId)) {
            return false; // Already completed
        }
        
        // Current timestamp
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        
        // Update progress
        int rowsAffected = database.update(
                "INSERT INTO player_achievements (player_id, achievement_id, earned_date, progress) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (player_id, achievement_id) " +
                "DO UPDATE SET progress = ?, earned_date = CASE WHEN progress < 1.0 AND ? >= 1.0 THEN ? ELSE earned_date END",
                playerId, achievementId, now, progress, progress, progress, now
        );
        
        return rowsAffected > 0;
    }
    
    /**
     * Gets the progress of a player towards an achievement.
     *
     * @param playerId The player's database ID
     * @param achievementId The achievement ID
     * @return The progress value (0.0-1.0)
     * @throws SQLException If an error occurs
     */
    public double getProgress(int playerId, String achievementId) throws SQLException {
        return database.queryFirst(
                "SELECT progress FROM player_achievements WHERE player_id = ? AND achievement_id = ?",
                rs -> rs.getDouble("progress"),
                playerId, achievementId
        ).orElse(0.0);
    }
    
    /**
     * Gets all achievement progress for a player.
     *
     * @param playerId The player's database ID
     * @return A map of achievement IDs to progress values
     * @throws SQLException If an error occurs
     */
    public Map<String, Double> getAllProgress(int playerId) throws SQLException {
        Map<String, Double> progressMap = new HashMap<>();
        
        database.executeQuery(
                "SELECT achievement_id, progress FROM player_achievements WHERE player_id = ?",
                rs -> {
                    String achievementId = rs.getString("achievement_id");
                    double progress = rs.getDouble("progress");
                    progressMap.put(achievementId, progress);
                },
                playerId
        );
        
        return progressMap;
    }
    
    /**
     * Resets all achievements for a player.
     *
     * @param playerId The player's database ID
     * @return The number of achievements reset
     * @throws SQLException If an error occurs
     */
    public int resetAchievements(int playerId) throws SQLException {
        return database.update(
                "DELETE FROM player_achievements WHERE player_id = ?",
                playerId
        );
    }
    
    /**
     * Maps a ResultSet to an Achievement object.
     *
     * @param rs The ResultSet
     * @return The Achievement object
     * @throws SQLException If an error occurs
     */
    private Achievement mapAchievement(ResultSet rs) throws SQLException {
        String id = rs.getString("achievement_id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        Material icon = Material.valueOf(rs.getString("icon_material"));
        String category = rs.getString("category");
        boolean secret = rs.getBoolean("secret");
        String criteriaJson = rs.getString("criteria_json");
        
        Map<String, Integer> criteria = jsonToMap(criteriaJson);
        
        return Achievement.builder()
                .id(id)
                .name(name)
                .description(description)
                .icon(icon)
                .category(category)
                .secret(secret)
                .criteria(criteria)
                .build();
    }
    
    /**
     * Converts a map to a JSON string.
     *
     * @param map The map to convert
     * @return The JSON string
     */
    private String mapToJson(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            json.append(entry.getValue());
            first = false;
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Converts a JSON string to a map.
     *
     * @param json The JSON string
     * @return The map
     */
    private Map<String, Integer> jsonToMap(String json) {
        Map<String, Integer> map = new HashMap<>();
        
        if (json == null || json.isEmpty() || json.equals("{}")) {
            return map;
        }
        
        // Simple JSON parsing (for a more robust solution, use a real JSON library)
        // Remove the outer braces
        json = json.substring(1, json.length() - 1);
        
        // Split by commas, but not within quotes
        String[] pairs = json.split(",");
        
        for (String pair : pairs) {
            // Split by colon
            String[] keyValue = pair.split(":");
            
            if (keyValue.length == 2) {
                // Extract key (remove quotes)
                String key = keyValue[0].trim();
                if (key.startsWith("\"") && key.endsWith("\"")) {
                    key = key.substring(1, key.length() - 1);
                }
                
                // Extract value
                String valueStr = keyValue[1].trim();
                try {
                    int value = Integer.parseInt(valueStr);
                    map.put(key, value);
                } catch (NumberFormatException e) {
                    LogUtil.warning("Invalid number in JSON: " + valueStr);
                }
            }
        }
        
        return map;
    }
    
    /**
     * Clears the achievement cache.
     */
    public void clearCache() {
        achievementCache.clear();
    }
}