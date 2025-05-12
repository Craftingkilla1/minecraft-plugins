// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/transaction/TransactionManager.java
package com.minecraft.sqlbridge.api.transaction;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.error.DatabaseException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Manages database transactions.
 */
public class TransactionManager {
    
    private final Executor asyncExecutor;
    
    /**
     * Constructor for TransactionManager.
     *
     * @param asyncExecutor The executor for asynchronous transactions
     */
    public TransactionManager(Executor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }
    
    /**
     * Execute a transaction with the provided connection.
     *
     * @param connection The database connection
     * @param transaction The transaction to execute
     * @param <T> The type of result returned by the transaction
     * @return The result of the transaction
     * @throws SQLException If an error occurs during the transaction
     */
    public <T> T executeTransaction(Connection connection, Transaction<T> transaction) throws SQLException {
        boolean originalAutoCommit = false;
        
        try {
            // Save the original auto-commit state
            originalAutoCommit = connection.getAutoCommit();
            
            // Disable auto-commit for the transaction
            if (originalAutoCommit) {
                connection.setAutoCommit(false);
            }
            
            // Execute the transaction
            T result = transaction.execute(connection);
            
            // Commit the transaction
            connection.commit();
            
            return result;
        } catch (SQLException e) {
            // Rollback the transaction if an error occurs
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                LogUtil.severe("Error rolling back transaction: " + rollbackEx.getMessage());
                e.addSuppressed(rollbackEx);
            }
            
            // Re-throw the exception
            throw e;
        } finally {
            // Restore the original auto-commit state
            if (originalAutoCommit) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                    LogUtil.warning("Error restoring auto-commit state: " + ex.getMessage());
                }
            }
        }
    }
    
    /**
     * Execute a transaction with a savepoint.
     *
     * @param connection The database connection
     * @param transaction The transaction to execute
     * @param savepointName The name of the savepoint
     * @param <T> The type of result returned by the transaction
     * @return The result of the transaction
     * @throws SQLException If an error occurs during the transaction
     */
    public <T> T executeTransactionWithSavepoint(Connection connection, Transaction<T> transaction, String savepointName) throws SQLException {
        Savepoint savepoint = null;
        
        try {
            // Create a savepoint
            savepoint = connection.setSavepoint(savepointName);
            
            // Execute the transaction
            T result = transaction.execute(connection);
            
            // Release the savepoint
            connection.releaseSavepoint(savepoint);
            
            return result;
        } catch (SQLException e) {
            // Rollback to the savepoint if an error occurs
            if (savepoint != null) {
                try {
                    connection.rollback(savepoint);
                } catch (SQLException rollbackEx) {
                    LogUtil.severe("Error rolling back to savepoint: " + rollbackEx.getMessage());
                    e.addSuppressed(rollbackEx);
                }
            }
            
            // Re-throw the exception
            throw e;
        }
    }
    
    /**
     * Execute a transaction asynchronously with a connection supplier.
     *
     * @param connectionSupplier The supplier for the database connection
     * @param transaction The transaction to execute
     * @param <T> The type of result returned by the transaction
     * @return A CompletableFuture for the result of the transaction
     */
    public <T> CompletableFuture<T> executeTransactionAsync(Supplier<Connection> connectionSupplier, Transaction<T> transaction) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = connectionSupplier.get()) {
                return executeTransaction(connection, transaction);
            } catch (SQLException e) {
                throw new DatabaseException("Error executing transaction asynchronously", e);
            }
        }, asyncExecutor);
    }
    
    /**
     * Execute a transaction asynchronously with a savepoint.
     *
     * @param connectionSupplier The supplier for the database connection
     * @param transaction The transaction to execute
     * @param savepointName The name of the savepoint
     * @param <T> The type of result returned by the transaction
     * @return A CompletableFuture for the result of the transaction
     */
    public <T> CompletableFuture<T> executeTransactionWithSavepointAsync(Supplier<Connection> connectionSupplier, Transaction<T> transaction, String savepointName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = connectionSupplier.get()) {
                return executeTransactionWithSavepoint(connection, transaction, savepointName);
            } catch (SQLException e) {
                throw new DatabaseException("Error executing transaction with savepoint asynchronously", e);
            }
        }, asyncExecutor);
    }
}