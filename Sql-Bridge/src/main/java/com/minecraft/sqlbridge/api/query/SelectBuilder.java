// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/query/SelectBuilder.java
package com.minecraft.sqlbridge.api.query;

/**
 * Interface for building SELECT queries.
 */
public interface SelectBuilder extends QueryBuilder {
    
    /**
     * Specify the columns to select.
     *
     * @param columns The column names
     * @return This SelectBuilder for chaining
     */
    SelectBuilder columns(String... columns);
    
    /**
     * Specify the table to select from.
     *
     * @param table The table name
     * @return This SelectBuilder for chaining
     */
    SelectBuilder from(String table);
    
    /**
     * Add a WHERE clause to the query.
     *
     * @param condition The WHERE condition
     * @param parameters The parameters for the condition
     * @return This SelectBuilder for chaining
     */
    SelectBuilder where(String condition, Object... parameters);
    
    /**
     * Add an AND condition to the WHERE clause.
     *
     * @param condition The AND condition
     * @param parameters The parameters for the condition
     * @return This SelectBuilder for chaining
     */
    SelectBuilder and(String condition, Object... parameters);
    
    /**
     * Add an OR condition to the WHERE clause.
     *
     * @param condition The OR condition
     * @param parameters The parameters for the condition
     * @return This SelectBuilder for chaining
     */
    SelectBuilder or(String condition, Object... parameters);
    
    /**
     * Add a JOIN clause to the query.
     *
     * @param type The type of join (INNER, LEFT, RIGHT, etc.)
     * @param table The table to join
     * @param condition The join condition
     * @return This SelectBuilder for chaining
     */
    SelectBuilder join(String type, String table, String condition);
    
    /**
     * Add an INNER JOIN clause to the query.
     *
     * @param table The table to join
     * @param condition The join condition
     * @return This SelectBuilder for chaining
     */
    SelectBuilder innerJoin(String table, String condition);
    
    /**
     * Add a LEFT JOIN clause to the query.
     *
     * @param table The table to join
     * @param condition The join condition
     * @return This SelectBuilder for chaining
     */
    SelectBuilder leftJoin(String table, String condition);
    
    /**
     * Add a RIGHT JOIN clause to the query.
     *
     * @param table The table to join
     * @param condition The join condition
     * @return This SelectBuilder for chaining
     */
    SelectBuilder rightJoin(String table, String condition);
    
    /**
     * Add a GROUP BY clause to the query.
     *
     * @param columns The columns to group by
     * @return This SelectBuilder for chaining
     */
    SelectBuilder groupBy(String... columns);
    
    /**
     * Add a HAVING clause to the query.
     *
     * @param condition The HAVING condition
     * @param parameters The parameters for the condition
     * @return This SelectBuilder for chaining
     */
    SelectBuilder having(String condition, Object... parameters);
    
    /**
     * Add an ORDER BY clause to the query.
     *
     * @param orderBy The ORDER BY expression
     * @return This SelectBuilder for chaining
     */
    SelectBuilder orderBy(String orderBy);
    
    /**
     * Add a LIMIT clause to the query.
     *
     * @param limit The maximum number of rows to return
     * @return This SelectBuilder for chaining
     */
    SelectBuilder limit(int limit);
    
    /**
     * Add an OFFSET clause to the query.
     *
     * @param offset The number of rows to skip
     * @return This SelectBuilder for chaining
     */
    SelectBuilder offset(int offset);
    
    /**
     * Add a FOR UPDATE clause to the query.
     *
     * @return This SelectBuilder for chaining
     */
    SelectBuilder forUpdate();
    
    /**
     * Execute the SELECT query and return the first result with safe error handling.
     *
     * @param mapper The mapper to convert result sets to objects
     * @param logger The logger to use for error logging
     * @param <T> The type of object to return
     * @return An Optional containing the first result, or empty if no results or an error occurs
     */
    default <T> java.util.Optional<T> executeQueryFirstSafe(com.minecraft.sqlbridge.api.result.ResultMapper<T> mapper, java.util.logging.Logger logger) {
        try {
            return executeQueryFirst(mapper);
        } catch (java.sql.SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Query first failed: " + getSQL(), e);
            return java.util.Optional.empty();
        }
    }
    
    /**
     * Execute the SELECT query and map the results with safe error handling.
     *
     * @param mapper The mapper to convert result sets to objects
     * @param logger The logger to use for error logging
     * @param <T> The type of objects to return
     * @return A list of mapped objects, or an empty list if an error occurs
     */
    default <T> java.util.List<T> executeQuerySafe(com.minecraft.sqlbridge.api.result.ResultMapper<T> mapper, java.util.logging.Logger logger) {
        try {
            return executeQuery(mapper);
        } catch (java.sql.SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Query failed: " + getSQL(), e);
            return java.util.Collections.emptyList();
        }
    }
}