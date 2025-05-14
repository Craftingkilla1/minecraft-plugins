// ./Core-Utils/src/main/java/com/minecraft/core/utils/TimeUtil.java
package com.minecraft.core.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for time-related operations
 */
public class TimeUtil {
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_ONLY_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private TimeUtil() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Format seconds into a readable time string (e.g., 1h 30m 15s)
     * 
     * @param seconds The number of seconds as an int
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
     * Format seconds into a readable time string (e.g., 1h 30m 15s)
     * This overload handles long values for seconds
     * 
     * @param seconds The number of seconds as a long
     * @return A formatted time string
     */
    public static String formatTime(long seconds) {
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
     * 
     * @param from The start date
     * @param to The end date
     * @return A formatted time string, or "Expired" if from is after to
     */
    public static String formatTimeRemaining(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            return "Expired";
        }
        
        Duration duration = Duration.between(from, to);
        long seconds = duration.getSeconds();
        
        return formatTime(seconds);
    }
    
    /**
     * Calculate seconds between two dates
     * 
     * @param from The start date
     * @param to The end date
     * @return The number of seconds between the dates, or 0 if from is after to
     */
    public static int secondsBetween(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            return 0;
        }
        
        Duration duration = Duration.between(from, to);
        return (int) duration.getSeconds();
    }
    
    /**
     * Format a date time using the default formatter
     * 
     * @param dateTime The date time
     * @return The formatted date time
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DEFAULT_FORMATTER);
    }
    
    /**
     * Format a date time using a custom formatter
     * 
     * @param dateTime The date time
     * @param formatter The formatter
     * @return The formatted date time
     */
    public static String formatDateTime(LocalDateTime dateTime, DateTimeFormatter formatter) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(formatter);
    }
    
    /**
     * Format a date time as a date only
     * 
     * @param dateTime The date time
     * @return The formatted date
     */
    public static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DATE_ONLY_FORMATTER);
    }
    
    /**
     * Format a date time as a time only
     * 
     * @param dateTime The date time
     * @return The formatted time
     */
    public static String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(TIME_ONLY_FORMATTER);
    }
    
    /**
     * Get the current date time
     * 
     * @return The current date time
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
    
    /**
     * Add seconds to a date time
     * 
     * @param dateTime The date time
     * @param seconds The number of seconds to add
     * @return The new date time
     */
    public static LocalDateTime addSeconds(LocalDateTime dateTime, int seconds) {
        return dateTime.plusSeconds(seconds);
    }
    
    /**
     * Add minutes to a date time
     * 
     * @param dateTime The date time
     * @param minutes The number of minutes to add
     * @return The new date time
     */
    public static LocalDateTime addMinutes(LocalDateTime dateTime, int minutes) {
        return dateTime.plusMinutes(minutes);
    }
    
    /**
     * Add hours to a date time
     * 
     * @param dateTime The date time
     * @param hours The number of hours to add
     * @return The new date time
     */
    public static LocalDateTime addHours(LocalDateTime dateTime, int hours) {
        return dateTime.plusHours(hours);
    }
    
    /**
     * Add days to a date time
     * 
     * @param dateTime The date time
     * @param days The number of days to add
     * @return The new date time
     */
    public static LocalDateTime addDays(LocalDateTime dateTime, int days) {
        return dateTime.plusDays(days);
    }
}