package com.example.exampleplugin;

import com.example.exampleplugin.database.DatabaseManager;
import com.minecraft.core.utils.LogUtil;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of the ExampleService interface
 */
public class DefaultExampleService implements ExampleService {
    private final Plugin plugin;
    private final Map<String, Boolean> features = new HashMap<>();
    private int processCount = 0;
    private final long startTime = System.currentTimeMillis();
    private DatabaseManager databaseManager;
    
    /**
     * Create a new DefaultExampleService
     * 
     * @param plugin The plugin instance
     */
    public DefaultExampleService(Plugin plugin) {
        this.plugin = plugin;
        
        // Initialize default features
        features.put("example_feature_1", true);
        features.put("example_feature_2", false);
        features.put("example_feature_3", true);
        features.put("database_integration", true);
        
        // Load features from config if they exist
        loadFeaturesFromConfig();
        
        // Get the database manager if it's an ExamplePlugin
        if (plugin instanceof ExamplePlugin) {
            databaseManager = ((ExamplePlugin) plugin).getDatabaseManager();
        }
        
        LogUtil.info("DefaultExampleService initialized");
    }
    
    /**
     * Load features from the plugin's configuration
     */
    private void loadFeaturesFromConfig() {
        FileConfiguration config = plugin.getConfig();
        
        if (config.isConfigurationSection("features")) {
            for (String key : config.getConfigurationSection("features").getKeys(false)) {
                boolean enabled = config.getBoolean("features." + key);
                features.put(key, enabled);
                LogUtil.debug("Loaded feature: " + key + " = " + enabled);
            }
        }
    }
    
    /**
     * Save features to the plugin's configuration
     */
    private void saveFeaturestoConfig() {
        FileConfiguration config = plugin.getConfig();
        
        // Save each feature
        for (Map.Entry<String, Boolean> entry : features.entrySet()) {
            config.set("features." + entry.getKey(), entry.getValue());
        }
        
        // Save the config
        plugin.saveConfig();
    }
    
    @Override
    public String processData(String data) {
        if (data == null || data.isEmpty()) {
            return "Error: Empty data";
        }
        
        // Increment process count
        processCount++;
        
        // Do some simple processing (in a real service, this would be more complex)
        String processed = data.toUpperCase() + " [Processed " + processCount + " times]";
        
        // If database integration is enabled and the database manager is available,
        // save this process as a stat for the "system" player
        if (isFeatureEnabled("database_integration") && databaseManager != null && databaseManager.isInitialized()) {
            try {
                String statName = "process_count";
                databaseManager.setPlayerStat("system", statName, processCount);
                LogUtil.debug("Saved process count to database: " + processCount);
            } catch (Exception e) {
                LogUtil.warning("Error saving process count to database: " + e.getMessage());
            }
        }
        
        LogUtil.debug("Processed data: " + processed);
        
        return processed;
    }
    
    @Override
    public List<String> getStats() {
        List<String> stats = new ArrayList<>();
        
        // Add some basic stats
        stats.add("Process Count: " + processCount);
        stats.add("Service Uptime: " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");
        stats.add("Features Enabled: " + countEnabledFeatures() + "/" + features.size());
        
        // Add database stats if available
        if (isFeatureEnabled("database_integration") && databaseManager != null && databaseManager.isInitialized()) {
            try {
                // Get database metrics
                List<String> dbMetrics = databaseManager.getDatabaseMetrics();
                
                stats.add("Database Status: CONNECTED");
                
                // Add first 3 metrics as an example
                int count = 0;
                for (String metric : dbMetrics) {
                    if (count < 3) {
                        stats.add("DB: " + metric);
                        count++;
                    } else {
                        break;
                    }
                }
                
                // Add count of players in database
                List<String> topPlayers = new ArrayList<>();
                databaseManager.getTopPlayers(5).forEach(player -> topPlayers.add(player.getName()));
                stats.add("Players in DB: " + topPlayers.size());
                
            } catch (Exception e) {
                stats.add("Database Status: ERROR - " + e.getMessage());
            }
        } else {
            stats.add("Database Status: DISABLED");
        }
        
        return stats;
    }
    
    /**
     * Count the number of enabled features
     * 
     * @return The number of enabled features
     */
    private int countEnabledFeatures() {
        int count = 0;
        
        for (Boolean enabled : features.values()) {
            if (enabled) {
                count++;
            }
        }
        
        return count;
    }
    
    @Override
    public boolean isFeatureEnabled(String featureName) {
        if (!features.containsKey(featureName)) {
            return false;
        }
        
        return features.get(featureName);
    }
    
    @Override
    public boolean setFeatureEnabled(String featureName, boolean enabled) {
        if (featureName == null || featureName.isEmpty()) {
            return false;
        }
        
        // Update the feature
        features.put(featureName, enabled);
        
        // Save to config
        saveFeaturestoConfig();
        
        LogUtil.debug("Feature " + featureName + " set to " + enabled);
        
        return true;
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
}