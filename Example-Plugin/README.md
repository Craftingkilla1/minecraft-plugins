# PlayerStats Plugin

A comprehensive player statistics tracking plugin that demonstrates the integration of Core-Utils and SQL-Bridge functionality.

## Overview

PlayerStats tracks a wide range of player statistics, from blocks broken to distance traveled, and awards achievements based on these statistics. It also features leaderboards to encourage competition among players.

This plugin serves as a practical example of how to use the Core-Utils and SQL-Bridge libraries in your own plugins, demonstrating:

- Service-based architecture using the Service Registry system
- Annotation-based command framework
- Database integration with SQL-Bridge
- GUI creation with InventoryUtil
- Configuration management

## Features

- **Extensive Statistics Tracking**: Automatically records player activities including blocks broken/placed, combat, movement, and more
- **Achievement System**: Awards achievements when players meet specific criteria
- **Leaderboards**: Shows top players for different statistics
- **Player Commands**: View personal and other players' statistics
- **Admin Commands**: Manage statistics, achievements, and leaderboards
- **Interactive GUI**: Browse statistics and achievements with an intuitive menu interface
- **SQLite Database**: Persistent storage of all player data

## Commands

### Player Commands

- `/stats` - Opens the statistics menu
- `/stats show` - Shows your statistics in chat
- `/stats show <player>` - Shows another player's statistics
- `/stats compare <player>` - Compares your statistics with another player
- `/stats top <stat> [limit]` - Shows top players for a specific statistic
- `/stats help` - Shows help information

### Admin Commands

- `/admin reset <player> [stat]` - Resets statistics for a player
- `/admin modify <player> <stat> <value>` - Modifies a statistic value for a player
- `/admin achievement <list|reset> [player]` - Manages achievements
- `/admin maintenance` - Performs database maintenance tasks

### Leaderboard Commands

- `/leaderboard list` - Lists all available leaderboards
- `/leaderboard view <id>` - Views a specific leaderboard
- `/leaderboard create <id> <displayName> <statName>` - Creates a new leaderboard
- `/leaderboard delete <id>` - Deletes a leaderboard
- `/leaderboard update <id>` - Manually updates a leaderboard

## Permissions

- `playerstats.view.others` - Allows viewing other players' statistics
- `playerstats.admin` - Allows access to administrative commands
- `playerstats.reset` - Allows resetting player statistics
- `playerstats.modify` - Allows modifying player statistics
- `playerstats.leaderboard.manage` - Allows managing leaderboards

## Core-Utils Integration

This plugin demonstrates the following Core-Utils features:

### Service Registry System

```java
// Service registration
ServiceRegistry.register(StatsService.class, statsService);
ServiceRegistry.register(AchievementService.class, achievementService);
ServiceRegistry.register(LeaderboardService.class, leaderboardService);

// Service consumption
StatsService statsService = ServiceRegistry.getService(StatsService.class);
```

### Annotation-based Command Framework

```java
@Command(name = "stats", description = "View player statistics", aliases = {"playerstats", "pstats"})
public class StatsCommand implements TabCompletionProvider {
    
    @SubCommand(name = "show", description = "View stats in chat format")
    public void showCommand(CommandSender sender, String[] args) {
        // Command implementation
    }
    
    @SubCommand(name = "compare", description = "Compare stats with another player", minArgs = 1)
    @Permission(value = "playerstats.view.others", message = "You don't have permission to compare stats.")
    public void compareCommand(CommandSender sender, String[] args) {
        // Command implementation
    }
}
```

### Configuration Management

```java
// Loading configuration
this.configManager = new ConfigManager(this);
this.pluginConfig = new PluginConfig(this, configManager);
this.messageConfig = new MessageConfig(this, configManager);

// Using messages
messageConfig.sendMessage(player, "welcome");
messageConfig.sendSuccessMessage(player, "stats.modified", "player", targetName, "stat", statName, "value", String.valueOf(value));
```

### Utility Classes

```java
// LogUtil example
LogUtil.init(this);
LogUtil.info("PlayerStats plugin enabled successfully!");
LogUtil.debug("Processing batched stats for " + statBatch.size() + " players");

// FormatUtil example
String formattedNumber = FormatUtil.formatNumber(value);
String displayName = FormatUtil.capitalizeWords(statName.replace('_', ' '));

// InventoryUtil example
ItemStack item = InventoryUtil.createItem(material, 
        ChatColor.YELLOW + displayName,
        ChatColor.WHITE + "Value: " + ChatColor.GOLD + FormatUtil.formatNumber(value));
```

## SQL-Bridge Integration

This plugin demonstrates the following SQL-Bridge features:

### Database Setup

```java
// Get the database service from SQL-Bridge
DatabaseService databaseService = ServiceRegistry.getService(DatabaseService.class);
this.database = databaseService.getDatabaseForPlugin(plugin.getName());

// Register database migrations
List<Migration> migrations = new ArrayList<>();
migrations.add(new CreateInitialTables());
migrations.add(new AddIndexesMigration());
databaseService.registerMigrations(plugin.getName(), migrations);
```

### Database Queries

```java
// Simple query example
int value = database.queryFirst(
        "SELECT stat_value FROM stats WHERE player_id = ? AND stat_name = ?",
        rs -> rs.getInt("stat_value"),
        playerId, statName
).orElse(0);

// Query with list result
List<Achievement> achievements = database.query(
        "SELECT a.achievement_id, a.name, a.description, a.icon_material, a.category, " +
        "a.secret, a.criteria_json, pa.earned_date " +
        "FROM achievements a " +
        "JOIN player_achievements pa ON a.achievement_id = pa.achievement_id " +
        "WHERE pa.player_id = ?",
        this::mapAchievement,
        playerId
);
```

### Asynchronous Database Operations

```java
// Asynchronous query
return database.queryFirstAsync(
        "SELECT stat_value FROM stats WHERE player_id = ? AND stat_name = ?",
        rs -> rs.getInt("stat_value"),
        playerId, statName
).thenApply(optional -> optional.orElse(0));

// Asynchronous update
return database.updateAsync(
        "UPDATE players SET name = ?, last_seen = ?, playtime = ? WHERE uuid = ?",
        playerData.getName(),
        Timestamp.valueOf(playerData.getLastSeen()),
        playerData.getPlaytime(),
        playerData.getUuid().toString()
).thenApply(rowsAffected -> rowsAffected > 0);
```

### Batch Operations

```java
// Batch update
List<Object[]> batchParams = new ArrayList<>();
for (Map.Entry<String, Integer> entry : playerData.getStats().entrySet()) {
    batchParams.add(new Object[]{
            playerId,
            entry.getKey(),
            entry.getValue(),
            now,
            playerId,
            entry.getKey()
    });
}

database.batchUpdate(
        "INSERT INTO stats (player_id, stat_name, stat_value, last_updated) " +
        "VALUES (?, ?, ?, ?) " +
        "ON CONFLICT (player_id, stat_name) " +
        "DO UPDATE SET stat_value = ?, last_updated = ?",
        batchParams
);
```

### Transactions

```java
database.executeTransaction(connection -> {
    try (PreparedStatement stmt1 = connection.prepareStatement(
            "INSERT INTO leaderboard_entries (leaderboard_id, player_id, rank, score, update_time) " +
            "VALUES (?, ?, ?, ?, ?)")) {
        // Transaction operations
        
        return true; // Commit transaction
    }
});
```

## Configuration

The plugin includes several configuration files:

- `config.yml` - General plugin settings
- `messages.yml` - Customizable messages
- `achievements.yml` - Achievement definitions

## Database Schema

The plugin uses the following database tables:

- `players` - Player information (UUID, name, join date, etc.)
- `stats` - Player statistics
- `achievements` - Achievement definitions
- `player_achievements` - Tracks which players have earned which achievements
- `leaderboards` - Leaderboard definitions
- `leaderboard_entries` - Entries in leaderboards

## Dependencies

- Core-Utils - Service registry, commands, configuration, and utilities
- SQL-Bridge - Database connectivity and operations
- Spigot/Bukkit API - Minecraft server API

## License

This plugin is licensed under the MIT License.