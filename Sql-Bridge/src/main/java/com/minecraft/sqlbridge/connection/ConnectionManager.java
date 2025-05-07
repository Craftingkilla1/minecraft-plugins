package com.minecraft.sqlbridge.connection;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.api.DatabaseType;
import com.minecraft.sqlbridge.dialect.Dialect;
import com.minecraft.sqlbridge.dialect.H2Dialect;
import com.minecraft.sqlbridge.dialect.MySQLDialect;
import com.minecraft.sqlbridge.dialect.PostgreSQLDialect;
import com.minecraft.sqlbridge.dialect.SQLiteDialect;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manages database connections and provides access to the connection pool.
 */
public class ConnectionManager {

    private final Plugin plugin;
    private final DatabaseType type;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private ConnectionPool connectionPool;
    private Dialect dialect;

    /**
     * Create a new connection manager
     *
     * @param plugin The plugin instance
     * @param type The database type
     * @param host The database host
     * @param port The database port
     * @param database The database name
     * @param username The database username
     * @param password The database password
     */
    public ConnectionManager(Plugin plugin, DatabaseType type, String host, int port, String database, String username, String password) {
        this.plugin = plugin;
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    /**
     * Initialize the connection manager and create the connection pool
     *
     * @return True if initialization was successful, false otherwise
     */
    public boolean initialize() {
        try {
            // Set up dialect based on database type
            setupDialect();
            
            // Create connection factory
            ConnectionFactory factory = createConnectionFactory();
            
            // Configure connection pool
            // Pass the plugin instance as the third parameter
            connectionPool = new ConnectionPool(factory, getPoolSize(), (SqlBridgePlugin) plugin);
            
            // Test connection
            try (Connection connection = connectionPool.getConnection()) {
                if (connection == null || !connection.isValid(5)) {
                    LogUtil.severe("Could not establish a valid database connection.");
                    return false;
                }
                return true;
            }
        } catch (SQLException e) {
            LogUtil.severe("Failed to initialize database connection: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reinitialize the connection manager by closing existing connections
     * and creating a new connection pool
     * 
     * @return True if reinitialization was successful, false otherwise
     */
    public boolean reinitialize() {
        LogUtil.info("Reinitializing database connection...");
        
        // Close existing connections
        if (connectionPool != null) {
            connectionPool.close();
        }
        
        // Initialize a new connection pool
        return initialize();
    }

    /**
     * Get a connection from the pool
     *
     * @return A database connection
     * @throws SQLException If a connection cannot be obtained
     */
    public Connection getConnection() throws SQLException {
        if (connectionPool == null) {
            throw new SQLException("Connection pool is not initialized.");
        }
        return connectionPool.getConnection();
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
        if (connectionPool == null) {
            throw new SQLException("Connection pool is not initialized.");
        }
        return connectionPool.getConnection(timeout, unit);
    }

    /**
     * Close all connections in the pool
     */
    public void closeAllConnections() {
        if (connectionPool != null) {
            connectionPool.close();
        }
    }
    
    /**
     * Close idle connections in the pool
     *
     * @return Number of connections closed
     */
    public int closeIdleConnections() {
        if (connectionPool != null) {
            return connectionPool.closeIdleConnections();
        }
        return 0;
    }

    /**
     * Set up the SQL dialect based on the database type
     */
    private void setupDialect() {
        switch (type) {
            case MYSQL:
                dialect = new MySQLDialect();
                break;
            case POSTGRESQL:
                dialect = new PostgreSQLDialect();
                break;
            case H2:
                dialect = new H2Dialect();
                break;
            case SQLITE:
            default:
                dialect = new SQLiteDialect();
                break;
        }
    }

    /**
     * Create a connection factory based on the database type
     *
     * @return A connection factory
     * @throws SQLException If the connection factory cannot be created
     */
    private ConnectionFactory createConnectionFactory() throws SQLException {
        switch (type) {
            case MYSQL:
                return new ConnectionFactory.Builder()
                        .withUrl("jdbc:mysql://" + host + ":" + port + "/" + database)
                        .withDriver("com.mysql.jdbc.Driver")
                        .withUsername(username)
                        .withPassword(password)
                        .withProperties("useSSL=false&autoReconnect=true&useUnicode=true&characterEncoding=utf8")
                        .build();
                
            case POSTGRESQL:
                return new ConnectionFactory.Builder()
                        .withUrl("jdbc:postgresql://" + host + ":" + port + "/" + database)
                        .withDriver("org.postgresql.Driver")
                        .withUsername(username)
                        .withPassword(password)
                        .build();
                
            case H2:
                File dataFolder = plugin.getDataFolder();
                return new ConnectionFactory.Builder()
                        .withUrl("jdbc:h2:" + dataFolder.getAbsolutePath() + File.separator + database)
                        .withDriver("org.h2.Driver")
                        .build();
                
            case SQLITE:
            default:
                // Ensure the data folder exists
                File dataDir = plugin.getDataFolder();
                if (!dataDir.exists()) {
                    dataDir.mkdirs();
                }
                
                // Create the connection factory for SQLite
                String dbFile = dataDir.getAbsolutePath() + File.separator + database + ".db";
                return new ConnectionFactory.Builder()
                        .withUrl("jdbc:sqlite:" + dbFile)
                        .withDriver("org.sqlite.JDBC")
                        .build();
        }
    }

    /**
     * Get the dialect for the current database type
     *
     * @return The SQL dialect
     */
    public Dialect getDialect() {
        return dialect;
    }

    /**
     * Get the database type
     *
     * @return The database type
     */
    public DatabaseType getType() {
        return type;
    }

    /**
     * Get the connection pool size based on the database type
     *
     * @return The connection pool size
     */
    private int getPoolSize() {
        // SQLite doesn't support concurrent connections well, so use a small pool
        if (type == DatabaseType.SQLITE) {
            return 1;
        }
        
        // For other database types, base the pool size on the server's max players
        int maxPlayers = plugin.getServer().getMaxPlayers();
        return Math.max(5, maxPlayers / 10);
    }
    
    /**
     * Get pool statistics
     *
     * @return Map of pool statistics
     */
    public Map<String, Object> getPoolStatistics() {
        Map<String, Object> stats = new HashMap<String, Object>();
        
        if (connectionPool != null) {
            stats.putAll(connectionPool.getStatistics());
        }
        
        // Add basic stats
        stats.put("databaseType", type.name());
        stats.put("host", host);
        stats.put("database", database);
        
        return stats;
    }
}