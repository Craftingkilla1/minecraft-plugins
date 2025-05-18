// ./Example-Plugin/src/main/java/com/minecraft/example/config/ConfigManager.java
package com.minecraft.example.config;

import com.minecraft.core.config.Messages;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.ExamplePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class ConfigManager {
    
    private final ExamplePlugin plugin;
    private FileConfiguration config;
    private YamlConfiguration messagesConfig;
    private Messages messages;
    
    // Cache configuration values
    private final Map<String, Object> configCache = new HashMap<>();
    
    public ConfigManager(ExamplePlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadMessages();
    }
    
    /**
     * Loads or reloads the main configuration file.
     */
    public void loadConfig() {
        // Create default config if it doesn't exist
        plugin.saveDefaultConfig();
        
        // Reload configuration
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        
        // Cache commonly used values
        configCache.clear();
        
        // Check debug mode and set log level
        boolean debugMode = config.getBoolean("debug", false);
        // Convert int to Level object - use FINE for debug mode
        Level logLevel = debugMode ? Level.FINE : Level.INFO;
        LogUtil.setLogLevel(logLevel);
        
        // Cache other commonly accessed values
        configCache.put("debug", debugMode);
        configCache.put("database.enabled", config.getBoolean("database.enabled", true));
        configCache.put("features.stats-tracking", config.getBoolean("features.stats-tracking", true));
        configCache.put("features.achievements", config.getBoolean("features.achievements", true));
    }
    
    /**
     * Loads the messages configuration file.
     */
    public void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        // Create default messages file if it doesn't exist
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        // Load messages configuration
        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Update with new constructor signature - passing plugin and config file
        this.messages = new Messages(plugin, messagesConfig);
    }
    
    /**
     * Gets the Messages instance.
     *
     * @return The Messages instance
     */
    public Messages getMessages() {
        return messages;
    }
    
    /**
     * Gets a boolean configuration value.
     *
     * @param path The configuration path
     * @param defaultValue The default value if not found
     * @return The configuration value
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        // Check cache first
        if (configCache.containsKey(path)) {
            Object value = configCache.get(path);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        }
        
        // Get from config
        boolean value = config.getBoolean(path, defaultValue);
        
        // Cache the value
        configCache.put(path, value);
        
        return value;
    }
    
    /**
     * Gets an integer configuration value.
     *
     * @param path The configuration path
     * @param defaultValue The default value if not found
     * @return The configuration value
     */
    public int getInt(String path, int defaultValue) {
        // Check cache first
        if (configCache.containsKey(path)) {
            Object value = configCache.get(path);
            if (value instanceof Integer) {
                return (Integer) value;
            }
        }
        
        // Get from config
        int value = config.getInt(path, defaultValue);
        
        // Cache the value
        configCache.put(path, value);
        
        return value;
    }
    
    /**
     * Gets a string configuration value.
     *
     * @param path The configuration path
     * @param defaultValue The default value if not found
     * @return The configuration value
     */
    public String getString(String path, String defaultValue) {
        // Check cache first
        if (configCache.containsKey(path)) {
            Object value = configCache.get(path);
            if (value instanceof String) {
                return (String) value;
            }
        }
        
        // Get from config
        String value = config.getString(path, defaultValue);
        
        // Cache the value
        configCache.put(path, value);
        
        return value;
    }
    
    /**
     * Gets a list of strings configuration value.
     *
     * @param path The configuration path
     * @return The configuration value, or an empty list if not found
     */
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }
    
    /**
     * Saves the configuration to disk.
     */
    public void saveConfig() {
        try {
            plugin.saveConfig();
        } catch (Exception e) {
            LogUtil.severe("Failed to save config: " + e.getMessage());
        }
    }
    
    /**
     * Saves the messages configuration to disk.
     */
    public void saveMessages() {
        try {
            messagesConfig.save(new File(plugin.getDataFolder(), "messages.yml"));
        } catch (IOException e) {
            LogUtil.severe("Failed to save messages: " + e.getMessage());
        }
    }
}