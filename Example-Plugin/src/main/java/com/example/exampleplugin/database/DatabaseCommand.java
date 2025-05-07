package com.example.exampleplugin;

import com.example.exampleplugin.database.DatabaseManager;
import com.example.exampleplugin.database.PlayerData;
import com.minecraft.core.command.TabCompletionProvider;
import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.utils.LogUtil;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Database command class that demonstrates how to use the
 * SQL-Bridge plugin functionalities
 */
@Command(name = "exampledb", description = "Example plugin database commands", aliases = {"exdb"})
@Permission("example.db")
public class DatabaseCommand implements TabCompletionProvider {
    private final ExamplePlugin plugin;
    private final DatabaseManager databaseManager;
    
    /**
     * Create a new database command
     * 
     * @param plugin The plugin instance
     * @param databaseManager The database manager
     */
    public DatabaseCommand(ExamplePlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    /**
     * Help command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "help", description = "Show help information", aliases = {"?"})
    public void helpCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "=== Example Database Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/exampledb help" + ChatColor.WHITE + " - Show this help page");
        sender.sendMessage(ChatColor.YELLOW + "/exampledb player <name> [level]" + ChatColor.WHITE + " - View or set player level");
        sender.sendMessage(ChatColor.YELLOW + "/exampledb stat <player> <name> [value]" + ChatColor.WHITE + " - View or set player stat");
        sender.sendMessage(ChatColor.YELLOW + "/exampledb top [limit]" + ChatColor.WHITE + " - Show top players by level");
        sender.sendMessage(ChatColor.YELLOW + "/exampledb transaction <player> <levelInc> <statName> <statValue>" + 
                          ChatColor.WHITE + " - Test transaction functionality");
        sender.sendMessage(ChatColor.YELLOW + "/exampledb metrics" + ChatColor.WHITE + " - Show database metrics");
    }
    
    /**
     * Player command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "player", description = "View or set player level", minArgs = 1, maxArgs = 2)
    public void playerCommand(CommandSender sender, String[] args) {
        String playerName = args[0];
        
        // Check if we're setting the level
        if (args.length == 2) {
            try {
                int level = Integer.parseInt(args[1]);
                
                // Save player data
                boolean success = databaseManager.savePlayerData(playerName, level);
                
                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "Set player " + playerName + " level to " + level);
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to set player level");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid level. Please enter a number.");
            }
        } else {
            // Get player data
            PlayerData playerData = databaseManager.getPlayerData(playerName);
            
            if (playerData != null) {
                sender.sendMessage(ChatColor.GREEN + "Player: " + playerData.getName());
                sender.sendMessage(ChatColor.GREEN + "Level: " + playerData.getLevel());
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Player not found in database");
            }
        }
    }
    
    /**
     * Stat command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "stat", description = "View or set player stat", minArgs = 2, maxArgs = 3)
    public void statCommand(CommandSender sender, String[] args) {
        String playerName = args[0];
        String statName = args[1];
        
        // Check if we're setting the stat
        if (args.length == 3) {
            try {
                int statValue = Integer.parseInt(args[2]);
                
                // Save player stat
                boolean success = databaseManager.setPlayerStat(playerName, statName, statValue);
                
                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "Set player " + playerName + " stat " + 
                                      statName + " to " + statValue);
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to set player stat");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid value. Please enter a number.");
            }
        } else {
            // Get player stats
            Map<String, Integer> stats = databaseManager.getPlayerStats(playerName);
            
            if (stats.containsKey(statName)) {
                sender.sendMessage(ChatColor.GREEN + "Player: " + playerName);
                sender.sendMessage(ChatColor.GREEN + "Stat " + statName + ": " + stats.get(statName));
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Stat not found for player");
            }
        }
    }
    
    /**
     * Top command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "top", description = "Show top players by level", maxArgs = 1)
    public void topCommand(CommandSender sender, String[] args) {
        int limit = 5; // Default limit
        
        // Check if limit is specified
        if (args.length == 1) {
            try {
                limit = Integer.parseInt(args[0]);
                if (limit <= 0) {
                    limit = 5;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid limit. Using default.");
            }
        }
        
        // Get top players
        List<PlayerData> topPlayers = databaseManager.getTopPlayers(limit);
        
        if (topPlayers.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No players found in database");
        } else {
            sender.sendMessage(ChatColor.GREEN + "=== Top " + limit + " Players ===");
            
            int rank = 1;
            for (PlayerData player : topPlayers) {
                sender.sendMessage(ChatColor.YELLOW + "#" + rank + ": " + 
                                  ChatColor.WHITE + player.getName() + 
                                  ChatColor.GRAY + " (Level " + player.getLevel() + ")");
                rank++;
            }
        }
    }
    
    /**
     * Transaction command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "transaction", description = "Test transaction functionality", minArgs = 4)
    public void transactionCommand(CommandSender sender, String[] args) {
        String playerName = args[0];
        
        try {
            int levelIncrease = Integer.parseInt(args[1]);
            String statName = args[2];
            int statValue = Integer.parseInt(args[3]);
            
            // Create stats map
            Map<String, Integer> stats = new HashMap<>();
            stats.put(statName, statValue);
            
            // Execute transaction
            boolean success = databaseManager.executeTransaction(playerName, levelIncrease, stats);
            
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "Transaction completed successfully");
                sender.sendMessage(ChatColor.GREEN + "Increased " + playerName + "'s level by " + levelIncrease);
                sender.sendMessage(ChatColor.GREEN + "Set stat " + statName + " to " + statValue);
            } else {
                sender.sendMessage(ChatColor.RED + "Transaction failed");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format. Usage: /exampledb transaction <player> <levelInc> <statName> <statValue>");
        }
    }
    
    /**
     * Metrics command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "metrics", description = "Show database metrics")
    public void metricsCommand(CommandSender sender, String[] args) {
        // Get database metrics
        List<String> metrics = databaseManager.getDatabaseMetrics();
        
        sender.sendMessage(ChatColor.GREEN + "=== Database Metrics ===");
        
        for (String metric : metrics) {
            sender.sendMessage(ChatColor.YELLOW + metric);
        }
    }
    
    @Override
    public List<String> getTabCompletions(String subCommand, CommandSender sender, String[] args) {
        // Tab completions for the player and stat commands
        if (subCommand.equalsIgnoreCase("player") || subCommand.equalsIgnoreCase("stat")) {
            if (args.length == 1) {
                // Return online player names
                List<String> playerNames = new ArrayList<>();
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
                return playerNames;
            }
        }
        
        // Tab completions for the stat command
        if (subCommand.equalsIgnoreCase("stat")) {
            if (args.length == 2) {
                // Return example stat names
                return Arrays.asList("kills", "deaths", "wins", "losses", "playtime");
            }
        }
        
        return new ArrayList<>();
    }
}