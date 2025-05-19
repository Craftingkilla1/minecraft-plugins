# SQL-Bridge

A high-performance database abstraction layer for Spigot/Bukkit plugins, providing a clean and consistent API for database operations.

## Features

* **Multiple Database Support**: MySQL, SQLite (with plans for PostgreSQL, H2)
* **Connection Pooling**: Built on HikariCP for optimal performance
* **Fluent Query Builder API**: Type-safe and intuitive query construction
* **Result Mapping**: Easy conversion from database results to Java objects
* **Transaction Support**: ACID-compliant operations
* **Schema Migrations**: Version-controlled database schema updates
* **Asynchronous Operations**: Non-blocking database interactions
* **SQL Injection Prevention**: Automatic security checks
* **Performance Monitoring**: Metrics and statistics for optimization
* **UUID Convenience Methods**: Specialized support for Minecraft's UUIDs

## Installation

### As a Server Owner

1. Download the latest versions of [Core-Utils.jar](https://github.com/your-repository/Core-Utils/releases) and [SQL-Bridge.jar](https://github.com/your-repository/Sql-Bridge/releases)
2. Place both JAR files in your server's `plugins` folder
3. Start your server to generate the configuration files
4. Configure in `plugins/SQL-Bridge/config.yml`

### As a Developer

#### Add the Dependencies

**Maven:**
```xml
<dependencies>
    <!-- Core-Utils dependency -->
    <dependency>
        <groupId>com.minecraft</groupId>
        <artifactId>core-utils</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- SQL-Bridge dependency -->
    <dependency>
        <groupId>com.minecraft</groupId>
        <artifactId>sql-bridge</artifactId>
        <version>1.0.4</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**Gradle:**
```groovy
dependencies {
    compileOnly 'com.minecraft:core-utils:1.0.0'
    compileOnly 'com.minecraft:sql-bridge:1.0.4'
}
```

#### Update plugin.yml

```yaml
name: YourPlugin
version: 1.0.0
main: com.example.YourPlugin
api-version: 1.16
# Add SQL-Bridge as a dependency
depend: [Core-Utils, SQL-Bridge]
# Or if optional:
softdepend: [Core-Utils, SQL-Bridge]
```

## Getting Started

### Accessing the Database Service

```java
import com.minecraft.core.api.service.ServiceRegistry;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.DatabaseService;

public class YourPlugin extends JavaPlugin {

    private DatabaseService databaseService;
    private Database database;

    @Override
    public void onEnable() {
        // Get the database service from the service registry
        databaseService = ServiceRegistry.getService(DatabaseService.class);
        
        if (databaseService == null) {
            getLogger().severe("Could not find DatabaseService! Is SQL-Bridge enabled?");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Get a database connection for your plugin
        database = databaseService.getDatabaseForPlugin(this);
        
        // Initialize your database tables
        initializeDatabase();
        
        getLogger().info("Plugin enabled with database support");
    }
    
    private void initializeDatabase() {
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
}
```

### Defining a Result Mapper

SQL-Bridge uses the `ResultMapper` functional interface to convert database results to Java objects:

```java
import com.minecraft.sqlbridge.api.result.ResultMapper;
import com.minecraft.sqlbridge.api.result.ResultRow;

// Define a mapper for Player objects
ResultMapper<Player> playerMapper = row -> {  // IMPORTANT: Use ResultRow, not ResultSet!
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

### Basic Database Operations

#### Querying Data

```java
// Get all players
try {
    List<Player> allPlayers = database.query(
        "SELECT * FROM players", 
        playerMapper
    );
    
    // Process players
} catch (SQLException e) {
    getLogger().severe("Error querying players: " + e.getMessage());
}

// Get a single player by UUID
try {
    Optional<Player> playerOptional = database.findByUuid(
        "SELECT * FROM players WHERE uuid = ?",
        playerMapper,
        playerUuid  // UUID convenience method - no toString() needed
    );
    
    if (playerOptional.isPresent()) {
        Player player = playerOptional.get();
        // Process player data
    }
} catch (SQLException e) {
    getLogger().severe("Error finding player: " + e.getMessage());
}
```

#### Updating Data

```java
// Insert a new player
try {
    int rowsInserted = database.update(
        "INSERT INTO players (uuid, name, level, experience) VALUES (?, ?, ?, ?)",
        playerUuid.toString(),  // Convert UUID to string for regular update methods
        playerName,
        1,
        0
    );
    
    if (rowsInserted > 0) {
        getLogger().info("Player inserted successfully");
    }
} catch (SQLException e) {
    getLogger().severe("Error inserting player: " + e.getMessage());
}

// Update a player's level
try {
    int rowsUpdated = database.update(
        "UPDATE players SET level = ?, experience = ? WHERE uuid = ?",
        newLevel,
        newExperience,
        playerUuid.toString()
    );
    
    if (rowsUpdated > 0) {
        getLogger().info("Player updated successfully");
    }
} catch (SQLException e) {
    getLogger().severe("Error updating player: " + e.getMessage());
}
```

### Using the Query Builder API

SQL-Bridge provides a fluent query builder API for constructing SQL queries:

#### SELECT Queries

```java
try {
    List<Player> highLevelPlayers = database.select()
        .columns("id", "uuid", "name", "level", "experience")
        .from("players")
        .where("level > ?", 10)
        .orderBy("level DESC")
        .limit(5)
        .executeQuery(playerMapper);
    
    // Process players
} catch (SQLException e) {
    getLogger().severe("Error querying high-level players: " + e.getMessage());
}
```

#### INSERT Queries

```java
try {
    int result = database.insertInto("players")
        .columns("uuid", "name", "level", "experience")
        .values(playerUuid.toString(), playerName, 1, 0)
        .executeUpdate();
    
    if (result > 0) {
        getLogger().info("Player inserted successfully");
    }
} catch (SQLException e) {
    getLogger().severe("Error inserting player: " + e.getMessage());
}
```

#### UPDATE Queries

```java
try {
    int result = database.update("players")
        .set("level", newLevel)
        .set("experience", newExperience)
        .where("uuid = ?", playerUuid.toString())
        .executeUpdate();
    
    if (result > 0) {
        getLogger().info("Player updated successfully");
    }
} catch (SQLException e) {
    getLogger().severe("Error updating player: " + e.getMessage());
}
```

#### DELETE Queries

```java
try {
    int result = database.deleteFrom("players")
        .where("uuid = ?", playerUuid.toString())
        .executeUpdate();
    
    if (result > 0) {
        getLogger().info("Player deleted successfully");
    }
} catch (SQLException e) {
    getLogger().severe("Error deleting player: " + e.getMessage());
}
```

### Asynchronous Operations

SQL-Bridge supports asynchronous database operations to prevent blocking the main server thread:

```java
// Async query
database.queryAsync(
    "SELECT * FROM players",
    playerMapper
).thenAccept(players -> {
    // Process players on completion
    Bukkit.getScheduler().runTask(this, () -> {
        // Use results on the main thread
    });
}).exceptionally(e -> {
    getLogger().severe("Error in async query: " + e.getMessage());
    return null;
});

// Async update
database.updateAsync(
    "UPDATE players SET level = ? WHERE uuid = ?",
    newLevel,
    playerUuid.toString()
).thenAccept(rowsUpdated -> {
    getLogger().info("Updated " + rowsUpdated + " rows");
}).exceptionally(e -> {
    getLogger().severe("Error in async update: " + e.getMessage());
    return null;
});
```

### Transaction Support

For operations that need to be performed atomically:

```java
try {
    boolean success = database.executeTransaction(connection -> {
        // Get player ID
        Optional<Integer> optionalId = database.queryFirst(
            "SELECT id FROM players WHERE uuid = ? FOR UPDATE",
            row -> row.getInt("id"),
            playerUuid.toString()
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

### Schema Migrations

SQL-Bridge provides a migration system for versioning your database schema:

```java
// Define migrations
List<Migration> migrations = new ArrayList<>();

// Add migration to create players table
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

// Add migration to add experience column
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
        // For a real implementation, you would need a more complex rollback
    }
});

// Register and run migrations
databaseService.registerMigrations(this, migrations);
int appliedCount = databaseService.runMigrationsSafe(this);
getLogger().info("Applied " + appliedCount + " migrations");
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

## Using the Data Access Object (DAO) Pattern

For organizing database code, the DAO pattern works well with SQL-Bridge:

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
        return database.findByUuidSafe(
            "SELECT * FROM players WHERE uuid = ?",
            playerMapper,
            uuid,
            logger
        );
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
                    "UPDATE players SET name = ?, level = ?, experience = ? WHERE uuid = ?",
                    player.getName(),
                    player.getLevel(),
                    player.getExperience(),
                    player.getUuid().toString()
                ) > 0;
            } else {
                // Insert
                return database.update(
                    "INSERT INTO players (uuid, name, level, experience) VALUES (?, ?, ?, ?)",
                    player.getUuid().toString(),
                    player.getName(),
                    player.getLevel(),
                    player.getExperience()
                ) > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error saving player: " + e.getMessage());
            return false;
        }
    }
    
    // More methods...
}
```

## Common Pitfalls

### 1. Using ResultSet Instead of ResultRow

**Problem**: The most common error is using `java.sql.ResultSet` instead of `com.minecraft.sqlbridge.api.result.ResultRow` in your mappers.

**Solution**: Always use `ResultRow` in your mappers:

```java
// CORRECT ✅
ResultMapper<Player> mapper = row -> {  // Using ResultRow
    Player player = new Player();
    player.setId(row.getInt("id"));
    // ...
    return player;
};

// WRONG ❌
ResultMapper<Player> wrongMapper = rs -> {  // Using ResultSet
    Player player = new Player();
    player.setId(rs.getInt("id"));
    // ...
    return player;
};
```

### 2. UUID Handling

**Problem**: UUIDs need special handling in SQL.

**Solution**:

- Use the UUID convenience methods when possible:
  ```java
  database.findByUuid("SELECT * FROM players WHERE uuid = ?", mapper, uuid);
  ```

- When using regular methods, convert UUIDs to strings:
  ```java
  database.queryFirst("SELECT * FROM players WHERE uuid = ?", mapper, uuid.toString());
  ```

### 3. Thread Safety with Bukkit API

**Problem**: Accessing Bukkit API from async database operations can cause errors.

**Solution**: Return to the main thread for Bukkit API calls:

```java
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
```

## Configuration

SQL-Bridge creates a default configuration file at `plugins/SQL-Bridge/config.yml`. Here are the key settings:

```yaml
# Debug mode
debug: false

# Database configuration
database:
  # Database type: MYSQL, SQLITE
  type: sqlite
  
  # MySQL configuration
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: root
    password: password
    auto-create-database: true
    
    # Connection pool settings
    pool:
      maximum-pool-size: 10
      minimum-idle: 5
      maximum-lifetime: 1800000
      connection-timeout: 5000
  
  # SQLite configuration
  sqlite:
    file: database.db
    wal-mode: true

# Security configuration
security:
  sql-injection-detection: true
  log-dangerous-operations: true
  max-query-length: 10000

# Performance monitoring
monitoring:
  enabled: true
  slow-query-threshold: 1000
  collect-metrics: true
  metrics-interval: 300

# Migration system
migrations:
  auto-migrate: true
  rollback-failed-migrations: true
```

## Commands

SQL-Bridge provides administrator commands to manage the database:

```
/sqlbridge info - Display plugin information
/sqlbridge status - Check database connection status
/sqlbridge test - Test database connectivity
/sqlbridge migrations - Show migration status
/sqlbridge reload - Reload the SQL-Bridge configuration
/sqlbridge errors - View recent database errors
```

## Best Practices

1. **Reuse mappers**: Define mappers once as class fields for better performance.
2. **Use asynchronous operations**: Avoid blocking the main server thread.
3. **Organize with DAOs**: Use the DAO pattern to organize database code.
4. **Use transactions for related operations**: Keep database operations atomic.
5. **Handle errors properly**: Use try-catch or safe methods.
6. **Use prepared statements**: Never build SQL with string concatenation.
7. **Check database existence**: Ensure tables exist before using them.
8. **Close resources properly**: SQL-Bridge handles this for you in most cases.
9. **Use batch operations for bulk updates**: Improve performance for multiple operations.
10. **Keep database logic separate**: Don't mix database code with other plugin logic.

## License

This project is licensed under the MIT License - see the LICENSE file for details.