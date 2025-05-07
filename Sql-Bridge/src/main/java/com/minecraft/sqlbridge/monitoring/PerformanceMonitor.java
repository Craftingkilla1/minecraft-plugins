package com.minecraft.sqlbridge.monitoring;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.core.utils.TimeUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.connection.ConnectionManager;
import com.minecraft.sqlbridge.logging.EnhancedLogger;
import com.minecraft.sqlbridge.utils.TimeUtilExtended;

import org.bukkit.scheduler.BukkitTask;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced performance monitoring system that tracks detailed metrics
 * about database operations, memory usage, and system health.
 */
public class PerformanceMonitor {

    private final SqlBridgePlugin plugin;
    private final ConnectionManager connectionManager;
    private final EnhancedLogger logger;
    
    // Sampling configuration
    private final int metricsHistorySize;
    private final long samplingInterval;
    
    // Scheduled tasks
    private BukkitTask monitorTask;
    
    // Memory statistics
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    // Query metrics tracking
    private final Map<QueryComplexity, AtomicInteger> queryComplexityCounts = new ConcurrentHashMap<>();
    private final Map<String, QueryTypeStats> queryTypeStats = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> tableAccessCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> slowestQueryByTable = new ConcurrentHashMap<>();
    
    // Performance metrics history
    private final List<PerformanceSnapshot> metricsHistory = new ArrayList<>();
    
    // Current metrics
    private final AtomicLong totalQueryTime = new AtomicLong(0);
    private final AtomicLong totalQueryCount = new AtomicLong(0);
    private final AtomicLong totalPrepareTime = new AtomicLong(0);
    private final AtomicLong totalExecuteTime = new AtomicLong(0);
    private final AtomicLong totalFetchTime = new AtomicLong(0);
    private final AtomicInteger activeTransactions = new AtomicInteger(0);
    private final AtomicInteger preparedStatementsOpen = new AtomicInteger(0);
    private final AtomicInteger resultSetsOpen = new AtomicInteger(0);
    
    // Database load metrics
    private long lastDatabaseLoadCheck = 0;
    private double currentDatabaseLoad = 0.0;
    
    // Patterns for query analysis
    private static final Pattern SELECT_PATTERN = Pattern.compile("SELECT\\s+.+?\\s+FROM\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSERT_PATTERN = Pattern.compile("INSERT\\s+INTO\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPDATE_PATTERN = Pattern.compile("UPDATE\\s+(\\w+)\\s+SET", Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE_PATTERN = Pattern.compile("DELETE\\s+FROM\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOIN_PATTERN = Pattern.compile("JOIN\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHERE_PATTERN = Pattern.compile("WHERE\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern GROUP_BY_PATTERN = Pattern.compile("GROUP\\s+BY", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORDER_BY_PATTERN = Pattern.compile("ORDER\\s+BY", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIMIT_PATTERN = Pattern.compile("LIMIT\\s+\\d+", Pattern.CASE_INSENSITIVE);
    
    /**
     * Create a new performance monitor
     *
     * @param plugin The SQL-Bridge plugin instance
     * @param connectionManager The connection manager
     */
    public PerformanceMonitor(SqlBridgePlugin plugin, ConnectionManager connectionManager) {
        this.plugin = plugin;
        this.connectionManager = connectionManager;
        this.logger = plugin.getEnhancedLogger();
        
        // Load configuration
        this.metricsHistorySize = plugin.getConfig().getInt("monitoring.history-size", 60);
        this.samplingInterval = plugin.getConfig().getLong("monitoring.sampling-interval", 60) * 1000; // Convert to ms
        
        // Initialize query complexity counts
        for (QueryComplexity complexity : QueryComplexity.values()) {
            queryComplexityCounts.put(complexity, new AtomicInteger(0));
        }
        
        // Initialize query type stats
        queryTypeStats.put("SELECT", new QueryTypeStats());
        queryTypeStats.put("INSERT", new QueryTypeStats());
        queryTypeStats.put("UPDATE", new QueryTypeStats());
        queryTypeStats.put("DELETE", new QueryTypeStats());
        queryTypeStats.put("OTHER", new QueryTypeStats());
    }
    
    /**
     * Start monitoring
     */
    public void startMonitoring() {
        logger.info(EnhancedLogger.PERFORMANCE, "Starting performance monitoring");
        
        // Schedule monitoring task
        monitorTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::collectMetrics,
                20 * 5, // 5 seconds initial delay
                20 * (samplingInterval / 1000) // Convert ms to ticks
        );
    }
    
    /**
     * Stop monitoring
     */
    public void stopMonitoring() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
        
        logger.info(EnhancedLogger.PERFORMANCE, "Performance monitoring stopped");
    }
    
    /**
     * Track a query execution with detailed metrics
     *
     * @param sql The SQL query
     * @param params The query parameters
     * @param operation A supplier that executes the query and returns a result
     * @param <T> The result type
     * @return The result of the query
     */
    public <T> T trackQueryExecution(String sql, Object[] params, Supplier<T> operation) {
        // Record start time
        long startTime = System.currentTimeMillis();
        long prepareEndTime = 0;
        long executeEndTime = 0;
        
        try {
            // Analyze query before execution
            analyzeQuery(sql);
            
            // Preparation phase complete
            prepareEndTime = System.currentTimeMillis();
            
            // Execute the query
            T result = operation.get();
            
            // Execution phase complete
            executeEndTime = System.currentTimeMillis();
            
            return result;
        } finally {
            // Record end time and calculate durations
            long endTime = System.currentTimeMillis();
            long totalDuration = endTime - startTime;
            
            // If we don't have phase timings, estimate them
            if (prepareEndTime == 0) {
                prepareEndTime = startTime + (totalDuration / 3);
            }
            if (executeEndTime == 0) {
                executeEndTime = prepareEndTime + (totalDuration / 3);
            }
            
            long prepareTime = prepareEndTime - startTime;
            long executeTime = executeEndTime - prepareEndTime;
            long fetchTime = endTime - executeEndTime;
            
            // Update metrics
            totalQueryTime.addAndGet(totalDuration);
            totalQueryCount.incrementAndGet();
            totalPrepareTime.addAndGet(prepareTime);
            totalExecuteTime.addAndGet(executeTime);
            totalFetchTime.addAndGet(fetchTime);
            
            // Update query type statistics
            updateQueryTypeStats(sql, totalDuration);
            
            // Track table access
            String tableName = extractMainTable(sql);
            if (tableName != null) {
                tableAccessCounts.computeIfAbsent(tableName, k -> new AtomicInteger(0))
                        .incrementAndGet();
                
                // Update slowest query for this table if applicable
                slowestQueryByTable.compute(tableName, (k, v) -> 
                        (v == null || totalDuration > v) ? totalDuration : v);
            }
            
            // Log slow queries
            if (totalDuration > plugin.getConfig().getLong("monitoring.slow-query-threshold", 1000)) {
                logger.warning(EnhancedLogger.PERFORMANCE, "Slow query detected (" + totalDuration + 
                        "ms): " + truncateForLogging(sql) + 
                        " [Prepare: " + prepareTime + "ms, Execute: " + executeTime + 
                        "ms, Fetch: " + fetchTime + "ms]");
            }
        }
    }
    
    /**
     * Track transaction operations
     *
     * @param starting True if transaction is starting, false if ending
     */
    public void trackTransaction(boolean starting) {
        if (starting) {
            activeTransactions.incrementAndGet();
        } else {
            activeTransactions.decrementAndGet();
        }
    }
    
    /**
     * Track prepared statement lifecycle
     *
     * @param opening True if statement is being opened, false if closing
     */
    public void trackPreparedStatement(boolean opening) {
        if (opening) {
            preparedStatementsOpen.incrementAndGet();
        } else {
            preparedStatementsOpen.decrementAndGet();
        }
    }
    
    /**
     * Track result set lifecycle
     *
     * @param opening True if result set is being opened, false if closing
     */
    public void trackResultSet(boolean opening) {
        if (opening) {
            resultSetsOpen.incrementAndGet();
        } else {
            resultSetsOpen.decrementAndGet();
        }
    }
    
    /**
     * Analyze query complexity and update statistics
     *
     * @param sql The SQL query
     */
    private void analyzeQuery(String sql) {
        if (sql == null || sql.isEmpty()) {
            return;
        }
        
        // Normalize query: remove extra whitespace and convert to uppercase
        String normalizedSql = sql.replaceAll("\\s+", " ").trim().toUpperCase();
        
        // Analyze query complexity
        QueryComplexity complexity = determineQueryComplexity(normalizedSql);
        queryComplexityCounts.get(complexity).incrementAndGet();
    }
    
    /**
     * Determine query complexity based on query features
     *
     * @param sql The SQL query
     * @return The query complexity
     */
    private QueryComplexity determineQueryComplexity(String sql) {
        // Count advanced features
        int complexityScore = 0;
        
        // Check for JOINs
        Matcher joinMatcher = JOIN_PATTERN.matcher(sql);
        int joinCount = 0;
        while (joinMatcher.find()) {
            joinCount++;
        }
        
        // Add complexity based on JOIN count
        if (joinCount > 3) {
            complexityScore += 3;
        } else if (joinCount > 0) {
            complexityScore += joinCount;
        }
        
        // Check for WHERE clause
        if (WHERE_PATTERN.matcher(sql).find()) {
            complexityScore += 1;
        }
        
        // Check for GROUP BY
        if (GROUP_BY_PATTERN.matcher(sql).find()) {
            complexityScore += 2;
        }
        
        // Check for ORDER BY
        if (ORDER_BY_PATTERN.matcher(sql).find()) {
            complexityScore += 1;
        }
        
        // Determine complexity level
        if (complexityScore >= 5) {
            return QueryComplexity.HIGH;
        } else if (complexityScore >= 2) {
            return QueryComplexity.MEDIUM;
        } else {
            return QueryComplexity.LOW;
        }
    }
    
    /**
     * Update query type statistics
     *
     * @param sql The SQL query
     * @param duration The query duration
     */
    private void updateQueryTypeStats(String sql, long duration) {
        if (sql == null || sql.isEmpty()) {
            return;
        }
        
        String upperSql = sql.toUpperCase().trim();
        QueryTypeStats stats;
        
        if (upperSql.startsWith("SELECT")) {
            stats = queryTypeStats.get("SELECT");
        } else if (upperSql.startsWith("INSERT")) {
            stats = queryTypeStats.get("INSERT");
        } else if (upperSql.startsWith("UPDATE")) {
            stats = queryTypeStats.get("UPDATE");
        } else if (upperSql.startsWith("DELETE")) {
            stats = queryTypeStats.get("DELETE");
        } else {
            stats = queryTypeStats.get("OTHER");
        }
        
        stats.count.incrementAndGet();
        stats.totalTime.addAndGet(duration);
        
        if (duration > stats.maxTime.get()) {
            stats.maxTime.set(duration);
        }
    }
    
    /**
     * Extract main table name from a query
     *
     * @param sql The SQL query
     * @return The main table name, or null if not found
     */
    private String extractMainTable(String sql) {
        if (sql == null || sql.isEmpty()) {
            return null;
        }
        
        // Check for SELECT queries
        Matcher selectMatcher = SELECT_PATTERN.matcher(sql);
        if (selectMatcher.find()) {
            return selectMatcher.group(1);
        }
        
        // Check for INSERT queries
        Matcher insertMatcher = INSERT_PATTERN.matcher(sql);
        if (insertMatcher.find()) {
            return insertMatcher.group(1);
        }
        
        // Check for UPDATE queries
        Matcher updateMatcher = UPDATE_PATTERN.matcher(sql);
        if (updateMatcher.find()) {
            return updateMatcher.group(1);
        }
        
        // Check for DELETE queries
        Matcher deleteMatcher = DELETE_PATTERN.matcher(sql);
        if (deleteMatcher.find()) {
            return deleteMatcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Collect metrics for monitoring
     */
    private void collectMetrics() {
        try {
            // Create a new snapshot
            PerformanceSnapshot snapshot = new PerformanceSnapshot();
            
            // Set timestamp
            snapshot.timestamp = System.currentTimeMillis();
            
            // Collect memory metrics
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            snapshot.heapUsed = heapUsage.getUsed();
            snapshot.heapMax = heapUsage.getMax();
            
            // Collect query metrics
            snapshot.queriesPerSecond = calculateQueriesPerSecond();
            snapshot.avgQueryTime = calculateAverageQueryTime();
            snapshot.activeTransactions = activeTransactions.get();
            snapshot.preparedStatementsOpen = preparedStatementsOpen.get();
            snapshot.resultSetsOpen = resultSetsOpen.get();
            
            // Collect database load
            snapshot.databaseLoad = getDatabaseLoad();
            
            // Save total queries count for comparison in next snapshot
            snapshot.totalQueries = totalQueryCount.get();
            
            // Add snapshot to history
            synchronized (metricsHistory) {
                metricsHistory.add(snapshot);
                
                // Trim history if needed
                if (metricsHistory.size() > metricsHistorySize) {
                    metricsHistory.remove(0);
                }
            }
            
            // Log periodic performance report if enabled
            if (plugin.getConfig().getBoolean("monitoring.log-performance", true)) {
                logPerformanceReport(snapshot);
            }
        } catch (Exception e) {
            logger.error(EnhancedLogger.PERFORMANCE, "Error collecting performance metrics: " + e.getMessage());
        }
    }
    
    /**
     * Calculate queries per second since the last measurement
     *
     * @return Queries per second
     */
    private double calculateQueriesPerSecond() {
        long currentCount = totalQueryCount.get();
        long currentTime = System.currentTimeMillis();
        
        // Get last snapshot for comparison
        if (!metricsHistory.isEmpty()) {
            PerformanceSnapshot lastSnapshot = metricsHistory.get(metricsHistory.size() - 1);
            long timeDiff = currentTime - lastSnapshot.timestamp;
            
            if (timeDiff > 0) {
                // Convert to queries per second
                return (currentCount - lastSnapshot.totalQueries) * 1000.0 / timeDiff;
            }
        }
        
        return 0.0;
    }
    
    /**
     * Calculate average query time
     *
     * @return Average query time in milliseconds
     */
    private double calculateAverageQueryTime() {
        long count = totalQueryCount.get();
        long time = totalQueryTime.get();
        
        if (count > 0) {
            return (double) time / count;
        }
        
        return 0.0;
    }
    
    /**
     * Get current database load
     * This uses a simple query to measure database responsiveness
     *
     * @return Database load factor (higher means more loaded)
     */
    private double getDatabaseLoad() {
        // Only check load periodically to avoid overloading
        long now = System.currentTimeMillis();
        if (now - lastDatabaseLoadCheck < 10000) { // 10 seconds
            return currentDatabaseLoad;
        }
        
        lastDatabaseLoadCheck = now;
        
        try (Connection conn = connectionManager.getConnection()) {
            // Measure time to execute a simple query
            long start = System.currentTimeMillis();
            
            try (PreparedStatement stmt = conn.prepareStatement("SELECT 1");
                 ResultSet rs = stmt.executeQuery()) {
                rs.next(); // Just to ensure the query completes
            }
            
            long duration = System.currentTimeMillis() - start;
            
            // Calculate load factor - normalize between 0 and 1
            // Assume anything over 100ms is high load
            currentDatabaseLoad = Math.min(duration / 100.0, 1.0);
            
            return currentDatabaseLoad;
        } catch (SQLException e) {
            logger.error(EnhancedLogger.PERFORMANCE, "Error checking database load: " + e.getMessage());
            return 1.0; // Assume maximum load if query fails
        }
    }
    

    /**
     * Log performance report based on current snapshot
     *
     * @param snapshot The current performance snapshot
     */
    private void logPerformanceReport(PerformanceSnapshot snapshot) {
        logger.info(EnhancedLogger.PERFORMANCE, "=== Performance Report ===");
        logger.info(EnhancedLogger.PERFORMANCE, "Time: " + TimeUtilExtended.formatFromMillis(snapshot.timestamp));
        logger.info(EnhancedLogger.PERFORMANCE, String.format("Memory: %.2f MB / %.2f MB (%.1f%%)", 
                snapshot.heapUsed / 1048576.0, snapshot.heapMax / 1048576.0, 
                (snapshot.heapUsed * 100.0 / snapshot.heapMax)));
        logger.info(EnhancedLogger.PERFORMANCE, String.format("Queries: %.2f/sec, Avg: %.2f ms", 
                snapshot.queriesPerSecond, snapshot.avgQueryTime));
        logger.info(EnhancedLogger.PERFORMANCE, String.format("Active Transactions: %d", 
                snapshot.activeTransactions));
        logger.info(EnhancedLogger.PERFORMANCE, String.format("Statements Open: %d, ResultSets Open: %d", 
                snapshot.preparedStatementsOpen, snapshot.resultSetsOpen));
        logger.info(EnhancedLogger.PERFORMANCE, String.format("Database Load: %.2f%%", 
                snapshot.databaseLoad * 100));
        
        // Log query type distribution
        Map<String, Object> queryTypeDistribution = getQueryTypeDistribution();
        if (!queryTypeDistribution.isEmpty()) {
            logger.info(EnhancedLogger.PERFORMANCE, "Query Distribution:");
            for (Map.Entry<String, Object> entry : queryTypeDistribution.entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typeStats = (Map<String, Object>) entry.getValue();
                logger.info(EnhancedLogger.PERFORMANCE, String.format("  %s: %d queries, Avg: %.2f ms, Max: %d ms", 
                        entry.getKey(), typeStats.get("count"), 
                        (Double) typeStats.get("avgTime"), 
                        (Long) typeStats.get("maxTime")));
            }
        }
        
        // Log top accessed tables
        Map<String, Integer> topTables = getTopAccessedTables(5);
        if (!topTables.isEmpty()) {
            logger.info(EnhancedLogger.PERFORMANCE, "Top Accessed Tables:");
            for (Map.Entry<String, Integer> entry : topTables.entrySet()) {
                logger.info(EnhancedLogger.PERFORMANCE, String.format("  %s: %d accesses", 
                        entry.getKey(), entry.getValue()));
            }
        }
    }
    
    /**
     * Get query type distribution
     *
     * @return Map of query type to statistics
     */
    public Map<String, Object> getQueryTypeDistribution() {
        Map<String, Object> result = new HashMap<>();
        
        for (Map.Entry<String, QueryTypeStats> entry : queryTypeStats.entrySet()) {
            QueryTypeStats stats = entry.getValue();
            int count = stats.count.get();
            
            if (count > 0) {
                Map<String, Object> typeStats = new HashMap<>();
                typeStats.put("count", count);
                typeStats.put("totalTime", stats.totalTime.get());
                typeStats.put("maxTime", stats.maxTime.get());
                typeStats.put("avgTime", (double) stats.totalTime.get() / count);
                
                result.put(entry.getKey(), typeStats);
            }
        }
        
        return result;
    }
    
    /**
     * Get top accessed tables
     *
     * @param limit Maximum number of tables to return
     * @return Map of table name to access count
     */
    public Map<String, Integer> getTopAccessedTables(int limit) {
        Map<String, Integer> result = new LinkedHashMap<>();
        
        tableAccessCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().get(), a.getValue().get()))
                .limit(limit)
                .forEach(entry -> result.put(entry.getKey(), entry.getValue().get()));
        
        return result;
    }
    
    /**
     * Get performance history
     *
     * @return List of performance snapshots
     */
    public List<PerformanceSnapshot> getPerformanceHistory() {
        synchronized (metricsHistory) {
            return new ArrayList<>(metricsHistory);
        }
    }
    
    /**
     * Get current performance metrics
     *
     * @return Map of metric name to value
     */
    public Map<String, Object> getCurrentMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Add memory metrics
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        metrics.put("heapUsed", heapUsage.getUsed());
        metrics.put("heapMax", heapUsage.getMax());
        metrics.put("heapUtilization", (double) heapUsage.getUsed() / heapUsage.getMax());
        
        // Add query metrics
        metrics.put("totalQueries", totalQueryCount.get());
        metrics.put("totalQueryTime", totalQueryTime.get());
        metrics.put("avgQueryTime", calculateAverageQueryTime());
        metrics.put("queriesPerSecond", calculateQueriesPerSecond());
        
        // Add phase timing metrics
        metrics.put("totalPrepareTime", totalPrepareTime.get());
        metrics.put("totalExecuteTime", totalExecuteTime.get());
        metrics.put("totalFetchTime", totalFetchTime.get());
        metrics.put("avgPrepareTime", totalQueryCount.get() > 0 ? 
                (double) totalPrepareTime.get() / totalQueryCount.get() : 0);
        metrics.put("avgExecuteTime", totalQueryCount.get() > 0 ? 
                (double) totalExecuteTime.get() / totalQueryCount.get() : 0);
        metrics.put("avgFetchTime", totalQueryCount.get() > 0 ? 
                (double) totalFetchTime.get() / totalQueryCount.get() : 0);
        
        // Add resource metrics
        metrics.put("activeTransactions", activeTransactions.get());
        metrics.put("preparedStatementsOpen", preparedStatementsOpen.get());
        metrics.put("resultSetsOpen", resultSetsOpen.get());
        
        // Add database load
        metrics.put("databaseLoad", getDatabaseLoad());
        
        return metrics;
    }
    
    /**
     * Reset all metrics
     */
    public void resetMetrics() {
        totalQueryTime.set(0);
        totalQueryCount.set(0);
        totalPrepareTime.set(0);
        totalExecuteTime.set(0);
        totalFetchTime.set(0);
        
        // Reset query complexity counts
        for (AtomicInteger count : queryComplexityCounts.values()) {
            count.set(0);
        }
        
        // Reset query type stats
        for (QueryTypeStats stats : queryTypeStats.values()) {
            stats.count.set(0);
            stats.totalTime.set(0);
            stats.maxTime.set(0);
        }
        
        // Reset table access counts
        tableAccessCounts.clear();
        slowestQueryByTable.clear();
        
        // Clear metrics history
        synchronized (metricsHistory) {
            metricsHistory.clear();
        }
        
        logger.info(EnhancedLogger.PERFORMANCE, "Performance metrics reset");
    }
    
    /**
     * Truncate SQL query for logging
     *
     * @param sql The SQL query
     * @return Truncated SQL query
     */
    private String truncateForLogging(String sql) {
        if (sql == null) {
            return "null";
        }
        
        int maxLength = 100;
        if (sql.length() <= maxLength) {
            return sql;
        }
        
        return sql.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Performance snapshot class
     */
    public static class PerformanceSnapshot {
        public long timestamp;
        public long heapUsed;
        public long heapMax;
        public double queriesPerSecond;
        public double avgQueryTime;
        public int activeTransactions;
        public int preparedStatementsOpen;
        public int resultSetsOpen;
        public double databaseLoad;
        public long totalQueries;
        
        public PerformanceSnapshot() {
            // Initialize with default values
            this.timestamp = System.currentTimeMillis();
            this.heapUsed = 0;
            this.heapMax = 0;
            this.queriesPerSecond = 0;
            this.avgQueryTime = 0;
            this.activeTransactions = 0;
            this.preparedStatementsOpen = 0;
            this.resultSetsOpen = 0;
            this.databaseLoad = 0;
            this.totalQueries = 0;
        }
    }
    
    /**
     * Query type statistics
     */
    private static class QueryTypeStats {
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicLong totalTime = new AtomicLong(0);
        final AtomicLong maxTime = new AtomicLong(0);
    }
    
    /**
     * Query complexity enum
     */
    public enum QueryComplexity {
        LOW,
        MEDIUM,
        HIGH
    }
}