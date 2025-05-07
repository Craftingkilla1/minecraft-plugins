package com.minecraft.sqlbridge.connection;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.core.utils.TimeUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;

import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Advanced connection monitoring system that integrates with Core-Utils.
 * Tracks connection health, detects issues, and coordinates recovery.
 */
public class ConnectionMonitor {

    private final SqlBridgePlugin plugin;
    private final ConnectionManager connectionManager;
    private final Map<String, ConnectionMetric> metrics = new ConcurrentHashMap<>();
    
    // Monitoring settings
    private final long healthCheckInterval;
    private final long connectionMaxIdleTime;
    private final int maxFailedChecks;
    private final int criticalFailureThreshold;
    
    // Connection state tracking
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger leakedConnections = new AtomicInteger(0);
    private final AtomicInteger failedHealthChecks = new AtomicInteger(0);
    private final AtomicInteger totalHealthChecks = new AtomicInteger(0);
    
    // Scheduled tasks
    private BukkitTask monitorTask;
    
    // Performance tracking
    private final List<ConnectionTimingRecord> timingRecords = new ArrayList<>();
    private final int maxTimingRecords = 100;
    
    /**
     * Create a new connection monitor
     *
     * @param plugin The SQL-Bridge plugin instance
     * @param connectionManager The connection manager
     */
    public ConnectionMonitor(SqlBridgePlugin plugin, ConnectionManager connectionManager) {
        this.plugin = plugin;
        this.connectionManager = connectionManager;
        
        // Load configuration
        this.healthCheckInterval = plugin.getConfig().getLong("monitor.health-check-interval", 60) * 1000; // Convert to ms
        this.connectionMaxIdleTime = plugin.getConfig().getLong("monitor.max-idle-time", 600) * 1000; // Convert to ms
        this.maxFailedChecks = plugin.getConfig().getInt("monitor.max-failed-checks", 3);
        this.criticalFailureThreshold = plugin.getConfig().getInt("monitor.critical-threshold", 5);
    }
    
    /**
     * Start monitoring connections
     */
    public void startMonitoring() {
        LogUtil.info("Starting connection monitoring system");
        
        // Schedule health check task
        monitorTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::performHealthCheck,
                20 * 10, // Initial delay: 10 seconds
                20 * (healthCheckInterval / 1000) // Convert ms to ticks
        );
        
        LogUtil.info("Connection monitoring started with " + 
                (healthCheckInterval / 1000) + " second check interval");
    }
    
    /**
     * Stop monitoring connections
     */
    public void stopMonitoring() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
            LogUtil.info("Connection monitoring stopped");
        }
    }
    
    /**
     * Track connection acquisition
     *
     * @param connection The connection
     */
    public void trackConnection(Connection connection) {
        // Increment active connections
        activeConnections.incrementAndGet();
        
        // Track the connection
        String id = createConnectionId(connection);
        metrics.put(id, new ConnectionMetric(id, System.currentTimeMillis()));
        
        // Track timing
        trackTiming("acquire", () -> {
            // Just return the connection, we're just timing the acquisition
            return connection;
        });
    }
    
    /**
     * Track connection release
     *
     * @param connection The connection
     */
    public void trackConnectionRelease(Connection connection) {
        // Decrement active connections
        activeConnections.decrementAndGet();
        
        // Remove connection from metrics
        String id = createConnectionId(connection);
        metrics.remove(id);
        
        // Track timing
        trackTiming("release", () -> {
            // Just return null, we're just timing the release
            return null;
        });
    }
    
    /**
     * Track connection operation timing
     *
     * @param operationType The operation type
     * @param operation The operation to time
     * @param <T> The operation result type
     * @return The operation result
     */
    public <T> T trackTiming(String operationType, Supplier<T> operation) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Execute the operation
            return operation.get();
        } finally {
            // Record timing
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // Add timing record
            synchronized (timingRecords) {
                timingRecords.add(new ConnectionTimingRecord(operationType, duration, startTime));
                
                // Trim records if needed
                if (timingRecords.size() > maxTimingRecords) {
                    timingRecords.remove(0);
                }
            }
        }
    }
    
    /**
     * Perform a health check on all connections
     */
    private void performHealthCheck() {
        // Increment total health checks
        totalHealthChecks.incrementAndGet();
        
        // Log start of health check if debug is enabled
        if (plugin.getConfig().getBoolean("debug.log-connections", false)) {
            LogUtil.info("Performing connection health check");
        }
        
        try {
            // Get a test connection to check overall database health
            try (Connection testConn = connectionManager.getConnection()) {
                // Test the connection
                if (!testConn.isValid(5)) {
                    // Connection is invalid, increment failed checks
                    failedHealthChecks.incrementAndGet();
                    LogUtil.warning("Database health check failed - connection is invalid");
                    
                    // If we've failed too many times, try to reinitialize
                    if (failedHealthChecks.get() >= maxFailedChecks) {
                        LogUtil.severe("Too many failed health checks - attempting to reinitialize connection pool");
                        connectionManager.reinitialize();
                        failedHealthChecks.set(0);
                    }
                    
                    return;
                }
            }
            
            // Reset failed health checks since we got a valid connection
            failedHealthChecks.set(0);
            
            // Check for connection leaks and idle connections
            long now = System.currentTimeMillis();
            int idleConnections = 0;
            
            // Iterate through metrics to find leaked and idle connections
            for (Map.Entry<String, ConnectionMetric> entry : metrics.entrySet()) {
                ConnectionMetric metric = entry.getValue();
                long age = now - metric.created;
                
                if (age > connectionMaxIdleTime) {
                    idleConnections++;
                    
                    // Log idle connection if debug is enabled
                    if (plugin.getConfig().getBoolean("debug.log-connections", false)) {
                        LogUtil.debug("Idle connection detected: " + metric.id + 
                                " (age: " + TimeUtil.formatTime((int)(age / 1000)) + ")");
                    }
                }
            }
            
            // Log results if debug is enabled or there are idle connections
            if (plugin.getConfig().getBoolean("debug.log-connections", false) || idleConnections > 0) {
                LogUtil.info("Connection health check: " + 
                        activeConnections.get() + " active, " + 
                        idleConnections + " idle, " + 
                        leakedConnections.get() + " leaked");
            }
            
            // If we have idle connections exceeding a threshold, trigger cleanup
            if (idleConnections > plugin.getConfig().getInt("monitor.idle-cleanup-threshold", 5)) {
                LogUtil.info("Idle connection cleanup triggered");
                connectionManager.closeIdleConnections();
            }
            
        } catch (SQLException e) {
            // Health check query failed
            failedHealthChecks.incrementAndGet();
            LogUtil.severe("Database health check failed with error: " + e.getMessage());
            
            // If we've reached the critical threshold, force reinitialize
            if (failedHealthChecks.get() >= criticalFailureThreshold) {
                LogUtil.severe("Critical failure threshold reached - forcing connection pool reinitialize");
                connectionManager.reinitialize();
                failedHealthChecks.set(0);
            }
        }
    }
    
    /**
     * Create a unique identifier for a connection
     *
     * @param connection The connection
     * @return The connection ID
     */
    private String createConnectionId(Connection connection) {
        // Use the connection's identity hash code as a unique identifier
        return String.valueOf(System.identityHashCode(connection));
    }
    
    /**
     * Get connection monitoring statistics
     *
     * @return Map of statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("activeConnections", activeConnections.get());
        stats.put("leakedConnections", leakedConnections.get());
        stats.put("totalHealthChecks", totalHealthChecks.get());
        stats.put("failedHealthChecks", failedHealthChecks.get());
        stats.put("healthCheckInterval", healthCheckInterval);
        stats.put("maxIdleTime", connectionMaxIdleTime);
        
        // Add timing statistics
        Map<String, Object> timingStats = calculateTimingStatistics();
        stats.put("timingStats", timingStats);
        
        return stats;
    }
    
    /**
     * Calculate timing statistics from collected records
     *
     * @return Map of timing statistics
     */
    private Map<String, Object> calculateTimingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        Map<String, List<Long>> timingsByType = new HashMap<>();
        
        // Group timings by operation type
        synchronized (timingRecords) {
            for (ConnectionTimingRecord record : timingRecords) {
                timingsByType.computeIfAbsent(record.operationType, k -> new ArrayList<>())
                        .add(record.duration);
            }
        }
        
        // Calculate statistics for each operation type
        for (Map.Entry<String, List<Long>> entry : timingsByType.entrySet()) {
            String type = entry.getKey();
            List<Long> timings = entry.getValue();
            
            if (timings.isEmpty()) {
                continue;
            }
            
            // Calculate average, min, max
            long sum = 0;
            long min = Long.MAX_VALUE;
            long max = 0;
            
            for (Long timing : timings) {
                sum += timing;
                min = Math.min(min, timing);
                max = Math.max(max, timing);
            }
            
            double avg = (double) sum / timings.size();
            
            // Add to stats
            Map<String, Object> typeStats = new HashMap<>();
            typeStats.put("count", timings.size());
            typeStats.put("avg", avg);
            typeStats.put("min", min);
            typeStats.put("max", max);
            
            stats.put(type, typeStats);
        }
        
        return stats;
    }
    
    /**
     * Reset monitoring statistics
     */
    public void resetStatistics() {
        failedHealthChecks.set(0);
        totalHealthChecks.set(0);
        leakedConnections.set(0);
        
        synchronized (timingRecords) {
            timingRecords.clear();
        }
    }
    
    /**
     * Connection metric class for tracking connection state
     */
    private static class ConnectionMetric {
        final String id;
        final long created;
        volatile long lastUsed;
        
        ConnectionMetric(String id, long created) {
            this.id = id;
            this.created = created;
            this.lastUsed = created;
        }
        
        void markUsed() {
            this.lastUsed = System.currentTimeMillis();
        }
    }
    
    /**
     * Connection timing record class for tracking operation performance
     */
    private static class ConnectionTimingRecord {
        final String operationType;
        final long duration;
        final long timestamp;
        
        ConnectionTimingRecord(String operationType, long duration, long timestamp) {
            this.operationType = operationType;
            this.duration = duration;
            this.timestamp = timestamp;
        }
    }
}