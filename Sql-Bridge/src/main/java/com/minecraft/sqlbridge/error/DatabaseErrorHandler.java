package com.minecraft.sqlbridge.error;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.connection.ConnectionManager;

import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles database errors and implements recovery strategies.
 * Integrates with Core-Utils LogUtil for error reporting.
 */
public class DatabaseErrorHandler {

    private final SqlBridgePlugin plugin;
    private final ConnectionManager connectionManager;
    private final Map<ErrorCategory, RecoveryStrategy> recoveryStrategies;
    
    // Error tracking
    private final Map<String, ErrorStat> errorStats = new ConcurrentHashMap<>();
    private final List<ErrorEvent> recentErrors = new ArrayList<>();
    private static final int MAX_RECENT_ERRORS = 50;
    
    // Connection failure tracking
    private final AtomicInteger consecutiveConnectionFailures = new AtomicInteger(0);
    
    // Recovery thresholds
    private final int maxConsecutiveFailures;
    private final long errorThresholdPeriod;
    private final int errorThresholdCount;
    
    // Recovery state
    private boolean recoveryMode = false;
    private long recoveryModeStartTime = 0;
    private long backoffTime = 1000; // Start with 1 second backoff
    private static final long MAX_BACKOFF_TIME = 60000; // Max 1 minute backoff
    
    /**
     * Create a new database error handler
     *
     * @param plugin The SQL-Bridge plugin instance
     * @param connectionManager The connection manager
     */
    public DatabaseErrorHandler(SqlBridgePlugin plugin, ConnectionManager connectionManager) {
        this.plugin = plugin;
        this.connectionManager = connectionManager;
        this.recoveryStrategies = new HashMap<>();
        
        // Load configuration
        this.maxConsecutiveFailures = plugin.getConfig().getInt("error.max-consecutive-failures", 3);
        this.errorThresholdPeriod = plugin.getConfig().getInt("error.threshold-period", 60) * 1000; // Convert to ms
        this.errorThresholdCount = plugin.getConfig().getInt("error.threshold-count", 10);
        
        // Initialize recovery strategies
        initializeRecoveryStrategies();
    }
    
    /**
     * Initialize recovery strategies for different error categories
     */
    private void initializeRecoveryStrategies() {
        // Connection-related errors
        recoveryStrategies.put(ErrorCategory.CONNECTION, (error, attempt) -> {
            LogUtil.warning("Applying CONNECTION recovery strategy (attempt " + attempt + ")");
            
            // Track consecutive failures
            int failures = consecutiveConnectionFailures.incrementAndGet();
            
            // If we've had too many consecutive failures, enter recovery mode
            if (failures >= maxConsecutiveFailures && !recoveryMode) {
                enterRecoveryMode();
            }
            
            // For connection errors, try to reinitialize the connection pool
            if (failures % maxConsecutiveFailures == 0) {
                LogUtil.info("Attempting to reinitialize connection pool");
                return connectionManager.reinitialize();
            }
            
            // Otherwise just wait and retry
            try {
                Thread.sleep(calculateBackoff(attempt));
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        });
        
        // Transaction-related errors
        recoveryStrategies.put(ErrorCategory.TRANSACTION, (error, attempt) -> {
            LogUtil.warning("Applying TRANSACTION recovery strategy (attempt " + attempt + ")");
            
            // For transaction errors, wait and retry
            try {
                Thread.sleep(calculateBackoff(attempt));
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        });
        
        // Data-related errors
        recoveryStrategies.put(ErrorCategory.DATA, (error, attempt) -> {
            LogUtil.warning("Applying DATA recovery strategy (attempt " + attempt + ")");
            
            // Data errors usually can't be recovered automatically
            return false;
        });
        
        // Query-related errors
        recoveryStrategies.put(ErrorCategory.QUERY, (error, attempt) -> {
            LogUtil.warning("Applying QUERY recovery strategy (attempt " + attempt + ")");
            
            // For query errors, just retry once
            return attempt <= 1;
        });
        
        // Unknown/other errors
        recoveryStrategies.put(ErrorCategory.OTHER, (error, attempt) -> {
            LogUtil.warning("Applying OTHER recovery strategy (attempt " + attempt + ")");
            
            // For unknown errors, retry once with a delay
            if (attempt <= 1) {
                try {
                    Thread.sleep(calculateBackoff(attempt));
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        });
    }
    
    /**
     * Handle a database error
     *
     * @param error The SQL exception
     * @param sql The SQL query that caused the error (can be null)
     * @param params The query parameters (can be null)
     * @return True if recovery was successful, false otherwise
     */
    public boolean handleError(SQLException error, String sql, Object[] params) {
        // Log the error
        LogUtil.severe("Database error: " + error.getMessage());
        if (sql != null) {
            LogUtil.severe("SQL: " + sql);
        }
        if (params != null && params.length > 0) {
            LogUtil.severe("Parameters: " + formatParameters(params));
        }
        
        // Track the error
        trackError(error, sql);
        
        // Determine error category
        ErrorCategory category = categorizeError(error);
        
        // Get the appropriate recovery strategy
        RecoveryStrategy strategy = recoveryStrategies.getOrDefault(category, 
                recoveryStrategies.get(ErrorCategory.OTHER));
        
        // Apply the recovery strategy
        boolean recovered = false;
        int attempt = 1;
        int maxAttempts = category == ErrorCategory.CONNECTION ? 3 : 2;
        
        while (!recovered && attempt <= maxAttempts) {
            recovered = strategy.applyRecovery(error, attempt);
            attempt++;
        }
        
        // Reset consecutive failures if recovered
        if (recovered && category == ErrorCategory.CONNECTION) {
            consecutiveConnectionFailures.set(0);
            
            // Exit recovery mode if we're in it
            if (recoveryMode) {
                exitRecoveryMode();
            }
        }
        
        return recovered;
    }
    
    /**
     * Enter recovery mode
     */
    private void enterRecoveryMode() {
        LogUtil.severe("Entering database recovery mode due to repeated connection failures");
        recoveryMode = true;
        recoveryModeStartTime = System.currentTimeMillis();
        
        // Notify server administrators
        plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("sqlbridge.admin"))
                .forEach(p -> p.sendMessage("§c[SQL-Bridge] Database connection issues detected. " +
                        "Entering recovery mode."));
        
        // Schedule a task to periodically attempt recovery
        scheduleRecoveryTask();
    }
    
    /**
     * Exit recovery mode
     */
    private void exitRecoveryMode() {
        LogUtil.info("Exiting database recovery mode - connection restored");
        recoveryMode = false;
        backoffTime = 1000; // Reset backoff time
        
        // Notify server administrators
        plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("sqlbridge.admin"))
                .forEach(p -> p.sendMessage("§a[SQL-Bridge] Database connection restored."));
    }
    
    /**
     * Schedule a task to periodically attempt recovery
     */
    private void scheduleRecoveryTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (!recoveryMode) {
                return; // Exit if no longer in recovery mode
            }
            
            // Try to reinitialize connection
            LogUtil.info("Recovery attempt: Trying to reinitialize database connection");
            boolean success = connectionManager.reinitialize();
            
            if (success) {
                // Connection restored
                LogUtil.info("Recovery successful: Database connection restored");
                exitRecoveryMode();
            } else {
                // Still failing, increase backoff time
                backoffTime = Math.min(backoffTime * 2, MAX_BACKOFF_TIME);
                LogUtil.warning("Recovery failed: Will retry in " + (backoffTime / 1000) + " seconds");
            }
        }, 20 * 5, 20 * 30); // First attempt after 5 seconds, then every 30 seconds
    }
    
    /**
     * Calculate backoff time based on attempt number
     *
     * @param attempt The attempt number
     * @return The backoff time in milliseconds
     */
    private long calculateBackoff(int attempt) {
        // Exponential backoff: 1s, 2s, 4s, 8s, etc. up to MAX_BACKOFF_TIME
        return Math.min((long) Math.pow(2, attempt - 1) * 1000, MAX_BACKOFF_TIME);
    }
    
    /**
     * Track an error
     *
     * @param error The SQL exception
     * @param sql The SQL query that caused the error
     */
    private void trackError(SQLException error, String sql) {
        // Create error event
        ErrorEvent event = new ErrorEvent(
                System.currentTimeMillis(),
                error.getClass().getSimpleName(),
                error.getMessage(),
                error.getSQLState(),
                error.getErrorCode(),
                sql
        );
        
        // Add to recent errors list
        synchronized (recentErrors) {
            recentErrors.add(event);
            if (recentErrors.size() > MAX_RECENT_ERRORS) {
                recentErrors.remove(0);
            }
        }
        
        // Update error stats
        String errorKey = error.getClass().getSimpleName() + ":" + error.getSQLState();
        errorStats.compute(errorKey, (key, stat) -> {
            if (stat == null) {
                return new ErrorStat(error.getClass().getSimpleName(), error.getSQLState(), 1);
            } else {
                stat.count.incrementAndGet();
                stat.lastOccurrence = System.currentTimeMillis();
                return stat;
            }
        });
    }
    
    /**
     * Categorize an SQL exception
     *
     * @param error The SQL exception
     * @return The error category
     */
    private ErrorCategory categorizeError(SQLException error) {
        // Check if it's a connection error
        if (error instanceof SQLTransientConnectionException || 
                error instanceof SQLNonTransientConnectionException ||
                error instanceof SQLRecoverableException ||
                (error.getSQLState() != null && error.getSQLState().startsWith("08"))) {
            return ErrorCategory.CONNECTION;
        }
        
        // Check if it's a transaction error
        if (error.getSQLState() != null && 
                (error.getSQLState().startsWith("25") || error.getSQLState().startsWith("40"))) {
            return ErrorCategory.TRANSACTION;
        }
        
        // Check if it's a data error
        if (error.getSQLState() != null && 
                (error.getSQLState().startsWith("22") || error.getSQLState().startsWith("23"))) {
            return ErrorCategory.DATA;
        }
        
        // Check if it's a query error
        if (error.getSQLState() != null && 
                (error.getSQLState().startsWith("42") || error.getSQLState().startsWith("2A"))) {
            return ErrorCategory.QUERY;
        }
        
        // Default to OTHER
        return ErrorCategory.OTHER;
    }
    
    /**
     * Format query parameters for logging
     *
     * @param params The query parameters
     * @return Formatted string representation
     */
    private String formatParameters(Object[] params) {
        if (params == null || params.length == 0) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(params[i] == null ? "null" : params[i].toString());
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Get error statistics
     *
     * @return Map of error statistics
     */
    public Map<String, Map<String, Object>> getErrorStatistics() {
        Map<String, Map<String, Object>> stats = new HashMap<>();
        
        errorStats.forEach((key, stat) -> {
            Map<String, Object> statMap = new HashMap<>();
            statMap.put("type", stat.type);
            statMap.put("sqlState", stat.sqlState);
            statMap.put("count", stat.count.get());
            statMap.put("lastOccurrence", stat.lastOccurrence);
            stats.put(key, statMap);
        });
        
        return stats;
    }
    
    /**
     * Get recent errors
     *
     * @return List of recent error events
     */
    public List<ErrorEvent> getRecentErrors() {
        synchronized (recentErrors) {
            return new ArrayList<>(recentErrors);
        }
    }
    
    /**
     * Clear error statistics
     */
    public void clearErrorStats() {
        errorStats.clear();
        synchronized (recentErrors) {
            recentErrors.clear();
        }
    }
    
    /**
     * Check if in recovery mode
     *
     * @return True if in recovery mode
     */
    public boolean isInRecoveryMode() {
        return recoveryMode;
    }
    
    /**
     * Error category enum
     */
    public enum ErrorCategory {
        CONNECTION,
        TRANSACTION,
        DATA,
        QUERY,
        OTHER
    }
    
    /**
     * Recovery strategy interface
     */
    @FunctionalInterface
    private interface RecoveryStrategy {
        boolean applyRecovery(SQLException error, int attempt);
    }
    
    /**
     * Error statistics tracking class
     */
    private static class ErrorStat {
        final String type;
        final String sqlState;
        final AtomicInteger count = new AtomicInteger(0);
        long lastOccurrence;
        
        ErrorStat(String type, String sqlState, int initialCount) {
            this.type = type;
            this.sqlState = sqlState;
            this.count.set(initialCount);
            this.lastOccurrence = System.currentTimeMillis();
        }
    }
    
    /**
     * Error event class for tracking recent errors
     */
    public static class ErrorEvent {
        private final long timestamp;
        private final String type;
        private final String message;
        private final String sqlState;
        private final int errorCode;
        private final String sql;
        
        ErrorEvent(long timestamp, String type, String message, String sqlState, int errorCode, String sql) {
            this.timestamp = timestamp;
            this.type = type;
            this.message = message;
            this.sqlState = sqlState;
            this.errorCode = errorCode;
            this.sql = sql;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public String getType() {
            return type;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getSqlState() {
            return sqlState;
        }
        
        public int getErrorCode() {
            return errorCode;
        }
        
        public String getSql() {
            return sql;
        }
    }
}