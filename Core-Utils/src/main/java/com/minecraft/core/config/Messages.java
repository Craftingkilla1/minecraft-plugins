// ./Core-Utils/src/main/java/com/minecraft/core/config/Messages.java
package com.minecraft.core.config;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for managing plugin messages
 * Handles loading, caching, and formatting messages from the configuration
 */
public class Messages {
    private final Plugin plugin;
    private FileConfiguration config;
    private final Map<String, String> messageCache = new HashMap<>();
    private final File messagesFile;
    
    // Default message prefixes
    private String prefix = "&7[&6CoreUtils&7] ";
    private String errorPrefix = "&7[&cCoreUtils&7] &c";
    private String successPrefix = "&7[&aCoreUtils&7] &a";
    
    /**
     * Create a new messages instance
     * 
     * @param plugin The plugin instance
     */
    public Messages(Plugin plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        createDefaultMessages();
        loadMessages();
    }
    
    /**
     * Create a new messages instance with a custom file name
     * 
     * @param plugin The plugin instance
     * @param filename The filename (without .yml extension)
     */
    public Messages(Plugin plugin, String filename) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), filename + ".yml");
        
        createDefaultMessages();
        loadMessages();
    }
    
    /**
     * Create a new messages instance with a pre-loaded config
     * 
     * @param plugin The plugin instance
     * @param config The pre-loaded configuration
     * @param customPrefix Custom prefix to use (can be null for default)
     */
    public Messages(Plugin plugin, FileConfiguration config, String customPrefix) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        this.config = config;
        
        if (customPrefix != null) {
            this.prefix = customPrefix;
        }
        
        // Load prefixes from config if available
        if (config.contains("prefixes.default")) {
            prefix = config.getString("prefixes.default", prefix);
        }
        if (config.contains("prefixes.error")) {
            errorPrefix = config.getString("prefixes.error", errorPrefix);
        }
        if (config.contains("prefixes.success")) {
            successPrefix = config.getString("prefixes.success", successPrefix);
        }
        
        // Cache commonly used messages
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                messageCache.put(key, config.getString(key));
            }
        }
    }
    
    /**
     * Create a new messages instance with a pre-loaded config file
     * 
     * @param plugin The plugin instance
     * @param messagesConfig The pre-loaded configuration
     */
    public Messages(Plugin plugin, YamlConfiguration messagesConfig) {
        this(plugin, messagesConfig, null);
    }
    
    /**
     * Create the default messages file if it doesn't exist
     */
    private void createDefaultMessages() {
        if (!messagesFile.exists()) {
            try {
                String resourceName = messagesFile.getName();
                plugin.saveResource(resourceName, false);
            } catch (IllegalArgumentException e) {
                // Resource doesn't exist, create empty file
                try {
                    messagesFile.getParentFile().mkdirs();
                    messagesFile.createNewFile();
                } catch (IOException ex) {
                    plugin.getLogger().severe("Failed to create messages file: " + ex.getMessage());
                }
            }
        }
    }
    
    /**
     * Load messages from the configuration
     */
    public void loadMessages() {
        // Skip if we already have a config (from constructor with pre-loaded config)
        if (config != null) {
            return;
        }
        
        // Clear cache first
        messageCache.clear();
        
        // Load the messages file
        config = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Load prefixes
        prefix = config.getString("prefixes.default", prefix);
        errorPrefix = config.getString("prefixes.error", errorPrefix);
        successPrefix = config.getString("prefixes.success", successPrefix);
        
        // Cache commonly used messages for faster access
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                messageCache.put(key, config.getString(key));
            }
        }
    }
    
    /**
     * Save the messages configuration
     * 
     * @return true if the messages were saved successfully
     */
    public boolean saveMessages() {
        try {
            config.save(messagesFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save messages.yml: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Reload the messages configuration
     */
    public void reloadMessages() {
        loadMessages();
    }
    
    /**
     * Get a message from the configuration
     * 
     * @param path The path to the message
     * @return The message, or an error message if not found
     */
    public String getMessage(String path) {
        // Check cache first
        String message = messageCache.get(path);
        
        if (message == null) {
            // Not in cache, try loading from config
            message = config.getString(path);
            
            if (message == null) {
                // Still not found, return a placeholder
                message = "&cMissing message: " + path;
            } else {
                // Cache for next time
                messageCache.put(path, message);
            }
        }
        
        return message;
    }
    
    /**
     * Get a formatted message with replacements
     * 
     * @param path The path to the message
     * @param replacements The replacements
     * @return The formatted message
     */
    public String getFormattedMessage(String path, Map<String, String> replacements) {
        String message = getMessage(path);
        
        // Apply replacements
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        // Apply color codes
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Get a formatted message with replacements
     * 
     * @param path The path to the message
     * @param replacements The replacements (key1, value1, key2, value2, ...)
     * @return The formatted message
     */
    public String getFormattedMessage(String path, String... replacements) {
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Replacements must be pairs of key-value");
        }
        
        Map<String, String> replacementsMap = new HashMap<>();
        for (int i = 0; i < replacements.length; i += 2) {
            replacementsMap.put(replacements[i], replacements[i + 1]);
        }
        
        return getFormattedMessage(path, replacementsMap);
    }
    
    /**
     * Send a message to a player
     * 
     * @param player The player
     * @param path The path to the message
     */
    public void sendMessage(Player player, String path) {
        String message = getMessage(path);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
    
    /**
     * Send a message to a player with replacements
     * 
     * @param player The player
     * @param path The path to the message
     * @param replacements The replacements
     */
    public void sendMessage(Player player, String path, Map<String, String> replacements) {
        String message = getFormattedMessage(path, replacements);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
    
    /**
     * Send a message to a player with replacements as key-value pairs
     * 
     * @param player The player
     * @param path The path to the message
     * @param replacements The replacements (key1, value1, key2, value2, ...)
     */
    public void sendMessage(Player player, String path, String... replacements) {
        String message = getFormattedMessage(path, replacements);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
    
    /**
     * Send a message to a command sender
     * 
     * @param sender The command sender
     * @param path The path to the message
     */
    public void sendMessage(CommandSender sender, String path) {
        String message = getMessage(path);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
    
    /**
     * Send a message to a command sender with replacements
     * 
     * @param sender The command sender
     * @param path The path to the message
     * @param replacements The replacements
     */
    public void sendMessage(CommandSender sender, String path, Map<String, String> replacements) {
        String message = getFormattedMessage(path, replacements);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
    
    /**
     * Send a message to a command sender with replacements as key-value pairs
     * 
     * @param sender The command sender
     * @param path The path to the message
     * @param replacements The replacements (key1, value1, key2, value2, ...)
     */
    public void sendMessage(CommandSender sender, String path, String... replacements) {
        String message = getFormattedMessage(path, replacements);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
    
    /**
     * Send an error message to a player
     * 
     * @param player The player
     * @param path The path to the message
     */
    public void sendErrorMessage(Player player, String path) {
        String message = getMessage(path);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', errorPrefix + message));
    }
    
    /**
     * Send an error message to a player with replacements
     * 
     * @param player The player
     * @param path The path to the message
     * @param replacements The replacements (key1, value1, key2, value2, ...)
     */
    public void sendErrorMessage(Player player, String path, String... replacements) {
        String message = getFormattedMessage(path, replacements);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', errorPrefix + message));
    }
    
    /**
     * Send an error message to a command sender
     * 
     * @param sender The command sender
     * @param path The path to the message
     */
    public void sendErrorMessage(CommandSender sender, String path) {
        String message = getMessage(path);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorPrefix + message));
    }
    
    /**
     * Send an error message to a command sender with replacements
     * 
     * @param sender The command sender
     * @param path The path to the message
     * @param replacements The replacements (key1, value1, key2, value2, ...)
     */
    public void sendErrorMessage(CommandSender sender, String path, String... replacements) {
        String message = getFormattedMessage(path, replacements);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorPrefix + message));
    }
    
    /**
     * Send a success message to a player
     * 
     * @param player The player
     * @param path The path to the message
     */
    public void sendSuccessMessage(Player player, String path) {
        String message = getMessage(path);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', successPrefix + message));
    }
    
    /**
     * Send a success message to a player with replacements
     * 
     * @param player The player
     * @param path The path to the message
     * @param replacements The replacements (key1, value1, key2, value2, ...)
     */
    public void sendSuccessMessage(Player player, String path, String... replacements) {
        String message = getFormattedMessage(path, replacements);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', successPrefix + message));
    }
    
    /**
     * Send a success message to a command sender
     * 
     * @param sender The command sender
     * @param path The path to the message
     */
    public void sendSuccessMessage(CommandSender sender, String path) {
        String message = getMessage(path);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', successPrefix + message));
    }
    
    /**
     * Send a success message to a command sender with replacements
     * 
     * @param sender The command sender
     * @param path The path to the message
     * @param replacements The replacements (key1, value1, key2, value2, ...)
     */
    public void sendSuccessMessage(CommandSender sender, String path, String... replacements) {
        String message = getFormattedMessage(path, replacements);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', successPrefix + message));
    }
    
    /**
     * Set a message in the configuration
     * 
     * @param path The path to the message
     * @param message The message
     */
    public void setMessage(String path, String message) {
        config.set(path, message);
        messageCache.put(path, message);
    }
    
    /**
     * Check if a message exists
     * 
     * @param path The path to the message
     * @return true if the message exists
     */
    public boolean hasMessage(String path) {
        return messageCache.containsKey(path) || config.contains(path);
    }
    
    /**
     * Get the default prefix
     * 
     * @return The default prefix
     */
    public String getPrefix() {
        return prefix;
    }
    
    /**
     * Get the error prefix
     * 
     * @return The error prefix
     */
    public String getErrorPrefix() {
        return errorPrefix;
    }
    
    /**
     * Get the success prefix
     * 
     * @return The success prefix
     */
    public String getSuccessPrefix() {
        return successPrefix;
    }
    
    /**
     * Get the underlying configuration
     * 
     * @return The configuration
     */
    public FileConfiguration getConfig() {
        return config;
    }
}