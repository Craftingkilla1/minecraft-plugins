// ./Example-Plugin/src/main/java/com/minecraft/example/config/PluginConfig.java
package com.minecraft.example.config;

import com.minecraft.core.config.ConfigManager;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.PlayerStatsPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the plugin's configuration settings.
 */
public class PluginConfig {
    
    private final PlayerStatsPlugin plugin;
    private final ConfigManager configManager;
    private FileConfiguration config;
    
    // Default values
    private boolean debug = false;
    private boolean autoTrackStats = true;
    private boolean displayNotifications = true;
    private int leaderboardUpdateInterval = 30; // minutes
    private int cacheExpiryTime = 10; // minutes
    private int statBatchSize = 10;
    private Map<String, String> statDisplayNames = new HashMap<>();
    
    /**
     * Creates a new PluginConfig instance.
     *
     * @param plugin The plugin instance
     * @param configManager The config manager
     */
    public PluginConfig(PlayerStatsPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        loadConfig();
    }
    
    /**
     * Loads or reloads the configuration.
     */
    public void loadConfig() {
        // Ensure config.yml exists (creates default if it doesn't)
        plugin.saveDefaultConfig();
        
        // Load the configuration
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        
        // Load settings
        this.debug = config.getBoolean("debug", false);
        this.autoTrackStats = config.getBoolean("features.auto-track-stats", true);
        this.displayNotifications = config.getBoolean("features.display-notifications", true);
        this.leaderboardUpdateInterval = config.getInt("leaderboards.update-interval", 30);
        this.cacheExpiryTime = config.getInt("performance.cache-expiry-time", 10);
        this.statBatchSize = config.getInt("performance.stat-batch-size", 10);
        
        // Load stat display names
        if (config.isConfigurationSection("stats.display-names")) {
            ConfigurationSection displayNamesSection = config.getConfigurationSection("stats.display-names");
            for (String key : displayNamesSection.getKeys(false)) {
                statDisplayNames.put(key, displayNamesSection.getString(key));
            }
        }
        
        LogUtil.debug("Configuration loaded: debug=" + debug + ", autoTrackStats=" + autoTrackStats);
    }
    
    /**
     * Gets whether debug mode is enabled.
     *
     * @return True if debug mode is enabled, false otherwise
     */
    public boolean isDebugEnabled() {
        return debug;
    }
    
    /**
     * Gets whether auto-tracking of stats is enabled.
     *
     * @return True if auto-tracking is enabled, false otherwise
     */
    public boolean isAutoTrackStatsEnabled() {
        return autoTrackStats;
    }
    
    /**
     * Gets whether notifications should be displayed.
     *
     * @return True if notifications should be displayed, false otherwise
     */
    public boolean isDisplayNotificationsEnabled() {
        return displayNotifications;
    }
    
    /**
     * Gets the leaderboard update interval in minutes.
     *
     * @return The leaderboard update interval
     */
    public int getLeaderboardUpdateInterval() {
        return leaderboardUpdateInterval;
    }
    
    /**
     * Gets the cache expiry time in minutes.
     *
     * @return The cache expiry time
     */
    public int getCacheExpiryTime() {
        return cacheExpiryTime;
    }
    
    /**
     * Gets the batch size for stat updates.
     *
     * @return The stat batch size
     */
    public int getStatBatchSize() {
        return statBatchSize;
    }
    
    /**
     * Gets the display name for a statistic.
     *
     * @param statName The statistic name
     * @return The display name, or the statistic name if not found
     */
    public String getStatDisplayName(String statName) {
        return statDisplayNames.getOrDefault(statName, statName);
    }
    
    /**
     * Gets all statistic display names.
     *
     * @return A map of statistic names to display names
     */
    public Map<String, String> getStatDisplayNames() {
        return new HashMap<>(statDisplayNames);
    }
    
    /**
     * Gets a value from the configuration.
     *
     * @param path The configuration path
     * @param defaultValue The default value if not found
     * @param <T> The type of the value
     * @return The configuration value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String path, T defaultValue) {
        if (config.contains(path)) {
            return (T) config.get(path);
        }
        return defaultValue;
    }
}