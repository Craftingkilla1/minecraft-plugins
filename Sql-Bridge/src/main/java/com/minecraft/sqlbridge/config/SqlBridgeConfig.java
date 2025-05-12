// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/config/SqlBridgeConfig.java
package com.minecraft.sqlbridge.config;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration manager for the SQL-Bridge plugin.
 */
public class SqlBridgeConfig {
    
    private final SqlBridgePlugin plugin;
    private FileConfiguration config;
    
    // Debug mode
    private boolean debugMode;
    
    // Database configuration
    private String databaseType;
    
    // MySQL configuration
    private String mySQLHost;
    private int mySQLPort;
    private String mySQLDatabase;
    private String mySQLUsername;
    private String mySQLPassword;
    private boolean mySQLAutoCreateDatabase;
    
    // MySQL connection pool settings
    private int mySQLMaxPoolSize;
    private int mySQLMinIdle;
    private long mySQLMaxLifetime;
    private long mySQLConnectionTimeout;
    private long mySQLIdleTimeout;
    private long mySQLKeepaliveTime;
    private long mySQLLeakDetectionThreshold;
    
    // SQLite configuration
    private String sqliteFile;
    private boolean sqliteWalMode;
    
    // BungeeSupport configuration
    private boolean bungeeEnabled;
    private boolean sharedDatabaseEnabled;
    
    // Security configuration
    private boolean sqlInjectionDetectionEnabled;
    private boolean logDangerousOperations;
    private int maxQueryLength;
    
    // Performance monitoring
    private boolean monitoringEnabled;
    private long slowQueryThreshold;
    private boolean collectMetrics;
    private int metricsInterval;
    
    // Migration system
    private boolean autoMigrateEnabled;
    private boolean rollbackFailedMigrationsEnabled;
    
    // Batch settings
    private int defaultBatchSize;
    private int maxBatchSize;
    
    // Database thread pool size
    private int databaseThreadPoolSize;
    
    /**
     * Constructor for SqlBridgeConfig.
     *
     * @param plugin The SQL-Bridge plugin instance
     */
    public SqlBridgeConfig(SqlBridgePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        
        // Load configuration
        loadConfig();
    }
    
    /**
     * Load the configuration from the config file.
     */
    public void loadConfig() {
        // Debug mode
        debugMode = config.getBoolean("debug", false);
        
        // Database configuration
        databaseType = config.getString("database.type", "mysql").toLowerCase();
        
        // MySQL configuration
        mySQLHost = config.getString("database.mysql.host", "localhost");
        mySQLPort = config.getInt("database.mysql.port", 3306);
        mySQLDatabase = config.getString("database.mysql.database", "minecraft");
        mySQLUsername = config.getString("database.mysql.username", "root");
        mySQLPassword = config.getString("database.mysql.password", "password");
        mySQLAutoCreateDatabase = config.getBoolean("database.mysql.auto-create-database", true);
        
        // MySQL connection pool settings
        ConfigurationSection mySQLPoolSection = config.getConfigurationSection("database.mysql.pool");
        if (mySQLPoolSection != null) {
            mySQLMaxPoolSize = mySQLPoolSection.getInt("maximum-pool-size", 10);
            mySQLMinIdle = mySQLPoolSection.getInt("minimum-idle", 5);
            mySQLMaxLifetime = mySQLPoolSection.getLong("maximum-lifetime", 1800000);
            mySQLConnectionTimeout = mySQLPoolSection.getLong("connection-timeout", 30000);
            mySQLIdleTimeout = mySQLPoolSection.getLong("idle-timeout", 600000);
            mySQLKeepaliveTime = mySQLPoolSection.getLong("keepalive-time", 30000);
            mySQLLeakDetectionThreshold = mySQLPoolSection.getLong("leak-detection-threshold", 30000);
        } else {
            // Default values
            mySQLMaxPoolSize = 10;
            mySQLMinIdle = 5;
            mySQLMaxLifetime = 1800000;
            mySQLConnectionTimeout = 30000;
            mySQLIdleTimeout = 600000;
            mySQLKeepaliveTime = 30000;
            mySQLLeakDetectionThreshold = 30000;
        }
        
        // SQLite configuration
        ConfigurationSection sqliteSection = config.getConfigurationSection("database.sqlite");
        if (sqliteSection != null) {
            sqliteFile = sqliteSection.getString("file", "database.db");
            sqliteWalMode = sqliteSection.getBoolean("wal-mode", true);
        } else {
            // Default values
            sqliteFile = "database.db";
            sqliteWalMode = true;
        }
        
        // BungeeSupport configuration
        ConfigurationSection bungeeSection = config.getConfigurationSection("bungee");
        if (bungeeSection != null) {
            bungeeEnabled = bungeeSection.getBoolean("enabled", false);
            sharedDatabaseEnabled = bungeeSection.getBoolean("shared-database", true);
        } else {
            // Default values
            bungeeEnabled = false;
            sharedDatabaseEnabled = true;
        }
        
        // Security configuration
        ConfigurationSection securitySection = config.getConfigurationSection("security");
        if (securitySection != null) {
            sqlInjectionDetectionEnabled = securitySection.getBoolean("sql-injection-detection", true);
            logDangerousOperations = securitySection.getBoolean("log-dangerous-operations", true);
            maxQueryLength = securitySection.getInt("max-query-length", 10000);
        } else {
            // Default values
            sqlInjectionDetectionEnabled = true;
            logDangerousOperations = true;
            maxQueryLength = 10000;
        }
        
        // Performance monitoring
        ConfigurationSection monitoringSection = config.getConfigurationSection("monitoring");
        if (monitoringSection != null) {
            monitoringEnabled = monitoringSection.getBoolean("enabled", true);
            slowQueryThreshold = monitoringSection.getLong("slow-query-threshold", 1000);
            collectMetrics = monitoringSection.getBoolean("collect-metrics", true);
            metricsInterval = monitoringSection.getInt("metrics-interval", 300);
        } else {
            // Default values
            monitoringEnabled = true;
            slowQueryThreshold = 1000;
            collectMetrics = true;
            metricsInterval = 300;
        }
        
        // Migration system
        ConfigurationSection migrationsSection = config.getConfigurationSection("migrations");
        if (migrationsSection != null) {
            autoMigrateEnabled = migrationsSection.getBoolean("auto-migrate", true);
            rollbackFailedMigrationsEnabled = migrationsSection.getBoolean("rollback-failed-migrations", true);
        } else {
            // Default values
            autoMigrateEnabled = true;
            rollbackFailedMigrationsEnabled = true;
        }
        
        // Batch settings
        ConfigurationSection batchSection = config.getConfigurationSection("batch");
        if (batchSection != null) {
            defaultBatchSize = batchSection.getInt("default-size", 100);
            maxBatchSize = batchSection.getInt("max-size", 1000);
        } else {
            // Default values
            defaultBatchSize = 100;
            maxBatchSize = 1000;
        }
        
        // Database thread pool size
        databaseThreadPoolSize = config.getInt("database-thread-pool-size", 4);
        
        // Log configuration info
        logConfigInfo();
    }
    
    /**
     * Reload the configuration.
     */
    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadConfig();
    }
    
    /**
     * Log configuration information.
     */
    private void logConfigInfo() {
        LogUtil.info("SQL-Bridge configuration loaded:");
        LogUtil.info("  Database Type: " + databaseType);
        
        if (databaseType.equals("mysql")) {
            LogUtil.info("  MySQL Host: " + mySQLHost + ":" + mySQLPort);
            LogUtil.info("  MySQL Database: " + mySQLDatabase);
            LogUtil.info("  MySQL Auto-Create Database: " + mySQLAutoCreateDatabase);
            LogUtil.info("  MySQL Connection Pool Size: " + mySQLMaxPoolSize);
        } else if (databaseType.equals("sqlite")) {
            LogUtil.info("  SQLite File: " + sqliteFile);
            LogUtil.info("  SQLite WAL Mode: " + sqliteWalMode);
        }
        
        LogUtil.info("  BungeeSupport Enabled: " + bungeeEnabled);
        if (bungeeEnabled) {
            LogUtil.info("  Shared Database Enabled: " + sharedDatabaseEnabled);
        }
        
        LogUtil.info("  Automatic Migration: " + autoMigrateEnabled);
        LogUtil.info("  Performance Monitoring: " + monitoringEnabled);
    }
    
    /**
     * Get whether debug mode is enabled.
     *
     * @return true if debug mode is enabled, false otherwise
     */
    public boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * Get the database type.
     *
     * @return The database type
     */
    public String getDatabaseType() {
        return databaseType;
    }
    
    /**
     * Get the MySQL host.
     *
     * @return The MySQL host
     */
    public String getMySQLHost() {
        return mySQLHost;
    }
    
    /**
     * Get the MySQL port.
     *
     * @return The MySQL port
     */
    public int getMySQLPort() {
        return mySQLPort;
    }
    
    /**
     * Get the MySQL database name.
     *
     * @return The MySQL database name
     */
    public String getMySQLDatabase() {
        return mySQLDatabase;
    }
    
    /**
     * Get the MySQL username.
     *
     * @return The MySQL username
     */
    public String getMySQLUsername() {
        return mySQLUsername;
    }
    
    /**
     * Get the MySQL password.
     *
     * @return The MySQL password
     */
    public String getMySQLPassword() {
        return mySQLPassword;
    }
    
    /**
     * Get whether to automatically create the MySQL database if it doesn't exist.
     *
     * @return true if auto-create is enabled, false otherwise
     */
    public boolean isMySQLAutoCreateDatabase() {
        return mySQLAutoCreateDatabase;
    }
    
    /**
     * Get the maximum size of the MySQL connection pool.
     *
     * @return The maximum pool size
     */
    public int getMySQLMaxPoolSize() {
        return mySQLMaxPoolSize;
    }
    
    /**
     * Get the minimum number of idle connections in the MySQL connection pool.
     *
     * @return The minimum idle connections
     */
    public int getMySQLMinIdle() {
        return mySQLMinIdle;
    }
    
    /**
     * Get the maximum lifetime of a MySQL connection in milliseconds.
     *
     * @return The maximum lifetime
     */
    public long getMySQLMaxLifetime() {
        return mySQLMaxLifetime;
    }
    
    /**
     * Get the MySQL connection timeout in milliseconds.
     *
     * @return The connection timeout
     */
    public long getMySQLConnectionTimeout() {
        return mySQLConnectionTimeout;
    }
    
    /**
     * Get the MySQL idle timeout in milliseconds.
     *
     * @return The idle timeout
     */
    public long getMySQLIdleTimeout() {
        return mySQLIdleTimeout;
    }
    
    /**
     * Get the MySQL keepalive time in milliseconds.
     *
     * @return The keepalive time
     */
    public long getMySQLKeepaliveTime() {
        return mySQLKeepaliveTime;
    }
    
    /**
     * Get the MySQL leak detection threshold in milliseconds.
     *
     * @return The leak detection threshold
     */
    public long getMySQLLeakDetectionThreshold() {
        return mySQLLeakDetectionThreshold;
    }
    
    /**
     * Get the SQLite database file.
     *
     * @return The SQLite database file
     */
    public String getSQLiteFile() {
        return sqliteFile;
    }
    
    /**
     * Get whether SQLite WAL mode is enabled.
     *
     * @return true if WAL mode is enabled, false otherwise
     */
    public boolean isSQLiteWalMode() {
        return sqliteWalMode;
    }
    
    /**
     * Get whether BungeeSupport is enabled.
     *
     * @return true if BungeeSupport is enabled, false otherwise
     */
    public boolean isBungeeEnabled() {
        return bungeeEnabled;
    }
    
    /**
     * Get whether shared database is enabled for BungeeSupport.
     *
     * @return true if shared database is enabled, false otherwise
     */
    public boolean isSharedDatabaseEnabled() {
        return sharedDatabaseEnabled;
    }
    
    /**
     * Get whether SQL injection detection is enabled.
     *
     * @return true if SQL injection detection is enabled, false otherwise
     */
    public boolean isSqlInjectionDetectionEnabled() {
        return sqlInjectionDetectionEnabled;
    }
    
    /**
     * Get whether logging of dangerous operations is enabled.
     *
     * @return true if logging of dangerous operations is enabled, false otherwise
     */
    public boolean isLogDangerousOperations() {
        return logDangerousOperations;
    }
    
    /**
     * Get the maximum query length.
     *
     * @return The maximum query length
     */
    public int getMaxQueryLength() {
        return maxQueryLength;
    }
    
    /**
     * Get whether performance monitoring is enabled.
     *
     * @return true if monitoring is enabled, false otherwise
     */
    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }
    
    /**
     * Get the slow query threshold in milliseconds.
     *
     * @return The slow query threshold
     */
    public long getSlowQueryThreshold() {
        return slowQueryThreshold;
    }
    
    /**
     * Get whether metrics collection is enabled.
     *
     * @return true if metrics collection is enabled, false otherwise
     */
    public boolean isCollectMetrics() {
        return collectMetrics;
    }
    
    /**
     * Get the metrics collection interval in seconds.
     *
     * @return The metrics interval
     */
    public int getMetricsInterval() {
        return metricsInterval;
    }
    
    /**
     * Get whether automatic migration is enabled.
     *
     * @return true if auto-migrate is enabled, false otherwise
     */
    public boolean isAutoMigrateEnabled() {
        return autoMigrateEnabled;
    }
    
    /**
     * Get whether failed migrations should be rolled back.
     *
     * @return true if rollback is enabled, false otherwise
     */
    public boolean isRollbackFailedMigrationsEnabled() {
        return rollbackFailedMigrationsEnabled;
    }
    
    /**
     * Get the default batch size for batch operations.
     *
     * @return The default batch size
     */
    public int getDefaultBatchSize() {
        return defaultBatchSize;
    }
    
    /**
     * Get the maximum batch size allowed.
     *
     * @return The maximum batch size
     */
    public int getMaxBatchSize() {
        return maxBatchSize;
    }
    
    /**
     * Get the database thread pool size.
     *
     * @return The thread pool size
     */
    public int getDatabaseThreadPoolSize() {
        return databaseThreadPoolSize;
    }
}