package com.minecraft.core.command;

import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.utils.LogUtil;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Command framework for registering and handling commands with annotations
 */
public class CommandFramework {
    private final Plugin plugin;
    private final Map<String, CommandHandler> commandHandlers = new HashMap<>();
    
    /**
     * Create a new command framework
     * @param plugin The plugin instance
     */
    public CommandFramework(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Register a command class
     * @param commandClass The command class instance
     */
    public void registerCommandClass(Object commandClass) {
        Class<?> clazz = commandClass.getClass();
        
        // Check if class has Command annotation
        if (!clazz.isAnnotationPresent(Command.class)) {
            LogUtil.warning("Class " + clazz.getName() + " is not annotated with @Command");
            return;
        }
        
        // Get command info from annotation
        Command commandAnnotation = clazz.getAnnotation(Command.class);
        String commandName = commandAnnotation.name();
        
        // Create command handler
        CommandHandler handler = new CommandHandler(plugin, commandClass, commandAnnotation);
        commandHandlers.put(commandName, handler);
        
        // Find all methods with SubCommand annotation
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(SubCommand.class)) {
                registerSubCommand(handler, method);
            }
        }
        
        // Register command executor and tab completer
        PluginCommand pluginCommand = plugin.getServer().getPluginCommand(commandName);
        if (pluginCommand != null) {
            pluginCommand.setExecutor(handler);
            pluginCommand.setTabCompleter(handler);
            
            // Set command description and usage from annotation
            if (!commandAnnotation.description().isEmpty()) {
                pluginCommand.setDescription(commandAnnotation.description());
            }
            
            if (!commandAnnotation.usage().isEmpty()) {
                pluginCommand.setUsage(commandAnnotation.usage());
            }
            
            if (clazz.isAnnotationPresent(Permission.class)) {
                Permission permissionAnnotation = clazz.getAnnotation(Permission.class);
                pluginCommand.setPermission(permissionAnnotation.value());
            }
            
            LogUtil.info("Registered command: " + commandName);
        } else {
            LogUtil.severe("Failed to register command: " + commandName + " - not found in plugin.yml");
        }
    }
    
    /**
     * Register a subcommand method
     * @param handler The command handler
     * @param method The method with SubCommand annotation
     */
    private void registerSubCommand(CommandHandler handler, Method method) {
        SubCommand subCommandAnnotation = method.getAnnotation(SubCommand.class);
        String subCommandName = subCommandAnnotation.name();
        
        // Validate method signature
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 2 || 
            !CommandSender.class.isAssignableFrom(paramTypes[0]) || 
            !paramTypes[1].equals(String[].class)) {
            
            LogUtil.warning("Invalid SubCommand method signature: " + method.getName() + 
                          " in " + method.getDeclaringClass().getName());
            LogUtil.warning("SubCommand methods must have signature: void method(CommandSender, String[])");
            return;
        }
        
        // Register the subcommand
        handler.registerSubCommand(subCommandName, method, subCommandAnnotation);
        LogUtil.debug("Registered subcommand: " + subCommandName + " for command: " + handler.getCommandName());
    }
    
    /**
     * Check if a command exists
     * @param commandName The command name
     * @return true if the command exists
     */
    public boolean hasCommand(String commandName) {
        return commandHandlers.containsKey(commandName);
    }
    
    /**
     * Get a command handler
     * @param commandName The command name
     * @return The command handler, or null if not found
     */
    public CommandHandler getCommandHandler(String commandName) {
        return commandHandlers.get(commandName);
    }
    
    /**
     * Unregister all commands
     */
    public void unregisterAll() {
        for (String commandName : commandHandlers.keySet()) {
            PluginCommand pluginCommand = plugin.getServer().getPluginCommand(commandName);
            if (pluginCommand != null) {
                pluginCommand.setExecutor(null);
                pluginCommand.setTabCompleter(null);
            }
        }
        
        commandHandlers.clear();
        LogUtil.info("Unregistered all commands");
    }
}