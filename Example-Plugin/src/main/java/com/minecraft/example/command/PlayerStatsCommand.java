// ./src/main/java/com/minecraft/example/command/PlayerStatsCommand.java
package com.minecraft.example.command;

import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.utils.FormatUtil;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.ExamplePlugin;
import com.minecraft.example.model.PlayerStats;
import com.minecraft.example.service.StatsService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Command class for player statistics
 * Demonstrates Core-Utils command framework
 */
@Command(name = "playerstats", description = "View and manage player statistics", aliases = {"pstats"})
public class PlayerStatsCommand {
    
    private final ExamplePlugin plugin;
    private final StatsService statsService;
    
    /**
     * Constructor
     * @param plugin Plugin instance
     */
    public PlayerStatsCommand(ExamplePlugin plugin) {
        this.plugin = plugin;
        this.statsService = plugin.getStatsService();
    }
    
    /**
     * Default command - show own stats
     * @param sender Command sender
     * @param args Command arguments
     */
    @SubCommand(isDefault = true, description = "View your own statistics")
    public void defaultCommand(CommandSender sender, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return;
        }
        
        Player player = (Player) sender;
        
        // Get player stats
        Optional<PlayerStats> statsOptional = statsService.getPlayerStats(player.getUniqueId());
        
        if (statsOptional.isPresent()) {
            displayStats(sender, statsOptional.get());
        } else {
            sender.sendMessage(ChatColor.RED + "You don't have any statistics yet.");
        }
    }
    
    /**
     * View command - view another player's stats
     * @param sender Command sender
     * @param args Command arguments
     */
    @SubCommand(name = "view", description = "View another player's statistics", minArgs = 1)
    public void viewCommand(CommandSender sender, String[] args) {
        String playerName = args[0];
        
        // Try to get player by name
        Player targetPlayer = Bukkit.getPlayer(playerName);
        
        if (targetPlayer != null) {
            // Player is online, get by UUID
            UUID playerUuid = targetPlayer.getUniqueId();
            Optional<PlayerStats> statsOptional = statsService.getPlayerStats(playerUuid);
            
            if (statsOptional.isPresent()) {
                displayStats(sender, statsOptional.get());
            } else {
                sender.sendMessage(ChatColor.RED + "Player " + playerName + " has no statistics.");
            }
        } else {
            // Player is offline, try to get by name
            Optional<PlayerStats> statsOptional = statsService.getPlayerStatsByName(playerName);
            
            if (statsOptional.isPresent()) {
                displayStats(sender, statsOptional.get());
            } else {
                sender.sendMessage(ChatColor.RED + "Player " + playerName + " not found or has no statistics.");
            }
        }
    }
    
    /**
     * Top command - view top players for a stat
     * @param sender Command sender
     * @param args Command arguments
     */
    @SubCommand(name = "top", description = "View top players for a statistic", minArgs = 1)
    public void topCommand(CommandSender sender, String[] args) {
        String statName = args[0].toLowerCase();
        int limit = 5; // Default limit
        
        // Check if limit is specified
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
                limit = Math.max(1, Math.min(limit, 20)); // Limit between 1 and 20
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid limit. Using default of 5.");
            }
        }
        
        // Get formatted stat name for display
        String formattedStatName;
        switch (statName) {
            case "kills":
                formattedStatName = "Kills";
                break;
            case "deaths":
                formattedStatName = "Deaths";
                break;
            case "blocks_placed":
            case "blocksplaced":
                formattedStatName = "Blocks Placed";
                statName = "blocks_placed";
                break;
            case "blocks_broken":
            case "blocksbroken":
                formattedStatName = "Blocks Broken";
                statName = "blocks_broken";
                break;
            case "time_played":
            case "timeplayed":
                formattedStatName = "Time Played";
                statName = "time_played";
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid statistic. Valid options: kills, deaths, blocksPlaced, blocksBroken, timePlayed");
                return;
        }
        
        // Get top players
        List<PlayerStats> topPlayers = statsService.getTopPlayers(statName, limit);
        
        if (topPlayers.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No player statistics found.");
            return;
        }
        
        // Display top players
        sender.sendMessage(ChatColor.GREEN + "=== Top " + limit + " Players by " + formattedStatName + " ===");
        
        for (int i = 0; i < topPlayers.size(); i++) {
            PlayerStats stats = topPlayers.get(i);
            int value;
            String formattedValue;
            
            switch (statName) {
                case "kills":
                    value = stats.getKills();
                    formattedValue = String.valueOf(value);
                    break;
                case "deaths":
                    value = stats.getDeaths();
                    formattedValue = String.valueOf(value);
                    break;
                case "blocks_placed":
                    value = stats.getBlocksPlaced();
                    formattedValue = String.valueOf(value);
                    break;
                case "blocks_broken":
                    value = stats.getBlocksBroken();
                    formattedValue = String.valueOf(value);
                    break;
                case "time_played":
                    value = stats.getTimePlayed();
                    formattedValue = stats.getFormattedTimePlayed();
                    break;
                default:
                    // This should never happen due to the earlier check
                    continue;
            }
            
            sender.sendMessage(ChatColor.YELLOW + (i + 1) + ". " + 
                              ChatColor.WHITE + stats.getPlayerName() + 
                              ChatColor.GRAY + " - " + 
                              ChatColor.GOLD + formattedValue);
        }
    }
    
    /**
     * Reset command - reset player's stats
     * @param sender Command sender
     * @param args Command arguments
     */
    @SubCommand(name = "reset", description = "Reset your statistics", permission = "exampleplugin.stats.reset")
    @Permission(value = "exampleplugin.stats.reset", message = "You don't have permission to reset your statistics.")
    public void resetCommand(CommandSender sender, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return;
        }
        
        Player player = (Player) sender;
        
        // Reset player stats
        boolean success = statsService.resetStats(player.getUniqueId());
        
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Your statistics have been reset.");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to reset your statistics.");
        }
    }
    
    /**
     * Admin reset command - reset another player's stats
     * @param sender Command sender
     * @param args Command arguments
     */
    @SubCommand(name = "adminreset", description = "Reset another player's statistics", minArgs = 1, permission = "exampleplugin.stats.adminreset")
    @Permission(value = "exampleplugin.stats.adminreset", message = "You don't have permission to reset other players' statistics.")
    public void adminResetCommand(CommandSender sender, String[] args) {
        String playerName = args[0];
        
        // Try to get player by name
        Player targetPlayer = Bukkit.getPlayer(playerName);
        
        if (targetPlayer != null) {
            // Player is online, reset by UUID
            boolean success = statsService.resetStats(targetPlayer.getUniqueId());
            
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "Reset statistics for player " + playerName + ".");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to reset statistics for player " + playerName + ".");
            }
        } else {
            // Player is offline, try to reset by name
            Optional<PlayerStats> statsOptional = statsService.getPlayerStatsByName(playerName);
            
            if (statsOptional.isPresent()) {
                PlayerStats stats = statsOptional.get();
                boolean success = statsService.resetStats(stats.getPlayerUuid());
                
                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "Reset statistics for player " + playerName + ".");
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to reset statistics for player " + playerName + ".");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Player " + playerName + " not found or has no statistics.");
            }
        }
    }
    
    /**
     * Display a player's statistics
     * @param sender Command sender
     * @param stats PlayerStats to display
     */
    private void displayStats(CommandSender sender, PlayerStats stats) {
        sender.sendMessage(ChatColor.GREEN + "=== Statistics for " + stats.getPlayerName() + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Kills: " + ChatColor.WHITE + stats.getKills());
        sender.sendMessage(ChatColor.YELLOW + "Deaths: " + ChatColor.WHITE + stats.getDeaths());
        sender.sendMessage(ChatColor.YELLOW + "K/D Ratio: " + ChatColor.WHITE + FormatUtil.formatNumber(stats.getKdRatio()));
        sender.sendMessage(ChatColor.YELLOW + "Blocks Placed: " + ChatColor.WHITE + stats.getBlocksPlaced());
        sender.sendMessage(ChatColor.YELLOW + "Blocks Broken: " + ChatColor.WHITE + stats.getBlocksBroken());
        sender.sendMessage(ChatColor.YELLOW + "Time Played: " + ChatColor.WHITE + stats.getFormattedTimePlayed());
        sender.sendMessage(ChatColor.YELLOW + "Last Seen: " + ChatColor.WHITE + stats.getLastSeen());
    }
}