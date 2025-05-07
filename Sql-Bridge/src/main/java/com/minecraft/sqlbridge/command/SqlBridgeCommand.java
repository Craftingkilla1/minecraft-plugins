package com.minecraft.sqlbridge.command;

import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.api.Database;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Command handler for SQL-Bridge plugin administrative commands.
 */
@Command(name = "sqlbridge", description = "SQL-Bridge administrative commands", aliases = {"sqlb", "sql"})
@Permission(value = "sqlbridge.admin", message = "You do not have permission to use this command.")
public class SqlBridgeCommand {

    private final SqlBridgePlugin plugin;

    /**
     * Create a new SQL-Bridge command handler
     *
     * @param plugin The SQL-Bridge plugin instance
     */
    public SqlBridgeCommand(SqlBridgePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle the help subcommand
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(name = "help", description = "Show help information")
    public void helpCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "=== SQL-Bridge Help ===");
        sender.sendMessage(ChatColor.YELLOW + "/sqlbridge help" + ChatColor.WHITE + " - Show this help information");
        sender.sendMessage(ChatColor.YELLOW + "/sqlbridge status" + ChatColor.WHITE + " - Check database connection status");
        sender.sendMessage(ChatColor.YELLOW + "/sqlbridge info" + ChatColor.WHITE + " - Show database information");
        sender.sendMessage(ChatColor.YELLOW + "/sqlbridge reload" + ChatColor.WHITE + " - Reload the plugin configuration");
        sender.sendMessage(ChatColor.YELLOW + "/sqlbridge list" + ChatColor.WHITE + " - List plugins using SQL-Bridge");
    }

    /**
     * Handle the status subcommand
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(name = "status", description = "Check database connection status")
    public void statusCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "Checking database connection...");
        
        // Get the database
        Database database = plugin.getServer().getServicesManager().getRegistration(Database.class).getProvider();
        
        try {
            // Attempt to get a connection
            try (Connection connection = database.getConnection()) {
                boolean valid = connection.isValid(3);
                
                if (valid) {
                    sender.sendMessage(ChatColor.GREEN + "Database connection is valid!");
                    sender.sendMessage(ChatColor.YELLOW + "Connection: " + ChatColor.WHITE + connection.getMetaData().getURL());
                    sender.sendMessage(ChatColor.YELLOW + "Database: " + ChatColor.WHITE + database.getType().name());
                } else {
                    sender.sendMessage(ChatColor.RED + "Database connection is invalid!");
                }
            }
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "Error checking database connection: " + e.getMessage());
            LogUtil.severe("Error checking database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle the info subcommand
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(name = "info", description = "Show database information")
    public void infoCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "=== Database Information ===");
        
        // Get database configuration from config
        String type = plugin.getConfig().getString("database.type", "UNKNOWN");
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String name = plugin.getConfig().getString("database.name", "minecraft");
        
        sender.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + type);
        
        // Show additional info based on database type
        if ("SQLITE".equalsIgnoreCase(type)) {
            sender.sendMessage(ChatColor.YELLOW + "File: " + ChatColor.WHITE + plugin.getDataFolder().getAbsolutePath() + "/" + name + ".db");
        } else if ("H2".equalsIgnoreCase(type)) {
            sender.sendMessage(ChatColor.YELLOW + "File: " + ChatColor.WHITE + plugin.getDataFolder().getAbsolutePath() + "/" + name);
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Host: " + ChatColor.WHITE + host + ":" + port);
            sender.sendMessage(ChatColor.YELLOW + "Database: " + ChatColor.WHITE + name);
            sender.sendMessage(ChatColor.YELLOW + "Username: " + ChatColor.WHITE + plugin.getConfig().getString("database.username", ""));
        }
        
        // Show connection pool info
        sender.sendMessage(ChatColor.YELLOW + "Pool Settings:");
        sender.sendMessage(ChatColor.YELLOW + "  Min Idle: " + ChatColor.WHITE + plugin.getConfig().getInt("pool.min-idle", 5));
        sender.sendMessage(ChatColor.YELLOW + "  Max Size: " + ChatColor.WHITE + plugin.getConfig().getInt("pool.max-size", 10));
        sender.sendMessage(ChatColor.YELLOW + "  Timeout: " + ChatColor.WHITE + plugin.getConfig().getInt("pool.timeout", 30000) + "ms");
    }

    /**
     * Handle the reload subcommand
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(name = "reload", description = "Reload the plugin configuration")
    public void reloadCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "Reloading SQL-Bridge configuration...");
        
        // Reload the configuration
        plugin.reloadConfig();
        
        // Notify the user
        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully!");
        sender.sendMessage(ChatColor.YELLOW + "Note: Some settings require a server restart to take effect.");
    }

    /**
     * Handle the list subcommand
     *
     * @param sender The command sender
     * @param args The command arguments
     */
    @SubCommand(name = "list", description = "List plugins using SQL-Bridge")
    public void listCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "=== Plugins Using SQL-Bridge ===");
        
        // Find plugins that depend on SQL-Bridge
        int count = 0;
        for (Plugin dep : plugin.getServer().getPluginManager().getPlugins()) {
            if (dep.getDescription().getDepend().contains("SqlBridge") || 
                dep.getDescription().getSoftDepend().contains("SqlBridge")) {
                
                sender.sendMessage(ChatColor.YELLOW + dep.getName() + ChatColor.WHITE + 
                    " v" + dep.getDescription().getVersion());
                count++;
            }
        }
        
        if (count == 0) {
            sender.sendMessage(ChatColor.YELLOW + "No plugins are currently using SQL-Bridge.");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Total: " + count + " plugin(s)");
        }
    }

    /**
     * Get tab completions for the command
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return List of tab completions
     */
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("help", "status", "info", "reload", "list");
        }
        
        return Arrays.asList();
    }
}