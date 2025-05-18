// ./Example-Plugin/src/main/java/com/minecraft/example/sql/models/PlayerStats.java
package com.minecraft.example.sql.models;

import java.time.LocalDateTime;

/**
 * Represents player statistics stored in the database.
 */
public class PlayerStats {
    
    private int id;
    private int playerId;
    private String name;
    private int value;
    private LocalDateTime lastUpdated;
    
    /**
     * Default constructor.
     */
    public PlayerStats() {
        this.lastUpdated = LocalDateTime.now();
    }
    
    /**
     * Creates a new PlayerStats with specified values.
     *
     * @param id The database ID
     * @param playerId The player ID
     * @param name The statistic name
     * @param value The statistic value
     * @param lastUpdated When the statistic was last updated
     */
    public PlayerStats(int id, int playerId, String name, int value, LocalDateTime lastUpdated) {
        this.id = id;
        this.playerId = playerId;
        this.name = name;
        this.value = value;
        this.lastUpdated = lastUpdated != null ? lastUpdated : LocalDateTime.now();
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
     * Gets the player ID.
     *
     * @return The player ID
     */
    public int getPlayerId() {
        return playerId;
    }
    
    /**
     * Sets the player ID.
     *
     * @param playerId The player ID
     */
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }
    
    /**
     * Gets the statistic name.
     *
     * @return The statistic name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the statistic name.
     *
     * @param name The statistic name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Gets the statistic value.
     *
     * @return The statistic value
     */
    public int getValue() {
        return value;
    }
    
    /**
     * Sets the statistic value.
     *
     * @param value The statistic value
     */
    public void setValue(int value) {
        this.value = value;
        this.lastUpdated = LocalDateTime.now();
    }
    
    /**
     * Gets when the statistic was last updated.
     *
     * @return When the statistic was last updated
     */
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    /**
     * Sets when the statistic was last updated.
     *
     * @param lastUpdated When the statistic was last updated
     */
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    /**
     * Increments the statistic value by a specified amount.
     *
     * @param amount The amount to increment by
     */
    public void increment(int amount) {
        this.value += amount;
        this.lastUpdated = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "PlayerStats{" +
                "id=" + id +
                ", playerId=" + playerId +
                ", name='" + name + '\'' +
                ", value=" + value +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}