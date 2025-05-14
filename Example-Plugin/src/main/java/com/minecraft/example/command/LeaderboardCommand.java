// ./Example-Plugin/src/main/java/com/minecraft/example/command/LeaderboardCommand.java
package com.minecraft.example.command;

import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.command.TabCompletionProvider;
import com.minecraft.core.utils.FormatUtil;
import com.minecraft.example.PlayerStatsPlugin;
import com.minecraft.example.api.LeaderboardService;
import com.minecraft.example.config.MessageConfig;
import com.minecraft.example.data.Leaderboard;
import com.minecraft.example.data.LeaderboardEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command for managing leaderboards.
 */
@Command(name = "leaderboard", description = "View and manage leaderboards", aliases = {"top", "lb"})
public class LeaderboardCommand implements TabCompletionProvider {
    
    private final PlayerStatsPlugin plugin;
    private final LeaderboardService leaderboardService;
    private final MessageConfig messageConfig;
    
    /**
     * Creates a new LeaderboardCommand instance.
     *
     * @param plugin The plugin instance
     * @param leaderboardService The leaderboard service
     */
    public LeaderboardCommand(PlayerStatsPlugin plugin, LeaderboardService leaderboardService) {
        this.plugin = plugin;
        this.leaderboardService = leaderboardService;
        this.messageConfig = plugin.getMessageConfig();
    }
    
    /**
     * Default command handler. Shows all available leaderboards.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "", description = "List all available leaderboards")
    public void defaultCommand(CommandSender sender, String[] args) {
        // Default to listing leaderboards
        listCommand(sender, args);
    }
    
    /**
     * Lists all available leaderboards.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "list", description = "List all available leaderboards")
    public void listCommand(CommandSender sender, String[] args) {
        // Get all leaderboards
        List<Leaderboard> leaderboards = leaderboardService.getAllLeaderboards();
        
        if (leaderboards.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No leaderboards found.");
            return;
        }
        
        // Display leaderboards
        sender.sendMessage(ChatColor.GREEN + "=== Available Leaderboards ===");
        
        // Group by category
        leaderboards.stream()
                .collect(Collectors.groupingBy(Leaderboard::getCategory))
                .forEach((category, categoryLeaderboards) -> {
                    sender.sendMessage(ChatColor.YELLOW + FormatUtil.capitalizeWords(category) + ":");
                    
                    for (Leaderboard leaderboard : categoryLeaderboards) {
                        sender.sendMessage(ChatColor.GRAY + " • " + ChatColor.WHITE + leaderboard.getDisplayName() +
                                ChatColor.GRAY + " (/leaderboard view " + leaderboard.getId() + ")");
                    }
                });
    }
    
    /**
     * Views a specific leaderboard.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "view", description = "View a specific leaderboard", minArgs = 1)
    public void viewCommand(CommandSender sender, String[] args) {
        String id = args[0];
        
        // Get leaderboard
        Leaderboard leaderboard = leaderboardService.getLeaderboard(id);
        if (leaderboard == null) {
            messageConfig.sendMessage(sender, "leaderboard.no-entries", "name", id);
            return;
        }
        
        // Get limit
        int limit = 10;
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
                limit = Math.max(1, Math.min(100, limit)); // Limit between 1 and 100
            } catch (NumberFormatException e) {
                messageConfig.sendMessage(sender, "commands.invalid-number", "input", args[1]);
                return;
            }
        }
        
        // Get leaderboard entries
        List<LeaderboardEntry> entries = leaderboardService.getLeaderboardEntries(id, limit);
        
        if (entries.isEmpty()) {
            messageConfig.sendMessage(sender, "leaderboard.no-entries", "name", leaderboard.getDisplayName());
            return;
        }
        
        // Display leaderboard
        sender.sendMessage(messageConfig.getMessage("leaderboard.header")
                .replace("{limit}", String.valueOf(limit))
                .replace("{name}", leaderboard.getDisplayName()));
        
        // Display entries
        for (LeaderboardEntry entry : entries) {
            String message = messageConfig.getMessage("leaderboard.entry")
                    .replace("{rank}", String.valueOf(entry.getRank()))
                    .replace("{player}", entry.getPlayerName())
                    .replace("{value}", FormatUtil.formatNumber(entry.getScore()));
            
            sender.sendMessage(message);
        }
        
        // Display player's rank
        if (sender instanceof Player) {
            Player player = (Player) sender;
            int rank = leaderboardService.getPlayerRank(player, id);
            
            if (rank > 0) {
                int score = plugin.getStatsService().getStat(player, leaderboard.getStatName());
                
                String message = messageConfig.getMessage("leaderboard.player-rank")
                        .replace("{rank}", String.valueOf(rank))
                        .replace("{value}", FormatUtil.formatNumber(score));
                
                sender.sendMessage(message);
            } else {
                sender.sendMessage(messageConfig.getMessage("leaderboard.not-ranked"));
            }
        }
    }
    
    /**
     * Creates a new leaderboard.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "create", description = "Create a new leaderboard", minArgs = 3)
    @Permission(value = "playerstats.leaderboard.manage", message = "You don't have permission to manage leaderboards.")
    public void createCommand(CommandSender sender, String[] args) {
        String id = args[0];
        String displayName = args[1];
        String statName = args[2];
        
        // Create leaderboard
        boolean created = leaderboardService.createLeaderboard(id, displayName, statName);
        
        if (created) {
            messageConfig.sendMessage(sender, "leaderboard.created", "name", displayName);
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to create leaderboard. It may already exist.");
        }
    }
    
    /**
     * Deletes a leaderboard.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "delete", description = "Delete a leaderboard", minArgs = 1)
    @Permission(value = "playerstats.leaderboard.manage", message = "You don't have permission to manage leaderboards.")
    public void deleteCommand(CommandSender sender, String[] args) {
        String id = args[0];
        
        // Get leaderboard
        Leaderboard leaderboard = leaderboardService.getLeaderboard(id);
        if (leaderboard == null) {
            sender.sendMessage(ChatColor.RED + "Leaderboard not found: " + id);
            return;
        }
        
        // Delete leaderboard
        boolean deleted = leaderboardService.deleteLeaderboard(id);
        
        if (deleted) {
            messageConfig.sendMessage(sender, "leaderboard.deleted", "name", leaderboard.getDisplayName());
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to delete leaderboard.");
        }
    }
    
    /**
     * Updates a leaderboard.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "update", description = "Update a leaderboard", minArgs = 1)
    @Permission(value = "playerstats.leaderboard.manage", message = "You don't have permission to manage leaderboards.")
    public void updateCommand(CommandSender sender, String[] args) {
        String id = args[0];
        
        // Get leaderboard
        Leaderboard leaderboard = leaderboardService.getLeaderboard(id);
        if (leaderboard == null) {
            sender.sendMessage(ChatColor.RED + "Leaderboard not found: " + id);
            return;
        }
        
        // Update leaderboard
        boolean updated = leaderboardService.updateLeaderboard(id);
        
        if (updated) {
            messageConfig.sendMessage(sender, "leaderboard.updated", "name", leaderboard.getDisplayName());
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to update leaderboard.");
        }
    }
    
    /**
     * Updates all leaderboards.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "updateall", description = "Update all leaderboards")
    @Permission(value = "playerstats.leaderboard.manage", message = "You don't have permission to manage leaderboards.")
    public void updateAllCommand(CommandSender sender, String[] args) {
        // Update all leaderboards
        leaderboardService.updateAllLeaderboards();
        
        sender.sendMessage(ChatColor.GREEN + "All leaderboards have been updated.");
    }
    
    /**
     * Sets the update interval for a leaderboard.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "interval", description = "Set update interval for a leaderboard", minArgs = 2)
    @Permission(value = "playerstats.leaderboard.manage", message = "You don't have permission to manage leaderboards.")
    public void intervalCommand(CommandSender sender, String[] args) {
        String id = args[0];
        
        // Get leaderboard
        Leaderboard leaderboard = leaderboardService.getLeaderboard(id);
        if (leaderboard == null) {
            sender.sendMessage(ChatColor.RED + "Leaderboard not found: " + id);
            return;
        }
        
        // Parse interval
        int interval;
        try {
            interval = Integer.parseInt(args[1]);
            if (interval < 1) {
                sender.sendMessage(ChatColor.RED + "Interval must be at least 1 minute.");
                return;
            }
        } catch (NumberFormatException e) {
            messageConfig.sendMessage(sender, "commands.invalid-number", "input", args[1]);
            return;
        }
        
        // Set interval
        boolean scheduled = leaderboardService.scheduleLeaderboardUpdates(id, interval);
        
        if (scheduled) {
            sender.sendMessage(ChatColor.GREEN + "Update interval for " + 
                    leaderboard.getDisplayName() + " set to " + interval + " minutes.");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to set update interval.");
        }
    }
    
    /**
     * Shows detailed help for the leaderboard command.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "help", description = "Show help information")
    public void helpCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "=== Leaderboard Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/leaderboard list" + ChatColor.GRAY + " - List all available leaderboards");
        sender.sendMessage(ChatColor.YELLOW + "/leaderboard view <id> [limit]" + ChatColor.GRAY + " - View a specific leaderboard");
        
        if (sender.hasPermission("playerstats.leaderboard.manage")) {
            sender.sendMessage(ChatColor.YELLOW + "/leaderboard create <id> <displayName> <statName>" + 
                    ChatColor.GRAY + " - Create a new leaderboard");
            sender.sendMessage(ChatColor.YELLOW + "/leaderboard delete <id>" + ChatColor.GRAY + " - Delete a leaderboard");
            sender.sendMessage(ChatColor.YELLOW + "/leaderboard update <id>" + ChatColor.GRAY + " - Update a leaderboard");
            sender.sendMessage(ChatColor.YELLOW + "/leaderboard updateall" + ChatColor.GRAY + " - Update all leaderboards");
            sender.sendMessage(ChatColor.YELLOW + "/leaderboard interval <id> <minutes>" + 
                    ChatColor.GRAY + " - Set update interval for a leaderboard");
        }
        
        sender.sendMessage(ChatColor.YELLOW + "/leaderboard help" + ChatColor.GRAY + " - Show this help message");
    }
    
    @Override
    public List<String> getTabCompletions(String subCommand, CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (subCommand.isEmpty()) {
            if (args.length == 1) {
                // Tab complete subcommands
                completions.add("list");
                completions.add("view");
                completions.add("help");
                
                if (sender.hasPermission("playerstats.leaderboard.manage")) {
                    completions.add("create");
                    completions.add("delete");
                    completions.add("update");
                    completions.add("updateall");
                    completions.add("interval");
                }
                
                return completions;
            }
        } else if (subCommand.equalsIgnoreCase("view") || 
                   subCommand.equalsIgnoreCase("delete") || 
                   subCommand.equalsIgnoreCase("update") ||
                   subCommand.equalsIgnoreCase("interval")) {
            if (args.length == 1) {
                // Tab complete leaderboard IDs
                List<Leaderboard> leaderboards = leaderboardService.getAllLeaderboards();
                return leaderboards.stream()
                        .map(Leaderboard::getId)
                        .collect(Collectors.toList());
            } else if (args.length == 2 && subCommand.equalsIgnoreCase("view")) {
                // Tab complete limits
                return Arrays.asList("5", "10", "20", "50");
            } else if (args.length == 2 && subCommand.equalsIgnoreCase("interval")) {
                // Tab complete intervals
                return Arrays.asList("5", "10", "30", "60");
            }
        } else if (subCommand.equalsIgnoreCase("create") && args.length == 3) {
            // Tab complete stat names
            return Arrays.asList(
                    "blocks.broken", 
                    "blocks.placed", 
                    "combat.kills", 
                    "combat.deaths",
                    "items.crafted", 
                    "items.consumed", 
                    "movement.distance",
                    "sessions.playtime"
            );
        }
        
        return completions;
    }
}