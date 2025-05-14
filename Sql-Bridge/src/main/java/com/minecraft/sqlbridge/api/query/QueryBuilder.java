// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/query/QueryBuilder.java
package com.minecraft.sqlbridge.api.query;

import com.minecraft.sqlbridge.api.result.ResultMapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for building SQL queries.
 * This is the base interface for all query builders.
 */
public interface QueryBuilder {
    
    /**
     * Get the SQL query string.
     *
     * @return The SQL query string
     */
    String getSQL();
    
    /**
     * Get the parameters for the query.
     *
     * @return An array of parameters for the query
     */
    Object[] getParameters();
    
    /**
     * Execute the query and map the results using the provided mapper.
     *
     * @param mapper The mapper to convert result sets to objects
     * @param <T> The type of objects to return
     * @return A list of mapped objects
     * @throws SQLException If an error occurs during execution
     */
    <T> List<T> executeQuery(ResultMapper<T> mapper) throws SQLException;
    
    /**
     * Execute the query asynchronously and map the results using the provided mapper.
     *
     * @param mapper The mapper to convert result sets to objects
     * @param <T> The type of objects to return
     * @return A CompletableFuture containing a list of mapped objects
     */
    <T> CompletableFuture<List<T>> executeQueryAsync(ResultMapper<T> mapper);
    
    /**
     * Execute the query and return the first result, if any.
     *
     * @param mapper The mapper to convert result sets to objects
     * @param <T> The type of object to return
     * @return An Optional containing the first result, or empty if no results
     * @throws SQLException If an error occurs during execution
     */
    <T> Optional<T> executeQueryFirst(ResultMapper<T> mapper) throws SQLException;
    
    /**
     * Execute the query asynchronously and return the first result, if any.
     *
     * @param mapper The mapper to convert result sets to objects
     * @param <T> The type of object to return
     * @return A CompletableFuture containing an Optional with the first result, or empty if no results
     */
    <T> CompletableFuture<Optional<T>> executeQueryFirstAsync(ResultMapper<T> mapper);
    
    /**
     * Execute the query as an update operation.
     *
     * @return The number of rows affected
     * @throws SQLException If an error occurs during execution
     */
    int executeUpdate() throws SQLException;
    
    /**
     * Execute the query as an update operation asynchronously.
     *
     * @return A CompletableFuture containing the number of rows affected
     */
    CompletableFuture<Integer> executeUpdateAsync();
}