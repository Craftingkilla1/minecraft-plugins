// ./Example-Plugin/src/main/java/com/minecraft/example/listener/StatTrackerListener.java
package com.minecraft.example.listener;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.PlayerStatsPlugin;
import com.minecraft.example.api.StatsService;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener that tracks various player statistics.
 */
public class StatTrackerListener implements Listener {
    
    private final PlayerStatsPlugin plugin;
    private final StatsService statsService;
    
    // Cache for tracking movement stats
    private final Map<UUID, Double> lastXPosition = new HashMap<>();
    private final Map<UUID, Double> lastZPosition = new HashMap<>();
    private final Map<UUID, Double> distanceTraveled = new HashMap<>();
    
    // Stats will be batched and saved periodically
    private final Map<UUID, Map<String, Integer>> statBatch = new HashMap<>();
    
    /**
     * Creates a new StatTrackerListener instance.
     *
     * @param plugin The plugin instance
     * @param statsService The stats service
     */
    public StatTrackerListener(PlayerStatsPlugin plugin, StatsService statsService) {
        this.plugin = plugin;
        this.statsService = statsService;
        
        // Schedule batch processing
        scheduleBatchProcessing();
    }
    
    /**
     * Schedules periodic processing of batched statistics.
     */
    private void scheduleBatchProcessing() {
        int batchInterval = plugin.getPluginConfig().get("performance.stat-batch-interval", 20) * 20; // Convert to ticks
        
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::processBatchedStats, 
                batchInterval, batchInterval);
    }
    
    /**
     * Processes batched statistics and saves them to the database.
     */
    private void processBatchedStats() {
        if (statBatch.isEmpty()) {
            return;
        }
        
        LogUtil.debug("Processing batched stats for " + statBatch.size() + " players");
        
        // Process each player's stats
        for (Map.Entry<UUID, Map<String, Integer>> entry : statBatch.entrySet()) {
            UUID uuid = entry.getKey();
            Map<String, Integer> stats = entry.getValue();
            
            // Get player
            Player player = plugin.getServer().getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue; // Skip offline players
            }
            
            // Save stats
            statsService.updateStatsAsync(player, stats);
        }
        
        // Clear batch
        statBatch.clear();
    }
    
    /**
     * Tracks a statistic in batch.
     *
     * @param player The player
     * @param statName The statistic name
     * @param amount The amount to increment by
     */
    private void trackStat(Player player, String statName, int amount) {
        if (!plugin.getPluginConfig().isAutoTrackStatsEnabled()) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        
        // Get or create player's batch
        Map<String, Integer> playerBatch = statBatch.computeIfAbsent(uuid, k -> new HashMap<>());
        
        // Update stat in batch
        int currentValue = playerBatch.getOrDefault(statName, 0);
        playerBatch.put(statName, currentValue + amount);
        
        // If immediate stats is enabled, update directly
        if (plugin.getPluginConfig().get("performance.immediate-stats", false)) {
            statsService.incrementStat(player, statName, amount);
        }
    }
    
    /**
     * Tracks block breaking.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();
        
        // Track overall blocks broken
        trackStat(player, "blocks.broken", 1);
        
        // Track specific block type
        trackStat(player, "blocks.broken." + blockType.name().toLowerCase(), 1);
    }
    
    /**
     * Tracks block placing.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();
        
        // Track overall blocks placed
        trackStat(player, "blocks.placed", 1);
        
        // Track specific block type
        trackStat(player, "blocks.placed." + blockType.name().toLowerCase(), 1);
    }
    
    /**
     * Tracks player kills.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            EntityType entityType = event.getEntityType();
            
            // Track overall kills
            trackStat(killer, "combat.kills", 1);
            
            // Track specific entity type
            trackStat(killer, "combat.kills." + entityType.name().toLowerCase(), 1);
            
            // Track if it was a player kill
            if (entityType == EntityType.PLAYER) {
                trackStat(killer, "combat.player_kills", 1);
            }
        }
    }
    
    /**
     * Tracks player deaths.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Track death
        trackStat(player, "combat.deaths", 1);
        
        // Track cause of death
        if (player.getLastDamageCause() != null) {
            String cause = player.getLastDamageCause().getCause().name().toLowerCase();
            trackStat(player, "combat.deaths." + cause, 1);
        }
    }
    
    /**
     * Tracks damage dealt.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            double damage = event.getFinalDamage();
            
            // Track damage dealt
            trackStat(player, "combat.damage_dealt", (int) damage);
            
            // Track damage to specific entity type
            String entityType = event.getEntityType().name().toLowerCase();
            trackStat(player, "combat.damage_dealt." + entityType, (int) damage);
        }
        
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            double damage = event.getFinalDamage();
            
            // Track damage taken
            trackStat(player, "combat.damage_taken", (int) damage);
            
            // Track damage from specific entity type
            if (event.getDamager() != null) {
                String damagerType = event.getDamager().getType().name().toLowerCase();
                trackStat(player, "combat.damage_taken." + damagerType, (int) damage);
            }
        }
    }
    
    /**
     * Tracks item crafting.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getRecipe().getResult();
        Material itemType = result.getType();
        
        // Get crafting amount
        int amount = result.getAmount();
        if (event.isShiftClick()) {
            // For shift-clicking, we need to calculate the maximum possible craft amount
            amount = calculateShiftClickAmount(event, result.getAmount());
        }
        
        // Track overall items crafted
        trackStat(player, "items.crafted", amount);
        
        // Track specific item type
        trackStat(player, "items.crafted." + itemType.name().toLowerCase(), amount);
    }
    
    /**
     * Calculates the amount of items crafted when shift-clicking.
     *
     * @param event The craft event
     * @param resultAmount The result amount per craft
     * @return The total amount crafted
     */
    private int calculateShiftClickAmount(CraftItemEvent event, int resultAmount) {
        // Find limiting ingredient
        int minAmount = Integer.MAX_VALUE;
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && !item.getType().isAir()) {
                minAmount = Math.min(minAmount, item.getAmount());
            }
        }
        
        if (minAmount == Integer.MAX_VALUE) {
            return resultAmount;
        }
        
        return resultAmount * minAmount;
    }
    
    /**
     * Tracks item consumption (eating/drinking).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        Material itemType = event.getItem().getType();
        
        // Track overall item consumption
        trackStat(player, "items.consumed", 1);
        
        // Track specific item type
        trackStat(player, "items.consumed." + itemType.name().toLowerCase(), 1);
        
        // Track food vs potion
        if (itemType.isEdible()) {
            trackStat(player, "items.food_eaten", 1);
        } else if (itemType == Material.POTION) {
            trackStat(player, "items.potions_drunk", 1);
        }
    }
    
    /**
     * Tracks fishing.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Track fishing
        trackStat(player, "fishing.caught", 1);
        
        // Track what was caught if it's an item
        if (event.getCaught() != null && event.getCaught().getType() == EntityType.DROPPED_ITEM) {
            ItemStack item = ((org.bukkit.entity.Item) event.getCaught()).getItemStack();
            Material itemType = item.getType();
            
            // Track specific item type
            trackStat(player, "fishing.caught." + itemType.name().toLowerCase(), 1);
        }
    }
    
    /**
     * Tracks player movement.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Skip small movements (looking around)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Get current position
        double x = event.getTo().getX();
        double z = event.getTo().getZ();
        
        // Check if we have previous position
        if (lastXPosition.containsKey(uuid) && lastZPosition.containsKey(uuid)) {
            // Calculate distance moved
            double lastX = lastXPosition.get(uuid);
            double lastZ = lastZPosition.get(uuid);
            
            double distance = Math.sqrt(Math.pow(x - lastX, 2) + Math.pow(z - lastZ, 2));
            
            // Update total distance
            double total = distanceTraveled.getOrDefault(uuid, 0.0) + distance;
            distanceTraveled.put(uuid, total);
            
            // If over 1 block, track and reset
            if (total >= 1.0) {
                int blocks = (int) total;
                trackStat(player, "movement.distance", blocks);
                
                // Track specific movement type
                if (player.isSwimming()) {
                    trackStat(player, "movement.swimming", blocks);
                } else if (player.isGliding()) {
                    trackStat(player, "movement.gliding", blocks);
                } else if (player.isFlying()) {
                    trackStat(player, "movement.flying", blocks);
                } else {
                    trackStat(player, "movement.walking", blocks);
                }
                
                // Reset with remainder
                distanceTraveled.put(uuid, total - blocks);
            }
        }
        
        // Update last position
        lastXPosition.put(uuid, x);
        lastZPosition.put(uuid, z);
    }
}