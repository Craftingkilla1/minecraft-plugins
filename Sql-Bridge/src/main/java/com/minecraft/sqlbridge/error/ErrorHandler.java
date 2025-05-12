// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/error/ErrorHandler.java
package com.minecraft.sqlbridge.error;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Handles errors in the SQL-Bridge plugin.
 */
public class ErrorHandler {
    
    private final SqlBridgePlugin plugin;
    private final List<String> errorLog;
    private final int maxErrorLogSize;
    
    /**
     * Constructor for ErrorHandler.
     *
     * @param plugin The SQL-Bridge plugin instance
     */
    public ErrorHandler(SqlBridgePlugin plugin) {
        this.plugin = plugin;
        this.errorLog = new ArrayList<>();
        this.maxErrorLogSize = 100; // Keep the last 100 errors
    }
    
    /**
     * Handle a critical error that should disable the plugin.
     *
     * @param error The error to handle
     */
    public void handleCriticalError(Throwable error) {
        logError("CRITICAL ERROR: " + error.getMessage(), error);
        
        // Try to shut down gracefully
        try {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                LogUtil.severe("Disabling SQL-Bridge due to critical error");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
            });
        } catch (Exception e) {
            // If we can't shut down gracefully, log the error
            plugin.getLogger().log(Level.SEVERE, "Failed to disable plugin after critical error", e);
        }
    }
    
    /**
     * Handle a database initialization error.
     *
     * @param error The error to handle
     */
    public void handleDatabaseInitError(Throwable error) {
        logError("Database initialization error: " + error.getMessage(), error);
    }
    
    /**
     * Handle a database connection error.
     *
     * @param error The error to handle
     */
    public void handleConnectionError(Throwable error) {
        logError("Database connection error: " + error.getMessage(), error);
    }
    
    /**
     * Handle a connection check error.
     *
     * @param connectionName The name of the connection
     * @param error The error to handle
     */
    public void handleConnectionCheckError(String connectionName, Throwable error) {
        logError("Connection check error for " + connectionName + ": " + error.getMessage(), error);
    }
    
    /**
     * Handle a service registration error.
     *
     * @param error The error to handle
     */
    public void handleServiceRegistrationError(Throwable error) {
        logError("Service registration error: " + error.getMessage(), error);
    }
    
    /**
     * Handle a command registration error.
     *
     * @param error The error to handle
     */
    public void handleCommandRegistrationError(Throwable error) {
        logError("Command registration error: " + error.getMessage(), error);
    }
    
    /**
     * Handle a migration error.
     *
     * @param error The error to handle
     */
    public void handleMigrationError(Throwable error) {
        logError("Migration error: " + error.getMessage(), error);
    }
    
    /**
     * Handle a database connection error.
     *
     * @param error The error to handle
     */
    public void handleDatabaseConnectionError(Throwable error) {
        logError("Database connection error: " + error.getMessage(), error);
    }
    
    /**
     * Handle a query error.
     *
     * @param query The SQL query that caused the error
     * @param error The error to handle
     */
    public void handleQueryError(String query, Throwable error) {
        logError("Query error: " + error.getMessage() + " in query: " + truncateQuery(query), error);
    }
    
    /**
     * Handle a security error (SQL injection, etc.).
     *
     * @param query The SQL query that caused the error
     * @param error The error to handle
     */
    public void handleSecurityError(String query, Throwable error) {
        logError("SECURITY ERROR: " + error.getMessage() + " in query: " + truncateQuery(query), error);
    }
    
    /**
     * Log an error to the plugin logger and the error log.
     *
     * @param message The error message
     * @param error The error to log
     */
    private void logError(String message, Throwable error) {
        // Log to the plugin logger
        plugin.getLogger().log(Level.SEVERE, message, error);
        
        // Log to the error log
        synchronized (errorLog) {
            String errorMessage = System.currentTimeMillis() + ": " + message;
            errorLog.add(errorMessage);
            
            // Trim the error log if it gets too large
            if (errorLog.size() > maxErrorLogSize) {
                errorLog.remove(0);
            }
        }
    }
    
    /**
     * Get the error log.
     *
     * @return The error log
     */
    public List<String> getErrorLog() {
        synchronized (errorLog) {
            return new ArrayList<>(errorLog);
        }
    }
    
    /**
     * Clear the error log.
     */
    public void clearErrorLog() {
        synchronized (errorLog) {
            errorLog.clear();
        }
    }
    
    /**
     * Truncate a query for logging purposes.
     *
     * @param query The SQL query to truncate
     * @return The truncated query
     */
    private String truncateQuery(String query) {
        final int maxLength = 200;
        if (query == null) {
            return "null";
        }
        
        if (query.length() <= maxLength) {
            return query;
        }
        
        return query.substring(0, maxLength) + "...";
    }
}