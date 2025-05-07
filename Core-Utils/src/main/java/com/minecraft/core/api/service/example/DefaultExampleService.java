package com.minecraft.core.api.service.example;

import com.minecraft.core.utils.LogUtil;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation of the ExampleService interface
 */
public class DefaultExampleService implements ExampleService {
    private final Plugin plugin;
    private final Map<String, String> values = new HashMap<>();
    private final List<ValueChangeListener> listeners = new CopyOnWriteArrayList<>();
    
    /**
     * Create a new default example service
     * 
     * @param plugin The plugin instance
     */
    public DefaultExampleService(Plugin plugin) {
        this.plugin = plugin;
        LogUtil.info("DefaultExampleService initialized");
    }
    
    @Override
    public String getValue(String key) {
        return values.get(key);
    }
    
    @Override
    public boolean setValue(String key, String value) {
        if (key == null || value == null) {
            return false;
        }
        
        String oldValue = values.put(key, value);
        
        // Notify listeners
        for (ValueChangeListener listener : listeners) {
            try {
                listener.onValueChange(key, oldValue, value);
            } catch (Exception e) {
                LogUtil.severe("Error notifying listener: " + e.getMessage());
            }
        }
        
        return true;
    }
    
    @Override
    public List<String> getKeys() {
        return new ArrayList<>(values.keySet());
    }
    
    @Override
    public void clear() {
        values.clear();
        LogUtil.info("Cleared all values in DefaultExampleService");
    }
    
    @Override
    public void registerListener(ValueChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            LogUtil.debug("Registered value change listener: " + listener.getClass().getName());
        }
    }
    
    @Override
    public void unregisterListener(ValueChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            LogUtil.debug("Unregistered value change listener: " + listener.getClass().getName());
        }
    }
    
    /**
     * Shutdown this service
     */
    public void shutdown() {
        clear();
        listeners.clear();
        LogUtil.info("DefaultExampleService shutdown");
    }
}