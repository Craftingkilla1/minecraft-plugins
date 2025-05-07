package com.minecraft.sqlbridge.connection;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple connection pool implementation that manages a collection of database connections.
 */
public class ConnectionPool {

    private final ConnectionFactory connectionFactory;
    private final BlockingQueue<MonitoredConnection> connections;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final int maxConnections;
    private volatile boolean closed = false;
    private final SqlBridgePlugin plugin;
    private final long connectionTimeout;
    private final long connectionMaxAge;
    private final long connectionIdleTimeout;

    /**
     * Create a new connection pool
     *
     * @param connectionFactory The factory to create new connections
     * @param maxConnections The maximum number of connections in the pool
     * @param plugin The SqlBridge plugin instance for configuration and stats
     */
    public ConnectionPool(ConnectionFactory connectionFactory, int maxConnections, SqlBridgePlugin plugin) {
        this.connectionFactory = connectionFactory;
        this.maxConnections = maxConnections;
        this.connections = new ArrayBlockingQueue<>(maxConnections);
        this.plugin = plugin;
        
        // Read configuration
        this.connectionTimeout = plugin.getConfig().getLong("connection.timeout", 30000);
        this.connectionMaxAge = plugin.getConfig().getLong("connection.max-age", 3600000); // 1 hour
        this.connectionIdleTimeout = plugin.getConfig().getLong("connection.idle-timeout", 600000); // 10 minutes
        
        // Pre-populate the pool with a few connections
        int initialConnections = Math.min(3, maxConnections);
        for (int i = 0; i < initialConnections; i++) {
            try {
                connections.offer(createConnection());
            } catch (SQLException e) {
                LogUtil.warning("Failed to create initial database connection: " + e.getMessage());
            }
        }
        
        LogUtil.info("Initialized connection pool with " + initialConnections + " connections (max: " + maxConnections + ")");
    }

    /**
     * Get a connection from the pool
     *
     * @return A database connection
     * @throws SQLException If a connection cannot be obtained
     */
    public Connection getConnection() throws SQLException {
        try {
            return getConnection(30, TimeUnit.SECONDS);
        } catch (SQLException e) {
            LogUtil.severe("Failed to get database connection: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get a connection from the pool with timeout
     *
     * @param timeout The timeout value
     * @param unit The timeout unit
     * @return A database connection
     * @throws SQLException If a connection cannot be obtained
     */
    public Connection getConnection(long timeout, TimeUnit unit) throws SQLException {
        if (closed) {
            throw new SQLException("Connection pool is closed");
        }
        
        try {
            // Try to get a connection from the pool
            MonitoredConnection connection = connections.poll();
            
            // If no connection is available, create a new one if possible
            if (connection == null) {
                if (activeConnections.get() < maxConnections) {
                    connection = createConnection();
                } else {
                    // Wait for a connection to become available
                    connection = connections.poll(timeout, unit);
                    if (connection == null) {
                        throw new SQLException("Timeout waiting for database connection");
                    }
                }
            }
            
            // Validate the connection
            if (!connection.isValid()) {
                LogUtil.warning("Replacing invalid database connection");
                connection.reallyClose();
                connection = createConnection();
            }
            
            // Check if connection is too old
            long age = System.currentTimeMillis() - connection.getCreationTime();
            if (age > connectionMaxAge) {
                LogUtil.info("Replacing connection that exceeded max age (" + (age / 1000) + " seconds)");
                connection.reallyClose();
                connection = createConnection();
            }
            
            return connection;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for database connection", e);
        }
    }
    
    /**
     * Create a new monitored connection
     *
     * @return A new monitored connection
     * @throws SQLException If the connection cannot be created
     */
    private MonitoredConnection createConnection() throws SQLException {
        activeConnections.incrementAndGet();
        try {
            return new MonitoredConnection(connectionFactory.createConnection(), this, 
                    plugin.getQueryStatistics());
        } catch (SQLException e) {
            activeConnections.decrementAndGet();
            throw e;
        }
    }
    
    /**
     * Close idle connections in the pool
     *
     * @return Number of connections closed
     */
    public int closeIdleConnections() {
        List<MonitoredConnection> allConnections = new ArrayList<>();
        connections.drainTo(allConnections);
        
        int closed = 0;
        long now = System.currentTimeMillis();
        List<MonitoredConnection> validConnections = new ArrayList<>();
        
        // Check each connection
        for (MonitoredConnection conn : allConnections) {
            if (!conn.isValid()) {
                // Invalid connection, close it
                try {
                    conn.reallyClose();
                    activeConnections.decrementAndGet();
                    closed++;
                } catch (SQLException e) {
                    // Ignore
                }
                continue;
            }
            
            // Check if connection is idle for too long
            if (now - conn.getCreationTime() > connectionIdleTimeout) {
                try {
                    conn.reallyClose();
                    activeConnections.decrementAndGet();
                    closed++;
                } catch (SQLException e) {
                    // Ignore
                }
                continue;
            }
            
            // Valid connection, keep it
            validConnections.add(conn);
        }
        
        // Return valid connections to the pool
        connections.addAll(validConnections);
        
        return closed;
    }

    /**
     * Return a connection to the pool
     *
     * @param connection The connection to return
     */
    void releaseConnection(MonitoredConnection connection) {
        if (closed) {
            try {
                connection.reallyClose();
            } catch (SQLException e) {
                LogUtil.warning("Error closing connection: " + e.getMessage());
            }
            return;
        }
        
        // Return the connection to the pool if it's valid
        if (connection.isValid()) {
            // Check if we have too many connections
            if (connections.size() >= maxConnections) {
                try {
                    connection.reallyClose();
                    activeConnections.decrementAndGet();
                } catch (SQLException e) {
                    LogUtil.warning("Error closing excess connection: " + e.getMessage());
                }
                return;
            }
            
            // Return to pool
            boolean returned = connections.offer(connection);
            if (!returned) {
                try {
                    connection.reallyClose();
                    activeConnections.decrementAndGet();
                } catch (SQLException e) {
                    LogUtil.warning("Error closing excess connection: " + e.getMessage());
                }
            }
        } else {
            try {
                connection.reallyClose();
                activeConnections.decrementAndGet();
            } catch (SQLException e) {
                LogUtil.warning("Error closing invalid connection: " + e.getMessage());
            }
        }
    }

    /**
     * Close all connections in the pool
     */
    public void close() {
        closed = true;
        
        List<MonitoredConnection> connectionList = new ArrayList<>();
        connections.drainTo(connectionList);
        
        for (MonitoredConnection connection : connectionList) {
            try {
                connection.reallyClose();
            } catch (SQLException e) {
                LogUtil.warning("Error closing connection during pool shutdown: " + e.getMessage());
            }
        }
        
        LogUtil.info("Database connection pool closed");
    }
    
    /**
     * Get current connection pool statistics
     *
     * @return Map of statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pooledConnections", connections.size());
        stats.put("maxConnections", maxConnections);
        stats.put("activeConnections", activeConnections.get());
        stats.put("available", connections.size());
        stats.put("inUse", activeConnections.get() - connections.size());
        stats.put("connectionTimeout", connectionTimeout);
        stats.put("connectionMaxAge", connectionMaxAge);
        stats.put("connectionIdleTimeout", connectionIdleTimeout);
        return stats;
    }
}