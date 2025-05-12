// ./Example-Plugin/src/main/java/com/example/exampleplugin/PlayerData.java
package com.example.exampleplugin;

import java.util.UUID;

/**
 * Represents player statistics stored in the database.
 */
public class PlayerData {
    
    private final UUID uuid;
    private final String name;
    private int blocksBroken;
    private int blocksPlaced;
    private int mobsKilled;
    private int deaths;
    private long lastSeen;
    private final long firstJoin;
    private boolean dirty;
    
    /**
     * Constructor for PlayerData.
     *
     * @param uuid The player's UUID
     * @param name The player's name
     * @param blocksBroken The number of blocks broken
     * @param blocksPlaced The number of blocks placed
     * @param mobsKilled The number of mobs killed
     * @param deaths The number of deaths
     * @param lastSeen The timestamp of last seen
     * @param firstJoin The timestamp of first join
     */
    public PlayerData(UUID uuid, String name, int blocksBroken, int blocksPlaced, 
                     int mobsKilled, int deaths, long lastSeen, long firstJoin) {
        this.uuid = uuid;
        this.name = name;
        this.blocksBroken = blocksBroken;
        this.blocksPlaced = blocksPlaced;
        this.mobsKilled = mobsKilled;
        this.deaths = deaths;
        this.lastSeen = lastSeen;
        this.firstJoin = firstJoin;
        this.dirty = false;
    }
    
    /**
     * Create a new PlayerData instance for a new player.
     *
     * @param uuid The player's UUID
     * @param name The player's name
     * @return A new PlayerData instance
     */
    public static PlayerData createNew(UUID uuid, String name) {
        long now = System.currentTimeMillis();
        return new PlayerData(uuid, name, 0, 0, 0, 0, now, now);
    }
    
    /**
     * Get the player's UUID.
     *
     * @return The player's UUID
     */
    public UUID getUuid() {
        return uuid;
    }
    
    /**
     * Get the player's name.
     *
     * @return The player's name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the number of blocks broken.
     *
     * @return The number of blocks broken
     */
    public int getBlocksBroken() {
        return blocksBroken;
    }
    
    /**
     * Increment the number of blocks broken.
     */
    public void incrementBlocksBroken() {
        blocksBroken++;
        dirty = true;
    }
    
    /**
     * Get the number of blocks placed.
     *
     * @return The number of blocks placed
     */
    public int getBlocksPlaced() {
        return blocksPlaced;
    }
    
    /**
     * Increment the number of blocks placed.
     */
    public void incrementBlocksPlaced() {
        blocksPlaced++;
        dirty = true;
    }
    
    /**
     * Get the number of mobs killed.
     *
     * @return The number of mobs killed
     */
    public int getMobsKilled() {
        return mobsKilled;
    }
    
    /**
     * Increment the number of mobs killed.
     */
    public void incrementMobsKilled() {
        mobsKilled++;
        dirty = true;
    }
    
    /**
     * Get the number of deaths.
     *
     * @return The number of deaths
     */
    public int getDeaths() {
        return deaths;
    }
    
    /**
     * Increment the number of deaths.
     */
    public void incrementDeaths() {
        deaths++;
        dirty = true;
    }
    
    /**
     * Get the timestamp of last seen.
     *
     * @return The timestamp of last seen
     */
    public long getLastSeen() {
        return lastSeen;
    }
    
    /**
     * Update the last seen timestamp.
     *
     * @param lastSeen The new timestamp
     */
    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
        dirty = true;
    }
    
    /**
     * Get the timestamp of first join.
     *
     * @return The timestamp of first join
     */
    public long getFirstJoin() {
        return firstJoin;
    }
    
    /**
     * Reset all player statistics.
     */
    public void resetStats() {
        blocksBroken = 0;
        blocksPlaced = 0;
        mobsKilled = 0;
        deaths = 0;
        dirty = true;
    }
    
    /**
     * Check if the data has been modified and needs to be saved.
     *
     * @return true if the data has been modified, false otherwise
     */
    public boolean isDirty() {
        return dirty;
    }
    
    /**
     * Mark the data as clean after saving.
     */
    public void markClean() {
        dirty = false;
    }
}