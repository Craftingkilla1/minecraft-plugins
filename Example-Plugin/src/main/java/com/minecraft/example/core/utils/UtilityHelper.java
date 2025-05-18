// ./Example-Plugin/src/main/java/com/minecraft/example/core/utils/UtilityHelper.java
package com.minecraft.example.core.utils;

import com.minecraft.core.utils.FormatUtil;
import com.minecraft.core.utils.InventoryUtil;
import com.minecraft.core.utils.TimeUtil;
import com.minecraft.example.data.Achievement;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Utility class with helper methods.
 */
public class UtilityHelper {
    
    // Pattern for validating leaderboard IDs
    private static final Pattern LEADERBOARD_ID_PATTERN = Pattern.compile("^[a-z0-9_-]{3,32}$");
    
    // Pattern for validating achievement IDs
    private static final Pattern ACHIEVEMENT_ID_PATTERN = Pattern.compile("^[a-z0-9_-]{3,32}$");
    
    /**
     * Formats a timestamp to a readable date string.
     *
     * @param timestamp The timestamp
     * @return The formatted date string
     */
    public static String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "Never";
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm");
        return timestamp.format(formatter);
    }
    
    /**
     * Formats a duration in seconds to a readable string.
     *
     * @param seconds The duration in seconds
     * @return The formatted duration string
     */
    public static String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + " seconds";
        }
        
        if (seconds < 3600) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return minutes + " minute" + (minutes != 1 ? "s" : "") + 
                   (remainingSeconds > 0 ? " " + remainingSeconds + " second" + (remainingSeconds != 1 ? "s" : "") : "");
        }
        
        if (seconds < 86400) {
            long hours = seconds / 3600;
            long remainingMinutes = (seconds % 3600) / 60;
            return hours + " hour" + (hours != 1 ? "s" : "") + 
                   (remainingMinutes > 0 ? " " + remainingMinutes + " minute" + (remainingMinutes != 1 ? "s" : "") : "");
        }
        
        long days = seconds / 86400;
        long remainingHours = (seconds % 86400) / 3600;
        return days + " day" + (days != 1 ? "s" : "") + 
               (remainingHours > 0 ? " " + remainingHours + " hour" + (remainingHours != 1 ? "s" : "") : "");
    }
    
    /**
     * Formats a progress value (0-1) to a percentage string.
     *
     * @param progress The progress value (0-1)
     * @return The formatted percentage string
     */
    public static String formatProgress(double progress) {
        // Ensure the progress is between 0 and 1
        progress = Math.max(0, Math.min(1, progress));
        
        // Convert to percentage
        int percentage = (int) Math.round(progress * 100);
        
        // Build progress bar
        StringBuilder progressBar = new StringBuilder();
        int bars = percentage / 5; // 20 bars = 100%
        
        progressBar.append(ChatColor.GREEN);
        for (int i = 0; i < bars; i++) {
            progressBar.append("█");
        }
        
        progressBar.append(ChatColor.RED);
        for (int i = bars; i < 20; i++) {
            progressBar.append("█");
        }
        
        return progressBar.toString() + ChatColor.YELLOW + " " + percentage + "%";
    }
    
    /**
     * Validates a leaderboard ID.
     *
     * @param id The leaderboard ID
     * @return True if valid, false otherwise
     */
    public static boolean isValidLeaderboardId(String id) {
        return id != null && LEADERBOARD_ID_PATTERN.matcher(id).matches();
    }
    
    /**
     * Validates an achievement ID.
     *
     * @param id The achievement ID
     * @return True if valid, false otherwise
     */
    public static boolean isValidAchievementId(String id) {
        return id != null && ACHIEVEMENT_ID_PATTERN.matcher(id).matches();
    }
    
    /**
     * Creates an item for the leaderboard menu.
     *
     * @param position The position in the leaderboard (1-based)
     * @param playerName The player name
     * @param score The player's score
     * @param material The material to use for the item
     * @return The item
     */
    public static ItemStack createLeaderboardItem(int position, String playerName, int score, Material material) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Position: " + ChatColor.GOLD + "#" + position);
        lore.add(ChatColor.GRAY + "Score: " + ChatColor.GOLD + FormatUtil.formatNumber(score));
        
        // Add medal emoji for top positions
        String prefix;
        switch (position) {
            case 1:
                prefix = ChatColor.GOLD + "🥇 ";
                break;
            case 2:
                prefix = ChatColor.GRAY + "🥈 ";
                break;
            case 3:
                prefix = ChatColor.DARK_RED + "🥉 ";
                break;
            default:
                prefix = ChatColor.WHITE + "#" + position + " ";
                break;
        }
        
        return InventoryUtil.createItem(material, prefix + ChatColor.WHITE + playerName, lore);
    }
    
    /**
     * Creates an item for the achievement menu.
     *
     * @param name The achievement name
     * @param description The achievement description
     * @param material The material to use for the item
     * @param earned Whether the achievement has been earned
     * @param progress The progress towards the achievement (0-1)
     * @param secret Whether the achievement is secret
     * @return The item
     */
    public static ItemStack createAchievementItem(String name, String description, Material material, 
                                                 boolean earned, double progress, boolean secret) {
        List<String> lore = new ArrayList<>();
        
        if (secret && !earned) {
            // Secret achievement that hasn't been earned
            lore.add(ChatColor.DARK_PURPLE + "??? Secret Achievement ???");
            lore.add(ChatColor.GRAY + "Complete special requirements to unlock");
        } else {
            // Regular achievement or earned secret
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            
            if (earned) {
                lore.add(ChatColor.GREEN + "✓ Completed");
            } else {
                lore.add(ChatColor.RED + "✗ Incomplete");
                lore.add(ChatColor.GRAY + "Progress: " + formatProgress(progress));
            }
        }
        
        if (earned) {
            return InventoryUtil.makeGlow(InventoryUtil.createItem(material, ChatColor.GREEN + name, lore));
        } else {
            return InventoryUtil.createItem(material, ChatColor.RED + name, lore);
        }
    }
    
    /**
     * Creates a confirmation inventory.
     *
     * @param title The inventory title
     * @param yesAction The action to perform when Yes is clicked
     * @param noAction The action to perform when No is clicked
     * @return The confirmation inventory
     */
    public static Inventory createConfirmationInventory(String title, Consumer<Player> yesAction, Consumer<Player> noAction) {
        // Now passing title as separate parameter to match Core-Utils API change
        return InventoryUtil.createConfirmationInventory(
                title,
                "Are you sure?", // Using a default question text
                yesAction,
                noAction
        );
    }
    
    /**
     * Creates a paginated inventory for displaying statistics.
     *
     * @param title The inventory title
     * @param player The player whose stats to display
     * @param stats The player's statistics
     * @param page The page number (0-based)
     * @return The inventory
     */
    public static Inventory createStatsInventory(String title, Player player, Map<String, Integer> stats, int page) {
        int rows = 6;
        int size = rows * 9;
        Inventory inventory = player.getServer().createInventory(null, size, title);
        
        // Create border
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, InventoryUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
            inventory.setItem(size - 9 + i, InventoryUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }
        
        for (int i = 1; i < rows - 1; i++) {
            inventory.setItem(i * 9, InventoryUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
            inventory.setItem(i * 9 + 8, InventoryUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }
        
        // Add player head
        inventory.setItem(4, InventoryUtil.createPlayerHead(player, 
                ChatColor.GOLD + player.getName() + "'s Stats", 
                ChatColor.GRAY + "Joined: " + TimeUtil.formatDate(player.getFirstPlayed()),
                ChatColor.GRAY + "Page " + (page + 1)));
        
        // Calculate stats per page
        int slotsPerPage = 28; // 4 rows of 7 slots each
        int totalPages = (int) Math.ceil(stats.size() / (double) slotsPerPage);
        page = Math.min(page, Math.max(0, totalPages - 1)); // Ensure page is valid
        
        // Add stats
        List<Map.Entry<String, Integer>> statList = new ArrayList<>(stats.entrySet());
        int startIndex = page * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, statList.size());
        
        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, Integer> entry = statList.get(i);
            String statName = entry.getKey();
            int value = entry.getValue();
            
            // Skip border slots
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
            }
            
            // Add stat item
            Material material = getStatMaterial(statName);
            inventory.setItem(slot, InventoryUtil.createItem(material, 
                    ChatColor.YELLOW + formatStatName(statName), 
                    ChatColor.GRAY + "Value: " + ChatColor.WHITE + FormatUtil.formatNumber(value)));
            
            slot++;
        }
        
        // Add navigation buttons
        if (page > 0) {
            inventory.setItem(size - 9 + 3, InventoryUtil.createItem(Material.ARROW, 
                    ChatColor.YELLOW + "Previous Page", 
                    ChatColor.GRAY + "Page " + page + " of " + totalPages));
        }
        
        if (page < totalPages - 1) {
            inventory.setItem(size - 9 + 5, InventoryUtil.createItem(Material.ARROW, 
                    ChatColor.YELLOW + "Next Page", 
                    ChatColor.GRAY + "Page " + (page + 2) + " of " + totalPages));
        }
        
        // Add close button
        inventory.setItem(size - 9 + 4, InventoryUtil.createItem(Material.BARRIER, ChatColor.RED + "Close"));
        
        return inventory;
    }
    
    /**
     * Gets an appropriate material for a statistic.
     *
     * @param statName The statistic name
     * @return The material
     */
    private static Material getStatMaterial(String statName) {
        statName = statName.toLowerCase();
        
        if (statName.contains("broken")) {
            return Material.IRON_PICKAXE;
        } else if (statName.contains("placed")) {
            return Material.BRICK;
        } else if (statName.contains("kill")) {
            return Material.DIAMOND_SWORD;
        } else if (statName.contains("death")) {
            return Material.SKELETON_SKULL;
        } else if (statName.contains("craft")) {
            return Material.CRAFTING_TABLE;
        } else if (statName.contains("eat") || statName.contains("consume")) {
            return Material.COOKED_BEEF;
        } else if (statName.contains("distance") || statName.contains("travel")) {
            return Material.LEATHER_BOOTS;
        } else if (statName.contains("time") || statName.contains("played")) {
            return Material.CLOCK;
        }
        
        return Material.PAPER;
    }
    
    /**
     * Formats a statistic name for display.
     *
     * @param statName The statistic name
     * @return The formatted name
     */
    private static String formatStatName(String statName) {
        if (statName.contains(".")) {
            String[] parts = statName.split("\\.");
            if (parts.length > 1) {
                statName = parts[1];
            }
        }
        
        return FormatUtil.capitalizeWords(statName.replace('_', ' '));
    }
    
    /**
     * Creates a paginated inventory for displaying achievements.
     *
     * @param title The inventory title
     * @param achievements The player's achievements
     * @param page The page number (0-based)
     * @return The inventory
     */
    public static Inventory createAchievementInventory(String title, List<Achievement> achievements, int page) {
        int rows = 6;
        int size = rows * 9;
        Inventory inventory = player.getServer().createInventory(null, size, title);
        
        // ... (similar to createStatsInventory)
        
        return inventory;
    }
    
    /**
     * Groups statistics by category.
     *
     * @param stats The statistics map
     * @return A map of category names to statistics maps
     */
    public static Map<String, Map<String, Integer>> groupStatsByCategory(Map<String, Integer> stats) {
        Map<String, Map<String, Integer>> groupedStats = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            String key = entry.getKey();
            String category = "general";
            
            if (key.contains(".")) {
                String[] parts = key.split("\\.", 2);
                category = parts[0];
                key = parts[1];
            }
            
            Map<String, Integer> categoryStats = groupedStats.computeIfAbsent(category, k -> new HashMap<>());
            categoryStats.put(key, entry.getValue());
        }
        
        return groupedStats;
    }
}