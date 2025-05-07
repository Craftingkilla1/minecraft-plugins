package com.minecraft.sqlbridge.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Represents a prepared SQL query with parameters.
 * This interface provides methods for executing a query with different
 * result transformations.
 */
public interface Query {
    
    /**
     * Set parameters for the query
     * 
     * @param params The parameters for the query
     * @return The query with parameters set
     */
    Query params(Object... params);
    
    /**
     * Execute the query and return multiple results
     * 
     * @param mapper Function to map each row to an object
     * @param <T> Type of the returned object
     * @return List of results
     */
    <T> List<T> executeQuery(Function<Map<String, Object>, T> mapper);
    
    /**
     * Execute the query and return the first result
     * 
     * @param mapper Function to map the row to an object
     * @param <T> Type of the returned object
     * @return Optional containing the result, or empty if no result
     */
    <T> Optional<T> executeQueryFirst(Function<Map<String, Object>, T> mapper);
    
    /**
     * Execute an update query and return the number of affected rows
     * 
     * @return Number of affected rows
     */
    int executeUpdate();
    
    /**
     * Execute an insert query and return the generated ID
     * 
     * @return The generated ID
     */
    long executeInsert();
    
    /**
     * Get the SQL statement
     * 
     * @return The SQL statement
     */
    String getSql();
    
    /**
     * Get the parameters for the query
     * 
     * @return The parameters for the query
     */
    Object[] getParams();
}