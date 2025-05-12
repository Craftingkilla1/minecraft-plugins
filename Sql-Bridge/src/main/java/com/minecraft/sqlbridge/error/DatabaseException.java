// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/error/DatabaseException.java
package com.minecraft.sqlbridge.error;

/**
 * Runtime exception for database-related errors.
 */
public class DatabaseException extends RuntimeException {
    
    /**
     * Constructor with a message.
     *
     * @param message The error message
     */
    public DatabaseException(String message) {
        super(message);
    }
    
    /**
     * Constructor with a message and a cause.
     *
     * @param message The error message
     * @param cause The cause of the exception
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}