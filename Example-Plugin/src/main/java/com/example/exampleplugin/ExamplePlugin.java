package com.example.exampleplugin;

import com.example.exampleplugin.database.DatabaseManager;
import com.minecraft.core.CorePlugin;
import com.minecraft.core.api.service.ServiceLocator;
import com.minecraft.core.api.service.ServiceRegistry;
import com.minecraft.core.command.CommandRegistry;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.core.utils.TimeUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Example plugin that demonstrates how to use both Core-Utils and SQL-Bridge APIs
 */
public class ExamplePlugin extends JavaPlugin {
    private CorePlugin coreUtils;
    private SqlBridgePlugin sqlBridge;
    private ExampleService exampleService;
    private DatabaseManager databaseManager;
    
    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Check if Core-Utils is available
        Plugin coreUtilsPlugin = Bukkit.getPluginManager().getPlugin("CoreUtils");
        if (coreUtilsPlugin == null || !(coreUtilsPlugin instanceof CorePlugin)) {
            getLogger().severe("Core-Utils not found! This plugin requires Core-Utils to function.");
            getLogger().severe("Please download Core-Utils from: https://example.com/core-utils");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        // Get the Core-Utils instance
        coreUtils = (CorePlugin) coreUtilsPlugin;
        
        // Initialize with Core-Utils LogUtil
        LogUtil.init(this);
        LogUtil.setPrefix("[ExamplePlugin] ");
        
        // Check if we should enable debug mode
        boolean debugEnabled = getConfig().getBoolean("debug", false);
        LogUtil.setDebugMode(debugEnabled);
        
        // Check if SQL-Bridge is available
        Plugin sqlBridgePlugin = Bukkit.getPluginManager().getPlugin("SQLBridge");
        if (sqlBridgePlugin == null) {
            LogUtil.warning("SQL-Bridge not found! Database functionality will be disabled.");
            LogUtil.warning("Please download SQL-Bridge from: https://example.com/sql-bridge");
        } else {
            try {
                // Try to safely cast to SqlBridgePlugin using reflection to avoid class not found errors
                Class<?> sqlBridgeClass = Class.forName("com.minecraft.sqlbridge.SqlBridgePlugin");
                if (sqlBridgeClass.isInstance(sqlBridgePlugin)) {
                    // Get the SQL-Bridge instance
                    sqlBridge = (SqlBridgePlugin) sqlBridgePlugin;
                    LogUtil.info("SQL-Bridge found! Database functionality is enabled.");
                } else {
                    LogUtil.warning("Found plugin named SQLBridge but it's not the expected type!");
                }
            } catch (ClassNotFoundException e) {
                LogUtil.warning("SQL-Bridge API classes not found in classpath. Database functionality will be disabled.");
                LogUtil.debug("Error: " + e.getMessage());
            } catch (Exception e) {
                LogUtil.warning("Error initializing SQL-Bridge: " + e.getMessage());
                LogUtil.debug("Stack trace: " + e.getClass().getName());
            }
        }
        
        // Initialize database manager
        databaseManager = new DatabaseManager(this);
        
        // Create and register our service
        exampleService = new DefaultExampleService(this);
        ServiceRegistry.register(ExampleService.class, exampleService);
        
        // Register our commands using the Core-Utils command framework
        registerCommands();
        
        LogUtil.info("Example plugin enabled successfully!");
        LogUtil.debug("Current time: " + TimeUtil.formatDateTime(TimeUtil.now()));
    }
    
    @Override
    public void onDisable() {
        // Unregister our service
        if (exampleService != null) {
            ServiceRegistry.unregister(ExampleService.class);
            exampleService = null;
        }
        
        // Any other cleanup
        LogUtil.info("Example plugin disabled successfully!");
    }
    
    /**
     * Register commands with Core-Utils command framework
     */
    private void registerCommands() {
        // Get the command registry from Core-Utils
        CommandRegistry commandRegistry = coreUtils.getCommandRegistry();
        
        // Register our standard command class
        ExampleCommand exampleCommand = new ExampleCommand(this);
        commandRegistry.registerCommand(exampleCommand);
        
        // Register our database command class if SQL-Bridge is available
        if (sqlBridge != null && databaseManager.isInitialized()) {
            DatabaseCommand databaseCommand = new DatabaseCommand(this, databaseManager);
            commandRegistry.registerCommand(databaseCommand);
            LogUtil.debug("Registered database commands");
        }
        
        LogUtil.debug("Registered Example plugin commands");
    }
    
    /**
     * Get the service instance
     * 
     * @return The example service instance
     */
    public ExampleService getService() {
        return exampleService;
    }
    
    /**
     * Get the database manager
     * 
     * @return The database manager instance
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    /**
     * Example of accessing a service from another plugin
     */
    public void useOtherService() {
        // Try to access a service from another plugin
        try {
            // This will throw an exception if the service isn't available
            AnotherService anotherService = ServiceLocator.requireService(AnotherService.class);
            
            // Use the service
            String result = anotherService.doSomething("example");
            LogUtil.info("Result from AnotherService: " + result);
            
        } catch (ServiceLocator.ServiceNotFoundException e) {
            LogUtil.warning("AnotherService is not available: " + e.getMessage());
        }
    }
    
    /**
     * Get the Core-Utils plugin instance
     * 
     * @return The Core-Utils plugin instance
     */
    public CorePlugin getCoreUtils() {
        return coreUtils;
    }
    
    /**
     * Get the SQL-Bridge plugin instance
     * 
     * @return The SQL-Bridge plugin instance or null if not available
     */
    public SqlBridgePlugin getSqlBridge() {
        return sqlBridge;
    }
    
    /**
     * Example service interface for another plugin
     */
    private interface AnotherService {
        String doSomething(String input);
    }
}