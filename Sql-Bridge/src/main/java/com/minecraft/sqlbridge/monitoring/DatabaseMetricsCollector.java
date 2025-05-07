package com.minecraft.sqlbridge.monitoring;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.api.DatabaseType;
import com.minecraft.sqlbridge.connection.ConnectionManager;
import com.minecraft.sqlbridge.logging.EnhancedLogger;
import com.minecraft.sqlbridge.utils.TimeUtilExtended;

import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Collects database-level metrics by querying the database system tables.
 * Different implementations for different database types (MySQL, SQLite, PostgreSQL, etc.)
 */
public class DatabaseMetricsCollector {

    private final SqlBridgePlugin plugin;
    private final ConnectionManager connectionManager;
    private final DatabaseType databaseType;
    private BukkitTask collectorTask;
    private final EnhancedLogger logger;
    private final AtomicBoolean collecting = new AtomicBoolean(false);
    
    // Last collected metrics
    private Map<String, Object> lastMetrics = new HashMap<>();
    
    /**
     * Create a new database metrics collector
     *
     * @param plugin The SQL-Bridge plugin instance
     * @param connectionManager The connection manager
     */
    public DatabaseMetricsCollector(SqlBridgePlugin plugin, ConnectionManager connectionManager) {
        this.plugin = plugin;
        this.connectionManager = connectionManager;
        this.databaseType = connectionManager.getType();
        this.logger = plugin.getEnhancedLogger();
    }
    
    /**
     * Start metrics collection
     */
    public void startCollection() {
        // Schedule metrics collection
        int interval = plugin.getConfig().getInt("monitoring.db-metrics-interval", 300); // 5 minutes default
        
        // Convert to ticks (20 ticks per second)
        int ticks = interval * 20;
        
        collectorTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::collectMetrics,
                ticks,  // Initial delay
                ticks   // Repeat interval
        );
        
        logger.info(EnhancedLogger.PERFORMANCE, "Database metrics collection started with " + 
                interval + " second interval");
    }
    
    /**
     * Stop metrics collection
     */
    public void stopCollection() {
        if (collectorTask != null) {
            collectorTask.cancel();
            collectorTask = null;
            
            logger.info(EnhancedLogger.PERFORMANCE, "Database metrics collection stopped");
        }
    }
    
    /**
     * Collect database metrics
     */
    public void collectMetrics() {
        // Prevent concurrent collection
        if (!collecting.compareAndSet(false, true)) {
            return;
        }
        
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            // Collect database-specific metrics
            switch (databaseType) {
                case MYSQL:
                    collectMySQLMetrics(metrics);
                    break;
                case POSTGRESQL:
                    collectPostgreSQLMetrics(metrics);
                    break;
                case SQLITE:
                    collectSQLiteMetrics(metrics);
                    break;
                case H2:
                    collectH2Metrics(metrics);
                    break;
                default:
                    logger.warning(EnhancedLogger.PERFORMANCE, 
                            "Unsupported database type for metrics collection: " + databaseType);
                    break;
            }
            
            // Update last metrics
            lastMetrics = metrics;
            
            // Log metrics if enabled
            if (plugin.getConfig().getBoolean("monitoring.log-db-metrics", true)) {
                logDatabaseMetrics(metrics);
            }
        } catch (Exception e) {
            logger.error(EnhancedLogger.PERFORMANCE, 
                    "Error collecting database metrics: " + e.getMessage());
        } finally {
            collecting.set(false);
        }
    }
    
    /**
     * Collect MySQL database metrics
     * 
     * @param metrics The metrics map to populate
     */
    private void collectMySQLMetrics(Map<String, Object> metrics) {
        try (Connection connection = connectionManager.getConnection()) {
            // Collect global status variables
            collectMySQLStatusVariables(connection, metrics);
            
            // Collect table metrics
            collectMySQLTableMetrics(connection, metrics);
            
            // Collect process information
            collectMySQLProcessList(connection, metrics);
            
            // Collect innodb metrics if available
            collectMySQLInnoDBMetrics(connection, metrics);
        } catch (SQLException e) {
            logger.error(EnhancedLogger.PERFORMANCE, 
                    "Error collecting MySQL metrics: " + e.getMessage());
        }
    }
    
    /**
     * Collect MySQL status variables
     * 
     * @param connection Database connection
     * @param metrics The metrics map to populate
     * @throws SQLException If a database error occurs
     */
    private void collectMySQLStatusVariables(Connection connection, Map<String, Object> metrics) 
            throws SQLException {
        Map<String, Object> statusMetrics = new HashMap<>();
        
        try (PreparedStatement stmt = connection.prepareStatement("SHOW GLOBAL STATUS");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String name = rs.getString(1);
                String value = rs.getString(2);
                
                // Only include numeric values and important non-numeric values
                try {
                    statusMetrics.put(name, Long.parseLong(value));
                } catch (NumberFormatException e) {
                    // Skip non-numeric values except for a few important ones
                    if (name.equals("Uptime") || name.startsWith("Innodb") || 
                            name.contains("buffer") || name.contains("cache")) {
                        statusMetrics.put(name, value);
                    }
                }
            }
        }
        
        // Add derived metrics
        if (statusMetrics.containsKey("Questions") && statusMetrics.containsKey("Uptime")) {
            long questions = parseLong(statusMetrics.get("Questions"), 0);
            long uptime = parseLong(statusMetrics.get("Uptime"), 1);
            
            // Queries per second
            statusMetrics.put("QueriesPerSecond", (double) questions / uptime);
        }
        
        metrics.put("status", statusMetrics);
    }
    
    /**
     * Collect MySQL table metrics
     * 
     * @param connection Database connection
     * @param metrics The metrics map to populate
     * @throws SQLException If a database error occurs
     */
    private void collectMySQLTableMetrics(Connection connection, Map<String, Object> metrics) 
            throws SQLException {
        List<Map<String, Object>> tableMetrics = new ArrayList<>();
        
        // Get the current database name
        String currentDb = null;
        try (PreparedStatement stmt = connection.prepareStatement("SELECT DATABASE()");
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                currentDb = rs.getString(1);
            }
        }
        
        if (currentDb == null) {
            return;
        }
        
        // Query table stats
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT TABLE_NAME, ENGINE, TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH " +
                "FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = ? " +
                "ORDER BY DATA_LENGTH DESC");
        ) {
            stmt.setString(1, currentDb);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> table = new HashMap<>();
                    table.put("name", rs.getString("TABLE_NAME"));
                    table.put("engine", rs.getString("ENGINE"));
                    table.put("rows", rs.getLong("TABLE_ROWS"));
                    table.put("dataSize", rs.getLong("DATA_LENGTH"));
                    table.put("indexSize", rs.getLong("INDEX_LENGTH"));
                    
                    tableMetrics.add(table);
                }
            }
        }
        
        metrics.put("tables", tableMetrics);
    }
    
    /**
     * Collect MySQL process list
     * 
     * @param connection Database connection
     * @param metrics The metrics map to populate
     * @throws SQLException If a database error occurs
     */
    private void collectMySQLProcessList(Connection connection, Map<String, Object> metrics) 
            throws SQLException {
        Map<String, Integer> processStats = new HashMap<>();
        processStats.put("total", 0);
        processStats.put("active", 0);
        processStats.put("sleeping", 0);
        
        try (PreparedStatement stmt = connection.prepareStatement("SHOW PROCESSLIST");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                processStats.put("total", processStats.get("total") + 1);
                
                String state = rs.getString("Command");
                if ("Sleep".equalsIgnoreCase(state)) {
                    processStats.put("sleeping", processStats.get("sleeping") + 1);
                } else {
                    processStats.put("active", processStats.get("active") + 1);
                }
            }
        }
        
        metrics.put("processes", processStats);
    }
    
    /**
     * Collect MySQL InnoDB metrics
     * 
     * @param connection Database connection
     * @param metrics The metrics map to populate
     * @throws SQLException If a database error occurs
     */
    private void collectMySQLInnoDBMetrics(Connection connection, Map<String, Object> metrics) 
            throws SQLException {
        Map<String, Object> innodbMetrics = new HashMap<>();
        
        try (PreparedStatement stmt = connection.prepareStatement("SHOW ENGINE INNODB STATUS");
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                String status = rs.getString("Status");
                
                // Extract key metrics from the status text
                innodbMetrics.put("rawStatus", status);
                
                // Parse buffer pool info
                if (status.contains("Buffer pool size")) {
                    String bufferPoolSizeStr = extractValue(status, "Buffer pool size", "\\n");
                    if (bufferPoolSizeStr != null) {
                        innodbMetrics.put("bufferPoolSize", bufferPoolSizeStr);
                    }
                }
                
                // Parse transaction info
                if (status.contains("History list length")) {
                    String historyListLengthStr = extractValue(status, "History list length", "\\n");
                    if (historyListLengthStr != null) {
                        innodbMetrics.put("historyListLength", historyListLengthStr);
                    }
                }
            }
        } catch (SQLException e) {
            // Some users might not have privileges to run this command
            logger.debug(EnhancedLogger.PERFORMANCE, "Could not collect InnoDB status: " + e.getMessage());
        }
        
        metrics.put("innodb", innodbMetrics);
    }
    
    /**
     * Collect PostgreSQL database metrics
     * 
     * @param metrics The metrics map to populate
     */
    private void collectPostgreSQLMetrics(Map<String, Object> metrics) {
        try (Connection connection = connectionManager.getConnection()) {
            // Collect database stats
            collectPostgreSQLDatabaseStats(connection, metrics);
            
            // Collect table metrics
            collectPostgreSQLTableMetrics(connection, metrics);
            
            // Collect active queries
            collectPostgreSQLActiveQueries(connection, metrics);
            
            // Collect index metrics
            collectPostgreSQLIndexMetrics(connection, metrics);
        } catch (SQLException e) {
            logger.error(EnhancedLogger.PERFORMANCE, 
                    "Error collecting PostgreSQL metrics: " + e.getMessage());
        }
    }
    
    /**
     * Collect PostgreSQL database stats
     * 
     * @param connection Database connection
     * @param metrics The metrics map to populate
     * @throws SQLException If a database error occurs
     */
    private void collectPostgreSQLDatabaseStats(Connection connection, Map<String, Object> metrics) 
            throws SQLException {
        Map<String, Object> dbStats = new HashMap<>();
        
        // Get database size
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT pg_database_size(current_database())");
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                dbStats.put("databaseSize", rs.getLong(1));
            }
        }
        
        // Get transaction stats
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT xact_commit, xact_rollback, blks_read, blks_hit " +
                "FROM pg_stat_database WHERE datname = current_database()");
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                dbStats.put("transactionsCommitted", rs.getLong("xact_commit"));
                dbStats.put("transactionsRolledBack", rs.getLong("xact_rollback"));
                dbStats.put("blocksRead", rs.getLong("blks_read"));
                dbStats.put("blocksHit", rs.getLong("blks_hit"));
                
                // Calculate cache hit ratio
                long blocksRead = rs.getLong("blks_read");
                long blocksHit = rs.getLong("blks_hit");
                double cacheHitRatio = (blocksRead + blocksHit) > 0 
                        ? (double) blocksHit / (blocksRead + blocksHit) 
                        : 0;
                dbStats.put("cacheHitRatio", cacheHitRatio);
            }
        }
        
        metrics.put("database", dbStats);
    }
    
    /**
     * Collect PostgreSQL table metrics
     * 
     * @param connection Database connection
     * @param metrics The metrics map to populate
     * @throws SQLException If a database error occurs
     */
    private void collectPostgreSQLTableMetrics(Connection connection, Map<String, Object> metrics) 
            throws SQLException {
        List<Map<String, Object>> tableMetrics = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT relname, n_live_tup, n_dead_tup, seq_scan, idx_scan " +
                "FROM pg_stat_user_tables " +
                "ORDER BY n_live_tup DESC");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Map<String, Object> table = new HashMap<>();
                table.put("name", rs.getString("relname"));
                table.put("liveRows", rs.getLong("n_live_tup"));
                table.put("deadRows", rs.getLong("n_dead_tup"));
                table.put("sequentialScans", rs.getLong("seq_scan"));
                table.put("indexScans", rs.getLong("idx_scan"));
                
                tableMetrics.add(table);
            }
        }
        
        metrics.put("tables", tableMetrics);
    }
    
    /**
     * Collect PostgreSQL active queries
     * 
     * @param connection Database connection
     * @param metrics The metrics map to populate
     * @throws SQLException If a database error occurs
     */
    private void collectPostgreSQLActiveQueries(Connection connection, Map<String, Object> metrics) 
            throws SQLException {
        Map<String, Integer> queryStats = new HashMap<>();
        queryStats.put("total", 0);
        queryStats.put("active", 0);
        queryStats.put("idle", 0);
        
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT state, count(*) FROM pg_stat_activity GROUP BY state");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String state = rs.getString("state");
                int count = rs.getInt(2);
                
                queryStats.put("total", queryStats.get("total") + count);
                
                if ("active".equalsIgnoreCase(state)) {
                    queryStats.put("active", count);
                } else if ("idle".equalsIgnoreCase(state)) {
                    queryStats.put("idle", count);
                }
            }
        }
        
        metrics.put("queries", queryStats);
    }
    
    /**
     * Collect PostgreSQL index metrics
     * 
     * @param connection Database connection
     * @param metrics The metrics map to populate
     * @throws SQLException If a database error occurs
     */
    private void collectPostgreSQLIndexMetrics(Connection connection, Map<String, Object> metrics) 
            throws SQLException {
        List<Map<String, Object>> indexMetrics = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT indexrelname, relname, idx_scan, idx_tup_read, idx_tup_fetch " +
                "FROM pg_stat_user_indexes " +
                "ORDER BY idx_scan DESC " +
                "LIMIT 10");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Map<String, Object> index = new HashMap<>();
                index.put("name", rs.getString("indexrelname"));
                index.put("table", rs.getString("relname"));
                index.put("scans", rs.getLong("idx_scan"));
                index.put("tupleReads", rs.getLong("idx_tup_read"));
                index.put("tupleFetches", rs.getLong("idx_tup_fetch"));
                
                indexMetrics.add(index);
            }
        }
        
        metrics.put("indexes", indexMetrics);
    }
    
    /**
     * Collect SQLite database metrics
     * 
     * @param metrics The metrics map to populate
     */
    private void collectSQLiteMetrics(Map<String, Object> metrics) {
        try (Connection connection = connectionManager.getConnection()) {
            // Basic database info
            collectSQLiteBasicInfo(connection, metrics);
            
            // Table metrics
            collectSQLiteTableMetrics(connection, metrics);
            
            // Index metrics
            collectSQLiteIndexMetrics(connection, metrics);
            
            // Pragma stats
            collectSQLitePragmaStats(connection, metrics);
        } catch (SQLException e) {
            logger.error(EnhancedLogger.PERFORMANCE, 
                    "Error collecting SQLite metrics: " + e.getMessage());
        }
    }
    
    /**
     * Collect SQLite basic information
     * 
     * @param connection Database connection
     * @param metrics The metrics map to populate
     * @throws SQLException If a database error occurs
     */
    private void collectSQLiteBasicInfo(Connection connection, Map<String, Object> metrics) 
            throws SQLException {
        Map<String, Object> basicInfo = new HashMap<>();
        
        // Get SQLite version
        try (PreparedStatement stmt = connection.prepareStatement("SELECT sqlite_version()");
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                basicInfo.put("version", rs.getString(1));
            }
        }
        
        // Get database filename
        try (PreparedStatement stmt = connection.prepareStatement("PRAGMA database_list");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                if ("main".equals(rs.getString("name"))) {
                    basicInfo.put("file", rs.getString("file"));
                    break;
                }
            }
        }
        
        metrics.put("basic", basicInfo);
    }
    
    /**
     * Collect SQLite table metrics
     * 
     * @param connection Database connection
     * @param metrics The metrics map to populate
     * @throws SQLException If a database error occurs
     */
    private void collectSQLiteTableMetrics(Connection connection, Map<String, Object> metrics) 
            throws SQLException {
        List<Map<String, Object>> tableMetrics = new ArrayList<>();
        
        // Get list of tables
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String tableName = rs.getString("name");
                Map<String, Object> table = new HashMap<>();
                table.put("name", tableName);
                
                // Get row count for this table
                try (PreparedStatement countStmt = connection.prepareStatement(
                        "SELECT COUNT(*) FROM " + tableName)) {
                    try (ResultSet countRs = countStmt.executeQuery()) {
                        if (countRs.next()) {
                            table.put("rows", countRs.getLong(1));
                        }
                    }
                } catch (SQLException e) {
                    // Skip this table if we can't get row count
                    logger.debug(EnhancedLogger.PERFORMANCE, 
                            "Could not get row count for table " + tableName + ": " + e.getMessage());
                }
                
                tableMetrics.add(table);
            }
        }
        
        metrics.put("tables", tableMetrics);
    }
    
    /**
     * Collect SQLite index metrics
     * 
     * @param connection Database connection
     * @param metrics The metrics map to populate
     * @throws SQLException If a database error occurs
     */
    private void collectSQLiteIndexMetrics(Connection connection, Map<String, Object> metrics) 
            throws SQLException {
        List<Map<String, Object>> indexMetrics = new ArrayList<>();
        
        // Get list of indexes
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT name, tbl_name FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%'");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Map<String, Object> index = new HashMap<>();
                index.put("name", rs.getString("name"));
                index.put("table", rs.getString("tbl_name"));
                
                indexMetrics.add(index);
            }
        }
        
        metrics.put("indexes", indexMetrics);
    }
    
    /**
     * Collect SQLite pragma statistics
     * 
     * @param connection Database connection
     * @param metrics The metrics map to populate
     * @throws SQLException If a database error occurs
     */
    private void collectSQLitePragmaStats(Connection connection, Map<String, Object> metrics) 
            throws SQLException {
        Map<String, Object> pragmaStats = new HashMap<>();
        
        // Collect various PRAGMA statistics
        String[] pragmas = {
            "cache_size", "page_size", "page_count", "max_page_count", 
            "freelist_count", "journal_mode", "synchronous", "temp_store"
        };
        
        for (String pragma : pragmas) {
            try (PreparedStatement stmt = connection.prepareStatement("PRAGMA " + pragma);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    pragmaStats.put(pragma, rs.getString(1));
                }
            } catch (SQLException e) {
                // Skip this pragma if we can't get it
                logger.debug(EnhancedLogger.PERFORMANCE, 
                        "Could not get pragma " + pragma + ": " + e.getMessage());
            }
        }
        
        metrics.put("pragma", pragmaStats);
    }
    
    /**
     * Collect H2 database metrics
     * 
     * @param metrics The metrics map to populate
     */
    private void collectH2Metrics(Map<String, Object> metrics) {
        try (Connection connection = connectionManager.getConnection()) {
            // Basic database info
            collectH2BasicInfo(connection, metrics);
            
            // Table metrics
            collectH2TableMetrics(connection, metrics);
            
            // Session metrics
            collectH2SessionMetrics(connection, metrics);
        } catch (SQLException e) {
            logger.error(EnhancedLogger.PERFORMANCE, 
                    "Error collecting H2 metrics: " + e.getMessage());
        }
    }
    
    /**
     * Collect H2 basic information
     * 
     * @param connection Database connection
     * @param metrics The metrics map to populate
     * @throws SQLException If a database error occurs
     */
    private void collectH2BasicInfo(Connection connection, Map<String, Object> metrics) 
            throws SQLException {
        Map<String, Object> basicInfo = new HashMap<>();
        
        // Get H2 version
        try (PreparedStatement stmt = connection.prepareStatement("SELECT H2VERSION()");
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                basicInfo.put("version", rs.getString(1));
            }
        }
        
        // Get database path
        try (PreparedStatement stmt = connection.prepareStatement("CALL DATABASE_PATH()");
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                basicInfo.put("path", rs.getString(1));
            }
        }
        
        metrics.put("basic", basicInfo);
    }
    
    /**
     * Collect H2 table metrics
     * 
     * @param connection Database connection
     * @param metrics The metrics map to populate
     * @throws SQLException If a database error occurs
     */
    private void collectH2TableMetrics(Connection connection, Map<String, Object> metrics) 
            throws SQLException {
        List<Map<String, Object>> tableMetrics = new ArrayList<>();
        
        // Get table information
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT table_name, storage_type, table_class FROM information_schema.tables " +
                "WHERE table_schema = 'PUBLIC'");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String tableName = rs.getString("table_name");
                Map<String, Object> table = new HashMap<>();
                table.put("name", tableName);
                table.put("storageType", rs.getString("storage_type"));
                table.put("tableClass", rs.getString("table_class"));
                
                // Get row count for this table
                try (PreparedStatement countStmt = connection.prepareStatement(
                        "SELECT COUNT(*) FROM " + tableName)) {
                    try (ResultSet countRs = countStmt.executeQuery()) {
                        if (countRs.next()) {
                            table.put("rows", countRs.getLong(1));
                        }
                    }
                } catch (SQLException e) {
                    // Skip this table if we can't get row count
                    logger.debug(EnhancedLogger.PERFORMANCE, 
                            "Could not get row count for table " + tableName + ": " + e.getMessage());
                }
                
                tableMetrics.add(table);
            }
        }
        
        metrics.put("tables", tableMetrics);
    }
    
    /**
     * Collect H2 session metrics
     * 
     * @param connection Database connection
     * @param metrics The metrics map to populate
     * @throws SQLException If a database error occurs
     */
    private void collectH2SessionMetrics(Connection connection, Map<String, Object> metrics) 
            throws SQLException {
        List<Map<String, Object>> sessionMetrics = new ArrayList<>();
        
        // Get session information
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, user_name, statement, contains_uncommitted FROM information_schema.sessions");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Map<String, Object> session = new HashMap<>();
                session.put("id", rs.getInt("id"));
                session.put("user", rs.getString("user_name"));
                session.put("statement", rs.getString("statement"));
                session.put("containsUncommitted", rs.getBoolean("contains_uncommitted"));
                
                sessionMetrics.add(session);
            }
        }
        
        metrics.put("sessions", sessionMetrics);
    }
    
    /**
     * Log collected database metrics
     * 
     * @param metrics The metrics to log
     */
    private void logDatabaseMetrics(Map<String, Object> metrics) {
        logger.info(EnhancedLogger.PERFORMANCE, "=== Database Metrics ===");
        
        switch (databaseType) {
            case MYSQL:
                logMySQLMetrics(metrics);
                break;
            case POSTGRESQL:
                logPostgreSQLMetrics(metrics);
                break;
            case SQLITE:
                logSQLiteMetrics(metrics);
                break;
            case H2:
                logH2Metrics(metrics);
                break;
            default:
                logger.info(EnhancedLogger.PERFORMANCE, "No specific logging for " + databaseType);
                break;
        }
    }
    
    /**
     * Log MySQL database metrics
     * 
     * @param metrics The MySQL metrics to log
     */
    private void logMySQLMetrics(Map<String, Object> metrics) {
        // Log status metrics
        @SuppressWarnings("unchecked")
        Map<String, Object> status = (Map<String, Object>) metrics.get("status");
        if (status != null) {
            logger.info(EnhancedLogger.PERFORMANCE, "MySQL Status:");
            
            // Log key performance indicators
            if (status.containsKey("QueriesPerSecond")) {
                logger.info(EnhancedLogger.PERFORMANCE, "  Queries/second: " + 
                        formatDouble((Double) status.get("QueriesPerSecond")));
            }
            
            if (status.containsKey("Threads_connected")) {
                logger.info(EnhancedLogger.PERFORMANCE, "  Connected threads: " + 
                        status.get("Threads_connected"));
            }
            
            if (status.containsKey("Threads_running")) {
                logger.info(EnhancedLogger.PERFORMANCE, "  Running threads: " + 
                        status.get("Threads_running"));
            }
            
            if (status.containsKey("Slow_queries")) {
                logger.info(EnhancedLogger.PERFORMANCE, "  Slow queries: " + 
                        status.get("Slow_queries"));
            }
        }
        
        // Log process info
        @SuppressWarnings("unchecked")
        Map<String, Integer> processes = (Map<String, Integer>) metrics.get("processes");
        if (processes != null) {
            logger.info(EnhancedLogger.PERFORMANCE, "MySQL Processes:");
            logger.info(EnhancedLogger.PERFORMANCE, "  Total: " + processes.get("total"));
            logger.info(EnhancedLogger.PERFORMANCE, "  Active: " + processes.get("active"));
            logger.info(EnhancedLogger.PERFORMANCE, "  Sleeping: " + processes.get("sleeping"));
        }
        
        // Log table info (top 3 tables by size)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) metrics.get("tables");
        if (tables != null && !tables.isEmpty()) {
            logger.info(EnhancedLogger.PERFORMANCE, "Top 3 MySQL Tables by Size:");
            
            int count = Math.min(tables.size(), 3);
            for (int i = 0; i < count; i++) {
                Map<String, Object> table = tables.get(i);
                long dataSize = (Long) table.get("dataSize");
                long indexSize = (Long) table.get("indexSize");
                long rows = (Long) table.get("rows");
                
                logger.info(EnhancedLogger.PERFORMANCE, "  " + table.get("name") + 
                        ": " + formatBytes(dataSize + indexSize) + 
                        " (" + rows + " rows)");
            }
        }
    }
    
    /**
     * Log PostgreSQL database metrics
     * 
     * @param metrics The PostgreSQL metrics to log
     */
    private void logPostgreSQLMetrics(Map<String, Object> metrics) {
        // Log database metrics
        @SuppressWarnings("unchecked")
        Map<String, Object> database = (Map<String, Object>) metrics.get("database");
        if (database != null) {
            logger.info(EnhancedLogger.PERFORMANCE, "PostgreSQL Database:");
            
            if (database.containsKey("databaseSize")) {
                logger.info(EnhancedLogger.PERFORMANCE, "  Size: " + 
                        formatBytes((Long) database.get("databaseSize")));
            }
            
            if (database.containsKey("cacheHitRatio")) {
                logger.info(EnhancedLogger.PERFORMANCE, "  Cache hit ratio: " + 
                        formatPercentage((Double) database.get("cacheHitRatio")));
            }
            
            if (database.containsKey("transactionsCommitted") && 
                    database.containsKey("transactionsRolledBack")) {
                long committed = (Long) database.get("transactionsCommitted");
                long rolledBack = (Long) database.get("transactionsRolledBack");
                long total = committed + rolledBack;
                
                logger.info(EnhancedLogger.PERFORMANCE, "  Transactions: " + 
                        committed + " committed, " + rolledBack + " rolled back" +
                        (total > 0 ? " (" + formatPercentage((double) committed / total) + " success)" : ""));
            }
        }
        
        // Log query metrics
        @SuppressWarnings("unchecked")
        Map<String, Integer> queries = (Map<String, Integer>) metrics.get("queries");
        if (queries != null) {
            logger.info(EnhancedLogger.PERFORMANCE, "PostgreSQL Queries:");
            logger.info(EnhancedLogger.PERFORMANCE, "  Total sessions: " + queries.get("total"));
            logger.info(EnhancedLogger.PERFORMANCE, "  Active queries: " + queries.get("active"));
            logger.info(EnhancedLogger.PERFORMANCE, "  Idle sessions: " + queries.get("idle"));
        }
        
        // Log table info (top 3 tables by size)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) metrics.get("tables");
        if (tables != null && !tables.isEmpty()) {
            logger.info(EnhancedLogger.PERFORMANCE, "Top 3 PostgreSQL Tables by Size:");
            
            int count = Math.min(tables.size(), 3);
            for (int i = 0; i < count; i++) {
                Map<String, Object> table = tables.get(i);
                long liveRows = (Long) table.get("liveRows");
                long deadRows = (Long) table.get("deadRows");
                
                logger.info(EnhancedLogger.PERFORMANCE, "  " + table.get("name") + 
                        ": " + liveRows + " live rows, " + deadRows + " dead rows");
            }
        }
    }
    
    /**
     * Log SQLite database metrics
     * 
     * @param metrics The SQLite metrics to log
     */
    private void logSQLiteMetrics(Map<String, Object> metrics) {
        // Log basic info
        @SuppressWarnings("unchecked")
        Map<String, Object> basic = (Map<String, Object>) metrics.get("basic");
        if (basic != null) {
            logger.info(EnhancedLogger.PERFORMANCE, "SQLite Database:");
            
            if (basic.containsKey("version")) {
                logger.info(EnhancedLogger.PERFORMANCE, "  Version: " + basic.get("version"));
            }
            
            if (basic.containsKey("file")) {
                logger.info(EnhancedLogger.PERFORMANCE, "  File: " + basic.get("file"));
            }
        }
        
        // Log pragma info
        @SuppressWarnings("unchecked")
        Map<String, Object> pragma = (Map<String, Object>) metrics.get("pragma");
        if (pragma != null) {
            logger.info(EnhancedLogger.PERFORMANCE, "SQLite Configuration:");
            
            if (pragma.containsKey("page_size") && pragma.containsKey("page_count")) {
                int pageSize = parseInt(pragma.get("page_size"), 0);
                int pageCount = parseInt(pragma.get("page_count"), 0);
                long dbSize = (long) pageSize * pageCount;
                
                logger.info(EnhancedLogger.PERFORMANCE, "  Database size: " + formatBytes(dbSize));
                logger.info(EnhancedLogger.PERFORMANCE, "  Pages: " + pageCount + " x " + pageSize + " bytes");
            }
            
            if (pragma.containsKey("cache_size")) {
                logger.info(EnhancedLogger.PERFORMANCE, "  Cache size: " + pragma.get("cache_size"));
            }
            
            if (pragma.containsKey("journal_mode")) {
                logger.info(EnhancedLogger.PERFORMANCE, "  Journal mode: " + pragma.get("journal_mode"));
            }
            
            if (pragma.containsKey("synchronous")) {
                logger.info(EnhancedLogger.PERFORMANCE, "  Synchronous mode: " + pragma.get("synchronous"));
            }
        }
        
        // Log table info (top 3 tables by size)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) metrics.get("tables");
        if (tables != null && !tables.isEmpty()) {
            logger.info(EnhancedLogger.PERFORMANCE, "Top 3 SQLite Tables by Row Count:");
            
            // Sort tables by row count
            tables.sort((a, b) -> {
                long aRows = a.containsKey("rows") ? (Long) a.get("rows") : 0;
                long bRows = b.containsKey("rows") ? (Long) b.get("rows") : 0;
                return Long.compare(bRows, aRows);
            });
            
            int count = Math.min(tables.size(), 3);
            for (int i = 0; i < count; i++) {
                Map<String, Object> table = tables.get(i);
                long rows = table.containsKey("rows") ? (Long) table.get("rows") : 0;
                
                logger.info(EnhancedLogger.PERFORMANCE, "  " + table.get("name") + 
                        ": " + rows + " rows");
            }
        }
    }
    
    /**
     * Log H2 database metrics
     * 
     * @param metrics The H2 metrics to log
     */
    private void logH2Metrics(Map<String, Object> metrics) {
        // Log basic info
        @SuppressWarnings("unchecked")
        Map<String, Object> basic = (Map<String, Object>) metrics.get("basic");
        if (basic != null) {
            logger.info(EnhancedLogger.PERFORMANCE, "H2 Database:");
            
            if (basic.containsKey("version")) {
                logger.info(EnhancedLogger.PERFORMANCE, "  Version: " + basic.get("version"));
            }
            
            if (basic.containsKey("path")) {
                logger.info(EnhancedLogger.PERFORMANCE, "  Path: " + basic.get("path"));
            }
        }
        
        // Log session info
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sessions = (List<Map<String, Object>>) metrics.get("sessions");
        if (sessions != null) {
            logger.info(EnhancedLogger.PERFORMANCE, "H2 Sessions:");
            logger.info(EnhancedLogger.PERFORMANCE, "  Active sessions: " + sessions.size());
            
            int uncommittedCount = 0;
            for (Map<String, Object> session : sessions) {
                boolean uncommitted = (Boolean) session.get("containsUncommitted");
                if (uncommitted) {
                    uncommittedCount++;
                }
            }
            
            if (uncommittedCount > 0) {
                logger.info(EnhancedLogger.PERFORMANCE, "  Sessions with uncommitted data: " + uncommittedCount);
            }
        }
        
        // Log table info (top 3 tables by size)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) metrics.get("tables");
        if (tables != null && !tables.isEmpty()) {
            logger.info(EnhancedLogger.PERFORMANCE, "Top 3 H2 Tables by Row Count:");
            
            // Sort tables by row count
            tables.sort((a, b) -> {
                long aRows = a.containsKey("rows") ? (Long) a.get("rows") : 0;
                long bRows = b.containsKey("rows") ? (Long) b.get("rows") : 0;
                return Long.compare(bRows, aRows);
            });
            
            int count = Math.min(tables.size(), 3);
            for (int i = 0; i < count; i++) {
                Map<String, Object> table = tables.get(i);
                long rows = table.containsKey("rows") ? (Long) table.get("rows") : 0;
                
                logger.info(EnhancedLogger.PERFORMANCE, "  " + table.get("name") + 
                        ": " + rows + " rows (" + table.get("storageType") + ")");
            }
        }
    }
    
    /**
     * Extract a value from a string
     * 
     * @param text The text to extract from
     * @param key The key to look for
     * @param delimiter The delimiter that comes after the value
     * @return The extracted value, or null if not found
     */
    private String extractValue(String text, String key, String delimiter) {
        int keyIndex = text.indexOf(key);
        if (keyIndex == -1) {
            return null;
        }
        
        int valueStart = keyIndex + key.length();
        int valueEnd = text.indexOf(delimiter, valueStart);
        
        if (valueEnd == -1) {
            return text.substring(valueStart).trim();
        } else {
            return text.substring(valueStart, valueEnd).trim();
        }
    }
    
    /**
     * Format a byte count into a human-readable string
     * 
     * @param bytes The number of bytes
     * @return A formatted string (e.g. "1.23 MB")
     */
    private String formatBytes(long bytes) {
        // Handle negative values
        if (bytes < 0) {
            return "0 B";
        }
        
        // Define units and thresholds
        final String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        int unitIndex = 0;
        double size = bytes;
        
        // Find the appropriate unit
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        // Format with appropriate precision
        return String.format("%.2f %s", size, units[unitIndex]);
    }
    
    /**
     * Format a double value with two decimal places
     * 
     * @param value The value to format
     * @return A formatted string
     */
    private String formatDouble(double value) {
        return String.format("%.2f", value);
    }
    
    /**
     * Format a percentage value
     * 
     * @param value The value to format (0.0 to 1.0)
     * @return A formatted percentage string
     */
    private String formatPercentage(double value) {
        return String.format("%.2f%%", value * 100);
    }
    
    /**
     * Parse a long value safely
     * 
     * @param value The value to parse
     * @param defaultValue The default value to return if parsing fails
     * @return The parsed long value
     */
    private long parseLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Parse an integer value safely
     * 
     * @param value The value to parse
     * @param defaultValue The default value to return if parsing fails
     * @return The parsed integer value
     */
    private int parseInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Get the last collected metrics
     *
     * @return The last collected metrics
     */
    public Map<String, Object> getLastMetrics() {
        return lastMetrics;
    }
}