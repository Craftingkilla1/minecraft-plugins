// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/bungee/SharedDatabaseManager.java
package com.minecraft.sqlbridge.bungee;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.connection.ConnectionManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages shared databases across a BungeeCord network.
 */
public class SharedDatabaseManager {
    
    private final SqlBridgePlugin plugin;
    private final BungeeSupport bungeeSupport;
    private final ConnectionManager connectionManager;
    private final Map<String, Long> lastAccessTimes;
    private final ReadWriteLock accessLock;
    
    /**
     * Constructor for SharedDatabaseManager.
     *
     * @param plugin The SQL-Bridge plugin instance
     * @param bungeeSupport The BungeeSupport instance
     * @param connectionManager The connection manager
     */
    public SharedDatabaseManager(SqlBridgePlugin plugin, BungeeSupport bungeeSupport, ConnectionManager connectionManager) {
        this.plugin = plugin;
        this.bungeeSupport = bungeeSupport;
        this.connectionManager = connectionManager;
        this.lastAccessTimes = new ConcurrentHashMap<>();
        this.accessLock = new ReentrantReadWriteLock();
        
        // Schedule cleanup task
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupInactiveEntries, 6000, 6000); // Every 5 minutes (6000 ticks)
    }
    
    /**
     * Get a value from the shared database.
     *
     * @param key The key to get
     * @return A CompletableFuture containing the value, or null if it doesn't exist
     */
    public CompletableFuture<String> get(String key) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        try {
            // Check if we're locked for reading
            accessLock.readLock().lock();
            try {
                // Update the last access time
                lastAccessTimes.put(key, System.currentTimeMillis());
            } finally {
                accessLock.readLock().unlock();
            }
            
            // Create the request
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);
            
            out.writeUTF("GET");
            out.writeUTF(key);
            
            // Send the request to all servers
            bungeeSupport.sendRequest("ALL", stream.toByteArray())
                    .thenAccept(responseData -> {
                        try (ByteArrayInputStream bis = new ByteArrayInputStream(responseData);
                             DataInputStream in = new DataInputStream(bis)) {
                            
                            String status = in.readUTF();
                            if (status.equals("SUCCESS")) {
                                boolean exists = in.readBoolean();
                                if (exists) {
                                    String value = in.readUTF();
                                    future.complete(value);
                                } else {
                                    future.complete(null);
                                }
                            } else {
                                future.completeExceptionally(new RuntimeException("Error getting value: " + in.readUTF()));
                            }
                        } catch (IOException e) {
                            future.completeExceptionally(e);
                        }
                    })
                    .exceptionally(e -> {
                        future.completeExceptionally(e);
                        return null;
                    });
            
            return future;
        } catch (Exception e) {
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Set a value in the shared database.
     *
     * @param key The key to set
     * @param value The value to set
     * @return A CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> set(String key, String value) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            // Check if we're locked for writing
            accessLock.writeLock().lock();
            try {
                // Update the last access time
                lastAccessTimes.put(key, System.currentTimeMillis());
            } finally {
                accessLock.writeLock().unlock();
            }
            
            // Create the request
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);
            
            out.writeUTF("SET");
            out.writeUTF(key);
            out.writeUTF(value);
            
            // Send the request to all servers
            bungeeSupport.sendRequest("ALL", stream.toByteArray())
                    .thenAccept(responseData -> {
                        try (ByteArrayInputStream bis = new ByteArrayInputStream(responseData);
                             DataInputStream in = new DataInputStream(bis)) {
                            
                            String status = in.readUTF();
                            if (status.equals("SUCCESS")) {
                                future.complete(null);
                            } else {
                                future.completeExceptionally(new RuntimeException("Error setting value: " + in.readUTF()));
                            }
                        } catch (IOException e) {
                            future.completeExceptionally(e);
                        }
                    })
                    .exceptionally(e -> {
                        future.completeExceptionally(e);
                        return null;
                    });
            
            return future;
        } catch (Exception e) {
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Remove a value from the shared database.
     *
     * @param key The key to remove
     * @return A CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> remove(String key) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            // Check if we're locked for writing
            accessLock.writeLock().lock();
            try {
                // Remove the key from the last access times
                lastAccessTimes.remove(key);
            } finally {
                accessLock.writeLock().unlock();
            }
            
            // Create the request
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);
            
            out.writeUTF("REMOVE");
            out.writeUTF(key);
            
            // Send the request to all servers
            bungeeSupport.sendRequest("ALL", stream.toByteArray())
                    .thenAccept(responseData -> {
                        try (ByteArrayInputStream bis = new ByteArrayInputStream(responseData);
                             DataInputStream in = new DataInputStream(bis)) {
                            
                            String status = in.readUTF();
                            if (status.equals("SUCCESS")) {
                                future.complete(null);
                            } else {
                                future.completeExceptionally(new RuntimeException("Error removing value: " + in.readUTF()));
                            }
                        } catch (IOException e) {
                            future.completeExceptionally(e);
                        }
                    })
                    .exceptionally(e -> {
                        future.completeExceptionally(e);
                        return null;
                    });
            
            return future;
        } catch (Exception e) {
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Check if a key exists in the shared database.
     *
     * @param key The key to check
     * @return A CompletableFuture containing true if the key exists, false otherwise
     */
    public CompletableFuture<Boolean> exists(String key) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        try {
            // Check if we're locked for reading
            accessLock.readLock().lock();
            try {
                // Update the last access time
                lastAccessTimes.put(key, System.currentTimeMillis());
            } finally {
                accessLock.readLock().unlock();
            }
            
            // Create the request
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);
            
            out.writeUTF("EXISTS");
            out.writeUTF(key);
            
            // Send the request to all servers
            bungeeSupport.sendRequest("ALL", stream.toByteArray())
                    .thenAccept(responseData -> {
                        try (ByteArrayInputStream bis = new ByteArrayInputStream(responseData);
                             DataInputStream in = new DataInputStream(bis)) {
                            
                            String status = in.readUTF();
                            if (status.equals("SUCCESS")) {
                                boolean exists = in.readBoolean();
                                future.complete(exists);
                            } else {
                                future.completeExceptionally(new RuntimeException("Error checking existence: " + in.readUTF()));
                            }
                        } catch (IOException e) {
                            future.completeExceptionally(e);
                        }
                    })
                    .exceptionally(e -> {
                        future.completeExceptionally(e);
                        return null;
                    });
            
            return future;
        } catch (Exception e) {
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Get all keys in the shared database.
     *
     * @return A CompletableFuture containing a map of all keys and values
     */
    public CompletableFuture<Map<String, String>> getAll() {
        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
        
        try {
            // Create the request
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);
            
            out.writeUTF("GETALL");
            
            // Send the request to all servers
            bungeeSupport.sendRequest("ALL", stream.toByteArray())
                    .thenAccept(responseData -> {
                        try (ByteArrayInputStream bis = new ByteArrayInputStream(responseData);
                             DataInputStream in = new DataInputStream(bis)) {
                            
                            String status = in.readUTF();
                            if (status.equals("SUCCESS")) {
                                int count = in.readInt();
                                Map<String, String> result = new HashMap<>();
                                
                                for (int i = 0; i < count; i++) {
                                    String key = in.readUTF();
                                    String value = in.readUTF();
                                    result.put(key, value);
                                    
                                    // Update the last access time
                                    accessLock.readLock().lock();
                                    try {
                                        lastAccessTimes.put(key, System.currentTimeMillis());
                                    } finally {
                                        accessLock.readLock().unlock();
                                    }
                                }
                                
                                future.complete(result);
                            } else {
                                future.completeExceptionally(new RuntimeException("Error getting all values: " + in.readUTF()));
                            }
                        } catch (IOException e) {
                            future.completeExceptionally(e);
                        }
                    })
                    .exceptionally(e -> {
                        future.completeExceptionally(e);
                        return null;
                    });
            
            return future;
        } catch (Exception e) {
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Clear all values in the shared database.
     *
     * @return A CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> clear() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            // Check if we're locked for writing
            accessLock.writeLock().lock();
            try {
                // Clear the last access times
                lastAccessTimes.clear();
            } finally {
                accessLock.writeLock().unlock();
            }
            
            // Create the request
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);
            
            out.writeUTF("CLEAR");
            
            // Send the request to all servers
            bungeeSupport.sendRequest("ALL", stream.toByteArray())
                    .thenAccept(responseData -> {
                        try (ByteArrayInputStream bis = new ByteArrayInputStream(responseData);
                             DataInputStream in = new DataInputStream(bis)) {
                            
                            String status = in.readUTF();
                            if (status.equals("SUCCESS")) {
                                future.complete(null);
                            } else {
                                future.completeExceptionally(new RuntimeException("Error clearing values: " + in.readUTF()));
                            }
                        } catch (IOException e) {
                            future.completeExceptionally(e);
                        }
                    })
                    .exceptionally(e -> {
                        future.completeExceptionally(e);
                        return null;
                    });
            
            return future;
        } catch (Exception e) {
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Clean up inactive entries.
     */
    private void cleanupInactiveEntries() {
        try {
            // Get the current time
            long now = System.currentTimeMillis();
            long threshold = now - (1000 * 60 * 60 * 24); // 24 hours
            
            // Find entries to remove
            accessLock.writeLock().lock();
            try {
                lastAccessTimes.entrySet().removeIf(entry -> entry.getValue() < threshold);
            } finally {
                accessLock.writeLock().unlock();
            }
        } catch (Exception e) {
            LogUtil.warning("Error cleaning up inactive entries: " + e.getMessage());
        }
    }
}