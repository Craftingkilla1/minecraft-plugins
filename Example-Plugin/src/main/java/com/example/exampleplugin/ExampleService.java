package com.example.exampleplugin;

import java.util.List;

/**
 * Example service interface that demonstrates how to create
 * a service for use with the Core-Utils service registry
 */
public interface ExampleService {
    
    /**
     * Process some data
     * 
     * @param data The data to process
     * @return The processed data
     */
    String processData(String data);
    
    /**
     * Get statistics
     * 
     * @return A list of statistics
     */
    List<String> getStats();
    
    /**
     * Check if a feature is enabled
     * 
     * @param featureName The feature name
     * @return true if the feature is enabled
     */
    boolean isFeatureEnabled(String featureName);
    
    /**
     * Enable or disable a feature
     * 
     * @param featureName The feature name
     * @param enabled Whether to enable or disable the feature
     * @return true if the operation was successful
     */
    boolean setFeatureEnabled(String featureName, boolean enabled);
    
    /**
     * Get the version of this service
     * 
     * @return The service version
     */
    String getVersion();
}