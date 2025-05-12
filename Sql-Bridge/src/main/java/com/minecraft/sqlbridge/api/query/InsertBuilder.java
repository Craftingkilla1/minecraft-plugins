// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/query/InsertBuilder.java
package com.minecraft.sqlbridge.api.query;

import java.util.Map;

/**
 * Interface for building INSERT queries.
 */
public interface InsertBuilder extends QueryBuilder {
    
    /**
     * Specify the columns for the INSERT statement.
     *
     * @param columns The column names
     * @return This InsertBuilder for chaining
     */
    InsertBuilder columns(String... columns);
    
    /**
     * Specify the values for the INSERT statement.
     *
     * @param values The values to insert
     * @return This InsertBuilder for chaining
     */
    InsertBuilder values(Object... values);
    
    /**
     * Add a row of values for a multi-row INSERT statement.
     *
     * @param values The values for the row
     * @return This InsertBuilder for chaining
     */
    InsertBuilder addRow(Object... values);
    
    /**
     * Specify column-value pairs for the INSERT statement.
     *
     * @param columnValues A map of column names to values
     * @return This InsertBuilder for chaining
     */
    InsertBuilder columnValues(Map<String, Object> columnValues);
    
    /**
     * Add an ON DUPLICATE KEY UPDATE clause to the query.
     *
     * @param column The column to update
     * @param value The value to set
     * @return This InsertBuilder for chaining
     */
    InsertBuilder onDuplicateKeyUpdate(String column, Object value);
    
    /**
     * Add multiple ON DUPLICATE KEY UPDATE clauses to the query.
     *
     * @param columnValues A map of column names to values
     * @return This InsertBuilder for chaining
     */
    InsertBuilder onDuplicateKeyUpdate(Map<String, Object> columnValues);
    
    /**
     * Use a SELECT statement as the source of values.
     *
     * @param selectBuilder The SelectBuilder for the SELECT statement
     * @return This InsertBuilder for chaining
     */
    InsertBuilder select(SelectBuilder selectBuilder);
    
    /**
     * Add a RETURNING clause to the query, if supported by the database.
     *
     * @param columns The columns to return
     * @return This InsertBuilder for chaining
     */
    InsertBuilder returning(String... columns);
}