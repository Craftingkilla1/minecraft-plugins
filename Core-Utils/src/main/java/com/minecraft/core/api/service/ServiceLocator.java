package com.minecraft.core.api.service;

import com.minecraft.core.utils.LogUtil;

/**
 * Service locator pattern implementation for easy access to services.
 * This is a simplified facade for ServiceRegistry that provides static access methods.
 */
public class ServiceLocator {
    
    private ServiceLocator() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Get a service from the registry
     * 
     * @param <T> The service type
     * @param serviceClass The service class
     * @return The service instance, or null if not found
     */
    public static <T> T getService(Class<T> serviceClass) {
        return ServiceRegistry.getService(serviceClass);
    }
    
    /**
     * Get a service, throwing an exception if not found
     * 
     * @param <T> The service type
     * @param serviceClass The service class
     * @return The service instance
     * @throws ServiceNotFoundException If the service is not found
     */
    public static <T> T requireService(Class<T> serviceClass) throws ServiceNotFoundException {
        T service = ServiceRegistry.getService(serviceClass);
        
        if (service == null) {
            throw new ServiceNotFoundException("Required service not found: " + serviceClass.getName());
        }
        
        return service;
    }
    
    /**
     * Check if a service is available
     * 
     * @param serviceClass The service class
     * @return true if the service is available
     */
    public static boolean hasService(Class<?> serviceClass) {
        return ServiceRegistry.isRegistered(serviceClass);
    }
    
    /**
     * Exception thrown when a required service is not found
     */
    public static class ServiceNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        public ServiceNotFoundException(String message) {
            super(message);
        }
        
        public ServiceNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}