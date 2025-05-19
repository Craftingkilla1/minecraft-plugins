// ./src/main/java/com/minecraft/example/model/PlayerStats.java
package com.minecraft.example.model;

import java.util.Date;
import java.util.UUID;

/**
 * Data model representing player statistics
 */
public class PlayerStats {
    
    private UUID uuid;
    private String name;
    private Date firstJoin;
    private Date lastJoin;
    private long playtimeSeconds;
    private int loginCount;
    private int blocksBroken;
    private int blocksPlaced;
    
    /**
     * Default constructor
     */
    public PlayerStats() {
        this.firstJoin = new Date();
        this.lastJoin = new Date();
        this.playtimeSeconds = 0;
        this.loginCount = 1;
        this.blocksBroken = 0;
        this.blocksPlaced = 0;
    }
    
    /**
     * Constructor for a new player
     * 
     * @param uuid Player UUID
     * @param name Player name
     */
    public PlayerStats(UUID uuid, String name) {
        this();
        this.uuid = uuid;
        this.name = name;
    }
    
    /**
     * Full constructor
     */
    public PlayerStats(UUID uuid, String name, Date firstJoin, Date lastJoin, 
                     long playtimeSeconds, int loginCount, int blocksBroken, int blocksPlaced) {
        this.uuid = uuid;
        this.name = name;
        this.firstJoin = firstJoin;
        this.lastJoin = lastJoin;
        this.playtimeSeconds = playtimeSeconds;
        this.loginCount = loginCount;
        this.blocksBroken = blocksBroken;
        this.blocksPlaced = blocksPlaced;
    }
    
    // Getters and Setters
    
    public UUID getUuid() {
        return uuid;
    }
    
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Date getFirstJoin() {
        return firstJoin;
    }
    
    public void setFirstJoin(Date firstJoin) {
        this.firstJoin = firstJoin;
    }
    
    public Date getLastJoin() {
        return lastJoin;
    }
    
    public void setLastJoin(Date lastJoin) {
        this.lastJoin = lastJoin;
    }
    
    public long getPlaytimeSeconds() {
        return playtimeSeconds;
    }
    
    public void setPlaytimeSeconds(long playtimeSeconds) {
        this.playtimeSeconds = playtimeSeconds;
    }
    
    public int getLoginCount() {
        return loginCount;
    }
    
    public void setLoginCount(int loginCount) {
        this.loginCount = loginCount;
    }
    
    public int getBlocksBroken() {
        return blocksBroken;
    }
    
    public void setBlocksBroken(int blocksBroken) {
        this.blocksBroken = blocksBroken;
    }
    
    public int getBlocksPlaced() {
        return blocksPlaced;
    }
    
    public void setBlocksPlaced(int blocksPlaced) {
        this.blocksPlaced = blocksPlaced;
    }
    
    /**
     * Format playtime as a human-readable string (e.g., "2h 30m 15s")
     * 
     * @return Formatted playtime string
     */
    public String getFormattedPlaytime() {
        long seconds = playtimeSeconds;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(seconds).append("s");
        
        return sb.toString();
    }
}