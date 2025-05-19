// ./Example-Plugin/src/main/java/com/minecraft/example/listener/PlayerListener.java
package com.minecraft.example.listener;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.ExamplePlugin;
import com.minecraft.example.service.DefaultStatsService;
import com.minecraft.example.service.StatsService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for player-related events
 */
public class PlayerListener implements Listener {
    
    private final ExamplePlugin plugin;
    private final StatsService statsService;
    
    public PlayerListener(ExamplePlugin plugin) {
        this.plugin = plugin;
        this.statsService = plugin.getStatsService();
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Record player join
        statsService.recordPlayerJoin(player);
        
        LogUtil.debug("Recorded join for player: " + player.getName());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Calculate session length and update playtime
        if (statsService instanceof DefaultStatsService) {
            DefaultStatsService defaultService = (DefaultStatsService) statsService;
            long sessionSeconds = defaultService.calculateSessionLength(player);
            
            if (sessionSeconds > 0) {
                // Update playtime
                statsService.updatePlaytime(player, sessionSeconds);
                LogUtil.debug("Updated playtime for " + player.getName() + ": +" + sessionSeconds + " seconds");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // Check if tracking is enabled in config
        if (!plugin.getConfig().getBoolean("track.blocks_broken", true)) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Run async to not impact server performance
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            statsService.incrementBlocksBroken(player);
        });
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Check if tracking is enabled in config
        if (!plugin.getConfig().getBoolean("track.blocks_placed", true)) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Run async to not impact server performance
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            statsService.incrementBlocksPlaced(player);
        });
    }
}