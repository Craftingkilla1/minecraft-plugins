package com.minecraft.core;

import com.minecraft.core.api.service.ServiceRegistry;
import com.minecraft.core.command.CommandRegistry;
import com.minecraft.core.command.CoreAdminCommand;
import com.minecraft.core.config.ConfigManager;
import com.minecraft.core.config.Messages;
import com.minecraft.core.utils.BungeeUtils;
import com.minecraft.core.utils.LogUtil;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Core utilities plugin providing shared functionality for other plugins
 */
public class CorePlugin extends JavaPlugin {
    private static CorePlugin instance;
    private CommandRegistry commandRegistry;
    private ConfigManager configManager;
    private Messages messages;
    private BungeeUtils bungeeUtils;
    private PluginMessageListener bungeeMessageListener;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize utilities
        LogUtil.init(this);
        LogUtil.setDebugMode(getConfig().getBoolean("debug-mode", false));
        
        // Load configuration
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Load messages
        messages = configManager.createMessages();
        
        // Initialize command registry
        commandRegistry = new CommandRegistry(this);
        
        // Register commands
        registerCommands();
        
        // Initialize service registry
        ServiceRegistry.init();
        
        // Initialize BungeeCord utilities if enabled
        if (getConfig().getBoolean("bungee-support", false)) {
            initializeBungeeUtils();
        }
        
        // Register event handlers
        registerEventHandlers();
        
        LogUtil.info("Core-Utils plugin v" + getDescription().getVersion() + " enabled successfully");
    }
    
    @Override
    public void onDisable() {
        // Clean up resources
        if (commandRegistry != null) {
            commandRegistry.unregisterAllCommands();
        }
        
        // Unregister BungeeCord channels if initialized
        if (bungeeUtils != null && bungeeMessageListener != null) {
            getServer().getMessenger().unregisterIncomingPluginChannel(this, "BungeeCord", bungeeMessageListener);
            getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
        }
        
        // Clear service registry
        ServiceRegistry.shutdown();
        
        LogUtil.info("Core-Utils plugin disabled successfully");
        instance = null;
    }
    
    /**
     * Register the plugin's commands
     */
    private void registerCommands() {
        // Register admin command
        CoreAdminCommand adminCommand = new CoreAdminCommand(this);
        commandRegistry.registerCommand(adminCommand);
        
        LogUtil.debug("Registered CoreUtils commands");
    }
    
    /**
     * Initialize BungeeCord utilities
     */
    private void initializeBungeeUtils() {
        bungeeUtils = new BungeeUtils(this);
        
        // Register BungeeCord plugin messaging channels
        bungeeMessageListener = BungeeUtils.createMessageListener(this, bungeeUtils);
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", bungeeMessageListener);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        
        LogUtil.info("Initialized BungeeCord support");
    }
    
    /**
     * Register event handlers
     */
    private void registerEventHandlers() {
        // Register event handlers here when needed
    }
    
    /**
     * Get the plugin instance
     * @return The plugin instance
     */
    public static CorePlugin getInstance() {
        return instance;
    }
    
    /**
     * Get the command registry
     * @return The command registry
     */
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }
    
    /**
     * Get the config manager
     * @return The config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Get the messages manager
     * @return The messages manager
     */
    public Messages getMessages() {
        return messages;
    }
    
    /**
     * Get the BungeeCord utilities
     * @return The BungeeCord utilities, or null if not enabled
     */
    public BungeeUtils getBungeeUtils() {
        return bungeeUtils;
    }
}