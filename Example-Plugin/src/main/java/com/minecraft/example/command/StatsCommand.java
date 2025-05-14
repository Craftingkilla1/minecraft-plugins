// ./Example-Plugin/src/main/java/com/minecraft/example/command/StatsCommand.java
package com.minecraft.example.command;

import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.command.TabCompletionProvider;
import com.minecraft.core.utils.FormatUtil;
import com.minecraft.example.PlayerStatsPlugin;
import com.minecraft.example.api.StatsService;
import com.minecraft.example.config.MessageConfig;
import com.minecraft.example.gui.StatsMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command for viewing player statistics.
 */
@Command(name = "stats", description = "View player statistics", aliases = {"playerstats", "pstats"})
public class StatsCommand implements TabCompletionProvider {
    
    private final PlayerStatsPlugin plugin;
    private final StatsService statsService;
    private final MessageConfig messageConfig;
    
    /**
     * Creates a new StatsCommand instance.
     *
     * @param plugin The plugin instance
     * @param statsService The stats service
     */
    public StatsCommand(PlayerStatsPlugin plugin, StatsService statsService) {
        this.plugin = plugin;
        this.statsService = statsService;
        this.messageConfig = plugin.getMessageConfig();
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
            messageConfig.sendMessage(sender, "commands.player-only");
            return;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Open the stats GUI
            new StatsMenu(plugin, player).open();
            return;
        }
        
        if (args.length == 1) {
            // View another player's stats if permission exists
            if (!player.hasPermission("playerstats.view.others")) {
                messageConfig.sendMessage(player, "commands.no-permission");
                return;
            }
            
            String targetName = args[0];
            Player target = Bukkit.getPlayer(targetName);
            
            if (target == null) {
                messageConfig.sendMessage(player, "commands.player-not-found", "player", targetName);
                return;
            }
            
            // Open the stats GUI for the target player
            new StatsMenu(plugin, player, target).open();
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
            messageConfig.sendMessage(sender, "commands.player-required");
            return;
        }
        
        Player target;
        if (args.length == 0) {
            target = (Player) sender;
        } else {
            String targetName = args[0];
            target = Bukkit.getPlayer(targetName);
            
            if (target == null) {
                messageConfig.sendMessage(sender, "commands.player-not-found", "player", targetName);
                return;
            }
            
            // Check permission for viewing other players
            if (sender instanceof Player && !sender.equals(target) && !sender.hasPermission("playerstats.view.others")) {
                messageConfig.sendMessage(sender, "commands.no-permission");
                return;
            }
        }
        
        // Get player stats
        Map<String, Integer> stats = statsService.getAllStats(target);
        
        if (stats.isEmpty()) {
            messageConfig.sendMessage(sender, "stats.no-stats", "player", target.getName());
            return;
        }
        
        // Format and display stats
        sender.sendMessage(ChatColor.GREEN + "=== " + target.getName() + "'s Statistics ===");
        
        // Group stats by category based on prefix
        Map<String, List<Map.Entry<String, Integer>>> groupedStats = stats.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> {
                    String key = entry.getKey();
                    if (key.contains(".")) {
                        return key.substring(0, key.indexOf('.'));
                    }
                    return "general";
                }));
        
        // Display stats by category
        groupedStats.forEach((category, statsList) -> {
            sender.sendMessage(ChatColor.YELLOW + FormatUtil.capitalizeWords(category) + ":");
            
            statsList.forEach(entry -> {
                String statName = entry.getKey();
                int value = entry.getValue();
                
                // Get display name for the stat
                String displayName = plugin.getPluginConfig().getStatDisplayName(statName);
                if (displayName.equals(statName) && statName.contains(".")) {
                    // Extract the name part after the category
                    displayName = FormatUtil.capitalizeWords(statName.substring(statName.indexOf('.') + 1));
                }
                
                sender.sendMessage(ChatColor.GRAY + " • " + ChatColor.WHITE + displayName + ": " + 
                        ChatColor.GOLD + FormatUtil.formatNumber(value));
            });
        });
    }
    
    /**
     * Compares the player's statistics with another player.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "compare", description = "Compare stats with another player", minArgs = 1)
    @Permission(value = "playerstats.view.others", message = "You don't have permission to compare stats with other players.")
    public void compareCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            messageConfig.sendMessage(sender, "commands.player-only");
            return;
        }
        
        Player player = (Player) sender;
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            messageConfig.sendMessage(player, "commands.player-not-found", "player", targetName);
            return;
        }
        
        if (player.equals(target)) {
            messageConfig.sendMessage(player, "stats.compare-self");
            return;
        }
        
        // Get player stats
        Map<String, Integer> playerStats = statsService.getAllStats(player);
        Map<String, Integer> targetStats = statsService.getAllStats(target);
        
        if (playerStats.isEmpty() || targetStats.isEmpty()) {
            messageConfig.sendMessage(player, "stats.no-stats-compare");
            return;
        }
        
        // Display comparison
        player.sendMessage(ChatColor.GREEN + "=== Comparing Stats: " + player.getName() + 
                " vs " + target.getName() + " ===");
        
        // Create a set of all stat names from both players
        List<String> allStats = new ArrayList<>(playerStats.keySet());
        targetStats.keySet().stream()
                .filter(stat -> !allStats.contains(stat))
                .forEach(allStats::add);
        
        // Group stats by category
        Map<String, List<String>> groupedStats = allStats.stream()
                .collect(Collectors.groupingBy(stat -> {
                    if (stat.contains(".")) {
                        return stat.substring(0, stat.indexOf('.'));
                    }
                    return "general";
                }));
        
        // Display stats by category
        groupedStats.forEach((category, statsList) -> {
            player.sendMessage(ChatColor.YELLOW + FormatUtil.capitalizeWords(category) + ":");
            
            for (String statName : statsList) {
                int playerValue = playerStats.getOrDefault(statName, 0);
                int targetValue = targetStats.getOrDefault(statName, 0);
                
                // Skip if both values are 0
                if (playerValue == 0 && targetValue == 0) {
                    continue;
                }
                
                // Get display name for the stat
                String displayName = plugin.getPluginConfig().getStatDisplayName(statName);
                if (displayName.equals(statName) && statName.contains(".")) {
                    // Extract the name part after the category
                    displayName = FormatUtil.capitalizeWords(statName.substring(statName.indexOf('.') + 1));
                }
                
                // Format the comparison
                String comparison;
                if (playerValue > targetValue) {
                    comparison = ChatColor.GREEN + "+" + FormatUtil.formatNumber(playerValue - targetValue);
                } else if (playerValue < targetValue) {
                    comparison = ChatColor.RED + "-" + FormatUtil.formatNumber(targetValue - playerValue);
                } else {
                    comparison = ChatColor.YELLOW + "Equal";
                }
                
                player.sendMessage(ChatColor.GRAY + " • " + ChatColor.WHITE + displayName + ": " + 
                        ChatColor.GOLD + FormatUtil.formatNumber(playerValue) + ChatColor.GRAY + " vs " + 
                        ChatColor.GOLD + FormatUtil.formatNumber(targetValue) + ChatColor.GRAY + " (" + 
                        comparison + ChatColor.GRAY + ")");
            }
        });
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
                messageConfig.sendMessage(sender, "commands.invalid-number", "input", args[1]);
                return;
            }
        }
        
        // Get top players
        Map<String, Integer> topPlayers = statsService.getTopPlayers(statName, limit);
        
        if (topPlayers.isEmpty()) {
            messageConfig.sendMessage(sender, "stats.no-stats-for", "stat", statName);
            return;
        }
        
        // Get display name for the stat
        String displayName = plugin.getPluginConfig().getStatDisplayName(statName);
        if (displayName.equals(statName) && statName.contains(".")) {
            // Extract the name part after the category
            displayName = FormatUtil.capitalizeWords(statName.substring(statName.indexOf('.') + 1));
        }
        
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
        sender.sendMessage(ChatColor.GREEN + "=== PlayerStats Help ===");
        sender.sendMessage(ChatColor.YELLOW + "/stats" + ChatColor.GRAY + " - Open your statistics menu");
        sender.sendMessage(ChatColor.YELLOW + "/stats <player>" + ChatColor.GRAY + " - View another player's statistics");
        sender.sendMessage(ChatColor.YELLOW + "/stats show" + ChatColor.GRAY + " - Show your statistics in chat");
        sender.sendMessage(ChatColor.YELLOW + "/stats show <player>" + ChatColor.GRAY + " - Show another player's statistics in chat");
        sender.sendMessage(ChatColor.YELLOW + "/stats compare <player>" + ChatColor.GRAY + " - Compare your statistics with another player");
        sender.sendMessage(ChatColor.YELLOW + "/stats top <stat> [limit]" + ChatColor.GRAY + " - View top players for a statistic");
        sender.sendMessage(ChatColor.YELLOW + "/stats help" + ChatColor.GRAY + " - Show this help message");
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
                
                if (sender.hasPermission("playerstats.view.others")) {
                    // Add online player names
                    Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
                }
                
                return completions;
            }
        } else if (subCommand.equalsIgnoreCase("show") || subCommand.equalsIgnoreCase("compare")) {
            if (args.length == 1 && sender.hasPermission("playerstats.view.others")) {
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