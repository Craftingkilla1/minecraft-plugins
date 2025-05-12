// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/security/SQLInjectionDetector.java
package com.minecraft.sqlbridge.security;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.error.DatabaseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects potential SQL injection attempts in queries.
 */
public class SQLInjectionDetector {

    // Common SQL injection patterns
    private static final List<Pattern> INJECTION_PATTERNS = new ArrayList<>();
    
    // Initialize patterns
    static {
        // Multiple statements (;)
        INJECTION_PATTERNS.add(Pattern.compile(";\\s*\\w+\\s*", Pattern.CASE_INSENSITIVE));
        
        // Comments (-- or /* */) used to terminate statements
        INJECTION_PATTERNS.add(Pattern.compile("--.*|/\\*.*?\\*/", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        
        // UNION-based injections
        INJECTION_PATTERNS.add(Pattern.compile("\\bunion\\b.*?\\bselect\\b", Pattern.CASE_INSENSITIVE));
        
        // Time-based blind injections
        INJECTION_PATTERNS.add(Pattern.compile("\\bbenchmark\\b\\s*\\(|\\bsleep\\b\\s*\\(|\\bwaitfor\\b\\s*\\b(delay|time)\\b", 
                Pattern.CASE_INSENSITIVE));
        
        // Generic string truncation to bypass filters
        INJECTION_PATTERNS.add(Pattern.compile("\\bor\\b\\s+'\\s*[0-9]\\s*'\\s*=\\s*'\\s*[0-9]", Pattern.CASE_INSENSITIVE));
        
        // Basic OR injections
        INJECTION_PATTERNS.add(Pattern.compile("\\bor\\b\\s+[0-9]\\s*=\\s*[0-9]\\b", Pattern.CASE_INSENSITIVE));
        
        // Basic AND injections
        INJECTION_PATTERNS.add(Pattern.compile("\\band\\b\\s+[0-9]\\s*=\\s*[0-9]\\b", Pattern.CASE_INSENSITIVE));
        
        // Common table/schema enumeration
        INJECTION_PATTERNS.add(Pattern.compile("\\bfrom\\b\\s+\\binformation_schema\\b\\.\\btables\\b", 
                Pattern.CASE_INSENSITIVE));
        
        // Error-based injections
        INJECTION_PATTERNS.add(Pattern.compile("\\bconvert\\b\\s*\\(", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("\\bcast\\b\\s*\\(", Pattern.CASE_INSENSITIVE));
    }

    private final SqlBridgePlugin plugin;
    
    /**
     * Constructor for SQLInjectionDetector.
     *
     * @param plugin The SQL-Bridge plugin instance
     */
    public SQLInjectionDetector(SqlBridgePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Check if a query contains potential SQL injection attempts.
     *
     * @param sql The SQL query to check
     * @param parameters The query parameters
     * @return true if the query is safe, false if potential injection is detected
     */
    public boolean isSafe(String sql, Object... parameters) {
        if (sql == null || sql.isEmpty()) {
            return false;
        }
        
        // Check for common SQL injection patterns in query
        for (Pattern pattern : INJECTION_PATTERNS) {
            Matcher matcher = pattern.matcher(sql);
            if (matcher.find()) {
                String match = matcher.group(0);
                LogUtil.severe("Potential SQL injection detected: " + match + " in query: " + sql);
                return false;
            }
        }
        
        // Check parameters for potential injection strings
        if (parameters != null) {
            for (Object param : parameters) {
                if (param instanceof String) {
                    String strParam = (String) param;
                    if (isParameterSuspicious(strParam)) {
                        LogUtil.severe("Suspicious SQL parameter detected: " + strParam);
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Check if a parameter string looks suspicious.
     *
     * @param param The parameter string to check
     * @return true if the parameter is suspicious, false otherwise
     */
    private boolean isParameterSuspicious(String param) {
        if (param == null || param.isEmpty()) {
            return false;
        }
        
        // List of suspicious strings that might indicate SQL injection
        List<String> suspiciousStrings = Arrays.asList(
            ";", "--", "/*", "*/", "union select", "drop table", "drop database",
            "delete from", "truncate table", "1=1", "or 1=1", "or 1 = 1", 
            "' or '", "' or 1=1", "\" or \"", "\" or 1=1"
        );
        
        String lowerParam = param.toLowerCase();
        for (String suspicious : suspiciousStrings) {
            if (lowerParam.contains(suspicious)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Sanitize a raw SQL query to prevent SQL injection.
     * This should be used with caution and only for non-parameterized queries.
     *
     * @param sql The SQL query to sanitize
     * @return The sanitized SQL query
     */
    public String sanitize(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "";
        }
        
        // Replace dangerous characters
        String sanitized = sql.replaceAll(";", "")  // No multiple statements
                             .replaceAll("--", "")  // No SQL comments
                             .replaceAll("/\\*.*?\\*/", "")  // No block comments
                             .replaceAll("'", "''") // Escape single quotes
                             .trim();
        
        return sanitized;
    }
    
    /**
     * Sanitize a parameter for use in a SQL query.
     * This should be used with caution and only when proper parameterization is not possible.
     *
     * @param param The parameter to sanitize
     * @return The sanitized parameter
     */
    public String sanitizeParameter(String param) {
        if (param == null || param.isEmpty()) {
            return "";
        }
        
        // Escape special characters
        String sanitized = param.replaceAll("'", "''")  // Escape single quotes
                               .replaceAll("\"", "\"\"")  // Escape double quotes
                               .replaceAll("\\\\", "\\\\\\\\")  // Escape backslashes
                               .trim();
        
        return sanitized;
    }
    
    /**
     * Validate and sanitize a table name to prevent SQL injection.
     *
     * @param tableName The table name to validate
     * @return The sanitized table name
     * @throws DatabaseException If the table name contains invalid characters
     */
    public String validateTableName(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            throw new DatabaseException("Table name cannot be null or empty");
        }
        
        // Check for valid table name (alphanumeric and underscores only)
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new DatabaseException("Invalid table name: " + tableName);
        }
        
        return tableName;
    }
    
    /**
     * Validate and sanitize a column name to prevent SQL injection.
     *
     * @param columnName The column name to validate
     * @return The sanitized column name
     * @throws DatabaseException If the column name contains invalid characters
     */
    public String validateColumnName(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            throw new DatabaseException("Column name cannot be null or empty");
        }
        
        // Check for valid column name (alphanumeric and underscores only)
        if (!columnName.matches("^[a-zA-Z0-9_]+$")) {
            throw new DatabaseException("Invalid column name: " + columnName);
        }
        
        return columnName;
    }
}