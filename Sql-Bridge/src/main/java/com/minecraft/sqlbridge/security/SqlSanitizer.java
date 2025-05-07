package com.minecraft.sqlbridge.security;

import com.minecraft.core.utils.LogUtil;
import java.util.regex.Pattern;

/**
 * Provides methods to sanitize SQL inputs and prevent SQL injection attacks.
 */
public class SqlSanitizer {

    // Regex patterns for identifying SQL injection attempts
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "('(''|[^'])*')|" +          // Single quoted strings
            "(;\\s*(--).*)|" +           // SQL comment after semicolon
            "(;\\s*(/\\*.*?\\*/))|" +    // SQL block comment after semicolon
            "(--\\s+)|" +                // Single-line comment
            "(/\\*.*?\\*/)|" +           // Block comment
            "(\\b(select|update|insert|delete|drop|alter|exec|execute|union|create|where)\\b)", 
            Pattern.CASE_INSENSITIVE);
    
    // Dangerous characters that might be used in SQL injection
    private static final String[] DANGEROUS_CHARS = {
            ";", "--", "/*", "*/", "@@", "@", "char", "nchar", 
            "varchar", "nvarchar", "exec", "execute", "union", "select", 
            "insert", "update", "delete", "drop", "alter", "create"
    };

    /**
     * Sanitize an SQL identifier (table name, column name) by removing or escaping dangerous characters.
     * This should be used when dynamically constructing SQL with identifiers from user input.
     *
     * @param identifier The SQL identifier to sanitize
     * @return The sanitized identifier, or null if the input was potentially malicious
     */
    public static String sanitizeIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return "";
        }
        
        // Check for SQL injection patterns
        if (isSqlInjectionAttempt(identifier)) {
            LogUtil.warning("Potential SQL injection detected in identifier: " + identifier);
            return null;
        }
        
        // Allow only alphanumeric chars and underscore in identifiers
        String sanitized = identifier.replaceAll("[^a-zA-Z0-9_]", "");
        
        // Ensure identifier doesn't start with a number (invalid SQL identifier)
        if (!sanitized.isEmpty() && Character.isDigit(sanitized.charAt(0))) {
            sanitized = "_" + sanitized;
        }
        
        return sanitized;
    }

    /**
     * Sanitize a string value for use in SQL queries.
     * This should be used when building dynamic SQL with string values.
     * Note: Prepared statements are preferred over string concatenation.
     *
     * @param value The string value to sanitize
     * @return The sanitized string value, or null if the input was potentially malicious
     */
    public static String sanitizeStringValue(String value) {
        if (value == null) {
            return "NULL";
        }
        
        // Check for SQL injection patterns
        if (isSqlInjectionAttempt(value)) {
            LogUtil.warning("Potential SQL injection detected in string value: " + value);
            return null;
        }
        
        // Escape single quotes by doubling them (SQL standard)
        return "'" + value.replace("'", "''") + "'";
    }

    /**
     * Check if a string contains potential SQL injection attempts
     *
     * @param input The string to check
     * @return True if SQL injection is detected, false otherwise
     */
    public static boolean isSqlInjectionAttempt(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            return true;
        }
        
        // Check for dangerous character sequences
        String upperCaseInput = input.toUpperCase();
        for (String dangerous : DANGEROUS_CHARS) {
            if (upperCaseInput.contains(dangerous.toUpperCase())) {
                // Additional context check to reduce false positives
                if (isLikelyMalicious(input, dangerous)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * More detailed check for potentially malicious inputs
     * Reduces false positives by checking context of dangerous strings
     *
     * @param input The string to check
     * @param dangerous The dangerous substring found
     * @return True if likely malicious, false otherwise
     */
    private static boolean isLikelyMalicious(String input, String dangerous) {
        // Simple whitelist for common false positives
        if (dangerous.equalsIgnoreCase("select") && 
                (input.toLowerCase().contains("selected") || input.toLowerCase().contains("selection"))) {
            return false;
        }
        
        if (dangerous.equalsIgnoreCase("update") && 
                input.toLowerCase().contains("updated")) {
            return false;
        }
        
        // Check if the dangerous term is a whole word (surrounded by non-alphanumeric)
        String regex = "\\b" + Pattern.quote(dangerous) + "\\b";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        
        return pattern.matcher(input).find();
    }

    /**
     * Sanitize an SQL query by removing dangerous constructs.
     * This should be used as a last resort; prepared statements are preferred.
     *
     * @param query The SQL query to sanitize
     * @return The sanitized query, or null if the input was potentially malicious
     */
    public static String sanitizeQuery(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        
        // Check for obvious SQL injection patterns
        if (isSqlInjectionAttempt(query)) {
            LogUtil.warning("Potential SQL injection detected in query: " + query);
            return null;
        }
        
        // Remove comments
        String sanitized = query.replaceAll("--.*?(\r|\n|$)", " ");
        sanitized = sanitized.replaceAll("/\\*.*?\\*/", " ");
        
        // Remove multiple semicolons (which could be used to chain commands)
        sanitized = sanitized.replaceAll(";\\s*;", ";");
        
        // Remove trailing semicolons
        sanitized = sanitized.replaceAll(";\\s*$", "");
        
        return sanitized;
    }
    
    /**
     * Validate integer input to prevent SQL injection
     *
     * @param value The string to validate as an integer
     * @return The integer value, or null if invalid
     */
    public static Integer validateInteger(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Validate and sanitize a table name
     *
     * @param tableName The table name to sanitize
     * @return The sanitized table name, or null if invalid
     */
    public static String sanitizeTableName(String tableName) {
        return sanitizeIdentifier(tableName);
    }
    
    /**
     * Validate and sanitize a column name
     *
     * @param columnName The column name to sanitize
     * @return The sanitized column name, or null if invalid
     */
    public static String sanitizeColumnName(String columnName) {
        return sanitizeIdentifier(columnName);
    }
}