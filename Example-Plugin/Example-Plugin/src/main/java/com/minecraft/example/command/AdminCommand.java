// ./Example-Plugin/src/main/java/com/minecraft/example/command/AdminCommand.java
package com.minecraft.example.command;

import com.minecraft.core.command.TabCompletionProvider;
import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.utils.LogUtil;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin commands for the example plugin
 * Demonstrates Core-Utils command framework with permissions
 */
@Command(name = "exampleadmin", description = "Admin commands for Example Plugin", aliases = {"exadmin"})
@Permission(value = "example.admin", message = "You don't have permission to use this command.")
public class AdminCommand implements TabCompletionProvider {
    
    private final ExamplePlugin plugin;
    private final StatsService statsService;
    
    public AdminCommand(ExamplePlugin plugin) {
        this.plugin = plugin;
        this.statsService = plugin.getStatsService();
    }
    
    /**
     * Default command - show help
     */
    @SubCommand(isDefault = true, description = "Show admin help")
    public void showHelp(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "=== Example Plugin Admin Commands ===");
        sender.sendMessage(ChatColor.GOLD + "/exampleadmin reload " + ChatColor.WHITE + "- Reload configuration");
        sender.sendMessage(ChatColor.GOLD + "/exampleadmin debug " + ChatColor.WHITE + "- Toggle debug mode");
        sender.sendMessage(ChatColor.GOLD + "/exampleadmin reset <player> <stat> " + ChatColor.WHITE + "- Reset a player's stat");
    }
    
    /**
     * Reload configuration
     */
    @SubCommand(name = "reload", description = "Reload plugin configuration")
    @Permission("example.admin.reload")
    public void reloadConfig(CommandSender sender, String[] args) {
        plugin.reloadPluginConfig();
        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
    }
    
    /**
     * Toggle debug mode
     */
    @SubCommand(name = "debug", description = "Toggle debug mode")
    @Permission("example.admin.debug")
    public void toggleDebug(CommandSender sender, String[] args) {
        // Get current debug state from LogUtil
        boolean currentDebug = LogUtil.isDebugEnabled();
        
        // Toggle state
        LogUtil.setDebugMode(!currentDebug);
        
        // Save to config
        plugin.getConfig().set("debug", !currentDebug);
        plugin.saveConfig();
        
        sender.sendMessage(ChatColor.GREEN + "Debug mode " + 
                          (!currentDebug ? "enabled" : "disabled") + ".");
    }
    
    /**
     * Reset a player's stat
     */
    @SubCommand(name = "reset", description = "Reset a player's stat", minArgs = 2, maxArgs = 2)
    @Permission("example.admin.reset")
    public void resetStat(CommandSender sender, String[] args) {
        String playerName = args[0];
        String statType = args[1].toLowerCase();
        
        // Find player (online or offline)
        Player targetPlayer = Bukkit.getPlayer(playerName);
        UUID targetUuid;
        
        if (targetPlayer != null) {
            // Player is online
            targetUuid = targetPlayer.getUniqueId();
        } else {
            // Try to find offline player by name
            sender.sendMessage(ChatColor.YELLOW + "Searching for offline player...");
            
            // Java 8 compatible version (no ifPresentOrElse)
            Optional<PlayerStats> statsOpt = statsService.getPlayerStatsByName(playerName);
            if (statsOpt.isPresent()) {
                resetStatForUuid(sender, statsOpt.get().getUuid(), playerName, statType);
            } else {
                sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            }
            return;
        }
        
        // Player is online, reset stat
        resetStatForUuid(sender, targetUuid, playerName, statType);
    }
    
    /**
     * Helper method to reset a stat for a known UUID
     */
    private void resetStatForUuid(CommandSender sender, UUID uuid, String playerName, String statType) {
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
        
        // Reset the stat
        boolean success = statsService.resetStat(uuid, statField);
        
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Reset " + statLabel + " for " + playerName + ".");
            LogUtil.info("Admin " + sender.getName() + " reset " + statLabel + " for " + playerName);
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to reset stat. See console for details.");
        }
    }
    
    @Override
    public List<String> getTabCompletions(String subCommand, CommandSender sender, String[] args) {
        // Tab completions for different subcommands
        if (subCommand.equalsIgnoreCase("reset")) {
            if (args.length == 1) {
                // Complete player names
                String partialName = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .collect(Collectors.toList());
            } else if (args.length == 2) {
                // Complete stat types
                String partial = args[1].toLowerCase();
                List<String> statTypes = Arrays.asList("playtime", "logins", "broken", "placed");
                return statTypes.stream()
                    .filter(type -> type.startsWith(partial))
                    .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }
}