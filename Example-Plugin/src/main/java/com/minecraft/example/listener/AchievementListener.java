// ./Example-Plugin/src/main/java/com/minecraft/example/listener/AchievementListener.java
package com.minecraft.example.listener;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.PlayerStatsPlugin;
import com.minecraft.example.api.AchievementService;
import com.minecraft.example.api.StatsService;
import com.minecraft.example.config.MessageConfig;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener that monitors player activities and awards achievements when criteria are met.
 */
public class AchievementListener implements Listener {
    
    private final PlayerStatsPlugin plugin;
    private final AchievementService achievementService;
    private final StatsService statsService;
    private final MessageConfig messageConfig;
    
    // Map of players to their achievement check tasks
    private final Map<UUID, BukkitTask> achievementCheckTasks = new HashMap<>();
    
    /**
     * Creates a new AchievementListener instance.
     *
     * @param plugin The plugin instance
     * @param achievementService The achievement service
     * @param statsService The stats service
     */
    public AchievementListener(PlayerStatsPlugin plugin, AchievementService achievementService, StatsService statsService) {
        this.plugin = plugin;
        this.achievementService = achievementService;
        this.statsService = statsService;
        this.messageConfig = plugin.getMessageConfig();
        
        // Schedule achievement check for all online players
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                startAchievementChecker(player);
            }
        }, 100L); // 5 seconds delay after server start
    }
    
    /**
     * Handles player join events.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Start achievement checker for the player
        startAchievementChecker(player);
    }
    
    /**
     * Handles player quit events.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Cancel achievement checker for the player
        BukkitTask task = achievementCheckTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Starts an achievement checker for a player.
     *
     * @param player The player to check achievements for
     */
    private void startAchievementChecker(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Cancel existing task if any
        BukkitTask existingTask = achievementCheckTasks.get(uuid);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Create a new task that checks for achievements periodically
        int checkInterval = plugin.getPluginConfig().get("achievements.check-interval", 20) * 20; // Convert to ticks
        
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                () -> checkAchievements(player),
                checkInterval,
                checkInterval
        );
        
        achievementCheckTasks.put(uuid, task);
        LogUtil.debug("Started achievement checker for " + player.getName());
    }
    
    /**
     * Checks if a player has earned any achievements.
     *
     * @param player The player to check
     */
    private void checkAchievements(Player player) {
        if (!player.isOnline()) {
            return;
        }
        
        // Check achievements
        achievementService.checkAchievements(player);
    }
    
    /**
     * Method called by the AchievementService when a player earns an achievement.
     *
     * @param player The player who earned the achievement
     * @param achievementId The ID of the earned achievement
     * @param displayName The display name of the achievement
     * @param isSecret Whether the achievement is secret
     */
    public void onAchievementEarned(Player player, String achievementId, String displayName, boolean isSecret) {
        // Run on the main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Show achievement message to the player
            if (plugin.getPluginConfig().isDisplayNotificationsEnabled()) {
                if (isSecret) {
                    player.sendMessage(ChatColor.GOLD + "✨ " + ChatColor.GREEN + "Secret Achievement Unlocked: " + 
                            ChatColor.YELLOW + displayName);
                } else {
                    player.sendMessage(ChatColor.GOLD + "✨ " + ChatColor.GREEN + "Achievement Unlocked: " + 
                            ChatColor.YELLOW + displayName);
                }
                
                // Play sound
                player.playSound(player.getLocation(), 
                        org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            
            // Broadcast achievement if enabled and not secret
            if (plugin.getPluginConfig().get("achievements.broadcast", true) && !isSecret) {
                for (Player online : plugin.getServer().getOnlinePlayers()) {
                    if (online != player) {
                        online.sendMessage(ChatColor.GOLD + "[Achievement] " + 
                                ChatColor.WHITE + player.getName() + ChatColor.GREEN + " has earned " + 
                                ChatColor.YELLOW + displayName + ChatColor.GREEN + "!");
                    }
                }
            }
            
            // Grant rewards if configured
            grantAchievementRewards(player, achievementId);
        });
    }
    
    /**
     * Grants rewards for earning an achievement.
     *
     * @param player The player who earned the achievement
     * @param achievementId The ID of the earned achievement
     */
    private void grantAchievementRewards(Player player, String achievementId) {
        // Check if rewards are enabled
        if (!plugin.getPluginConfig().get("achievements.rewards.enabled", false)) {
            return;
        }
        
        // Get reward commands from config
        String rewardPath = "achievements.rewards.list." + achievementId;
        List<String> rewardCommands = plugin.getPluginConfig().get(rewardPath, new ArrayList<String>());
        
        if (rewardCommands.isEmpty()) {
            // Check for default rewards
            rewardCommands = plugin.getPluginConfig().get("achievements.rewards.default", new ArrayList<String>());
        }
        
        // Execute reward commands
        for (String command : rewardCommands) {
            String processedCommand = command.replace("{player}", player.getName());
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), processedCommand);
        }
    }
}