package com.example.exampleplugin.database;

import com.minecraft.core.utils.LogUtil;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.reflect.Method;

/**
 * Manager class for SQL-Bridge database operations
 * Uses reflection to avoid hard dependencies on SQL-Bridge
 */
public class DatabaseManager {
    private final Plugin plugin;
    private Object sqlBridge;
    private Object database;
    private boolean initialized = false;
    
    /**
     * Create a new DatabaseManager
     * 
     * @param plugin The plugin instance
     */
    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        initialize();
    }
    
    /**
     * Initialize the database manager
     */
    private void initialize() {
        try {
            // Check if SQL-Bridge is available
            Plugin sqlBridgePlugin = Bukkit.getPluginManager().getPlugin("SQLBridge");
            if (sqlBridgePlugin == null) {
                LogUtil.warning("SQL-Bridge not found! Database functionality will be disabled.");
                return;
            }
            
            // Try to get the SQL-Bridge instance using reflection
            Class<?> sqlBridgeClass = Class.forName("com.minecraft.sqlbridge.SqlBridgePlugin");
            if (!sqlBridgeClass.isInstance(sqlBridgePlugin)) {
                LogUtil.warning("Found plugin named SQLBridge but it's not the expected type!");
                return;
            }
            
            // Get the SQL-Bridge instance
            sqlBridge = sqlBridgePlugin;
            
            // Use reflection to get the database
            Method getDatabaseMethod = sqlBridgeClass.getMethod("getDatabase");
            database = getDatabaseMethod.invoke(sqlBridge);
            
            if (database == null) {
                LogUtil.warning("Failed to get database from SQL-Bridge!");
                return;
            }
            
            // Create required tables
            createTables();
            
            initialized = true;
            LogUtil.info("DatabaseManager initialized successfully!");
        } catch (ClassNotFoundException e) {
            LogUtil.warning("SQL-Bridge API classes not found in classpath.");
        } catch (Exception e) {
            LogUtil.severe("Error initializing DatabaseManager: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create required tables
     */
    private void createTables() {
        try {
            if (database == null) return;
            
            // Get the update method
            Method updateMethod = database.getClass().getMethod("update", String.class, Object[].class);
            
            // Create example_players table
            updateMethod.invoke(database, 
                "CREATE TABLE IF NOT EXISTS example_players (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(36) NOT NULL UNIQUE, " +
                "level INT NOT NULL DEFAULT 1, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")",
                new Object[0]
            );
            
            // Create example_stats table
            updateMethod.invoke(database,
                "CREATE TABLE IF NOT EXISTS example_stats (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "player_id INT NOT NULL, " +
                "stat_name VARCHAR(64) NOT NULL, " +
                "stat_value INT NOT NULL DEFAULT 0, " +
                "FOREIGN KEY (player_id) REFERENCES example_players(id) ON DELETE CASCADE, " +
                "UNIQUE KEY player_stat (player_id, stat_name)" +
                ")",
                new Object[0]
            );
            
            LogUtil.debug("Database tables created successfully");
        } catch (Exception e) {
            LogUtil.severe("Error creating database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if the database manager is initialized
     * 
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get player data by name
     * 
     * @param playerName Player name
     * @return PlayerData or null if not found
     */
    public PlayerData getPlayerData(String playerName) {
        if (!initialized || database == null) {
            return null;
        }
        
        try {
            // Use reflection to call queryFirst method
            Method queryFirstMethod = database.getClass().getMethod("queryFirst", 
                String.class, Class.forName("com.minecraft.sqlbridge.api.RowMapper"), Object[].class);
            
            // Create a RowMapper using reflection
            Class<?> rowMapperClass = Class.forName("com.minecraft.sqlbridge.api.RowMapper");
            Object rowMapper = java.lang.reflect.Proxy.newProxyInstance(
                rowMapperClass.getClassLoader(),
                new Class<?>[] { rowMapperClass },
                (proxy, method, args) -> {
                    if (method.getName().equals("mapRow")) {
                        Object row = args[0];
                        
                        // Get row methods
                        Method getIntMethod = row.getClass().getMethod("getInt", String.class);
                        Method getStringMethod = row.getClass().getMethod("getString", String.class);
                        
                        // Create PlayerData
                        PlayerData data = new PlayerData();
                        data.setId((Integer) getIntMethod.invoke(row, "id"));
                        data.setName((String) getStringMethod.invoke(row, "name"));
                        data.setLevel((Integer) getIntMethod.invoke(row, "level"));
                        return data;
                    }
                    return null;
                }
            );
            
            // Execute query
            Object result = queryFirstMethod.invoke(database, 
                "SELECT * FROM example_players WHERE name = ?", 
                rowMapper, 
                new Object[] { playerName }
            );
            
            return (PlayerData) result;
        } catch (Exception e) {
            LogUtil.severe("Error getting player data: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create or update player data
     * 
     * @param playerName Player name
     * @param level Player level
     * @return true if successful
     */
    public boolean savePlayerData(String playerName, int level) {
        if (!initialized || database == null) {
            return false;
        }
        
        try {
            // Get the update method
            Method updateMethod = database.getClass().getMethod("update", String.class, Object[].class);
            
            // Check if player exists
            PlayerData existingPlayer = getPlayerData(playerName);
            
            if (existingPlayer == null) {
                // Insert new player
                updateMethod.invoke(database,
                    "INSERT INTO example_players (name, level) VALUES (?, ?)",
                    new Object[] { playerName, level }
                );
                LogUtil.debug("Created new player data for " + playerName);
            } else {
                // Update existing player
                updateMethod.invoke(database,
                    "UPDATE example_players SET level = ? WHERE name = ?",
                    new Object[] { level, playerName }
                );
                LogUtil.debug("Updated player data for " + playerName);
            }
            
            return true;
        } catch (Exception e) {
            LogUtil.severe("Error saving player data: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get player statistics
     * 
     * @param playerName Player name
     * @return Map of stat name to stat value
     */
    public Map<String, Integer> getPlayerStats(String playerName) {
        Map<String, Integer> stats = new HashMap<>();
        
        if (!initialized || database == null) {
            return stats;
        }
        
        try {
            // Get player ID
            PlayerData player = getPlayerData(playerName);
            if (player == null) {
                return stats;
            }
            
            // Use reflection to call query method
            Method queryMethod = database.getClass().getMethod("query", 
                String.class, Class.forName("com.minecraft.sqlbridge.api.RowMapper"), Object[].class);
            
            // Create a RowMapper using reflection
            Class<?> rowMapperClass = Class.forName("com.minecraft.sqlbridge.api.RowMapper");
            Object rowMapper = java.lang.reflect.Proxy.newProxyInstance(
                rowMapperClass.getClassLoader(),
                new Class<?>[] { rowMapperClass },
                (proxy, method, args) -> {
                    if (method.getName().equals("mapRow")) {
                        Object row = args[0];
                        
                        // Get row methods
                        Method getStringMethod = row.getClass().getMethod("getString", String.class);
                        Method getIntMethod = row.getClass().getMethod("getInt", String.class);
                        
                        // Create result map
                        Map<String, Object> result = new HashMap<>();
                        result.put("name", getStringMethod.invoke(row, "stat_name"));
                        result.put("value", getIntMethod.invoke(row, "stat_value"));
                        return result;
                    }
                    return null;
                }
            );
            
            // Execute query
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) queryMethod.invoke(database,
                "SELECT stat_name, stat_value FROM example_stats WHERE player_id = ?",
                rowMapper,
                new Object[] { player.getId() }
            );
            
            // Convert to map
            for (Map<String, Object> result : results) {
                stats.put((String) result.get("name"), (Integer) result.get("value"));
            }
            
            return stats;
        } catch (Exception e) {
            LogUtil.severe("Error getting player stats: " + e.getMessage());
            return stats;
        }
    }
    
    /**
     * Set a player statistic
     * 
     * @param playerName Player name
     * @param statName Stat name
     * @param statValue Stat value
     * @return true if successful
     */
    public boolean setPlayerStat(String playerName, String statName, int statValue) {
        if (!initialized || database == null) {
            return false;
        }
        
        try {
            // Get the update method
            Method updateMethod = database.getClass().getMethod("update", String.class, Object[].class);
            
            // Get player ID
            PlayerData player = getPlayerData(playerName);
            if (player == null) {
                // Create player
                savePlayerData(playerName, 1);
                player = getPlayerData(playerName);
                if (player == null) {
                    return false;
                }
            }
            
            // Upsert stat - using a simple INSERT followed by UPDATE if needed to avoid DB-specific syntax
            try {
                // Try to insert first
                updateMethod.invoke(database,
                    "INSERT INTO example_stats (player_id, stat_name, stat_value) VALUES (?, ?, ?)",
                    new Object[] { player.getId(), statName, statValue }
                );
            } catch (Exception e) {
                // If insert fails (due to duplicate), try update
                updateMethod.invoke(database,
                    "UPDATE example_stats SET stat_value = ? WHERE player_id = ? AND stat_name = ?",
                    new Object[] { statValue, player.getId(), statName }
                );
            }
            
            return true;
        } catch (Exception e) {
            LogUtil.severe("Error setting player stat: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get top players by level
     * 
     * @param limit Maximum number of players to return
     * @return List of PlayerData objects
     */
    public List<PlayerData> getTopPlayers(int limit) {
        List<PlayerData> players = new ArrayList<>();
        
        if (!initialized || database == null) {
            return players;
        }
        
        try {
            // Use reflection to call query method
            Method queryMethod = database.getClass().getMethod("query", 
                String.class, Class.forName("com.minecraft.sqlbridge.api.RowMapper"), Object[].class);
            
            // Create a RowMapper using reflection
            Class<?> rowMapperClass = Class.forName("com.minecraft.sqlbridge.api.RowMapper");
            Object rowMapper = java.lang.reflect.Proxy.newProxyInstance(
                rowMapperClass.getClassLoader(),
                new Class<?>[] { rowMapperClass },
                (proxy, method, args) -> {
                    if (method.getName().equals("mapRow")) {
                        Object row = args[0];
                        
                        // Get row methods
                        Method getIntMethod = row.getClass().getMethod("getInt", String.class);
                        Method getStringMethod = row.getClass().getMethod("getString", String.class);
                        
                        // Create PlayerData
                        PlayerData data = new PlayerData();
                        data.setId((Integer) getIntMethod.invoke(row, "id"));
                        data.setName((String) getStringMethod.invoke(row, "name"));
                        data.setLevel((Integer) getIntMethod.invoke(row, "level"));
                        return data;
                    }
                    return null;
                }
            );
            
            // Execute query
            @SuppressWarnings("unchecked")
            List<PlayerData> results = (List<PlayerData>) queryMethod.invoke(database,
                "SELECT * FROM example_players ORDER BY level DESC LIMIT ?",
                rowMapper,
                new Object[] { limit }
            );
            
            return results != null ? results : players;
        } catch (Exception e) {
            LogUtil.severe("Error getting top players: " + e.getMessage());
            return players;
        }
    }
    
    /**
     * Execute a transaction example
     * 
     * @param playerName Player name
     * @param levelIncrease Level increase
     * @param stats Map of stat name to stat value
     * @return true if successful
     */
    public boolean executeTransaction(String playerName, int levelIncrease, Map<String, Integer> stats) {
        if (!initialized || database == null) {
            return false;
        }
        
        try {
            // Get player data
            PlayerData player = getPlayerData(playerName);
            if (player == null) {
                // Create player
                savePlayerData(playerName, 1);
                player = getPlayerData(playerName);
                if (player == null) {
                    return false;
                }
            }
            
            // Get the update method
            Method updateMethod = database.getClass().getMethod("update", String.class, Object[].class);
            
            // Update player level
            int newLevel = player.getLevel() + levelIncrease;
            updateMethod.invoke(database,
                "UPDATE example_players SET level = ? WHERE id = ?",
                new Object[] { newLevel, player.getId() }
            );
            
            // Update stats
            for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                try {
                    // Try insert first
                    updateMethod.invoke(database,
                        "INSERT INTO example_stats (player_id, stat_name, stat_value) VALUES (?, ?, ?)",
                        new Object[] { player.getId(), entry.getKey(), entry.getValue() }
                    );
                } catch (Exception e) {
                    // If insert fails, try update
                    updateMethod.invoke(database,
                        "UPDATE example_stats SET stat_value = ? WHERE player_id = ? AND stat_name = ?",
                        new Object[] { entry.getValue(), player.getId(), entry.getKey() }
                    );
                }
            }
            
            return true;
        } catch (Exception e) {
            LogUtil.severe("Error executing transaction: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get database metrics
     * 
     * @return List of metrics
     */
    public List<String> getDatabaseMetrics() {
        List<String> metrics = new ArrayList<>();
        
        if (!initialized || sqlBridge == null) {
            metrics.add("Database not initialized");
            return metrics;
        }
        
        try {
            metrics.add("Database connection: ACTIVE");
            metrics.add("Database Status: OK");
            metrics.add("Connection Pool: Active");
            
            // Try to get more detailed metrics if available
            try {
                Method getMetricsMethod = sqlBridge.getClass().getMethod("getMetrics");
                Object metricsObj = getMetricsMethod.invoke(sqlBridge);
                
                if (metricsObj != null) {
                    // Try to get common metrics
                    Method[] methods = metricsObj.getClass().getMethods();
                    for (Method method : methods) {
                        if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
                            try {
                                Object value = method.invoke(metricsObj);
                                String metricName = method.getName().substring(3); // Remove "get"
                                metrics.add(metricName + ": " + value);
                            } catch (Exception e) {
                                // Ignore method call errors
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore metrics errors
                metrics.add("Detailed metrics unavailable");
            }
            
            return metrics;
        } catch (Exception e) {
            LogUtil.severe("Error getting database metrics: " + e.getMessage());
            metrics.add("Error getting database metrics: " + e.getMessage());
            return metrics;
        }
    }
}