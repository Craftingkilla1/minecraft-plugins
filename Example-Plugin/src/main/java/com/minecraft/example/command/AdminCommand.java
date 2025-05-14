// ./Example-Plugin/src/main/java/com/minecraft/example/command/AdminCommand.java
package com.minecraft.example.command;

import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.command.TabCompletionProvider;
import com.minecraft.core.utils.FormatUtil;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.PlayerStatsPlugin;
import com.minecraft.example.api.AchievementService;
import com.minecraft.example.api.LeaderboardService;
import com.minecraft.example.api.StatsService;
import com.minecraft.example.config.MessageConfig;
import com.minecraft.example.data.Achievement;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Administrative command for the PlayerStats plugin.
 */
@Command(name = "admin", description = "Administrative commands for PlayerStats")
@Permission(value = "playerstats.admin", message = "You don't have permission to use admin commands.")
public class AdminCommand implements TabCompletionProvider {
    
    private final PlayerStatsPlugin plugin;
    private final StatsService statsService;
    private final AchievementService achievementService;
    private final LeaderboardService leaderboardService;
    private final MessageConfig messageConfig;
    
    /**
     * Creates a new AdminCommand instance.
     *
     * @param plugin The plugin instance
     * @param statsService The stats service
     * @param achievementService The achievement service
     * @param leaderboardService The leaderboard service
     */
    public AdminCommand(PlayerStatsPlugin plugin, StatsService statsService, 
            AchievementService achievementService, LeaderboardService leaderboardService) {
        this.plugin = plugin;
        this.statsService = statsService;
        this.achievementService = achievementService;
        this.leaderboardService = leaderboardService;
        this.messageConfig = plugin.getMessageConfig();
    }
    
    /**
     * Default command handler. Shows help information.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "", description = "Show admin help")
    public void defaultCommand(CommandSender sender, String[] args) {
        helpCommand(sender, args);
    }
    
    /**
     * Reloads the plugin configuration.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "reload", description = "Reload the plugin configuration")
    public void reloadCommand(CommandSender sender, String[] args) {
        // Reload configuration
        plugin.reloadConfig();
        plugin.getPluginConfig().loadConfig();
        plugin.getMessageConfig().loadMessages();
        
        // Reload achievements
        achievementService.loadAchievements();
        
        // Reload leaderboards
        leaderboardService.loadLeaderboards();
        
        messageConfig.sendMessage(sender, "plugin-reload");
    }
    
    /**
     * Resets player statistics.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "reset", description = "Reset player statistics", minArgs = 1)
    @Permission(value = "playerstats.reset", message = "You don't have permission to reset statistics.")
    public void resetCommand(CommandSender sender, String[] args) {
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            messageConfig.sendMessage(sender, "commands.player-not-found", "player", targetName);
            return;
        }
        
        if (args.length == 1) {
            // Reset all stats
            boolean reset = statsService.resetAllStats(target);
            
            if (reset) {
                messageConfig.sendMessage(sender, "stats.reset", "player", target.getName());
                
                // Notify the target
                messageConfig.sendMessage(target, "stats.reset", "player", "your");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to reset statistics for " + target.getName());
            }
        } else {
            // Reset specific stat
            String statName = args[1];
            boolean reset = statsService.resetStat(target, statName);
            
            if (reset) {
                messageConfig.sendMessage(sender, "stats.reset-specific", 
                        "player", target.getName(), "stat", statName);
                
                // Notify the target
                messageConfig.sendMessage(target, "stats.reset-specific", 
                        "player", "your", "stat", statName);
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to reset statistic " + statName + 
                        " for " + target.getName());
            }
        }
    }
    
    /**
     * Modifies player statistics.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "modify", description = "Modify player statistics", minArgs = 3)
    @Permission(value = "playerstats.modify", message = "You don't have permission to modify statistics.")
    public void modifyCommand(CommandSender sender, String[] args) {
        String targetName = args[0];
        String statName = args[1];
        
        // Parse value
        int value;
        try {
            value = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            messageConfig.sendMessage(sender, "commands.invalid-number", "input", args[2]);
            return;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            messageConfig.sendMessage(sender, "commands.player-not-found", "player", targetName);
            return;
        }
        
        // Set statistic
        statsService.setStat(target, statName, value);
        
        // Notify sender
        messageConfig.sendMessage(sender, "stats.modified", 
                "player", target.getName(), "stat", statName, "value", String.valueOf(value));
        
        // Notify target
        messageConfig.sendMessage(target, "stats.modified", 
                "player", "your", "stat", statName, "value", String.valueOf(value));
        
        // Check achievements
        achievementService.checkAchievements(target);
    }
    
    /**
     * Manages achievements.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "achievement", description = "Manage achievements", minArgs = 1)
    public void achievementCommand(CommandSender sender, String[] args) {
        String action = args[0].toLowerCase();
        
        if (action.equals("list")) {
            // List achievements
            listAchievements(sender, args.length > 1 ? args[1] : null);
        } else if (action.equals("info") && args.length > 1) {
            // Show achievement info
            showAchievementInfo(sender, args[1]);
        } else if (action.equals("award") && args.length > 2) {
            // Award achievement
            awardAchievement(sender, args[1], args[2]);
        } else if (action.equals("reset") && args.length > 1) {
            // Reset achievements
            resetAchievements(sender, args[1]);
        } else if (action.equals("reload")) {
            // Reload achievements
            achievementService.loadAchievements();
            sender.sendMessage(ChatColor.GREEN + "Achievements reloaded.");
        } else {
            // Show achievement help
            sender.sendMessage(ChatColor.GREEN + "=== Achievement Commands ===");
            sender.sendMessage(ChatColor.YELLOW + "/admin achievement list [category]" + 
                    ChatColor.GRAY + " - List all achievements");
            sender.sendMessage(ChatColor.YELLOW + "/admin achievement info <id>" + 
                    ChatColor.GRAY + " - Show achievement info");
            sender.sendMessage(ChatColor.YELLOW + "/admin achievement award <player> <id>" + 
                    ChatColor.GRAY + " - Award an achievement to a player");
            sender.sendMessage(ChatColor.YELLOW + "/admin achievement reset <player>" + 
                    ChatColor.GRAY + " - Reset a player's achievements");
            sender.sendMessage(ChatColor.YELLOW + "/admin achievement reload" + 
                    ChatColor.GRAY + " - Reload achievements from configuration");
        }
    }
    
    /**
     * Lists achievements.
     *
     * @param sender The command sender
     * @param category The category to filter by, or null for all
     */
    private void listAchievements(CommandSender sender, String category) {
        List<Achievement> achievements = achievementService.getAllAchievements();
        
        if (category != null) {
            // Filter by category
            achievements = achievements.stream()
                    .filter(a -> category.equalsIgnoreCase(a.getCategory()))
                    .collect(Collectors.toList());
        }
        
        if (achievements.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No achievements found" + 
                    (category != null ? " in category " + category : "") + ".");
            return;
        }
        
        // Group by category
        Map<String, List<Achievement>> byCategory = achievements.stream()
                .collect(Collectors.groupingBy(Achievement::getCategory));
        
        sender.sendMessage(ChatColor.GREEN + "=== Achievements ===");
        
        byCategory.forEach((cat, catAchievements) -> {
            sender.sendMessage(ChatColor.YELLOW + FormatUtil.capitalizeWords(cat) + ":");
            
            for (Achievement achievement : catAchievements) {
                // Don't show details of secret achievements
                if (achievement.isSecret()) {
                    sender.sendMessage(ChatColor.GRAY + " • " + ChatColor.RED + "[Secret] " + 
                            ChatColor.WHITE + achievement.getName() + ChatColor.GRAY + " (" + 
                            achievement.getId() + ")");
                } else {
                    sender.sendMessage(ChatColor.GRAY + " • " + ChatColor.WHITE + achievement.getName() + 
                            ChatColor.GRAY + " (" + achievement.getId() + ")");
                }
            }
        });
    }
    
    /**
     * Shows detailed information about an achievement.
     *
     * @param sender The command sender
     * @param id The achievement ID
     */
    private void showAchievementInfo(CommandSender sender, String id) {
        Achievement achievement = achievementService.getAchievement(id);
        
        if (achievement == null) {
            sender.sendMessage(ChatColor.RED + "Achievement not found: " + id);
            return;
        }
        
        sender.sendMessage(ChatColor.GREEN + "=== Achievement: " + achievement.getName() + " ===");
        sender.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + achievement.getId());
        sender.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.WHITE + achievement.getName());
        sender.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + achievement.getDescription());
        sender.sendMessage(ChatColor.YELLOW + "Category: " + ChatColor.WHITE + achievement.getCategory());
        sender.sendMessage(ChatColor.YELLOW + "Icon: " + ChatColor.WHITE + achievement.getIcon().name());
        sender.sendMessage(ChatColor.YELLOW + "Secret: " + ChatColor.WHITE + achievement.isSecret());
        
        // Show criteria
        sender.sendMessage(ChatColor.YELLOW + "Criteria:");
        for (Map.Entry<String, Integer> entry : achievement.getCriteria().entrySet()) {
            sender.sendMessage(ChatColor.GRAY + " • " + ChatColor.WHITE + entry.getKey() + 
                    ": " + ChatColor.GOLD + entry.getValue());
        }
    }
    
    /**
     * Awards an achievement to a player.
     *
     * @param sender The command sender
     * @param playerName The player name
     * @param achievementId The achievement ID
     */
    private void awardAchievement(CommandSender sender, String playerName, String achievementId) {
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            messageConfig.sendMessage(sender, "commands.player-not-found", "player", playerName);
            return;
        }
        
        Achievement achievement = achievementService.getAchievement(achievementId);
        
        if (achievement == null) {
            sender.sendMessage(ChatColor.RED + "Achievement not found: " + achievementId);
            return;
        }
        
        // Check if player already has the achievement
        if (achievementService.hasAchievement(target, achievementId)) {
            sender.sendMessage(ChatColor.RED + target.getName() + " already has the achievement " + 
                    achievement.getName());
            return;
        }
        
        // Award achievement
        boolean awarded = achievementService.awardAchievement(target, achievementId);
        
        if (awarded) {
            sender.sendMessage(ChatColor.GREEN + "Awarded achievement " + achievement.getName() + 
                    " to " + target.getName());
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to award achievement " + achievement.getName() + 
                    " to " + target.getName());
        }
    }
    
    /**
     * Resets a player's achievements.
     *
     * @param sender The command sender
     * @param playerName The player name
     */
    private void resetAchievements(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            messageConfig.sendMessage(sender, "commands.player-not-found", "player", playerName);
            return;
        }
        
        // Reset achievements
        boolean reset = achievementService.resetAchievements(target);
        
        if (reset) {
            messageConfig.sendMessage(sender, "achievements.reset", "player", target.getName());
            
            // Notify the target
            messageConfig.sendMessage(target, "achievements.reset", "player", "your");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to reset achievements for " + target.getName());
        }
    }
    
    /**
     * Performs database maintenance tasks.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "maintenance", description = "Perform database maintenance tasks")
    public void maintenanceCommand(CommandSender sender, String[] args) {
        // Perform maintenance
        sender.sendMessage(ChatColor.YELLOW + "Starting database maintenance...");
        
        // Run on async thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Test database connection
            boolean success = plugin.getDatabaseManager().testConnection()
                    .exceptionally(e -> {
                        LogUtil.severe("Database connection test failed: " + e.getMessage());
                        return false;
                    })
                    .join();
            
            if (success) {
                // Update all leaderboards
                leaderboardService.updateAllLeaderboards();
                
                // Run on main thread to notify sender
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.GREEN + "Database maintenance completed successfully.");
                    sender.sendMessage(ChatColor.GREEN + "All leaderboards have been updated.");
                });
            } else {
                // Run on main thread to notify sender
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "Database maintenance failed. " + 
                            "Please check server logs for details.");
                });
            }
        });
    }
    
    /**
     * Shows detailed help for the admin command.
     *
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "help", description = "Show help information")
    public void helpCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "=== PlayerStats Admin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/admin reload" + ChatColor.GRAY + " - Reload the plugin configuration");
        sender.sendMessage(ChatColor.YELLOW + "/admin reset <player> [stat]" + 
                ChatColor.GRAY + " - Reset player statistics");
        sender.sendMessage(ChatColor.YELLOW + "/admin modify <player> <stat> <value>" + 
                ChatColor.GRAY + " - Modify player statistics");
        sender.sendMessage(ChatColor.YELLOW + "/admin achievement" + ChatColor.GRAY + " - Manage achievements");
        sender.sendMessage(ChatColor.YELLOW + "/admin maintenance" + 
                ChatColor.GRAY + " - Perform database maintenance tasks");
        sender.sendMessage(ChatColor.YELLOW + "/admin help" + ChatColor.GRAY + " - Show this help message");
    }
    
    @Override
    public List<String> getTabCompletions(String subCommand, CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (subCommand.isEmpty()) {
            if (args.length == 1) {
                // Tab complete subcommands
                completions.add("reload");
                completions.add("reset");
                completions.add("modify");
                completions.add("achievement");
                completions.add("maintenance");
                completions.add("help");
                
                return completions;
            }
        } else if (subCommand.equalsIgnoreCase("reset")) {
            if (args.length == 1) {
                // Tab complete player names
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            } else if (args.length == 2) {
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
        } else if (subCommand.equalsIgnoreCase("modify")) {
            if (args.length == 1) {
                // Tab complete player names
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            } else if (args.length == 2) {
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
            } else if (args.length == 3) {
                // Tab complete values
                return Arrays.asList("0", "10", "50", "100", "1000");
            }
        } else if (subCommand.equalsIgnoreCase("achievement")) {
            if (args.length == 1) {
                // Tab complete achievement actions
                return Arrays.asList("list", "info", "award", "reset", "reload");
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("list")) {
                    // Tab complete categories
                    return Arrays.asList("blocks", "combat", "movement", "items", "general");
                } else if (args[0].equalsIgnoreCase("info")) {
                    // Tab complete achievement IDs
                    return achievementService.getAllAchievements().stream()
                            .map(Achievement::getId)
                            .collect(Collectors.toList());
                } else if (args[0].equalsIgnoreCase("award") || args[0].equalsIgnoreCase("reset")) {
                    // Tab complete player names
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("award")) {
                // Tab complete achievement IDs
                return achievementService.getAllAchievements().stream()
                        .map(Achievement::getId)
                        .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
}