package com.minecraft.core.command.args;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Utility class for parsing command arguments
 */
public class ArgumentParser {
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    
    private final String[] args;
    private int position = 0;
    
    /**
     * Create a new argument parser
     * 
     * @param args The command arguments
     */
    public ArgumentParser(String[] args) {
        this.args = args;
    }
    
    /**
     * Check if there are any more arguments
     * 
     * @return true if there are more arguments
     */
    public boolean hasNext() {
        return position < args.length;
    }
    
    /**
     * Get the next argument as a string
     * 
     * @return The next argument
     * @throws ArgumentException If there are no more arguments
     */
    public String next() throws ArgumentException {
        if (!hasNext()) {
            throw new ArgumentException("No more arguments");
        }
        
        return args[position++];
    }
    
    /**
     * Peek at the next argument without advancing the position
     * 
     * @return The next argument, or null if there are no more arguments
     */
    public String peek() {
        if (!hasNext()) {
            return null;
        }
        
        return args[position];
    }
    
    /**
     * Get the next argument as an integer
     * 
     * @return The next argument as an integer
     * @throws ArgumentException If there are no more arguments or the argument is not a valid integer
     */
    public int nextInt() throws ArgumentException {
        String arg = next();
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            throw new ArgumentException("'" + arg + "' is not a valid integer");
        }
    }
    
    /**
     * Get the next argument as a double
     * 
     * @return The next argument as a double
     * @throws ArgumentException If there are no more arguments or the argument is not a valid double
     */
    public double nextDouble() throws ArgumentException {
        String arg = next();
        try {
            return Double.parseDouble(arg);
        } catch (NumberFormatException e) {
            throw new ArgumentException("'" + arg + "' is not a valid number");
        }
    }
    
    /**
     * Get the next argument as a boolean
     * 
     * @return The next argument as a boolean
     * @throws ArgumentException If there are no more arguments or the argument is not a valid boolean
     */
    public boolean nextBoolean() throws ArgumentException {
        String arg = next().toLowerCase();
        if (arg.equals("true") || arg.equals("yes") || arg.equals("y") || arg.equals("1")) {
            return true;
        } else if (arg.equals("false") || arg.equals("no") || arg.equals("n") || arg.equals("0")) {
            return false;
        } else {
            throw new ArgumentException("'" + arg + "' is not a valid boolean");
        }
    }
    
    /**
     * Get the next argument as a player
     * 
     * @return The next argument as a player
     * @throws ArgumentException If there are no more arguments or the player cannot be found
     */
    public Player nextPlayer() throws ArgumentException {
        String arg = next();
        Player player = Bukkit.getPlayer(arg);
        
        if (player == null) {
            throw new ArgumentException("Player '" + arg + "' not found or not online");
        }
        
        return player;
    }
    
    /**
     * Get the next argument as an offline player
     * 
     * @return The next argument as an offline player
     * @throws ArgumentException If there are no more arguments
     */
    public OfflinePlayer nextOfflinePlayer() throws ArgumentException {
        String arg = next();
        
        // First try UUID format
        if (isUUID(arg)) {
            try {
                UUID uuid = UUID.fromString(arg);
                return Bukkit.getOfflinePlayer(uuid);
            } catch (IllegalArgumentException e) {
                // Fall through to name-based lookup
            }
        }
        
        // Then try by name
        OfflinePlayer player = Bukkit.getOfflinePlayer(arg);
        
        // Check if the player exists
        if (player == null || (!player.hasPlayedBefore() && !player.isOnline())) {
            throw new ArgumentException("Player '" + arg + "' not found");
        }
        
        return player;
    }
    
    /**
     * Get the next argument as a UUID
     * 
     * @return The next argument as a UUID
     * @throws ArgumentException If there are no more arguments or the argument is not a valid UUID
     */
    public UUID nextUUID() throws ArgumentException {
        String arg = next();
        try {
            return UUID.fromString(arg);
        } catch (IllegalArgumentException e) {
            throw new ArgumentException("'" + arg + "' is not a valid UUID");
        }
    }
    
    /**
     * Get the next argument as an enum value
     * 
     * @param <T> The enum type
     * @param enumClass The enum class
     * @return The next argument as an enum value
     * @throws ArgumentException If there are no more arguments or the argument is not a valid enum value
     */
    public <T extends Enum<T>> T nextEnum(Class<T> enumClass) throws ArgumentException {
        String arg = next().toUpperCase();
        try {
            return Enum.valueOf(enumClass, arg);
        } catch (IllegalArgumentException e) {
            // List all valid values
            List<String> validValues = new ArrayList<>();
            for (T value : enumClass.getEnumConstants()) {
                validValues.add(value.name());
            }
            
            throw new ArgumentException("'" + arg + "' is not a valid " + enumClass.getSimpleName() + 
                                     ". Valid values: " + String.join(", ", validValues));
        }
    }
    
    /**
     * Get all remaining arguments joined as a single string
     * 
     * @return All remaining arguments joined as a single string
     */
    public String remainingAsString() {
        StringBuilder builder = new StringBuilder();
        try {
            while (hasNext()) {
                builder.append(next());
                if (hasNext()) {
                    builder.append(" ");
                }
            }
        } catch (ArgumentException e) {
            // This should never happen because we check hasNext()
        }
        return builder.toString();
    }
    
    /**
     * Get the current position
     * 
     * @return The current position
     */
    public int getPosition() {
        return position;
    }
    
    /**
     * Set the current position
     * 
     * @param position The new position
     */
    public void setPosition(int position) {
        if (position < 0 || position > args.length) {
            throw new IllegalArgumentException("Invalid position: " + position);
        }
        this.position = position;
    }
    
    /**
     * Get all arguments
     * 
     * @return All arguments
     */
    public String[] getArgs() {
        return args;
    }
    
    /**
     * Get the length of the arguments array
     * 
     * @return The length of the arguments array
     */
    public int length() {
        return args.length;
    }
    
    /**
     * Check if a string is a valid UUID
     * 
     * @param str The string to check
     * @return true if the string is a valid UUID
     */
    private boolean isUUID(String str) {
        return UUID_PATTERN.matcher(str).matches();
    }
    
    /**
     * Exception thrown when parsing arguments fails
     */
    public static class ArgumentException extends Exception {
        private static final long serialVersionUID = 1L;
        
        public ArgumentException(String message) {
            super(message);
        }
    }
}