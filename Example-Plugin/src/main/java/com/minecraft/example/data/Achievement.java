// ./Example-Plugin/src/main/java/com/minecraft/example/data/Achievement.java
package com.minecraft.example.data;

import org.bukkit.Material;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an achievement that can be earned by players.
 */
public class Achievement {
    
    private int id;
    private String achievementId;
    private String name;
    private String description;
    private Material icon;
    private String category;
    private boolean secret;
    private Map<String, Integer> criteria;
    private LocalDateTime earnedDate;
    private UUID playerId;
    
    /**
     * Default constructor.
     */
    public Achievement() {
        this.criteria = new HashMap<>();
    }
    
    /**
     * Creates a new Achievement with all properties.
     *
     * @param id The database ID
     * @param achievementId The achievement ID
     * @param name The achievement name
     * @param description The achievement description
     * @param icon The achievement icon
     * @param category The achievement category
     * @param secret Whether the achievement is secret
     * @param criteria The achievement criteria
     * @param earnedDate When the achievement was earned (null if not earned)
     * @param playerId The player who earned the achievement (null if not earned)
     */
    public Achievement(int id, String achievementId, String name, String description, Material icon, 
                      String category, boolean secret, Map<String, Integer> criteria, 
                      LocalDateTime earnedDate, UUID playerId) {
        this.id = id;
        this.achievementId = achievementId;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.category = category;
        this.secret = secret;
        this.criteria = criteria != null ? criteria : new HashMap<>();
        this.earnedDate = earnedDate;
        this.playerId = playerId;
    }
    
    /**
     * Gets the database ID.
     *
     * @return The database ID
     */
    public int getId() {
        return id;
    }
    
    /**
     * Sets the database ID.
     *
     * @param id The database ID
     */
    public void setId(int id) {
        this.id = id;
    }
    
    /**
     * Gets the achievement ID.
     *
     * @return The achievement ID
     */
    public String getAchievementId() {
        return achievementId;
    }
    
    /**
     * Sets the achievement ID.
     *
     * @param achievementId The achievement ID
     */
    public void setAchievementId(String achievementId) {
        this.achievementId = achievementId;
    }
    
    /**
     * Gets the achievement name.
     *
     * @return The achievement name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the achievement name.
     *
     * @param name The achievement name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Gets the achievement description.
     *
     * @return The achievement description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the achievement description.
     *
     * @param description The achievement description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Gets the achievement icon.
     *
     * @return The achievement icon
     */
    public Material getIcon() {
        return icon;
    }
    
    /**
     * Sets the achievement icon.
     *
     * @param icon The achievement icon
     */
    public void setIcon(Material icon) {
        this.icon = icon;
    }
    
    /**
     * Gets the achievement category.
     *
     * @return The achievement category
     */
    public String getCategory() {
        return category;
    }
    
    /**
     * Sets the achievement category.
     *
     * @param category The achievement category
     */
    public void setCategory(String category) {
        this.category = category;
    }
    
    /**
     * Checks if the achievement is secret.
     *
     * @return True if the achievement is secret, false otherwise
     */
    public boolean isSecret() {
        return secret;
    }
    
    /**
     * Sets whether the achievement is secret.
     *
     * @param secret Whether the achievement is secret
     */
    public void setSecret(boolean secret) {
        this.secret = secret;
    }
    
    /**
     * Gets the achievement criteria.
     *
     * @return The achievement criteria
     */
    public Map<String, Integer> getCriteria() {
        return criteria;
    }
    
    /**
     * Sets the achievement criteria.
     *
     * @param criteria The achievement criteria
     */
    public void setCriteria(Map<String, Integer> criteria) {
        this.criteria = criteria != null ? criteria : new HashMap<>();
    }
    
    /**
     * Gets when the achievement was earned.
     *
     * @return When the achievement was earned
     */
    public LocalDateTime getEarnedDate() {
        return earnedDate;
    }
    
    /**
     * Sets when the achievement was earned.
     *
     * @param earnedDate When the achievement was earned
     */
    public void setEarnedDate(LocalDateTime earnedDate) {
        this.earnedDate = earnedDate;
    }
    
    /**
     * Gets the player who earned the achievement.
     *
     * @return The player who earned the achievement
     */
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * Sets the player who earned the achievement.
     *
     * @param playerId The player who earned the achievement
     */
    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }
    
    /**
     * Checks if all criteria for the achievement are met.
     *
     * @param playerStats The player's statistics
     * @return True if all criteria are met, false otherwise
     */
    public boolean checkCriteria(Map<String, Integer> playerStats) {
        if (criteria == null || criteria.isEmpty() || playerStats == null) {
            return false;
        }
        
        for (Map.Entry<String, Integer> entry : criteria.entrySet()) {
            String statName = entry.getKey();
            int requiredValue = entry.getValue();
            
            int playerValue = playerStats.getOrDefault(statName, 0);
            if (playerValue < requiredValue) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Calculates the progress towards completing the achievement.
     *
     * @param playerStats The player's statistics
     * @return Progress as a value between 0.0 and 1.0
     */
    public double calculateProgress(Map<String, Integer> playerStats) {
        if (criteria == null || criteria.isEmpty() || playerStats == null) {
            return 0.0;
        }
        
        double totalProgress = 0.0;
        int criteriaCount = criteria.size();
        
        for (Map.Entry<String, Integer> entry : criteria.entrySet()) {
            String statName = entry.getKey();
            int requiredValue = entry.getValue();
            
            int playerValue = playerStats.getOrDefault(statName, 0);
            double criterionProgress = Math.min(1.0, (double) playerValue / requiredValue);
            
            totalProgress += criterionProgress;
        }
        
        return totalProgress / criteriaCount;
    }
    
    /**
     * Builder for creating Achievement instances.
     */
    public static class Builder {
        private int id;
        private String achievementId;
        private String name;
        private String description;
        private Material icon;
        private String category;
        private boolean secret;
        private Map<String, Integer> criteria = new HashMap<>();
        private LocalDateTime earnedDate;
        private UUID playerId;
        
        public Builder id(int id) {
            this.id = id;
            return this;
        }
        
        public Builder achievementId(String achievementId) {
            this.achievementId = achievementId;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder icon(Material icon) {
            this.icon = icon;
            return this;
        }
        
        public Builder category(String category) {
            this.category = category;
            return this;
        }
        
        public Builder secret(boolean secret) {
            this.secret = secret;
            return this;
        }
        
        public Builder criteria(Map<String, Integer> criteria) {
            this.criteria = criteria != null ? criteria : new HashMap<>();
            return this;
        }
        
        public Builder earnedDate(LocalDateTime earnedDate) {
            this.earnedDate = earnedDate;
            return this;
        }
        
        public Builder playerId(UUID playerId) {
            this.playerId = playerId;
            return this;
        }
        
        public Achievement build() {
            return new Achievement(id, achievementId, name, description, icon, 
                                  category, secret, criteria, earnedDate, playerId);
        }
    }
    
    /**
     * Creates a new builder instance.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}