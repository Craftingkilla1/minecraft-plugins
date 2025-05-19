// ./src/main/java/com/minecraft/example/command/ExampleCommand.java
package com.minecraft.example.command;

import com.minecraft.core.api.service.ServiceRegistry;
import com.minecraft.core.command.TabCompletionProvider;
import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.utils.FormatUtil;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.core.utils.TimeUtil;
import com.minecraft.example.ExamplePlugin;
import com.minecraft.example.service.StatsService;
import com.minecraft.sqlbridge.api.DatabaseService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Example command that demonstrates various Core-Utils features
 */
@Command(name = "example", description = "Example plugin commands", aliases = {"ex"})
public class ExampleCommand implements TabCompletionProvider {
    
    private final ExamplePlugin plugin;
    
    /**
     * Constructor
     * @param plugin Plugin instance
     */
    public ExampleCommand(ExamplePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Default command - show help
     * @param sender Command sender
     * @param args Command arguments
     */
    @SubCommand(isDefault = true, description = "Show help information")
    public void defaultCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "=== Example Plugin Help ===");
        sender.sendMessage(ChatColor.YELLOW + "/example help" + ChatColor.WHITE + " - Show this help message");
        sender.sendMessage(ChatColor.YELLOW + "/example info" + ChatColor.WHITE + " - Show plugin information");
        sender.sendMessage(ChatColor.YELLOW + "/example services" + ChatColor.WHITE + " - List registered services");
        sender.sendMessage(ChatColor.YELLOW + "/example reload" + ChatColor.WHITE + " - Reload the plugin configuration");
        sender.sendMessage(ChatColor.YELLOW + "/example debug" + ChatColor.WHITE + " - Toggle debug mode");
        sender.sendMessage(ChatColor.YELLOW + "/playerstats" + ChatColor.WHITE + " - View your player statistics");
        sender.sendMessage(ChatColor.YELLOW + "/playerstats view <player>" + ChatColor.WHITE + " - View another player's statistics");
        sender.sendMessage(ChatColor.YELLOW + "/playerstats top <stat> [limit]" + ChatColor.WHITE + " - View top players for a statistic");
    }
    
    /**
     * Help command - show help
     * @param sender Command sender
     * @param args Command arguments
     */
    @SubCommand(name = "help", description = "Show help information")
    public void helpCommand(CommandSender sender, String[] args) {
        defaultCommand(sender, args);
    }
    
    /**
     * Info command - show plugin information
     * @param sender Command sender
     * @param args Command arguments
     */
    @SubCommand(name = "info", description = "Show plugin information")
    public void infoCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "=== Example Plugin Information ===");
        sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "Author: " + ChatColor.WHITE + plugin.getDescription().getAuthors().get(0));
        sender.sendMessage(ChatColor.YELLOW + "Website: " + ChatColor.WHITE + plugin.getDescription().getWebsite());
        sender.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + plugin.getDescription().getDescription());
        
        // Show Core-Utils and SQL-Bridge versions
        sender.sendMessage(ChatColor.YELLOW + "Core-Utils Version: " + 
                          ChatColor.WHITE + plugin.getServer().getPluginManager().getPlugin("CoreUtils").getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "SQL-Bridge Version: " + 
                          ChatColor.WHITE + plugin.getServer().getPluginManager().getPlugin("SQL-Bridge").getDescription().getVersion());
        
        // Show current time using TimeUtil
        sender.sendMessage(ChatColor.YELLOW + "Current Time: " + 
                          ChatColor.WHITE + TimeUtil.formatDateTime(TimeUtil.now()));
    }
    
    /**
     * Services command - list registered services
     * @param sender Command sender
     * @param args Command arguments
     */
    @SubCommand(name = "services", description = "List registered services")
    public void servicesCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "=== Registered Services ===");
        
        // Check if StatsService is registered
        if (ServiceRegistry.getService(StatsService.class) != null) {
            sender.sendMessage(ChatColor.YELLOW + "StatsService: " + 
                              ChatColor.GREEN + "Registered " + 
                              ChatColor.GRAY + "(Implementation: " + 
                              ServiceRegistry.getService(StatsService.class).getClass().getSimpleName() + ")");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "StatsService: " + ChatColor.RED + "Not Registered");
        }
        
        // Check if DatabaseService is registered
        if (ServiceRegistry.getService(DatabaseService.class) != null) {
            sender.sendMessage(ChatColor.YELLOW + "DatabaseService: " + 
                              ChatColor.GREEN + "Registered " + 
                              ChatColor.GRAY + "(Implementation: " + 
                              ServiceRegistry.getService(DatabaseService.class).getClass().getSimpleName() + ")");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "DatabaseService: " + ChatColor.RED + "Not Registered");
        }
    }
    
    /**
     * Reload command - reload the plugin configuration
     * @param sender Command sender
     * @param args Command arguments
     */
    @SubCommand(name = "reload", description = "Reload the plugin configuration", permission = "exampleplugin.reload")
    @Permission(value = "exampleplugin.reload", message = "You don't have permission to reload the plugin.")
    public void reloadCommand(CommandSender sender, String[] args) {
        // Reload configuration
        plugin.getConfigManager().reloadConfig();
        
        sender.sendMessage(ChatColor.GREEN + "Example Plugin configuration reloaded!");
    }
    
    /**
     * Debug command - toggle debug mode
     * @param sender Command sender
     * @param args Command arguments
     */
    @SubCommand(name = "debug", description = "Toggle debug mode", permission = "exampleplugin.debug")
    @Permission(value = "exampleplugin.debug", message = "You don't have permission to toggle debug mode.")
    public void debugCommand(CommandSender sender, String[] args) {
        // Toggle debug mode in config
        boolean debugMode = !plugin.getConfigManager().isDebugMode();
        plugin.getConfig().set("debug", debugMode);
        plugin.getConfigManager().saveConfig();
        
        // Set debug mode in LogUtil
        LogUtil.setDebugMode(debugMode);
        
        sender.sendMessage(ChatColor.GREEN + "Debug mode " + (debugMode ? "enabled" : "disabled") + "!");
        
        // Demonstrate debug logging
        if (debugMode) {
            LogUtil.debug("Debug mode enabled by " + sender.getName());
            
            // If sender is a player, output some debug info
            if (sender instanceof Player) {
                Player player = (Player) sender;
                LogUtil.debug("Player location: " + FormatUtil.formatLocation(player.getLocation()));
                LogUtil.debug("Player health: " + FormatUtil.formatNumber(player.getHealth()) + 
                             "/" + FormatUtil.formatNumber(player.getMaxHealth()));
            }
        }
    }
    
    @Override
    public List<String> getTabCompletions(String subCommand, CommandSender sender, String[] args) {
        // Provide tab completions for main command
        if (args.length == 1) {
            return Arrays.asList("help", "info", "services", "reload", "debug");
        }
        
        return new ArrayList<>();
    }
}