package com.minecraft.sqlbridge.utils;

import com.minecraft.core.utils.TimeUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Extended time utility methods that build upon Core-Utils TimeUtil
 */
public class TimeUtilExtended {

    /**
     * Convert milliseconds since epoch to LocalDateTime
     *
     * @param millis Milliseconds since epoch
     * @return The corresponding LocalDateTime
     */
    public static LocalDateTime fromMillis(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }
    
    /**
     * Format a timestamp in milliseconds to a date-time string
     * 
     * @param millis Milliseconds since epoch
     * @return Formatted date-time string
     */
    public static String formatFromMillis(long millis) {
        return TimeUtil.formatDateTime(fromMillis(millis));
    }
    
    // You can add other extension methods here
}