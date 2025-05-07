package com.minecraft.core.api.service.example;

import java.util.List;
import java.util.UUID;

/**
 * Example service interface to demonstrate service registration
 */
public interface ExampleService {
    
    /**
     * Get a value from the service
     * 
     * @param key The key
     * @return The value, or null if not found
     */
    String getValue(String key);
    
    /**
     * Set a value in the service
     * 
     * @param key The key
     * @param value The value
     * @return true if successful
     */
    boolean setValue(String key, String value);
    
    /**
     * Get all keys
     * 
     * @return List of all keys
     */
    List<String> getKeys();
    
    /**
     * Clear all values
     */
    void clear();
    
    /**
     * Register a listener to be notified when values change
     * 
     * @param listener The listener
     */
    void registerListener(ValueChangeListener listener);
    
    /**
     * Unregister a listener
     * 
     * @param listener The listener
     */
    void unregisterListener(ValueChangeListener listener);
    
    /**
     * Interface for value change listeners
     */
    interface ValueChangeListener {
        /**
         * Called when a value changes
         * 
         * @param key The key
         * @param oldValue The old value
         * @param newValue The new value
         */
        void onValueChange(String key, String oldValue, String newValue);
    }
}