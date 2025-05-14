// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/query/InsertBuilder.java
package com.minecraft.sqlbridge.api.query;

import java.util.Map;

/**
 * Interface for building INSERT queries.
 */
public interface InsertBuilder extends QueryBuilder {
    
    /**
     * Specify the columns for the INSERT query.
     *
     * @param columns The column names
     * @return This InsertBuilder for chaining
     */
    InsertBuilder columns(String... columns);
    
    /**
     * Specify the values for the INSERT query.
     *
     * @param values The values to insert
     * @return This InsertBuilder for chaining
     */
    InsertBuilder values(Object... values);
    
    /**
     * Add another row of values for a multi-row INSERT.
     *
     * @param values The values for the additional row
     * @return This InsertBuilder for chaining
     */
    InsertBuilder addRow(Object... values);
    
    /**
     * Specify column-value pairs for the INSERT query.
     *
     * @param columnValues A map of column names to values
     * @return This InsertBuilder for chaining
     */
    InsertBuilder columnValues(Map<String, Object> columnValues);
    
    /**
     * Add ON DUPLICATE KEY UPDATE clause for MySQL.
     *
     * @param column The column to update
     * @param value The new value
     * @return This InsertBuilder for chaining
     */
    InsertBuilder onDuplicateKeyUpdate(String column, Object value);
    
    /**
     * Add ON DUPLICATE KEY UPDATE clause for MySQL with multiple columns.
     *
     * @param columnValues A map of column names to new values
     * @return This InsertBuilder for chaining
     */
    InsertBuilder onDuplicateKeyUpdate(Map<String, Object> columnValues);
    
    /**
     * Use a SELECT query as the source for an INSERT.
     *
     * @param selectBuilder The SELECT query builder
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
    
    /**
     * Execute the INSERT query and return the number of affected rows with safe error handling.
     *
     * @param logger The logger to use for error logging
     * @return The number of rows inserted, or 0 if the insertion fails
     */
    default int executeUpdateSafe(java.util.logging.Logger logger) {
        try {
            return executeUpdate();
        } catch (java.sql.SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Insert failed: " + getSQL(), e);
            return 0;
        }
    }
}