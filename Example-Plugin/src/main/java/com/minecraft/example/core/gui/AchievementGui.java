// ./Example-Plugin/src/main/java/com/minecraft/example/core/gui/AchievementGui.java
package com.minecraft.example.core.gui;

import com.minecraft.core.api.CoreAPI;
import com.minecraft.core.utils.InventoryUtil;
import com.minecraft.example.ExamplePlugin;
import com.minecraft.example.core.services.AchievementService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Interactive GUI for viewing and managing achievements.
 * Demonstrates advanced inventory manipulation with Core-Utils.
 */
public class AchievementGui implements Listener {
    
    private static final String INVENTORY_TITLE = "Achievements";
    private static final int INVENTORY_SIZE = 54; // 6 rows
    
    private final ExamplePlugin plugin;
    private final Map<UUID, Integer> playerPages;
    private final Set<UUID> activeViewers;
    
    /**
     * Constructs a new AchievementGui.
     *
     * @param plugin The plugin instance
     */
    public AchievementGui(ExamplePlugin plugin) {
        this.plugin = plugin;
        this.playerPages = new HashMap<>();
        this.activeViewers = new HashSet<>();
        
        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Opens the achievement GUI for a player.
     *
     * @param player The player
     */
    public void openGui(Player player) {
        openGui(player, 0); // Start at first page
    }
    
    /**
     * Opens the achievement GUI for a player at a specific page.
     *
     * @param player The player
     * @param page The page number
     */
    public void openGui(Player player, int page) {
        // Get achievement service
        AchievementService achievementService = CoreAPI.Services.get(AchievementService.class);
        if (achievementService == null) {
            player.sendMessage(ChatColor.RED + "Achievement service is not available.");
            return;
        }
        
        // Get player achievements
        List<String> playerAchievements = achievementService.getPlayerAchievements(player);
        
        // Get all achievements
        Map<String, String> allAchievements = achievementService.getAllAchievements();
        
        // Calculate total pages
        int itemsPerPage = 45; // 9x5 grid, bottom row for navigation
        int totalPages = (int) Math.ceil(allAchievements.size() / (double) itemsPerPage);
        page = Math.max(0, Math.min(page, totalPages - 1));
        
        // Store current page
        playerPages.put(player.getUniqueId(), page);
        
        // Create inventory
        String title = ChatColor.DARK_GRAY + INVENTORY_TITLE + " - Page " + (page + 1) + "/" + totalPages;
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, title);
        
        // Add achievements
        List<Map.Entry<String, String>> achievements = new ArrayList<>(allAchievements.entrySet());
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, achievements.size());
        
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, String> entry = achievements.get(i);
            String achievementId = entry.getKey();
            String achievementName = entry.getValue();
            
            // Get achievement details
            Map<String, Object> details = achievementService.getAchievementDetails(achievementId);
            String description = (String) details.getOrDefault("description", "");
            String iconMaterial = (String) details.getOrDefault("icon_material", "DIAMOND");
            
            // Determine if earned
            boolean earned = playerAchievements.contains(achievementId);
            
            // Create item based on earned status
            ItemStack item;
            
            try {
                Material material = Material.valueOf(iconMaterial.toUpperCase());
                if (earned) {
                    item = InventoryUtil.createItem(
                            material,
                            "&a" + achievementName,
                            "&7" + description,
                            "&aUnlocked"
                    );
                    item = InventoryUtil.makeGlow(item);
                } else {
                    item = InventoryUtil.createItem(
                            Material.GRAY_DYE,
                            "&7" + achievementName,
                            "&8" + description,
                            "&cLocked"
                    );
                }
            } catch (Exception e) {
                // Fallback to DIAMOND if material is invalid
                item = InventoryUtil.createItem(
                        Material.DIAMOND,
                        (earned ? "&a" : "&7") + achievementName,
                        "&7" + description,
                        earned ? "&aUnlocked" : "&cLocked"
                );
                if (earned) {
                    item = InventoryUtil.makeGlow(item);
                }
            }
            
            // Add achievement ID to item NBT or lore for reference
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.getLore();
            lore.add(ChatColor.BLACK + achievementId); // Hidden ID
            meta.setLore(lore);
            item.setItemMeta(meta);
            
            inventory.setItem(slot++, item);
        }
        
        // Add navigation buttons
        addNavigationButtons(inventory, page, totalPages);
        
        // Fill empty slots
        InventoryUtil.fillEmptySlots(inventory, 
                InventoryUtil.createDivider(Material.BLACK_STAINED_GLASS_PANE));
        
        // Track active viewer
        activeViewers.add(player.getUniqueId());
        
        // Show inventory
        player.openInventory(inventory);
    }
    
    /**
     * Adds navigation buttons to the inventory.
     *
     * @param inventory The inventory
     * @param currentPage The current page
     * @param totalPages The total number of pages
     */
    private void addNavigationButtons(Inventory inventory, int currentPage, int totalPages) {
        // Previous page button
        if (currentPage > 0) {
            ItemStack prevButton = InventoryUtil.createItem(
                    Material.ARROW,
                    "&6Previous Page",
                    "&7Go to page " + currentPage,
                    "&eClick to view"
            );
            inventory.setItem(45, prevButton);
        } else {
            // Disabled previous button
            ItemStack disabledPrevButton = InventoryUtil.createItem(
                    Material.BARRIER,
                    "&7Previous Page",
                    "&8No previous page",
                    "&8Disabled"
            );
            inventory.setItem(45, disabledPrevButton);
        }
        
        // Home button
        ItemStack homeButton = InventoryUtil.createItem(
                Material.COMPASS,
                "&6Achievements Home",
                "&7Return to achievements summary",
                "&eClick to view"
        );
        inventory.setItem(49, homeButton);
        
        // Next page button
        if (currentPage < totalPages - 1) {
            ItemStack nextButton = InventoryUtil.createItem(
                    Material.ARROW,
                    "&6Next Page",
                    "&7Go to page " + (currentPage + 2),
                    "&eClick to view"
            );
            inventory.setItem(53, nextButton);
        } else {
            // Disabled next button
            ItemStack disabledNextButton = InventoryUtil.createItem(
                    Material.BARRIER,
                    "&7Next Page",
                    "&8No next page",
                    "&8Disabled"
            );
            inventory.setItem(53, disabledNextButton);
        }
        
        // Close button
        ItemStack closeButton = InventoryUtil.createItem(
                Material.BARRIER,
                "&cClose",
                "&7Close the achievements menu",
                "&eClick to close"
        );
        inventory.setItem(47, closeButton);
        
        // Refresh button
        ItemStack refreshButton = InventoryUtil.createItem(
                Material.EXPERIENCE_BOTTLE,
                "&aCheck for New Achievements",
                "&7Check if you've earned new achievements",
                "&eClick to check"
        );
        inventory.setItem(51, refreshButton);
    }
    
    /**
     * Handles inventory click events.
     *
     * @param event The event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        
        if (!activeViewers.contains(uuid)) {
            return;
        }
        
        // Cancel the event to prevent taking items
        event.setCancelled(true);
        
        // Get clicked inventory
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }
        
        // Get current page
        int currentPage = playerPages.getOrDefault(uuid, 0);
        
        // Check if this is a navigation button
        int slot = event.getSlot();
        
        if (slot == 45) {
            // Previous page
            if (currentPage > 0) {
                openGui(player, currentPage - 1);
            }
        } else if (slot == 53) {
            // Next page
            openGui(player, currentPage + 1);
        } else if (slot == 47) {
            // Close
            player.closeInventory();
        } else if (slot == 49) {
            // Home
            openGui(player, 0);
        } else if (slot == 51) {
            // Refresh/Check
            AchievementService achievementService = CoreAPI.Services.get(AchievementService.class);
            if (achievementService != null) {
                List<String> awarded = achievementService.checkAndAwardAchievements(player);
                
                if (!awarded.isEmpty()) {
                    player.sendMessage(ChatColor.GREEN + "You earned " + awarded.size() + " new achievement(s)!");
                    
                    // Reload the GUI
                    Bukkit.getScheduler().runTaskLater(plugin, () -> openGui(player, currentPage), 20L);
                } else {
                    player.sendMessage(ChatColor.YELLOW + "No new achievements earned.");
                }
            }
        } else if (slot < 45) {
            // Achievement item
            ItemStack clickedItem = clickedInventory.getItem(slot);
            if (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasLore()) {
                // Get achievement ID from lore
                List<String> lore = clickedItem.getItemMeta().getLore();
                String lastLine = lore.get(lore.size() - 1);
                
                if (lastLine.startsWith(ChatColor.BLACK.toString())) {
                    String achievementId = ChatColor.stripColor(lastLine);
                    
                    // Show achievement details
                    AchievementService achievementService = CoreAPI.Services.get(AchievementService.class);
                    if (achievementService != null) {
                        Map<String, Object> details = achievementService.getAchievementDetails(achievementId);
                        
                        if (!details.isEmpty()) {
                            String name = (String) details.getOrDefault("name", achievementId);
                            String description = (String) details.getOrDefault("description", "");
                            
                            player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + name + ChatColor.GOLD + " ===");
                            player.sendMessage(ChatColor.GRAY + description);
                            
                            boolean hasAchievement = achievementService.hasAchievement(player, achievementId);
                            player.sendMessage(hasAchievement ? 
                                    ChatColor.GREEN + "You have unlocked this achievement!" : 
                                    ChatColor.RED + "You have not unlocked this achievement yet.");
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Handles inventory close events.
     *
     * @param event The event
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Remove from active viewers
        activeViewers.remove(uuid);
    }
}