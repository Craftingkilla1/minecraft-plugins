# Core-Utils Developer Documentation

## Table of Contents
1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
   - [Project Setup](#project-setup)
   - [Adding Core-Utils as a Dependency](#adding-core-utils-as-a-dependency)
   - [Quick Start](#quick-start)
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
    maven { url = uri('../build/repo') }
}

dependencies {
    // Bukkit/Spigot API
    compileOnly 'org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT'
    
    // Option 1: For standalone projects - use version range to get latest compatible version
    compileOnly 'com.minecraft:core-utils:1.+'  // Any 1.x version
    
    // Option 2: For standalone projects - define version in gradle.properties
    // compileOnly "com.minecraft:core-utils:${coreUtilsVersion}"
    
    // Option 3: For multi-module projects - direct project reference
    // compileOnly project(':Core-Utils')
}
```

> **Note:** For Option 2, create a `gradle.properties` file with `coreUtilsVersion=1.0.9` or the current version.
> This allows easier updating of dependencies across your project.


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
        <version>[1.0,2.0)</version> <!-- Use version range to get latest 1.x version -->
        <scope>provided</scope>
    </dependency>
</dependencies>
```

> **Note:** The version range `[1.0,2.0)` means "any version from 1.0 (inclusive) to 2.0 (exclusive)".
> This ensures you always get the latest compatible version of Core-Utils.


#### plugin.yml

Make sure to add Core-Utils as a dependency in your `plugin.yml`:

```yaml
name: YourPlugin
version: 1.0.0
main: com.your.plugin.YourPlugin
api-version: '1.16'
depend: [CoreUtils]
```

### Quick Start

Here's a basic setup for a plugin that uses Core-Utils. There are two main approaches:

#### Approach 1: Using CoreAPI (Recommended)

```java
import com.minecraft.core.api.CoreAPI;
import com.minecraft.core.utils.LogUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class YourPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // Initialize logging
        LogUtil.init(this);
        
        // Check if Core-Utils is available
        if (!checkCoreUtilsAvailable()) {
            return;
        }
        
        // Register commands
        CoreAPI.Commands.register(new YourCommand(this));
        
        // Register a service
        CoreAPI.Services.register(YourService.class, new YourServiceImpl());
        
        LogUtil.info("Your plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Unregister services
        CoreAPI.Services.unregister(YourService.class);
        
        LogUtil.info("Your plugin disabled successfully!");
    }
    
    private boolean checkCoreUtilsAvailable() {
        try {
            Class.forName("com.minecraft.core.api.CoreAPI");
            return true;
        } catch (ClassNotFoundException e) {
            getLogger().severe("Core-Utils not found! This plugin requires Core-Utils to function.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
    }
}
```

#### Approach 2: Using Direct Core-Utils Classes

```java
import com.minecraft.core.CorePlugin;
import com.minecraft.core.api.service.ServiceRegistry;
import com.minecraft.core.command.CommandRegistry;
import com.minecraft.core.utils.LogUtil;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;

public class YourPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // Initialize logging
        LogUtil.init(this);
        
        // Check for Core-Utils
        Plugin corePlugin = getServer().getPluginManager().getPlugin("CoreUtils");
        if (!(corePlugin instanceof CorePlugin)) {
            LogUtil.severe("CoreUtils not found! This plugin requires CoreUtils to function.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register commands
        CommandRegistry commandRegistry = ((CorePlugin) corePlugin).getCommandRegistry();
        commandRegistry.registerCommand(new YourCommand(this));
        
        // Register a service
        ServiceRegistry.register(YourService.class, new YourServiceImpl(this));
        
        LogUtil.info("Your plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Unregister services
        ServiceRegistry.unregister(YourService.class);
        
        LogUtil.info("Your plugin disabled successfully!");
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
    ServiceRegistry.unregister(YourService.class);
    
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

// Create a config manager for your plugin
ConfigManager configManager = new ConfigManager(yourPlugin);

// Load the main config
configManager.loadConfig();
FileConfiguration config = configManager.getConfig();

// Load custom config files
YamlConfiguration customConfig = configManager.loadConfigFile("custom");

// Access configuration values
String value = config.getString("some.path", "default");
boolean enabled = config.getBoolean("feature.enabled", false);

// Save configuration
configManager.saveConfig();
configManager.saveConfigFile("custom");
```

### Using the Messages System

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

// Format string logging
LogUtil.info("Player %s connected from %s", player.getName(), address);
LogUtil.warning("Failed to load file: %s", fileName);
LogUtil.severe("Exception in %s: %s", "TaskManager", e.getMessage());

// Setting debug mode
LogUtil.setDebugMode(true);

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

// Create an inventory
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

// Create a confirmation inventory with message
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

// Capitalize words
String capitalized = FormatUtil.capitalizeWords("hello world"); // "Hello World"
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

// Get player list on a server
bungeeUtils.getPlayerList("survival", playerList -> {
    getLogger().info("Players on survival: " + String.join(", ", playerList));
});

// Broadcast a plugin message
bungeeUtils.broadcastPluginMessage("MyChannel", "Some message data");

// Send a plugin message to a specific server
bungeeUtils.sendPluginMessage("MyChannel", "lobby", "Some message data");
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

Alternatively, consider renaming your model classes to avoid conflicts:

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

### Dependency Issues

If you're getting "cannot find symbol" errors for Core-Utils classes during compilation, check the following:

1. **Verify your plugin.yml** has the correct dependency:
   ```yaml
   depend: [CoreUtils]
   ```

2. **Check your build script** is properly referencing Core-Utils:
   - For multi-module projects: `compileOnly project(':Core-Utils')`
   - For standalone projects: `compileOnly 'com.minecraft:core-utils:1.+'`

3. **Make sure Core-Utils is built first** if using a multi-module setup.

4. **Check import statements** to ensure you're using the correct package paths:
   ```java
   import com.minecraft.core.api.CoreAPI;
   import com.minecraft.core.utils.LogUtil;
   import com.minecraft.core.command.annotation.Command;
   ```

5. **Verify Core-Utils is in the classpath** at runtime (installed as a plugin on your server).


## Best Practices

1. **Check for Core-Utils**: Always verify that Core-Utils is available before using its features.

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

2. **Handle Missing Services**: Gracefully handle situations where a service isn't available.

```java
AnotherService service = ServiceLocator.getService(AnotherService.class);
if (service == null) {
    // Fallback behavior or inform the user
    getLogger().warning("AnotherService is not available. Some features will be disabled.");
} else {
    // Use the service
}
```

3. **Unregister Services**: Always unregister your services during your plugin's `onDisable()` method.

4. **Separate Interface and Implementation**: Define service interfaces in a separate package from their implementations.

5. **Document Services**: Clearly document what your services do and what other plugins might need them.

6. **Consistent Command Structure**: Use a consistent command structure across all your plugins.

7. **Use Debug Mode Appropriately**: Use `LogUtil.debug()` for debugging information, not for regular logging.

8. **Avoid Class Name Conflicts**: Use distinct class names or fully qualified names to avoid conflicts.

9. **Maintain Java 8 Compatibility**: Avoid using Java 9+ features to ensure compatibility with all servers.

## Example Plugin

Here's a complete example of a simple plugin using Core-Utils:

```java
import com.minecraft.core.CorePlugin;
import com.minecraft.core.api.service.ServiceRegistry;
import com.minecraft.core.command.CommandRegistry;
import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.utils.LogUtil;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ExamplePlugin extends JavaPlugin {
    
    private ServiceRegistry serviceRegistry;
    private CommandRegistry commandRegistry;
    
    @Override
    public void onEnable() {
        // Initialize logging
        LogUtil.init(this);
        
        // Check for Core-Utils
        Plugin corePlugin = getServer().getPluginManager().getPlugin("CoreUtils");
        if (!(corePlugin instanceof CorePlugin)) {
            LogUtil.severe("Core-Utils not found! This plugin requires Core-Utils to function.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        CorePlugin core = (CorePlugin) corePlugin;
        
        // Get command registry
        commandRegistry = core.getCommandRegistry();
        
        // Register commands
        ExampleCommand command = new ExampleCommand(this);
        commandRegistry.registerCommand(command);
        
        // Register services
        ExampleService service = new DefaultExampleService(this);
        ServiceRegistry.register(ExampleService.class, service);
        
        LogUtil.info("ExamplePlugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Unregister services
        ServiceRegistry.unregister(ExampleService.class);
        
        LogUtil.info("ExamplePlugin disabled successfully!");
    }
    
    // Command class
    @Command(name = "example", description = "Example command")
    public class ExampleCommand {
        private final ExamplePlugin plugin;
        
        public ExampleCommand(ExamplePlugin plugin) {
            this.plugin = plugin;
        }
        
        @SubCommand(name = "help", description = "Show help")
        public void helpCommand(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.GREEN + "Example Plugin Help");
        }
        
        @SubCommand(isDefault = true, description = "Default command")
        public void defaultCommand(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.GREEN + "This is the default command!");
        }
    }
    
    // Service interface
    public interface ExampleService {
        String performTask(String input);
    }
    
    // Service implementation
    public class DefaultExampleService implements ExampleService {
        private final ExamplePlugin plugin;
        
        public DefaultExampleService(ExamplePlugin plugin) {
            this.plugin = plugin;
        }
        
        @Override
        public String performTask(String input) {
            return input.toUpperCase();
        }
    }
}
```

This example demonstrates:
- Initializing logging with LogUtil
- Checking for Core-Utils
- Registering commands with the CommandRegistry
- Registering services with the ServiceRegistry
- Creating a command class with @Command and @SubCommand annotations
- Creating a service interface and implementation
- Unregistering services on disable