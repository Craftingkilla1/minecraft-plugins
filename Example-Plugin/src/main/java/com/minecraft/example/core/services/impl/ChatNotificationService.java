// ./Example-Plugin/src/main/java/com/minecraft/example/core/services/impl/ChatNotificationService.java
package com.minecraft.example.core.services.impl;

import com.minecraft.core.utils.FormatUtil;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.config.ConfigManager;
import com.minecraft.example.core.services.NotificationService;
import com.minecraft.example.core.utils.MapUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for sending notifications to players.
 */
public class ChatNotificationService implements NotificationService {
    
    private final Plugin plugin;
    private final ConfigManager configManager;
    
    // Stores cooldowns for notifications to prevent spamming
    private final Map<UUID, Map<String, Long>> notificationCooldowns = new ConcurrentHashMap<>();
    
    /**
     * Creates a new ChatNotificationService instance.
     *
     * @param plugin The plugin instance
     * @param configManager The config manager
     */
    public ChatNotificationService(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    @Override
    public void sendMessage(Player player, String key) {
        String message = configManager.getMessages().getMessage(key);
        if (message != null && !message.isEmpty()) {
            player.sendMessage(message);
        }
    }
    
    @Override
    public void sendMessage(Player player, String key, Map<String, String> replacements) {
        String message = configManager.getMessages().getMessage(key);
        if (message != null && !message.isEmpty()) {
            // Apply replacements
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            
            player.sendMessage(message);
        }
    }
    
    @Override
    public void broadcastMessage(String key) {
        String message = configManager.getMessages().getMessage(key);
        if (message != null && !message.isEmpty()) {
            plugin.getServer().broadcastMessage(message);
        }
    }
    
    @Override
    public void broadcastMessage(String key, Map<String, String> replacements) {
        String message = configManager.getMessages().getMessage(key);
        if (message != null && !message.isEmpty()) {
            // Apply replacements
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            
            plugin.getServer().broadcastMessage(message);
        }
    }
    
    @Override
    public void sendSuccessMessage(Player player, String key) {
        String message = configManager.getMessages().getMessage(key);
        if (message != null && !message.isEmpty()) {
            // Add success prefix
            String prefix = ChatColor.GREEN + "✓ ";
            player.sendMessage(prefix + message);
        }
    }
    
    @Override
    public void sendSuccessMessage(Player player, String key, Map<String, String> replacements) {
        String message = configManager.getMessages().getMessage(key);
        if (message != null && !message.isEmpty()) {
            // Apply replacements
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            
            // Add success prefix
            String prefix = ChatColor.GREEN + "✓ ";
            player.sendMessage(prefix + message);
        }
    }
    
    @Override
    public void sendErrorMessage(Player player, String key) {
        String message = configManager.getMessages().getMessage(key);
        if (message != null && !message.isEmpty()) {
            // Add error prefix
            String prefix = ChatColor.RED + "✗ ";
            player.sendMessage(prefix + message);
        }
    }
    
    @Override
    public void sendErrorMessage(Player player, String key, Map<String, String> replacements) {
        String message = configManager.getMessages().getMessage(key);
        if (message != null && !message.isEmpty()) {
            // Apply replacements
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            
            // Add error prefix
            String prefix = ChatColor.RED + "✗ ";
            player.sendMessage(prefix + message);
        }
    }
    
    @Override
    public void sendAchievementMessage(Player player, String achievementName) {
        // Check cooldown
        if (isOnCooldown(player.getUniqueId(), "achievement", 5000)) {
            return;
        }
        
        // Use our MapUtil.of() instead of Map.of()
        Map<String, String> replacements = MapUtil.of("achievement", achievementName);
        
        String message = configManager.getMessages().getMessage("achievements.earned");
        if (message != null && !message.isEmpty()) {
            // Apply replacements
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            
            // Format and send message
            message = FormatUtil.color(message);
            player.sendMessage(message);
            
            // Play achievement sound
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }
    
    @Override
    public void broadcastAchievementMessage(Player player, String achievementName, boolean isSecret) {
        if (isSecret) {
            // Don't broadcast secret achievements
            return;
        }
        
        // Use our MapUtil.of() instead of Map.of()
        Map<String, String> replacements = MapUtil.of(
                "player", player.getName(),
                "achievement", achievementName
        );
        
        String message = configManager.getMessages().getMessage("achievements.broadcast");
        if (message != null && !message.isEmpty()) {
            // Apply replacements
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            
            // Format and broadcast message
            message = FormatUtil.color(message);
            
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (!online.equals(player)) {
                    online.sendMessage(message);
                }
            }
        }
    }
    
    /**
     * Checks if a notification is on cooldown.
     *
     * @param uuid The player UUID
     * @param type The notification type
     * @param cooldownMs The cooldown in milliseconds
     * @return True if on cooldown, false otherwise
     */
    private boolean isOnCooldown(UUID uuid, String type, long cooldownMs) {
        Map<String, Long> cooldowns = notificationCooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
        long now = System.currentTimeMillis();
        long lastNotification = cooldowns.getOrDefault(type, 0L);
        
        if (now - lastNotification < cooldownMs) {
            return true;
        }
        
        cooldowns.put(type, now);
        return false;
    }
    
    /**
     * Clears cooldowns for a player.
     *
     * @param uuid The player UUID
     */
    public void clearCooldowns(UUID uuid) {
        notificationCooldowns.remove(uuid);
    }
}