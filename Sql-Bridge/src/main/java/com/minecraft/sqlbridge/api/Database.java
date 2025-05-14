// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/Database.java
package com.minecraft.sqlbridge.api;

import com.minecraft.sqlbridge.api.query.QueryBuilder;
import com.minecraft.sqlbridge.api.query.SelectBuilder;
import com.minecraft.sqlbridge.api.query.InsertBuilder;
import com.minecraft.sqlbridge.api.query.UpdateBuilder;
import com.minecraft.sqlbridge.api.query.DeleteBuilder;
import com.minecraft.sqlbridge.api.result.ResultMapper;
import com.minecraft.sqlbridge.api.result.ResultRow;
import com.minecraft.sqlbridge.api.transaction.Transaction;
import com.minecraft.sqlbridge.api.callback.DatabaseCallback;
import com.minecraft.sqlbridge.api.callback.DatabaseResultCallback;
import com.minecraft.sqlbridge.error.DatabaseException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core interface for database operations.
 * Provides methods for executing queries, updates, and managing transactions.
 */
public interface Database {

    /**
     * Execute a SQL query and map the results using the provided mapper.
     *
     * @param sql The SQL query to execute
     * @param mapper The mapper to convert result sets to objects
     * @param params The query parameters
     * @param <T> The type of objects to return
     * @return A list of mapped objects
     * @throws SQLException If an error occurs during execution
     */
    <T> List<T> query(String sql, ResultMapper<T> mapper, Object... params) throws SQLException;

    /**
     * Execute a SQL query asynchronously and map the results using the provided mapper.
     *
     * @param sql The SQL query to execute
     * @param mapper The mapper to convert result sets to objects
     * @param params The query parameters
     * @param <T> The type of objects to return
     * @return A CompletableFuture containing a list of mapped objects
     */
    <T> CompletableFuture<List<T>> queryAsync(String sql, ResultMapper<T> mapper, Object... params);

    /**
     * Execute a SQL query and return the first result, if any.
     *
     * @param sql The SQL query to execute
     * @param mapper The mapper to convert result sets to objects
     * @param params The query parameters
     * @param <T> The type of object to return
     * @return An Optional containing the first result, or empty if no results
     * @throws SQLException If an error occurs during execution
     */
    <T> Optional<T> queryFirst(String sql, ResultMapper<T> mapper, Object... params) throws SQLException;

    /**
     * Execute a SQL query asynchronously and return the first result, if any.
     *
     * @param sql The SQL query to execute
     * @param mapper The mapper to convert result sets to objects
     * @param params The query parameters
     * @param <T> The type of object to return
     * @return A CompletableFuture containing an Optional with the first result, or empty if no results
     */
    <T> CompletableFuture<Optional<T>> queryFirstAsync(String sql, ResultMapper<T> mapper, Object... params);

    /**
     * Execute a SQL update query (INSERT, UPDATE, DELETE).
     *
     * @param sql The SQL update query to execute
     * @param params The query parameters
     * @return The number of rows affected
     * @throws SQLException If an error occurs during execution
     */
    int update(String sql, Object... params) throws SQLException;

    /**
     * Execute a SQL update query asynchronously.
     *
     * @param sql The SQL update query to execute
     * @param params The query parameters
     * @return A CompletableFuture containing the number of rows affected
     */
    CompletableFuture<Integer> updateAsync(String sql, Object... params);

    /**
     * Execute a batch update with a list of parameter sets.
     *
     * @param sql The SQL update query to execute
     * @param parameterSets The list of parameter sets, one for each batch execution
     * @return An array containing the number of rows affected by each batch execution
     * @throws SQLException If an error occurs during execution
     */
    int[] batchUpdate(String sql, List<Object[]> parameterSets) throws SQLException;

    /**
     * Execute a batch update asynchronously.
     *
     * @param sql The SQL update query to execute
     * @param parameterSets The list of parameter sets, one for each batch execution
     * @return A CompletableFuture containing an array with the number of rows affected by each batch execution
     */
    CompletableFuture<int[]> batchUpdateAsync(String sql, List<Object[]> parameterSets);

    /**
     * Create a new SELECT query builder.
     *
     * @return A new SelectBuilder instance
     */
    SelectBuilder select();

    /**
     * Create a new INSERT query builder.
     *
     * @param table The table to insert into
     * @return A new InsertBuilder instance
     */
    InsertBuilder insertInto(String table);

    /**
     * Create a new UPDATE query builder.
     *
     * @param table The table to update
     * @return A new UpdateBuilder instance
     */
    UpdateBuilder update(String table);
    
    /**
     * Create a new DELETE query builder.
     *
     * @param table The table to delete from
     * @return A new DeleteBuilder instance
     */
    default DeleteBuilder deleteFrom(String table) {
        DeleteBuilder builder = (DeleteBuilder) createQuery();
        return builder.from(table);
    }

    /**
     * Create a new query builder for custom queries.
     *
     * @return A new QueryBuilder instance
     */
    QueryBuilder createQuery();

    /**
     * Execute a transaction with the provided transaction function.
     *
     * @param transactionFunction The function to execute within the transaction
     * @param <T> The type of result returned by the transaction
     * @return The result of the transaction
     * @throws SQLException If an error occurs during the transaction
     */
    <T> T executeTransaction(Transaction<T> transactionFunction) throws SQLException;

    /**
     * Execute a transaction asynchronously.
     *
     * @param transactionFunction The function to execute within the transaction
     * @param <T> The type of result returned by the transaction
     * @return A CompletableFuture containing the result of the transaction
     */
    <T> CompletableFuture<T> executeTransactionAsync(Transaction<T> transactionFunction);

    /**
     * Execute a raw query and process the results with a consumer.
     *
     * @param sql The SQL query to execute
     * @param resultConsumer The consumer to process each result row
     * @param params The query parameters
     * @throws SQLException If an error occurs during execution
     */
    void executeQuery(String sql, Consumer<ResultRow> resultConsumer, Object... params) throws SQLException;

    /**
     * Get a raw JDBC connection from the connection pool.
     * Warning: The connection must be closed by the caller.
     *
     * @return A JDBC Connection object
     * @throws SQLException If an error occurs obtaining the connection
     */
    Connection getConnection() throws SQLException;

    /**
     * Check if the database connection is valid.
     *
     * @return true if the connection is valid, false otherwise
     */
    boolean isConnectionValid();

    /**
     * Get database statistics and monitoring information.
     *
     * @return A map of statistics names to values
     */
    Map<String, Object> getStatistics();
    
    /**
     * Execute a SQL query with safe error handling.
     * This method catches and logs exceptions, returning an empty list if an error occurs.
     *
     * @param sql The SQL query to execute
     * @param mapper The mapper to convert result sets to objects
     * @param logger The logger to use for error logging
     * @param params The query parameters
     * @param <T> The type of objects to return
     * @return A list of mapped objects, or an empty list if the query fails
     */
    default <T> List<T> querySafe(String sql, ResultMapper<T> mapper, Logger logger, Object... params) {
        try {
            return query(sql, mapper, params);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Query failed: " + sql, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Execute a SQL query and return the first result with safe error handling.
     * This method catches and logs exceptions, returning an empty Optional if an error occurs.
     *
     * @param sql The SQL query to execute
     * @param mapper The mapper to convert result sets to objects
     * @param logger The logger to use for error logging
     * @param params The query parameters
     * @param <T> The type of object to return
     * @return An Optional containing the first result, or empty if no results or an error occurs
     */
    default <T> Optional<T> queryFirstSafe(String sql, ResultMapper<T> mapper, Logger logger, Object... params) {
        try {
            return queryFirst(sql, mapper, params);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Query first failed: " + sql, e);
            return Optional.empty();
        }
    }
    
    /**
     * Execute a SQL update query with safe error handling.
     * This method catches and logs exceptions, returning 0 if an error occurs.
     *
     * @param sql The SQL update query to execute
     * @param logger The logger to use for error logging
     * @param params The query parameters
     * @return The number of rows affected, or 0 if the update fails
     */
    default int updateSafe(String sql, Logger logger, Object... params) {
        try {
            return update(sql, params);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Update failed: " + sql, e);
            return 0;
        }
    }
    
    /**
     * Execute a batch update with safe error handling.
     * This method catches and logs exceptions, returning an empty array if an error occurs.
     *
     * @param sql The SQL update query to execute
     * @param parameterSets The list of parameter sets, one for each batch execution
     * @param logger The logger to use for error logging
     * @return An array containing the number of rows affected by each batch execution, or an empty array if the batch update fails
     */
    default int[] batchUpdateSafe(String sql, List<Object[]> parameterSets, Logger logger) {
        try {
            return batchUpdate(sql, parameterSets);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Batch update failed: " + sql, e);
            return new int[0];
        }
    }
    
    /**
     * Execute a SQL query with a callback for asynchronous operation.
     *
     * @param sql The SQL query to execute
     * @param mapper The mapper to convert results to objects
     * @param callback The callback to handle the results
     * @param params The query parameters
     * @param <T> The type of objects to return
     */
    default <T> void queryWithCallback(String sql, ResultMapper<T> mapper, 
                                  DatabaseResultCallback<List<T>> callback, Object... params) {
        queryAsync(sql, mapper, params)
            .thenAccept(callback::onSuccess)
            .exceptionally(e -> {
                callback.onError(new DatabaseException("Query failed", e));
                return null;
            });
    }
    
    /**
     * Execute a SQL query and return the first result with a callback for asynchronous operation.
     *
     * @param sql The SQL query to execute
     * @param mapper The mapper to convert results to objects
     * @param callback The callback to handle the result
     * @param params The query parameters
     * @param <T> The type of object to return
     */
    default <T> void queryFirstWithCallback(String sql, ResultMapper<T> mapper, 
                                      DatabaseResultCallback<Optional<T>> callback, Object... params) {
        queryFirstAsync(sql, mapper, params)
            .thenAccept(callback::onSuccess)
            .exceptionally(e -> {
                callback.onError(new DatabaseException("Query first failed", e));
                return null;
            });
    }
    
    /**
     * Execute a SQL update query with a callback for asynchronous operation.
     *
     * @param sql The SQL update query to execute
     * @param callback The callback to handle the result
     * @param params The query parameters
     */
    default void updateWithCallback(String sql, DatabaseResultCallback<Integer> callback, Object... params) {
        updateAsync(sql, params)
            .thenAccept(callback::onSuccess)
            .exceptionally(e -> {
                callback.onError(new DatabaseException("Update failed", e));
                return null;
            });
    }
    
    /**
     * Execute a batch update with a callback for asynchronous operation.
     *
     * @param sql The SQL update query to execute
     * @param parameterSets The list of parameter sets, one for each batch execution
     * @param callback The callback to handle the result
     */
    default void batchUpdateWithCallback(String sql, List<Object[]> parameterSets, 
                                    DatabaseResultCallback<int[]> callback) {
        batchUpdateAsync(sql, parameterSets)
            .thenAccept(callback::onSuccess)
            .exceptionally(e -> {
                callback.onError(new DatabaseException("Batch update failed", e));
                return null;
            });
    }
    
    /**
     * Check if a table exists in the database.
     *
     * @param tableName The name of the table to check
     * @return true if the table exists, false otherwise
     * @throws SQLException If an error occurs during execution
     */
    default boolean tableExists(String tableName) throws SQLException {
        try (Connection conn = getConnection()) {
            return conn.getMetaData().getTables(null, null, tableName, null).next();
        }
    }
    
    /**
     * Check if a table exists in the database with safe error handling.
     *
     * @param tableName The name of the table to check
     * @param logger The logger to use for error logging
     * @return true if the table exists, false otherwise
     */
    default boolean tableExistsSafe(String tableName, Logger logger) {
        try {
            return tableExists(tableName);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking if table exists: " + tableName, e);
            return false;
        }
    }
    
    /**
     * Create a table if it doesn't exist.
     *
     * @param tableName The name of the table
     * @param createTableSQL The SQL statement to create the table
     * @return true if the table exists or was created successfully, false otherwise
     * @throws SQLException If an error occurs during execution
     */
    default boolean createTableIfNotExists(String tableName, String createTableSQL) throws SQLException {
        if (tableExists(tableName)) {
            return true;
        }
        
        // Execute the SQL directly as this is a DDL statement
        return update(createTableSQL, new Object[0]) >= 0;
    }
    
    /**
     * Create a table if it doesn't exist with safe error handling.
     *
     * @param tableName The name of the table
     * @param createTableSQL The SQL statement to create the table
     * @param logger The logger to use for error logging
     * @return true if the table exists or was created successfully, false otherwise
     */
    default boolean createTableIfNotExistsSafe(String tableName, String createTableSQL, Logger logger) {
        try {
            return createTableIfNotExists(tableName, createTableSQL);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating table: " + tableName, e);
            return false;
        }
    }
    
    /**
     * Execute a transaction safely with error handling.
     *
     * @param transactionFunction The function to execute within the transaction
     * @param logger The logger to use for error logging
     * @param <T> The type of result returned by the transaction
     * @return The result of the transaction, or null if the transaction fails
     */
    default <T> T executeTransactionSafe(Transaction<T> transactionFunction, Logger logger) {
        try {
            return executeTransaction(transactionFunction);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Transaction failed", e);
            return null;
        }
    }
}