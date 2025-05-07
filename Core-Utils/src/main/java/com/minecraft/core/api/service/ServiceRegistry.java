package com.minecraft.core.api.service;

import com.minecraft.core.utils.LogUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for plugin services that enables cross-plugin communication
 * and dependency management.
 */
public class ServiceRegistry {
    private static final Map<Class<?>, Object> services = new ConcurrentHashMap<>();
    private static boolean initialized = false;
    
    private ServiceRegistry() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Initialize the service registry
     */
    public static void init() {
        if (initialized) {
            LogUtil.warning("ServiceRegistry has already been initialized");
            return;
        }
        
        services.clear();
        initialized = true;
        LogUtil.info("ServiceRegistry initialized");
    }
    
    /**
     * Shutdown the service registry
     */
    public static void shutdown() {
        services.clear();
        initialized = false;
        LogUtil.info("ServiceRegistry shut down");
    }
    
    /**
     * Register a service implementation
     * 
     * @param <T> The service type
     * @param serviceClass The service interface class
     * @param implementation The service implementation
     * @return true if registration was successful
     */
    public static <T> boolean register(Class<T> serviceClass, T implementation) {
        if (!initialized) {
            LogUtil.warning("ServiceRegistry has not been initialized");
            return false;
        }
        
        if (serviceClass == null || implementation == null) {
            LogUtil.warning("Cannot register null service class or implementation");
            return false;
        }
        
        if (services.containsKey(serviceClass)) {
            LogUtil.warning("Service " + serviceClass.getName() + " is already registered");
            return false;
        }
        
        services.put(serviceClass, implementation);
        LogUtil.info("Registered service: " + serviceClass.getName());
        return true;
    }
    
    /**
     * Unregister a service
     * 
     * @param <T> The service type
     * @param serviceClass The service interface class
     * @return true if unregistration was successful
     */
    public static <T> boolean unregister(Class<T> serviceClass) {
        if (!initialized) {
            LogUtil.warning("ServiceRegistry has not been initialized");
            return false;
        }
        
        if (serviceClass == null) {
            LogUtil.warning("Cannot unregister null service class");
            return false;
        }
        
        if (!services.containsKey(serviceClass)) {
            LogUtil.warning("Service " + serviceClass.getName() + " is not registered");
            return false;
        }
        
        services.remove(serviceClass);
        LogUtil.info("Unregistered service: " + serviceClass.getName());
        return true;
    }
    
    /**
     * Get a registered service
     * 
     * @param <T> The service type
     * @param serviceClass The service interface class
     * @return The service implementation, or null if not registered
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(Class<T> serviceClass) {
        if (!initialized) {
            LogUtil.warning("ServiceRegistry has not been initialized");
            return null;
        }
        
        if (serviceClass == null) {
            LogUtil.warning("Cannot get null service class");
            return null;
        }
        
        Object service = services.get(serviceClass);
        
        if (service == null) {
            LogUtil.debug("Service " + serviceClass.getName() + " is not registered");
            return null;
        }
        
        return (T) service;
    }
    
    /**
     * Check if a service is registered
     * 
     * @param serviceClass The service interface class
     * @return true if the service is registered
     */
    public static boolean isRegistered(Class<?> serviceClass) {
        if (!initialized) {
            LogUtil.warning("ServiceRegistry has not been initialized");
            return false;
        }
        
        if (serviceClass == null) {
            LogUtil.warning("Cannot check null service class");
            return false;
        }
        
        return services.containsKey(serviceClass);
    }
    
    /**
     * Get all registered services
     * 
     * @return A map of all registered services
     */
    public static Map<Class<?>, Object> getAllServices() {
        if (!initialized) {
            LogUtil.warning("ServiceRegistry has not been initialized");
            return new HashMap<>();
        }
        
        return new HashMap<>(services);
    }
}