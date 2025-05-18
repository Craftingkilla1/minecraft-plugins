// ./Core-Utils/src/main/java/com/minecraft/core/command/CommandHandler.java
package com.minecraft.core.command;

import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.utils.LogUtil;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Handler for a specific command and its subcommands
 */
public class CommandHandler implements CommandExecutor, TabCompleter {
    private final Plugin plugin;
    private final Object commandInstance;
    private final Command commandAnnotation;
    private final Map<String, SubCommandInfo> subCommands = new LinkedHashMap<>();
    private final Map<String, String> subCommandAliases = new HashMap<>();
    private SubCommandInfo defaultSubCommand = null;
    
    /**
     * Creates a new command handler
     * 
     * @param plugin The plugin instance
     * @param commandInstance The command class instance
     * @param commandAnnotation The Command annotation
     */
    public CommandHandler(Plugin plugin, Object commandInstance, Command commandAnnotation) {
        this.plugin = plugin;
        this.commandInstance = commandInstance;
        this.commandAnnotation = commandAnnotation;
    }
    
    /**
     * Register a subcommand
     * 
     * @param name The subcommand name
     * @param method The method to invoke
     * @param annotation The SubCommand annotation
     */
    public void registerSubCommand(String name, Method method, SubCommand annotation) {
        SubCommandInfo info = new SubCommandInfo(name, method, annotation);
        
        // Handle default subcommand
        if (annotation.isDefault() || name.isEmpty()) {
            defaultSubCommand = info;
            LogUtil.debug("Registered default subcommand for command: " + commandAnnotation.name());
        }
        
        // Only register in the map if it has a name
        if (!name.isEmpty()) {
            subCommands.put(name.toLowerCase(), info);
            
            // Register aliases
            for (String alias : annotation.aliases()) {
                subCommandAliases.put(alias.toLowerCase(), name.toLowerCase());
            }
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        SubCommandInfo subCommand = null;
        String[] subArgs;
        
        // If no args provided, either use default command or show help
        if (args.length == 0) {
            if (defaultSubCommand != null) {
                subCommand = defaultSubCommand;
                subArgs = args;
            } else {
                showHelp(sender);
                return true;
            }
        } else {
            String subCommandName = args[0].toLowerCase();
            
            // Check for aliases
            if (subCommandAliases.containsKey(subCommandName)) {
                subCommandName = subCommandAliases.get(subCommandName);
            }
            
            // Get the subcommand
            subCommand = subCommands.get(subCommandName);
            if (subCommand == null) {
                // If no matching subcommand found, try the default command if it exists
                if (defaultSubCommand != null) {
                    subCommand = defaultSubCommand;
                    subArgs = args;
                } else {
                    showHelp(sender);
                    return true;
                }
            } else {
                subArgs = Arrays.copyOfRange(args, 1, args.length);
            }
        }

        // Check permission
        if (!hasPermission(sender, subCommand)) {
            String message = "";
            
            // Check for Permission annotation on method
            Permission methodPermission = subCommand.method.getAnnotation(Permission.class);
            if (methodPermission != null) {
                message = methodPermission.message();
            } else {
                message = "You don't have permission to use this command.";
            }
            
            sender.sendMessage(ChatColor.RED + message);
            return true;
        }
        
        // Check arg count
        if (subArgs.length < subCommand.annotation.minArgs()) {
            sender.sendMessage(ChatColor.RED + "Not enough arguments. Usage: " + getUsage(subCommand));
            return true;
        }
        
        if (subCommand.annotation.maxArgs() >= 0 && subArgs.length > subCommand.annotation.maxArgs()) {
            sender.sendMessage(ChatColor.RED + "Too many arguments. Usage: " + getUsage(subCommand));
            return true;
        }
        
        // Execute the command
        try {
            subCommand.method.invoke(commandInstance, sender, subArgs);
        } catch (InvocationTargetException e) {
            LogUtil.severe("Error executing command: " + (subCommand.name.isEmpty() ? "(default)" : subCommand.name));
            LogUtil.severe("Cause: " + e.getCause().getMessage());
            e.getCause().printStackTrace();
            sender.sendMessage(ChatColor.RED + "An error occurred while executing the command.");
        } catch (Exception e) {
            LogUtil.severe("Error executing command: " + (subCommand.name.isEmpty() ? "(default)" : subCommand.name));
            LogUtil.severe("Error: " + e.getMessage());
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "An error occurred while executing the command.");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Complete subcommand names and aliases
            String partial = args[0].toLowerCase();
            
            // Add subcommands the player has permission to use
            for (SubCommandInfo subCommand : subCommands.values()) {
                if (hasPermission(sender, subCommand) && 
                    subCommand.name.toLowerCase().startsWith(partial)) {
                    completions.add(subCommand.name);
                }
            }
            
            // Add aliases
            for (Map.Entry<String, String> aliasEntry : subCommandAliases.entrySet()) {
                if (aliasEntry.getKey().startsWith(partial)) {
                    SubCommandInfo subCommand = subCommands.get(aliasEntry.getValue());
                    if (subCommand != null && hasPermission(sender, subCommand)) {
                        completions.add(aliasEntry.getKey());
                    }
                }
            }
        } else if (args.length > 1) {
            // Get the subcommand
            String subCommandName = args[0].toLowerCase();
            
            // Check for aliases
            if (subCommandAliases.containsKey(subCommandName)) {
                subCommandName = subCommandAliases.get(subCommandName);
            }
            
            SubCommandInfo subCommand = subCommands.get(subCommandName);
            if (subCommand != null && hasPermission(sender, subCommand)) {
                // Let the command class provide tab completions
                if (commandInstance instanceof TabCompletionProvider) {
                    TabCompletionProvider provider = (TabCompletionProvider) commandInstance;
                    
                    List<String> providedCompletions = provider.getTabCompletions(
                        subCommand.name, 
                        sender, 
                        Arrays.copyOfRange(args, 1, args.length)
                    );
                    
                    if (providedCompletions != null) {
                        completions.addAll(providedCompletions);
                    }
                }
            } else if (defaultSubCommand != null && hasPermission(sender, defaultSubCommand)) {
                // Try tab completions for default command
                if (commandInstance instanceof TabCompletionProvider) {
                    TabCompletionProvider provider = (TabCompletionProvider) commandInstance;
                    
                    List<String> providedCompletions = provider.getTabCompletions(
                        "", 
                        sender, 
                        args
                    );
                    
                    if (providedCompletions != null) {
                        completions.addAll(providedCompletions);
                    }
                }
            }
        }
        
        return completions;
    }
    
    /**
     * Show help for the command
     * 
     * @param sender The command sender
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== " + commandAnnotation.name() + " Commands ===");
        
        // If there's a default command, show it first
        if (defaultSubCommand != null && hasPermission(sender, defaultSubCommand)) {
            String description = defaultSubCommand.annotation.description();
            if (description.isEmpty()) {
                description = "Default command";
            }
            
            sender.sendMessage(ChatColor.YELLOW + "/" + commandAnnotation.name() + " " + 
                             getUsage(defaultSubCommand) +
                             ChatColor.WHITE + " - " + description);
        }
        
        for (SubCommandInfo subCommand : subCommands.values()) {
            if (hasPermission(sender, subCommand)) {
                String description = subCommand.annotation.description();
                if (description.isEmpty()) {
                    description = "No description available";
                }
                
                sender.sendMessage(ChatColor.YELLOW + "/" + commandAnnotation.name() + " " + 
                                 subCommand.name + " " + getUsage(subCommand) +
                                 ChatColor.WHITE + " - " + description);
            }
        }
    }
    
    /**
     * Get the usage string for a subcommand
     * 
     * @param subCommand The subcommand info
     * @return The usage string
     */
    private String getUsage(SubCommandInfo subCommand) {
        String usage = subCommand.annotation.usage();
        if (usage.isEmpty()) {
            return "";
        }
        return usage;
    }
    
    /**
     * Check if a sender has permission for a subcommand
     * 
     * @param sender The command sender
     * @param subCommand The subcommand info
     * @return true if the sender has permission
     */
    private boolean hasPermission(CommandSender sender, SubCommandInfo subCommand) {
        // Console always has permission
        if (!(sender instanceof Player)) {
            return true;
        }
        
        // Check permission from SubCommand annotation
        String permission = subCommand.annotation.permission();
        if (!permission.isEmpty()) {
            return sender.hasPermission(permission);
        }
        
        // Check for Permission annotation on method
        Permission methodPermission = subCommand.method.getAnnotation(Permission.class);
        if (methodPermission != null) {
            return sender.hasPermission(methodPermission.value());
        }
        
        // No specified permission, allow by default
        return true;
    }
    
    /**
     * Get the command name
     * 
     * @return The command name
     */
    public String getCommandName() {
        return commandAnnotation.name();
    }
    
    /**
     * Check if a default subcommand is registered
     * 
     * @return true if a default subcommand is registered
     */
    public boolean hasDefaultSubCommand() {
        return defaultSubCommand != null;
    }
    
    /**
     * Class to hold information about a subcommand
     */
    private static class SubCommandInfo {
        private final String name;
        private final Method method;
        private final SubCommand annotation;
        
        public SubCommandInfo(String name, Method method, SubCommand annotation) {
            this.name = name;
            this.method = method;
            this.annotation = annotation;
        }
    }
}