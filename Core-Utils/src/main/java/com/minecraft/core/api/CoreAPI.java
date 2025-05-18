package com.minecraft.core.api;

import com.minecraft.core.CorePlugin;
import com.minecraft.core.api.events.EventManager;
import com.minecraft.core.api.service.ServiceLocator;
import com.minecraft.core.api.service.ServiceRegistry;
import com.minecraft.core.command.CommandRegistry;
import com.minecraft.core.command.TabCompletionProvider;
import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.config.ConfigManager;
import com.minecraft.core.config.Messages;
import com.minecraft.core.utils.BungeeUtils;
import com.minecraft.core.utils.FormatUtil;
import com.minecraft.core.utils.InventoryUtil;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.core.utils.TimeUtil;

import org.bukkit.plugin.Plugin;

/**
 * Central access point for Core-Utils API
 * This class provides easy access to all major components of the Core-Utils framework.
 */
public final class CoreAPI {
    
    private static CorePlugin corePlugin;
    
    private CoreAPI() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Initialize the Core API
     * This is automatically called by CorePlugin during startup
     *
     * @param plugin The Core-Utils plugin instance
     */
    public static void init(CorePlugin plugin) {
        corePlugin = plugin;
        LogUtil.debug("CoreAPI initialized");
    }
    
    /**
     * Get the Core-Utils plugin instance
     *
     * @return The Core-Utils plugin instance
     * @throws IllegalStateException if Core-Utils is not initialized
     */
    public static CorePlugin getPlugin() {
        checkInitialized();
        return corePlugin;
    }
    
    /**
     * Get the Command Registry for registering custom commands
     *
     * @return The command registry
     * @throws IllegalStateException if Core-Utils is not initialized
     */
    public static CommandRegistry getCommandRegistry() {
        checkInitialized();
        return corePlugin.getCommandRegistry();
    }
    
    /**
     * Get the Config Manager for handling configuration files
     *
     * @return The config manager
     * @throws IllegalStateException if Core-Utils is not initialized
     */
    public static ConfigManager getConfigManager() {
        checkInitialized();
        return corePlugin.getConfigManager();
    }
    
    /**
     * Get the Messages utility for localized messages
     *
     * @return The messages utility
     * @throws IllegalStateException if Core-Utils is not initialized
     */
    public static Messages getMessages() {
        checkInitialized();
        return corePlugin.getMessages();
    }
    
    /**
     * Get the BungeeUtils for cross-server communication
     *
     * @return The BungeeUtils instance, or null if BungeeCord support is disabled
     * @throws IllegalStateException if Core-Utils is not initialized
     */
    public static BungeeUtils getBungeeUtils() {
        checkInitialized();
        return corePlugin.getBungeeUtils();
    }
    
    /**
     * Check if Core-Utils is properly initialized
     *
     * @throws IllegalStateException if Core-Utils is not initialized
     */
    private static void checkInitialized() {
        if (corePlugin == null) {
            throw new IllegalStateException("Core-Utils API is not initialized. Make sure Core-Utils plugin is loaded and enabled.");
        }
    }
    
    /**
     * Utility class that provides quick access to common API components
     */
    public static final class Services {
        /**
         * Register a service implementation
         *
         * @param <T> The service type
         * @param serviceClass The service interface class
         * @param implementation The service implementation
         * @return true if registration was successful
         */
        public static <T> boolean register(Class<T> serviceClass, T implementation) {
            return ServiceRegistry.register(serviceClass, implementation);
        }
        
        /**
         * Get a registered service
         *
         * @param <T> The service type
         * @param serviceClass The service interface class
         * @return The service implementation, or null if not registered
         */
        public static <T> T get(Class<T> serviceClass) {
            return ServiceLocator.getService(serviceClass);
        }
        
        /**
         * Get a service, throwing an exception if not found
         *
         * @param <T> The service type
         * @param serviceClass The service interface class
         * @return The service implementation
         * @throws ServiceLocator.ServiceNotFoundException If the service is not found
         */
        public static <T> T require(Class<T> serviceClass) {
            return ServiceLocator.requireService(serviceClass);
        }
        
        /**
         * Check if a service is registered
         *
         * @param serviceClass The service interface class
         * @return true if the service is registered
         */
        public static boolean isRegistered(Class<?> serviceClass) {
            return ServiceRegistry.isRegistered(serviceClass);
        }
        
        /**
         * Unregister a service
         *
         * @param <T> The service type
         * @param serviceClass The service interface class
         * @return true if unregistration was successful
         */
        public static <T> boolean unregister(Class<T> serviceClass) {
            return ServiceRegistry.unregister(serviceClass);
        }
    }
    
    /**
     * Utility class that provides quick access to common utility methods
     */
    public static final class Utils {
        /**
         * Initialize logging for a plugin
         *
         * @param plugin The plugin
         */
        public static void initLogging(Plugin plugin) {
            LogUtil.init(plugin);
        }
        
        /**
         * Create a custom logger for a plugin
         *
         * @param plugin The plugin
         * @param prefix The log prefix
         * @return A custom logger
         */
        public static LogUtil.PluginLogger createLogger(Plugin plugin, String prefix) {
            return LogUtil.forPlugin(plugin, prefix);
        }
    }
    
    /**
     * Utility class for working with commands
     */
    public static final class Commands {
        /**
         * Register a command class with Core-Utils
         * The class must be annotated with {@link Command}
         *
         * @param commandClass The command class instance
         * @return true if registration was successful
         */
        public static boolean register(Object commandClass) {
            return getCommandRegistry().registerCommand(commandClass);
        }
        
        /**
         * Register multiple command classes with Core-Utils
         *
         * @param commandClasses The command class instances
         * @return The number of successfully registered commands
         */
        public static int registerAll(Object... commandClasses) {
            return getCommandRegistry().registerCommands(commandClasses);
        }
    }
    
    /**
     * Utility class for working with events
     */
    public static final class Events {
        /**
         * Register a listener with Bukkit
         *
         * @param plugin The plugin
         * @param listener The listener to register
         */
        public static void registerListener(Plugin plugin, org.bukkit.event.Listener listener) {
            EventManager.registerListener(plugin, listener);
        }
        
        /**
         * Unregister a listener
         *
         * @param listener The listener to unregister
         */
        public static void unregisterListener(org.bukkit.event.Listener listener) {
            EventManager.unregisterListener(listener);
        }
    }
}