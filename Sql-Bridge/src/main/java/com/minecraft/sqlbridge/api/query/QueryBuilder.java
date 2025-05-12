// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/query/QueryBuilder.java
package com.minecraft.sqlbridge.api.query;

import com.minecraft.sqlbridge.api.result.ResultMapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for building and executing SQL queries.
 */
public interface QueryBuilder {
    
    /**
     * Get the SQL string for this query.
     *
     * @return The SQL string
     */
    String getSQL();
    
    /**
     * Get the query parameters.
     *
     * @return The query parameters as an array
     */
    Object[] getParameters();
    
    /**
     * Execute this query and map the results.
     *
     * @param mapper The result mapper
     * @param <T> The type of objects to return
     * @return A list of mapped objects
     * @throws SQLException If an error occurs during execution
     */
    <T> List<T> executeQuery(ResultMapper<T> mapper) throws SQLException;
    
    /**
     * Execute this query asynchronously and map the results.
     *
     * @param mapper The result mapper
     * @param <T> The type of objects to return
     * @return A CompletableFuture containing a list of mapped objects
     */
    <T> CompletableFuture<List<T>> executeQueryAsync(ResultMapper<T> mapper);
    
    /**
     * Execute this query and return the first result, if any.
     *
     * @param mapper The result mapper
     * @param <T> The type of object to return
     * @return An Optional containing the first result, or empty if no results
     * @throws SQLException If an error occurs during execution
     */
    <T> Optional<T> executeQueryFirst(ResultMapper<T> mapper) throws SQLException;
    
    /**
     * Execute this query asynchronously and return the first result, if any.
     *
     * @param mapper The result mapper
     * @param <T> The type of object to return
     * @return A CompletableFuture containing an Optional with the first result, or empty if no results
     */
    <T> CompletableFuture<Optional<T>> executeQueryFirstAsync(ResultMapper<T> mapper);
    
    /**
     * Execute this query as an update statement.
     *
     * @return The number of rows affected
     * @throws SQLException If an error occurs during execution
     */
    int executeUpdate() throws SQLException;
    
    /**
     * Execute this query asynchronously as an update statement.
     *
     * @return A CompletableFuture containing the number of rows affected
     */
    CompletableFuture<Integer> executeUpdateAsync();
}