// ./src/main/java/com/minecraft/example/service/DefaultStatsService.java
package com.minecraft.example.service;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.ExamplePlugin;
import com.minecraft.example.database.StatsDAO;
import com.minecraft.example.model.PlayerStats;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DefaultStatsService implements StatsService {
    
    private final ExamplePlugin plugin;
    private final StatsDAO statsDAO;
    private final Map<UUID, Long> sessionStartTimes;
    
    public DefaultStatsService(ExamplePlugin plugin, StatsDAO statsDAO) {
        this.plugin = plugin;
        this.statsDAO = statsDAO;
        this.sessionStartTimes = new HashMap<>();
    }
    
    @Override
    public Optional<PlayerStats> getPlayerStats(UUID uuid) {
        return statsDAO.findByUuid(uuid);
    }
    
    @Override
    public CompletableFuture<Optional<PlayerStats>> getPlayerStatsAsync(UUID uuid) {
        return statsDAO.findByUuidAsync(uuid);
    }
    
    @Override
    public Optional<PlayerStats> getPlayerStatsByName(String name) {
        return statsDAO.findByName(name);
    }
    
    @Override
    public List<PlayerStats> getTopPlayers(String statField, int limit) {
        // Validate the stat field first to prevent SQL injection
        List<String> validFields = Arrays.asList("playtime_seconds", "login_count", "blocks_broken", "blocks_placed");
        
        if (!validFields.contains(statField)) {
            LogUtil.warning("Invalid stat field requested: " + statField);
            return new ArrayList<>();
        }
        
        return statsDAO.findTopPlayersByStat(statField, limit);
    }
    
    @Override
    public void recordPlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        
        // Record session start time for later playtime calculation
        sessionStartTimes.put(uuid, System.currentTimeMillis());
        
        // Create or update player record
        Optional<PlayerStats> existingStats = statsDAO.findByUuid(uuid);
        
        if (existingStats.isPresent()) {
            // Player exists, update login count and last join time
            PlayerStats stats = existingStats.get();
            stats.setName(name); // Update name in case it changed
            stats.setLastJoin(new Date());
            stats.setLoginCount(stats.getLoginCount() + 1);
            
            statsDAO.update(stats);
            LogUtil.debug("Updated player stats for " + name + " (UUID: " + uuid + ")");
        } else {
            // New player, create record
            PlayerStats newStats = new PlayerStats(uuid, name);
            statsDAO.insert(newStats);
            LogUtil.debug("Created new player stats for " + name + " (UUID: " + uuid + ")");
        }
    }
    
    @Override
    public void updatePlaytime(Player player, long sessionSeconds) {
        UUID uuid = player.getUniqueId();
        
        // Remove session start time
        sessionStartTimes.remove(uuid);
        
        // Update playtime in database
        statsDAO.updatePlaytime(uuid, sessionSeconds);
        LogUtil.debug("Updated playtime for " + player.getName() + ": +" + sessionSeconds + " seconds");
    }
    
    @Override
    public void incrementBlocksBroken(Player player) {
        UUID uuid = player.getUniqueId();
        statsDAO.incrementBlocksBroken(uuid);
    }
    
    @Override
    public void incrementBlocksPlaced(Player player) {
        UUID uuid = player.getUniqueId();
        statsDAO.incrementBlocksPlaced(uuid);
    }
    
    @Override
    public boolean resetStat(UUID uuid, String statField) {
        // Validate the stat field first to prevent SQL injection
        List<String> validFields = Arrays.asList("playtime_seconds", "login_count", "blocks_broken", "blocks_placed");
        
        if (!validFields.contains(statField)) {
            LogUtil.warning("Invalid stat field for reset: " + statField);
            return false;
        }
        
        return statsDAO.resetStat(uuid, statField);
    }
    
    /**
     * Calculate session length for a player who is leaving
     * 
     * @param player The player who is leaving
     * @return Session length in seconds
     */
    public long calculateSessionLength(Player player) {
        UUID uuid = player.getUniqueId();
        Long startTime = sessionStartTimes.get(uuid);
        
        if (startTime == null) {
            LogUtil.warning("No session start time found for " + player.getName());
            return 0;
        }
        
        long sessionMillis = System.currentTimeMillis() - startTime;
        return sessionMillis / 1000; // Convert to seconds
    }
}