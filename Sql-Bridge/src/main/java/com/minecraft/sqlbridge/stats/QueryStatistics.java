package com.minecraft.sqlbridge.stats;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks statistics for database queries.
 * Provides insights into query performance and usage patterns.
 */
public class QueryStatistics {

    private static final int MAX_SLOW_QUERIES = 100;
    private static final int MAX_QUERY_LENGTH = 200;

    // Singleton instance
    private static QueryStatistics instance;

    // General statistics
    private final AtomicInteger totalQueries = new AtomicInteger(0);
    private final AtomicInteger totalUpdates = new AtomicInteger(0);
    private final AtomicInteger totalInserts = new AtomicInteger(0);
    private final AtomicInteger totalDeletes = new AtomicInteger(0);
    private final AtomicInteger totalSelects = new AtomicInteger(0);
    private final AtomicInteger totalBatchOperations = new AtomicInteger(0);
    private final AtomicInteger totalTransactions = new AtomicInteger(0);
    private final AtomicInteger totalErrors = new AtomicInteger(0);
    private final AtomicLong totalQueryTime = new AtomicLong(0);
    private final AtomicLong maxQueryTime = new AtomicLong(0);

    // Query-specific statistics
    private final Map<String, QueryStat> queryStats = new ConcurrentHashMap<>();
    
    // Slow query tracking
    private final List<SlowQuery> slowQueries = Collections.synchronizedList(new ArrayList<>());

    // Connection statistics
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger totalConnectionsCreated = new AtomicInteger(0);
    private final AtomicInteger maxConcurrentConnections = new AtomicInteger(0);
    
    // Plugin instance for configuration
    private final SqlBridgePlugin plugin;
    private boolean enabled;
    private int slowQueryThreshold;

    /**
     * Private constructor for singleton pattern
     */
    private QueryStatistics(SqlBridgePlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    /**
     * Get the singleton instance
     * 
     * @param plugin The plugin instance
     * @return The query statistics instance
     */
    public static synchronized QueryStatistics getInstance(SqlBridgePlugin plugin) {
        if (instance == null) {
            instance = new QueryStatistics(plugin);
        }
        return instance;
    }

    /**
     * Reload configuration settings
     */
    public void reloadConfig() {
        this.enabled = plugin.getConfig().getBoolean("statistics.enabled", true);
        this.slowQueryThreshold = plugin.getConfig().getInt("statistics.slow-query-threshold", 1000);
    }

    /**
     * Record a database query
     * 
     * @param sql The SQL query
     * @param startTime The query start time
     * @param endTime The query end time
     * @param success Whether the query executed successfully
     */
    public void recordQuery(String sql, long startTime, long endTime, boolean success) {
        if (!enabled) {
            return;
        }
        
        String normalizedSql = normalizeSql(sql);
        long duration = endTime - startTime;
        
        // Update general statistics
        totalQueries.incrementAndGet();
        totalQueryTime.addAndGet(duration);
        
        // Update max query time if this query took longer
        while (true) {
            long current = maxQueryTime.get();
            if (duration <= current || maxQueryTime.compareAndSet(current, duration)) {
                break;
            }
        }
        
        // Categorize query
        if (normalizedSql.startsWith("SELECT")) {
            totalSelects.incrementAndGet();
        } else if (normalizedSql.startsWith("INSERT")) {
            totalInserts.incrementAndGet();
        } else if (normalizedSql.startsWith("UPDATE")) {
            totalUpdates.incrementAndGet();
        } else if (normalizedSql.startsWith("DELETE")) {
            totalDeletes.incrementAndGet();
        }
        
        // Record errors
        if (!success) {
            totalErrors.incrementAndGet();
        }
        
        // Update query-specific statistics
        QueryStat stat = queryStats.computeIfAbsent(normalizedSql, k -> new QueryStat());
        stat.count.incrementAndGet();
        stat.totalTime.addAndGet(duration);
        
        if (duration > stat.maxTime.get()) {
            stat.maxTime.set(duration);
        }
        
        // Track slow queries
        if (duration >= slowQueryThreshold) {
            recordSlowQuery(normalizedSql, duration);
        }
    }

    /**
     * Record a batch operation
     * 
     * @param batchSize The number of operations in the batch
     */
    public void recordBatchOperation(int batchSize) {
        if (!enabled) {
            return;
        }
        
        totalBatchOperations.incrementAndGet();
    }

    /**
     * Record a transaction
     * 
     * @param startTime The transaction start time
     * @param endTime The transaction end time
     * @param success Whether the transaction completed successfully
     */
    public void recordTransaction(long startTime, long endTime, boolean success) {
        if (!enabled) {
            return;
        }
        
        totalTransactions.incrementAndGet();
        
        if (!success) {
            totalErrors.incrementAndGet();
        }
    }

    /**
     * Record connection activity
     * 
     * @param acquired Whether a connection was acquired (true) or released (false)
     */
    public void recordConnection(boolean acquired) {
        if (!enabled) {
            return;
        }
        
        if (acquired) {
            int current = activeConnections.incrementAndGet();
            totalConnectionsCreated.incrementAndGet();
            
            // Update max concurrent connections if needed
            while (true) {
                int max = maxConcurrentConnections.get();
                if (current <= max || maxConcurrentConnections.compareAndSet(max, current)) {
                    break;
                }
            }
        } else {
            activeConnections.decrementAndGet();
        }
    }

    /**
     * Get a summary of database statistics
     * 
     * @return A map of statistic names to values
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // General statistics
        stats.put("totalQueries", totalQueries.get());
        stats.put("totalUpdates", totalUpdates.get());
        stats.put("totalInserts", totalInserts.get());
        stats.put("totalDeletes", totalDeletes.get());
        stats.put("totalSelects", totalSelects.get());
        stats.put("totalBatchOperations", totalBatchOperations.get());
        stats.put("totalTransactions", totalTransactions.get());
        stats.put("totalErrors", totalErrors.get());
        
        // Query timing
        long total = totalQueries.get();
        stats.put("totalQueryTime", totalQueryTime.get());
        stats.put("maxQueryTime", maxQueryTime.get());
        stats.put("averageQueryTime", total > 0 ? totalQueryTime.get() / total : 0);
        
        // Connection statistics
        stats.put("activeConnections", activeConnections.get());
        stats.put("totalConnectionsCreated", totalConnectionsCreated.get());
        stats.put("maxConcurrentConnections", maxConcurrentConnections.get());
        
        return stats;
    }

    /**
     * Get the top N most frequently executed queries
     * 
     * @param limit The maximum number of queries to return
     * @return A list of query statistics
     */
    public List<Map<String, Object>> getTopQueries(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        queryStats.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue().count.get(), a.getValue().count.get()))
            .limit(limit)
            .forEach(entry -> {
                Map<String, Object> query = new HashMap<>();
                query.put("sql", entry.getKey());
                query.put("count", entry.getValue().count.get());
                query.put("totalTime", entry.getValue().totalTime.get());
                query.put("maxTime", entry.getValue().maxTime.get());
                query.put("averageTime", entry.getValue().count.get() > 0 ? 
                        entry.getValue().totalTime.get() / entry.getValue().count.get() : 0);
                result.add(query);
            });
        
        return result;
    }

    /**
     * Get a list of slow queries
     * 
     * @return A list of slow query information
     */
    public List<Map<String, Object>> getSlowQueries() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        synchronized (slowQueries) {
            for (SlowQuery slowQuery : slowQueries) {
                Map<String, Object> query = new HashMap<>();
                query.put("sql", slowQuery.sql);
                query.put("time", slowQuery.time);
                query.put("timestamp", slowQuery.timestamp);
                result.add(query);
            }
        }
        
        return result;
    }

    /**
     * Reset all statistics
     */
    public void reset() {
        totalQueries.set(0);
        totalUpdates.set(0);
        totalInserts.set(0);
        totalDeletes.set(0);
        totalSelects.set(0);
        totalBatchOperations.set(0);
        totalTransactions.set(0);
        totalErrors.set(0);
        totalQueryTime.set(0);
        maxQueryTime.set(0);
        
        queryStats.clear();
        
        synchronized (slowQueries) {
            slowQueries.clear();
        }
        
        // Don't reset connection counters since they represent current state
    }

    /**
     * Record a slow query
     * 
     * @param sql The SQL query
     * @param time The execution time in milliseconds
     */
    private void recordSlowQuery(String sql, long time) {
        synchronized (slowQueries) {
            // Add new slow query
            SlowQuery slowQuery = new SlowQuery(sql, time, System.currentTimeMillis());
            slowQueries.add(slowQuery);
            
            // Log slow query
            LogUtil.warning("Slow query detected (" + time + "ms): " + sql);
            
            // Trim list if it exceeds maximum size
            if (slowQueries.size() > MAX_SLOW_QUERIES) {
                slowQueries.remove(0);
            }
        }
    }

    /**
     * Normalize an SQL query by removing literals and extra whitespace
     * 
     * @param sql The SQL query
     * @return The normalized query
     */
    private String normalizeSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "";
        }
        
        // Truncate long queries
        if (sql.length() > MAX_QUERY_LENGTH) {
            sql = sql.substring(0, MAX_QUERY_LENGTH) + "...";
        }
        
        // Get the first few words to determine query type
        String[] words = sql.trim().split("\\s+", 4);
        if (words.length > 0) {
            return words[0].toUpperCase();
        }
        
        return "";
    }

    /**
     * Inner class to track statistics for a specific query
     */
    private static class QueryStat {
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicLong totalTime = new AtomicLong(0);
        final AtomicLong maxTime = new AtomicLong(0);
    }

    /**
     * Inner class to represent a slow query
     */
    private static class SlowQuery {
        final String sql;
        final long time;
        final long timestamp;
        
        SlowQuery(String sql, long time, long timestamp) {
            this.sql = sql;
            this.time = time;
            this.timestamp = timestamp;
        }
    }
}