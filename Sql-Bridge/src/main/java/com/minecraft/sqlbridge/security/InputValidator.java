package com.minecraft.sqlbridge.security;

import java.util.regex.Pattern;

/**
 * Provides methods to validate and sanitize input for use in database operations.
 * This helps prevent SQL injection attacks by ensuring inputs conform to expected formats.
 */
public class InputValidator {

    // Regex patterns for validation
    private static final Pattern ALPHA_PATTERN = Pattern.compile("^[a-zA-Z]+$");
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern IP_PATTERN = Pattern.compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /**
     * Validate that a string contains only alphabetic characters
     *
     * @param input The string to validate
     * @return True if valid, false otherwise
     */
    public static boolean isAlpha(String input) {
        return input != null && ALPHA_PATTERN.matcher(input).matches();
    }

    /**
     * Validate that a string contains only alphanumeric characters
     *
     * @param input The string to validate
     * @return True if valid, false otherwise
     */
    public static boolean isAlphanumeric(String input) {
        return input != null && ALPHANUMERIC_PATTERN.matcher(input).matches();
    }

    /**
     * Validate that a string is a valid SQL identifier
     * (starts with letter, contains only letters, numbers, and underscores)
     *
     * @param input The string to validate
     * @return True if valid, false otherwise
     */
    public static boolean isIdentifier(String input) {
        return input != null && IDENTIFIER_PATTERN.matcher(input).matches();
    }

    /**
     * Validate that a string is a valid integer
     *
     * @param input The string to validate
     * @return True if valid, false otherwise
     */
    public static boolean isInteger(String input) {
        if (input == null) {
            return false;
        }
        
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate that a string is a valid long integer
     *
     * @param input The string to validate
     * @return True if valid, false otherwise
     */
    public static boolean isLong(String input) {
        if (input == null) {
            return false;
        }
        
        try {
            Long.parseLong(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate that a string is a valid double
     *
     * @param input The string to validate
     * @return True if valid, false otherwise
     */
    public static boolean isDouble(String input) {
        if (input == null) {
            return false;
        }
        
        try {
            Double.parseDouble(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate that a string is a valid email address
     *
     * @param input The string to validate
     * @return True if valid, false otherwise
     */
    public static boolean isEmail(String input) {
        return input != null && EMAIL_PATTERN.matcher(input).matches();
    }

    /**
     * Validate that a string is a valid IP address
     *
     * @param input The string to validate
     * @return True if valid, false otherwise
     */
    public static boolean isIpAddress(String input) {
        return input != null && IP_PATTERN.matcher(input).matches();
    }

    /**
     * Validate that a string is a valid date in YYYY-MM-DD format
     *
     * @param input The string to validate
     * @return True if valid, false otherwise
     */
    public static boolean isDate(String input) {
        return input != null && DATE_PATTERN.matcher(input).matches();
    }

    /**
     * Validate that a string is a valid UUID
     *
     * @param input The string to validate
     * @return True if valid, false otherwise
     */
    public static boolean isUuid(String input) {
        return input != null && UUID_PATTERN.matcher(input).matches();
    }

    /**
     * Validate that a numeric value is within a specified range
     *
     * @param value The value to validate
     * @param min The minimum allowed value
     * @param max The maximum allowed value
     * @return True if within range, false otherwise
     */
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Validate that a string is not empty or null
     *
     * @param input The string to validate
     * @return True if not empty, false otherwise
     */
    public static boolean isNotEmpty(String input) {
        return input != null && !input.trim().isEmpty();
    }

    /**
     * Validate that a string has a length within specified bounds
     *
     * @param input The string to validate
     * @param minLength The minimum allowed length
     * @param maxLength The maximum allowed length
     * @return True if length is valid, false otherwise
     */
    public static boolean hasValidLength(String input, int minLength, int maxLength) {
        return input != null && input.length() >= minLength && input.length() <= maxLength;
    }

    /**
     * Validate and convert a string to an Integer
     *
     * @param input The string to validate
     * @return The Integer value, or null if invalid
     */
    public static Integer validateInteger(String input) {
        if (isInteger(input)) {
            return Integer.parseInt(input);
        }
        return null;
    }

    /**
     * Validate and convert a string to a Long
     *
     * @param input The string to validate
     * @return The Long value, or null if invalid
     */
    public static Long validateLong(String input) {
        if (isLong(input)) {
            return Long.parseLong(input);
        }
        return null;
    }

    /**
     * Validate and convert a string to a Double
     *
     * @param input The string to validate
     * @return The Double value, or null if invalid
     */
    public static Double validateDouble(String input) {
        if (isDouble(input)) {
            return Double.parseDouble(input);
        }
        return null;
    }

    /**
     * Sanitize a string for use as an SQL identifier
     *
     * @param input The string to sanitize
     * @return The sanitized string
     */
    public static String sanitizeIdentifier(String input) {
        return SqlSanitizer.sanitizeIdentifier(input);
    }

    /**
     * Sanitize a string value for use in SQL queries
     *
     * @param input The string to sanitize
     * @return The sanitized string
     */
    public static String sanitizeStringValue(String input) {
        return SqlSanitizer.sanitizeStringValue(input);
    }
}