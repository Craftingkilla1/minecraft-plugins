// ./Example-Plugin/src/main/java/com/minecraft/example/integration/features/StatTrackerFeature.java
package com.minecraft.example.integration.features;

import com.minecraft.core.api.CoreAPI;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.ExamplePlugin;
import com.minecraft.example.core.services.AchievementService;
import com.minecraft.example.core.services.StatsService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks player statistics and updates the database.
 * Demonstrates integration of Core-Utils and SQL-Bridge.
 */
public class StatTrackerFeature implements Listener {
    
    private final ExamplePlugin plugin;
    private final Map<UUID, Location> lastPositions;
    private final boolean trackBlocksBroken;
    private final boolean trackBlocksPlaced;
    private final boolean trackPlayerKills;
    private final boolean trackMobKills;
    private final boolean trackPlaytime;
    private final boolean trackDistance;
    
    /**
     * Constructs a new StatTrackerFeature.
     *
     * @param plugin The plugin instance
     */
    public StatTrackerFeature(ExamplePlugin plugin) {
        this.plugin = plugin;
        this.lastPositions = new HashMap<>();
        
        // Load configuration
        this.trackBlocksBroken = plugin.getConfig().getBoolean("statistics.track_blocks_broken", true);
        this.trackBlocksPlaced = plugin.getConfig().getBoolean("statistics.track_blocks_placed", true);
        this.trackPlayerKills = plugin.getConfig().getBoolean("statistics.track_player_kills", true);
        this.trackMobKills = plugin.getConfig().getBoolean("statistics.track_mob_kills", true);
        this.trackPlaytime = plugin.getConfig().getBoolean("statistics.track_playtime", true);
        this.trackDistance = plugin.getConfig().getBoolean("statistics.track_distance", true);
    }
    
    /**
     * Enables the feature.
     */
    public void enable() {
        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Start distance tracking task if enabled
        if (trackDistance) {
            startDistanceTrackingTask();
        }
        
        LogUtil.info("Stat tracker feature enabled");
    }
    
    /**
     * Disables the feature.
     */
    public void disable() {
        // Save all stats
        saveAllStats();
        
        LogUtil.info("Stat tracker feature disabled");
    }
    
    /**
     * Saves stats for all online players.
     */
    private void saveAllStats() {
        // Get stats service
        StatsService statsService = CoreAPI.Services.get(StatsService.class);
        if (statsService == null) {
            return;
        }
        
        // Save stats for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            statsService.saveStats(player);
        }
    }
    
    /**
     * Starts the distance tracking task.
     */
    private void startDistanceTrackingTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::trackPlayerDistances, 20L, 20L);
    }
    
    /**
     * Tracks player distances.
     */
    private void trackPlayerDistances() {
        // Skip if distance tracking is disabled
        if (!trackDistance) {
            return;
        }
        
        // Get stats service
        StatsService statsService = CoreAPI.Services.get(StatsService.class);
        if (statsService == null) {
            return;
        }
        
        // Track distance for each player
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Location currentLocation = player.getLocation();
            
            // Skip if player has no last position
            if (!lastPositions.containsKey(uuid)) {
                lastPositions.put(uuid, currentLocation);
                continue;
            }
            
            // Skip if player is in a different world
            Location lastLocation = lastPositions.get(uuid);
            if (!currentLocation.getWorld().equals(lastLocation.getWorld())) {
                lastPositions.put(uuid, currentLocation);
                continue;
            }
            
            // Calculate distance
            double distance = lastLocation.distance(currentLocation);
            
            // Ignore small movements and teleports
            if (distance > 0.5 && distance < 100) {
                // Increment distance stat
                statsService.incrementDistance(player, distance);
                
                // Check for achievement if over 100 blocks traveled
                if (distance > 100) {
                    checkDistanceAchievements(player);
                }
            }
            
            // Update last position
            lastPositions.put(uuid, currentLocation);
        }
    }
    
    /**
     * Checks for distance-related achievements.
     *
     * @param player The player
     */
    private void checkDistanceAchievements(Player player) {
        // Get achievement service
        AchievementService achievementService = CoreAPI.Services.get(AchievementService.class);
        if (achievementService == null) {
            return;
        }
        
        // Check for achievements
        achievementService.checkAndAwardAchievements(player);
    }
    
    /**
     * Handles when a player joins the server.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Get stats service
        StatsService statsService = CoreAPI.Services.get(StatsService.class);
        if (statsService != null) {
            statsService.handlePlayerJoin(player);
        }
        
        // Get achievement service
        AchievementService achievementService = CoreAPI.Services.get(AchievementService.class);
        if (achievementService != null) {
            achievementService.handlePlayerJoin(player);
        }
        
        // Track player position
        lastPositions.put(player.getUniqueId(), player.getLocation());
    }
    
    /**
     * Handles when a player quits the server.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Get stats service
        StatsService statsService = CoreAPI.Services.get(StatsService.class);
        if (statsService != null) {
            statsService.handlePlayerQuit(player);
        }
        
        // Remove from tracking
        lastPositions.remove(uuid);
    }
    
    /**
     * Handles when a player breaks a block.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // Skip if blocks broken tracking is disabled
        if (!trackBlocksBroken) {
            return;
        }
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Skip non-natural blocks
        if (isNonNaturalBlock(block.getType())) {
            return;
        }
        
        // Get stats service
        StatsService statsService = CoreAPI.Services.get(StatsService.class);
        if (statsService == null) {
            return;
        }
        
        // Increment blocks broken stat
        statsService.incrementStat(player, "blocks_broken", 1);
        
        // Check for achievement every 10 blocks
        if (statsService.getStat(player, "blocks_broken") % 10 == 0) {
            checkBlockAchievements(player);
        }
    }
    
    /**
     * Handles when a player places a block.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Skip if blocks placed tracking is disabled
        if (!trackBlocksPlaced) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Get stats service
        StatsService statsService = CoreAPI.Services.get(StatsService.class);
        if (statsService == null) {
            return;
        }
        
        // Increment blocks placed stat
        statsService.incrementStat(player, "blocks_placed", 1);
        
        // Check for achievement every 10 blocks
        if (statsService.getStat(player, "blocks_placed") % 10 == 0) {
            checkBlockAchievements(player);
        }
    }
    
    /**
     * Handles when a player dies.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();
        
        // Get stats service
        StatsService statsService = CoreAPI.Services.get(StatsService.class);
        if (statsService == null) {
            return;
        }
        
        // Increment deaths stat for the victim
        statsService.incrementStat(player, "deaths", 1);
        
        // Increment player kills stat for the killer if applicable
        if (killer != null && trackPlayerKills) {
            statsService.incrementStat(killer, "player_kills", 1);
            
            // Check for achievement
            if (statsService.getStat(killer, "player_kills") % 5 == 0) {
                checkKillAchievements(killer);
            }
        }
    }
    
    /**
     * Handles when an entity dies.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        // Skip if mob kills tracking is disabled
        if (!trackMobKills) {
            return;
        }
        
        // Skip if not killed by a player
        if (!(event.getEntity().getKiller() instanceof Player)) {
            return;
        }
        
        // Skip if the entity is a player
        if (event.getEntityType() == EntityType.PLAYER) {
            return;
        }
        
        Player killer = event.getEntity().getKiller();
        
        // Get stats service
        StatsService statsService = CoreAPI.Services.get(StatsService.class);
        if (statsService == null) {
            return;
        }
        
        // Increment mob kills stat
        statsService.incrementStat(killer, "mob_kills", 1);
        
        // Check for achievement every 20 kills
        if (statsService.getStat(killer, "mob_kills") % 20 == 0) {
            checkKillAchievements(killer);
        }
    }
    
    /**
     * Handles when a player moves.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // We track distance in the task instead
        // This event is only used to update the last position
        
        // Skip if distance tracking is disabled
        if (!trackDistance) {
            return;
        }
        
        // Update last position
        lastPositions.put(event.getPlayer().getUniqueId(), event.getTo());
    }
    
    /**
     * Checks for block-related achievements.
     *
     * @param player The player
     */
    private void checkBlockAchievements(Player player) {
        // Get achievement service
        AchievementService achievementService = CoreAPI.Services.get(AchievementService.class);
        if (achievementService == null) {
            return;
        }
        
        // Check for achievements
        achievementService.checkAndAwardAchievements(player);
    }
    
    /**
     * Checks for kill-related achievements.
     *
     * @param player The player
     */
    private void checkKillAchievements(Player player) {
        // Get achievement service
        AchievementService achievementService = CoreAPI.Services.get(AchievementService.class);
        if (achievementService == null) {
            return;
        }
        
        // Check for achievements
        achievementService.checkAndAwardAchievements(player);
    }
    
    /**
     * Checks if a block type is non-natural.
     *
     * @param type The block type
     * @return True if the block is non-natural
     */
    private boolean isNonNaturalBlock(Material type) {
        return type == Material.AIR ||
                type.name().contains("SLAB") ||
                type.name().contains("STAIRS") ||
                type.name().contains("FENCE") ||
                type.name().contains("SIGN") ||
                type.name().contains("BUTTON") ||
                type.name().contains("PRESSURE_PLATE") ||
                type.name().contains("DOOR") ||
                type.name().contains("TRAPDOOR") ||
                type.name().contains("CARPET");
    }
}