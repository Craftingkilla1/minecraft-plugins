# SQL-Bridge User Guide

This guide provides detailed information on how to use SQL-Bridge effectively in your Minecraft plugins.

## Table of Contents

- [Getting Started](#getting-started)
  - [Setting Up SQL-Bridge](#setting-up-sql-bridge)
  - [Initializing a Database](#initializing-a-database)
- [Key Concepts](#key-concepts)
  - [ResultRow Interface](#resultrow-interface)
  - [ResultMapper](#resultmapper)
- [Basic Database Operations](#basic-database-operations)
  - [Querying Data](#querying-data)
  - [Updating Data](#updating-data)
  - [Batch Updates](#batch-updates)
- [Working with UUID Values](#working-with-uuid-values)
  - [UUID Convenience Methods](#uuid-convenience-methods)
  - [Converting UUIDs to Strings](#converting-uuids-to-strings)
- [Query Builder API](#query-builder-api)
  - [SelectBuilder](#selectbuilder)
  - [InsertBuilder](#insertbuilder)
  - [UpdateBuilder](#updatebuilder)
  - [DeleteBuilder](#deletebuilder)
- [Error Handling](#error-handling)
  - [Using try-catch](#using-try-catch)
  - [Safe Methods](#safe-methods)
- [Asynchronous Operations](#asynchronous-operations)
  - [CompletableFuture API](#completablefuture-api)
  - [Callback Methods](#callback-methods)
- [Transaction Support](#transaction-support)
  - [Basic Transactions](#basic-transactions)
  - [Using Savepoints](#using-savepoints)
- [Schema Migrations](#schema-migrations)
  - [Defining Migrations](#defining-migrations)
  - [Running Migrations](#running-migrations)
- [Data Access Objects (DAOs)](#data-access-objects-daos)
  - [DAO Pattern Structure](#dao-pattern-structure)
  - [Complete DAO Example](#complete-dao-example)
- [Performance Optimization](#performance-optimization)
  - [Connection Pooling](#connection-pooling)
  - [Batch Processing](#batch-processing)
- [Common Pitfalls and Solutions](#common-pitfalls-and-solutions)
  - [ResultSet vs ResultRow](#resultset-vs-resultrow)
  - [Method Reference Issues](#method-reference-issues)
  - [Thread Safety](#thread-safety)
- [Best Practices](#best-practices)

## Getting Started

### Setting Up SQL-Bridge

First, add SQL-Bridge as a dependency to your plugin:

```yaml
# plugin.yml
depend: [Core-Utils, SQL-Bridge]
```

### Initializing a Database

Get the database service from the Core-Utils ServiceRegistry:

```java
@Override
public void onEnable() {
    // Get the database service
    DatabaseService databaseService = ServiceRegistry.getService(DatabaseService.class);
    
    if (databaseService == null) {
        getLogger().severe("Could not find DatabaseService! Is SQL-Bridge enabled?");
        getServer().getPluginManager().disablePlugin(this);
        return;
    }
    
    // Get a database connection for your plugin
    Database database = databaseService.getDatabaseForPlugin(this);
    
    // Initialize your database tables
    initializeDatabase(database);
    
    getLogger().info("Plugin enabled with database support");
}

private void initializeDatabase(Database database) {
    try {
        // Create tables if they don't exist
        database.update(
            "CREATE TABLE IF NOT EXISTS players (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "  uuid VARCHAR(36) NOT NULL UNIQUE, " +
            "  name VARCHAR(16) NOT NULL, " +
            "  level INTEGER NOT NULL DEFAULT 1, " +
            "  experience INTEGER NOT NULL DEFAULT 0, " +
            "  last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );
        
        getLogger().info("Database tables initialized");
    } catch (SQLException e) {
        getLogger().severe("Failed to initialize database: " + e.getMessage());
    }
}
```

## Key Concepts

### ResultRow Interface

The `ResultRow` interface is a key part of SQL-Bridge. It provides methods to access data from database query results:

```java
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

### ResultMapper

The `ResultMapper` interface is used to convert database rows to Java objects:

```java
@FunctionalInterface
public interface ResultMapper<T> {
    T map(ResultRow row) throws SQLException;
}
```

Example of defining a mapper:

```java
// Define a mapper for Player objects
ResultMapper<Player> playerMapper = row -> {
    Player player = new Player();
    player.setId(row.getInt("id"));
    player.setUuid(UUID.fromString(row.getString("uuid")));
    player.setName(row.getString("name"));
    player.setLevel(row.getInt("level"));
    player.setExperience(row.getInt("experience"));
    player.setLastLogin(row.getTimestamp("last_login"));
    return player;
};
```

## Basic Database Operations

### Querying Data

To retrieve data from the database:

```java
// Get all players
List<Player> allPlayers = database.query(
    "SELECT * FROM players",
    playerMapper
);

// Get players with a specific level
List<Player> levelTenPlayers = database.query(
    "SELECT * FROM players WHERE level = ?",
    playerMapper,
    10
);

// Get a single player by ID
Optional<Player> playerOptional = database.queryFirst(
    "SELECT * FROM players WHERE id = ?",
    playerMapper,
    playerId
);

// Check if a player exists
boolean exists = database.queryFirst(
    "SELECT 1 FROM players WHERE name = ?",
    row -> true,  // Simple mapper that just returns true
    playerName
).isPresent();
```

### Updating Data

To modify data in the database:

```java
// Insert a new player
int rowsInserted = database.update(
    "INSERT INTO players (uuid, name, level, experience) VALUES (?, ?, ?, ?)",
    uuid.toString(),
    playerName,
    1,
    0
);

// Update an existing player's level
int rowsUpdated = database.update(
    "UPDATE players SET level = ?, experience = ? WHERE uuid = ?",
    newLevel,
    newExperience,
    uuid.toString()
);

// Delete a player
int rowsDeleted = database.update(
    "DELETE FROM players WHERE uuid = ?",
    uuid.toString()
);
```

### Batch Updates

For bulk operations:

```java
// Prepare batch parameter sets
List<Object[]> parameterSets = new ArrayList<>();

// Add parameters for multiple players
for (Player player : players) {
    parameterSets.add(new Object[] {
        player.getUuid().toString(),
        player.getName(),
        player.getLevel(),
        player.getExperience()
    });
}

// Execute batch insert
int[] results = database.batchUpdate(
    "INSERT INTO players (uuid, name, level, experience) VALUES (?, ?, ?, ?)",
    parameterSets
);
```

## Working with UUID Values

### UUID Convenience Methods

SQL-Bridge provides convenience methods for working with UUIDs, which are common in Minecraft plugins:

```java
// Find a player by UUID
Optional<Player> player = database.findByUuid(
    "SELECT * FROM players WHERE uuid = ?",
    playerMapper,
    playerUuid  // No need for toString()
);

// Find a player by UUID asynchronously
database.findByUuidAsync(
    "SELECT * FROM players WHERE uuid = ?",
    playerMapper,
    playerUuid
).thenAccept(optionalPlayer -> {
    // Process player data
});

// Safe version with error handling
Optional<Player> player = database.findByUuidSafe(
    "SELECT * FROM players WHERE uuid = ?",
    playerMapper,
    playerUuid,
    getLogger()
);
```

### Converting UUIDs to Strings

If you're not using the convenience methods, you must convert UUIDs to strings:

```java
// When using regular query methods
Optional<Player> player = database.queryFirst(
    "SELECT * FROM players WHERE uuid = ?",
    playerMapper,
    playerUuid.toString()  // Convert to string
);
```

## Query Builder API

### SelectBuilder

```java
// Basic select
List<Player> allPlayers = database.select()
    .columns("*")
    .from("players")
    .executeQuery(playerMapper);

// Select with conditions
List<Player> highLevelPlayers = database.select()
    .columns("id", "uuid", "name", "level", "experience")
    .from("players")
    .where("level > ?", 10)
    .orderBy("level DESC, experience DESC")
    .limit(5)
    .executeQuery(playerMapper);

// Join example
List<PlayerWithItems> playersWithItems = database.select()
    .columns("p.id", "p.uuid", "p.name", "p.level", "i.item_id", "i.item_name")
    .from("players p")
    .innerJoin("player_items i", "p.id = i.player_id")
    .where("p.level >= ?", 5)
    .executeQuery(playerWithItemsMapper);
```

### InsertBuilder

```java
// Basic insert
int result = database.insertInto("players")
    .columns("uuid", "name", "level", "experience")
    .values(uuid.toString(), "PlayerName", 1, 0)
    .executeUpdate();

// Insert with column-value pairs
Map<String, Object> playerData = new HashMap<>();
playerData.put("uuid", uuid.toString());
playerData.put("name", "PlayerName");
playerData.put("level", 1);
playerData.put("experience", 0);

int result = database.insertInto("players")
    .columnValues(playerData)
    .executeUpdate();

// Multiple row insert
database.insertInto("player_items")
    .columns("player_id", "item_id", "quantity")
    .values(playerId, 101, 1)
    .addRow(playerId, 102, 5)
    .addRow(playerId, 103, 10)
    .executeUpdate();
```

### UpdateBuilder

```java
// Basic update
int result = database.update("players")
    .set("level", 5)
    .set("experience", 500)
    .where("uuid = ?", uuid.toString())
    .executeUpdate();

// Update with column-value pairs
Map<String, Object> updateData = new HashMap<>();
updateData.put("level", 5);
updateData.put("experience", 500);
updateData.put("last_login", new Timestamp(System.currentTimeMillis()));

int result = database.update("players")
    .set(updateData)
    .where("uuid = ?", uuid.toString())
    .executeUpdate();
```

### DeleteBuilder

```java
// Basic delete
int result = database.deleteFrom("players")
    .where("uuid = ?", uuid.toString())
    .executeUpdate();

// Delete with additional conditions
int result = database.deleteFrom("player_items")
    .where("player_id = ?", playerId)
    .and("item_id IN (?, ?, ?)", 101, 102, 103)
    .executeUpdate();

// Delete with limit (MySQL only)
int result = database.deleteFrom("inactive_players")
    .where("last_login < ?", cutoffDate)
    .orderBy("last_login ASC")
    .limit(100)
    .executeUpdate();
```

## Error Handling

### Using try-catch

Standard error handling with try-catch:

```java
try {
    List<Player> players = database.query(
        "SELECT * FROM players",
        playerMapper
    );
    // Process players
} catch (SQLException e) {
    getLogger().severe("Error querying players: " + e.getMessage());
    e.printStackTrace();
}
```

### Safe Methods

SQL-Bridge provides "safe" versions of methods that handle exceptions internally:

```java
// Safe query (no try-catch needed)
List<Player> players = database.querySafe(
    "SELECT * FROM players",
    playerMapper,
    getLogger()  // Logger for error reporting
);

// Safe queryFirst
Optional<Player> player = database.queryFirstSafe(
    "SELECT * FROM players WHERE id = ?",
    playerMapper,
    getLogger(),
    playerId
);

// Safe update
int rowsUpdated = database.updateSafe(
    "UPDATE players SET level = ? WHERE id = ?",
    getLogger(),
    newLevel,
    playerId
);
```

## Asynchronous Operations

### CompletableFuture API

SQL-Bridge provides asynchronous versions of all operations that return `CompletableFuture`:

```java
// Async query
database.queryAsync(
    "SELECT * FROM players",
    playerMapper
).thenAccept(players -> {
    // Process players
}).exceptionally(e -> {
    getLogger().severe("Error in async query: " + e.getMessage());
    return null;
});

// Async update
database.updateAsync(
    "UPDATE players SET level = ? WHERE id = ?",
    newLevel,
    playerId
).thenAccept(rowsUpdated -> {
    getLogger().info("Updated " + rowsUpdated + " rows");
}).exceptionally(e -> {
    getLogger().severe("Error in async update: " + e.getMessage());
    return null;
});

// Chain async operations
database.queryFirstAsync(
    "SELECT id FROM players WHERE uuid = ?",
    row -> row.getInt("id"),
    uuid.toString()
).thenCompose(optionalId -> {
    if (optionalId.isPresent()) {
        int playerId = optionalId.get();
        return database.queryAsync(
            "SELECT * FROM player_items WHERE player_id = ?",
            itemMapper,
            playerId
        );
    } else {
        return CompletableFuture.completedFuture(List.of());
    }
}).thenAccept(items -> {
    // Process items
});
```

### Callback Methods

For callback-style async operations:

```java
// Query with callback
database.queryWithCallback(
    "SELECT * FROM players",
    playerMapper,
    new DatabaseResultCallback<List<Player>>() {
        @Override
        public void onSuccess(List<Player> players) {
            // Process players
        }
        
        @Override
        public void onError(Exception e) {
            getLogger().severe("Error: " + e.getMessage());
        }
    }
);

// Update with callback
database.updateWithCallback(
    "UPDATE players SET level = ? WHERE id = ?",
    new DatabaseResultCallback<Integer>() {
        @Override
        public void onSuccess(Integer rowsUpdated) {
            getLogger().info("Updated " + rowsUpdated + " rows");
        }
        
        @Override
        public void onError(Exception e) {
            getLogger().severe("Error: " + e.getMessage());
        }
    },
    newLevel,
    playerId
);
```

## Transaction Support

### Basic Transactions

```java
try {
    boolean success = database.executeTransaction(connection -> {
        // Get player ID
        Optional<Integer> optionalId = database.queryFirst(
            "SELECT id FROM players WHERE uuid = ? FOR UPDATE",
            row -> row.getInt("id"),
            uuid.toString()
        );
        
        if (!optionalId.isPresent()) {
            return false; // Player not found
        }
        
        int playerId = optionalId.get();
        
        // Update player level
        database.update(
            "UPDATE players SET level = level + 1 WHERE id = ?",
            playerId
        );
        
        // Add achievement for leveling up
        database.update(
            "INSERT INTO achievements (player_id, type, earned_at) VALUES (?, ?, ?)",
            playerId,
            "LEVEL_UP",
            new Timestamp(System.currentTimeMillis())
        );
        
        return true;
    });
    
    if (success) {
        getLogger().info("Player leveled up and achievement added");
    } else {
        getLogger().warning("Transaction failed - player not found");
    }
} catch (SQLException e) {
    getLogger().severe("Transaction error: " + e.getMessage());
}
```

### Using Savepoints

For more complex transactions with partial rollbacks:

```java
try {
    database.executeTransaction(connection -> {
        // Start transaction
        
        // Create a savepoint
        Savepoint savepoint = connection.setSavepoint("before_risky_operation");
        
        try {
            // Perform risky operation
            database.update("DELETE FROM player_achievements WHERE player_id = ?", playerId);
            
            // If successful, commit
        } catch (SQLException e) {
            // If failed, rollback to savepoint
            connection.rollback(savepoint);
            getLogger().warning("Rolled back risky operation: " + e.getMessage());
        }
        
        // Continue with other operations that will still commit
        database.update("UPDATE players SET last_login = ? WHERE id = ?", 
                      new Timestamp(System.currentTimeMillis()), playerId);
        
        return true;
    });
} catch (SQLException e) {
    getLogger().severe("Transaction error: " + e.getMessage());
}
```

## Schema Migrations

### Defining Migrations

```java
List<Migration> migrations = new ArrayList<>();

// Add your migrations in order
migrations.add(new Migration() {
    @Override
    public int getVersion() {
        return 1; // Version number
    }
    
    @Override
    public String getDescription() {
        return "Create players table";
    }
    
    @Override
    public void migrate(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE players (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  uuid VARCHAR(36) NOT NULL UNIQUE, " +
                "  name VARCHAR(16) NOT NULL, " +
                "  level INTEGER NOT NULL DEFAULT 1" +
                ")"
            );
        }
    }
    
    @Override
    public void rollback(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS players");
        }
    }
});

// Add a second migration
migrations.add(new Migration() {
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
            stmt.execute("ALTER TABLE players ADD COLUMN experience INTEGER NOT NULL DEFAULT 0");
        }
    }
    
    @Override
    public void rollback(Connection connection) throws SQLException {
        // SQLite doesn't support dropping columns directly
        // For a real implementation, you might need to recreate the table
        // This is just an example
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE players_temp AS " +
                "SELECT id, uuid, name, level FROM players"
            );
            stmt.execute("DROP TABLE players");
            stmt.execute(
                "CREATE TABLE players (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  uuid VARCHAR(36) NOT NULL UNIQUE, " +
                "  name VARCHAR(16) NOT NULL, " +
                "  level INTEGER NOT NULL DEFAULT 1" +
                ")"
            );
            stmt.execute(
                "INSERT INTO players (id, uuid, name, level) " +
                "SELECT id, uuid, name, level FROM players_temp"
            );
            stmt.execute("DROP TABLE players_temp");
        }
    }
});
```

### Running Migrations

```java
// Register migrations
databaseService.registerMigrations(this, migrations);

// Run migrations
int appliedCount = databaseService.runMigrationsSafe(this);
getLogger().info("Applied " + appliedCount + " migrations");

// Check current schema version
int currentVersion = databaseService.getCurrentSchemaVersion(this);
getLogger().info("Current schema version: " + currentVersion);
```

## Data Access Objects (DAOs)

### DAO Pattern Structure

Using the DAO pattern helps organize database code:

```java
public class PlayerDAO {
    private final Database database;
    private final Logger logger;
    private final ResultMapper<Player> playerMapper;
    
    public PlayerDAO(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
        this.playerMapper = createPlayerMapper();
    }
    
    private ResultMapper<Player> createPlayerMapper() {
        return row -> {
            Player player = new Player();
            player.setId(row.getInt("id"));
            player.setUuid(UUID.fromString(row.getString("uuid")));
            player.setName(row.getString("name"));
            player.setLevel(row.getInt("level"));
            player.setExperience(row.getInt("experience"));
            return player;
        };
    }
    
    // Database methods for Player entity
    // ...
}
```

### Complete DAO Example

Here's a complete example of a DAO implementation:

```java
public class PlayerDAO {
    private final Database database;
    private final Logger logger;
    private final ResultMapper<Player> playerMapper;
    
    public PlayerDAO(Database database, Plugin plugin) {
        this.database = database;
        this.logger = plugin.getLogger();
        
        // Define the mapper once as a field
        this.playerMapper = row -> {
            Player player = new Player();
            player.setId(row.getInt("id"));
            player.setUuid(UUID.fromString(row.getString("uuid")));
            player.setName(row.getString("name"));
            player.setLevel(row.getInt("level"));
            player.setExperience(row.getInt("experience"));
            player.setLastLogin(row.getTimestamp("last_login"));
            return player;
        };
    }
    
    /**
     * Find a player by UUID.
     */
    public Optional<Player> findByUuid(UUID uuid) {
        try {
            return database.findByUuid(
                "SELECT * FROM players WHERE uuid = ?",
                playerMapper,
                uuid
            );
        } catch (SQLException e) {
            logger.severe("Error finding player by UUID: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Find a player by UUID asynchronously.
     */
    public CompletableFuture<Optional<Player>> findByUuidAsync(UUID uuid) {
        return database.findByUuidAsync(
            "SELECT * FROM players WHERE uuid = ?",
            playerMapper,
            uuid
        ).exceptionally(e -> {
            logger.severe("Error finding player by UUID async: " + e.getMessage());
            return Optional.empty();
        });
    }
    
    /**
     * Find a player by name.
     */
    public Optional<Player> findByName(String name) {
        try {
            return database.queryFirst(
                "SELECT * FROM players WHERE name = ? COLLATE NOCASE",
                playerMapper,
                name
            );
        } catch (SQLException e) {
            logger.severe("Error finding player by name: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Get all players.
     */
    public List<Player> findAll() {
        try {
            return database.query(
                "SELECT * FROM players ORDER BY name", 
                playerMapper
            );
        } catch (SQLException e) {
            logger.severe("Error getting all players: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get players with a minimum level.
     */
    public List<Player> findByMinimumLevel(int minLevel) {
        try {
            return database.select()
                .columns("*")
                .from("players")
                .where("level >= ?", minLevel)
                .orderBy("level DESC, name ASC")
                .executeQuery(playerMapper);
        } catch (SQLException e) {
            logger.severe("Error finding players by level: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Save a player (insert or update).
     */
    public boolean save(Player player) {
        try {
            // Check if player exists
            boolean exists = database.findByUuid(
                "SELECT 1 FROM players WHERE uuid = ?",
                row -> true,
                player.getUuid()
            ).isPresent();
            
            if (exists) {
                // Update
                return database.update(
                    "UPDATE players SET name = ?, level = ?, experience = ?, last_login = ? WHERE uuid = ?",
                    player.getName(),
                    player.getLevel(),
                    player.getExperience(),
                    new Timestamp(System.currentTimeMillis()),
                    player.getUuid().toString()
                ) > 0;
            } else {
                // Insert
                return database.update(
                    "INSERT INTO players (uuid, name, level, experience, last_login) VALUES (?, ?, ?, ?, ?)",
                    player.getUuid().toString(),
                    player.getName(),
                    player.getLevel(),
                    player.getExperience(),
                    new Timestamp(System.currentTimeMillis())
                ) > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error saving player: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Save a player asynchronously.
     */
    public CompletableFuture<Boolean> saveAsync(Player player) {
        return findByUuidAsync(player.getUuid())
            .thenCompose(optPlayer -> {
                if (optPlayer.isPresent()) {
                    // Update
                    return database.updateAsync(
                        "UPDATE players SET name = ?, level = ?, experience = ?, last_login = ? WHERE uuid = ?",
                        player.getName(),
                        player.getLevel(),
                        player.getExperience(),
                        new Timestamp(System.currentTimeMillis()),
                        player.getUuid().toString()
                    ).thenApply(rows -> rows > 0);
                } else {
                    // Insert
                    return database.updateAsync(
                        "INSERT INTO players (uuid, name, level, experience, last_login) VALUES (?, ?, ?, ?, ?)",
                        player.getUuid().toString(),
                        player.getName(),
                        player.getLevel(),
                        player.getExperience(),
                        new Timestamp(System.currentTimeMillis())
                    ).thenApply(rows -> rows > 0);
                }
            });
    }
    
    /**
     * Delete a player by UUID.
     */
    public boolean deleteByUuid(UUID uuid) {
        try {
            return database.update(
                "DELETE FROM players WHERE uuid = ?",
                uuid.toString()
            ) > 0;
        } catch (SQLException e) {
            logger.severe("Error deleting player: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Add experience to a player with transaction.
     */
    public boolean addExperience(UUID uuid, int amount) {
        try {
            return database.executeTransaction(connection -> {
                // Get current player data with lock
                Optional<Player> optPlayer = database.findByUuid(
                    "SELECT * FROM players WHERE uuid = ? FOR UPDATE",
                    playerMapper,
                    uuid
                );
                
                if (!optPlayer.isPresent()) {
                    return false;
                }
                
                Player player = optPlayer.get();
                int newExp = player.getExperience() + amount;
                int newLevel = player.getLevel();
                
                // Simple level up logic: 1000 XP per level
                while (newExp >= 1000) {
                    newExp -= 1000;
                    newLevel++;
                }
                
                // Update player
                int updated = database.update(
                    "UPDATE players SET level = ?, experience = ? WHERE uuid = ?",
                    newLevel,
                    newExp,
                    uuid.toString()
                );
                
                return updated > 0;
            });
        } catch (SQLException e) {
            logger.severe("Error adding experience: " + e.getMessage());
            return false;
        }
    }
}
```

## Performance Optimization

### Connection Pooling

SQL-Bridge uses HikariCP for connection pooling. You can configure it in `config.yml`:

```yaml
database:
  mysql:
    pool:
      maximum-pool-size: 10
      minimum-idle: 5
      maximum-lifetime: 1800000
      connection-timeout: 5000
```

### Batch Processing

For bulk operations, use batch updates:

```java
// Prepare batch parameters
List<Object[]> parameters = new ArrayList<>();
for (Player player : players) {
    parameters.add(new Object[] {
        player.getUuid().toString(),
        player.getName(),
        player.getLevel(),
        player.getExperience()
    });
}

// Execute batch update
int[] results = database.batchUpdate(
    "INSERT INTO players (uuid, name, level, experience) VALUES (?, ?, ?, ?)",
    parameters
);
```

## Common Pitfalls and Solutions

### ResultSet vs ResultRow

**Problem**: Using `java.sql.ResultSet` instead of `com.minecraft.sqlbridge.api.result.ResultRow` in mappers.

**Solution**: Always use `ResultRow` in your mappers:

```java
// CORRECT
ResultMapper<Player> mapper = row -> {  // Using ResultRow
    Player player = new Player();
    player.setId(row.getInt("id"));
    // ...
    return player;
};

// WRONG - Will cause errors
ResultMapper<Player> wrongMapper = rs -> {  // Using ResultSet
    Player player = new Player();
    player.setId(rs.getInt("id"));
    // ...
    return player;
};
```

### Method Reference Issues

**Problem**: Using method references with incompatible parameter types.

**Solution**: Ensure your methods accept `ResultRow`:

```java
// Define the method with ResultRow parameter
private Player mapPlayer(ResultRow row) throws SQLException {
    Player player = new Player();
    player.setId(row.getInt("id"));
    // ...
    return player;
}

// CORRECT - Method accepts ResultRow
database.queryFirst("SELECT * FROM players WHERE id = ?", this::mapPlayer, id);

// ALTERNATIVE - Use a lambda
database.queryFirst("SELECT * FROM players WHERE id = ?", 
    row -> {
        Player player = new Player();
        player.setId(row.getInt("id"));
        // ...
        return player;
    }, 
    id
);
```

### Thread Safety

**Problem**: Accessing Bukkit API from async database operations.

**Solution**: Use Bukkit's scheduler to return to the main thread:

```java
// CORRECT - Run Bukkit API calls on the main thread
database.findByUuidAsync("SELECT * FROM players WHERE uuid = ?", playerMapper, uuid)
    .thenAccept(optionalPlayer -> {
        if (optionalPlayer.isPresent()) {
            Player player = optionalPlayer.get();
            
            // Run on the main thread for Bukkit API
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Safe to use Bukkit API here
                Bukkit.getPlayer(uuid).sendMessage("Your level: " + player.getLevel());
            });
        }
    });

// WRONG - Will cause errors
database.findByUuidAsync("SELECT * FROM players WHERE uuid = ?", playerMapper, uuid)
    .thenAccept(optionalPlayer -> {
        if (optionalPlayer.isPresent()) {
            // Bukkit API called from async thread - dangerous!
            Bukkit.getPlayer(uuid).sendMessage("Your level: " + optionalPlayer.get().getLevel());
        }
    });
```

## Best Practices

1. **Reuse mappers**: Define mappers once as class fields for better performance.

2. **Use asynchronous operations**: Avoid blocking the main server thread with database operations.

3. **Organize with DAOs**: Use the DAO pattern to organize database code by entity.

4. **Use transactions for related operations**: Group related modifications into transactions.

5. **Validate input data**: Validate parameters before passing them to database methods.

6. **Log errors properly**: Include useful information in error messages.

7. **Use prepared statements**: Never build SQL with string concatenation (SQL-Bridge handles this for you).

8. **Close resources properly**: SQL-Bridge handles this automatically, but be careful with raw JDBC code.

9. **Handle nulls and exceptions**: Use Optional, safe methods, and proper exception handling.

10. **Batch operations for bulk updates**: Use batch updates for multiple related operations.