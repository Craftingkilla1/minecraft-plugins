// ./src/main/java/com/minecraft/example/model/PlayerStats.java
package com.minecraft.example.model;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Model class for player statistics
 */
public class PlayerStats {
    
    private int id;
    private UUID playerUuid;
    private String playerName;
    private int kills;
    private int deaths;
    private int blocksPlaced;
    private int blocksBroken;
    private int timePlayed;
    private Timestamp lastSeen;
    
    /**
     * Default constructor
     */
    public PlayerStats() {
    }
    
    /**
     * Constructor with UUID and name
     * @param playerUuid Player UUID
     * @param playerName Player name
     */
    public PlayerStats(UUID playerUuid, String playerName) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.kills = 0;
        this.deaths = 0;
        this.blocksPlaced = 0;
        this.blocksBroken = 0;
        this.timePlayed = 0;
        this.lastSeen = new Timestamp(System.currentTimeMillis());
    }
    
    /**
     * Full constructor
     * @param id Database ID
     * @param playerUuid Player UUID
     * @param playerName Player name
     * @param kills Kills count
     * @param deaths Deaths count
     * @param blocksPlaced Blocks placed count
     * @param blocksBroken Blocks broken count
     * @param timePlayed Time played in seconds
     * @param lastSeen Last seen timestamp
     */
    public PlayerStats(int id, UUID playerUuid, String playerName, int kills, int deaths, 
                       int blocksPlaced, int blocksBroken, int timePlayed, Timestamp lastSeen) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.kills = kills;
        this.deaths = deaths;
        this.blocksPlaced = blocksPlaced;
        this.blocksBroken = blocksBroken;
        this.timePlayed = timePlayed;
        this.lastSeen = lastSeen;
    }
    
    /**
     * Get the database ID
     * @return Database ID
     */
    public int getId() {
        return id;
    }
    
    /**
     * Set the database ID
     * @param id Database ID
     */
    public void setId(int id) {
        this.id = id;
    }
    
    /**
     * Get the player UUID
     * @return Player UUID
     */
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    /**
     * Set the player UUID
     * @param playerUuid Player UUID
     */
    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }
    
    /**
     * Get the player name
     * @return Player name
     */
    public String getPlayerName() {
        return playerName;
    }
    
    /**
     * Set the player name
     * @param playerName Player name
     */
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    /**
     * Get the kills count
     * @return Kills count
     */
    public int getKills() {
        return kills;
    }
    
    /**
     * Set the kills count
     * @param kills Kills count
     */
    public void setKills(int kills) {
        this.kills = kills;
    }
    
    /**
     * Get the deaths count
     * @return Deaths count
     */
    public int getDeaths() {
        return deaths;
    }
    
    /**
     * Set the deaths count
     * @param deaths Deaths count
     */
    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }
    
    /**
     * Get the blocks placed count
     * @return Blocks placed count
     */
    public int getBlocksPlaced() {
        return blocksPlaced;
    }
    
    /**
     * Set the blocks placed count
     * @param blocksPlaced Blocks placed count
     */
    public void setBlocksPlaced(int blocksPlaced) {
        this.blocksPlaced = blocksPlaced;
    }
    
    /**
     * Get the blocks broken count
     * @return Blocks broken count
     */
    public int getBlocksBroken() {
        return blocksBroken;
    }
    
    /**
     * Set the blocks broken count
     * @param blocksBroken Blocks broken count
     */
    public void setBlocksBroken(int blocksBroken) {
        this.blocksBroken = blocksBroken;
    }
    
    /**
     * Get the time played in seconds
     * @return Time played in seconds
     */
    public int getTimePlayed() {
        return timePlayed;
    }
    
    /**
     * Set the time played in seconds
     * @param timePlayed Time played in seconds
     */
    public void setTimePlayed(int timePlayed) {
        this.timePlayed = timePlayed;
    }
    
    /**
     * Get the last seen timestamp
     * @return Last seen timestamp
     */
    public Timestamp getLastSeen() {
        return lastSeen;
    }
    
    /**
     * Set the last seen timestamp
     * @param lastSeen Last seen timestamp
     */
    public void setLastSeen(Timestamp lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    /**
     * Get the K/D ratio
     * @return K/D ratio or 0 if no deaths
     */
    public double getKdRatio() {
        if (deaths == 0) {
            return kills; // Avoid division by zero
        }
        return (double) kills / deaths;
    }
    
    /**
     * Format time played as a readable string
     * @return Formatted time string (e.g., "2h 30m")
     */
    public String getFormattedTimePlayed() {
        int hours = timePlayed / 3600;
        int minutes = (timePlayed % 3600) / 60;
        
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
}