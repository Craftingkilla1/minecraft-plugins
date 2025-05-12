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

Database connectivity plugin built on top of Core-Utils. Features:
- Connection pooling with multiple database drivers
- Query utilities and SQL injection prevention
- Transactional support
- Performance monitoring

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
/sqlbridge status - Check database connection status
/sqlbridge metrics - View performance metrics
```

#### Example-Plugin
```
/example help - Show command help
/exampledb - Access database features
```

### For Plugin Developers

These plugins provide APIs that can be used in your own plugins. See the [Developer Documentation](DEVELOPMENT.md) for details.

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
  type: MYSQL
  host: localhost
  port: 3306
  database: minecraft
  username: user
  password: pass
```

### Example-Plugin
```yaml
# config.yml
features:
  example_feature_1: true
  database_integration: true
```

## 🔄 Building From Source

See the [Developer Documentation](DEVELOPMENT.md) for information on building from source.

## 📄 License

Each plugin is licensed under the MIT License - see individual plugin directories for details.

## 🤝 Contributing

Contributions are welcome! Please see [DEVELOPMENT.md](DEVELOPMENT.md) for development workflows and guidelines.

## 📚 Documentation

- Check each plugin's dedicated documentation for detailed information
- For version history and changes, see the [Releases](../../releases) page
- For development info, refer to [DEVELOPMENT.md](DEVELOPMENT.md)