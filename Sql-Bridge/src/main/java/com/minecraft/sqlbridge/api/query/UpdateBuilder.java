// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/query/UpdateBuilder.java
package com.minecraft.sqlbridge.api.query;

import java.util.Map;

/**
 * Interface for building UPDATE queries.
 */
public interface UpdateBuilder extends QueryBuilder {
    
    /**
     * Set a value for a column in the UPDATE query.
     *
     * @param column The column name
     * @param value The new value
     * @return This UpdateBuilder for chaining
     */
    UpdateBuilder set(String column, Object value);
    
    /**
     * Set values for multiple columns in the UPDATE query.
     *
     * @param columnValues A map of column names to new values
     * @return This UpdateBuilder for chaining
     */
    UpdateBuilder set(Map<String, Object> columnValues);
    
    /**
     * Add a WHERE clause to the query.
     *
     * @param condition The WHERE condition
     * @param parameters The parameters for the condition
     * @return This UpdateBuilder for chaining
     */
    UpdateBuilder where(String condition, Object... parameters);
    
    /**
     * Add an AND condition to the WHERE clause.
     *
     * @param condition The AND condition
     * @param parameters The parameters for the condition
     * @return This UpdateBuilder for chaining
     */
    UpdateBuilder and(String condition, Object... parameters);
    
    /**
     * Add an OR condition to the WHERE clause.
     *
     * @param condition The OR condition
     * @param parameters The parameters for the condition
     * @return This UpdateBuilder for chaining
     */
    UpdateBuilder or(String condition, Object... parameters);
    
    /**
     * Add a LIMIT clause to the query.
     *
     * @param limit The maximum number of rows to update
     * @return This UpdateBuilder for chaining
     */
    UpdateBuilder limit(int limit);
    
    /**
     * Add an ORDER BY clause to the query.
     *
     * @param orderBy The ORDER BY expression
     * @return This UpdateBuilder for chaining
     */
    UpdateBuilder orderBy(String orderBy);
    
    /**
     * Add a RETURNING clause to the query, if supported by the database.
     *
     * @param columns The columns to return
     * @return This UpdateBuilder for chaining
     */
    UpdateBuilder returning(String... columns);
    
    /**
     * Execute the UPDATE query and return the number of affected rows with safe error handling.
     *
     * @param logger The logger to use for error logging
     * @return The number of rows updated, or 0 if the update fails
     */
    default int executeUpdateSafe(java.util.logging.Logger logger) {
        try {
            return executeUpdate();
        } catch (java.sql.SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Update failed: " + getSQL(), e);
            return 0;
        }
    }
}