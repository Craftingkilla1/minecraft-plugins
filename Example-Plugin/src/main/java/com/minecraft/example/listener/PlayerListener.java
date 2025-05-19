// ./src/main/java/com/minecraft/example/listener/PlayerListener.java
package com.minecraft.example.listener;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.ExamplePlugin;
import com.minecraft.example.model.PlayerStats;
import com.minecraft.example.service.StatsService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Event listener for tracking player statistics
 */
public class PlayerListener implements Listener {
    
    private final ExamplePlugin plugin;
    private final StatsService statsService;
    
    // Track join times for calculating time played
    private final Map<UUID, Long> joinTimes = new HashMap<>();
    
    /**
     * Constructor
     * @param plugin Plugin instance
     */
    public PlayerListener(ExamplePlugin plugin) {
        this.plugin = plugin;
        this.statsService = plugin.getStatsService();
    }
    
    /**
     * Handle player join event
     * @param event PlayerJoinEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        
        // Record join time for tracking time played
        joinTimes.put(playerUuid, System.currentTimeMillis());
        
        // Get player stats
        Optional<PlayerStats> statsOptional = statsService.getPlayerStats(playerUuid);
        
        if (!statsOptional.isPresent()) {
            // Create new player stats
            PlayerStats stats = new PlayerStats(playerUuid, player.getName());
            statsService.savePlayerStats(stats);
            LogUtil.debug("Created new player stats for " + player.getName());
        } else {
            // Update player name if changed
            PlayerStats stats = statsOptional.get();
            if (!stats.getPlayerName().equals(player.getName())) {
                stats.setPlayerName(player.getName());
                statsService.savePlayerStats(stats);
                LogUtil.debug("Updated player name for " + player.getName());
            }
        }
    }
    
    /**
     * Handle player quit event
     * @param event PlayerQuitEvent
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        
        // Calculate time played
        if (joinTimes.containsKey(playerUuid)) {
            long joinTime = joinTimes.get(playerUuid);
            long quitTime = System.currentTimeMillis();
            int sessionSeconds = (int) ((quitTime - joinTime) / 1000);
            
            // Update time played
            statsService.updateTimePlayed(playerUuid, sessionSeconds);
            
            // Remove from join times map
            joinTimes.remove(playerUuid);
            
            LogUtil.debug("Updated time played for " + player.getName() + ": +" + sessionSeconds + " seconds");
        }
    }
    
    /**
     * Handle player death event
     * @param event PlayerDeathEvent
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Increment death count
        statsService.incrementStat(player.getUniqueId(), "deaths");
        
        // Increment kill count for killer (if killed by a player)
        Player killer = player.getKiller();
        if (killer != null) {
            statsService.incrementStat(killer.getUniqueId(), "kills");
        }
    }
    
    /**
     * Handle block place event
     * @param event BlockPlaceEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        // Increment blocks placed count
        statsService.incrementStat(player.getUniqueId(), "blocks_placed");
    }
    
    /**
     * Handle block break event
     * @param event BlockBreakEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        // Increment blocks broken count
        statsService.incrementStat(player.getUniqueId(), "blocks_broken");
    }
}