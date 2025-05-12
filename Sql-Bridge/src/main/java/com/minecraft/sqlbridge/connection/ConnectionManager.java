// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/connection/ConnectionManager.java
package com.minecraft.sqlbridge.connection;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.config.SqlBridgeConfig;
import com.minecraft.sqlbridge.dialect.Dialect;
import com.minecraft.sqlbridge.dialect.MySQLDialect;
import com.minecraft.sqlbridge.error.ConnectionException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Manages database connections and connection pools.
 */
public class ConnectionManager {

    private final SqlBridgePlugin plugin;
    private final SqlBridgeConfig config;
    private final Map<String, HikariDataSource> dataSources;
    private final Map<String, Dialect> dialects;
    private final ScheduledExecutorService connectionMonitor;
    private HikariDataSource mainDataSource;
    private HikariDataSource sharedDataSource;

    /**
     * Constructor for ConnectionManager.
     *
     * @param plugin The SQL-Bridge plugin instance
     * @param config The plugin configuration
     */
    public ConnectionManager(SqlBridgePlugin plugin, SqlBridgeConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.dataSources = new ConcurrentHashMap<>();
        this.dialects = new ConcurrentHashMap<>();
        
        // Initialize connection monitoring
        this.connectionMonitor = Executors.newSingleThreadScheduledExecutor(
                r -> {
                    Thread thread = new Thread(r, "SqlBridge-ConnectionMonitor");
                    thread.setDaemon(true);
                    return thread;
                });
        
        // Initialize main connection
        initializeMainConnection();
        
        // Initialize shared connection for BungeeSupport if enabled
        if (config.isBungeeEnabled() && config.isSharedDatabaseEnabled()) {
            initializeSharedConnection();
        }
        
        // Start connection monitoring
        startConnectionMonitoring();
    }

    /**
     * Initialize the main database connection.
     */
    private void initializeMainConnection() {
        try {
            mainDataSource = createMySQLDataSource("main", 
                    config.getMySQLHost(), 
                    config.getMySQLPort(), 
                    config.getMySQLDatabase(), 
                    config.getMySQLUsername(), 
                    config.getMySQLPassword());
            
            dialects.put("main", new MySQLDialect());
            dataSources.put("main", mainDataSource);
            LogUtil.info("Main connection initialized successfully using MySQL");
            
            // Test connection
            testConnection(mainDataSource);
        } catch (Exception e) {
            LogUtil.severe("Failed to initialize main connection: " + e.getMessage());
            plugin.getErrorHandler().handleConnectionError(e);
            throw new ConnectionException("Could not initialize main connection", e);
        }
    }

    /**
     * Initialize the shared database connection for BungeeSupport.
     */
    private void initializeSharedConnection() {
        try {
            // For shared databases, we always use MySQL
            sharedDataSource = createMySQLDataSource("shared", 
                    config.getMySQLHost(), 
                    config.getMySQLPort(), 
                    config.getMySQLDatabase() + "_shared", 
                    config.getMySQLUsername(), 
                    config.getMySQLPassword());
            
            dialects.put("shared", new MySQLDialect());
            dataSources.put("shared", sharedDataSource);
            LogUtil.info("Shared connection initialized successfully using MySQL");
            
            // Test connection
            testConnection(sharedDataSource);
        } catch (Exception e) {
            LogUtil.severe("Failed to initialize shared connection: " + e.getMessage());
            plugin.getErrorHandler().handleConnectionError(e);
        }
    }

    /**
     * Create a MySQL data source with HikariCP.
     *
     * @param name The name of the data source
     * @param host The MySQL host
     * @param port The MySQL port
     * @param database The MySQL database name
     * @param username The MySQL username
     * @param password The MySQL password
     * @return The HikariDataSource
     */
    private HikariDataSource createMySQLDataSource(String name, String host, int port, 
                                                String database, String username, String password) {
        HikariConfig hikariConfig = new HikariConfig();
        
        hikariConfig.setPoolName("SqlBridge-" + name);
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        
        // Connection pool settings
        hikariConfig.setMaximumPoolSize(config.getMySQLMaxPoolSize());
        hikariConfig.setMinimumIdle(config.getMySQLMinIdle());
        hikariConfig.setMaxLifetime(config.getMySQLMaxLifetime());
        hikariConfig.setConnectionTimeout(config.getMySQLConnectionTimeout());
        hikariConfig.setIdleTimeout(config.getMySQLIdleTimeout());
        hikariConfig.setKeepaliveTime(config.getMySQLKeepaliveTime());
        hikariConfig.setLeakDetectionThreshold(config.getMySQLLeakDetectionThreshold());
        
        // Additional properties
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
        
        // Create database if it doesn't exist
        if (config.isMySQLAutoCreateDatabase()) {
            createMySQLDatabaseIfNotExists(host, port, database, username, password);
        }
        
        return new HikariDataSource(hikariConfig);
    }

    /**
     * Create a MySQL database if it doesn't exist.
     *
     * @param host The MySQL host
     * @param port The MySQL port
     * @param database The MySQL database name
     * @param username The MySQL username
     * @param password The MySQL password
     */
    private void createMySQLDatabaseIfNotExists(String host, int port, String database, 
                                             String username, String password) {
        HikariConfig tempConfig = new HikariConfig();
        tempConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        tempConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port +
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        tempConfig.setUsername(username);
        tempConfig.setPassword(password);
        tempConfig.setMaximumPoolSize(1);
        tempConfig.setConnectionTimeout(5000);
        
        try (HikariDataSource tempDataSource = new HikariDataSource(tempConfig);
             Connection conn = tempDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database + "` " +
                              "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            
            LogUtil.info("Created database '" + database + "' if it didn't exist");
        } catch (SQLException e) {
            LogUtil.severe("Failed to create database: " + e.getMessage());
            throw new ConnectionException("Could not create database: " + database, e);
        }
    }

    /**
     * Get the main database connection pool.
     *
     * @return The DataSource for the main database
     */
    public DataSource getMainConnection() {
        return mainDataSource;
    }

    /**
     * Get the shared database connection pool.
     *
     * @return The DataSource for the shared database
     */
    public DataSource getSharedConnection() {
        if (sharedDataSource == null) {
            throw new ConnectionException("Shared database connection is not enabled");
        }
        return sharedDataSource;
    }

    /**
     * Get a named database connection pool.
     *
     * @param name The name of the connection
     * @return The DataSource for the named database
     */
    public DataSource getConnection(String name) {
        if (dataSources.containsKey(name)) {
            return dataSources.get(name);
        }
        
        // Create new connection if it doesn't exist
        synchronized (this) {
            if (dataSources.containsKey(name)) {
                return dataSources.get(name);
            }
            
            LogUtil.info("Creating new connection: " + name);
            
            HikariDataSource dataSource = createMySQLDataSource(name, 
                    config.getMySQLHost(), 
                    config.getMySQLPort(), 
                    config.getMySQLDatabase() + "_" + name.toLowerCase(), 
                    config.getMySQLUsername(), 
                    config.getMySQLPassword());
            
            dataSources.put(name, dataSource);
            dialects.put(name, new MySQLDialect());
            return dataSource;
        }
    }

    /**
     * Check if a named connection exists.
     *
     * @param name The name of the connection
     * @return true if the connection exists, false otherwise
     */
    public boolean connectionExists(String name) {
        return dataSources.containsKey(name);
    }

    /**
     * Get the dialect for a named connection.
     *
     * @param name The name of the connection
     * @return The Dialect for the named connection
     */
    public Dialect getDialect(String name) {
        return dialects.getOrDefault(name, dialects.get("main"));
    }

    /**
     * Start monitoring connections for health checks.
     */
    private void startConnectionMonitoring() {
        connectionMonitor.scheduleAtFixedRate(() -> {
            try {
                checkConnections();
            } catch (Exception e) {
                LogUtil.warning("Error in connection monitoring: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Check all connections for validity.
     */
    private void checkConnections() {
        for (Map.Entry<String, HikariDataSource> entry : dataSources.entrySet()) {
            String name = entry.getKey();
            HikariDataSource dataSource = entry.getValue();
            
            try (Connection conn = dataSource.getConnection()) {
                if (!conn.isValid(5)) {
                    LogUtil.warning("Connection '" + name + "' is invalid. Attempting to recover...");
                    // The HikariCP pool will automatically replace the invalid connection
                }
            } catch (SQLException e) {
                LogUtil.warning("Error checking connection '" + name + "': " + e.getMessage());
                plugin.getErrorHandler().handleConnectionCheckError(name, e);
            }
        }
    }

    /**
     * Test a data source to ensure it's working properly.
     *
     * @param dataSource The data source to test
     * @throws SQLException If the connection test fails
     */
    private void testConnection(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(5)) {
                throw new SQLException("Connection test failed: connection is not valid");
            }
        }
    }

    /**
     * Get the number of active database connections.
     *
     * @return The number of active connections
     */
    public int getActiveConnectionCount() {
        return dataSources.size();
    }

    /**
     * Get statistics about the connection pools.
     *
     * @return A map of statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        for (Map.Entry<String, HikariDataSource> entry : dataSources.entrySet()) {
            String name = entry.getKey();
            HikariDataSource dataSource = entry.getValue();
            
            stats.put(name + ".activeConnections", dataSource.getHikariPoolMXBean().getActiveConnections());
            stats.put(name + ".idleConnections", dataSource.getHikariPoolMXBean().getIdleConnections());
            stats.put(name + ".totalConnections", dataSource.getHikariPoolMXBean().getTotalConnections());
            stats.put(name + ".threadsAwaitingConnection", dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        }
        
        stats.put("connectionPoolCount", dataSources.size());
        
        return stats;
    }

    /**
     * Close all connections and clean up resources.
     * This should be called when the plugin is disabled.
     */
    public void close() {
        // Shutdown connection monitor
        connectionMonitor.shutdown();
        try {
            connectionMonitor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Close all data sources
        for (Map.Entry<String, HikariDataSource> entry : dataSources.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                LogUtil.warning("Error closing connection '" + entry.getKey() + "': " + e.getMessage());
            }
        }
        
        dataSources.clear();
        dialects.clear();
        
        LogUtil.info("All database connections closed.");
    }
}