// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/error/ConnectionException.java
package com.minecraft.sqlbridge.error;

/**
 * Exception thrown when a database connection error occurs.
 */
public class ConnectionException extends DatabaseException {
    
    /**
     * Constructor with a message.
     *
     * @param message The error message
     */
    public ConnectionException(String message) {
        super(message);
    }
    
    /**
     * Constructor with a message and a cause.
     *
     * @param message The error message
     * @param cause The cause of the exception
     */
    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}