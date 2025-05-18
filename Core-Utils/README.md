// ./Core-Utils/README.md
# Core-Utils Developer Documentation

## Table of Contents
1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
   - [Project Setup](#project-setup)
   - [Adding Core-Utils as a Dependency](#adding-core-utils-as-a-dependency)
   - [Quick Start with CoreAPI](#quick-start-with-coreapi)
3. [Service Registry System](#service-registry-system)
   - [Creating a Service](#creating-a-service)
   - [Registering a Service](#registering-a-service)
   - [Consuming a Service](#consuming-a-service)
   - [Service Lifecycle](#service-lifecycle)
4. [Command Framework](#command-framework)
   - [Creating Commands](#creating-commands)
   - [Command Annotations](#command-annotations)
   - [Default Commands](#default-commands)
   - [Registering Commands](#registering-commands)
   - [Tab Completion](#tab-completion)
5. [Configuration Management](#configuration-management)
   - [Loading Configuration](#loading-configuration)
   - [Using the Messages System](#using-the-messages-system)
6. [Utility Classes](#utility-classes)
   - [LogUtil](#logutil)
   - [TimeUtil](#timeutil)
   - [InventoryUtil](#inventoryutil)
   - [FormatUtil](#formatutil)
   - [BungeeUtils](#bungeeutils)
7. [Common Issues and Solutions](#common-issues-and-solutions)
   - [Type Import Conflicts](#type-import-conflicts)
   - [Java Version Compatibility](#java-version-compatibility)
   - [Method Signature Alternatives](#method-signature-alternatives)
8. [Best Practices](#best-practices)
9. [Example Plugin](#example-plugin)

## Introduction

Core-Utils is a foundation plugin that provides common functionality for our modular plugin architecture. It offers several key features:

- **Service Registry**: A system for plugins to register and consume services from each other
- **Command Framework**: An annotation-based command system that simplifies command creation
- **Configuration Management**: Tools for managing plugin configuration files
- **Utility Classes**: A collection of utility classes for common operations

This document provides a comprehensive guide on how to use Core-Utils in your plugin development.

## Getting Started

### Project Setup

To create a new plugin that uses Core-Utils, you'll need to set up a Gradle or Maven project.

#### Gradle Project Structure
```
your-plugin/
├── build.gradle
├── settings.gradle
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── your/
        │           └── plugin/
        │               └── YourPlugin.java
        └── resources/
            └── plugin.yml
```

### Adding Core-Utils as a Dependency

#### Using Gradle

In your `build.gradle` file:

```groovy
plugins {
    id 'java'
    id 'com.github.johnrengelman.shadow' version '7.1.2'
}

repositories {
    mavenCentral()
    // Add Maven Local repository to find Core-Utils
    mavenLocal()
    // Spigot repository
    maven { url = 'https://hub.spigotmc.org/nexus/content/repositories/snapshots/' }
    // Core-Utils repository (if used)
    maven { url = uri('../Core-Utils/build/repo') }
}

dependencies {
    // Bukkit/Spigot API
    compileOnly 'org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT'
    
    // Core-Utils dependency
    compileOnly 'com.minecraft:core-utils:1.0.0'
}
```

#### Using Maven

In your `pom.xml` file:

```xml
<dependencies>
    <!-- Spigot API -->
    <dependency>
        <groupId>org.spigotmc</groupId>
        <artifactId>spigot-api</artifactId>
        <version>1.16.5-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- Core-Utils -->
    <dependency>
        <groupId>com.minecraft</groupId>
        <artifactId>core-utils</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

#### plugin.yml

Make sure to add Core-Utils as a dependency in your `plugin.yml`:

```yaml
name: YourPlugin
version: 1.0.0
main: com.your.plugin.YourPlugin
api-version: '1.16'
depend: [CoreUtils]
```

### Quick Start with CoreAPI

CoreAPI is a centralized access point for all Core-Utils features. It makes it easy to use the framework in your plugins:

```java
import com.minecraft.core.api.CoreAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class YourPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // Initialize logging
        CoreAPI.Utils.initLogging(this);
        
        // Register commands
        CoreAPI.Commands.register(new YourCommand());
        
        // Register a service
        CoreAPI.Services.register(YourService.class, new YourServiceImpl());
        
        getLogger().info("Your plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Unregister services
        CoreAPI.Services.unregister(YourService.class);
        
        getLogger().info("Your plugin disabled successfully!");
    }
}
```

## Service Registry System

The Service Registry allows plugins to register and consume services from each other, creating a modular architecture.

### Creating a Service

1. Define a service interface in your plugin:

```java
public interface YourService {
    String performTask(String input);
    boolean isFeatureEnabled(String featureName);
    // other methods...
}
```

2. Implement the service:

```java
public class DefaultYourService implements YourService {
    private final YourPlugin plugin;
    
    public DefaultYourService(YourPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String performTask(String input) {
        // Implementation
        return input.toUpperCase();
    }
    
    @Override
    public boolean isFeatureEnabled(String featureName) {
        // Implementation
        return plugin.getConfig().getBoolean("features." + featureName, false);
    }
    
    // Implement other methods...
}
```

### Registering a Service

Using CoreAPI (recommended):

```java
// Create the service implementation
YourService service = new DefaultYourService(this);

// Register the service with the registry
CoreAPI.Services.register(YourService.class, service);
```

Using ServiceRegistry directly:

```java
import com.minecraft.core.api.service.ServiceRegistry;

@Override
public void onEnable() {
    // Create the service implementation
    YourService service = new DefaultYourService(this);
    
    // Register the service with the registry
    ServiceRegistry.register(YourService.class, service);
    
    // Other initialization...
}
```

### Consuming a Service

Using CoreAPI (recommended):

```java
// Option 1: Get service, check if it exists
YourService service = CoreAPI.Services.get(YourService.class);
if (service != null) {
    // Use the service
    String result = service.performTask("hello");
}

// Option 2: Require service (throws exception if not found)
try {
    YourService service = CoreAPI.Services.require(YourService.class);
    // Use the service
    String result = service.performTask("hello");
} catch (ServiceLocator.ServiceNotFoundException e) {
    getLogger().warning("Required service not found: " + e.getMessage());
}
```

Using ServiceLocator directly:

```java
import com.minecraft.core.api.service.ServiceLocator;
import com.minecraft.core.api.service.ServiceLocator.ServiceNotFoundException;

// Option 1: Get service, check if it exists
YourService service = ServiceLocator.getService(YourService.class);
if (service != null) {
    // Use the service
    String result = service.performTask("hello");
}

// Option 2: Require service (throws exception if not found)
try {
    YourService service = ServiceLocator.requireService(YourService.class);
    // Use the service
    String result = service.performTask("hello");
} catch (ServiceNotFoundException e) {
    getLogger().warning("Required service not found: " + e.getMessage());
}
```

### Service Lifecycle

Services should be registered during your plugin's `onEnable()` method and unregistered during `onDisable()`:

```java
@Override
public void onDisable() {
    // Unregister the service
    CoreAPI.Services.unregister(YourService.class);
    
    // Other cleanup...
}
```

## Command Framework

The Command Framework simplifies creation and management of commands using annotations.

### Creating Commands

Create a command class with the `@Command` annotation:

```java
import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.TabCompletionProvider;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "yourcommand", description = "Your command description", aliases = {"yc"})
public class YourCommand implements TabCompletionProvider {

    private final YourPlugin plugin;
    
    public YourCommand(YourPlugin plugin) {
        this.plugin = plugin;
    }
    
    @SubCommand(name = "help", description = "Show help information")
    public void helpCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "=== Your Plugin Help ===");
        // Send help messages
    }
    
    @SubCommand(name = "reload", description = "Reload the configuration", permission = "yourplugin.reload")
    public void reloadCommand(CommandSender sender, String[] args) {
        plugin.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
    }
    
    @SubCommand(name = "set", description = "Set a value", minArgs = 2, maxArgs = 2, permission = "yourplugin.set")
    @Permission(value = "yourplugin.admin", message = "You do not have permission to use this command.")
    public void setCommand(CommandSender sender, String[] args) {
        String key = args[0];
        String value = args[1];
        
        // Do something with key and value
        sender.sendMessage(ChatColor.GREEN + "Set " + key + " to " + value);
    }
    
    @Override
    public List<String> getTabCompletions(String subCommand, CommandSender sender, String[] args) {
        // Provide tab completions for specific subcommands
        if (subCommand.equalsIgnoreCase("set") && args.length == 1) {
            return Arrays.asList("option1", "option2", "option3");
        }
        
        return new ArrayList<>();
    }
}
```

### Command Annotations

- **@Command**: Class-level annotation for the main command
  - `name`: The command name (required)
  - `description`: Command description
  - `aliases`: Alternative command names
  
- **@SubCommand**: Method-level annotation for subcommands
  - `name`: The subcommand name (required unless isDefault=true)
  - `description`: Subcommand description
  - `permission`: Permission node required
  - `aliases`: Alternative subcommand names
  - `minArgs`: Minimum number of arguments required
  - `maxArgs`: Maximum number of arguments allowed (-1 for unlimited)
  - `isDefault`: Whether this is the default subcommand (invoked when no subcommand name is provided)
  
- **@Permission**: Can be applied to classes or methods
  - `value`: The permission node required
  - `message`: Custom message when permission is denied

### Default Commands

You can define a default command that is executed when no subcommand is specified:

```java
@Command(name = "yourcommand", description = "Your command description")
public class YourCommand {

    // This will be executed when the user types just "/yourcommand" with no subcommand
    @SubCommand(isDefault = true, description = "Default command")
    public void defaultCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "This is the default command!");
    }
    
    // Other subcommands...
}
```

Or using an empty name (which automatically sets isDefault):

```java
@SubCommand(name = "", description = "Default command")
public void defaultCommand(CommandSender sender, String[] args) {
    sender.sendMessage(ChatColor.GREEN + "This is the default command!");
}
```

### Registering Commands

With CoreAPI (recommended):

```java
// Create your command
YourCommand yourCommand = new YourCommand(this);

// Register your command
CoreAPI.Commands.register(yourCommand);
```

With CommandRegistry directly:

```java
import com.minecraft.core.CorePlugin;
import com.minecraft.core.command.CommandRegistry;

@Override
public void onEnable() {
    // Get CorePlugin instance
    Plugin corePlugin = getServer().getPluginManager().getPlugin("CoreUtils");
    if (corePlugin instanceof CorePlugin) {
        CorePlugin core = (CorePlugin) corePlugin;
        
        // Get command registry
        CommandRegistry commandRegistry = core.getCommandRegistry();
        
        // Register your command
        YourCommand yourCommand = new YourCommand(this);
        commandRegistry.registerCommand(yourCommand);
    }
}
```

### Tab Completion

To provide tab completions, implement the `TabCompletionProvider` interface in your command class.

## Configuration Management

Core-Utils provides tools for managing plugin configurations.

### Loading Configuration

```java
import com.minecraft.core.config.ConfigManager;

// Option 1: Using CoreAPI
ConfigManager configManager = CoreAPI.getConfigManager();

// Option 2: Create a config manager for your plugin
ConfigManager configManager = new ConfigManager(yourPlugin);

// Load the main config
configManager.loadConfig();
FileConfiguration config = configManager.getConfig();

// Load custom config files
YamlConfiguration customConfig = configManager.loadConfigFile("custom");

// Access configuration values
String value = config.getString("some.path", "default");
boolean enabled = config.getBoolean("feature.enabled", false);
```

### Using the Messages System

The Messages system provides several constructors to accommodate different needs:

```java
import com.minecraft.core.config.Messages;
import org.bukkit.configuration.file.YamlConfiguration;

// Option 1: Default messages file (messages.yml)
Messages messages = new Messages(plugin);

// Option 2: Custom messages file name
Messages messages = new Messages(plugin, "custom-messages");

// Option 3: Use a pre-loaded configuration
YamlConfiguration messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
Messages messages = new Messages(plugin, messagesConfig);

// Option 4: Use a pre-loaded configuration with custom prefix
Messages messages = new Messages(plugin, messagesConfig, "&8[&bCustom&8] &r");

// Send messages to players
messages.sendMessage(player, "welcome");
messages.sendSuccessMessage(player, "item.purchased", "item", "Diamond Sword", "price", "100");

// Format messages for use elsewhere
String formatted = messages.getFormattedMessage("welcome");

// Messages with replacements
Map<String, String> replacements = new HashMap<>();
replacements.put("player", player.getName());
replacements.put("item", "Diamond Sword");
String message = messages.getFormattedMessage("message.key", replacements);

// Get the underlying configuration if needed
FileConfiguration messagesConfig = messages.getConfig();
```

## Utility Classes

Core-Utils provides several utility classes for common operations.

### LogUtil

```java
import com.minecraft.core.utils.LogUtil;

// Initialize with your plugin
LogUtil.init(yourPlugin);

// Basic logging
LogUtil.info("This is an information message");
LogUtil.warning("This is a warning message");
LogUtil.severe("This is a severe error message");
LogUtil.debug("This is a debug message (only shown if debug mode is enabled)");

// Format string logging (new in v1.1.0)
LogUtil.info("Player %s connected from %s", player.getName(), address);
LogUtil.warning("Failed to load file: %s", fileName);
LogUtil.severe("Exception in %s: %s", "TaskManager", e.getMessage());

// Setting debug mode
LogUtil.setDebugMode(true);  // Preferred method
LogUtil.setDebugEnabled(true);  // Alternative method (added for compatibility)

// Setting log level
LogUtil.setLogLevel(Level.FINE);

// Per-plugin logging
LogUtil.PluginLogger logger = LogUtil.forPlugin(yourPlugin, "[YourPlugin] ");
logger.info("This is a plugin-specific message");
logger.info("Player %s connected from %s", player.getName(), address);
```

### TimeUtil

```java
import com.minecraft.core.utils.TimeUtil;
import java.time.LocalDateTime;

// Format time in seconds
String formatted = TimeUtil.formatTime(3665); // "1h 1m 5s"

// Get current time
LocalDateTime now = TimeUtil.now();

// Format date and time
String dateTime = TimeUtil.formatDateTime(now); // "2025-05-06 15:30:45"
String date = TimeUtil.formatDate(now); // "2025-05-06"
String time = TimeUtil.formatTime(now); // "15:30:45"

// Time manipulation
LocalDateTime later = TimeUtil.addHours(now, 2);
LocalDateTime tomorrow = TimeUtil.addDays(now, 1);
```

### InventoryUtil

```java
import com.minecraft.core.utils.InventoryUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;

// Create items
ItemStack item = InventoryUtil.createItem(Material.DIAMOND_SWORD, 
    "&6Special Sword", "&7A very special sword", "&cHandle with care");

// Create player head
ItemStack head = InventoryUtil.createPlayerHead(player, 
    "&b" + player.getName(), "&7Click to view profile");

// Make an item glow
ItemStack glowing = InventoryUtil.makeGlow(item);

// Create an inventory (new in v1.1.0)
Inventory inventory = InventoryUtil.createInventory(null, 54, "&8Example Inventory");

// Fill inventory
InventoryUtil.fillEmptySlots(inventory, 
    InventoryUtil.createDivider(Material.BLACK_STAINED_GLASS_PANE));

// Create a border
InventoryUtil.createBorder(inventory, 54, 
    InventoryUtil.createDivider(Material.GRAY_STAINED_GLASS_PANE));

// Create a confirmation menu
Inventory confirmMenu = InventoryUtil.createConfirmationMenu(
    "Confirm Purchase",
    player -> {
        // Action when Yes is clicked
        player.sendMessage("Purchase confirmed!");
    },
    player -> {
        // Action when No is clicked
        player.sendMessage("Purchase cancelled!");
    }
);

// Create a confirmation inventory with message (new in v1.1.0)
Inventory confirmInventory = InventoryUtil.createConfirmationInventory(
    "&8Confirm Delete",
    "&cAre you sure you want to delete this item?",
    player -> {
        player.sendMessage("Item deleted!");
    },
    player -> {
        player.sendMessage("Delete cancelled!");
    }
);
```

### FormatUtil

```java
import com.minecraft.core.utils.FormatUtil;

// Format text with color codes
String colored = FormatUtil.color("&aThis text is green");

// Format numbers
String number = FormatUtil.formatNumber(1234567.89); // "1,234,567.89"
String percent = FormatUtil.formatPercentage(0.75); // "75%"

// Format boolean
String yesNo = FormatUtil.formatBoolean(true); // "Yes"

// Truncate text
String truncated = FormatUtil.truncate("This is a long text", 10); // "This is a..."
```

### BungeeUtils

```java
import com.minecraft.core.utils.BungeeUtils;

// Create BungeeUtils instance
BungeeUtils bungeeUtils = new BungeeUtils(yourPlugin);

// Register for plugin messages
getServer().getMessenger().registerIncomingPluginChannel(yourPlugin, 
    "BungeeCord", BungeeUtils.createMessageListener(yourPlugin, bungeeUtils));

// Send player to another server
bungeeUtils.transferPlayerToServer(player, "lobby");

// Get server name
bungeeUtils.getServerNameAsync(serverName -> {
    getLogger().info("Current server: " + serverName);
});

// Get player count on a server
bungeeUtils.getPlayerCount("survival", count -> {
    getLogger().info("Players on survival: " + count);
});
```

## Common Issues and Solutions

### Type Import Conflicts

When you have classes with the same name from different packages, you need to use fully qualified names to avoid conflicts:

```java
// Instead of importing both classes
import org.bukkit.entity.Player;
import com.minecraft.example.sql.models.Player;  // This will cause a conflict

// Do this:
import org.bukkit.entity.Player;  // Import only one

// And use fully qualified name for the other
com.minecraft.example.sql.models.Player playerModel = new com.minecraft.example.sql.models.Player();
```

Alternatively, you can use an import alias (Java 10+):

```java
import org.bukkit.entity.Player;
import com.minecraft.example.sql.models.Player as ModelPlayer;

ModelPlayer playerModel = new ModelPlayer();
```

In Java 8, consider renaming your model classes to avoid conflicts:

```java
// Rename your model class
public class PlayerModel {  // Instead of just "Player"
    // ...
}
```

### Java Version Compatibility

Core-Utils is built with Java 8 compatibility in mind. Avoid using Java 9+ features like:

```java
// Java 9+ feature (not compatible)
Map<String, String> map = Map.of("key1", "value1", "key2", "value2");

// Use this instead (Java 8 compatible)
Map<String, String> map = new HashMap<>();
map.put("key1", "value1");
map.put("key2", "value2");
```

For immutable collections, use Guava or other libraries compatible with Java 8:

```java
// Using Guava
import com.google.common.collect.ImmutableMap;

Map<String, String> map = ImmutableMap.of("key1", "value1", "key2", "value2");
```

### Method Signature Alternatives

Some methods have multiple overloads for flexibility:

```java
// LogUtil has both simple and format string versions
LogUtil.info("A simple message");
LogUtil.info("Player %s joined from %s", playerName, address);

// InventoryUtil has multiple ways to create items
ItemStack item1 = InventoryUtil.createItem(Material.DIAMOND_SWORD, "&6Special Sword", "&7Very special");
ItemStack item2 = InventoryUtil.createItem(Material.DIAMOND_SWORD, "&6Special Sword", Arrays.asList("&7Very special", "&8Rare"));

// Messages system offers multiple constructors
Messages messages1 = new Messages(plugin);
Messages messages2 = new Messages(plugin, messagesConfig);
```

## Best Practices

1. **Use CoreAPI**: Always use the CoreAPI class for accessing Core-Utils features. It provides a clean, centralized interface.

```java
// Instead of this:
ServiceRegistry.register(YourService.class, yourService);

// Use this:
CoreAPI.Services.register(YourService.class, yourService);
```

2. **Check for Core-Utils**: Always verify that Core-Utils is available before using its features.

```java
private boolean checkCoreUtils() {
    Plugin plugin = getServer().getPluginManager().getPlugin("CoreUtils");
    if (plugin == null || !(plugin instanceof CorePlugin)) {
        getLogger().severe("CoreUtils not found! This plugin requires CoreUtils to function.");
        getServer().getPluginManager().disablePlugin(this);
        return false;
    }
    return true;
}
```

3. **Handle Missing Services**: Gracefully handle situations where a service isn't available.

```java
AnotherService service = CoreAPI.Services.get(AnotherService.class);
if (service == null) {
    // Fallback behavior or inform the user
    getLogger().warning("AnotherService is not available. Some features will be disabled.");
} else {
    // Use the service
}
```

4. **Unregister Services**: Always unregister your services during your plugin's `onDisable()` method.

5. **Separate Interface and Implementation**: Define service interfaces in a separate package from their implementations.

6. **Document Services**: Clearly document what your services do and what other plugins might need them.

7. **Consistent Command Structure**: Use a consistent command structure across all your plugins.

8. **Use Debug Mode Appropriately**: Use `LogUtil.debug()` for debugging information, not for regular logging.

9. **Avoid Class Name Conflicts**: Use distinct class names or fully qualified names to avoid conflicts.

10. **Maintain Java 8 Compatibility**: Avoid using Java 9+ features to ensure compatibility with all servers.

## Example Plugin

Refer to the Example Plugin for a complete implementation demonstrating all Core-Utils features.
The Example Plugin includes:

- Service registration and consumption
- Command creation using annotations
- Configuration management
- Usage of various utility classes

For a quick API usage reference, see the `ExampleApiUsage` class included with Core-Utils.

You can use this as a template for developing new plugins in the modular architecture.

---

For more information or assistance, contact the development team or refer to the source code documentation.