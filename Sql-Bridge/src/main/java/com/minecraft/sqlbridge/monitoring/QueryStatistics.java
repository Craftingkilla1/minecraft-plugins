// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/monitoring/QueryStatistics.java
package com.minecraft.sqlbridge.monitoring;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects statistics about database queries.
 */
public class QueryStatistics {
    
    private final SqlBridgePlugin plugin;
    private final long slowQueryThreshold;
    
    // Query counters
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong totalUpdates = new AtomicLong(0);
    private final AtomicLong totalBatchUpdates = new AtomicLong(0);
    private final AtomicLong failedQueries = new AtomicLong(0);
    private final AtomicLong failedUpdates = new AtomicLong(0);
    private final AtomicLong failedBatchUpdates = new AtomicLong(0);
    private final AtomicLong slowQueries = new AtomicLong(0);
    
    // Query time tracking
    private final AtomicLong totalQueryTime = new AtomicLong(0);
    private final AtomicLong totalUpdateTime = new AtomicLong(0);
    private final AtomicLong totalBatchUpdateTime = new AtomicLong(0);
    
    // Slow query tracking
    private final Map<String, Long> slowQueryLog = new ConcurrentHashMap<>();
    private final int maxSlowQueries = 100;
    
    /**
     * Constructor for QueryStatistics.
     *
     * @param plugin The SQL-Bridge plugin instance
     */
    public QueryStatistics(SqlBridgePlugin plugin) {
        this.plugin = plugin;
        this.slowQueryThreshold = plugin.getPluginConfig().getSlowQueryThreshold();
    }
    
    /**
     * Record a query execution.
     *
     * @param sql The SQL query
     * @param executionTime The execution time in milliseconds
     */
    public void recordQuery(String sql, long executionTime) {
        totalQueries.incrementAndGet();
        totalQueryTime.addAndGet(executionTime);
        
        // Check if it's a slow query
        if (executionTime > slowQueryThreshold) {
            slowQueries.incrementAndGet();
            
            // Log the slow query
            logSlowQuery(sql, executionTime);
        }
    }
    
    /**
     * Record a failed query.
     *
     * @param sql The SQL query
     */
    public void recordFailedQuery(String sql) {
        failedQueries.incrementAndGet();
    }
    
    /**
     * Record an update execution.
     *
     * @param sql The SQL update
     * @param executionTime The execution time in milliseconds
     */
    public void recordUpdate(String sql, long executionTime) {
        totalUpdates.incrementAndGet();
        totalUpdateTime.addAndGet(executionTime);
        
        // Check if it's a slow update
        if (executionTime > slowQueryThreshold) {
            slowQueries.incrementAndGet();
            
            // Log the slow update
            logSlowQuery(sql, executionTime);
        }
    }
    
    /**
     * Record a failed update.
     *
     * @param sql The SQL update
     */
    public void recordFailedUpdate(String sql) {
        failedUpdates.incrementAndGet();
    }
    
    /**
     * Record a batch update execution.
     *
     * @param sql The SQL batch update
     * @param batchSize The number of batch operations
     * @param executionTime The execution time in milliseconds
     */
    public void recordBatchUpdate(String sql, int batchSize, long executionTime) {
        totalBatchUpdates.incrementAndGet();
        totalBatchUpdateTime.addAndGet(executionTime);
        
        // Check if it's a slow batch update
        if (executionTime > slowQueryThreshold) {
            slowQueries.incrementAndGet();
            
            // Log the slow batch update
            logSlowQuery("BATCH: " + sql + " (size: " + batchSize + ")", executionTime);
        }
    }
    
    /**
     * Record a failed batch update.
     *
     * @param sql The SQL batch update
     */
    public void recordFailedBatchUpdate(String sql) {
        failedBatchUpdates.incrementAndGet();
    }
    
    /**
     * Log a slow query.
     *
     * @param sql The SQL query
     * @param executionTime The execution time in milliseconds
     */
    private void logSlowQuery(String sql, long executionTime) {
        // Log to console if debug mode is enabled
        if (plugin.getPluginConfig().isDebugMode()) {
            LogUtil.warning("Slow query detected (" + executionTime + "ms): " + truncateQuery(sql));
        }
        
        // Store in slow query log
        synchronized (slowQueryLog) {
            slowQueryLog.put(truncateQuery(sql), executionTime);
            
            // Trim the log if it gets too large
            if (slowQueryLog.size() > maxSlowQueries) {
                // Remove the oldest entry (LinkedHashMap maintains insertion order)
                String oldest = slowQueryLog.keySet().iterator().next();
                slowQueryLog.remove(oldest);
            }
        }
    }
    
    /**
     * Get query statistics.
     *
     * @return A map of statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Add query counters
        stats.put("totalQueries", totalQueries.get());
        stats.put("totalUpdates", totalUpdates.get());
        stats.put("totalBatchUpdates", totalBatchUpdates.get());
        stats.put("failedQueries", failedQueries.get());
        stats.put("failedUpdates", failedUpdates.get());
        stats.put("failedBatchUpdates", failedBatchUpdates.get());
        stats.put("slowQueries", slowQueries.get());
        
        // Calculate average times
        long queries = totalQueries.get();
        if (queries > 0) {
            double avgQueryTime = (double) totalQueryTime.get() / queries;
            stats.put("averageQueryTime", avgQueryTime);
        } else {
            stats.put("averageQueryTime", 0.0);
        }
        
        long updates = totalUpdates.get();
        if (updates > 0) {
            double avgUpdateTime = (double) totalUpdateTime.get() / updates;
            stats.put("averageUpdateTime", avgUpdateTime);
        } else {
            stats.put("averageUpdateTime", 0.0);
        }
        
        long batchUpdates = totalBatchUpdates.get();
        if (batchUpdates > 0) {
            double avgBatchUpdateTime = (double) totalBatchUpdateTime.get() / batchUpdates;
            stats.put("averageBatchUpdateTime", avgBatchUpdateTime);
        } else {
            stats.put("averageBatchUpdateTime", 0.0);
        }
        
        // Add slow query statistics
        Map<String, Long> slowQueriesCopy;
        synchronized (slowQueryLog) {
            slowQueriesCopy = new LinkedHashMap<>(slowQueryLog);
        }
        
        stats.put("slowQueryCount", slowQueriesCopy.size());
        stats.put("slowQueryLog", slowQueriesCopy);
        
        // Calculate total success rate
        long totalOps = queries + updates + batchUpdates;
        long totalFailed = failedQueries.get() + failedUpdates.get() + failedBatchUpdates.get();
        if (totalOps > 0) {
            double successRate = 100.0 * (totalOps - totalFailed) / totalOps;
            stats.put("successRate", successRate);
        } else {
            stats.put("successRate", 100.0);
        }
        
        return stats;
    }
    
    /**
     * Reset statistics.
     */
    public void reset() {
        totalQueries.set(0);
        totalUpdates.set(0);
        totalBatchUpdates.set(0);
        failedQueries.set(0);
        failedUpdates.set(0);
        failedBatchUpdates.set(0);
        slowQueries.set(0);
        totalQueryTime.set(0);
        totalUpdateTime.set(0);
        totalBatchUpdateTime.set(0);
        
        synchronized (slowQueryLog) {
            slowQueryLog.clear();
        }
    }
    
    /**
     * Truncate a query for logging purposes.
     *
     * @param sql The SQL query to truncate
     * @return The truncated query
     */
    private String truncateQuery(String sql) {
        final int maxLength = 200;
        if (sql == null) {
            return "null";
        }
        
        if (sql.length() <= maxLength) {
            return sql;
        }
        
        return sql.substring(0, maxLength) + "...";
    }
}