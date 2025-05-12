// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/security/QueryValidator.java
package com.minecraft.sqlbridge.security;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.error.DatabaseException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates SQL queries for security issues.
 */
public class QueryValidator {
    
    private final SqlBridgePlugin plugin;
    private final boolean sqlInjectionDetectionEnabled;
    private final boolean logDangerousOperations;
    private final int maxQueryLength;
    
    // SQL injection patterns to check for
    private static final Pattern MULTIPLE_STATEMENTS_PATTERN = Pattern.compile(";\\s*\\w+\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMENT_PATTERN = Pattern.compile("--.*|/\\*.*?\\*/", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern UNION_PATTERN = Pattern.compile("\\bUNION\\b.*?\\bSELECT\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BENCHMARK_PATTERN = Pattern.compile("\\bBENCHMARK\\b\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern SLEEP_PATTERN = Pattern.compile("\\bSLEEP\\b\\s*\\(", Pattern.CASE_INSENSITIVE);
    
    // Dangerous operations
    private static final Set<String> DANGEROUS_OPERATIONS = new HashSet<>(Arrays.asList(
        "DROP DATABASE", "DROP TABLE", "DROP SCHEMA", "TRUNCATE TABLE",
        "ALTER DATABASE", "ALTER TABLE DROP", "DELETE FROM", "UPDATE", "RENAME TABLE"
    ));
    
    /**
     * Constructor for QueryValidator.
     *
     * @param plugin The SQL-Bridge plugin instance
     */
    public QueryValidator(SqlBridgePlugin plugin) {
        this.plugin = plugin;
        this.sqlInjectionDetectionEnabled = plugin.getPluginConfig().isSqlInjectionDetectionEnabled();
        this.logDangerousOperations = plugin.getPluginConfig().isLogDangerousOperations();
        this.maxQueryLength = plugin.getPluginConfig().getMaxQueryLength();
    }
    
    /**
     * Validate a SQL query for security issues.
     *
     * @param sql The SQL query to validate
     * @throws DatabaseException If the query contains security issues
     */
    public void validateQuery(String sql) {
        if (sql == null) {
            throw new DatabaseException("SQL query cannot be null");
        }
        
        // Check query length
        if (sql.length() > maxQueryLength) {
            throw new DatabaseException("SQL query exceeds maximum length of " + maxQueryLength);
        }
        
        // Skip validation if disabled
        if (!sqlInjectionDetectionEnabled) {
            return;
        }
        
        // Check for common SQL injection patterns
        if (MULTIPLE_STATEMENTS_PATTERN.matcher(sql).find()) {
            String message = "Potential SQL injection detected: Multiple statements";
            logSecurityIssue(message, sql);
            throw new DatabaseException(message);
        }
        
        if (COMMENT_PATTERN.matcher(sql).find()) {
            String message = "Potential SQL injection detected: SQL comments";
            logSecurityIssue(message, sql);
            throw new DatabaseException(message);
        }
        
        if (UNION_PATTERN.matcher(sql).find()) {
            String message = "Potential SQL injection detected: UNION SELECT";
            logSecurityIssue(message, sql);
            throw new DatabaseException(message);
        }
        
        if (BENCHMARK_PATTERN.matcher(sql).find()) {
            String message = "Potential SQL injection detected: BENCHMARK";
            logSecurityIssue(message, sql);
            throw new DatabaseException(message);
        }
        
        if (SLEEP_PATTERN.matcher(sql).find()) {
            String message = "Potential SQL injection detected: SLEEP";
            logSecurityIssue(message, sql);
            throw new DatabaseException(message);
        }
        
        // Check for dangerous operations
        if (logDangerousOperations) {
            String sqlUpper = sql.toUpperCase();
            for (String operation : DANGEROUS_OPERATIONS) {
                if (sqlUpper.contains(operation)) {
                    logDangerousOperation(operation, sql);
                    break;
                }
            }
        }
    }
    
    /**
     * Log a security issue.
     *
     * @param message The security issue message
     * @param sql The SQL query that caused the issue
     */
    private void logSecurityIssue(String message, String sql) {
        LogUtil.severe(message + ": " + truncateQuery(sql));
        plugin.getErrorHandler().handleSecurityError(sql, new SecurityException(message));
    }
    
    /**
     * Log a dangerous operation.
     *
     * @param operation The dangerous operation
     * @param sql The SQL query that contains the operation
     */
    private void logDangerousOperation(String operation, String sql) {
        LogUtil.warning("Dangerous SQL operation detected: " + operation + " in query: " + truncateQuery(sql));
    }
    
    /**
     * Truncate a query for logging purposes.
     *
     * @param sql The SQL query to truncate
     * @return The truncated query
     */
    private String truncateQuery(String sql) {
        final int maxLength = 100;
        if (sql.length() <= maxLength) {
            return sql;
        }
        
        return sql.substring(0, maxLength) + "...";
    }
    
    /**
     * Security exception for SQL injection issues.
     */
    public static class SecurityException extends RuntimeException {
        
        /**
         * Constructor for SecurityException.
         *
         * @param message The error message
         */
        public SecurityException(String message) {
            super(message);
        }
    }
}