// ./Example-Plugin/src/main/java/com/minecraft/example/core/commands/StatsCommand.java
package com.minecraft.example.core.commands;

import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.command.TabCompletionProvider;
import com.minecraft.core.utils.FormatUtil;
import com.minecraft.example.ExamplePlugin;
import com.minecraft.example.core.services.NotificationService;
import com.minecraft.example.core.services.StatsService;
import com.minecraft.example.core.utils.MapUtil;
import com.minecraft.example.core.utils.UtilityHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command for viewing player statistics.
 */
@Command(name = "stats", description = "View player statistics", aliases = {"playerstats", "pstats"})
public class StatsCommand implements TabCompletionProvider {
    
    private final ExamplePlugin plugin;
    private final StatsService statsService;
    private final NotificationService notificationService;
    
    /**
     * Creates a new StatsCommand instance.
     *
     * @param plugin The plugin instance
     * @param statsService The stats service
     * @param notificationService The notification service
     */
    public StatsCommand(ExamplePlugin plugin, StatsService statsService, NotificationService notificationService) {
        this.plugin = plugin;
        this.statsService = statsService;
        this.notificationService = notificationService;
    }
    
    /**
     * Default command handler. Shows the player's own stats or opens the stats GUI.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "", description = "View your own stats")
    public void defaultCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            notificationService.sendMessage((Player)sender, "commands.player-only");
            return;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Open the stats GUI
            showStatsMenu(player, player);
            return;
        }
        
        if (args.length == 1) {
            // View another player's stats if permission exists
            if (!player.hasPermission("example.stats.others")) {
                notificationService.sendMessage(player, "commands.no-permission");
                return;
            }
            
            String targetName = args[0];
            Player target = Bukkit.getPlayer(targetName);
            
            if (target == null) {
                notificationService.sendMessage(player, "commands.player-not-found");
                return;
            }
            
            // Open the stats GUI for the target player
            showStatsMenu(player, target);
        }
    }
    
    /**
     * Shows the player's statistics in chat format.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "show", description = "View stats in chat format")
    public void showCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) && args.length == 0) {
            sender.sendMessage(ChatColor.RED + "You must specify a player name when using this command from console.");
            return;
        }
        
        Player target;
        if (args.length == 0) {
            target = (Player) sender;
        } else {
            String targetName = args[0];
            target = Bukkit.getPlayer(targetName);
            
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + targetName);
                return;
            }
            
            // Check permission for viewing other players
            if (sender instanceof Player && !sender.equals(target) && !sender.hasPermission("example.stats.others")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to view other players' stats.");
                return;
            }
        }
        
        // Get player stats
        Map<String, Integer> stats = statsService.getAllStats(target);
        
        if (stats.isEmpty()) {
            sender.sendMessage(ChatColor.RED + target.getName() + " has no recorded statistics.");
            return;
        }
        
        // Display stats
        sender.sendMessage(ChatColor.GREEN + "=== " + target.getName() + "'s Statistics ===");
        
        // Group stats by category
        Map<String, Map<String, Integer>> groupedStats = UtilityHelper.groupStatsByCategory(stats);
        
        // Display each category
        for (Map.Entry<String, Map<String, Integer>> category : groupedStats.entrySet()) {
            String categoryName = FormatUtil.capitalizeWords(category.getKey());
            sender.sendMessage(ChatColor.YELLOW + categoryName + ":");
            
            // Display stats in this category
            for (Map.Entry<String, Integer> entry : category.getValue().entrySet()) {
                String statName = FormatUtil.capitalizeWords(entry.getKey().replace('_', ' '));
                String formattedValue = FormatUtil.formatNumber(entry.getValue());
                
                // Send a message to the sender instead of using notificationService
                sender.sendMessage(ChatColor.GRAY + " • " + ChatColor.WHITE + statName + ": " + 
                        ChatColor.GOLD + formattedValue);
                
                // This was causing the error - notificationService only works with Players
                // notificationService.sendSuccessMessage(sender,
                //         "stats.show-entry",
                //         Map.of("stat", entry.getKey(), "value", entry.getValue())
                // );
            }
        }
    }
    
    /**
     * Compares the player's statistics with another player.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "compare", description = "Compare stats with another player", minArgs = 1)
    @Permission(value = "example.stats.others", message = "You don't have permission to compare stats with other players.")
    public void compareCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return;
        }
        
        Player player = (Player) sender;
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            notificationService.sendMessage(player, "commands.player-not-found");
            return;
        }
        
        if (player.equals(target)) {
            notificationService.sendMessage(player, "stats.compare-self");
            return;
        }
        
        // Get player stats
        Map<String, Integer> playerStats = statsService.getAllStats(player);
        Map<String, Integer> targetStats = statsService.getAllStats(target);
        
        if (playerStats.isEmpty() || targetStats.isEmpty()) {
            notificationService.sendMessage(player, "stats.no-stats-compare");
            return;
        }
        
        // Display comparison
        player.sendMessage(ChatColor.GREEN + "=== Comparing Stats: " + player.getName() + 
                " vs " + target.getName() + " ===");
        
        // Group stats by category
        Map<String, Map<String, Integer>> groupedPlayerStats = UtilityHelper.groupStatsByCategory(playerStats);
        Map<String, Map<String, Integer>> groupedTargetStats = UtilityHelper.groupStatsByCategory(targetStats);
        
        // Merge category keys
        Set<String> allCategories = new HashSet<>();
        allCategories.addAll(groupedPlayerStats.keySet());
        allCategories.addAll(groupedTargetStats.keySet());
        
        // Display each category
        for (String category : allCategories) {
            String categoryName = FormatUtil.capitalizeWords(category);
            player.sendMessage(ChatColor.YELLOW + categoryName + ":");
            
            // Get stats maps
            Map<String, Integer> playerCategoryStats = groupedPlayerStats.getOrDefault(category, Collections.emptyMap());
            Map<String, Integer> targetCategoryStats = groupedTargetStats.getOrDefault(category, Collections.emptyMap());
            
            // Merge stat keys
            Set<String> allStats = new HashSet<>();
            allStats.addAll(playerCategoryStats.keySet());
            allStats.addAll(targetCategoryStats.keySet());
            
            // Display each stat
            for (String stat : allStats) {
                int playerValue = playerCategoryStats.getOrDefault(stat, 0);
                int targetValue = targetCategoryStats.getOrDefault(stat, 0);
                
                // Skip if both are zero
                if (playerValue == 0 && targetValue == 0) {
                    continue;
                }
                
                String statName = FormatUtil.capitalizeWords(stat.replace('_', ' '));
                String playerFormatted = FormatUtil.formatNumber(playerValue);
                String targetFormatted = FormatUtil.formatNumber(targetValue);
                
                // Calculate difference
                String comparison;
                if (playerValue > targetValue) {
                    int diff = playerValue - targetValue;
                    comparison = ChatColor.GREEN + "(+" + FormatUtil.formatNumber(diff) + ")";
                } else if (playerValue < targetValue) {
                    int diff = targetValue - playerValue;
                    comparison = ChatColor.RED + "(-" + FormatUtil.formatNumber(diff) + ")";
                } else {
                    comparison = ChatColor.YELLOW + "(Equal)";
                }
                
                player.sendMessage(ChatColor.GRAY + " • " + ChatColor.WHITE + statName + ": " + 
                        ChatColor.GOLD + playerFormatted + ChatColor.GRAY + " vs " + 
                        ChatColor.GOLD + targetFormatted + " " + comparison);
            }
        }
    }
    
    /**
     * Shows the top players for a statistic.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "top", description = "View top players for a statistic", minArgs = 1)
    public void topCommand(CommandSender sender, String[] args) {
        String statName = args[0];
        int limit = 10;
        
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
                limit = Math.max(1, Math.min(50, limit)); // Limit between 1 and 50
            } catch (NumberFormatException e) {
                // If it's a player, use the notification service
                if (sender instanceof Player) {
                    // Only use notificationService for Player objects
                    notificationService.sendMessage((Player)sender, "commands.invalid-number");
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid number format: " + args[1]);
                }
                return;
            }
        }
        
        // Get top players
        Map<String, Integer> topPlayers = statsService.getTopPlayers(statName, limit);
        
        if (topPlayers.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No statistics found for: " + statName);
            return;
        }
        
        // Get display name for the stat
        String displayName = FormatUtil.capitalizeWords(statName.replace('.', ' '));
        
        // Format and display top players
        sender.sendMessage(ChatColor.GREEN + "=== Top " + limit + " Players for " + displayName + " ===");
        
        int rank = 1;
        for (Map.Entry<String, Integer> entry : topPlayers.entrySet()) {
            String playerName = entry.getKey();
            int value = entry.getValue();
            
            String rankPrefix;
            switch (rank) {
                case 1:
                    rankPrefix = ChatColor.GOLD + "1st";
                    break;
                case 2:
                    rankPrefix = ChatColor.GRAY + "2nd";
                    break;
                case 3:
                    rankPrefix = ChatColor.DARK_RED + "3rd";
                    break;
                default:
                    rankPrefix = ChatColor.DARK_GRAY + rank + "th";
                    break;
            }
            
            sender.sendMessage(rankPrefix + ChatColor.GRAY + " • " + ChatColor.WHITE + playerName + ": " + 
                    ChatColor.GOLD + FormatUtil.formatNumber(value));
            
            rank++;
        }
        
        // If sender is a player, show their rank
        if (sender instanceof Player) {
            Player player = (Player) sender;
            int playerRank = statsService.getPlayerRank(player, statName);
            
            if (playerRank > 0) {
                int playerValue = statsService.getStat(player, statName);
                sender.sendMessage(ChatColor.GRAY + "Your rank: " + ChatColor.WHITE + "#" + playerRank + 
                        ChatColor.GRAY + " with " + ChatColor.GOLD + FormatUtil.formatNumber(playerValue));
            }
        }
    }
    
    /**
     * Shows detailed help for the stats command.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "help", description = "Show help information")
    public void helpCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "=== Player Stats Help ===");
        sender.sendMessage(ChatColor.YELLOW + "/stats" + ChatColor.GRAY + " - Open your statistics menu");
        sender.sendMessage(ChatColor.YELLOW + "/stats <player>" + ChatColor.GRAY + " - View another player's statistics");
        sender.sendMessage(ChatColor.YELLOW + "/stats show" + ChatColor.GRAY + " - Show your statistics in chat");
        sender.sendMessage(ChatColor.YELLOW + "/stats show <player>" + ChatColor.GRAY + " - Show another player's statistics in chat");
        sender.sendMessage(ChatColor.YELLOW + "/stats compare <player>" + ChatColor.GRAY + " - Compare your statistics with another player");
        sender.sendMessage(ChatColor.YELLOW + "/stats top <stat> [limit]" + ChatColor.GRAY + " - View top players for a statistic");
        sender.sendMessage(ChatColor.YELLOW + "/stats help" + ChatColor.GRAY + " - Show this help message");
    }
    
    /**
     * Shows the stats menu for a player.
     *
     * @param viewer The player viewing the menu
     * @param target The player whose stats to show
     */
    private void showStatsMenu(Player viewer, Player target) {
        // Get player stats
        Map<String, Integer> stats = statsService.getAllStats(target);
        
        // Create and open the inventory
        String title = target.equals(viewer) 
                ? ChatColor.DARK_GREEN + "Your Statistics" 
                : ChatColor.DARK_GREEN + target.getName() + "'s Statistics";
        
        Inventory inventory = UtilityHelper.createStatsInventory(title, target, stats, 0);
        viewer.openInventory(inventory);
    }
    
    @Override
    public List<String> getTabCompletions(String subCommand, CommandSender sender, String[] args) {
        if (subCommand.isEmpty()) {
            if (args.length == 1) {
                // Tab complete player names and subcommands
                List<String> completions = new ArrayList<>();
                completions.add("show");
                completions.add("compare");
                completions.add("top");
                completions.add("help");
                
                if (sender.hasPermission("example.stats.others")) {
                    // Add online player names
                    Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
                }
                
                return completions;
            }
        } else if (subCommand.equalsIgnoreCase("show") || subCommand.equalsIgnoreCase("compare")) {
            if (args.length == 1 && sender.hasPermission("example.stats.others")) {
                // Tab complete player names
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
        } else if (subCommand.equalsIgnoreCase("top")) {
            if (args.length == 1) {
                // Tab complete stat names
                return Arrays.asList(
                        "blocks.broken", 
                        "blocks.placed", 
                        "combat.kills", 
                        "combat.deaths",
                        "items.crafted", 
                        "items.dropped", 
                        "movement.distance"
                );
            } else if (args.length == 2) {
                // Tab complete limits
                return Arrays.asList("5", "10", "20", "50");
            }
        }
        
        return new ArrayList<>();
    }
}