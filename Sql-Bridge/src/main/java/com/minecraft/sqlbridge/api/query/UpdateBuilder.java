// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/query/UpdateBuilder.java
package com.minecraft.sqlbridge.api.query;

import java.util.Map;

/**
 * Interface for building UPDATE queries.
 */
public interface UpdateBuilder extends QueryBuilder {
    
    /**
     * Set a column to a value.
     *
     * @param column The column to set
     * @param value The value to set
     * @return This UpdateBuilder for chaining
     */
    UpdateBuilder set(String column, Object value);
    
    /**
     * Set multiple columns to values.
     *
     * @param columnValues A map of column names to values
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
}