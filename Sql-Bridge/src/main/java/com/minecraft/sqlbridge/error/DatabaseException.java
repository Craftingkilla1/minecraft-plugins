// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/error/DatabaseException.java
package com.minecraft.sqlbridge.error;

/**
 * Exception class for database errors.
 * This wraps SQLExceptions and other exceptions that occur during database operations.
 */
public class DatabaseException extends RuntimeException {
    
    /**
     * Default constructor.
     */
    public DatabaseException() {
        super();
    }
    
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
     * @param cause The cause of the error
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructor with a cause.
     *
     * @param cause The cause of the error
     */
    public DatabaseException(Throwable cause) {
        super(cause);
    }
}