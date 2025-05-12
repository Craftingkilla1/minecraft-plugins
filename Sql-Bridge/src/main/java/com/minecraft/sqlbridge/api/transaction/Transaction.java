// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/transaction/Transaction.java
package com.minecraft.sqlbridge.api.transaction;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Functional interface for executing operations within a transaction.
 *
 * @param <T> The type of result returned by the transaction
 */
@FunctionalInterface
public interface Transaction<T> {
    
    /**
     * Execute operations within a transaction.
     * 
     * @param connection The database connection to use
     * @return The result of the transaction
     * @throws SQLException If an error occurs during the transaction
     */
    T execute(Connection connection) throws SQLException;
}