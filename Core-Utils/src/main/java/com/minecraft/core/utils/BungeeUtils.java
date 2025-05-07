package com.minecraft.core.utils;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Utility class for BungeeCord-related operations
 */
public class BungeeUtils {
    private final Plugin plugin;
    private final Map<String, List<Consumer<String>>> serverNameCallbacks = new HashMap<>();
    private final Map<String, List<Consumer<Integer>>> playerCountCallbacks = new HashMap<>();
    private final Map<String, List<Consumer<String[]>>> playerListCallbacks = new HashMap<>();
    
    private String currentServerName = null;
    
    /**
     * Create a new BungeeUtils instance
     * 
     * @param plugin The plugin instance
     */
    public BungeeUtils(Plugin plugin) {
        this.plugin = plugin;
        
        // Register plugin channels
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
    }
    
    /**
     * Transfer a player to another server
     * 
     * @param player The player to transfer
     * @param serverName The target server name
     * @return true if the transfer request was sent, false otherwise
     */
    public boolean transferPlayerToServer(Player player, String serverName) {
        if (player == null || !player.isOnline() || serverName == null || serverName.isEmpty()) {
            return false;
        }
        
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            LogUtil.info("Sending player " + player.getName() + " to server " + serverName);
            return true;
        } catch (Exception e) {
            LogUtil.severe("Error transferring player to server: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the current server name
     * This will send a request to BungeeCord and register a callback
     * 
     * @return The current server name, or null if not available
     */
    public String getServerName() {
        if (currentServerName != null) {
            return currentServerName;
        }
        
        // Find an online player to send the request
        if (plugin.getServer().getOnlinePlayers().isEmpty()) {
            LogUtil.debug("Cannot get server name - no players online");
            return null;
        }
        
        Player player = plugin.getServer().getOnlinePlayers().iterator().next();
        
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServer");
        
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        LogUtil.debug("Sent GetServer request to BungeeCord");
        
        return null;
    }
    
    /**
     * Get the server name asynchronously
     * 
     * @param callback The callback to receive the server name
     */
    public void getServerNameAsync(Consumer<String> callback) {
        if (currentServerName != null) {
            callback.accept(currentServerName);
            return;
        }
        
        // Register the callback
        serverNameCallbacks.computeIfAbsent("GetServer", k -> new ArrayList<>()).add(callback);
        
        // Send the request
        getServerName();
    }
    
    /**
     * Get the number of players on a server
     * 
     * @param serverName The server name
     * @param callback The callback to receive the player count
     */
    public void getPlayerCount(String serverName, Consumer<Integer> callback) {
        // Find an online player to send the request
        if (plugin.getServer().getOnlinePlayers().isEmpty()) {
            LogUtil.debug("Cannot get player count - no players online");
            callback.accept(0);
            return;
        }
        
        Player player = plugin.getServer().getOnlinePlayers().iterator().next();
        
        // Register the callback
        playerCountCallbacks.computeIfAbsent(serverName, k -> new ArrayList<>()).add(callback);
        
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerCount");
        out.writeUTF(serverName);
        
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        LogUtil.debug("Sent PlayerCount request for server " + serverName);
    }
    
    /**
     * Get the list of players on a server
     * 
     * @param serverName The server name
     * @param callback The callback to receive the player list
     */
    public void getPlayerList(String serverName, Consumer<String[]> callback) {
        // Find an online player to send the request
        if (plugin.getServer().getOnlinePlayers().isEmpty()) {
            LogUtil.debug("Cannot get player list - no players online");
            callback.accept(new String[0]);
            return;
        }
        
        Player player = plugin.getServer().getOnlinePlayers().iterator().next();
        
        // Register the callback
        playerListCallbacks.computeIfAbsent(serverName, k -> new ArrayList<>()).add(callback);
        
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerList");
        out.writeUTF(serverName);
        
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        LogUtil.debug("Sent PlayerList request for server " + serverName);
    }
    
    /**
     * Broadcast a plugin message to all servers
     * 
     * @param channel The channel to use
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public boolean broadcastPluginMessage(String channel, String message) {
        // Find an online player to send the request
        if (plugin.getServer().getOnlinePlayers().isEmpty()) {
            LogUtil.debug("Cannot broadcast plugin message - no players online");
            return false;
        }
        
        Player player = plugin.getServer().getOnlinePlayers().iterator().next();
        
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Forward");
            out.writeUTF("ALL");
            out.writeUTF(channel);
            
            byte[] msgBytes = message.getBytes();
            out.writeShort(msgBytes.length);
            out.write(msgBytes);
            
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            LogUtil.debug("Broadcasted plugin message on channel " + channel);
            return true;
        } catch (Exception e) {
            LogUtil.severe("Error broadcasting plugin message: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Send a plugin message to a specific server
     * 
     * @param channel The channel to use
     * @param server The target server
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public boolean sendPluginMessage(String channel, String server, String message) {
        // Find an online player to send the request
        if (plugin.getServer().getOnlinePlayers().isEmpty()) {
            LogUtil.debug("Cannot send plugin message - no players online");
            return false;
        }
        
        Player player = plugin.getServer().getOnlinePlayers().iterator().next();
        
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Forward");
            out.writeUTF(server);
            out.writeUTF(channel);
            
            byte[] msgBytes = message.getBytes();
            out.writeShort(msgBytes.length);
            out.write(msgBytes);
            
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            LogUtil.debug("Sent plugin message to server " + server + " on channel " + channel);
            return true;
        } catch (Exception e) {
            LogUtil.severe("Error sending plugin message: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Handle a BungeeCord plugin message
     * This should be called from the PluginMessageListener
     * 
     * @param subchannel The subchannel
     * @param in The data input
     */
    public void handlePluginMessage(String subchannel, ByteArrayDataInput in) {
        try {
            switch (subchannel) {
                case "GetServer":
                    String serverName = in.readUTF();
                    currentServerName = serverName;
                    
                    // Notify callbacks
                    List<Consumer<String>> callbacks = serverNameCallbacks.getOrDefault("GetServer", new ArrayList<>());
                    for (Consumer<String> callback : callbacks) {
                        callback.accept(serverName);
                    }
                    serverNameCallbacks.remove("GetServer");
                    
                    LogUtil.debug("Received server name from BungeeCord: " + serverName);
                    break;
                    
                case "PlayerCount":
                    String countServer = in.readUTF();
                    int playerCount = in.readInt();
                    
                    // Notify callbacks
                    List<Consumer<Integer>> countCallbacks = playerCountCallbacks.getOrDefault(countServer, new ArrayList<>());
                    for (Consumer<Integer> callback : countCallbacks) {
                        callback.accept(playerCount);
                    }
                    playerCountCallbacks.remove(countServer);
                    
                    LogUtil.debug("Received player count for server " + countServer + ": " + playerCount);
                    break;
                    
                case "PlayerList":
                    String listServer = in.readUTF();
                    String playerListStr = in.readUTF();
                    String[] playerList = playerListStr.split(", ");
                    
                    // Notify callbacks
                    List<Consumer<String[]>> listCallbacks = playerListCallbacks.getOrDefault(listServer, new ArrayList<>());
                    for (Consumer<String[]> callback : listCallbacks) {
                        callback.accept(playerList);
                    }
                    playerListCallbacks.remove(listServer);
                    
                    LogUtil.debug("Received player list for server " + listServer + ": " + playerListStr);
                    break;
                    
                default:
                    LogUtil.debug("Received unknown BungeeCord subchannel: " + subchannel);
                    break;
            }
        } catch (Exception e) {
            LogUtil.severe("Error handling BungeeCord message: " + e.getMessage());
        }
    }
    
    /**
     * Get the current server name
     * 
     * @return The current server name, or null if not available
     */
    public String getCurrentServerName() {
        return currentServerName;
    }
    
    /**
     * Set the current server name
     * 
     * @param serverName The server name
     */
    public void setCurrentServerName(String serverName) {
        this.currentServerName = serverName;
    }
    
    /**
     * Create a BungeeCord message listener for a plugin
     * 
     * @param plugin The plugin
     * @param bungeeUtils The BungeeUtils instance
     * @return The message listener
     */
    public static PluginMessageListener createMessageListener(Plugin plugin, BungeeUtils bungeeUtils) {
        return (channel, player, message) -> {
            if (!channel.equals("BungeeCord")) {
                return;
            }
            
            try {
                ByteArrayDataInput in = ByteStreams.newDataInput(message);
                String subchannel = in.readUTF();
                
                bungeeUtils.handlePluginMessage(subchannel, in);
            } catch (Exception e) {
                LogUtil.severe("Error handling BungeeCord message: " + e.getMessage());
            }
        };
    }
}