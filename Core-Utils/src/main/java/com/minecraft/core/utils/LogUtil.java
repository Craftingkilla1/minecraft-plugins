package com.minecraft.core.utils;

import org.bukkit.plugin.Plugin;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for consistent logging throughout plugins
 * Improved to support multiple plugins and reduce redundant logging
 */
public class LogUtil {
    private static Logger logger;
    private static String prefix = "[CoreUtils] ";
    private static boolean debugMode = false;
    private static Plugin plugin;
    
    private LogUtil() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Initialize the logger
     * @param pluginInstance The plugin instance
     */
    public static void init(Plugin pluginInstance) {
        plugin = pluginInstance;
        logger = pluginInstance.getLogger();
        debugMode = pluginInstance.getConfig().getBoolean("debug-mode", false);
        info("Logging initialized, debug mode: " + debugMode);
    }
    
    /**
     * Set the prefix for log messages
     * @param newPrefix The new prefix
     */
    public static void setPrefix(String newPrefix) {
        prefix = newPrefix;
    }
    
    /**
     * Set whether debug logging is enabled
     * @param enabled Whether debug logging is enabled
     */
    public static void setDebugMode(boolean enabled) {
        boolean changed = debugMode != enabled;
        debugMode = enabled;
        
        if (changed) {
            info("Debug mode " + (enabled ? "enabled" : "disabled"));
        }
    }
    
    /**
     * Log an informational message
     * @param message The message to log
     */
    public static void info(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }
    
    /**
     * Log a warning message
     * @param message The message to log
     */
    public static void warning(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }
    
    /**
     * Log a severe error message
     * @param message The message to log
     */
    public static void severe(String message) {
        if (logger != null) {
            logger.severe(message);
        }
    }
    
    /**
     * Log a debug message (only if debug mode is enabled)
     * @param message The message to log
     */
    public static void debug(String message) {
        if (debugMode && logger != null) {
            logger.log(Level.FINE, prefix + "[DEBUG] " + message);
        }
    }
    
    /**
     * Log an exception with a message
     * @param message The message to log
     * @param ex The exception
     */
    public static void exception(String message, Throwable ex) {
        if (logger != null) {
            logger.log(Level.SEVERE, prefix + message, ex);
        }
    }
    
    /**
     * Log a formatted message with arguments
     * Similar to String.format but combines it with logging
     * @param format The format string
     * @param args The arguments
     */
    public static void format(String format, Object... args) {
        if (logger != null) {
            logger.info(String.format(format, args));
        }
    }
    
    /**
     * Log a formatted debug message with arguments (only if debug mode is enabled)
     * @param format The format string
     * @param args The arguments
     */
    public static void debugFormat(String format, Object... args) {
        if (debugMode && logger != null) {
            logger.log(Level.FINE, prefix + "[DEBUG] " + String.format(format, args));
        }
    }
    
    /**
     * Check if debug mode is enabled
     * @return Whether debug mode is enabled
     */
    public static boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * Get the current plugin
     * @return The current plugin
     */
    public static Plugin getPlugin() {
        return plugin;
    }
    
    /**
     * Create a new instance of LogUtil with a specific plugin and prefix
     * @param pluginInstance The plugin instance
     * @param logPrefix The log prefix
     * @return A new PluginLogger
     */
    public static PluginLogger forPlugin(Plugin pluginInstance, String logPrefix) {
        return new PluginLogger(pluginInstance, logPrefix);
    }
    
    /**
     * A logger for a specific plugin
     */
    public static class PluginLogger {
        private final Logger pluginLogger;
        private final String logPrefix;
        private boolean pluginDebugMode;
        
        /**
         * Create a new plugin logger
         * @param pluginInstance The plugin instance
         * @param prefix The log prefix
         */
        private PluginLogger(Plugin pluginInstance, String prefix) {
            this.pluginLogger = pluginInstance.getLogger();
            this.logPrefix = prefix;
            this.pluginDebugMode = pluginInstance.getConfig().getBoolean("debug-mode", false);
        }
        
        /**
         * Set whether debug logging is enabled for this plugin
         * @param enabled Whether debug logging is enabled
         */
        public void setDebugMode(boolean enabled) {
            this.pluginDebugMode = enabled;
        }
        
        /**
         * Log an informational message
         * @param message The message to log
         */
        public void info(String message) {
            pluginLogger.info(message);
        }
        
        /**
         * Log a warning message
         * @param message The message to log
         */
        public void warning(String message) {
            pluginLogger.warning(message);
        }
        
        /**
         * Log a severe error message
         * @param message The message to log
         */
        public void severe(String message) {
            pluginLogger.severe(message);
        }
        
        /**
         * Log a debug message (only if debug mode is enabled)
         * @param message The message to log
         */
        public void debug(String message) {
            if (pluginDebugMode) {
                pluginLogger.log(Level.FINE, logPrefix + "[DEBUG] " + message);
            }
        }
        
        /**
         * Log an exception with a message
         * @param message The message to log
         * @param ex The exception
         */
        public void exception(String message, Throwable ex) {
            pluginLogger.log(Level.SEVERE, logPrefix + message, ex);
        }
    }
}