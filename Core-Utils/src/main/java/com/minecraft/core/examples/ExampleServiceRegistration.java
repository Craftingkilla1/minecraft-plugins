package com.minecraft.core.examples;

import com.minecraft.core.CorePlugin;
import com.minecraft.core.api.service.ServiceLocator;
import com.minecraft.core.api.service.ServiceRegistry;
import com.minecraft.core.api.service.example.DefaultExampleService;
import com.minecraft.core.api.service.example.ExampleService;
import com.minecraft.core.utils.LogUtil;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Example class demonstrating how to register and use services
 */
public class ExampleServiceRegistration extends JavaPlugin {
    private DefaultExampleService exampleService;
    
    @Override
    public void onEnable() {
        // Create the service
        exampleService = new DefaultExampleService(this);
        
        // Register the service with the ServiceRegistry
        ServiceRegistry.register(ExampleService.class, exampleService);
        
        // Register a value change listener
        exampleService.registerListener((key, oldValue, newValue) -> {
            getLogger().info("Value changed: " + key + " = " + newValue + " (was " + oldValue + ")");
        });
        
        // Set some example values
        exampleService.setValue("example.greeting", "Hello, world!");
        exampleService.setValue("example.version", getDescription().getVersion());
        
        getLogger().info("Example service registered and initialized");
    }
    
    @Override
    public void onDisable() {
        // Unregister the service when the plugin is disabled
        if (exampleService != null) {
            ServiceRegistry.unregister(ExampleService.class);
            exampleService.shutdown();
            exampleService = null;
        }
        
        getLogger().info("Example service unregistered");
    }
    
    /**
     * Example of how another plugin would use the service
     */
    public static class ExampleServiceConsumer {
        public void useService() {
            // Get the service using ServiceLocator (which is a facade for ServiceRegistry)
            ExampleService service = ServiceLocator.getService(ExampleService.class);
            
            if (service != null) {
                // Use the service
                String greeting = service.getValue("example.greeting");
                LogUtil.info("Greeting from service: " + greeting);
                
                // Register a listener
                service.registerListener(new ExampleService.ValueChangeListener() {
                    @Override
                    public void onValueChange(String key, String oldValue, String newValue) {
                        LogUtil.info("Consumer notified of value change: " + key);
                    }
                });
                
                // Set a new value
                service.setValue("example.consumer", "I'm a consumer!");
            } else {
                LogUtil.warning("ExampleService is not available");
            }
        }
        
        /**
         * Example of using the service with error handling
         */
        public void useServiceWithErrorHandling() {
            try {
                // This will throw an exception if the service is not available
                ExampleService service = ServiceLocator.requireService(ExampleService.class);
                
                // Use the service (this will only execute if the service is available)
                service.setValue("example.important", "This is important!");
            } catch (ServiceLocator.ServiceNotFoundException e) {
                LogUtil.severe("Failed to get required service: " + e.getMessage());
                // Handle the error appropriately
            }
        }
    }
}