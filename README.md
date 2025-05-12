# Minecraft Plugins Monorepo

A comprehensive collection of interconnected Minecraft plugins designed to work together while maintaining independent versioning and distribution.

## 📦 Plugins

This monorepo contains the following plugins:

### Core-Utils

Core utilities and service framework for Minecraft plugins. Provides:
- Service registry system for plugin interoperability
- Annotation-based command framework
- Configuration management utilities
- Common utility classes

### SQL-Bridge

High-performance database connectivity plugin built on top of Core-Utils. Features:
- Multi-database support (MySQL, PostgreSQL, SQLite, H2)
- Connection pooling with HikariCP
- Query builder with fluent API
- Transaction and batch operation support
- Schema migration system
- SQL injection prevention
- Performance monitoring and metrics
- BungeeCord support for cross-server database sharing

### Example-Plugin

Reference implementation demonstrating how to use both Core-Utils and SQL-Bridge. Includes:
- Service implementation examples
- Command registration examples
- Database operations with SQL-Bridge
- Best practices for plugin development

## 🚀 Installation

1. Download the latest release JARs for each plugin you want to use
2. Place the JARs in your server's `plugins` directory
3. Start or restart your server
4. Configure the plugins using their respective configuration files

### Dependencies

- Core-Utils: No dependencies
- SQL-Bridge: Requires Core-Utils
- Example-Plugin: Requires Core-Utils, optional dependency on SQL-Bridge

## 📋 Usage

### For Server Administrators

Each plugin has its own configuration files and commands:

#### Core-Utils
```
/coreutils reload - Reload configuration
/coreutils info - Show plugin information
/coreutils services - List registered services
```

#### SQL-Bridge
```
/sqlbridge info - Display plugin information
/sqlbridge status - Check database connection status
/sqlbridge test - Test database connectivity
/sqlbridge migrations - Show migration status
/sqlbridge errors - View recent database errors
```

#### Example-Plugin
```
/example help - Show command help
/exampledb - Access database features
```

### For Plugin Developers

#### Using Core-Utils
```java
// Register a service
ServiceRegistry.register(MyService.class, myServiceImpl);

// Get a registered service
MyService service = ServiceRegistry.getService(MyService.class);

// Register commands with annotations
@Command(name = "mycommand", description = "My custom command")
public class MyCommand {
    @SubCommand(name = "test", description = "Test subcommand")
    public void testCommand(CommandSender sender, String[] args) {
        // Command implementation
    }
}
```

#### Using SQL-Bridge
```java
// Get database service
DatabaseService dbService = ServiceRegistry.getService(DatabaseService.class);
Database db = dbService.getDatabaseForPlugin(yourPlugin.getName());

// Execute queries with result mapping
List<Player> players = db.query(
    "SELECT * FROM players WHERE level > ?",
    row -> {
        Player player = new Player();
        player.setId(row.getInt("id"));
        player.setName(row.getString("name"));
        player.setLevel(row.getInt("level"));
        return player;
    },
    10
);

// Use the query builder API
int rowsAffected = db.update("players")
    .set("level", 5)
    .where("name = ?", "PlayerName")
    .executeUpdate();
```

## ⚙️ Configuration

Each plugin has its own configuration files in the `plugins/PluginName` directory:

### Core-Utils
```yaml
# config.yml
debug: false
```

### SQL-Bridge
```yaml
# config.yml
database:
  type: MYSQL    # MYSQL, SQLITE, POSTGRESQL, H2
  
  # MySQL configuration
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: user
    password: pass
    auto-create-database: true
    
    # Connection pool settings
    pool:
      maximum-pool-size: 10
      minimum-idle: 5
    
  # SQLite configuration
  sqlite:
    file: database.db
    wal-mode: true

# Performance monitoring
monitoring:
  enabled: true
  slow-query-threshold: 1000
```

### Example-Plugin
```yaml
# config.yml
features:
  example_feature_1: true
  database_integration: true
```

## 📄 License

Each plugin is licensed under the MIT License - see individual plugin directories for details.

## 📚 Documentation

- Check each plugin's dedicated README.md for detailed information
- For version history and changes, see the [Releases](../../releases) page