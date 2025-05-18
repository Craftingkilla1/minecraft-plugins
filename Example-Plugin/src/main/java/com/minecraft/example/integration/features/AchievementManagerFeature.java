// ./Example-Plugin/src/main/java/com/minecraft/example/integration/features/AchievementManagerFeature.java
package com.minecraft.example.integration.features;

import com.minecraft.core.api.CoreAPI;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.ExamplePlugin;
import com.minecraft.example.config.ConfigManager;
import com.minecraft.example.core.services.AchievementService;
import com.minecraft.example.core.services.NotificationService;
import com.minecraft.example.core.services.StatsService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

/**
 * Manages player achievements and rewards.
 * Demonstrates integration of Core-Utils and SQL-Bridge.
 */
public class AchievementManagerFeature implements Listener {
    
    private final ExamplePlugin plugin;
    private final ConfigManager configManager;
    private final Map<String, AchievementDefinition> customAchievements;
    
    /**
     * Constructs a new AchievementManagerFeature.
     *
     * @param plugin The plugin instance
     */
    public AchievementManagerFeature(ExamplePlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.customAchievements = new HashMap<>();
    }
    
    /**
     * Enables the feature.
     */
    public void enable() {
        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Load custom achievements
        loadCustomAchievements();
        
        // Initialize achievement tracking
        initializeAchievements();
        
        // Start achievement check task
        startAchievementCheckTask();
        
        LogUtil.info("Achievement manager feature enabled");
    }
    
    /**
     * Disables the feature.
     */
    public void disable() {
        LogUtil.info("Achievement manager feature disabled");
    }
    
    /**
     * Loads custom achievements from file.
     */
    private void loadCustomAchievements() {
        // Create achievements directory if it doesn't exist
        File achievementsDir = new File(plugin.getDataFolder(), "achievements");
        if (!achievementsDir.exists()) {
            achievementsDir.mkdirs();
        }
        
        // Create default achievements file if it doesn't exist
        File defaultAchievementsFile = new File(achievementsDir, "default.yml");
        if (!defaultAchievementsFile.exists()) {
            plugin.saveResource("achievements/default.yml", false);
        }
        
        // Load all achievement files
        File[] achievementFiles = achievementsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (achievementFiles == null) {
            return;
        }
        
        for (File file : achievementFiles) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                
                for (String key : config.getKeys(false)) {
                    // Skip non-achievement sections
                    if (!config.isConfigurationSection(key)) {
                        continue;
                    }
                    
                    // Load achievement definition
                    String identifier = key;
                    String name = config.getString(key + ".name", key);
                    String description = config.getString(key + ".description", "");
                    String iconMaterial = config.getString(key + ".icon", "DIAMOND");
                    
                    // Create achievement condition
                    Map<String, Integer> conditions = new HashMap<>();
                    if (config.isConfigurationSection(key + ".conditions")) {
                        for (String conditionKey : config.getConfigurationSection(key + ".conditions").getKeys(false)) {
                            int value = config.getInt(key + ".conditions." + conditionKey);
                            conditions.put(conditionKey, value);
                        }
                    }
                    
                    // Create achievement rewards
                    List<ItemStack> rewards = new ArrayList<>();
                    if (config.isList(key + ".rewards.items")) {
                        List<Map<?, ?>> itemsList = config.getMapList(key + ".rewards.items");
                        for (Map<?, ?> itemMap : itemsList) {
                            try {
                                String itemMaterial = (String) itemMap.get("material");
                                int amount = (int) itemMap.getOrDefault("amount", 1);
                                
                                Material material = Material.valueOf(itemMaterial.toUpperCase());
                                ItemStack item = new ItemStack(material, amount);
                                rewards.add(item);
                            } catch (Exception e) {
                                LogUtil.warning("Invalid item reward in achievement " + key + ": " + e.getMessage());
                            }
                        }
                    }
                    
                    // Add experience reward
                    int experienceReward = config.getInt(key + ".rewards.experience", 0);
                    
                    // Create achievement definition
                    AchievementDefinition achievement = new AchievementDefinition(
                            identifier, name, description, iconMaterial, conditions, rewards, experienceReward);
                    
                    // Register custom achievement
                    customAchievements.put(identifier, achievement);
                    
                    LogUtil.debug("Loaded custom achievement: " + identifier);
                }
            } catch (Exception e) {
                LogUtil.severe("Error loading custom achievements from " + file.getName() + ": " + e.getMessage());
            }
        }
        
        LogUtil.info("Loaded " + customAchievements.size() + " custom achievements");
    }
    
    /**
     * Initializes achievements in the database.
     */
    private void initializeAchievements() {
        // Skip if database is disabled
        if (!plugin.isDatabaseEnabled()) {
            return;
        }
        
        // Get achievement service
        AchievementService achievementService = CoreAPI.Services.get(AchievementService.class);
        if (achievementService == null) {
            return;
        }
        
        // Create custom achievements in database
        for (AchievementDefinition achievement : customAchievements.values()) {
            achievementService.createAchievement(
                    achievement.getIdentifier(),
                    achievement.getName(),
                    achievement.getDescription(),
                    achievement.getIconMaterial()
            );
        }
    }
    
    /**
     * Starts the achievement check task.
     */
    private void startAchievementCheckTask() {
        // Run task every 5 minutes (6000 ticks)
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkAllPlayerAchievements, 6000L, 6000L);
    }
    
    /**
     * Checks all online players for achievements.
     */
    private void checkAllPlayerAchievements() {
        // Get achievement service
        AchievementService achievementService = CoreAPI.Services.get(AchievementService.class);
        if (achievementService == null) {
            return;
        }
        
        // Check each online player
        for (Player player : Bukkit.getOnlinePlayers()) {
            achievementService.checkAndAwardAchievements(player);
            checkCustomAchievements(player);
        }
    }
    
    /**
     * Checks a player for custom achievements.
     *
     * @param player The player
     */
    private void checkCustomAchievements(Player player) {
        // Get services
        AchievementService achievementService = CoreAPI.Services.get(AchievementService.class);
        StatsService statsService = CoreAPI.Services.get(StatsService.class);
        
        if (achievementService == null || statsService == null) {
            return;
        }
        
        // Get player stats
        Map<String, Integer> stats = statsService.getAllStats(player);
        
        // Check each custom achievement
        for (AchievementDefinition achievement : customAchievements.values()) {
            // Skip if player already has the achievement
            if (achievementService.hasAchievement(player, achievement.getIdentifier())) {
                continue;
            }
            
            // Check conditions
            boolean allConditionsMet = true;
            for (Map.Entry<String, Integer> condition : achievement.getConditions().entrySet()) {
                String statName = condition.getKey();
                int requiredValue = condition.getValue();
                int playerValue = stats.getOrDefault(statName, 0);
                
                if (playerValue < requiredValue) {
                    allConditionsMet = false;
                    break;
                }
            }
            
            // Award achievement if all conditions are met
            if (allConditionsMet) {
                boolean awarded = achievementService.awardAchievement(player, achievement.getIdentifier());
                
                if (awarded) {
                    // Give rewards
                    giveAchievementRewards(player, achievement);
                }
            }
        }
    }
    
    /**
     * Gives achievement rewards to a player.
     *
     * @param player The player
     * @param achievement The achievement
     */
    private void giveAchievementRewards(Player player, AchievementDefinition achievement) {
        // Skip if rewards are disabled
        if (!plugin.getConfig().getBoolean("achievements.enable_rewards", true)) {
            return;
        }
        
        // Get notification service
        NotificationService notificationService = CoreAPI.Services.get(NotificationService.class);
        
        // Give item rewards
        for (ItemStack item : achievement.getRewards()) {
            player.getInventory().addItem(item);
        }
        
        // Give experience reward
        if (achievement.getExperienceReward() > 0) {
            player.giveExp(achievement.getExperienceReward());
        }
        
        // Send reward message
        if (notificationService != null && !achievement.getRewards().isEmpty()) {
            notificationService.sendInfoMessage(player, "You received rewards for the achievement!");
        }
    }
    
    /**
     * Handles when a player joins the server.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check for achievements
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkCustomAchievements(player), 40L);
    }
    
    /**
     * Handles when a player quits the server.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Nothing to do here for now
    }
    
    /**
     * A custom achievement definition.
     */
    private static class AchievementDefinition {
        private final String identifier;
        private final String name;
        private final String description;
        private final String iconMaterial;
        private final Map<String, Integer> conditions;
        private final List<ItemStack> rewards;
        private final int experienceReward;
        
        /**
         * Constructs a new AchievementDefinition.
         *
         * @param identifier The identifier
         * @param name The name
         * @param description The description
         * @param iconMaterial The icon material
         * @param conditions The conditions
         * @param rewards The rewards
         * @param experienceReward The experience reward
         */
        public AchievementDefinition(String identifier, String name, String description, String iconMaterial,
                                   Map<String, Integer> conditions, List<ItemStack> rewards, int experienceReward) {
            this.identifier = identifier;
            this.name = name;
            this.description = description;
            this.iconMaterial = iconMaterial;
            this.conditions = conditions;
            this.rewards = rewards;
            this.experienceReward = experienceReward;
        }
        
        /**
         * Gets the identifier.
         *
         * @return The identifier
         */
        public String getIdentifier() {
            return identifier;
        }
        
        /**
         * Gets the name.
         *
         * @return The name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Gets the description.
         *
         * @return The description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the icon material.
         *
         * @return The icon material
         */
        public String getIconMaterial() {
            return iconMaterial;
        }
        
        /**
         * Gets the conditions.
         *
         * @return The conditions
         */
        public Map<String, Integer> getConditions() {
            return conditions;
        }
        
        /**
         * Gets the rewards.
         *
         * @return The rewards
         */
        public List<ItemStack> getRewards() {
            return rewards;
        }
        
        /**
         * Gets the experience reward.
         *
         * @return The experience reward
         */
        public int getExperienceReward() {
            return experienceReward;
        }
    }
}