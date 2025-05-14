// ./Example-Plugin/src/main/java/com/minecraft/example/gui/StatsMenu.java
package com.minecraft.example.gui;

import com.minecraft.core.utils.FormatUtil;
import com.minecraft.core.utils.InventoryUtil;
import com.minecraft.example.PlayerStatsPlugin;
import com.minecraft.example.api.StatsService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GUI for displaying player statistics.
 */
public class StatsMenu implements Listener {
    
    private final PlayerStatsPlugin plugin;
    private final Player viewer;
    private final Player target;
    private final StatsService statsService;
    private Inventory inventory;
    
    // Menu state and configuration
    private String currentCategory = "general";
    private final List<String> categories = Arrays.asList("general", "blocks", "combat", "items", "movement");
    private int currentPage = 0;
    private final int STATS_PER_PAGE = 28;
    
    /**
     * Creates a new StatsMenu for viewing your own stats.
     *
     * @param plugin The plugin instance
     * @param player The player viewing their own stats
     */
    public StatsMenu(PlayerStatsPlugin plugin, Player player) {
        this(plugin, player, player);
    }
    
    /**
     * Creates a new StatsMenu for viewing another player's stats.
     *
     * @param plugin The plugin instance
     * @param viewer The player viewing the stats
     * @param target The player whose stats are being viewed
     */
    public StatsMenu(PlayerStatsPlugin plugin, Player viewer, Player target) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.target = target;
        this.statsService = plugin.getStatsService();
    }
    
    /**
     * Opens the stats menu.
     */
    public void open() {
        // Create inventory
        String title = target.equals(viewer) 
                ? ChatColor.DARK_GREEN + "Your Statistics" 
                : ChatColor.DARK_GREEN + target.getName() + "'s Statistics";
        
        this.inventory = Bukkit.createInventory(null, 54, title);
        
        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Load stats data
        loadStatsAsync();
        
        // Show inventory
        viewer.openInventory(inventory);
    }
    
    /**
     * Loads player statistics asynchronously.
     */
    private void loadStatsAsync() {
        // Show loading indicator
        showLoadingIndicator();
        
        // Load stats async
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Integer> stats = statsService.getAllStats(target);
            
            // Update inventory on the main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                updateInventory(stats);
            });
        });
    }
    
    /**
     * Shows a loading indicator in the inventory.
     */
    private void showLoadingIndicator() {
        // Show loading indicator
        ItemStack loadingItem = InventoryUtil.createItem(Material.HOPPER,
                ChatColor.YELLOW + "Loading statistics...",
                ChatColor.GRAY + "Please wait while we retrieve the data.");
        
        inventory.setItem(22, loadingItem);
        
        // Create border
        InventoryUtil.createBorder(inventory, 54, InventoryUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
    }
    
    /**
     * Updates the inventory with player statistics.
     *
     * @param stats The player statistics
     */
    private void updateInventory(Map<String, Integer> stats) {
        // Clear inventory
        inventory.clear();
        
        // Create border
        InventoryUtil.createBorder(inventory, 54, InventoryUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        
        // Set category selection buttons
        setupCategoryButtons();
        
        // Show category stats
        displayStats(stats);
        
        // Show navigation buttons if needed
        setupNavigationButtons(stats);
        
        // Update player head icon
        inventory.setItem(4, InventoryUtil.createPlayerHead(target, 
                ChatColor.GOLD + target.getName(),
                ChatColor.GRAY + "Joined: " + getFirstJoinDate(),
                ChatColor.GRAY + "Playtime: " + getPlaytimeFormatted(stats)));
    }
    
    /**
     * Sets up category selection buttons.
     */
    private void setupCategoryButtons() {
        // General category
        inventory.setItem(1, createCategoryButton("general", Material.BOOK, "General Statistics"));
        
        // Blocks category
        inventory.setItem(2, createCategoryButton("blocks", Material.GRASS_BLOCK, "Block Statistics"));
        
        // Combat category
        inventory.setItem(3, createCategoryButton("combat", Material.DIAMOND_SWORD, "Combat Statistics"));
        
        // Items category
        inventory.setItem(5, createCategoryButton("items", Material.CHEST, "Item Statistics"));
        
        // Movement category
        inventory.setItem(6, createCategoryButton("movement", Material.LEATHER_BOOTS, "Movement Statistics"));
        
        // Close button
        inventory.setItem(8, InventoryUtil.createItem(Material.BARRIER, ChatColor.RED + "Close"));
    }
    
    /**
     * Creates a category button.
     *
     * @param category The category name
     * @param material The button material
     * @param displayName The display name
     * @return The category button
     */
    private ItemStack createCategoryButton(String category, Material material, String displayName) {
        boolean isSelected = category.equals(currentCategory);
        
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        
        if (meta != null) {
            // Set display name
            meta.setDisplayName(isSelected 
                    ? ChatColor.GREEN + displayName + ChatColor.BOLD + " ✓" 
                    : ChatColor.YELLOW + displayName);
            
            // Set lore
            List<String> lore = new ArrayList<>();
            lore.add(isSelected 
                    ? ChatColor.GRAY + "Currently selected" 
                    : ChatColor.GRAY + "Click to view");
            meta.setLore(lore);
            
            // Add glow effect if selected
            if (isSelected) {
                button = InventoryUtil.makeGlow(button);
            }
            
            button.setItemMeta(meta);
        }
        
        return button;
    }
    
    /**
     * Displays player statistics for the current category and page.
     *
     * @param stats The player statistics
     */
    private void displayStats(Map<String, Integer> stats) {
        // Filter stats by category
        Map<String, Integer> filteredStats = filterStatsByCategory(stats, currentCategory);
        
        // Sort stats by value in descending order
        List<Map.Entry<String, Integer>> sortedStats = filteredStats.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());
        
        // Calculate pagination
        int totalPages = (int) Math.ceil((double) sortedStats.size() / STATS_PER_PAGE);
        currentPage = Math.min(currentPage, Math.max(0, totalPages - 1));
        
        // Get stats for current page
        int startIndex = currentPage * STATS_PER_PAGE;
        int endIndex = Math.min(startIndex + STATS_PER_PAGE, sortedStats.size());
        
        if (sortedStats.isEmpty()) {
            // No stats for this category
            inventory.setItem(22, InventoryUtil.createItem(Material.BARRIER,
                    ChatColor.RED + "No statistics found",
                    ChatColor.GRAY + "This player has no recorded statistics",
                    ChatColor.GRAY + "for the " + currentCategory + " category."));
            return;
        }
        
        // Display stats
        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, Integer> entry = sortedStats.get(i);
            String statName = entry.getKey();
            int value = entry.getValue();
            
            // Skip empty rows and border slots
            if (slot % 9 == 0) {
                slot++;
            }
            if (slot % 9 == 8) {
                slot += 2;
            }
            
            // Create stat display item
            Material material = getStatMaterial(statName);
            String displayName = getStatDisplayName(statName);
            
            ItemStack item = InventoryUtil.createItem(material,
                    ChatColor.YELLOW + displayName,
                    ChatColor.WHITE + "Value: " + ChatColor.GOLD + FormatUtil.formatNumber(value));
            
            inventory.setItem(slot, item);
            slot++;
        }
    }
    
    /**
     * Sets up navigation buttons if pagination is needed.
     *
     * @param stats The player statistics
     */
    private void setupNavigationButtons(Map<String, Integer> stats) {
        // Filter stats by category
        Map<String, Integer> filteredStats = filterStatsByCategory(stats, currentCategory);
        
        // Calculate total pages
        int totalPages = (int) Math.ceil((double) filteredStats.size() / STATS_PER_PAGE);
        
        // Show previous page button if needed
        if (currentPage > 0) {
            inventory.setItem(45, InventoryUtil.createItem(Material.ARROW,
                    ChatColor.YELLOW + "Previous Page",
                    ChatColor.GRAY + "Page " + (currentPage) + " of " + totalPages));
        }
        
        // Show next page button if needed
        if (currentPage < totalPages - 1) {
            inventory.setItem(53, InventoryUtil.createItem(Material.ARROW,
                    ChatColor.YELLOW + "Next Page",
                    ChatColor.GRAY + "Page " + (currentPage + 2) + " of " + totalPages));
        }
        
        // Show page indicator
        if (totalPages > 1) {
            inventory.setItem(49, InventoryUtil.createItem(Material.PAPER,
                    ChatColor.YELLOW + "Page " + (currentPage + 1) + " of " + totalPages));
        }
    }
    
    /**
     * Handles inventory click events.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory) {
            return;
        }
        
        // Cancel the event to prevent item movement
        event.setCancelled(true);
        
        if (event.getWhoClicked() != viewer) {
            return;
        }
        
        // Get clicked item
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        // Handle button clicks
        int slot = event.getSlot();
        
        if (slot == 1) {
            // General category
            currentCategory = "general";
            currentPage = 0;
            loadStatsAsync();
        } else if (slot == 2) {
            // Blocks category
            currentCategory = "blocks";
            currentPage = 0;
            loadStatsAsync();
        } else if (slot == 3) {
            // Combat category
            currentCategory = "combat";
            currentPage = 0;
            loadStatsAsync();
        } else if (slot == 5) {
            // Items category
            currentCategory = "items";
            currentPage = 0;
            loadStatsAsync();
        } else if (slot == 6) {
            // Movement category
            currentCategory = "movement";
            currentPage = 0;
            loadStatsAsync();
        } else if (slot == 8) {
            // Close button
            viewer.closeInventory();
        } else if (slot == 45 && clickedItem.getType() == Material.ARROW) {
            // Previous page
            currentPage--;
            loadStatsAsync();
        } else if (slot == 53 && clickedItem.getType() == Material.ARROW) {
            // Next page
            currentPage++;
            loadStatsAsync();
        }
    }
    
    /**
     * Handles inventory close events.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != inventory) {
            return;
        }
        
        // Unregister listeners
        HandlerList.unregisterAll(this);
    }
    
    /**
     * Filters statistics by category.
     *
     * @param stats The player statistics
     * @param category The category to filter by
     * @return The filtered statistics
     */
    private Map<String, Integer> filterStatsByCategory(Map<String, Integer> stats, String category) {
        Map<String, Integer> filtered = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            String key = entry.getKey();
            
            if (category.equals("general")) {
                // General category includes stats without a dot or with a top-level category that's not in our list
                if (!key.contains(".") || !categories.contains(key.substring(0, key.indexOf('.')))) {
                    filtered.put(key, entry.getValue());
                }
            } else if (key.startsWith(category + ".")) {
                // Include stats that start with the category prefix
                filtered.put(key, entry.getValue());
            }
        }
        
        return filtered;
    }
    
    /**
     * Gets the display name for a statistic.
     *
     * @param statName The statistic name
     * @return The display name
     */
    private String getStatDisplayName(String statName) {
        // Check if we have a custom display name in the config
        String displayName = plugin.getPluginConfig().getStatDisplayName(statName);
        
        if (displayName.equals(statName)) {
            // No custom display name, format the stat name
            if (statName.contains(".")) {
                // For category.stat format, just use the stat part
                displayName = statName.substring(statName.indexOf('.') + 1);
            }
            
            // Convert to title case and replace underscores with spaces
            displayName = FormatUtil.capitalizeWords(displayName.replace('_', ' '));
        }
        
        return displayName;
    }
    
    /**
     * Gets an appropriate material icon for a statistic.
     *
     * @param statName The statistic name
     * @return The material for the icon
     */
    private Material getStatMaterial(String statName) {
        // Default icon
        Material defaultMaterial = Material.PAPER;
        
        // Check for specific stat names
        if (statName.contains("broken")) {
            return Material.IRON_PICKAXE;
        } else if (statName.contains("placed")) {
            return Material.BRICK;
        } else if (statName.contains("kills")) {
            return Material.DIAMOND_SWORD;
        } else if (statName.contains("deaths")) {
            return Material.SKELETON_SKULL;
        } else if (statName.contains("damage")) {
            return Material.SHIELD;
        } else if (statName.contains("crafted")) {
            return Material.CRAFTING_TABLE;
        } else if (statName.contains("consumed") || statName.contains("eaten")) {
            return Material.COOKED_BEEF;
        } else if (statName.contains("distance") || statName.contains("movement")) {
            return Material.LEATHER_BOOTS;
        } else if (statName.contains("sessions")) {
            return Material.CLOCK;
        } else if (statName.contains("playtime")) {
            return Material.CLOCK;
        } else if (statName.contains("fishing")) {
            return Material.FISHING_ROD;
        }
        
        // Check for category prefixes
        if (statName.startsWith("blocks.")) {
            return Material.GRASS_BLOCK;
        } else if (statName.startsWith("combat.")) {
            return Material.IRON_SWORD;
        } else if (statName.startsWith("items.")) {
            return Material.CHEST;
        } else if (statName.startsWith("movement.")) {
            return Material.LEATHER_BOOTS;
        }
        
        return defaultMaterial;
    }
    
    /**
     * Gets the player's first join date.
     *
     * @return The formatted first join date
     */
    private String getFirstJoinDate() {
        // Try to get from stats
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        
        try {
            // Get from player metadata
            LocalDateTime firstJoin = target.getFirstPlayed() > 0 
                    ? LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(target.getFirstPlayed()), 
                            java.time.ZoneId.systemDefault())
                    : LocalDateTime.now();
            
            return firstJoin.format(formatter);
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * Gets the player's playtime formatted as a string.
     *
     * @param stats The player statistics
     * @return The formatted playtime
     */
    private String getPlaytimeFormatted(Map<String, Integer> stats) {
        int playtimeSeconds = stats.getOrDefault("sessions.playtime", 0);
        
        if (playtimeSeconds <= 0) {
            return "Less than a minute";
        }
        
        // Format playtime
        int hours = playtimeSeconds / 3600;
        int minutes = (playtimeSeconds % 3600) / 60;
        
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + " minutes";
        }
    }
}