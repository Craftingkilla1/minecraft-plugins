// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/command/SqlBridgeCommand.java
package com.minecraft.sqlbridge.command;

import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.command.TabCompletionProvider;
import com.minecraft.core.utils.FormatUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.DatabaseService;
import com.minecraft.sqlbridge.migration.SchemaVersion;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Admin command for the SQL-Bridge plugin.
 */
@Command(name = "sqlbridge", description = "Admin commands for the SQL-Bridge plugin", aliases = {"sqlb"})
@Permission(value = "sqlbridge.admin", message = "You do not have permission to use SQL-Bridge admin commands.")
public class SqlBridgeCommand implements TabCompletionProvider {
    
    private final SqlBridgePlugin plugin;
    
    /**
     * Constructor for SqlBridgeCommand.
     *
     * @param plugin The SQL-Bridge plugin instance
     */
    public SqlBridgeCommand(SqlBridgePlugin plugin) {
        this.plugin = plugin;
    }
    
    @SubCommand(name = "info", description = "Show information about the SQL-Bridge plugin")
    @Permission("sqlbridge.info")
    public void infoCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "=== SQL-Bridge Information ===");
        sender.sendMessage(ChatColor.GREEN + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.GREEN + "Database Type: " + ChatColor.WHITE + plugin.getPluginConfig().getDatabaseType());
        
        DatabaseService databaseService = plugin.getDatabaseService();
        Map<String, Object> stats = databaseService.getStatistics();
        
        sender.sendMessage(ChatColor.GREEN + "Active Connections: " + 
                          ChatColor.WHITE + stats.getOrDefault("connectionPoolCount", "N/A"));
        
        if (stats.containsKey("totalQueries")) {
            sender.sendMessage(ChatColor.GREEN + "Total Queries: " + 
                              ChatColor.WHITE + stats.get("totalQueries"));
        }
        
        if (stats.containsKey("averageQueryTime")) {
            sender.sendMessage(ChatColor.GREEN + "Average Query Time: " + 
                              ChatColor.WHITE + stats.get("averageQueryTime") + "ms");
        }
        
        sender.sendMessage(ChatColor.GREEN + "BungeeSupport: " + 
                          ChatColor.WHITE + (plugin.getPluginConfig().isBungeeEnabled() ? "Enabled" : "Disabled"));
        
        if (plugin.getPluginConfig().isBungeeEnabled() && plugin.getPluginConfig().isSharedDatabaseEnabled()) {
            sender.sendMessage(ChatColor.GREEN + "Shared Database: " + 
                              ChatColor.WHITE + "Enabled");
        }
    }
    
    @SubCommand(name = "status", description = "Show the status of the database connections")
    @Permission("sqlbridge.status")
    public void statusCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "=== SQL-Bridge Connection Status ===");
        
        Map<String, Object> stats = plugin.getConnectionManager().getStatistics();
        
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (key.contains(".")) {
                String[] parts = key.split("\\.", 2);
                String connectionName = parts[0];
                String statName = parts[1];
                
                // Format nicely
                if (statName.equals("activeConnections")) {
                    sender.sendMessage(ChatColor.GREEN + connectionName + ": " + 
                                      ChatColor.WHITE + value + " active connections");
                } else if (statName.equals("idleConnections")) {
                    sender.sendMessage(ChatColor.GREEN + connectionName + ": " + 
                                      ChatColor.WHITE + value + " idle connections");
                } else if (statName.equals("totalConnections")) {
                    sender.sendMessage(ChatColor.GREEN + connectionName + ": " + 
                                      ChatColor.WHITE + value + " total connections");
                } else if (statName.equals("threadsAwaitingConnection")) {
                    sender.sendMessage(ChatColor.GREEN + connectionName + ": " + 
                                      ChatColor.WHITE + value + " threads waiting");
                }
            }
        }
        
        // Show connection validity
        sender.sendMessage(ChatColor.GOLD + "Connection Validity:");
        Database database = plugin.getDatabaseService().getDatabase();
        boolean isValid = database.isConnectionValid();
        
        sender.sendMessage(ChatColor.GREEN + "Main Connection: " + 
                          (isValid ? ChatColor.GREEN + "Valid" : ChatColor.RED + "Invalid"));
    }
    
    @SubCommand(name = "reload", description = "Reload the SQL-Bridge configuration")
    @Permission("sqlbridge.reload")
    public void reloadCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "Reloading SQL-Bridge configuration...");
        
        try {
            plugin.reloadConfig();
            plugin.getPluginConfig().reload();
            
            sender.sendMessage(ChatColor.GREEN + "SQL-Bridge configuration reloaded successfully.");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error reloading configuration: " + e.getMessage());
            plugin.getLogger().severe("Error reloading configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @SubCommand(name = "migrations", description = "Show migration status for plugins")
    @Permission("sqlbridge.migrations")
    public void migrationsCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "=== SQL-Bridge Migration Status ===");
        
        Map<String, List<SchemaVersion>> allVersions = plugin.getMigrationManager().getAllSchemaVersions();
        
        if (allVersions.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No migrations have been applied yet.");
            return;
        }
        
        for (Map.Entry<String, List<SchemaVersion>> entry : allVersions.entrySet()) {
            String pluginName = entry.getKey();
            List<SchemaVersion> versions = entry.getValue();
            
            if (!versions.isEmpty()) {
                SchemaVersion latestVersion = versions.get(versions.size() - 1);
                
                sender.sendMessage(ChatColor.GREEN + pluginName + ": " + 
                                  ChatColor.WHITE + "v" + latestVersion.getVersion() + 
                                  ChatColor.GRAY + " (" + versions.size() + " migrations applied)");
            }
        }
    }
    
    @SubCommand(name = "test", description = "Test the database connection", permission = "sqlbridge.test")
    public void testCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "Testing database connection...");
        
        try {
            Database database = plugin.getDatabaseService().getDatabase();
            
            try (Connection conn = database.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                
                if (rs.next()) {
                    sender.sendMessage(ChatColor.GREEN + "Database connection is working properly!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to execute test query.");
                }
            }
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "Error testing database connection: " + e.getMessage());
            plugin.getLogger().severe("Error testing database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @SubCommand(name = "errors", description = "Show recent error logs", permission = "sqlbridge.errors")
    public void errorsCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "=== SQL-Bridge Error Log ===");
        
        List<String> errorLog = plugin.getErrorHandler().getErrorLog();
        
        if (errorLog.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "No errors logged.");
            return;
        }
        
        // Show the last 10 errors (or fewer if there are fewer)
        int startIndex = Math.max(0, errorLog.size() - 10);
        for (int i = startIndex; i < errorLog.size(); i++) {
            sender.sendMessage(ChatColor.RED + errorLog.get(i));
        }
        
        if (errorLog.size() > 10) {
            sender.sendMessage(ChatColor.YELLOW + "Showing " + (errorLog.size() - startIndex) + 
                             " of " + errorLog.size() + " errors. Use '/sqlbridge errors clear' to clear.");
        }
    }
    
    @SubCommand(name = "errors clear", description = "Clear error logs", permission = "sqlbridge.errors.clear")
    public void errorsClearCommand(CommandSender sender, String[] args) {
        plugin.getErrorHandler().clearErrorLog();
        sender.sendMessage(ChatColor.GREEN + "Error log cleared.");
    }
    
    @Override
    public List<String> getTabCompletions(String subCommand, CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (subCommand.isEmpty() && args.length == 1) {
            // First argument - suggest subcommands
            String arg = args[0].toLowerCase();
            
            List<String> commands = Arrays.asList("info", "status", "reload", "migrations", "test", "errors");
            
            for (String cmd : commands) {
                if (cmd.startsWith(arg) && sender.hasPermission("sqlbridge." + cmd)) {
                    completions.add(cmd);
                }
            }
        } else if (subCommand.equalsIgnoreCase("errors") && args.length == 1) {
            // Suggest 'clear' for the errors subcommand
            String arg = args[0].toLowerCase();
            if ("clear".startsWith(arg) && sender.hasPermission("sqlbridge.errors.clear")) {
                completions.add("clear");
            }
        }
        
        return completions;
    }
}