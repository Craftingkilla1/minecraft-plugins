package com.minecraft.sqlbridge.api;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Interface for batch database operations.
 * Provides methods for efficiently executing multiple database operations at once.
 */
public interface BatchOperations {
    
    /**
     * Execute a batch insert with the provided statement and batch of parameters
     *
     * @param sql The SQL statement to execute
     * @param batchParams List of parameter arrays for each batch operation
     * @return Array of update counts for each batch operation
     */
    int[] batchInsert(String sql, List<Object[]> batchParams);
    
    /**
     * Execute a batch update with the provided statement and batch of parameters
     *
     * @param sql The SQL statement to execute
     * @param batchParams List of parameter arrays for each batch operation
     * @return Array of update counts for each batch operation
     */
    int[] batchUpdate(String sql, List<Object[]> batchParams);
    
    /**
     * Execute a batch of statements in a single transaction
     *
     * @param statements The SQL statements to execute
     * @return True if all statements were executed successfully
     */
    boolean executeBatch(List<String> statements);
    
    /**
     * Execute multiple object insertions in a batch
     *
     * @param tableName The table to insert into
     * @param columns The columns to insert values for
     * @param objects The objects to insert
     * @param mapper Function to map objects to parameter arrays
     * @param <T> The type of objects to insert
     * @return Array of update counts for each insertion
     */
    <T> int[] batchInsertObjects(String tableName, String[] columns, List<T> objects, Function<T, Object[]> mapper);
    
    /**
     * Execute multiple object updates in a batch
     *
     * @param tableName The table to update
     * @param columns The columns to update
     * @param idColumn The column to use for identifying records to update
     * @param objects The objects to update
     * @param mapper Function to map objects to parameter arrays (including ID as the last parameter)
     * @param <T> The type of objects to update
     * @return Array of update counts for each update
     */
    <T> int[] batchUpdateObjects(String tableName, String[] columns, String idColumn, List<T> objects, Function<T, Object[]> mapper);
    
    /**
     * Execute a batch query with parameters and return the results
     *
     * @param sql The SQL statement to execute
     * @param batchParams List of parameter arrays for each batch operation
     * @param mapper Function to map result sets to objects
     * @param <T> The type of results
     * @return List of lists containing the results for each batch operation
     */
    <T> List<List<T>> batchQuery(String sql, List<Object[]> batchParams, Function<Map<String, Object>, T> mapper);
}