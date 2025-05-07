package com.minecraft.core.config;

import com.minecraft.core.CorePlugin;
import com.minecraft.core.utils.LogUtil;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager for plugin configuration
 */
public class ConfigManager {
    private final Plugin plugin;
    private FileConfiguration config;
    private final Map<String, YamlConfiguration> configFiles = new HashMap<>();
    
    /**
     * Create a new config manager
     * 
     * @param plugin The plugin instance
     */
    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load the main configuration
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        LogUtil.info("Loaded main configuration");
    }
    
    /**
     * Reload the main configuration
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        LogUtil.info("Reloaded main configuration");
    }
    
    /**
     * Save the main configuration
     */
    public void saveConfig() {
        plugin.saveConfig();
        LogUtil.info("Saved main configuration");
    }
    
    /**
     * Get the main configuration
     * 
     * @return The main configuration
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * Load a configuration file
     * 
     * @param name The name of the file (without .yml extension)
     * @return The loaded configuration, or null if loading failed
     */
    public YamlConfiguration loadConfigFile(String name) {
        File file = new File(plugin.getDataFolder(), name + ".yml");
        
        // Save default if it doesn't exist
        if (!file.exists()) {
            try {
                plugin.saveResource(name + ".yml", false);
                LogUtil.info("Created default " + name + ".yml");
            } catch (IllegalArgumentException e) {
                try {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                    LogUtil.info("Created empty " + name + ".yml");
                } catch (IOException ex) {
                    LogUtil.severe("Failed to create " + name + ".yml: " + ex.getMessage());
                    return null;
                }
            }
        }
        
        // Load the configuration
        YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(file);
        configFiles.put(name, yamlConfig);
        LogUtil.info("Loaded " + name + ".yml");
        
        return yamlConfig;
    }
    
    /**
     * Get a configuration file
     * 
     * @param name The name of the file (without .yml extension)
     * @return The configuration, or null if not loaded
     */
    public YamlConfiguration getConfigFile(String name) {
        // Load if not already loaded
        if (!configFiles.containsKey(name)) {
            return loadConfigFile(name);
        }
        
        return configFiles.get(name);
    }
    
    /**
     * Save a configuration file
     * 
     * @param name The name of the file (without .yml extension)
     * @return true if saving was successful
     */
    public boolean saveConfigFile(String name) {
        if (!configFiles.containsKey(name)) {
            LogUtil.warning("Cannot save " + name + ".yml - not loaded");
            return false;
        }
        
        YamlConfiguration yamlConfig = configFiles.get(name);
        File file = new File(plugin.getDataFolder(), name + ".yml");
        
        try {
            yamlConfig.save(file);
            LogUtil.info("Saved " + name + ".yml");
            return true;
        } catch (IOException e) {
            LogUtil.severe("Failed to save " + name + ".yml: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a configuration file is loaded
     * 
     * @param name The name of the file (without .yml extension)
     * @return true if the file is loaded
     */
    public boolean isConfigFileLoaded(String name) {
        return configFiles.containsKey(name);
    }
    
    /**
     * Create a messages configuration
     * 
     * @return The messages configuration
     */
    public Messages createMessages() {
        return new Messages(plugin);
    }
}