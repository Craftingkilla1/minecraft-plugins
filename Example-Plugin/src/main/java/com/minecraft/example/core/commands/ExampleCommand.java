// ./Example-Plugin/src/main/java/com/minecraft/example/core/commands/ExampleCommand.java
package com.minecraft.example.core.commands;

import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.ExamplePlugin;
import com.minecraft.example.config.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main command class for Example-Plugin.
 * Demonstrates Core-Utils command framework.
 */
@Command(name = "example", description = "Main command for Example-Plugin", aliases = {"exp", "exampleplugin"})
@Permission("exampleplugin.use")
public class ExampleCommand implements com.minecraft.core.command.TabCompletionProvider {
    
    private final ExamplePlugin plugin;
    
    /**
     * Constructs a new ExampleCommand.
     *
     * @param plugin The plugin instance
     */
    public ExampleCommand(ExamplePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Default command handler when no subcommand is provided.
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(description = "Shows plugin information")
    public void defaultCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.AQUA + "=== Example-Plugin ===");
        sender.sendMessage(ChatColor.GRAY + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.GRAY + "Author: " + ChatColor.WHITE + plugin.getDescription().getAuthors().get(0));
        sender.sendMessage(ChatColor.GRAY + "Type " + ChatColor.YELLOW + "/example help" + ChatColor.GRAY + " for a list of commands.");
    }
    
    /**
     * Help command handler.
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(name = "help", description = "Shows help information")
    public void helpCommand(CommandSender sender, String[] args) {
        ConfigManager configManager = plugin.getConfigManager();
        
        // Send header
        sender.sendMessage(configManager.getMessages().getFormattedMessage("commands.help.header"));
        
        // Send available commands
        sender.sendMessage(formatHelpLine("example", "Shows plugin information"));
        sender.sendMessage(formatHelpLine("example help", "Shows this help message"));
        
        if (sender.hasPermission("exampleplugin.admin")) {
            sender.sendMessage(formatHelpLine("example reload", "Reloads the plugin configuration"));
            sender.sendMessage(formatHelpLine("example version", "Shows the plugin version"));
            sender.sendMessage(formatHelpLine("example info", "Shows detailed plugin information"));
        }
        
        // Send stats commands
        sender.sendMessage(ChatColor.AQUA + "--- Stats Commands ---");
        sender.sendMessage(formatHelpLine("stats", "Shows your statistics"));
        
        if (sender.hasPermission("exampleplugin.stats.others")) {
            sender.sendMessage(formatHelpLine("stats <player>", "Shows another player's statistics"));
        }
        
        if (sender.hasPermission("exampleplugin.stats.reset")) {
            sender.sendMessage(formatHelpLine("stats reset [player]", "Resets statistics"));
        }
        
        // Send achievement commands
        sender.sendMessage(ChatColor.AQUA + "--- Achievement Commands ---");
        sender.sendMessage(formatHelpLine("achievements", "Shows your achievements"));
        
        if (sender.hasPermission("exampleplugin.achievements.others")) {
            sender.sendMessage(formatHelpLine("achievements <player>", "Shows another player's achievements"));
        }
        
        if (sender.hasPermission("exampleplugin.achievements.award")) {
            sender.sendMessage(formatHelpLine("achievements award <player> <achievement>", "Awards an achievement"));
        }
        
        // Send footer
        sender.sendMessage(configManager.getMessages().getFormattedMessage("commands.help.footer"));
    }
    
    /**
     * Formats a help line.
     *
     * @param command The command
     * @param description The description
     * @return The formatted help line
     */
    private String formatHelpLine(String command, String description) {
        return ChatColor.YELLOW + "/" + command + ChatColor.GRAY + " - " + ChatColor.WHITE + description;
    }
    
    /**
     * Reload command handler.
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(name = "reload", description = "Reloads the plugin configuration")
    @Permission(value = "exampleplugin.admin", message = "You don't have permission to reload the plugin.")
    public void reloadCommand(CommandSender sender, String[] args) {
        // Reload configuration
        plugin.getConfigManager().reloadAll();
        
        // Send message
        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully.");
        
        LogUtil.info("Configuration reloaded by " + sender.getName());
    }
    
    /**
     * Version command handler.
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(name = "version", description = "Shows the plugin version", aliases = {"ver"})
    public void versionCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.AQUA + "Example-Plugin v" + plugin.getDescription().getVersion());
    }
    
    /**
     * Info command handler.
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(name = "info", description = "Shows detailed plugin information")
    @Permission(value = "exampleplugin.admin", message = "You don't have permission to view detailed info.")
    public void infoCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.AQUA + "=== Example-Plugin Information ===");
        sender.sendMessage(ChatColor.GRAY + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.GRAY + "Author: " + ChatColor.WHITE + plugin.getDescription().getAuthors().get(0));
        sender.sendMessage(ChatColor.GRAY + "Database: " + ChatColor.WHITE + (plugin.isDatabaseEnabled() ? "Enabled" : "Disabled"));
        
        // Show feature status
        sender.sendMessage(ChatColor.GRAY + "Features:");
        sender.sendMessage(ChatColor.GRAY + "  - Stat Tracking: " + featureStatus("statistics.track_blocks_broken"));
        sender.sendMessage(ChatColor.GRAY + "  - Achievements: " + featureStatus("features.achievements"));
        sender.sendMessage(ChatColor.GRAY + "  - Admin Tools: " + featureStatus("features.admin_tools"));
        
        // Show save interval
        int saveInterval = plugin.getConfig().getInt("statistics.save_interval", 300);
        sender.sendMessage(ChatColor.GRAY + "Save Interval: " + ChatColor.WHITE + 
                (saveInterval > 0 ? saveInterval + " seconds" : "Disabled"));
    }
    
    /**
     * Gets the status of a feature.
     *
     * @param configPath The configuration path
     * @return The formatted status
     */
    private String featureStatus(String configPath) {
        boolean enabled = plugin.getConfig().getBoolean(configPath, false);
        return enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled";
    }
    
    /**
     * Debug command handler.
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(name = "debug", description = "Toggles debug mode")
    @Permission(value = "exampleplugin.admin", message = "You don't have permission to use debug mode.")
    public void debugCommand(CommandSender sender, String[] args) {
        // Toggle debug mode
        boolean debug = !plugin.getConfig().getBoolean("debug.enabled", false);
        plugin.getConfig().set("debug.enabled", debug);
        plugin.saveConfig();
        
        // Update LogUtil
        LogUtil.setDebugEnabled(debug);
        
        // Send message
        sender.sendMessage((debug ? ChatColor.GREEN + "Debug mode enabled." : ChatColor.RED + "Debug mode disabled."));
    }
    
    /**
     * Database command handler.
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(name = "database", description = "Shows database information", aliases = {"db"})
    @Permission(value = "exampleplugin.admin", message = "You don't have permission to view database info.")
    public void databaseCommand(CommandSender sender, String[] args) {
        if (!plugin.isDatabaseEnabled()) {
            sender.sendMessage(ChatColor.RED + "Database features are disabled.");
            return;
        }
        
        sender.sendMessage(ChatColor.AQUA + "=== Database Information ===");
        
        try {
            // Get database metadata
            String dbName = plugin.getDatabase().queryFirst(
                    "SELECT DATABASE() as db",
                    row -> row.getString("db")
            ).orElse("Unknown");
            
            // Count players
            int playerCount = plugin.getDatabase().queryFirst(
                    "SELECT COUNT(*) as count FROM players",
                    row -> row.getInt("count")
            ).orElse(0);
            
            // Count achievements
            int achievementCount = plugin.getDatabase().queryFirst(
                    "SELECT COUNT(*) as count FROM achievements",
                    row -> row.getInt("count")
            ).orElse(0);
            
            // Show info
            sender.sendMessage(ChatColor.GRAY + "Database: " + ChatColor.WHITE + dbName);
            sender.sendMessage(ChatColor.GRAY + "Players: " + ChatColor.WHITE + playerCount);
            sender.sendMessage(ChatColor.GRAY + "Achievements: " + ChatColor.WHITE + achievementCount);
            sender.sendMessage(ChatColor.GRAY + "Status: " + ChatColor.GREEN + "Connected");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.GRAY + "Status: " + ChatColor.RED + "Error");
            sender.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
            LogUtil.severe("Error getting database info: " + e.getMessage());
        }
    }
    
    @Override
    public List<String> getTabCompletions(String subCommand, CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (subCommand.isEmpty()) {
            // Root command completions
            if (args.length == 1) {
                String arg = args[0].toLowerCase();
                List<String> subCommands = Arrays.asList("help", "version", "info", "reload", "debug", "database");
                return subCommands.stream()
                        .filter(cmd -> cmd.startsWith(arg))
                        .filter(cmd -> hasPermissionForSubCommand(sender, cmd))
                        .collect(Collectors.toList());
            }
        } else if (subCommand.equalsIgnoreCase("database") || subCommand.equalsIgnoreCase("db")) {
            // No subcommand arguments for database command
        }
        
        return completions;
    }
    
    /**
     * Checks if the sender has permission for a subcommand.
     *
     * @param sender The command sender
     * @param subCommand The subcommand
     * @return True if the sender has permission
     */
    private boolean hasPermissionForSubCommand(CommandSender sender, String subCommand) {
        switch (subCommand) {
            case "reload":
            case "info":
            case "debug":
            case "database":
                return sender.hasPermission("exampleplugin.admin");
            default:
                return true;
        }
    }
}