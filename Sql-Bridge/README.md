# SQL-Bridge

A lightweight, high-performance database abstraction layer for Minecraft plugins.

SQL-Bridge provides a simple, consistent API for database operations in Minecraft plugins. It handles connection pooling, query building, and result mapping while offering both synchronous and asynchronous operations.

## Features

* Multiple database support (MySQL, SQLite, PostgreSQL)
* Connection pooling with HikariCP
* Fluent query builder API
* Type-safe result mapping
* Asynchronous operations
* Transaction support
* Schema migrations
* UUID convenience methods for Minecraft

## Installation

### As a Server Owner

1. Download [Core-Utils.jar](https://github.com/Craftingkilla1/Core-Utils/releases) and [SQL-Bridge.jar](https://github.com/Craftingkilla1/SQL-Bridge/releases)
2. Place both jars in your server's `plugins` folder
3. Start your server to generate the configuration file
4. Configure in `plugins/SQL-Bridge/config.yml`

### As a Developer

#### Maven
```xml
<repositories>
    <repository>
        <id>minecraft-repo</id>
        <url>https://repo.example.com/minecraft</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.minecraft</groupId>
        <artifactId>core-utils</artifactId>
        <version>1.0.6</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>com.minecraft</groupId>
        <artifactId>sql-bridge</artifactId>
        <version>1.0.9</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

#### Gradle
```groovy
repositories {
    maven { url 'https://repo.example.com/minecraft' }
}

dependencies {
    compileOnly 'com.minecraft:core-utils:1.0.6'
    compileOnly 'com.minecraft:sql-bridge:1.0.9'
}
```

Add to your plugin.yml:
```yaml
depend: [Core-Utils, SQL-Bridge]
# or if optional:
softdepend: [Core-Utils, SQL-Bridge]
```

## Basic Usage

```java
// Get the database service
DatabaseService databaseService = ServiceRegistry.getService(DatabaseService.class);
Database database = databaseService.getDatabaseForPlugin(this);

// Define a mapper for your data type
ResultMapper<Player> playerMapper = row -> {  // IMPORTANT: Use ResultRow, not ResultSet!
    Player player = new Player();
    player.setId(row.getInt("id"));
    player.setName(row.getString("name"));
    player.setUuid(UUID.fromString(row.getString("uuid")));
    return player;
};

// Find a player by UUID
Optional<Player> player = database.findByUuid(
    "SELECT * FROM players WHERE uuid = ?",
    playerMapper,
    uuid  // UUID convenience method handles conversion
);

// Query all players
List<Player> allPlayers = database.query(
    "SELECT * FROM players",
    playerMapper
);

// Async operations
database.findByUuidAsync(
    "SELECT * FROM players WHERE uuid = ?", 
    playerMapper,
    uuid
).thenAccept(optPlayer -> {
    // Process player data
});
```

## Working with Query Builders

```java
// Select with conditions
List<Player> highLevelPlayers = database.select()
    .columns("id", "name", "uuid", "level")
    .from("players")
    .where("level > ?", 50)
    .orderBy("level DESC")
    .limit(10)
    .executeQuery(playerMapper);

// Insert data
database.insertInto("players")
    .columns("name", "uuid", "level")
    .values(playerName, uuid.toString(), 1)
    .executeUpdate();
```

## Common Pitfalls

1. **ResultRow vs ResultSet**: Always use `ResultRow` in your mappers, not `ResultSet`.

```java
// CORRECT
ResultMapper<Player> mapper = row -> {  // Using ResultRow
    // mapping code...
};

// WRONG
ResultMapper<Player> mapper = rs -> {  // Using ResultSet
    // This will fail!
};
```

2. **UUID Handling**: Use the UUID convenience methods or convert to strings.

```java
// PREFERRED: Use convenience methods
database.findByUuid("SELECT * FROM players WHERE uuid = ?", mapper, uuid);

// ALTERNATIVE: Convert to string
database.queryFirst("SELECT * FROM players WHERE uuid = ?", mapper, uuid.toString());
```

See [GUIDE.md](GUIDE.md) for detailed examples, best practices, and more detailed information about SQL-Bridge's features.

## Configuration

SQL-Bridge creates a default configuration file at `plugins/SQL-Bridge/config.yml`.

```yaml
# Database configuration
database:
  # Database type: MYSQL, SQLITE, POSTGRESQL
  type: sqlite
  
  # MySQL configuration
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: root
    password: password
    
  # SQLite configuration
  sqlite:
    file: database.db
    wal-mode: true
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.