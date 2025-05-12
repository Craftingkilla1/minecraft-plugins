// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/Database.java
package com.minecraft.sqlbridge.api;

import com.minecraft.sqlbridge.api.query.QueryBuilder;
import com.minecraft.sqlbridge.api.query.SelectBuilder;
import com.minecraft.sqlbridge.api.query.InsertBuilder;
import com.minecraft.sqlbridge.api.query.UpdateBuilder;
import com.minecraft.sqlbridge.api.result.ResultMapper;
import com.minecraft.sqlbridge.api.result.ResultRow;
import com.minecraft.sqlbridge.api.transaction.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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
}