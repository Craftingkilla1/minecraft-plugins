# SQL-Bridge

A high-performance database connectivity plugin for Minecraft servers that provides a robust, user-friendly API for SQL database operations with comprehensive monitoring and error handling.

![SQL-Bridge Banner](https://i.imgur.com/placeholder.png)

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Configuration](#configuration)
- [Quick Start Guide](#quick-start-guide)
- [Core Concepts](#core-concepts)
- [API Reference](#api-reference)
  - [DatabaseService](#databaseservice)
  - [Database](#database)
  - [Query Builders](#query-builders)
  - [Result Mapping](#result-mapping)
  - [Transactions](#transactions)
  - [Migrations](#migrations)
  - [Error Handling](#error-handling)
  - [Callback Support](#callback-support)
- [Advanced Features](#advanced-features)
  - [Connection Pooling](#connection-pooling)
  - [Performance Monitoring](#performance-monitoring)
  - [BungeeSupport](#bungeesupport)
  - [Security Features](#security-features)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)
- [Example Projects](#example-projects)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Multi-Database Support**: Connect to MySQL, PostgreSQL, SQLite, and H2 databases
- **Connection Pooling**: Efficient connection management with HikariCP
- **Fluent Query API**: Build SQL queries with intuitive Java methods
- **Type-Safe Results**: Map query results to Java objects
- **Flexible Execution**: Synchronous, asynchronous, or callback-based operations
- **Transaction Support**: Simplified transaction management
- **Migration System**: Database schema versioning and migrations
- **Comprehensive Error Handling**: Robust exception management
- **Performance Monitoring**: Real-time metrics collection
- **Security Features**: SQL injection protection and query validation
- **BungeeSupport**: Share database connections across a network of servers
- **Debug Tools**: Extensive logging and error tracking

## Installation

### Requirements

- Java 8 or higher
- Bukkit/Spigot/Paper server 1.12.2 or higher
- Core-Utils plugin (dependency)

### Steps

1. Download the latest release from the [releases page](https://github.com/yourusername/sql-bridge/releases)
2. Place `SQL-Bridge.jar` and `Core-Utils.jar` in your server's `plugins` directory
3. Start or restart your server
4. Configure SQL-Bridge in `plugins/SQL-Bridge/config.yml`
5. Restart your server again to apply the configuration

### Adding SQL-Bridge to Your Plugin

Add SQL-Bridge as a dependency in your `plugin.yml`:

```yaml
depend: [Core-Utils, SQL-Bridge]
```

Or as a soft dependency if your plugin can function without it:

```yaml
softdepend: [Core-Utils, SQL-Bridge]
```

## Configuration

SQL-Bridge creates a default configuration file at `plugins/SQL-Bridge/config.yml`. Below is an explanation of key configuration options:

```yaml
# Debug mode - enables additional logging
debug: false

# Database configuration
database:
  # Database type: MYSQL, SQLITE, POSTGRESQL, H2
  type: sqlite
  
  # MySQL configuration (used when type is MYSQL)
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: root
    password: password
    # Whether to create the database if it doesn't exist
    auto-create-database: true
    
    # Connection pool settings
    pool:
      # Maximum number of connections in the pool
      maximum-pool-size: 10
      # Minimum idle connections
      minimum-idle: 5
      # Maximum lifetime of a connection in milliseconds
      maximum-lifetime: 1800000
      # Connection timeout in milliseconds
      connection-timeout: 5000

  # SQLite configuration (used when type is SQLITE)
  sqlite:
    # Database file (relative to plugin folder)
    file: database.db
    # Whether to use WAL mode for better performance
    wal-mode: true

# Performance monitoring
monitoring:
  # Enable monitoring
  enabled: true
  # Log slow queries that take longer than this many milliseconds
  slow-query-threshold: 1000
  # Collect metrics data
  collect-metrics: true
  # Metrics collection interval in seconds
  metrics-interval: 300

# Security configuration
security:
  # Enable SQL injection detection
  sql-injection-detection: true
  # Log dangerous operations
  log-dangerous-operations: true
  # Maximum query length
  max-query-length: 10000

# BungeeSupport configuration
bungee:
  # Whether to enable BungeeSupport for multi-server setups
  enabled: false
  # Use shared database across BungeeCord network
  shared-database: true
```

### Database Choice Considerations

- **MySQL**: High-performance, supports multiple concurrent connections, requires external database server
- **PostgreSQL**: Advanced features, good for complex queries, requires external database server
- **SQLite**: Simple file-based database, good for small plugins, limited concurrent access
- **H2**: High-performance embedded database, good balance between SQLite and MySQL

## Quick Start Guide

Here's how to quickly integrate SQL-Bridge into your plugin:

```java
public class YourPlugin extends JavaPlugin {
    private DatabaseService databaseService;
    private Database database;
    
    @Override
    public void onEnable() {
        // Get Core-Utils service registry
        if (!setupDatabaseService()) {
            getLogger().severe("Failed to connect to database, disabling plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize your database schema
        if (!initDatabase()) {
            getLogger().severe("Failed to initialize database, disabling plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        getLogger().info("Database connection established successfully!");
    }
    
    private boolean setupDatabaseService() {
        // Get the database service from Core-Utils
        databaseService = ServiceRegistry.getService(DatabaseService.class);
        if (databaseService == null) {
            getLogger().severe("Could not find DatabaseService - is SQL-Bridge enabled?");
            return false;
        }
        
        // Get a database connection for your plugin
        database = databaseService.getDatabaseForPlugin(this);
        return database != null;
    }
    
    private boolean initDatabase() {
        // Using the initializeDatabase convenience method
        return databaseService.initializeDatabase(this,
            "CREATE TABLE IF NOT EXISTS players (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "name VARCHAR(100) NOT NULL, " +
            "level INT NOT NULL DEFAULT 1, " +
            "experience INT NOT NULL DEFAULT 0)"
        );
    }
    
    @Override
    public void onDisable() {
        // Clean up happens automatically with SQL-Bridge
    }
}
```

## Core Concepts

### 1. Service-Based Architecture

SQL-Bridge uses Core-Utils' service registry to provide database functionality:

```java
DatabaseService databaseService = ServiceRegistry.getService(DatabaseService.class);
```

### 2. Database Connections

Each plugin can have its own database or connection:

```java
// Main database for the server
Database mainDatabase = databaseService.getDatabase();

// Database specific to your plugin
Database pluginDatabase = databaseService.getDatabaseForPlugin(yourPlugin);

// Named database for special purposes
Database customDatabase = databaseService.getDatabase("custom");

// Shared database across a BungeeCord network
databaseService.getSharedDatabase().ifPresent(sharedDatabase -> {
    // Use shared database
});
```

### 3. Execution Patterns

SQL-Bridge supports multiple execution patterns:

**Synchronous (blocking):**
```java
try {
    List<Player> players = database.query("SELECT * FROM players", playerMapper);
} catch (SQLException e) {
    // Handle error
}
```

**Asynchronous with CompletableFuture:**
```java
database.queryAsync("SELECT * FROM players", playerMapper)
    .thenAccept(players -> {
        // Process players
    })
    .exceptionally(e -> {
        // Handle error
        return null;
    });
```

**Asynchronous with Callbacks:**
```java
database.queryWithCallback("SELECT * FROM players", playerMapper,
    new DatabaseResultCallback<List<Player>>() {
        @Override
        public void onSuccess(List<Player> players) {
            // Process players
        }
        
        @Override
        public void onError(Exception e) {
            // Handle error
        }
    });
```

**Safe Methods (automatic error handling):**
```java
List<Player> players = database.querySafe("SELECT * FROM players", playerMapper, getLogger());
```

### 4. Builder Pattern

SQL-Bridge uses a fluent builder pattern for constructing queries:

```java
List<Player> players = database.select()
    .columns("id", "name", "level")
    .from("players")
    .where("level > ?", 10)
    .orderBy("level DESC")
    .limit(10)
    .executeQuery(playerMapper);
```

## API Reference

### DatabaseService

The entry point for all database operations.

#### Key Methods

```java
// Get the main database connection
Database getDatabase();

// Get a database for a specific plugin
Database getDatabaseForPlugin(Plugin plugin);

// Get a named database connection
Database getDatabase(String name);

// Check if a database exists
boolean databaseExists(String name);

// Get a shared database for BungeeSupport
Optional<Database> getSharedDatabase();

// Register migrations
void registerMigrations(Plugin plugin, List<Migration> migrations);

// Run migrations
int runMigrations(Plugin plugin);
int runMigrationsSafe(Plugin plugin);

// Run migrations asynchronously
CompletableFuture<Integer> runMigrationsAsync(Plugin plugin);

// Get current schema version
int getCurrentSchemaVersion(Plugin plugin);

// Initialize database (convenience method)
boolean initializeDatabase(Plugin plugin, String... createTableStatements);

// Get statistics
Map<String, Object> getStatistics();

// Close all connections
void shutdown();
```

### Database

The main interface for executing database operations.

#### Query Methods

```java
// Basic query methods
<T> List<T> query(String sql, ResultMapper<T> mapper, Object... params) throws SQLException;
<T> Optional<T> queryFirst(String sql, ResultMapper<T> mapper, Object... params) throws SQLException;

// Asynchronous query methods
<T> CompletableFuture<List<T>> queryAsync(String sql, ResultMapper<T> mapper, Object... params);
<T> CompletableFuture<Optional<T>> queryFirstAsync(String sql, ResultMapper<T> mapper, Object... params);

// Safe query methods (no exceptions)
<T> List<T> querySafe(String sql, ResultMapper<T> mapper, Logger logger, Object... params);
<T> Optional<T> queryFirstSafe(String sql, ResultMapper<T> mapper, Logger logger, Object... params);

// Callback-based query methods
<T> void queryWithCallback(String sql, ResultMapper<T> mapper, DatabaseResultCallback<List<T>> callback, Object... params);
<T> void queryFirstWithCallback(String sql, ResultMapper<T> mapper, DatabaseResultCallback<Optional<T>> callback, Object... params);

// Execute query with consumer
void executeQuery(String sql, Consumer<ResultRow> resultConsumer, Object... params) throws SQLException;
```

#### Update Methods

```java
// Basic update methods
int update(String sql, Object... params) throws SQLException;

// Asynchronous update methods
CompletableFuture<Integer> updateAsync(String sql, Object... params);

// Safe update methods (no exceptions)
int updateSafe(String sql, Logger logger, Object... params);

// Callback-based update methods
void updateWithCallback(String sql, DatabaseResultCallback<Integer> callback, Object... params);

// Batch update methods
int[] batchUpdate(String sql, List<Object[]> parameterSets) throws SQLException;
CompletableFuture<int[]> batchUpdateAsync(String sql, List<Object[]> parameterSets);
int[] batchUpdateSafe(String sql, List<Object[]> parameterSets, Logger logger);
void batchUpdateWithCallback(String sql, List<Object[]> parameterSets, DatabaseResultCallback<int[]> callback);
```

#### Schema Methods

```java
// Check if a table exists
boolean tableExists(String tableName) throws SQLException;
boolean tableExistsSafe(String tableName, Logger logger);

// Create a table if it doesn't exist
boolean createTableIfNotExists(String tableName, String createTableSQL) throws SQLException;
boolean createTableIfNotExistsSafe(String tableName, String createTableSQL, Logger logger);
```

#### Connection Methods

```java
// Get a raw JDBC connection
Connection getConnection() throws SQLException;

// Check if the connection is valid
boolean isConnectionValid();

// Get database statistics
Map<String, Object> getStatistics();
```

#### Query Builder Methods

```java
// Create query builders
SelectBuilder select();
InsertBuilder insertInto(String table);
UpdateBuilder update(String table);
DeleteBuilder deleteFrom(String table);
QueryBuilder createQuery();
```

#### Transaction Methods

```java
// Execute a transaction
<T> T executeTransaction(Transaction<T> transactionFunction) throws SQLException;
<T> CompletableFuture<T> executeTransactionAsync(Transaction<T> transactionFunction);
<T> T executeTransactionSafe(Transaction<T> transactionFunction, Logger logger);
```

### Query Builders

SQL-Bridge provides fluent builders for different query types.

#### SelectBuilder

```java
SelectBuilder select = database.select();
select.columns("id", "name", "level") // Select columns
      .from("players")                // From table
      .where("level > ?", 10)         // Where clause with parameters
      .and("guild = ?", "Knights")    // Additional AND condition
      .or("rank = ?", "Elite")        // Additional OR condition
      .innerJoin("guilds", "players.guild_id = guilds.id") // Join
      .groupBy("level")               // Group by
      .having("COUNT(*) > ?", 5)      // Having clause
      .orderBy("level DESC")          // Order by
      .limit(10)                      // Limit results
      .offset(20)                     // Offset (pagination)
      .forUpdate();                   // For update lock
      
// Execute the query
List<Player> players = select.executeQuery(playerMapper);
```

#### InsertBuilder

```java
InsertBuilder insert = database.insertInto("players");
insert.columns("name", "level", "experience") // Columns to insert
      .values("PlayerOne", 1, 0)              // Values for a row
      .addRow("PlayerTwo", 2, 100)            // Add additional row
      .onDuplicateKeyUpdate("level", 1)       // On duplicate key update
      .returning("id");                       // Return generated keys
      
// Execute the insert
int rowsAffected = insert.executeUpdate();

// Insert using a map
Map<String, Object> playerData = new HashMap<>();
playerData.put("name", "PlayerThree");
playerData.put("level", 3);
playerData.put("experience", 200);

insert.columnValues(playerData)
      .executeUpdate();
```

#### UpdateBuilder

```java
UpdateBuilder update = database.update("players");
update.set("level", 5)                        // Set a column value
      .set("experience", 500)                 // Set another column
      .where("id = ?", 123)                   // Where clause
      .limit(1);                              // Limit the update
      
// Execute the update
int rowsAffected = update.executeUpdate();

// Update using a map
Map<String, Object> updateValues = new HashMap<>();
updateValues.put("level", 6);
updateValues.put("experience", 600);

update.set(updateValues)
      .where("name = ?", "PlayerOne")
      .executeUpdate();
```

#### DeleteBuilder

```java
DeleteBuilder delete = database.deleteFrom("players");
delete.where("level < ?", 5)                  // Where clause
      .limit(10)                              // Limit the delete
      .orderBy("last_login ASC");             // Order by (for MySQL)
      
// Execute the delete
int rowsAffected = delete.executeUpdate();
```

### Result Mapping

#### ResultMapper Interface

```java
// Define how to map database rows to objects
ResultMapper<Player> playerMapper = row -> {
    Player player = new Player();
    player.setId(row.getInt("id"));
    player.setName(row.getString("name"));
    player.setLevel(row.getInt("level"));
    player.setExperience(row.getInt("experience"));
    return player;
};

// Use the mapper in queries
List<Player> players = database.query("SELECT * FROM players", playerMapper);
```

#### ResultRow Interface

```java
// Access data from a database row
public interface ResultRow {
    String getString(String columnName) throws SQLException;
    int getInt(String columnName) throws SQLException;
    long getLong(String columnName) throws SQLException;
    double getDouble(String columnName) throws SQLException;
    boolean getBoolean(String columnName) throws SQLException;
    byte[] getBytes(String columnName) throws SQLException;
    java.sql.Date getDate(String columnName) throws SQLException;
    java.sql.Timestamp getTimestamp(String columnName) throws SQLException;
    Object getObject(String columnName) throws SQLException;
    boolean isNull(String columnName) throws SQLException;
}
```

### Transactions

Transactions allow you to execute multiple operations as a single unit:

```java
// Using transactions with lambda
int newPlayerId = database.executeTransaction(connection -> {
    // Insert player
    try (PreparedStatement stmt = connection.prepareStatement(
            "INSERT INTO players (name, level) VALUES (?, ?)",
            Statement.RETURN_GENERATED_KEYS)) {
        stmt.setString(1, "NewPlayer");
        stmt.setInt(2, 1);
        stmt.executeUpdate();
        
        // Get generated ID
        try (ResultSet keys = stmt.getGeneratedKeys()) {
            if (keys.next()) {
                int playerId = keys.getInt(1);
                
                // Insert initial inventory items
                try (PreparedStatement itemStmt = connection.prepareStatement(
                        "INSERT INTO items (player_id, name, quantity) VALUES (?, ?, ?)")) {
                    // Add starter sword
                    itemStmt.setInt(1, playerId);
                    itemStmt.setString(2, "Wooden Sword");
                    itemStmt.setInt(3, 1);
                    itemStmt.executeUpdate();
                    
                    // Add starter food
                    itemStmt.setInt(1, playerId);
                    itemStmt.setString(2, "Apple");
                    itemStmt.setInt(3, 5);
                    itemStmt.executeUpdate();
                }
                
                return playerId;
            }
        }
    }
    
    return -1; // Failed to get player ID
});

// Asynchronous transactions
database.executeTransactionAsync(connection -> {
    // Transaction code here
    return true;
}).thenAccept(result -> {
    // Process result
});

// Safe transactions (no exceptions)
Boolean result = database.executeTransactionSafe(connection -> {
    // Transaction code here
    return true;
}, getLogger());
```

### Migrations

Migrations allow you to manage database schema versions:

```java
// Create migration classes
public class CreatePlayersTable implements Migration {
    @Override
    public int getVersion() {
        return 1; // Migration version number
    }
    
    @Override
    public String getDescription() {
        return "Create players table";
    }
    
    @Override
    public void migrate(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE players (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "name VARCHAR(100) NOT NULL, " +
                        "level INT NOT NULL DEFAULT 1)");
        }
    }
    
    @Override
    public void rollback(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS players");
        }
    }
}

public class AddExperienceColumn implements Migration {
    @Override
    public int getVersion() {
        return 2; // Next version number
    }
    
    @Override
    public String getDescription() {
        return "Add experience column to players table";
    }
    
    @Override
    public void migrate(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE players " +
                        "ADD COLUMN experience INT NOT NULL DEFAULT 0");
        }
    }
    
    @Override
    public void rollback(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE players DROP COLUMN experience");
        }
    }
}

// Register migrations with SQL-Bridge
@Override
public void onEnable() {
    // ... setup database service
    
    // Register migrations
    List<Migration> migrations = new ArrayList<>();
    migrations.add(new CreatePlayersTable());
    migrations.add(new AddExperienceColumn());
    
    databaseService.registerMigrations(this, migrations);
    
    // Run migrations
    int appliedCount = databaseService.runMigrationsSafe(this);
    getLogger().info("Applied " + appliedCount + " migrations");
}
```

You can also use anonymous inner classes for simpler migrations:

```java
// Register migrations
List<Migration> migrations = new ArrayList<>();

// Add migration to create a new table
migrations.add(new Migration() {
    @Override
    public int getVersion() {
        return 1;
    }
    
    @Override
    public String getDescription() {
        return "Create leaderboard table";
    }
    
    @Override
    public void migrate(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE leaderboard (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "player_id INT NOT NULL, " +
                        "score INT NOT NULL, " +
                        "FOREIGN KEY (player_id) REFERENCES players(id))");
        }
    }
    
    @Override
    public void rollback(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS leaderboard");
        }
    }
});

// Add a second migration to add a column
migrations.add(new Migration() {
    @Override
    public int getVersion() {
        return 2;
    }
    
    @Override
    public String getDescription() {
        return "Add timestamp column to leaderboard";
    }
    
    @Override
    public void migrate(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE leaderboard " +
                        "ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        }
    }
    
    @Override
    public void rollback(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE leaderboard DROP COLUMN created_at");
        }
    }
});

// Register migrations with the database service
databaseService.registerMigrations(this, migrations);

// Run migrations
int appliedCount = databaseService.runMigrationsSafe(this);
getLogger().info("Applied " + appliedCount + " migrations");
```

Note: To use the Migration interface, you need to import it from the API package:

```java
import com.minecraft.sqlbridge.api.migration.Migration;
```

### Error Handling

SQL-Bridge provides multiple approaches to error handling:

#### 1. Traditional Try-Catch

```java
try {
    List<Player> players = database.query("SELECT * FROM players", playerMapper);
    // Process players
} catch (SQLException e) {
    getLogger().severe("Database error: " + e.getMessage());
    e.printStackTrace();
}
```

#### 2. Safe Methods

```java
// No try-catch needed, errors are logged automatically
List<Player> players = database.querySafe("SELECT * FROM players", playerMapper, getLogger());
```

#### 3. Asynchronous Error Handling

```java
// With CompletableFuture
database.queryAsync("SELECT * FROM players", playerMapper)
    .thenAccept(players -> {
        // Process players
    })
    .exceptionally(e -> {
        getLogger().severe("Async error: " + e.getMessage());
        return null;
    });

// With callbacks
database.queryWithCallback("SELECT * FROM players", playerMapper,
    new DatabaseResultCallback<List<Player>>() {
        @Override
        public void onSuccess(List<Player> players) {
            // Process players
        }
        
        @Override
        public void onError(Exception e) {
            getLogger().severe("Callback error: " + e.getMessage());
        }
    });
```

### Callback Support

SQL-Bridge provides callback interfaces for asynchronous operations:

#### DatabaseCallback

```java
// For operations without results
database.updateWithCallback("UPDATE players SET level = level + 1",
    new DatabaseCallback() {
        @Override
        public void onSuccess() {
            // Operation completed successfully
        }
        
        @Override
        public void onError(Exception e) {
            // Handle error
        }
    });
```

#### DatabaseResultCallback

```java
// For operations with results
database.queryWithCallback("SELECT * FROM players", playerMapper,
    new DatabaseResultCallback<List<Player>>() {
        @Override
        public void onSuccess(List<Player> players) {
            // Process players
        }
        
        @Override
        public void onError(Exception e) {
            // Handle error
        }
    });
```

## Advanced Features

### Connection Pooling

SQL-Bridge uses HikariCP for efficient connection pooling. You can configure the connection pool in the `config.yml` file:

```yaml
database:
  mysql:
    pool:
      maximum-pool-size: 10
      minimum-idle: 5
      maximum-lifetime: 1800000
      connection-timeout: 5000
```

Consider the following when configuring your connection pool:

- **maximum-pool-size**: Should be proportional to the expected concurrent database operations
- **minimum-idle**: Keep some connections warm to avoid startup latency
- **connection-timeout**: How long to wait for a connection before timing out
- **maximum-lifetime**: How long a connection can exist before being recycled

### Performance Monitoring

SQL-Bridge includes performance monitoring features:

```java
// Get database statistics
Map<String, Object> stats = database.getStatistics();

// Log statistics
getLogger().info("Total queries: " + stats.get("totalQueries"));
getLogger().info("Average query time: " + stats.get("averageQueryTime") + "ms");
getLogger().info("Slow queries: " + stats.get("slowQueries"));
```

You can also enable detailed monitoring in the `config.yml`:

```yaml
monitoring:
  enabled: true
  slow-query-threshold: 1000  # Log queries that take longer than 1 second
  collect-metrics: true
  metrics-interval: 300       # Collect metrics every 5 minutes
```

### BungeeSupport

SQL-Bridge can share a database across a BungeeCord network:

```yaml
bungee:
  enabled: true
  shared-database: true
```

```java
// Check if shared database is available
databaseService.getSharedDatabase().ifPresent(sharedDb -> {
    // Use shared database for cross-server data
    try {
        sharedDb.update("INSERT INTO global_stats (player, value) VALUES (?, ?)",
                      "PlayerName", 100);
    } catch (SQLException e) {
        // Handle error
    }
});
```

For more advanced cross-server communication:

```java
// Access BungeeSupport features
if (plugin instanceof SqlBridgePlugin) {
    SqlBridgePlugin sqlBridgePlugin = (SqlBridgePlugin) plugin;
    BungeeSupport bungeeSupport = sqlBridgePlugin.getBungeeSupport();
    
    if (bungeeSupport != null) {
        SharedDatabaseManager sharedManager = bungeeSupport.getSharedDatabaseManager();
        
        // Store a value accessible by all servers
        sharedManager.set("global.player.count", "42")
            .thenAccept(v -> getLogger().info("Value set successfully"));
            
        // Get a value from any server
        sharedManager.get("global.player.count")
            .thenAccept(value -> getLogger().info("Player count: " + value));
    }
}
```

### Security Features

SQL-Bridge includes security features to prevent SQL injection:

```yaml
security:
  sql-injection-detection: true
  log-dangerous-operations: true
  max-query-length: 10000
```

The `QueryValidator` automatically validates all queries for potential SQL injection attempts. Always use parameterized queries:

```java
// SAFE: Using parameters (recommended)
database.update("UPDATE players SET level = ? WHERE name = ?", 5, playerName);

// UNSAFE: Building queries with string concatenation (never do this)
database.update("UPDATE players SET level = 5 WHERE name = '" + playerName + "'");
```

## Best Practices

### 1. Use Asynchronous Operations

Database operations can be slow. Use asynchronous methods to avoid blocking the main server thread:

```java
database.queryAsync("SELECT * FROM players", playerMapper)
    .thenAccept(players -> {
        // Process players (this will run off the main thread)
        
        // If you need to update something on the main thread:
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Update UI or game state on the main thread
        });
    });
```

### 2. Use Query Builders

Query builders are safer and more maintainable than raw SQL strings:

```java
// Better than writing raw SQL
database.select()
    .columns("id", "name", "level")
    .from("players")
    .where("level > ?", 10)
    .orderBy("level DESC")
    .executeQueryAsync(playerMapper)
    .thenAccept(players -> {
        // Process players
    });
```

### 3. Create Data Access Objects (DAOs)

Encapsulate database operations in dedicated classes:

```java
public class PlayerDao {
    private final Database database;
    private final ResultMapper<Player> playerMapper;
    
    public PlayerDao(Database database) {
        this.database = database;
        this.playerMapper = row -> {
            Player player = new Player();
            player.setId(row.getInt("id"));
            player.setName(row.getString("name"));
            player.setLevel(row.getInt("level"));
            return player;
        };
    }
    
    // CRUD operations
    public Optional<Player> findById(int id) throws SQLException {
        return database.queryFirst(
            "SELECT * FROM players WHERE id = ?", 
            playerMapper, 
            id
        );
    }
    
    public List<Player> findAll() throws SQLException {
        return database.query("SELECT * FROM players", playerMapper);
    }
    
    public int createPlayer(String name, int level) throws SQLException {
        return database.update(
            "INSERT INTO players (name, level) VALUES (?, ?)",
            name, level
        );
    }
    
    // etc.
}
```

### 4. Use Migrations for Schema Evolution

Always define migrations for database schema changes:

```java
// Register migrations on plugin startup
databaseService.registerMigrations(this, getAllMigrations());

// Then run them
databaseService.runMigrationsAsync(this)
    .thenAccept(count -> getLogger().info("Applied " + count + " migrations"));
```

### 5. Properly Close Resources

SQL-Bridge automatically manages connections, but if you get a raw connection, make sure to close it:

```java
try (Connection conn = database.getConnection()) {
    // Use connection
} // Connection automatically closed here
```

### 6. Use Safe Methods for Cleaner Code

Safe methods reduce error handling boilerplate:

```java
// No try-catch needed, much cleaner code
database.updateSafe(
    "UPDATE players SET level = ? WHERE name = ?",
    getLogger(),
    5, playerName
);
```

## Troubleshooting

### Common Issues

#### Connection Refused

```
SQL error executing query: Communications link failure
```

**Solution**: 
- Check that your database server is running and accessible
- Verify the host and port in your config.yml
- Check firewall settings

#### Authentication Failed

```
Access denied for user 'username'@'localhost'
```

**Solution**:
- Verify username and password in config.yml
- Check that the user has permissions to access the database

#### Table Not Found

```
Table 'database.table_name' doesn't exist
```

**Solution**:
- Make sure you've created the table
- Check table name spelling and case sensitivity
- Run migrations to create necessary tables

#### Performance Issues

```
Detected slow query (1500ms): SELECT * FROM players WHERE level > 10
```

**Solution**:
- Add indexes to columns used in WHERE clauses
- Optimize your queries to retrieve only needed data
- Use limits to restrict result sets

### Enabling Debug Logging

For detailed logging:

1. Set `debug: true` in the configuration
2. Restart the server

Now you'll see detailed logs in the console and in `plugins/SQL-Bridge/logs/`.

### Testing Database Connectivity

Use the built-in command to test your connection:

```
/sqlbridge test
```

This will test if the database connection is working.

## Example Projects

Check out these example projects that demonstrate SQL-Bridge usage:

- [Simple Player Database](https://github.com/example/simple-player-db) - Basic player data storage
- [Economy Plugin](https://github.com/example/economy-plugin) - Advanced usage with transactions
- [Multi-Server Stats](https://github.com/example/multi-server-stats) - BungeeSupport example

## Contributing

We welcome contributions! Here's how you can help:

1. Fork the repository
2. Create a feature branch: `git checkout -b my-feature`
3. Make your changes
4. Run tests: `./gradlew test`
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

For more information, issues, or feature requests, please visit the [GitHub repository](https://github.com/yourusername/sql-bridge).