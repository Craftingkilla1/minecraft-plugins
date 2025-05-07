package com.minecraft.core.command;

import com.minecraft.core.CorePlugin;
import com.minecraft.core.api.service.ServiceRegistry;
import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.utils.LogUtil;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Admin command for Core-Utils plugin
 */
@Command(name = "coreutils", description = "Core-Utils administration command", aliases = {"cu"})
@Permission("coreutils.admin")
public class CoreAdminCommand implements TabCompletionProvider {
    private final CorePlugin plugin;
    
    /**
     * Create a new admin command
     * 
     * @param plugin The plugin instance
     */
    public CoreAdminCommand(CorePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Show help command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "help", description = "Show help information", aliases = {"?"})
    public void helpCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "=== Core-Utils Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/coreutils help" + ChatColor.WHITE + " - Show this help page");
        sender.sendMessage(ChatColor.YELLOW + "/coreutils reload" + ChatColor.WHITE + " - Reload the plugin configuration");
        sender.sendMessage(ChatColor.YELLOW + "/coreutils info" + ChatColor.WHITE + " - Show plugin information");
        sender.sendMessage(ChatColor.YELLOW + "/coreutils services" + ChatColor.WHITE + " - List registered services");
        sender.sendMessage(ChatColor.YELLOW + "/coreutils debug [on|off]" + ChatColor.WHITE + " - Toggle debug mode");
        sender.sendMessage(ChatColor.YELLOW + "/coreutils version" + ChatColor.WHITE + " - Show plugin version");
    }
    
    /**
     * Reload command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "reload", description = "Reload the plugin configuration", permission = "coreutils.reload")
    public void reloadCommand(CommandSender sender, String[] args) {
        plugin.getConfigManager().reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Core-Utils configuration reloaded!");
    }
    
    /**
     * Info command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "info", description = "Show plugin information", permission = "coreutils.info")
    public void infoCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "=== Core-Utils Information ===");
        sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "Author: " + ChatColor.WHITE + String.join(", ", plugin.getDescription().getAuthors()));
        sender.sendMessage(ChatColor.YELLOW + "Website: " + ChatColor.WHITE + plugin.getDescription().getWebsite());
        sender.sendMessage(ChatColor.YELLOW + "Debug Mode: " + ChatColor.WHITE + (LogUtil.isDebugMode() ? "Enabled" : "Disabled"));
        sender.sendMessage(ChatColor.YELLOW + "Registered Commands: " + ChatColor.WHITE + plugin.getCommandRegistry().getCommandCount());
        sender.sendMessage(ChatColor.YELLOW + "Registered Services: " + ChatColor.WHITE + ServiceRegistry.getAllServices().size());
    }
    
    /**
     * Services command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "services", description = "List registered services", permission = "coreutils.services")
    public void servicesCommand(CommandSender sender, String[] args) {
        Map<Class<?>, Object> services = ServiceRegistry.getAllServices();
        
        if (services.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No services are currently registered.");
            return;
        }
        
        sender.sendMessage(ChatColor.GREEN + "=== Registered Services ===");
        
        for (Map.Entry<Class<?>, Object> entry : services.entrySet()) {
            Class<?> serviceClass = entry.getKey();
            Object implementation = entry.getValue();
            
            sender.sendMessage(ChatColor.YELLOW + serviceClass.getSimpleName() + ChatColor.WHITE + 
                            " - Implementation: " + implementation.getClass().getSimpleName());
        }
    }
    
    /**
     * Debug command
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    @SubCommand(name = "debug", description = "Toggle debug mode", permission = "coreutils.debug", minArgs = 0, maxArgs = 1)
    public void debugCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // Toggle mode
            boolean newMode = !LogUtil.isDebugMode();
            LogUtil.setDebugMode(newMode);
            sender.sendMessage(ChatColor.GREEN + "Debug mode " + (newMode ? "enabled" : "disabled") + ".");
        } else {
            // Set specific mode
            String mode = args[0].toLowerCase();
            
            if (mode.equals("on") || mode.equals("true") || mode.equals("enable")) {
                LogUtil.setDebugMode(true);
                sender.sendMessage(ChatColor.GREEN + "Debug mode enabled.");
            } else if (mode.equals("off") || mode.equals("false") || mode.equals("disable")) {
                LogUtil.setDebugMode(false);
                sender.sendMessage(ChatColor.GREEN + "Debug mode disabled.");
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid mode. Use 'on' or 'off'.");
            }
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
        sender.sendMessage(ChatColor.GREEN + "Core-Utils version: " + plugin.getDescription().getVersion());
        
        // List dependent plugins
        sender.sendMessage(ChatColor.YELLOW + "Plugins using Core-Utils:");
        boolean found = false;
        
        for (Plugin p : plugin.getServer().getPluginManager().getPlugins()) {
            if (p.equals(plugin)) continue;
            
            List<String> depend = p.getDescription().getDepend();
            List<String> softDepend = p.getDescription().getSoftDepend();
            
            if ((depend != null && depend.contains("CoreUtils")) || 
                (softDepend != null && softDepend.contains("CoreUtils"))) {
                sender.sendMessage(ChatColor.WHITE + "- " + p.getName() + " v" + p.getDescription().getVersion());
                found = true;
            }
        }
        
        if (!found) {
            sender.sendMessage(ChatColor.WHITE + "- None detected");
        }
    }
    
    @Override
    public List<String> getTabCompletions(String subCommand, CommandSender sender, String[] args) {
        if (subCommand.equalsIgnoreCase("debug") && args.length == 1) {
            return Arrays.asList("on", "off");
        }
        
        return new ArrayList<>();
    }
}