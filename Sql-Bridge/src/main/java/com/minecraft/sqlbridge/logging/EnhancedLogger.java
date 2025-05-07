package com.minecraft.sqlbridge.logging;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.core.utils.TimeUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.utils.TimeUtilExtended;

import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Enhanced logging system that builds on Core-Utils LogUtil.
 * Provides advanced features like categorized logging, log rotation,
 * and in-memory log buffering.
 */
public class EnhancedLogger {

    private final SqlBridgePlugin plugin;
    private final File logDirectory;
    private final Map<LogCategory, Deque<LogEntry>> logBuffers = new ConcurrentHashMap<>();
    private final int maxBufferSize;
    private final boolean fileLoggingEnabled;
    private final boolean consoleLoggingEnabled;
    private final List<LogFilter> filters = new ArrayList<>();
    private final List<Consumer<LogEntry>> listeners = new ArrayList<>();
    private BukkitTask fileWriterTask;
    private final AtomicBoolean flushingLogs = new AtomicBoolean(false);
    
    // Default categories
    public static final LogCategory QUERY = new LogCategory("query", true);
    public static final LogCategory ERROR = new LogCategory("error", true);
    public static final LogCategory SECURITY = new LogCategory("security", true);
    public static final LogCategory PERFORMANCE = new LogCategory("performance", true);
    public static final LogCategory CONNECTION = new LogCategory("connection", true);
    public static final LogCategory MIGRATION = new LogCategory("migration", true);
    public static final LogCategory GENERAL = new LogCategory("general", true);
    
    /**
     * Create a new enhanced logger
     *
     * @param plugin The SQL-Bridge plugin instance
     */
    public EnhancedLogger(SqlBridgePlugin plugin) {
        this.plugin = plugin;
        this.logDirectory = new File(plugin.getDataFolder(), "logs");
        this.maxBufferSize = plugin.getConfig().getInt("logging.buffer-size", 1000);
        this.fileLoggingEnabled = plugin.getConfig().getBoolean("logging.file-enabled", true);
        this.consoleLoggingEnabled = plugin.getConfig().getBoolean("logging.console-enabled", true);
        
        // Initialize log directory
        if (fileLoggingEnabled && !logDirectory.exists()) {
            logDirectory.mkdirs();
        }
        
        // Initialize buffers for default categories
        initializeBuffer(QUERY);
        initializeBuffer(ERROR);
        initializeBuffer(SECURITY);
        initializeBuffer(PERFORMANCE);
        initializeBuffer(CONNECTION);
        initializeBuffer(MIGRATION);
        initializeBuffer(GENERAL);
        
        // Start log file writer task if enabled
        if (fileLoggingEnabled) {
            startFileWriterTask();
        }
    }
    
    /**
     * Initialize a log buffer for a category
     *
     * @param category The log category
     */
    private void initializeBuffer(LogCategory category) {
        logBuffers.put(category, new ArrayDeque<>(maxBufferSize));
    }
    
    /**
     * Start the task that writes logs to files
     */
    private void startFileWriterTask() {
        // Create task to flush logs to files periodically
        long interval = plugin.getConfig().getLong("logging.flush-interval", 60) * 20; // Convert seconds to ticks
        
        fileWriterTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::flushLogsToFiles,
                interval,
                interval
        );
    }
    
    /**
     * Add a log filter
     *
     * @param filter The filter to add
     */
    public void addFilter(LogFilter filter) {
        filters.add(filter);
    }
    
    /**
     * Add a log listener
     *
     * @param listener The listener to add
     */
    public void addListener(Consumer<LogEntry> listener) {
        listeners.add(listener);
    }
    
    /**
     * Log a message to a specified category
     *
     * @param category The log category
     * @param level The log level
     * @param message The log message
     */
    public void log(LogCategory category, LogLevel level, String message) {
        // Create log entry
        LogEntry entry = new LogEntry(
                System.currentTimeMillis(),
                category,
                level,
                message,
                Thread.currentThread().getName()
        );
        
        // Apply filters
        for (LogFilter filter : filters) {
            if (!filter.test(entry)) {
                return; // Skip this log entry
            }
        }
        
        // Store in buffer
        Deque<LogEntry> buffer = logBuffers.get(category);
        if (buffer != null) {
            synchronized (buffer) {
                // If buffer is full, remove oldest entry
                if (buffer.size() >= maxBufferSize) {
                    buffer.pollFirst();
                }
                
                buffer.addLast(entry);
            }
        }
        
        // Log to console if enabled
        if (consoleLoggingEnabled && category.isConsoleEnabled()) {
            logToConsole(entry);
        }
        
        // Notify listeners
        for (Consumer<LogEntry> listener : listeners) {
            try {
                listener.accept(entry);
            } catch (Exception e) {
                LogUtil.warning("Error in log listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * Log an info message
     *
     * @param category The log category
     * @param message The log message
     */
    public void info(LogCategory category, String message) {
        log(category, LogLevel.INFO, message);
    }
    
    /**
     * Log a warning message
     *
     * @param category The log category
     * @param message The log message
     */
    public void warning(LogCategory category, String message) {
        log(category, LogLevel.WARNING, message);
    }
    
    /**
     * Log an error message
     *
     * @param category The log category
     * @param message The log message
     */
    public void error(LogCategory category, String message) {
        log(category, LogLevel.ERROR, message);
    }
    
    /**
     * Log a debug message
     *
     * @param category The log category
     * @param message The log message
     */
    public void debug(LogCategory category, String message) {
        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            log(category, LogLevel.DEBUG, message);
        }
    }
    
    /**
     * Get recent logs for a category
     *
     * @param category The log category
     * @param limit Maximum number of logs to return
     * @return List of recent log entries
     */
    public List<LogEntry> getRecentLogs(LogCategory category, int limit) {
        Deque<LogEntry> buffer = logBuffers.get(category);
        if (buffer == null) {
            return new ArrayList<>();
        }
        
        synchronized (buffer) {
            List<LogEntry> logs = new ArrayList<>(buffer);
            if (logs.size() > limit) {
                return logs.subList(logs.size() - limit, logs.size());
            }
            return logs;
        }
    }
    
    /**
     * Get recent logs for all categories
     *
     * @param limit Maximum number of logs to return per category
     * @return Map of category to recent log entries
     */
    public Map<LogCategory, List<LogEntry>> getAllRecentLogs(int limit) {
        Map<LogCategory, List<LogEntry>> result = new ConcurrentHashMap<>();
        
        for (Map.Entry<LogCategory, Deque<LogEntry>> entry : logBuffers.entrySet()) {
            result.put(entry.getKey(), getRecentLogs(entry.getKey(), limit));
        }
        
        return result;
    }
    
    /**
     * Flush all logs to their respective files
     */
    public void flushLogsToFiles() {
        if (!fileLoggingEnabled || !logDirectory.exists()) {
            return;
        }
        
        // Prevent concurrent flushes
        if (!flushingLogs.compareAndSet(false, true)) {
            return;
        }
        
        try {
            // Get current date for log file name
            LocalDateTime now = TimeUtil.now();
            String dateStr = TimeUtil.formatDate(now).replace("-", "");
            
            // Flush each category to its own file
            for (Map.Entry<LogCategory, Deque<LogEntry>> entry : logBuffers.entrySet()) {
                LogCategory category = entry.getKey();
                Deque<LogEntry> buffer = entry.getValue();
                
                // Skip if category doesn't have file logging enabled
                if (!category.isFileEnabled()) {
                    continue;
                }
                
                // Nothing to flush
                if (buffer.isEmpty()) {
                    continue;
                }
                
                // Get log entries to flush
                List<LogEntry> logsToFlush = new ArrayList<>();
                synchronized (buffer) {
                    logsToFlush.addAll(buffer);
                    buffer.clear();
                }
                
                // Create log file
                File logFile = new File(logDirectory, category.getName() + "-" + dateStr + ".log");
                boolean fileExists = logFile.exists();
                
                // Write logs to file
                try (FileWriter fw = new FileWriter(logFile, true);
                     PrintWriter out = new PrintWriter(fw)) {
                    
                    // Add header if new file
                    if (!fileExists) {
                        out.println("# SQL-Bridge " + category.getName() + " Log - " + 
                                TimeUtil.formatDateTime(now));
                        out.println("# Format: [Timestamp] [Level] [Thread] Message");
                        out.println("# ------------------------------------------------");
                    }
                    
                    // Write each log entry
                    for (LogEntry log : logsToFlush) {
                        out.println(formatLogForFile(log));
                    }
                } catch (IOException e) {
                    LogUtil.severe("Error writing " + category.getName() + " logs to file: " + e.getMessage());
                }
            }
        } finally {
            flushingLogs.set(false);
        }
    }
    
    /**
     * Format a log entry for file output
     *
     * @param entry The log entry
     * @return Formatted log string
     */
    private String formatLogForFile(LogEntry entry) {
        LocalDateTime time = TimeUtilExtended.fromMillis(entry.getTimestamp());
        String timestamp = TimeUtil.formatDateTime(time);
        return String.format("[%s] [%s] [%s] %s", 
                timestamp, entry.getLevel(), entry.getThread(), entry.getMessage());
    }
    
    /**
     * Log an entry to the console using Core-Utils LogUtil
     *
     * @param entry The log entry
     */
    private void logToConsole(LogEntry entry) {
        String message = "[" + entry.getCategory().getName().toUpperCase() + "] " + entry.getMessage();
        
        switch (entry.getLevel()) {
            case DEBUG:
                LogUtil.debug(message);
                break;
            case INFO:
                LogUtil.info(message);
                break;
            case WARNING:
                LogUtil.warning(message);
                break;
            case ERROR:
                LogUtil.severe(message);
                break;
        }
    }
    
    /**
     * Clean up resources
     */
    public void shutdown() {
        // Cancel file writer task
        if (fileWriterTask != null) {
            fileWriterTask.cancel();
        }
        
        // Flush remaining logs
        flushLogsToFiles();
    }
    
    /**
     * Log category class
     */
    public static class LogCategory {
        private final String name;
        private final boolean consoleEnabled;
        private boolean fileEnabled;
        
        public LogCategory(String name, boolean consoleEnabled) {
            this.name = name;
            this.consoleEnabled = consoleEnabled;
            this.fileEnabled = true;
        }
        
        public String getName() {
            return name;
        }
        
        public boolean isConsoleEnabled() {
            return consoleEnabled;
        }
        
        public boolean isFileEnabled() {
            return fileEnabled;
        }
        
        public void setFileEnabled(boolean fileEnabled) {
            this.fileEnabled = fileEnabled;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            LogCategory that = (LogCategory) obj;
            return name.equals(that.name);
        }
        
        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
    
    /**
     * Log level enum
     */
    public enum LogLevel {
        DEBUG, INFO, WARNING, ERROR
    }
    
    /**
     * Log entry class
     */
    public static class LogEntry {
        private final long timestamp;
        private final LogCategory category;
        private final LogLevel level;
        private final String message;
        private final String thread;
        
        public LogEntry(long timestamp, LogCategory category, LogLevel level, String message, String thread) {
            this.timestamp = timestamp;
            this.category = category;
            this.level = level;
            this.message = message;
            this.thread = thread;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public LogCategory getCategory() {
            return category;
        }
        
        public LogLevel getLevel() {
            return level;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getThread() {
            return thread;
        }
    }
    
    /**
     * Log filter interface
     */
    @FunctionalInterface
    public interface LogFilter extends Predicate<LogEntry> {
        @Override
        boolean test(LogEntry log);
    }
}