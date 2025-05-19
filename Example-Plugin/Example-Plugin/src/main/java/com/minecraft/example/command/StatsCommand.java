// ./Example-Plugin/src/main/java/com/minecraft/example/command/StatsCommand.java
package com.minecraft.example.command;

import com.minecraft.core.command.TabCompletionProvider;
import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.utils.FormatUtil;
import com.minecraft.core.utils.TimeUtil;
import com.minecraft.example.ExamplePlugin;
import com.minecraft.example.model.PlayerStats;
import com.minecraft.example.service.StatsService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Command for viewing player statistics
 * Demonstrates Core-Utils command framework
 */
@Command(name = "stats", description = "View player statistics", aliases = {"playerstats"})
public class StatsCommand implements TabCompletionProvider {
    
    private final ExamplePlugin plugin;
    private final StatsService statsService;
    
    public StatsCommand(ExamplePlugin plugin) {
        this.plugin = plugin;
        this.statsService = plugin.getStatsService();
    }
    
    /**
     * Default command - view your own stats or someone else's stats
     */
    @SubCommand(isDefault = true, description = "View your stats or another player's stats", maxArgs = 1)
    public void viewStats(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // View own stats (sender must be a player)
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player name.");
                return;
            }
            
            Player player = (Player) sender;
            displayStats(sender, player.getUniqueId().toString(), player.getName());
        } else {
            // View another player's stats
            String targetName = args[0];
            
            // Find player by name (could be offline)
            Optional<PlayerStats> statsOpt = statsService.getPlayerStatsByName(targetName);
            
            if (statsOpt.isPresent()) {
                PlayerStats stats = statsOpt.get();
                displayStats(sender, stats.getUuid().toString(), stats.getName());
            } else {
                sender.sendMessage(ChatColor.RED + "No stats found for player: " + targetName);
            }
        }
    }
    
    /**
     * View top players for a specific stat
     */
    @SubCommand(name = "top", description = "View top players by stat", minArgs = 1, maxArgs = 2)
    public void viewTopPlayers(CommandSender sender, String[] args) {
        String statType = args[0].toLowerCase();
        int limit = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        
        // Map user-friendly stat names to database field names
        String statField;
        String statLabel;
        
        switch (statType) {
            case "playtime":
                statField = "playtime_seconds";
                statLabel = "Playtime";
                break;
            case "logins":
                statField = "login_count";
                statLabel = "Logins";
                break;
            case "broken":
            case "blocks_broken":
                statField = "blocks_broken";
                statLabel = "Blocks Broken";
                break;
            case "placed":
            case "blocks_placed":
                statField = "blocks_placed";
                statLabel = "Blocks Placed";
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown stat type. Use: playtime, logins, broken, placed");
                return;
        }
        
        // Limit to reasonable range
        if (limit < 1) limit = 1;
        if (limit > 20) limit = 20;
        
        // Get top players
        List<PlayerStats> topPlayers = statsService.getTopPlayers(statField, limit);
        
        if (topPlayers.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No player statistics found.");
            return;
        }
        
        // Display results
        sender.sendMessage(ChatColor.GREEN + "=== Top Players by " + statLabel + " ===");
        
        for (int i = 0; i < topPlayers.size(); i++) {
            PlayerStats stats = topPlayers.get(i);
            String value;
            
            // Format the stat value based on stat type
            if (statField.equals("playtime_seconds")) {
                value = stats.getFormattedPlaytime();
            } else {
                value = FormatUtil.formatNumber(
                    statField.equals("login_count") ? stats.getLoginCount() :
                    statField.equals("blocks_broken") ? stats.getBlocksBroken() :
                    stats.getBlocksPlaced()
                );
            }
            
            sender.sendMessage(ChatColor.GOLD + (i + 1) + ". " + ChatColor.WHITE + stats.getName() + 
                              ChatColor.GRAY + " - " + ChatColor.YELLOW + value);
        }
    }
    
    /**
     * Private helper method to display a player's stats
     */
    private void displayStats(CommandSender sender, String uuid, String playerName) {
        // Using CompletableFuture for async database access
        statsService.getPlayerStatsAsync(java.util.UUID.fromString(uuid))
            .thenAccept(statsOpt -> {
                if (!statsOpt.isPresent()) {
                    // Run on the server thread for Bukkit API
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.RED + "No stats found for player: " + playerName);
                    });
                    return;
                }
                
                PlayerStats stats = statsOpt.get();
                
                // Run on the server thread for Bukkit API
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.GREEN + "=== Stats for " + playerName + " ===");
                    sender.sendMessage(ChatColor.GOLD + "First join: " + ChatColor.WHITE + 
                                      TimeUtil.formatDateTime(stats.getFirstJoin().toInstant()));
                    sender.sendMessage(ChatColor.GOLD + "Last join: " + ChatColor.WHITE + 
                                      TimeUtil.formatDateTime(stats.getLastJoin().toInstant()));
                    sender.sendMessage(ChatColor.GOLD + "Playtime: " + ChatColor.WHITE + 
                                      stats.getFormattedPlaytime());
                    sender.sendMessage(ChatColor.GOLD + "Logins: " + ChatColor.WHITE + 
                                      stats.getLoginCount());
                    sender.sendMessage(ChatColor.GOLD + "Blocks broken: " + ChatColor.WHITE + 
                                      FormatUtil.formatNumber(stats.getBlocksBroken()));
                    sender.sendMessage(ChatColor.GOLD + "Blocks placed: " + ChatColor.WHITE + 
                                      FormatUtil.formatNumber(stats.getBlocksPlaced()));
                });
            })
            .exceptionally(ex -> {
                // Handle exceptions
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "Error retrieving stats: " + ex.getMessage());
                });
                return null;
            });
    }
    
    @Override
    public List<String> getTabCompletions(String subCommand, CommandSender sender, String[] args) {
        // Tab completions for different subcommands
        if (subCommand.isEmpty() && args.length == 1) {
            // Complete player names for the default command
            String partialName = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partialName))
                .collect(Collectors.toList());
        } else if (subCommand.equalsIgnoreCase("top")) {
            if (args.length == 1) {
                // Complete stat types
                String partial = args[0].toLowerCase();
                List<String> statTypes = Arrays.asList("playtime", "logins", "broken", "placed");
                return statTypes.stream()
                    .filter(type -> type.startsWith(partial))
                    .collect(Collectors.toList());
            } else if (args.length == 2) {
                // Suggest some common limits
                String partial = args[1];
                List<String> limits = Arrays.asList("5", "10", "15", "20");
                return limits.stream()
                    .filter(limit -> limit.startsWith(partial))
                    .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }
}