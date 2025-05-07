package com.minecraft.core.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Interface for providing tab completions for commands
 */
public interface TabCompletionProvider {
    
    /**
     * Get tab completions for a subcommand
     * 
     * @param subCommand The subcommand name
     * @param sender The command sender
     * @param args The command arguments (without the subcommand)
     * @return List of completions, or null if none
     */
    List<String> getTabCompletions(String subCommand, CommandSender sender, String[] args);
}