# Example-Plugin

A reference implementation demonstrating the functionality of Core-Utils and SQL-Bridge in a Minecraft plugin.

## 🚀 Features

This plugin serves as a comprehensive example of how to use both Core-Utils and SQL-Bridge in your own plugins, including:

### Core-Utils Showcase
- **Service Registry System**: Demonstrates registering and consuming services
- **Command Framework**: Showcases annotation-based command creation
- **Configuration Management**: Shows how to manage plugin configuration effectively
- **Utility Classes**: Examples of various utility classes in action

### SQL-Bridge Showcase
- **Database Operations**: Examples of basic and advanced database operations
- **Data Access Objects (DAOs)**: Demonstrates the DAO pattern implementation
- **Migration System**: Shows how to handle schema migrations
- **Query Builder API**: Examples of using the fluent query builder

### Integrated Features
- **Player Statistics**: Tracks and displays various player statistics
- **Achievement System**: Awards and tracks player achievements
- **Admin Tools**: Administrative commands and tools for server management

## 📋 Requirements

- Spigot/Paper server 1.16.5 or higher
- Core-Utils plugin
- SQL-Bridge plugin (optional, but recommended for full functionality)

## 📥 Installation

1. Download the latest versions of Core-Utils, SQL-Bridge, and Example-Plugin
2. Place all three JAR files in your server's `plugins` directory
3. Start or restart your server
4. The plugins will generate their configuration files automatically

## ⚙️ Configuration

Configuration files are located in the `plugins/Example-Plugin` directory:

### config.yml
Controls the main plugin settings:
```yaml
# Plugin features
features:
  database_integration: true  # Enable/disable database features
  stat_tracking: true         # Enable/disable stat tracking
  achievements: true          # Enable/disable achievements
  admin_tools: true           # Enable/disable admin tools

# Statistics tracking settings
statistics:
  save_interval: 300          # Auto-save interval in seconds
  track_blocks_broken: true   # Track blocks broken
  # ... other tracking options
```

### messages.yml
Contains all messages displayed by the plugin:
```yaml
prefix: "&8[&bExample&8] &r"
# ... various messages
```

## 📝 Commands

### Main Commands
- `/example` - Shows plugin information
  - `/example help` - Shows help information
  - `/example reload` - Reloads configuration
  - `/example version` - Shows plugin version
  - `/example info` - Shows detailed information
  - `/example debug` - Toggles debug mode
  - `/example database` - Shows database information

### Statistics Commands
- `/stats` - Shows your statistics
  - `/stats <player>` - Shows another player's statistics
  - `/stats reset [player]` - Resets statistics
  - `/stats top <stat> [limit]` - Shows top players for a stat

### Achievement Commands
- `/achievements` - Shows your achievements
  - `/achievements <player>` - Shows another player's achievements
  - `/achievements list` - Lists all available achievements
  - `/achievements award <player> <achievement>` - Awards an achievement
  - `/achievements recent [limit]` - Shows recently earned achievements
  - `/achievements check [player]` - Checks for new achievements

### Admin Commands
- `/admin` - Shows admin menu
  - `/admin stats` - Shows database statistics
  - `/admin export <type>` - Exports data to files
  - `/admin purge <days> [confirm]` - Purges inactive players
  - `/admin backup` - Backs up database data
  - `/admin repair` - Repairs database issues

## 🔧 Permissions

The plugin uses the following permission nodes:

### General Permissions
- `exampleplugin.use` - Access to basic commands (default: true)
- `exampleplugin.admin` - Access to admin commands (default: op)

### Stats Permissions
- `exampleplugin.stats` - Access to stats commands (default: true)
- `exampleplugin.stats.others` - View other players' stats (default: op)
- `exampleplugin.stats.reset` - Reset statistics (default: op)

### Achievement Permissions
- `exampleplugin.achievements` - Access to achievement commands (default: true)
- `exampleplugin.achievements.others` - View other players' achievements (default: op)
- `exampleplugin.achievements.award` - Award achievements (default: op)

## 💻 Developer Guide

### Service Implementation

This example demonstrates how to define a service interface:
```java
// Define the service interface
public interface StatsService {
    boolean incrementStat(Player player, String statName, int value);
    // ... other methods
}

// Implement the service
public class DefaultStatsService implements StatsService {
    // ... implementation
}

// Register the service
CoreAPI.Services.register(StatsService.class, new DefaultStatsService(this));

// Consume the service elsewhere
StatsService statsService = CoreAPI.Services.get(StatsService.class);
if (statsService != null) {
    statsService.incrementStat(player, "blocks_broken", 1);
}
```

### Command Implementation

Creating commands using the annotation framework:
```java
@Command(name = "example", description = "Main command for Example-Plugin", aliases = {"exp"})
@Permission("exampleplugin.use")
public class ExampleCommand implements TabCompletionProvider {
    
    @SubCommand(description = "Shows plugin information")
    public void defaultCommand(CommandSender sender, String[] args) {
        // Implementation for /example
    }
    
    @SubCommand(name = "reload", description = "Reloads the plugin")
    @Permission("exampleplugin.admin")
    public void reloadCommand(CommandSender sender, String[] args) {
        // Implementation for /example reload
    }
    
    // Tab completion implementation
    @Override
    public List<String> getTabCompletions(String subCommand, CommandSender sender, String[] args) {
        // Implementation
    }
}
```

### Database Operations

Example of using the SQL-Bridge DAO pattern:
```java
// Example of finding a player by UUID
public Optional<Player> findByUuid(UUID uuid) {
    try {
        return database.findByUuid(
            "SELECT * FROM players WHERE uuid = ?",
            playerMapper,
            uuid
        );
    } catch (SQLException e) {
        LogUtil.severe("Error finding player by UUID: " + e.getMessage());
        return Optional.empty();
    }
}

// Example of using the query builder
public List<PlayerStats> getTopPlayers(String statName, int limit) {
    try {
        return database.select()
            .columns("ps.*")
            .from("player_stats ps")
            .join("players p", "ps.player_id = p.id")
            .orderBy("ps." + statName + " DESC")
            .limit(limit)
            .executeQuery(statsMapper);
    } catch (SQLException e) {
        LogUtil.severe("Error getting top players: " + e.getMessage());
        return new ArrayList<>();
    }
}
```

## 🛠️ Contributing

This plugin is intended as a reference implementation. If you find bugs or have suggestions for improvements, please open an issue or pull request on the GitHub repository.

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.