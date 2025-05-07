package com.minecraft.sqlbridge.api;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Main database interface providing methods for executing queries and transactions.
 * This interface is registered with the Core-Utils service registry for other plugins to use.
 */
public interface Database {
    
    /**
     * Execute a query that returns a result
     * 
     * @param sql The SQL query to execute
     * @param mapper Function to map result set to object
     * @param params Query parameters
     * @param <T> Type of the returned object
     * @return List of results
     */
    <T> List<T> query(String sql, Function<Map<String, Object>, T> mapper, Object... params);
    
    /**
     * Execute a query that returns a single result
     * 
     * @param sql The SQL query to execute
     * @param mapper Function to map result set to object
     * @param params Query parameters
     * @param <T> Type of the returned object
     * @return Optional containing the result, or empty if no result
     */
    <T> Optional<T> queryFirst(String sql, Function<Map<String, Object>, T> mapper, Object... params);
    
    /**
     * Execute an update query (INSERT, UPDATE, DELETE)
     * 
     * @param sql The SQL query to execute
     * @param params Query parameters
     * @return Number of affected rows
     */
    int update(String sql, Object... params);
    
    /**
     * Execute an insert query and return the generated id
     * 
     * @param sql The SQL query to execute
     * @param params Query parameters
     * @return The generated id, or -1 if no id was generated
     */
    long insert(String sql, Object... params) throws SQLException;
    
    /**
     * Execute multiple statements in a single transaction
     * 
     * @param transaction The transaction to execute
     * @param <T> Type of the returned object
     * @return Result of the transaction
     */
    <T> T transaction(Transaction<T> transaction);
    
    /**
     * Get a raw connection from the pool for advanced operations
     * (Connection must be closed by the caller)
     * 
     * @return A database connection
     */
    Connection getConnection() throws SQLException;
    
    /**
     * Check if the database connection is valid
     * 
     * @return true if the connection is valid, false otherwise
     */
    boolean isConnectionValid();
    
    /**
     * Get the query builder for this database
     * 
     * @return A query builder for creating SQL queries
     */
    QueryBuilder createQueryBuilder();
    
    /**
     * Get the database type
     * 
     * @return The database type
     */

    DatabaseType getType();

    /**
     * Execute a batch update with the provided statement and batch of parameters
     * 
     * @param sql The SQL query to execute
     * @param batchParams List of parameter arrays for each batch operation
     * @return Array of update counts for each batch operation
     */
    int[] batchUpdate(String sql, List<Object[]> batchParams);
}