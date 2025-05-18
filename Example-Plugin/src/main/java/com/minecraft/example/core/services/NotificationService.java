// ./Example-Plugin/src/main/java/com/minecraft/example/core/services/NotificationService.java
package com.minecraft.example.core.services;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Service interface for player notifications.
 * Demonstrates Core-Utils service registry system.
 */
public interface NotificationService {
    
    /**
     * Sends a simple message to a player.
     *
     * @param player The player
     * @param message The message
     */
    void sendMessage(Player player, String message);
    
    /**
     * Sends a message to a player with formatting.
     *
     * @param player The player
     * @param message The message
     * @param args The formatting arguments
     */
    void sendMessage(Player player, String message, Object... args);
    
    /**
     * Sends a message to a player with replacements.
     *
     * @param player The player
     * @param message The message
     * @param replacements The replacements map
     */
    void sendMessage(Player player, String message, Map<String, String> replacements);
    
    /**
     * Sends a success message to a player.
     *
     * @param player The player
     * @param message The message
     */
    void sendSuccessMessage(Player player, String message);
    
    /**
     * Sends an error message to a player.
     *
     * @param player The player
     * @param message The message
     */
    void sendErrorMessage(Player player, String message);
    
    /**
     * Sends a warning message to a player.
     *
     * @param player The player
     * @param message The message
     */
    void sendWarningMessage(Player player, String message);
    
    /**
     * Sends an info message to a player.
     *
     * @param player The player
     * @param message The message
     */
    void sendInfoMessage(Player player, String message);
    
    /**
     * Sends a message to all online players.
     *
     * @param message The message
     */
    void broadcastMessage(String message);
    
    /**
     * Sends a message to all online players with a permission.
     *
     * @param message The message
     * @param permission The permission
     */
    void broadcastMessage(String message, String permission);
    
    /**
     * Sends a title to a player.
     *
     * @param player The player
     * @param title The title
     * @param subtitle The subtitle
     * @param fadeIn The fade in time in ticks
     * @param stay The stay time in ticks
     * @param fadeOut The fade out time in ticks
     */
    void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);
    
    /**
     * Sends an achievement notification to a player.
     *
     * @param player The player
     * @param achievementName The achievement name
     * @param achievementDescription The achievement description
     */
    void sendAchievementNotification(Player player, String achievementName, String achievementDescription);
    
    /**
     * Sends a global achievement notification.
     *
     * @param player The player who earned the achievement
     * @param achievementName The achievement name
     */
    void broadcastAchievement(Player player, String achievementName);
    
    /**
     * Shows a list of items to a player with pagination.
     *
     * @param player The player
     * @param title The title
     * @param items The items
     * @param page The page number
     * @param itemsPerPage The number of items per page
     */
    void showPaginatedList(Player player, String title, List<String> items, int page, int itemsPerPage);
    
    /**
     * Shows a progress bar to a player.
     *
     * @param player The player
     * @param title The title
     * @param progress The progress (0.0 to 1.0)
     * @param length The length of the progress bar
     */
    void showProgressBar(Player player, String title, double progress, int length);
    
    /**
     * Shows a countdown to a player.
     *
     * @param player The player
     * @param title The title
     * @param seconds The number of seconds
     */
    void showCountdown(Player player, String title, int seconds);
    
    /**
     * Plays a sound to a player.
     *
     * @param player The player
     * @param sound The sound name
     * @param volume The volume
     * @param pitch The pitch
     */
    void playSound(Player player, String sound, float volume, float pitch);
    
    /**
     * Clears the chat for a player.
     *
     * @param player The player
     * @param lines The number of lines to clear
     */
    void clearChat(Player player, int lines);
}