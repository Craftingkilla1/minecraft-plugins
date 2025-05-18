// ./Example-Plugin/src/main/java/com/minecraft/example/sql/models/PlayerAchievement.java
package com.minecraft.example.sql.models;

import java.sql.Timestamp;

/**
 * Represents a player's earned achievement in the database.
 * Used with SQL-Bridge's result mapping.
 */
public class PlayerAchievement {
    
    private int playerId;
    private int achievementId;
    private Timestamp earnedAt;
    
    // Used for joined queries
    private String achievementIdentifier;
    private String achievementName;
    private String achievementDescription;
    private String iconMaterial;
    
    /**
     * Default constructor.
     */
    public PlayerAchievement() {
    }
    
    /**
     * Creates a new player achievement record.
     *
     * @param playerId The player's database ID
     * @param achievementId The achievement's database ID
     */
    public PlayerAchievement(int playerId, int achievementId) {
        this.playerId = playerId;
        this.achievementId = achievementId;
        this.earnedAt = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Gets the player's database ID.
     *
     * @return The player's ID
     */
    public int getPlayerId() {
        return playerId;
    }

    /**
     * Sets the player's database ID.
     *
     * @param playerId The player's ID
     */
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    /**
     * Gets the achievement's database ID.
     *
     * @return The achievement's ID
     */
    public int getAchievementId() {
        return achievementId;
    }

    /**
     * Sets the achievement's database ID.
     *
     * @param achievementId The achievement's ID
     */
    public void setAchievementId(int achievementId) {
        this.achievementId = achievementId;
    }

    /**
     * Gets the timestamp when the achievement was earned.
     *
     * @return The earned timestamp
     */
    public Timestamp getEarnedAt() {
        return earnedAt;
    }

    /**
     * Sets the timestamp when the achievement was earned.
     *
     * @param earnedAt The earned timestamp
     */
    public void setEarnedAt(Timestamp earnedAt) {
        this.earnedAt = earnedAt;
    }

    /**
     * Gets the achievement's unique identifier.
     * This is only available when joined with the achievements table.
     *
     * @return The achievement identifier
     */
    public String getAchievementIdentifier() {
        return achievementIdentifier;
    }

    /**
     * Sets the achievement's unique identifier.
     *
     * @param achievementIdentifier The achievement identifier
     */
    public void setAchievementIdentifier(String achievementIdentifier) {
        this.achievementIdentifier = achievementIdentifier;
    }

    /**
     * Gets the achievement's display name.
     * This is only available when joined with the achievements table.
     *
     * @return The achievement name
     */
    public String getAchievementName() {
        return achievementName;
    }

    /**
     * Sets the achievement's display name.
     *
     * @param achievementName The achievement name
     */
    public void setAchievementName(String achievementName) {
        this.achievementName = achievementName;
    }

    /**
     * Gets the achievement's description.
     * This is only available when joined with the achievements table.
     *
     * @return The achievement description
     */
    public String getAchievementDescription() {
        return achievementDescription;
    }

    /**
     * Sets the achievement's description.
     *
     * @param achievementDescription The achievement description
     */
    public void setAchievementDescription(String achievementDescription) {
        this.achievementDescription = achievementDescription;
    }

    /**
     * Gets the achievement's icon material name.
     * This is only available when joined with the achievements table.
     *
     * @return The icon material name
     */
    public String getIconMaterial() {
        return iconMaterial;
    }

    /**
     * Sets the achievement's icon material name.
     *
     * @param iconMaterial The icon material name
     */
    public void setIconMaterial(String iconMaterial) {
        this.iconMaterial = iconMaterial;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        PlayerAchievement that = (PlayerAchievement) o;
        return playerId == that.playerId && achievementId == that.achievementId;
    }
    
    @Override
    public int hashCode() {
        int result = playerId;
        result = 31 * result + achievementId;
        return result;
    }
    
    @Override
    public String toString() {
        return "PlayerAchievement{" +
                "playerId=" + playerId +
                ", achievementId=" + achievementId +
                ", earnedAt=" + earnedAt +
                ", achievementIdentifier='" + achievementIdentifier + '\'' +
                ", achievementName='" + achievementName + '\'' +
                ", achievementDescription='" + achievementDescription + '\'' +
                ", iconMaterial='" + iconMaterial + '\'' +
                '}';
    }
}