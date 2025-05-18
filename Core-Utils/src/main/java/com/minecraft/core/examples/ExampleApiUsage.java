package com.minecraft.core.examples;

import com.minecraft.core.api.CoreAPI;
import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.utils.InventoryUtil;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.core.utils.TimeUtil;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * Example plugin showcasing how to use Core-Utils API
 * This class demonstrates the recommended patterns for utilizing Core-Utils
 */
public class ExampleApiUsage extends JavaPlugin {
    
    private LogUtil.PluginLogger logger;
    
    @Override
    public void onEnable() {
        // Initialize logging
        CoreAPI.Utils.initLogging(this);
        logger = CoreAPI.Utils.createLogger(this, "[Example] ");
        
        // Register commands
        registerCommands();
        
        // Register services
        registerServices();
        
        logger.info("ExampleApiUsage plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Unregister services
        if (CoreAPI.Services.isRegistered(ExampleService.class)) {
            CoreAPI.Services.unregister(ExampleService.class);
        }
        
        logger.info("ExampleApiUsage plugin disabled successfully!");
    }
    
    /**
     * Register commands using Core-Utils command framework
     */
    private void registerCommands() {
        // Create a command instance
        ExampleCommand command = new ExampleCommand();
        
        // Register with the CoreAPI
        if (CoreAPI.Commands.register(command)) {
            logger.info("Registered ExampleCommand successfully");
        } else {
            logger.warning("Failed to register ExampleCommand");
        }
    }
    
    /**
     * Register services using Core-Utils service registry
     */
    private void registerServices() {
        // Create a service implementation
        ExampleService serviceImpl = new DefaultExampleService();
        
        // Register with the CoreAPI
        if (CoreAPI.Services.register(ExampleService.class, serviceImpl)) {
            logger.info("Registered ExampleService successfully");
        } else {
            logger.warning("Failed to register ExampleService");
        }
    }
    
    /**
     * Example command implementation
     */
    @Command(name = "example", description = "Example command showcasing Core-Utils API", aliases = {"ex"})
    public class ExampleCommand {
        
        @SubCommand(name = "help", description = "Show help information")
        public void helpCommand(CommandSender sender, String[] args) {
            sender.sendMessage("§a=== Example Plugin Help ===");
            sender.sendMessage("§e/example help §7- Show this help page");
            sender.sendMessage("§e/example item §7- Create a custom item");
            sender.sendMessage("§e/example time §7- Show current time");
            sender.sendMessage("§e/example service §7- Test service registry");
        }
        
        @SubCommand(name = "item", description = "Create a custom item using InventoryUtil")
        public void itemCommand(CommandSender sender, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players");
                return;
            }
            
            Player player = (Player) sender;
            
            // Create a glowing diamond sword with custom name and lore
            ItemStack item = InventoryUtil.createItem(
                Material.DIAMOND_SWORD,
                "§b§lMagic Sword",
                "§7A powerful sword created by",
                "§7the Core-Utils API",
                "",
                "§e§lRARE ITEM"
            );
            
            // Make it glow
            item = InventoryUtil.makeGlow(item);
            
            // Give it to the player
            player.getInventory().addItem(item);
            player.sendMessage("§aYou've received a Magic Sword!");
        }
        
        @SubCommand(name = "time", description = "Show current time using TimeUtil")
        public void timeCommand(CommandSender sender, String[] args) {
            // Get current time
            String currentTime = TimeUtil.formatDateTime(TimeUtil.now());
            
            // Format a duration
            String duration = TimeUtil.formatTime(3665); // 1h 1m 5s
            
            sender.sendMessage("§aCurrent time: §f" + currentTime);
            sender.sendMessage("§aExample duration: §f" + duration);
        }
        
        @SubCommand(name = "service", description = "Test service registry")
        public void serviceCommand(CommandSender sender, String[] args) {
            // Get service using CoreAPI
            ExampleService service = CoreAPI.Services.get(ExampleService.class);
            
            if (service != null) {
                String result = service.doSomething("test");
                sender.sendMessage("§aService result: §f" + result);
            } else {
                sender.sendMessage("§cService not found!");
            }
        }
    }
    
    /**
     * Example service interface
     */
    public interface ExampleService {
        String doSomething(String input);
    }
    
    /**
     * Example service implementation
     */
    public class DefaultExampleService implements ExampleService {
        @Override
        public String doSomething(String input) {
            return "Processed: " + input.toUpperCase();
        }
    }
}