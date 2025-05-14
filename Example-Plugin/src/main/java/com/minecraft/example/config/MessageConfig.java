// ./Example-Plugin/src/main/java/com/minecraft/example/config/MessageConfig.java
package com.minecraft.example.config;

import com.minecraft.core.config.ConfigManager;
import com.minecraft.core.config.Messages;
import com.minecraft.core.utils.FormatUtil;
import com.minecraft.example.PlayerStatsPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the plugin's messages.
 */
public class MessageConfig {
    
    private final PlayerStatsPlugin plugin;
    private final ConfigManager configManager;
    private YamlConfiguration messagesConfig;
    private Messages messages;
    
    /**
     * Creates a new MessageConfig instance.
     *
     * @param plugin The plugin instance
     * @param configManager The config manager
     */
    public MessageConfig(PlayerStatsPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        loadMessages();
    }
    
    /**
     * Loads or reloads the messages configuration.
     */
    public void loadMessages() {
        // Ensure messages.yml exists (will create default if it doesn't)
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        // Load the messages configuration
        this.messagesConfig = configManager.loadConfigFile("messages");
        this.messages = configManager.createMessages("messages");
    }
    
    /**
     * Sends a message to a player.
     *
     * @param player The player
     * @param key The message key
     */
    public void sendMessage(Player player, String key) {
        messages.sendMessage(player, key);
    }
    
    /**
     * Sends a message to a player with placeholders.
     *
     * @param player The player
     * @param key The message key
     * @param replacements The placeholder replacements (key1, value1, key2, value2, ...)
     */
    public void sendMessage(Player player, String key, String... replacements) {
        messages.sendMessage(player, key, replacements);
    }
    
    /**
     * Sends a message to a command sender.
     *
     * @param sender The command sender
     * @param key The message key
     */
    public void sendMessage(CommandSender sender, String key) {
        String message = getMessage(key);
        sender.sendMessage(message);
    }
    
    /**
     * Sends a message to a command sender with placeholders.
     *
     * @param sender The command sender
     * @param key The message key
     * @param replacements The placeholder replacements (key1, value1, key2, value2, ...)
     */
    public void sendMessage(CommandSender sender, String key, String... replacements) {
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Replacements must be in pairs (key, value)");
        }
        
        Map<String, String> replacementMap = new HashMap<>();
        for (int i = 0; i < replacements.length; i += 2) {
            replacementMap.put(replacements[i], replacements[i + 1]);
        }
        
        String message = getMessage(key, replacementMap);
        sender.sendMessage(message);
    }
    
    /**
     * Sends a success message to a player.
     *
     * @param player The player
     * @param key The message key
     * @param replacements The placeholder replacements (key1, value1, key2, value2, ...)
     */
    public void sendSuccessMessage(Player player, String key, String... replacements) {
        messages.sendSuccessMessage(player, key, replacements);
    }
    
    /**
     * Sends an error message to a player.
     *
     * @param player The player
     * @param key The message key
     * @param replacements The placeholder replacements (key1, value1, key2, value2, ...)
     */
    public void sendErrorMessage(Player player, String key, String... replacements) {
        messages.sendErrorMessage(player, key, replacements);
    }
    
    /**
     * Gets a message.
     *
     * @param key The message key
     * @return The formatted message
     */
    public String getMessage(String key) {
        String message = messagesConfig.getString(key);
        if (message == null) {
            return "Missing message: " + key;
        }
        
        return FormatUtil.color(message);
    }
    
    /**
     * Gets a message with placeholders.
     *
     * @param key The message key
     * @param replacements The placeholder replacements
     * @return The formatted message
     */
    public String getMessage(String key, Map<String, String> replacements) {
        String message = getMessage(key);
        
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return message;
    }
    
    /**
     * Gets the Messages instance for direct usage.
     *
     * @return The Messages instance
     */
    public Messages getMessages() {
        return messages;
    }
}