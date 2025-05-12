// ./Example-Plugin/src/main/java/com/example/exampleplugin/PlayerListener.java
package com.example.exampleplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for player events to update statistics.
 */
public class PlayerListener implements Listener {
    
    private final ExamplePlugin plugin;
    private final PlayerDataManager playerDataManager;
    
    /**
     * Constructor for PlayerListener.
     *
     * @param plugin The plugin instance
     * @param playerDataManager The player data manager
     */
    public PlayerListener(ExamplePlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }
    
    /**
     * Handle player join events.
     *
     * @param event The join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load or create player data asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            playerDataManager.createOrUpdatePlayerData(player);
        });
    }
    
    /**
     * Handle player quit events.
     *
     * @param event The quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Save and remove player data asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData playerData = playerDataManager.getPlayerData(player.getUniqueId());
            
            if (playerData != null) {
                // Update last seen timestamp
                playerData.setLastSeen(System.currentTimeMillis());
                
                // Save player data and remove from cache
                playerDataManager.savePlayerData(playerData);
                playerDataManager.removePlayerData(player.getUniqueId());
            }
        });
    }
    
    /**
     * Handle block break events.
     *
     * @param event The block break event
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Skip if not in survival mode
        if (event.getPlayer().getGameMode().toString().equals("CREATIVE")) {
            return;
        }
        
        Player player = event.getPlayer();
        PlayerData playerData = playerDataManager.getPlayerData(player.getUniqueId());
        
        if (playerData != null) {
            playerData.incrementBlocksBroken();
        }
    }
    
    /**
     * Handle block place events.
     *
     * @param event The block place event
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // Skip if not in survival mode
        if (event.getPlayer().getGameMode().toString().equals("CREATIVE")) {
            return;
        }
        
        Player player = event.getPlayer();
        PlayerData playerData = playerDataManager.getPlayerData(player.getUniqueId());
        
        if (playerData != null) {
            playerData.incrementBlocksPlaced();
        }
    }
    
    /**
     * Handle player death events.
     *
     * @param event The player death event
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData playerData = playerDataManager.getPlayerData(player.getUniqueId());
        
        if (playerData != null) {
            playerData.incrementDeaths();
        }
    }
    
    /**
     * Handle entity death events.
     *
     * @param event The entity death event
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Check if a player killed the entity
        Player killer = event.getEntity().getKiller();
        
        if (killer != null) {
            PlayerData playerData = playerDataManager.getPlayerData(killer.getUniqueId());
            
            if (playerData != null) {
                playerData.incrementMobsKilled();
            }
        }
    }
}