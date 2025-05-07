package com.minecraft.sqlbridge.api;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Functional interface for executing database transactions.
 * Ensures that multiple database operations are executed atomically.
 *
 * @param <T> The type of the result of the transaction
 */
@FunctionalInterface
public interface Transaction<T> {
    
    /**
     * Execute the transaction with the provided connection.
     * The connection will be automatically committed or rolled back
     * depending on whether an exception is thrown.
     *
     * @param connection The database connection with auto-commit disabled
     * @return The result of the transaction
     * @throws SQLException If an error occurs during the transaction
     */
    T execute(Connection connection) throws SQLException;
}