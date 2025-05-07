package com.example.exampleplugin.database;

/**
 * Data class for player information
 */
public class PlayerData {
    private int id;
    private String name;
    private int level;
    
    /**
     * Default constructor
     */
    public PlayerData() {
        this.id = 0;
        this.name = "";
        this.level = 1;
    }
    
    /**
     * Constructor with all fields
     * 
     * @param id Player ID
     * @param name Player name
     * @param level Player level
     */
    public PlayerData(int id, String name, int level) {
        this.id = id;
        this.name = name;
        this.level = level;
    }
    
    /**
     * Get player ID
     * 
     * @return Player ID
     */
    public int getId() {
        return id;
    }
    
    /**
     * Set player ID
     * 
     * @param id Player ID
     */
    public void setId(int id) {
        this.id = id;
    }
    
    /**
     * Get player name
     * 
     * @return Player name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set player name
     * 
     * @param name Player name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get player level
     * 
     * @return Player level
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Set player level
     * 
     * @param level Player level
     */
    public void setLevel(int level) {
        this.level = level;
    }
    
    @Override
    public String toString() {
        return "PlayerData{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", level=" + level +
                '}';
    }
}