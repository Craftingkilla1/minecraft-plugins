// ./src/main/java/com/minecraft/example/config/ConfigManager.java
package com.minecraft.example.config;

import com.minecraft.core.config.Messages;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.ExamplePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Configuration manager for Example-Plugin
 * Demonstrates Core-Utils configuration management
 */
public class ConfigManager {
    
    private final ExamplePlugin plugin;
    
    private FileConfiguration config;
    private Messages messages;
    
    private boolean debugMode;
    private boolean trackKills;
    private boolean trackDeaths;
    private boolean trackBlocks;
    private boolean trackTimePlayed;
    
    /**
     * Constructor
     * @param plugin Plugin instance
     */
    public ConfigManager(ExamplePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load the plugin configuration
     */
    public void loadConfig() {
        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();
        
        // Load config
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Load settings
        loadSettings();
        
        // Load messages
        loadMessages();
        
        // Set debug mode in LogUtil
        LogUtil.setDebugMode(debugMode);
    }
    
    /**
     * Load settings from config
     */
    private void loadSettings() {
        debugMode = config.getBoolean("debug", false);
        trackKills = config.getBoolean("tracking.kills", true);
        trackDeaths = config.getBoolean("tracking.deaths", true);
        trackBlocks = config.getBoolean("tracking.blocks", true);
        trackTimePlayed = config.getBoolean("tracking.time_played", true);
        
        LogUtil.info("Settings loaded: " +
                     "debug=" + debugMode + ", " +
                     "trackKills=" + trackKills + ", " +
                     "trackDeaths=" + trackDeaths + ", " +
                     "trackBlocks=" + trackBlocks + ", " +
                     "trackTimePlayed=" + trackTimePlayed);
    }
    
    /**
     * Load messages from messages.yml
     */
    private void loadMessages() {
        // Save default messages.yml if it doesn't exist
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        // Load messages
        messages = new Messages(plugin);
        
        LogUtil.info("Messages loaded");
    }
    
    /**
     * Reload the configuration
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadSettings();
        loadMessages();
    }
    
    /**
     * Save the configuration
     */
    public void saveConfig() {
        try {
            plugin.saveConfig();
        } catch (Exception e) {
            LogUtil.severe("Failed to save config: " + e.getMessage());
        }
    }
    
    /**
     * Get the plugin configuration
     * @return FileConfiguration
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * Get the messages handler
     * @return Messages
     */
    public Messages getMessages() {
        return messages;
    }
    
    /**
     * Check if debug mode is enabled
     * @return true if debug mode is enabled
     */
    public boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * Check if kill tracking is enabled
     * @return true if kill tracking is enabled
     */
    public boolean isTrackKills() {
        return trackKills;
    }
    
    /**
     * Check if death tracking is enabled
     * @return true if death tracking is enabled
     */
    public boolean isTrackDeaths() {
        return trackDeaths;
    }
    
    /**
     * Check if block tracking is enabled
     * @return true if block tracking is enabled
     */
    public boolean isTrackBlocks() {
        return trackBlocks;
    }
    
    /**
     * Check if time played tracking is enabled
     * @return true if time played tracking is enabled
     */
    public boolean isTrackTimePlayed() {
        return trackTimePlayed;
    }
}