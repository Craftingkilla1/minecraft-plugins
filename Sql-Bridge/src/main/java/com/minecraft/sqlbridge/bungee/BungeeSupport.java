// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/bungee/BungeeSupport.java
package com.minecraft.sqlbridge.bungee;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.connection.ConnectionManager;

import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Provides support for BungeeCord networks, allowing for shared database connections
 * across multiple servers.
 */
public class BungeeSupport implements PluginMessageListener {
    
    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final String SQLB_CHANNEL = "SqlBridge";
    
    private final SqlBridgePlugin plugin;
    private final ConnectionManager connectionManager;
    private final ConcurrentMap<UUID, Consumer<byte[]>> responseHandlers;
    private String serverName;
    private List<String> serverList;
    
    /**
     * Constructor for BungeeSupport.
     *
     * @param plugin The SQL-Bridge plugin instance
     * @param connectionManager The connection manager
     */
    public BungeeSupport(SqlBridgePlugin plugin, ConnectionManager connectionManager) {
        this.plugin = plugin;
        this.connectionManager = connectionManager;
        this.responseHandlers = new ConcurrentHashMap<>();
        this.serverName = "unknown";
        this.serverList = new ArrayList<>();
    }
    
    /**
     * Enable BungeeSupport.
     *
     * @return true if BungeeSupport was enabled successfully, false otherwise
     */
    public boolean enable() {
        try {
            // Register plugin channels
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
            
            // Get the server name
            getServerName();
            
            // Get the server list
            getServerList();
            
            LogUtil.info("BungeeSupport enabled");
            return true;
        } catch (Exception e) {
            LogUtil.severe("Failed to enable BungeeSupport: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Disable BungeeSupport.
     */
    public void disable() {
        // Unregister plugin channels
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
        
        LogUtil.info("BungeeSupport disabled");
    }
    
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(BUNGEE_CHANNEL)) {
            return;
        }
        
        try (ByteArrayInputStream stream = new ByteArrayInputStream(message);
             DataInputStream in = new DataInputStream(stream)) {
            
            String subChannel = in.readUTF();
            
            if (subChannel.equals("GetServer")) {
                // Response to GetServer
                serverName = in.readUTF();
                LogUtil.info("Server name: " + serverName);
            } else if (subChannel.equals("GetServers")) {
                // Response to GetServers
                String[] servers = in.readUTF().split(", ");
                serverList = new ArrayList<>();
                for (String server : servers) {
                    serverList.add(server);
                }
                LogUtil.info("Server list: " + String.join(", ", serverList));
            } else if (subChannel.equals(SQLB_CHANNEL)) {
                // Custom SQL-Bridge message
                handleSqlBridgeMessage(in);
            }
        } catch (IOException e) {
            LogUtil.warning("Error processing BungeeCord message: " + e.getMessage());
        }
    }
    
    /**
     * Handle a SQL-Bridge plugin message.
     *
     * @param in The data input stream
     * @throws IOException If an I/O error occurs
     */
    private void handleSqlBridgeMessage(DataInputStream in) throws IOException {
        // Read the message type
        String type = in.readUTF();
        
        if (type.equals("Response")) {
            // Handle response
            UUID requestId = UUID.fromString(in.readUTF());
            int dataLength = in.readInt();
            byte[] data = new byte[dataLength];
            in.readFully(data);
            
            // Find the handler for this request
            Consumer<byte[]> handler = responseHandlers.remove(requestId);
            if (handler != null) {
                handler.accept(data);
            }
        }
    }
    
    /**
     * Send a message to a specific server via BungeeCord.
     *
     * @param server The server to send the message to
     * @param data The message data
     */
    public void sendMessageToServer(String server, byte[] data) {
        Player player = getAnyPlayer();
        if (player == null) {
            LogUtil.warning("Cannot send message to server - no player available");
            return;
        }
        
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(stream)) {
            
            out.writeUTF("Forward");
            out.writeUTF(server);
            out.writeUTF(SQLB_CHANNEL);
            
            // Write the data length and data
            out.writeShort(data.length);
            out.write(data);
            
            player.sendPluginMessage(plugin, BUNGEE_CHANNEL, stream.toByteArray());
        } catch (IOException e) {
            LogUtil.warning("Error sending message to server " + server + ": " + e.getMessage());
        }
    }
    
    /**
     * Send a message to all servers via BungeeCord.
     *
     * @param data The message data
     */
    public void sendMessageToAll(byte[] data) {
        Player player = getAnyPlayer();
        if (player == null) {
            LogUtil.warning("Cannot send message to all servers - no player available");
            return;
        }
        
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(stream)) {
            
            out.writeUTF("Forward");
            out.writeUTF("ALL");
            out.writeUTF(SQLB_CHANNEL);
            
            // Write the data length and data
            out.writeShort(data.length);
            out.write(data);
            
            player.sendPluginMessage(plugin, BUNGEE_CHANNEL, stream.toByteArray());
        } catch (IOException e) {
            LogUtil.warning("Error sending message to all servers: " + e.getMessage());
        }
    }
    
    /**
     * Send a request to a specific server and get a response.
     *
     * @param server The server to send the request to
     * @param data The request data
     * @return A CompletableFuture for the response
     */
    public CompletableFuture<byte[]> sendRequest(String server, byte[] data) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(stream)) {
            
            out.writeUTF("Request");
            out.writeUTF(requestId.toString());
            out.writeInt(data.length);
            out.write(data);
            
            byte[] requestData = stream.toByteArray();
            
            // Register the response handler
            responseHandlers.put(requestId, responseData -> future.complete(responseData));
            
            // Send the request
            sendMessageToServer(server, requestData);
            
            // Set a timeout for the request
            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                if (!future.isDone() && responseHandlers.remove(requestId) != null) {
                    future.completeExceptionally(new RuntimeException("Request timed out"));
                }
            }, 100); // 5 seconds timeout (100 ticks)
            
            return future;
        } catch (IOException e) {
            LogUtil.warning("Error creating request: " + e.getMessage());
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Get the name of this server.
     *
     * @return The server name, or a CompletableFuture for the server name if it's not available yet
     */
    public Object getServerName() {
        if (serverName != null && !serverName.equals("unknown")) {
            return serverName;
        }
        
        CompletableFuture<String> future = new CompletableFuture<>();
        
        // Schedule a task to request the server name
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player player = getAnyPlayer();
            if (player == null) {
                future.completeExceptionally(new RuntimeException("No player available to send GetServer request"));
                return;
            }
            
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
                 DataOutputStream out = new DataOutputStream(stream)) {
                
                out.writeUTF("GetServer");
                player.sendPluginMessage(plugin, BUNGEE_CHANNEL, stream.toByteArray());
                
                // Schedule a timeout
                plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                    if (!future.isDone()) {
                        future.completeExceptionally(new RuntimeException("GetServer request timed out"));
                    }
                }, 100); // 5 seconds timeout (100 ticks)
                
            } catch (IOException e) {
                future.completeExceptionally(e);
            }
        }, 20); // 1 second delay (20 ticks)
        
        return future;
    }
    
    /**
     * Get the list of servers in the BungeeCord network.
     *
     * @return The server list, or a CompletableFuture for the server list if it's not available yet
     */
    public Object getServerList() {
        if (!serverList.isEmpty()) {
            return serverList;
        }
        
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        
        // Schedule a task to request the server list
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player player = getAnyPlayer();
            if (player == null) {
                future.completeExceptionally(new RuntimeException("No player available to send GetServers request"));
                return;
            }
            
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
                 DataOutputStream out = new DataOutputStream(stream)) {
                
                out.writeUTF("GetServers");
                player.sendPluginMessage(plugin, BUNGEE_CHANNEL, stream.toByteArray());
                
                // Schedule a timeout
                plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                    if (!future.isDone()) {
                        future.completeExceptionally(new RuntimeException("GetServers request timed out"));
                    }
                }, 100); // 5 seconds timeout (100 ticks)
                
            } catch (IOException e) {
                future.completeExceptionally(e);
            }
        }, 20); // 1 second delay (20 ticks)
        
        return future;
    }
    
    /**
     * Get any online player to use for sending plugin messages.
     *
     * @return Any online player, or null if no players are online
     */
    private Player getAnyPlayer() {
        if (plugin.getServer().getOnlinePlayers().isEmpty()) {
            return null;
        }
        return plugin.getServer().getOnlinePlayers().iterator().next();
    }
    
    /**
     * Get the SharedDatabaseManager for managing shared databases.
     *
     * @return The SharedDatabaseManager
     */
    public SharedDatabaseManager getSharedDatabaseManager() {
        return new SharedDatabaseManager(plugin, this, connectionManager);
    }
    
    /**
     * Check if BungeeSupport is properly working.
     *
     * @return true if BungeeSupport is working, false otherwise
     */
    public boolean isWorking() {
        return serverName != null && !serverName.equals("unknown") && !serverList.isEmpty();
    }
}