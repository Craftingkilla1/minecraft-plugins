// ./Example-Plugin/src/main/java/com/minecraft/example/listener/PlayerListener.java
package com.minecraft.example.listener;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.core.utils.TimeUtil;
import com.minecraft.example.PlayerStatsPlugin;
import com.minecraft.example.api.StatsService;
import com.minecraft.example.config.MessageConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for player join and quit events.
 */
public class PlayerListener implements Listener {
    
    private final PlayerStatsPlugin plugin;
    private final StatsService statsService;
    private final MessageConfig messageConfig;
    
    // Map to track player join timestamps for calculating session durations
    private final Map<UUID, Long> joinTimes = new HashMap<>();
    
    /**
     * Creates a new PlayerListener instance.
     *
     * @param plugin The plugin instance
     * @param statsService The stats service
     */
    public PlayerListener(PlayerStatsPlugin plugin, StatsService statsService) {
        this.plugin = plugin;
        this.statsService = statsService;
        this.messageConfig = plugin.getMessageConfig();
    }
    
    /**
     * Handles player join events.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Store join time for session tracking
        joinTimes.put(uuid, System.currentTimeMillis());
        
        // Schedule async init to avoid blocking the main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // Initialize player in the database
            statsService.initializePlayer(player);
            
            // Run on main thread for displaying messages
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Show welcome message if enabled
                if (plugin.getPluginConfig().get("features.welcome-message", true)) {
                    messageConfig.sendMessage(player, "welcome");
                }
                
                // Show stats notification if enabled
                if (plugin.getPluginConfig().get("features.join-stats", false)) {
                    // Get session count
                    int sessions = statsService.getStat(player, "sessions.count");
                    
                    if (sessions > 0) {
                        messageConfig.sendMessage(player, "stats.join-stats",
                                "sessions", String.valueOf(sessions));
                    }
                }
            });
            
            // Increment sessions count
            statsService.incrementStat(player, "sessions.count");
        });
    }
    
    /**
     * Handles player quit events.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Calculate session duration
        long joinTime = joinTimes.getOrDefault(uuid, System.currentTimeMillis());
        long quitTime = System.currentTimeMillis();
        long sessionDuration = (quitTime - joinTime) / 1000; // Convert to seconds
        
        // Remove from join times map
        joinTimes.remove(uuid);
        
        // Schedule async update to avoid blocking the main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // Update player last seen time
            statsService.updatePlayerActivity(player);
            
            // Update playtime stats
            statsService.incrementStat(player, "sessions.playtime", (int) sessionDuration);
            
            // Get the service implementation instance to call its onPlayerQuit method
            if (statsService instanceof com.minecraft.example.service.DefaultStatsService) {
                ((com.minecraft.example.service.DefaultStatsService) statsService).onPlayerQuit(player);
            }
            
            LogUtil.debug("Player " + player.getName() + " logged out. Session duration: " + 
                    TimeUtil.formatTime(sessionDuration));
        });
    }
}