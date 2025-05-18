// ./Example-Plugin/src/main/java/com/minecraft/example/core/commands/AchievementCommand.java
package com.minecraft.example.core.commands;

import com.minecraft.core.api.CoreAPI;
import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.utils.InventoryUtil;
import com.minecraft.example.ExamplePlugin;
import com.minecraft.example.config.ConfigManager;
import com.minecraft.example.core.services.AchievementService;
import com.minecraft.example.core.services.NotificationService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command class for achievements.
 * Demonstrates Core-Utils command framework.
 */
@Command(name = "achievements", description = "View and manage achievements", aliases = {"achieve", "ach"})
@Permission("exampleplugin.achievements")
public class AchievementCommand implements com.minecraft.core.command.TabCompletionProvider {
    
    private final ExamplePlugin plugin;
    private final ConfigManager configManager;
    
    /**
     * Constructs a new AchievementCommand.
     *
     * @param plugin The plugin instance
     */
    public AchievementCommand(ExamplePlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }
    
    /**
     * Default command handler when no subcommand is provided.
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(description = "Shows your achievements or another player's")
    public void defaultCommand(CommandSender sender, String[] args) {
        // Get achievement service
        AchievementService achievementService = CoreAPI.Services.get(AchievementService.class);
        if (achievementService == null) {
            sender.sendMessage(ChatColor.RED + "Achievement service is not available.");
            return;
        }
        
        // Determine target player
        Player targetPlayer;
        
        if (args.length > 0) {
            // Looking up another player
            if (!sender.hasPermission("exampleplugin.achievements.others")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to view other players' achievements.");
                return;
            }
            
            targetPlayer = Bukkit.getPlayer(args[0]);
            if (targetPlayer == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found.");
                return;
            }
        } else {
            // Self achievements
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Please specify a player name.");
                return;
            }
            
            targetPlayer = (Player) sender;
        }
        
        // Determine display mode
        boolean useGui = false;
        if (args.length > 1 && args[1].equalsIgnoreCase("gui") && sender instanceof Player) {
            useGui = true;
        }
        
        if (useGui) {
            // Show GUI
            showAchievementsGui((Player) sender, targetPlayer, achievementService);
        } else {
            // Show text list
            showAchievementsText(sender, targetPlayer, achievementService);
        }
    }
    
    /**
     * Shows achievements in text form.
     *
     * @param sender The command sender
     * @param targetPlayer The target player
     * @param achievementService The achievement service
     */
    private void showAchievementsText(CommandSender sender, Player targetPlayer, AchievementService achievementService) {
        // Get player achievements with names
        Map<String, String> playerAchievements = achievementService.getPlayerAchievementsWithNames(targetPlayer);
        
        // Get messages
        String header = configManager.getMessages().getFormattedMessage(
                "commands.achievements.header", 
                Collections.singletonMap("player", targetPlayer.getName())
        );
        
        String footer = configManager.getMessages().getFormattedMessage("commands.achievements.footer");
        
        // Send header
        sender.sendMessage(header);
        
        if (playerAchievements.isEmpty()) {
            sender.sendMessage(configManager.getMessages().getFormattedMessage(
                    "commands.achievements.not_found",
                    Collections.singletonMap("player", targetPlayer.getName())
            ));
        } else {
            // Send each achievement
            for (Map.Entry<String, String> entry : playerAchievements.entrySet()) {
                String achievementId = entry.getKey();
                String achievementName = entry.getValue();
                
                // Get description
                Map<String, Object> details = achievementService.getAchievementDetails(achievementId);
                String description = (String) details.getOrDefault("description", "");
                
                // Format and send
                String line = configManager.getMessages().getFormattedMessage(
                        "commands.achievements.format",
                        Map.of(
                                "achievement", achievementName,
                                "description", description
                        )
                );
                sender.sendMessage(line);
            }
            
            // Send achievement count
            int count = playerAchievements.size();
            int total = achievementService.getAllAchievements().size();
            sender.sendMessage(ChatColor.YELLOW + "Progress: " + ChatColor.WHITE + 
                    count + "/" + total + " (" + (int)((count / (double)total) * 100) + "%)");
        }
        
        // Send footer
        sender.sendMessage(footer);
    }
    
    /**
     * Shows achievements in a GUI.
     *
     * @param player The player viewing the GUI
     * @param targetPlayer The target player
     * @param achievementService The achievement service
     */
    private void showAchievementsGui(Player player, Player targetPlayer, AchievementService achievementService) {
        // Get player achievements
        List<String> playerAchievements = achievementService.getPlayerAchievements(targetPlayer);
        
        // Get all achievements
        Map<String, String> allAchievements = achievementService.getAllAchievements();
        
        // Create inventory
        int size = (int) Math.ceil(allAchievements.size() / 9.0) * 9;
        size = Math.max(9, Math.min(54, size)); // Min 9, max 54
        
        Inventory inventory = Bukkit.createInventory(null, size, 
                targetPlayer.getName() + "'s Achievements");
        
        // Add achievements to inventory
        int slot = 0;
        for (Map.Entry<String, String> entry : allAchievements.entrySet()) {
            String achievementId = entry.getKey();
            String achievementName = entry.getValue();
            
            // Get details
            Map<String, Object> details = achievementService.getAchievementDetails(achievementId);
            String description = (String) details.getOrDefault("description", "");
            String iconMaterial = (String) details.getOrDefault("icon_material", "DIAMOND");
            
            // Determine if earned
            boolean earned = playerAchievements.contains(achievementId);
            
            // Create item
            Material material;
            try {
                material = Material.valueOf(iconMaterial.toUpperCase());
            } catch (IllegalArgumentException e) {
                material = Material.DIAMOND;
            }
            
            ItemStack item;
            if (earned) {
                // Earned achievement
                item = InventoryUtil.createItem(material, 
                        "&a" + achievementName,
                        "&7" + description,
                        "&aEarned");
            } else {
                // Locked achievement
                item = InventoryUtil.createItem(Material.GRAY_DYE, 
                        "&7???" + (player.hasPermission("exampleplugin.admin") ? " (" + achievementName + ")" : ""),
                        "&8" + (player.hasPermission("exampleplugin.admin") ? description : "???"),
                        "&cLocked");
            }
            
            // Add to inventory
            inventory.setItem(slot++, item);
            
            // Break if inventory is full
            if (slot >= size) {
                break;
            }
        }
        
        // Show inventory
        player.openInventory(inventory);
    }
    
    /**
     * List command handler.
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(name = "list", description = "Lists all available achievements")
    public void listCommand(CommandSender sender, String[] args) {
        // Get achievement service
        AchievementService achievementService = CoreAPI.Services.get(AchievementService.class);
        if (achievementService == null) {
            sender.sendMessage(ChatColor.RED + "Achievement service is not available.");
            return;
        }
        
        // Get all achievements
        Map<String, String> allAchievements = achievementService.getAllAchievements();
        
        // Show achievements
        sender.sendMessage(ChatColor.AQUA + "=== Available Achievements ===");
        
        if (allAchievements.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No achievements available.");
        } else {
            for (Map.Entry<String, String> entry : allAchievements.entrySet()) {
                String achievementId = entry.getKey();
                String achievementName = entry.getValue();
                
                // Get description
                Map<String, Object> details = achievementService.getAchievementDetails(achievementId);
                String description = (String) details.getOrDefault("description", "");
                
                sender.sendMessage(ChatColor.YELLOW + achievementId + ChatColor.GRAY + ": " + 
                        ChatColor.WHITE + achievementName + ChatColor.GRAY + " - " + 
                        ChatColor.ITALIC + description);
            }
        }
    }
    
    /**
     * Award command handler.
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(name = "award", description = "Awards an achievement to a player", minArgs = 2)
    @Permission(value = "exampleplugin.achievements.award", message = "You don't have permission to award achievements.")
    public void awardCommand(CommandSender sender, String[] args) {
        // Get achievement service
        AchievementService achievementService = CoreAPI.Services.get(AchievementService.class);
        if (achievementService == null) {
            sender.sendMessage(ChatColor.RED + "Achievement service is not available.");
            return;
        }
        
        // Get target player
        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found.");
            return;
        }
        
        // Get achievement ID
        String achievementId = args[1].toLowerCase();
        
        // Check if achievement exists
        Map<String, Object> details = achievementService.getAchievementDetails(achievementId);
        if (details.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Achievement '" + achievementId + "' not found.");
            return;
        }
        
        // Check if player already has achievement
        if (achievementService.hasAchievement(targetPlayer, achievementId)) {
            sender.sendMessage(configManager.getMessages().getFormattedMessage(
                    "commands.achievements.already_has",
                    Collections.singletonMap("player", targetPlayer.getName())
            ));
            return;
        }
        
        // Award achievement
        boolean awarded = achievementService.awardAchievement(targetPlayer, achievementId);
        
        // Get notification service
        NotificationService notificationService = CoreAPI.Services.get(NotificationService.class);
        
        if (awarded) {
            // Notify sender
            if (notificationService != null) {
                notificationService.sendSuccessMessage(sender, 
                        configManager.getMessages().getFormattedMessage(
                                "commands.achievements.awarded", 
                                Map.of(
                                        "player", targetPlayer.getName(),
                                        "achievement", (String) details.getOrDefault("name", achievementId)
                                )
                        )
                );
            } else {
                sender.sendMessage(ChatColor.GREEN + "Awarded achievement '" + 
                        details.getOrDefault("name", achievementId) + "' to " + targetPlayer.getName() + ".");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to award achievement.");
        }
    }
    
    /**
     * Recent command handler.
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(name = "recent", description = "Shows recently earned achievements", minArgs = 0, maxArgs = 1)
    public void recentCommand(CommandSender sender, String[] args) {
        // Get achievement service
        AchievementService achievementService = CoreAPI.Services.get(AchievementService.class);
        if (achievementService == null) {
            sender.sendMessage(ChatColor.RED + "Achievement service is not available.");
            return;
        }
        
        // Get limit
        int limit = 5;
        if (args.length > 0) {
            try {
                limit = Integer.parseInt(args[0]);
                limit = Math.max(1, Math.min(20, limit)); // Limit to 1-20
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid limit. Must be a number between 1 and 20.");
                return;
            }
        }
        
        // Get recent global achievements
        Map<String, String> recentAchievements = achievementService.getGlobalRecentAchievements(limit);
        
        // Show recent achievements
        sender.sendMessage(ChatColor.AQUA + "=== Recent Achievements ===");
        
        if (recentAchievements.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No recent achievements.");
        } else {
            for (Map.Entry<String, String> entry : recentAchievements.entrySet()) {
                String playerName = entry.getKey();
                String achievementId = entry.getValue();
                
                // Get achievement details
                Map<String, Object> details = achievementService.getAchievementDetails(achievementId);
                String achievementName = (String) details.getOrDefault("name", achievementId);
                
                sender.sendMessage(ChatColor.YELLOW + playerName + ChatColor.GRAY + " earned " + 
                        ChatColor.WHITE + achievementName);
            }
        }
    }
    
    /**
     * Check command handler.
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(name = "check", description = "Checks and awards achievements", minArgs = 0, maxArgs = 1)
    @Permission(value = "exampleplugin.admin", message = "You don't have permission to use this command.")
    public void checkCommand(CommandSender sender, String[] args) {
        // Get achievement service
        AchievementService achievementService = CoreAPI.Services.get(AchievementService.class);
        if (achievementService == null) {
            sender.sendMessage(ChatColor.RED + "Achievement service is not available.");
            return;
        }
        
        // Determine target player
        Player targetPlayer;
        
        if (args.length > 0) {
            targetPlayer = Bukkit.getPlayer(args[0]);
            if (targetPlayer == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found.");
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Please specify a player name.");
                return;
            }
            
            targetPlayer = (Player) sender;
        }
        
        // Check and award achievements
        List<String> awarded = achievementService.checkAndAwardAchievements(targetPlayer);
        
        if (awarded.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No new achievements awarded to " + targetPlayer.getName() + ".");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Awarded " + awarded.size() + " achievement(s) to " + 
                    targetPlayer.getName() + ":");
            
            for (String achievementId : awarded) {
                // Get achievement details
                Map<String, Object> details = achievementService.getAchievementDetails(achievementId);
                String achievementName = (String) details.getOrDefault("name", achievementId);
                
                sender.sendMessage(ChatColor.YELLOW + "- " + achievementName);
            }
        }
    }
    
    @Override
    public List<String> getTabCompletions(String subCommand, CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (subCommand.isEmpty()) {
            // Root command completions
            if (args.length == 1) {
                String arg = args[0].toLowerCase();
                
                // Add subcommands
                List<String> subCommands = new ArrayList<>();
                subCommands.add("list");
                subCommands.add("recent");
                
                // Add admin subcommands
                if (sender.hasPermission("exampleplugin.admin")) {
                    subCommands.add("check");
                }
                
                if (sender.hasPermission("exampleplugin.achievements.award")) {
                    subCommands.add("award");
                }
                
                // Add online player names if user has permission
                if (sender.hasPermission("exampleplugin.achievements.others")) {
                    Bukkit.getOnlinePlayers().forEach(player -> subCommands.add(player.getName()));
                }
                
                // Filter by prefix
                return subCommands.stream()
                        .filter(cmd -> cmd.toLowerCase().startsWith(arg))
                        .collect(Collectors.toList());
            } else if (args.length == 2 && sender instanceof Player) {
                String arg = args[1].toLowerCase();
                
                // Check if first arg is a player name
                if (Bukkit.getPlayer(args[0]) != null) {
                    // Suggest gui option
                    if ("gui".startsWith(arg)) {
                        completions.add("gui");
                    }
                }
            }
        } else if (subCommand.equalsIgnoreCase("award")) {
            if (args.length == 1) {
                // Player name completions
                String arg = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(arg))
                        .collect(Collectors.toList());
            } else if (args.length == 2) {
                // Achievement ID completions
                String arg = args[1].toLowerCase();
                
                // Get achievement service
                AchievementService achievementService = CoreAPI.Services.get(AchievementService.class);
                if (achievementService != null) {
                    return achievementService.getAllAchievements().keySet().stream()
                            .filter(id -> id.startsWith(arg))
                            .collect(Collectors.toList());
                }
            }
        } else if (subCommand.equalsIgnoreCase("check")) {
            if (args.length == 1) {
                // Player name completions
                String arg = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(arg))
                        .collect(Collectors.toList());
            }
        } else if (subCommand.equalsIgnoreCase("recent")) {
            if (args.length == 1) {
                // Limit completions
                String arg = args[0].toLowerCase();
                return Arrays.asList("5", "10", "15", "20")
                        .stream()
                        .filter(limit -> limit.startsWith(arg))
                        .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
}