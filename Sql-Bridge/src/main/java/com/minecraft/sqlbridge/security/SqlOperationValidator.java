package com.minecraft.sqlbridge.security;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.error.DatabaseErrorHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Provides comprehensive validation for SQL operations.
 * Integrates with SqlInjectionDetector and DatabaseErrorHandler.
 */
public class SqlOperationValidator {

    private final SqlBridgePlugin plugin;
    private final SqlInjectionDetector injectionDetector;
    private final DatabaseErrorHandler errorHandler;
    
    // Configuration
    private final boolean validateQueries;
    private final boolean blockDangerousQueries;
    private final int maxQueryLength;
    private final int maxParamLength;
    
    // SQL Operation validation patterns
    private static final Pattern DELETE_WITHOUT_WHERE = Pattern.compile(
            "\\bDELETE\\s+FROM\\s+[^\\s;]+\\s*(?:;|$)", 
            Pattern.CASE_INSENSITIVE);
    
    private static final Pattern UPDATE_WITHOUT_WHERE = Pattern.compile(
            "\\bUPDATE\\s+[^\\s;]+\\s+SET\\s+[^\\s;]+\\s*(?:;|$)", 
            Pattern.CASE_INSENSITIVE);
    
    private static final Pattern DROP_TABLE = Pattern.compile(
            "\\bDROP\\s+TABLE\\b", 
            Pattern.CASE_INSENSITIVE);
    
    private static final Pattern TRUNCATE_TABLE = Pattern.compile(
            "\\bTRUNCATE\\s+TABLE\\b", 
            Pattern.CASE_INSENSITIVE);
    
    private static final Pattern ALTER_TABLE = Pattern.compile(
            "\\bALTER\\s+TABLE\\b", 
            Pattern.CASE_INSENSITIVE);
    
    // Statistics tracking
    private final Map<String, AtomicInteger> validationStats = new ConcurrentHashMap<>();
    private final AtomicInteger totalValidations = new AtomicInteger(0);
    private final AtomicInteger blockedQueries = new AtomicInteger(0);
    
    /**
     * Create a new SQL operation validator
     *
     * @param plugin The SQL-Bridge plugin instance
     * @param injectionDetector The SQL injection detector
     * @param errorHandler The database error handler
     */
    public SqlOperationValidator(SqlBridgePlugin plugin, 
            SqlInjectionDetector injectionDetector,
            DatabaseErrorHandler errorHandler) {
        this.plugin = plugin;
        this.injectionDetector = injectionDetector;
        this.errorHandler = errorHandler;
        
        // Load configuration
        this.validateQueries = plugin.getConfig().getBoolean("security.validation.enabled", true);
        this.blockDangerousQueries = plugin.getConfig().getBoolean("security.validation.block-dangerous", true);
        this.maxQueryLength = plugin.getConfig().getInt("security.validation.max-query-length", 10000);
        this.maxParamLength = plugin.getConfig().getInt("security.validation.max-param-length", 1000);
    }
    
    /**
     * Validate a SQL query
     *
     * @param sql The SQL query to validate
     * @param params The query parameters (can be null)
     * @param source The source of the query (plugin name, etc.)
     * @return True if the query is valid, false otherwise
     */
    public boolean validateQuery(String sql, Object[] params, String source) {
        if (!validateQueries) {
            return true;
        }
        
        // Track validation
        totalValidations.incrementAndGet();
        
        // Check for null or empty SQL
        if (sql == null || sql.trim().isEmpty()) {
            trackValidationResult("null_or_empty");
            return false;
        }
        
        // Check query length
        if (sql.length() > maxQueryLength) {
            LogUtil.warning("Query exceeds maximum length: " + sql.length() + " > " + maxQueryLength);
            trackValidationResult("exceeds_length");
            
            if (blockDangerousQueries) {
                blockedQueries.incrementAndGet();
                return false;
            }
        }
        
        // Check parameters if provided
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] != null && params[i] instanceof String) {
                    String paramValue = (String) params[i];
                    
                    // Check parameter length
                    if (paramValue.length() > maxParamLength) {
                        LogUtil.warning("Parameter " + (i+1) + " exceeds maximum length: " + 
                                paramValue.length() + " > " + maxParamLength);
                        trackValidationResult("param_exceeds_length");
                        
                        if (blockDangerousQueries) {
                            blockedQueries.incrementAndGet();
                            return false;
                        }
                    }
                    
                    // Parameter values are individually checked for SQL injection patterns
                    if (injectionDetector != null && 
                            injectionDetector.detectInjection(paramValue, source, null)) {
                        LogUtil.severe("SQL injection detected in parameter " + (i+1) + ": " + paramValue);
                        trackValidationResult("injection_in_param");
                        
                        if (blockDangerousQueries) {
                            blockedQueries.incrementAndGet();
                            return false;
                        }
                    }
                }
            }
        }
        
        // Check for SQL injection in the query
        if (injectionDetector != null && 
                injectionDetector.detectInjection(sql, source, null)) {
            LogUtil.severe("SQL injection detected in query: " + sql);
            trackValidationResult("injection_in_query");
            
            if (blockDangerousQueries) {
                blockedQueries.incrementAndGet();
                return false;
            }
        }
        
        // Check for dangerous operations
        if (isDangerousOperation(sql)) {
            LogUtil.warning("Potentially dangerous SQL operation detected: " + sql);
            trackValidationResult("dangerous_operation");
            
            if (blockDangerousQueries) {
                blockedQueries.incrementAndGet();
                return false;
            }
        }
        
        // If we made it here, the query is valid
        trackValidationResult("valid");
        return true;
    }
    
    /**
     * Check if a SQL query contains dangerous operations
     *
     * @param sql The SQL query
     * @return True if the query contains dangerous operations
     */
    private boolean isDangerousOperation(String sql) {
        // Check for DELETE without WHERE
        if (DELETE_WITHOUT_WHERE.matcher(sql).find()) {
            trackValidationResult("delete_without_where");
            return true;
        }
        
        // Check for UPDATE without WHERE
        if (UPDATE_WITHOUT_WHERE.matcher(sql).find()) {
            trackValidationResult("update_without_where");
            return true;
        }
        
        // Check for DROP TABLE
        if (DROP_TABLE.matcher(sql).find()) {
            trackValidationResult("drop_table");
            return true;
        }
        
        // Check for TRUNCATE TABLE
        if (TRUNCATE_TABLE.matcher(sql).find()) {
            trackValidationResult("truncate_table");
            return true;
        }
        
        // Check for ALTER TABLE
        if (ALTER_TABLE.matcher(sql).find()) {
            trackValidationResult("alter_table");
            return true;
        }
        
        return false;
    }
    
    /**
     * Track validation result for statistics
     *
     * @param result The validation result
     */
    private void trackValidationResult(String result) {
        validationStats.computeIfAbsent(result, k -> new AtomicInteger(0))
                .incrementAndGet();
    }
    
    /**
     * Get validation statistics
     *
     * @return Map of validation statistics
     */
    public Map<String, Integer> getValidationStatistics() {
        Map<String, Integer> stats = new ConcurrentHashMap<>();
        
        validationStats.forEach((key, value) -> {
            stats.put(key, value.get());
        });
        
        stats.put("total", totalValidations.get());
        stats.put("blocked", blockedQueries.get());
        
        return stats;
    }
    
    /**
     * Reset validation statistics
     */
    public void resetStatistics() {
        validationStats.clear();
        totalValidations.set(0);
        blockedQueries.set(0);
    }
    
    /**
     * Get total validations
     *
     * @return Total number of validations
     */
    public int getTotalValidations() {
        return totalValidations.get();
    }
    
    /**
     * Get total blocked queries
     *
     * @return Total number of blocked queries
     */
    public int getBlockedQueries() {
        return blockedQueries.get();
    }
}