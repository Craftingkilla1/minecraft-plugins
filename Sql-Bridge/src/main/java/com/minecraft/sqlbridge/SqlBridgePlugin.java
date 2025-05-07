package com.minecraft.sqlbridge;

import com.minecraft.core.CorePlugin;
import com.minecraft.core.api.service.ServiceRegistry;
import com.minecraft.core.command.CommandRegistry;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.api.BatchOperations;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.DatabaseType;
import com.minecraft.sqlbridge.command.SqlBridgeCommand;
import com.minecraft.sqlbridge.connection.ConnectionManager;
import com.minecraft.sqlbridge.connection.ConnectionMonitor;
import com.minecraft.sqlbridge.error.DatabaseErrorHandler;
import com.minecraft.sqlbridge.impl.DefaultDatabase;
import com.minecraft.sqlbridge.logging.EnhancedLogger;
import com.minecraft.sqlbridge.migrations.MigrationManager;
import com.minecraft.sqlbridge.security.SqlInjectionDetector;
import com.minecraft.sqlbridge.security.SqlOperationValidator;
import com.minecraft.sqlbridge.stats.QueryStatistics;
import com.minecraft.sqlbridge.test.ConnectionTest;
import com.minecraft.sqlbridge.test.QueryTest;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Main class for the SQL-Bridge plugin providing database connectivity for other plugins.
 * Depends on Core-Utils for service registry and utility functions.
 */
public class SqlBridgePlugin extends JavaPlugin {
    
    private ConnectionManager connectionManager;
    private MigrationManager migrationManager;
    private Database database;
    private SqlInjectionDetector injectionDetector;
    private QueryStatistics statistics;
    private BukkitTask statsLoggerTask;
    private DatabaseErrorHandler errorHandler;
    private ConnectionMonitor connectionMonitor;
    private SqlOperationValidator sqlValidator;
    private EnhancedLogger enhancedLogger;
    
    @Override
    public void onEnable() {
        // Setup default logging
        LogUtil.init(this);
        LogUtil.info("Initializing SQL-Bridge plugin...");
        
        // Load configuration
        saveDefaultConfig();
        
        // Initialize enhanced logging
        initEnhancedLogging();
        
        // Initialize statistics tracking
        initStatistics();
        
        // Initialize security components
        initSecurity();
        
        // Initialize error handler
        initErrorHandler();
        
        // Initialize database connection
        initDatabaseConnection();
        
        // Initialize connection monitor
        initConnectionMonitor();
        
        // Initialize migration manager
        migrationManager = new MigrationManager(this, connectionManager);
        
        // Initialize SQL validator
        initSqlValidator();
        
        // Register services with Core-Utils service registry
        registerServices();
        
        // Register commands
        registerCommands();
        
        // Run tests if enabled
        runTests();
        
        enhancedLogger.info(EnhancedLogger.GENERAL, "SQL-Bridge plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Stop statistics logging task
        if (statsLoggerTask != null) {
            statsLoggerTask.cancel();
        }
        
        // Stop connection monitoring
        if (connectionMonitor != null) {
            connectionMonitor.stopMonitoring();
        }
        
        // Shutdown enhanced logger
        if (enhancedLogger != null) {
            enhancedLogger.shutdown();
        }
        
        // Unregister services
        ServiceRegistry.unregister(Database.class);
        ServiceRegistry.unregister(BatchOperations.class);
        
        // Close database connections
        if (connectionManager != null) {
            connectionManager.closeAllConnections();
        }
        
        LogUtil.info("SQL-Bridge plugin disabled.");
    }
    
    /**
     * Initialize enhanced logging system
     */
    private void initEnhancedLogging() {
        enhancedLogger = new EnhancedLogger(this);
        
        // Add default filters if needed
        if (getConfig().getBoolean("logging.filter-debug", true) && 
                !getConfig().getBoolean("debug.enabled", false)) {
            enhancedLogger.addFilter(entry -> 
                    entry.getLevel() != EnhancedLogger.LogLevel.DEBUG);
        }
        
        enhancedLogger.info(EnhancedLogger.GENERAL, "Enhanced logging system initialized");
    }
    
    /**
     * Initialize statistics tracking
     */
    private void initStatistics() {
        statistics = QueryStatistics.getInstance(this);
        
        // Setup periodic statistics logging if enabled
        if (getConfig().getBoolean("statistics.log-stats", true)) {
            int interval = getConfig().getInt("statistics.log-interval", 60) * 20 * 60; // convert minutes to ticks
            
            statsLoggerTask = getServer().getScheduler().runTaskTimerAsynchronously(this, 
                    this::logStatistics, interval, interval);
        }
        
        enhancedLogger.info(EnhancedLogger.GENERAL, "Statistics tracking initialized");
    }
    
    /**
     * Initialize security components
     */
    private void initSecurity() {
        injectionDetector = new SqlInjectionDetector(this);
        enhancedLogger.info(EnhancedLogger.SECURITY, "SQL injection detection initialized");
    }
    
    /**
     * Initialize error handler
     */
    private void initErrorHandler() {
        errorHandler = new DatabaseErrorHandler(this, connectionManager);
        enhancedLogger.info(EnhancedLogger.GENERAL, "Database error handler initialized");
    }
    
    /**
     * Initialize connection monitoring
     */
    private void initConnectionMonitor() {
        connectionMonitor = new ConnectionMonitor(this, connectionManager);
        connectionMonitor.startMonitoring();
        enhancedLogger.info(EnhancedLogger.CONNECTION, "Connection monitoring started");
    }
    
    /**
     * Initialize SQL operation validator
     */
    private void initSqlValidator() {
        sqlValidator = new SqlOperationValidator(this, injectionDetector, errorHandler);
        enhancedLogger.info(EnhancedLogger.SECURITY, "SQL operation validator initialized");
    }
    
    /**
     * Log database statistics
     */
    private void logStatistics() {
        Map<String, Object> stats = statistics.getStatistics();
        
        enhancedLogger.info(EnhancedLogger.PERFORMANCE, "=== Database Statistics ===");
        enhancedLogger.info(EnhancedLogger.PERFORMANCE, "Total Queries: " + stats.get("totalQueries"));
        enhancedLogger.info(EnhancedLogger.PERFORMANCE, "- Selects: " + stats.get("totalSelects"));
        enhancedLogger.info(EnhancedLogger.PERFORMANCE, "- Inserts: " + stats.get("totalInserts"));
        enhancedLogger.info(EnhancedLogger.PERFORMANCE, "- Updates: " + stats.get("totalUpdates"));
        enhancedLogger.info(EnhancedLogger.PERFORMANCE, "- Deletes: " + stats.get("totalDeletes"));
        enhancedLogger.info(EnhancedLogger.PERFORMANCE, "Batch Operations: " + stats.get("totalBatchOperations"));
        enhancedLogger.info(EnhancedLogger.PERFORMANCE, "Transactions: " + stats.get("totalTransactions"));
        enhancedLogger.info(EnhancedLogger.PERFORMANCE, "Errors: " + stats.get("totalErrors"));
        enhancedLogger.info(EnhancedLogger.PERFORMANCE, "Average Query Time: " + stats.get("averageQueryTime") + "ms");
        enhancedLogger.info(EnhancedLogger.PERFORMANCE, "Max Query Time: " + stats.get("maxQueryTime") + "ms");
        enhancedLogger.info(EnhancedLogger.PERFORMANCE, "Active Connections: " + stats.get("activeConnections"));
        enhancedLogger.info(EnhancedLogger.PERFORMANCE, "Total Connections Created: " + stats.get("totalConnectionsCreated"));
        enhancedLogger.info(EnhancedLogger.PERFORMANCE, "Max Concurrent Connections: " + stats.get("maxConcurrentConnections"));
        
        // Add connection pool stats if available
        if (connectionManager != null) {
            Map<String, Object> poolStats = connectionManager.getPoolStatistics();
            if (poolStats != null) {
                enhancedLogger.info(EnhancedLogger.PERFORMANCE, "=== Connection Pool ===");
                enhancedLogger.info(EnhancedLogger.PERFORMANCE, "Pooled Connections: " + poolStats.get("pooledConnections"));
                enhancedLogger.info(EnhancedLogger.PERFORMANCE, "In Use: " + poolStats.get("inUse"));
                enhancedLogger.info(EnhancedLogger.PERFORMANCE, "Available: " + poolStats.get("available"));
            }
        }
    }
    
    /**
     * Initialize database connection based on configuration
     */
    private void initDatabaseConnection() {
        FileConfiguration config = getConfig();
        String typeStr = config.getString("database.type", "SQLITE").toUpperCase();
        DatabaseType type;
        
        try {
            type = DatabaseType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            enhancedLogger.warning(EnhancedLogger.GENERAL, "Invalid database type: " + typeStr + ". Defaulting to SQLite.");
            type = DatabaseType.SQLITE;
        }
        
        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String database = config.getString("database.name", "minecraft");
        String username = config.getString("database.username", "root");
        String password = config.getString("database.password", "");
        
        // If type is MySQL, make sure we have MySQL specific settings
        if (type == DatabaseType.MYSQL && 
                (username == null || username.isEmpty() || password == null)) {
            enhancedLogger.warning(EnhancedLogger.CONNECTION, 
                "MySQL configuration is incomplete. Make sure host, port, name, username, and password are set.");
        }
        
        // Add default config value for SQL dialect adaptation
        if (!config.contains("database.adapt-sql-dialect")) {
            config.set("database.adapt-sql-dialect", true);
            saveConfig();
        }
        
        // Log the database type being used
        enhancedLogger.info(EnhancedLogger.CONNECTION, "Using database type: " + type.name());
        
        // Create connection manager
        connectionManager = new ConnectionManager(this, type, host, port, database, username, password);
        
        // Initialize and verify connection
        if (connectionManager.initialize()) {
            enhancedLogger.info(EnhancedLogger.CONNECTION, "Successfully connected to " + type + " database.");
        } else {
            enhancedLogger.error(EnhancedLogger.CONNECTION, "Failed to connect to database. Check your configuration.");
        }
    }
    
    /**
     * Register services with the Core-Utils service registry
     */
    private void registerServices() {
        // Create and register Database service
        database = new DefaultDatabase(this, connectionManager);
        ServiceRegistry.register(Database.class, database);
        
        // Register the database as a BatchOperations service if it implements the interface
        if (database instanceof BatchOperations) {
            ServiceRegistry.register(BatchOperations.class, (BatchOperations) database);
            enhancedLogger.info(EnhancedLogger.GENERAL, "Registered BatchOperations service.");
        }
        
        enhancedLogger.info(EnhancedLogger.GENERAL, "Registered Database service.");
    }
    
    /**
     * Register commands with the Core-Utils command registry
     */
    private void registerCommands() {
        // Get CorePlugin instance
        Plugin corePlugin = getServer().getPluginManager().getPlugin("CoreUtils");
        if (corePlugin instanceof CorePlugin) {
            CorePlugin core = (CorePlugin) corePlugin;
            
            // Get command registry
            CommandRegistry commandRegistry = core.getCommandRegistry();
            
            // Register commands
            SqlBridgeCommand sqlBridgeCommand = new SqlBridgeCommand(this);
            commandRegistry.registerCommand(sqlBridgeCommand);
            
            enhancedLogger.info(EnhancedLogger.GENERAL, "Registered SqlBridge commands.");
        } else {
            enhancedLogger.warning(EnhancedLogger.GENERAL, 
                    "CoreUtils plugin not found or not an instance of CorePlugin. Commands will not be available.");
        }
    }
    
    /**
     * Run tests if enabled in config
     */
    private void runTests() {
        if (!getConfig().getBoolean("test.enabled", false)) {
            return;
        }
        
        if (getConfig().getBoolean("test.run-on-enable", false)) {
            enhancedLogger.info(EnhancedLogger.GENERAL, "Running database tests...");
            
            // Run tests asynchronously to avoid blocking the main thread
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                // Run connection tests
                ConnectionTest connectionTest = new ConnectionTest(this);
                connectionTest.runAllTests();
                
                // Run query tests
                QueryTest queryTest = new QueryTest(this);
                queryTest.runAllTests();
                
                enhancedLogger.info(EnhancedLogger.GENERAL, "Database tests completed.");
            });
        }
    }
    
    /**
     * Get the connection manager instance
     * 
     * @return The connection manager
     */
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }
    
    /**
     * Get the migration manager instance
     * 
     * @return The migration manager
     */
    public MigrationManager getMigrationManager() {
        return migrationManager;
    }
    
    /**
     * Get the SQL injection detector instance
     * 
     * @return The SQL injection detector
     */
    public SqlInjectionDetector getSqlInjectionDetector() {
        return injectionDetector;
    }
    
    /**
     * Get the query statistics instance
     * 
     * @return The query statistics
     */
    public QueryStatistics getQueryStatistics() {
        return statistics;
    }
    
    /**
     * Get the database instance
     * 
     * @return The database
     */
    public Database getDatabase() {
        return database;
    }
    
    /**
     * Get the database error handler
     * 
     * @return The database error handler
     */
    public DatabaseErrorHandler getErrorHandler() {
        return errorHandler;
    }
    
    /**
     * Get the connection monitor
     * 
     * @return The connection monitor
     */
    public ConnectionMonitor getConnectionMonitor() {
        return connectionMonitor;
    }
    
    /**
     * Get the SQL operation validator
     * 
     * @return The SQL operation validator
     */
    public SqlOperationValidator getSqlValidator() {
        return sqlValidator;
    }
    
    /**
     * Get the enhanced logger
     * 
     * @return The enhanced logger
     */
    public EnhancedLogger getEnhancedLogger() {
        return enhancedLogger;
    }
}