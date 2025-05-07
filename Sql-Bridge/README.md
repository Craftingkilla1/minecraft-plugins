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

## Requirements

- Java 8 or higher
- Bukkit/Spigot/Paper server 1.12.2 or higher
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
# Database configuration
database:
  # Database type: MYSQL, POSTGRESQL, SQLITE, H2
  type: MYSQL
  # Database connection settings
  host: localhost
  port: 3306
  database: minecraft
  username: username
  password: password
  # Connection pool settings
  pool:
    # Maximum number of connections in the pool
    max-connections: 10
    # Minimum idle connections
    min-idle: 5
    # Maximum lifetime of a connection in milliseconds
    max-lifetime: 1800000
    # Connection timeout in milliseconds
    connection-timeout: 5000
    # Allow the pool to self-heal connection failures
    auto-reconnect: true

# Monitoring settings
monitoring:
  # Enable performance monitoring
  enabled: true
  # Log performance metrics
  log-performance: true
  # Log database metrics
  log-db-metrics: true
  # Metrics history size
  history-size: 60
  # Sampling interval in seconds
  sampling-interval: 60
  # Slow query threshold in milliseconds
  slow-query-threshold: 1000
  # Database metrics collection interval in seconds
  db-metrics-interval: 300

# Logging settings
logging:
  # Log level: DEBUG, INFO, WARNING, ERROR
  level: INFO
  # Enable logging of SQL queries
  log-queries: false
  # Log slow queries
  log-slow-queries: true
```

## Usage

### Basic Usage

```java
// Get the plugin instance
SqlBridgePlugin plugin = (SqlBridgePlugin) Bukkit.getPluginManager().getPlugin("SQL-Bridge");

// Get the database
Database database = plugin.getDatabase();

// Execute a query
List<Map<String, Object>> results = database.query(
    "SELECT * FROM players WHERE level > ?",
    row -> row,
    10
);

// Process results
for (Map<String, Object> row : results) {
    String playerName = (String) row.get("name");
    int playerLevel = ((Number) row.get("level")).intValue();
    // Do something with the data
}
```

### Query Builder

```java
// Get the query builder
QueryBuilder builder = database.createQueryBuilder();

// Build a SELECT query
String sql = builder
    .select("p.name", "p.level", "g.name AS guild_name")
    .from("players p")
    .join("guilds g", "p.guild_id = g.id")
    .where("p.level", ">", 20)
    .orderBy("p.level", "DESC")
    .limit(10)
    .build();

// Execute the query
List<Map<String, Object>> results = database.query(
    sql,
    row -> row,
    builder.getParameters()
);
```

### Transactions

```java
// Execute a transaction
boolean success = database.transaction(connection -> {
    // Execute multiple operations in a transaction
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
```

## API Documentation

### Key Classes

- `SqlBridgePlugin`: Main plugin class
- `Database`: Interface for database operations
- `DatabaseImpl`: Implementation of the Database interface
- `ConnectionManager`: Manages database connections
- `QueryBuilder`: Builds SQL queries with a fluent API
- `PerformanceMonitor`: Monitors database performance
- `DatabaseMetricsCollector`: Collects database-specific metrics

### Database Interface

The `Database` interface provides the following methods:

- `query(String sql, RowMapper<T> mapper, Object... params)`: Execute a query and map results
- `queryFirst(String sql, RowMapper<T> mapper, Object... params)`: Execute a query and map the first result
- `update(String sql, Object... params)`: Execute an update query
- `insert(String sql, Object... params)`: Execute an insert query and return generated keys
- `batchUpdate(String sql, List<Object[]> batchParams)`: Execute a batch update
- `transaction(TransactionCallback<T> callback)`: Execute operations in a transaction
- `createQueryBuilder()`: Create a query builder

## Troubleshooting

### Common Issues

1. **Connection Refused**: Check that your database server is running and accessible
2. **Authentication Failed**: Verify username and password in the configuration
3. **Table Not Found**: Ensure that your database schema is correctly set up
4. **Performance Issues**: Check the monitoring dashboard for slow queries

### Enabling Debug Logging

To enable detailed logging for troubleshooting:

1. Set `logging.level: DEBUG` in the configuration
2. Set `logging.log-queries: true` to log all SQL queries
3. Restart the server
4. Check the server logs for detailed information

## Contributing

Contributions are welcome! Here's how you can contribute:

1. Fork the repository
2. Create a feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contact

For support or questions, open an issue on the GitHub repository or contact us at support@example.com.