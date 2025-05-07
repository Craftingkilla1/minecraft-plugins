package com.minecraft.core.command;

import com.minecraft.core.CorePlugin;
import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.utils.LogUtil;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for plugin commands
 * Manages the registration and lookup of commands
 */
public class CommandRegistry {
    private final Plugin plugin;
    private final CommandFramework commandFramework;
    private final Map<String, CommandHandler> registeredCommands = new HashMap<>();
    
    /**
     * Create a new command registry
     * 
     * @param plugin The plugin instance
     */
    public CommandRegistry(Plugin plugin) {
        this.plugin = plugin;
        this.commandFramework = new CommandFramework(plugin);
    }
    
    /**
     * Register a command class
     * The class must be annotated with {@link Command}
     * 
     * @param commandClass The command class instance
     * @return true if the command was registered successfully
     */
    public boolean registerCommand(Object commandClass) {
        try {
            // Check if class has Command annotation
            Class<?> clazz = commandClass.getClass();
            if (!clazz.isAnnotationPresent(Command.class)) {
                LogUtil.warning("Class " + clazz.getName() + " is not annotated with @Command");
                return false;
            }
            
            // Register the command with the framework
            commandFramework.registerCommandClass(commandClass);
            
            // Get command info from annotation
            Command commandAnnotation = clazz.getAnnotation(Command.class);
            String commandName = commandAnnotation.name();
            
            // Get the handler from the framework
            CommandHandler handler = commandFramework.getCommandHandler(commandName);
            
            if (handler != null) {
                // Store the handler for later lookup
                registeredCommands.put(commandName.toLowerCase(), handler);
                
                LogUtil.info("Registered command: " + commandName);
                return true;
            } else {
                LogUtil.warning("Failed to get command handler for " + commandName);
                return false;
            }
        } catch (Exception e) {
            LogUtil.severe("Error registering command class: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Register multiple command classes
     * 
     * @param commandClasses The command class instances
     * @return The number of successfully registered commands
     */
    public int registerCommands(Object... commandClasses) {
        int count = 0;
        for (Object commandClass : commandClasses) {
            if (registerCommand(commandClass)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Get a registered command handler
     * 
     * @param commandName The command name
     * @return The command handler, or null if not found
     */
    public CommandHandler getCommandHandler(String commandName) {
        return registeredCommands.get(commandName.toLowerCase());
    }
    
    /**
     * Check if a command is registered
     * 
     * @param commandName The command name
     * @return true if the command is registered
     */
    public boolean isCommandRegistered(String commandName) {
        return registeredCommands.containsKey(commandName.toLowerCase());
    }
    
    /**
     * Unregister a command
     * 
     * @param commandName The command name
     * @return true if the command was unregistered
     */
    public boolean unregisterCommand(String commandName) {
        String name = commandName.toLowerCase();
        if (!registeredCommands.containsKey(name)) {
            return false;
        }
        
        // Remove the command from our registry
        registeredCommands.remove(name);
        
        // Get the bukkit command
        PluginCommand pluginCommand = plugin.getServer().getPluginCommand(commandName);
        if (pluginCommand != null) {
            // Reset the executor and tab completer
            pluginCommand.setExecutor(null);
            pluginCommand.setTabCompleter(null);
            LogUtil.info("Unregistered command: " + commandName);
            return true;
        }
        
        return false;
    }
    
    /**
     * Unregister all commands
     */
    public void unregisterAllCommands() {
        for (String commandName : new HashMap<>(registeredCommands).keySet()) {
            unregisterCommand(commandName);
        }
        
        // Also unregister in the framework
        commandFramework.unregisterAll();
    }
    
    /**
     * Get the command framework
     * 
     * @return The command framework
     */
    public CommandFramework getCommandFramework() {
        return commandFramework;
    }
    
    /**
     * Set a custom executor for a registered command
     * 
     * @param commandName The command name
     * @param executor The executor
     * @return true if successful
     */
    public boolean setExecutor(String commandName, CommandExecutor executor) {
        PluginCommand pluginCommand = plugin.getServer().getPluginCommand(commandName);
        if (pluginCommand != null) {
            pluginCommand.setExecutor(executor);
            return true;
        }
        return false;
    }
    
    /**
     * Set a custom tab completer for a registered command
     * 
     * @param commandName The command name
     * @param tabCompleter The tab completer
     * @return true if successful
     */
    public boolean setTabCompleter(String commandName, TabCompleter tabCompleter) {
        PluginCommand pluginCommand = plugin.getServer().getPluginCommand(commandName);
        if (pluginCommand != null) {
            pluginCommand.setTabCompleter(tabCompleter);
            return true;
        }
        return false;
    }
    
    /**
     * Get the number of registered commands
     * 
     * @return The number of registered commands
     */
    public int getCommandCount() {
        return registeredCommands.size();
    }
    
    /**
     * Check if the registry has any registered commands
     * 
     * @return true if there are registered commands
     */
    public boolean hasRegisteredCommands() {
        return !registeredCommands.isEmpty();
    }
}