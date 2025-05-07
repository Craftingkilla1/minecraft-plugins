package com.minecraft.core.api.events;

import com.minecraft.core.CorePlugin;
import com.minecraft.core.utils.LogUtil;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Centralized event management system for plugins
 */
public class EventManager {
    private static final Map<Class<? extends Listener>, Listener> registeredListeners = new HashMap<>();
    
    private EventManager() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Register a listener
     * 
     * @param plugin The plugin
     * @param listener The listener to register
     */
    public static void registerListener(Plugin plugin, Listener listener) {
        if (plugin == null || !plugin.isEnabled()) {
            LogUtil.warning("Cannot register listener - plugin is null or disabled");
            return;
        }
        
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        registeredListeners.put(listener.getClass(), listener);
        LogUtil.debug("Registered listener: " + listener.getClass().getName());
    }
    
    /**
     * Unregister a listener
     * 
     * @param listener The listener to unregister
     */
    public static void unregisterListener(Listener listener) {
        if (listener == null) {
            return;
        }
        
        HandlerList.unregisterAll(listener);
        registeredListeners.remove(listener.getClass());
        LogUtil.debug("Unregistered listener: " + listener.getClass().getName());
    }
    
    /**
     * Unregister all listeners for a plugin
     * 
     * @param plugin The plugin
     */
    public static void unregisterAllListeners(Plugin plugin) {
        if (plugin == null) {
            return;
        }
        
        HandlerList.unregisterAll(plugin);
        registeredListeners.entrySet().removeIf(entry -> {
            try {
                return Bukkit.getPluginManager().getPlugin(plugin.getName()) == plugin;
            } catch (Exception e) {
                return false;
            }
        });
        
        LogUtil.debug("Unregistered all listeners for plugin: " + plugin.getName());
    }
    
    /**
     * Call an event
     * 
     * @param event The event to call
     * @return The called event
     */
    public static <T extends Event> T callEvent(T event) {
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }
    
    /**
     * Register a temporary event handler
     * 
     * @param <T> The event type
     * @param plugin The plugin
     * @param eventClass The event class
     * @param handler The event handler
     * @param priority The event priority
     * @param ignoreCancelled Whether to ignore cancelled events
     * @return The registered listener
     */
    public static <T extends Event> Listener registerEvent(
            Plugin plugin, 
            Class<T> eventClass, 
            Consumer<T> handler,
            EventPriority priority,
            boolean ignoreCancelled) {
        
        Listener listener = new Listener() {};
        
        Bukkit.getPluginManager().registerEvent(
            eventClass,
            listener,
            priority,
            (l, event) -> {
                if (eventClass.isInstance(event)) {
                    handler.accept(eventClass.cast(event));
                }
            },
            plugin,
            ignoreCancelled
        );
        
        registeredListeners.put(listener.getClass(), listener);
        LogUtil.debug("Registered temporary event handler for: " + eventClass.getName());
        
        return listener;
    }
    
    /**
     * Register a temporary event handler with normal priority
     * 
     * @param <T> The event type
     * @param plugin The plugin
     * @param eventClass The event class
     * @param handler The event handler
     * @return The registered listener
     */
    public static <T extends Event> Listener registerEvent(
            Plugin plugin, 
            Class<T> eventClass, 
            Consumer<T> handler) {
        
        return registerEvent(plugin, eventClass, handler, EventPriority.NORMAL, false);
    }
    
    /**
     * Get a registered listener
     * 
     * @param <T> The listener type
     * @param listenerClass The listener class
     * @return The listener, or null if not registered
     */
    @SuppressWarnings("unchecked")
    public static <T extends Listener> T getListener(Class<T> listenerClass) {
        return (T) registeredListeners.get(listenerClass);
    }
    
    /**
     * Check if a listener is registered
     * 
     * @param listenerClass The listener class
     * @return true if the listener is registered
     */
    public static boolean isListenerRegistered(Class<? extends Listener> listenerClass) {
        return registeredListeners.containsKey(listenerClass);
    }
    
    /**
     * Get the plugin's instance
     * 
     * @return The plugin instance
     */
    private static CorePlugin getPlugin() {
        return CorePlugin.getInstance();
    }
}