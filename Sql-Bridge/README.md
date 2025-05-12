# SQL-Bridge

A high-performance database connectivity plugin for Minecraft servers, providing a robust API for SQL database operations with comprehensive monitoring and performance optimization.

## Features

- **Multi-Database Support**: Connect to MySQL, PostgreSQL, SQLite, and H2 databases
- **Connection Pooling**: Efficient connection management with HikariCP
- **Performance Monitoring**: Real-time metrics collection for database operations
- **Query Builder**: Fluent API for building SQL queries
- **Transaction Support**: Simplified transaction management
- **Batch Operations**: Execute multiple queries efficiently
- **Comprehensive Error Handling**: Robust exception management for stable operation
- **Extensible API**: Easy integration with other plugins
- **Monitoring Dashboards**: Track database performance with built-in metrics
- **Scalable Architecture**: Designed for high-traffic Minecraft servers
- **Migration System**: Manage database schema versions and migrations
- **BungeeCord Support**: Share database connections across a network of servers

## Requirements

- Java 8 or higher
- Bukkit/Spigot/Paper server 1.12.2 or higher
- CoreUtils plugin
- Database server (MySQL, PostgreSQL) or file-based database (SQLite, H2)

## Installation

1. Download the latest release from the [releases page](https://github.com/yourusername/sql-bridge/releases)
2. Place the JAR file in your server's `plugins` directory
3. Start or restart your server
4. Edit the configuration file at `plugins/SQL-Bridge/config.yml`
5. Restart your server again to apply the configuration

## Configuration

The plugin creates a default configuration file at `plugins/SQL-Bridge/config.yml`. Here's a sample configuration:

```yaml
# Debug mode - enables additional logging
debug: false

# Database configuration
database:
  # Database type: MYSQL, POSTGRESQL, SQLITE, H2
  type: sqlite
  
  # MySQL configuration
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
      # Allow the pool to self-heal connection failures
      auto-reconnect: true

  # SQLite configuration
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
```

## Developer Guide

### Getting Started

SQL-Bridge uses a service-based architecture to provide database connectivity to your plugin. Here's how to integrate it with your plugin:

1. Add SQL-Bridge as a dependency in your `plugin.yml`:

```yaml
depend: [SQL-Bridge, CoreUtils]
```

2. Access the database service in your plugin:

```java
import com.minecraft.sqlbridge.api.DatabaseService;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.core.api.service.ServiceRegistry;

public class YourPlugin extends JavaPlugin {
    private DatabaseService databaseService;
    private Database database;
    
    @Override
    public void onEnable() {
        // Get the DatabaseService from the ServiceRegistry
        databaseService = ServiceRegistry.getService(DatabaseService.class);
        if (databaseService == null) {
            getLogger().severe("Failed to get DatabaseService - is SQL-Bridge enabled?");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Get a database connection specific to your plugin
        database = databaseService.getDatabaseForPlugin(getName());
        
        // Initialize your database schema
        initDatabase();
    }
    
    private void initDatabase() {
        try {
            // Create tables, etc.
            database.update("CREATE TABLE IF NOT EXISTS my_table (" +
                           "id INT AUTO_INCREMENT PRIMARY KEY, " +
                           "name VARCHAR(100) NOT NULL, " +
                           "value INT NOT NULL)");
            
            getLogger().info("Database initialized successfully!");
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### Basic Database Operations

#### Executing Queries

```java
// Simple query with ResultMapper to map results to objects
List<Player> players = database.query(
    "SELECT * FROM players WHERE level > ?",
    row -> {
        // Map each row to a Player object
        Player player = new Player();
        player.setId(row.getInt("id"));
        player.setName(row.getString("name"));
        player.setLevel(row.getInt("level"));
        return player;
    },
    10 // Parameter value for the ? placeholder
);

// Get a single result
Optional<Player> playerOpt = database.queryFirst(
    "SELECT * FROM players WHERE name = ?",
    row -> {
        Player player = new Player();
        player.setId(row.getInt("id"));
        player.setName(row.getString("name"));
        player.setLevel(row.getInt("level"));
        return player;
    },
    "PlayerName"
);

// Process results with a consumer
database.executeQuery(
    "SELECT * FROM players", 
    row -> {
        // Process each row
        String name = row.getString("name");
        int level = row.getInt("level");
        System.out.println(name + " is level " + level);
    }
);
```

#### Executing Updates

```java
// Insert a new player
int rowsAffected = database.update(
    "INSERT INTO players (name, level) VALUES (?, ?)",
    "NewPlayer", 1
);

// Update player level
database.update(
    "UPDATE players SET level = ? WHERE name = ?",
    5, "PlayerName"
);

// Delete a player
database.update(
    "DELETE FROM players WHERE name = ?",
    "PlayerName"
);
```

#### Asynchronous Operations

```java
// Async query
database.queryAsync(
    "SELECT * FROM players WHERE level > ?",
    row -> {
        Player player = new Player();
        player.setId(row.getInt("id"));
        player.setName(row.getString("name"));
        player.setLevel(row.getInt("level"));
        return player;
    },
    10
).thenAccept(players -> {
    // This will run when the query completes
    System.out.println("Found " + players.size() + " players");
}).exceptionally(ex -> {
    // Handle exceptions
    ex.printStackTrace();
    return null;
});

// Async update
database.updateAsync(
    "UPDATE players SET level = level + 1 WHERE name = ?",
    "PlayerName"
).thenAccept(rowsAffected -> {
    System.out.println("Updated " + rowsAffected + " rows");
}).exceptionally(ex -> {
    ex.printStackTrace();
    return null;
});
```

### Using Query Builders

SQL-Bridge provides a fluent API for building SQL queries:

#### Select Builder

```java
List<Player> players = database.select()
    .columns("id", "name", "level")
    .from("players")
    .where("level > ?", 10)
    .orderBy("level DESC")
    .limit(10)
    .executeQuery(row -> {
        Player player = new Player();
        player.setId(row.getInt("id"));
        player.setName(row.getString("name"));
        player.setLevel(row.getInt("level"));
        return player;
    });
```

#### Insert Builder

```java
int rowsAffected = database.insertInto("players")
    .columns("name", "level")
    .values("NewPlayer", 1)
    .executeUpdate();

// Insert with on duplicate key update
database.insertInto("players")
    .columns("name", "level")
    .values("ExistingPlayer", 5)
    .onDuplicateKeyUpdate("level", 5)
    .executeUpdate();

// Insert multiple rows
database.insertInto("players")
    .columns("name", "level")
    .values("Player1", 1)
    .addRow("Player2", 2)
    .addRow("Player3", 3)
    .executeUpdate();
```

#### Update Builder

```java
int rowsAffected = database.update("players")
    .set("level", 5)
    .where("name = ?", "PlayerName")
    .executeUpdate();

// Update multiple columns
Map<String, Object> updates = new HashMap<>();
updates.put("level", 10);
updates.put("experience", 500);

database.update("players")
    .set(updates)
    .where("name = ?", "PlayerName")
    .executeUpdate();
```

### Transactions

```java
// Execute operations in a transaction
boolean success = database.executeTransaction(connection -> {
    try (PreparedStatement stmt1 = connection.prepareStatement(
            "INSERT INTO players (name, level) VALUES (?, ?)")) {
        stmt1.setString(1, "NewPlayer");
        stmt1.setInt(2, 1);
        stmt1.executeUpdate();
        
        try (PreparedStatement stmt2 = connection.prepareStatement(
                "UPDATE statistics SET player_count = player_count + 1")) {
            stmt2.executeUpdate();
        }
    }
    
    return true; // Commit the transaction
});

// Async transaction
database.executeTransactionAsync(connection -> {
    // Your transaction code here
    return true;
}).thenAccept(result -> {
    System.out.println("Transaction completed with result: " + result);
}).exceptionally(ex -> {
    ex.printStackTrace();
    return null;
});
```

### Batch Operations

```java
// Create batch parameters
List<Object[]> batchParams = new ArrayList<>();
batchParams.add(new Object[] { "Player1", 10 });
batchParams.add(new Object[] { "Player2", 15 });
batchParams.add(new Object[] { "Player3", 20 });

// Execute batch update
int[] results = database.batchUpdate(
    "INSERT INTO players (name, level) VALUES (?, ?)",
    batchParams
);

// Async batch update
database.batchUpdateAsync(
    "INSERT INTO players (name, level) VALUES (?, ?)",
    batchParams
).thenAccept(results -> {
    System.out.println("Batch update completed, inserted " + results.length + " rows");
});
```

### Database Migrations

SQL-Bridge provides a migration system to manage database schema versions:

```java
public class YourPlugin extends JavaPlugin {
    private DatabaseService databaseService;
    
    @Override
    public void onEnable() {
        databaseService = ServiceRegistry.getService(DatabaseService.class);
        
        // Register migrations
        List<Migration> migrations = new ArrayList<>();
        migrations.add(new CreateInitialTables());
        migrations.add(new AddColumnsMigration());
        
        databaseService.registerMigrations(getName(), migrations);
        
        // Run migrations
        databaseService.runMigrationsAsync(getName())
            .thenAccept(count -> {
                getLogger().info("Applied " + count + " migrations");
            });
    }
    
    // Migration implementation example
    private static class CreateInitialTables implements Migration {
        @Override
        public int getVersion() {
            return 1;
        }
        
        @Override
        public String getDescription() {
            return "Create initial tables";
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
}
```

## Advanced Features

### BungeeSupport and Shared Databases

If you're running a BungeeCord network, you can enable shared databases:

```java
// Check if shared database is available
databaseService.getSharedDatabase().ifPresent(sharedDb -> {
    // Use shared database for cross-server data
    try {
        sharedDb.update("INSERT INTO global_stats (player, value) VALUES (?, ?)",
                      "PlayerName", 100);
    } catch (SQLException e) {
        e.printStackTrace();
    }
});

// Or with the BungeeSupport directly
if (plugin instanceof SqlBridgePlugin) {
    SqlBridgePlugin sqlBridgePlugin = (SqlBridgePlugin) plugin;
    BungeeSupport bungeeSupport = sqlBridgePlugin.getBungeeSupport();
    
    if (bungeeSupport != null) {
        SharedDatabaseManager sharedManager = bungeeSupport.getSharedDatabaseManager();
        
        // Store a value accessible by all servers
        sharedManager.set("global.player.count", "42")
            .thenAccept(v -> System.out.println("Value set successfully"));
    }
}
```

### Performance Monitoring

```java
// Get database statistics
Map<String, Object> stats = databaseService.getStatistics();

// Log some key metrics
System.out.println("Total queries: " + stats.get("totalQueries"));
System.out.println("Average query time: " + stats.get("averageQueryTime") + "ms");
System.out.println("Slow queries: " + stats.get("slowQueries"));
```

## Troubleshooting

### Common Issues

1. **Connection Refused**: Check that your database server is running and accessible
2. **Authentication Failed**: Verify username and password in the configuration
3. **Table Not Found**: Ensure that your database schema is correctly set up
4. **Performance Issues**: Check the monitoring dashboard for slow queries

### Enabling Debug Logging

To enable detailed logging for troubleshooting:

1. Set `debug: true` in the configuration
2. Restart the server
3. Check the server logs for detailed information

## License

This project is licensed under the MIT License - see the LICENSE file for details.