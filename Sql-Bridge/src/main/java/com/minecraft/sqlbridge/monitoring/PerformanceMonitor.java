// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/monitoring/PerformanceMonitor.java
package com.minecraft.sqlbridge.monitoring;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.connection.ConnectionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitors the performance of database operations.
 */
public class PerformanceMonitor {
    
    private final SqlBridgePlugin plugin;
    private final ConnectionManager connectionManager;
    private final QueryStatistics queryStatistics;
    private final boolean collectMetrics;
    private final int metricsInterval;
    private final ScheduledExecutorService scheduler;
    
    // Performance metrics
    private final AtomicLong totalOperations = new AtomicLong(0);
    private final AtomicLong totalOperationTime = new AtomicLong(0);
    
    /**
     * Constructor for PerformanceMonitor.
     *
     * @param plugin The SQL-Bridge plugin instance
     * @param connectionManager The connection manager
     */
    public PerformanceMonitor(SqlBridgePlugin plugin, ConnectionManager connectionManager) {
        this.plugin = plugin;
        this.connectionManager = connectionManager;
        this.queryStatistics = new QueryStatistics(plugin);
        this.collectMetrics = plugin.getPluginConfig().isCollectMetrics();
        this.metricsInterval = plugin.getPluginConfig().getMetricsInterval();
        
        // Create scheduler for periodic metrics collection
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                r -> {
                    Thread thread = new Thread(r, "SqlBridge-PerformanceMonitor");
                    thread.setDaemon(true);
                    return thread;
                });
    }
    
    /**
     * Start performance monitoring.
     */
    public void start() {
        if (collectMetrics) {
            // Schedule metrics collection
            scheduler.scheduleAtFixedRate(
                this::collectMetrics,
                metricsInterval,
                metricsInterval,
                TimeUnit.SECONDS
            );
            
            LogUtil.info("Performance monitoring started with interval of " + metricsInterval + " seconds");
        }
    }
    
    /**
     * Collect performance metrics.
     */
    private void collectMetrics() {
        try {
            // Get connection pool metrics
            Map<String, Object> connectionMetrics = connectionManager.getStatistics();
            
            // Get query statistics
            Map<String, Object> queryMetrics = queryStatistics.getStatistics();
            
            // Log metrics at debug level
            if (plugin.getPluginConfig().isDebugMode()) {
                LogUtil.info("Performance metrics:");
                
                for (Map.Entry<String, Object> entry : connectionMetrics.entrySet()) {
                    LogUtil.info("  " + entry.getKey() + ": " + entry.getValue());
                }
                
                for (Map.Entry<String, Object> entry : queryMetrics.entrySet()) {
                    LogUtil.info("  " + entry.getKey() + ": " + entry.getValue());
                }
            }
            
            // Log slow queries
            long slowQueries = (long) queryMetrics.getOrDefault("slowQueries", 0L);
            if (slowQueries > 0) {
                LogUtil.warning("Detected " + slowQueries + " slow queries");
            }
        } catch (Exception e) {
            LogUtil.warning("Error collecting performance metrics: " + e.getMessage());
        }
    }
    
    /**
     * Record an operation.
     *
     * @param operationTime The time taken for the operation in milliseconds
     */
    public void recordOperation(long operationTime) {
        totalOperations.incrementAndGet();
        totalOperationTime.addAndGet(operationTime);
    }
    
    /**
     * Get performance statistics.
     *
     * @return A map of statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Add our statistics
        stats.put("totalOperations", totalOperations.get());
        stats.put("totalOperationTime", totalOperationTime.get());
        
        // Calculate average operation time
        long ops = totalOperations.get();
        if (ops > 0) {
            double avgTime = (double) totalOperationTime.get() / ops;
            stats.put("averageOperationTime", avgTime);
        } else {
            stats.put("averageOperationTime", 0.0);
        }
        
        // Add query statistics
        stats.putAll(queryStatistics.getStatistics());
        
        return stats;
    }
    
    /**
     * Shutdown the performance monitor.
     */
    public void shutdown() {
        // Shutdown the scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        LogUtil.info("Performance monitor shutdown completed");
    }
}