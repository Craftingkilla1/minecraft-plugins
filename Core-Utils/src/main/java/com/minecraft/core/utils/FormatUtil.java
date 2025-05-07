package com.minecraft.invitesystem.utils;

import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for formatting text, numbers, and time
 */
public class FormatUtil {
    private static final DecimalFormat POINTS_FORMAT = new DecimalFormat("#,##0.##");
    private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#0.##%");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    private FormatUtil() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Format a message with color codes
     * @param message The message to format
     * @return The formatted message
     */
    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Format a number with commas and up to 2 decimal places
     * @param number The number to format
     * @return The formatted number
     */
    public static String formatNumber(double number) {
        return POINTS_FORMAT.format(number);
    }
    
    /**
     * Format a decimal as a percentage
     * @param decimal The decimal to format (e.g., 0.75)
     * @return The formatted percentage (e.g., 75%)
     */
    public static String formatPercentage(double decimal) {
        return PERCENTAGE_FORMAT.format(decimal);
    }
    
    /**
     * Format seconds into a readable time string (e.g., 1h 30m 15s)
     * @param seconds The number of seconds
     * @return A formatted time string
     */
    public static String formatTime(int seconds) {
        if (seconds < 0) {
            return "0s";
        }
        
        long hours = TimeUnit.SECONDS.toHours(seconds);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
        long secs = seconds % 60;
        
        StringBuilder builder = new StringBuilder();
        if (hours > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            builder.append(minutes).append("m ");
        }
        builder.append(secs).append("s");
        
        return builder.toString();
    }
    
    /**
     * Format a duration between two dates as time remaining
     * @param from The start date
     * @param to The end date
     * @return A formatted time string
     */
    public static String formatTimeRemaining(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            return "Expired";
        }
        
        Duration duration = Duration.between(from, to);
        long seconds = duration.getSeconds();
        
        return formatTime((int) seconds);
    }
    
    /**
     * Format a date and time
     * @param dateTime The date time to format
     * @return The formatted date time
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }
    
    /**
     * Format a plural word based on count
     * @param count The count
     * @param singular The singular form
     * @param plural The plural form
     * @return The appropriate form
     */
    public static String pluralize(int count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }
    
    /**
     * Format a boolean as Yes/No
     * @param value The boolean value
     * @return "Yes" if true, "No" if false
     */
    public static String formatBoolean(boolean value) {
        return value ? "Yes" : "No";
    }
    
    /**
     * Remove color codes from a string
     * @param message The message with color codes
     * @return The message without color codes
     */
    public static String stripColor(String message) {
        return ChatColor.stripColor(message);
    }
    
    /**
     * Truncate a string if it's longer than a specified length
     * @param text The text to truncate
     * @param maxLength The maximum length
     * @return The truncated text
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}