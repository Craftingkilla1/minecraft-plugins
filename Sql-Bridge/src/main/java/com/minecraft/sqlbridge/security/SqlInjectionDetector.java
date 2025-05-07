package com.minecraft.sqlbridge.security;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced SQL injection detection system.
 * Provides methods to detect and log SQL injection attempts.
 */
public class SqlInjectionDetector {

    private final SqlBridgePlugin plugin;
    private boolean enabled;
    private boolean blockQueries;
    private boolean logAttempts;
    private boolean notifyAdmins;
    
    // Collection of SQL injection patterns
    private static final Pattern[] INJECTION_PATTERNS = {
        // Basic SQL injection
        Pattern.compile("'\\s*OR\\s*'?\\s*'?\\s*'?\\s*=\\s*'?\\s*'?\\s*'", Pattern.CASE_INSENSITIVE),
        Pattern.compile("'\\s*OR\\s*[0-9]+=\\s*[0-9]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("'\\s*OR\\s*'x'='x", Pattern.CASE_INSENSITIVE),
        
        // Comment-based SQL injection
        Pattern.compile("--\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\/\\*.*?\\*\\/", Pattern.CASE_INSENSITIVE),
        
        // UNION-based SQL injection
        Pattern.compile("UNION\\s+ALL\\s+SELECT", Pattern.CASE_INSENSITIVE),
        Pattern.compile("UNION\\s+SELECT", Pattern.CASE_INSENSITIVE),
        
        // Batch SQL injection
        Pattern.compile(";\\s*\\w+\\s*", Pattern.CASE_INSENSITIVE),
        
        // Boolean-based SQL injection
        Pattern.compile("AND\\s+[0-9]+=\\s*[0-9]+", Pattern.CASE_INSENSITIVE),
        
        // Time-based SQL injection
        Pattern.compile("SLEEP\\s*\\(\\s*[0-9]+\\s*\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("WAITFOR\\s+DELAY", Pattern.CASE_INSENSITIVE),
        Pattern.compile("BENCHMARK\\s*\\(", Pattern.CASE_INSENSITIVE),
        
        // Error-based SQL injection
        Pattern.compile("CONVERT\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("CAST\\s*\\(", Pattern.CASE_INSENSITIVE)
    };
    
    // Store recent injection attempts
    private final List<InjectionAttempt> recentAttempts = new ArrayList<>();
    private final Map<String, Integer> attemptsByIp = new HashMap<>();
    private static final int MAX_RECENT_ATTEMPTS = 50;

    /**
     * Create a new SQL injection detector
     *
     * @param plugin The SQL-Bridge plugin instance
     */
    public SqlInjectionDetector(SqlBridgePlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    /**
     * Reload configuration from the plugin config
     */
    public void reloadConfig() {
        this.enabled = plugin.getConfig().getBoolean("security.injection-detection.enabled", true);
        this.blockQueries = plugin.getConfig().getBoolean("security.injection-detection.block-queries", true);
        this.logAttempts = plugin.getConfig().getBoolean("security.injection-detection.log-attempts", true);
        this.notifyAdmins = plugin.getConfig().getBoolean("security.injection-detection.notify-admins", true);
    }

    /**
     * Check if a query contains potential SQL injection
     *
     * @param query The SQL query to check
     * @param source The source of the query (plugin name, etc.)
     * @param ip The IP address of the request (if applicable, can be null)
     * @return True if injection is detected, false otherwise
     */
    public boolean detectInjection(String query, String source, String ip) {
        if (!enabled || query == null || query.isEmpty()) {
            return false;
        }
        
        // Apply patterns to detect injections
        for (Pattern pattern : INJECTION_PATTERNS) {
            Matcher matcher = pattern.matcher(query);
            if (matcher.find()) {
                // SQL injection detected
                handleInjectionAttempt(query, pattern.pattern(), source, ip);
                return true;
            }
        }
        
        // Check for additional signs of SQL injection using the SqlSanitizer
        if (SqlSanitizer.isSqlInjectionAttempt(query)) {
            handleInjectionAttempt(query, "Advanced detection", source, ip);
            return true;
        }
        
        return false;
    }

    /**
     * Handle a detected SQL injection attempt
     *
     * @param query The SQL query containing the injection
     * @param pattern The pattern that matched
     * @param source The source of the query
     * @param ip The IP address (can be null)
     */
    private void handleInjectionAttempt(String query, String pattern, String source, String ip) {
        // Log the attempt
        if (logAttempts) {
            LogUtil.severe("SQL INJECTION ATTEMPT DETECTED!");
            LogUtil.severe("Query: " + query);
            LogUtil.severe("Pattern: " + pattern);
            LogUtil.severe("Source: " + source);
            if (ip != null) {
                LogUtil.severe("IP: " + ip);
            }
        }
        
        // Record the attempt
        InjectionAttempt attempt = new InjectionAttempt(
                System.currentTimeMillis(),
                query,
                pattern,
                source,
                ip
        );
        
        synchronized (recentAttempts) {
            recentAttempts.add(attempt);
            if (recentAttempts.size() > MAX_RECENT_ATTEMPTS) {
                recentAttempts.remove(0);
            }
        }
        
        // Track attempts by IP
        if (ip != null) {
            synchronized (attemptsByIp) {
                attemptsByIp.put(ip, attemptsByIp.getOrDefault(ip, 0) + 1);
            }
        }
        
        // Notify admins if enabled
        if (notifyAdmins) {
            plugin.getServer().getOnlinePlayers().stream()
                    .filter(player -> player.hasPermission("sqlbridge.admin"))
                    .forEach(admin -> admin.sendMessage(
                            "§c[SQL-Bridge] SQL Injection Attempt Detected from " + 
                            (source != null ? source : "unknown source") +
                            (ip != null ? " (" + ip + ")" : "")
                    ));
        }
    }

    /**
     * Get a list of recent injection attempts
     *
     * @return List of recent injection attempts
     */
    public List<InjectionAttempt> getRecentAttempts() {
        synchronized (recentAttempts) {
            return new ArrayList<>(recentAttempts);
        }
    }

    /**
     * Check if queries should be blocked when injection is detected
     *
     * @return True if queries should be blocked
     */
    public boolean shouldBlockQueries() {
        return enabled && blockQueries;
    }

    /**
     * Clear all recorded injection attempts
     */
    public void clearAttempts() {
        synchronized (recentAttempts) {
            recentAttempts.clear();
        }
        
        synchronized (attemptsByIp) {
            attemptsByIp.clear();
        }
    }

    /**
     * Get the number of attempts from a specific IP
     *
     * @param ip The IP address
     * @return The number of attempts
     */
    public int getAttemptsFromIp(String ip) {
        if (ip == null) {
            return 0;
        }
        
        synchronized (attemptsByIp) {
            return attemptsByIp.getOrDefault(ip, 0);
        }
    }

    /**
     * Inner class to represent an injection attempt
     */
    public static class InjectionAttempt {
        private final long timestamp;
        private final String query;
        private final String pattern;
        private final String source;
        private final String ip;

        public InjectionAttempt(long timestamp, String query, String pattern, String source, String ip) {
            this.timestamp = timestamp;
            this.query = query;
            this.pattern = pattern;
            this.source = source;
            this.ip = ip;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getQuery() {
            return query;
        }

        public String getPattern() {
            return pattern;
        }

        public String getSource() {
            return source;
        }

        public String getIp() {
            return ip;
        }
    }
}