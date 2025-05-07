package com.example.exampleplugin;

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
import java.util.List;

/**
 * Example command class that demonstrates how to use the
 * Core-Utils command framework
 */
@Command(name = "example", description = "Example plugin commands", aliases = {"ex"})
@Permission("example.use")
public class ExampleCommand implements TabCompletionProvider {
    private final ExamplePlugin plugin;
    
    /**
     * Create a new example command
     * 
     * @param plugin The plugin instance
     */
    public ExampleCommand(ExamplePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Help command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "help", description = "Show help information", aliases = {"?"})
    public void helpCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "=== Example Plugin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/example help" + ChatColor.WHITE + " - Show this help page");
        sender.sendMessage(ChatColor.YELLOW + "/example process <text>" + ChatColor.WHITE + " - Process some text");
        sender.sendMessage(ChatColor.YELLOW + "/example stats" + ChatColor.WHITE + " - Show service statistics");
        sender.sendMessage(ChatColor.YELLOW + "/example feature <name> [on|off]" + ChatColor.WHITE + " - Check or set feature status");
        sender.sendMessage(ChatColor.YELLOW + "/example version" + ChatColor.WHITE + " - Show plugin version");
        
        // Add database command help if SQL-Bridge is available
        if (plugin.getSqlBridge() != null) {
            sender.sendMessage(ChatColor.YELLOW + "/example dbstatus" + ChatColor.WHITE + " - Show SQL-Bridge status");
            sender.sendMessage(ChatColor.YELLOW + "/exampledb" + ChatColor.WHITE + " - Access database commands");
        }
    }
    
    /**
     * Process command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "process", description = "Process text", minArgs = 1, permission = "example.process")
    public void processCommand(CommandSender sender, String[] args) {
        // Get the text to process (joining all arguments)
        String text = String.join(" ", args);
        
        // Get our service from the plugin
        ExampleService service = plugin.getService();
        
        // Process the text
        String result = service.processData(text);
        
        // Send the result
        sender.sendMessage(ChatColor.GREEN + "Processed result: " + ChatColor.WHITE + result);
    }
    
    /**
     * Stats command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "stats", description = "Show service statistics", permission = "example.stats")
    public void statsCommand(CommandSender sender, String[] args) {
        // Get our service from the plugin
        ExampleService service = plugin.getService();
        
        // Get the stats
        List<String> stats = service.getStats();
        
        // Send the stats
        sender.sendMessage(ChatColor.GREEN + "=== Service Statistics ===");
        
        for (String stat : stats) {
            sender.sendMessage(ChatColor.YELLOW + stat);
        }
    }
    
    /**
     * Feature command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "feature", description = "Check or set feature status", minArgs = 1, maxArgs = 2, permission = "example.feature")
    public void featureCommand(CommandSender sender, String[] args) {
        // Get the feature name
        String featureName = args[0];
        
        // Get our service from the plugin
        ExampleService service = plugin.getService();
        
        // Check if we're setting the feature
        if (args.length == 2) {
            String value = args[1].toLowerCase();
            boolean enabled;
            
            if (value.equals("on") || value.equals("true") || value.equals("enable")) {
                enabled = true;
            } else if (value.equals("off") || value.equals("false") || value.equals("disable")) {
                enabled = false;
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid value. Use 'on' or 'off'.");
                return;
            }
            
            // Set the feature
            if (service.setFeatureEnabled(featureName, enabled)) {
                sender.sendMessage(ChatColor.GREEN + "Feature '" + featureName + "' " + (enabled ? "enabled" : "disabled") + ".");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to set feature status.");
            }
        } else {
            // Just check the feature status
            boolean enabled = service.isFeatureEnabled(featureName);
            sender.sendMessage(ChatColor.GREEN + "Feature '" + featureName + "' is " + 
                             (enabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ".");
        }
    }
    
    /**
     * Version command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "version", description = "Show plugin version", aliases = {"ver"})
    public void versionCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "Example Plugin version: " + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.GREEN + "Core-Utils version: " + plugin.getCoreUtils().getDescription().getVersion());
        
        // Show SQL-Bridge version if available
        if (plugin.getSqlBridge() != null) {
            sender.sendMessage(ChatColor.GREEN + "SQL-Bridge version: " + plugin.getSqlBridge().getDescription().getVersion());
        }
    }
    
    /**
     * Database status command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "dbstatus", description = "Show SQL-Bridge status", permission = "example.db")
    public void dbStatusCommand(CommandSender sender, String[] args) {
        if (plugin.getSqlBridge() == null) {
            sender.sendMessage(ChatColor.RED + "SQL-Bridge is not available! Database functionality is disabled.");
            sender.sendMessage(ChatColor.RED + "Please install SQL-Bridge to enable database features.");
            return;
        }
        
        sender.sendMessage(ChatColor.GREEN + "=== SQL-Bridge Status ===");
        sender.sendMessage(ChatColor.YELLOW + "SQL-Bridge version: " + plugin.getSqlBridge().getDescription().getVersion());
        
        // Check if database manager is initialized
        if (plugin.getDatabaseManager().isInitialized()) {
            sender.sendMessage(ChatColor.GREEN + "Database connection: " + ChatColor.GREEN + "ACTIVE");
            sender.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.WHITE + "/exampledb" + 
                             ChatColor.YELLOW + " commands to interact with the database.");
        } else {
            sender.sendMessage(ChatColor.RED + "Database connection: " + ChatColor.RED + "INACTIVE");
            sender.sendMessage(ChatColor.RED + "Database manager is not initialized. Check your configuration.");
        }
    }
    
    @Override
    public List<String> getTabCompletions(String subCommand, CommandSender sender, String[] args) {
        // Tab completions for the feature command
        if (subCommand.equalsIgnoreCase("feature")) {
            if (args.length == 1) {
                // Return a list of feature names
                return Arrays.asList("example_feature_1", "example_feature_2", "example_feature_3", "database_integration");
            } else if (args.length == 2) {
                // Return on/off options
                return Arrays.asList("on", "off");
            }
        }
        
        return new ArrayList<>();
    }
}