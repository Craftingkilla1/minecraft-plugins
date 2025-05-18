// ./Example-Plugin/src/main/java/com/minecraft/example/sql/models/Player.java
package com.minecraft.example.sql.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a player in the database.
 */
public class Player {
    
    private int id;
    private UUID uuid;
    private String name;
    private LocalDateTime firstJoin;
    private LocalDateTime lastSeen;
    private long playtime;
    
    /**
     * Default constructor.
     */
    public Player() {
        this.firstJoin = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
        this.playtime = 0;
    }
    
    /**
     * Creates a new Player with specified values.
     *
     * @param id The database ID
     * @param uuid The player UUID
     * @param name The player name
     * @param firstJoin When the player first joined
     * @param lastSeen When the player was last seen
     * @param playtime The player's playtime in seconds
     */
    public Player(int id, UUID uuid, String name, LocalDateTime firstJoin, LocalDateTime lastSeen, long playtime) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.firstJoin = firstJoin != null ? firstJoin : LocalDateTime.now();
        this.lastSeen = lastSeen != null ? lastSeen : LocalDateTime.now();
        this.playtime = playtime;
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
     * Gets the player UUID.
     *
     * @return The player UUID
     */
    public UUID getUuid() {
        return uuid;
    }
    
    /**
     * Sets the player UUID.
     *
     * @param uuid The player UUID
     */
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    
    /**
     * Gets the player name.
     *
     * @return The player name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the player name.
     *
     * @param name The player name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Gets when the player first joined.
     *
     * @return When the player first joined
     */
    public LocalDateTime getFirstJoin() {
        return firstJoin;
    }
    
    /**
     * Sets when the player first joined.
     *
     * @param firstJoin When the player first joined
     */
    public void setFirstJoin(LocalDateTime firstJoin) {
        this.firstJoin = firstJoin;
    }
    
    /**
     * Gets when the player was last seen.
     *
     * @return When the player was last seen
     */
    public LocalDateTime getLastSeen() {
        return lastSeen;
    }
    
    /**
     * Sets when the player was last seen.
     *
     * @param lastSeen When the player was last seen
     */
    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    /**
     * Gets the player's playtime in seconds.
     *
     * @return The player's playtime in seconds
     */
    public long getPlaytime() {
        return playtime;
    }
    
    /**
     * Sets the player's playtime in seconds.
     *
     * @param playtime The player's playtime in seconds
     */
    public void setPlaytime(long playtime) {
        this.playtime = playtime;
    }
    
    /**
     * Updates the last seen time to now.
     */
    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
    }
    
    /**
     * Adds playtime in seconds.
     *
     * @param seconds The seconds to add
     */
    public void addPlaytime(long seconds) {
        this.playtime += seconds;
    }
    
    @Override
    public String toString() {
        return "Player{" +
                "id=" + id +
                ", uuid=" + uuid +
                ", name='" + name + '\'' +
                ", firstJoin=" + firstJoin +
                ", lastSeen=" + lastSeen +
                ", playtime=" + playtime +
                '}';
    }
}