// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/query/DeleteBuilder.java
package com.minecraft.sqlbridge.api.query;

/**
 * Interface for building DELETE queries.
 */
public interface DeleteBuilder extends QueryBuilder {
    
    /**
     * Specify the table to delete from.
     *
     * @param table The table name
     * @return This DeleteBuilder for chaining
     */
    DeleteBuilder from(String table);
    
    /**
     * Add a WHERE clause to the query.
     *
     * @param condition The WHERE condition
     * @param parameters The parameters for the condition
     * @return This DeleteBuilder for chaining
     */
    DeleteBuilder where(String condition, Object... parameters);
    
    /**
     * Add an AND condition to the WHERE clause.
     *
     * @param condition The AND condition
     * @param parameters The parameters for the condition
     * @return This DeleteBuilder for chaining
     */
    DeleteBuilder and(String condition, Object... parameters);
    
    /**
     * Add an OR condition to the WHERE clause.
     *
     * @param condition The OR condition
     * @param parameters The parameters for the condition
     * @return This DeleteBuilder for chaining
     */
    DeleteBuilder or(String condition, Object... parameters);
    
    /**
     * Add a LIMIT clause to the query.
     *
     * @param limit The maximum number of rows to delete
     * @return This DeleteBuilder for chaining
     */
    DeleteBuilder limit(int limit);
    
    /**
     * Add an ORDER BY clause to the query.
     *
     * @param orderBy The ORDER BY expression
     * @return This DeleteBuilder for chaining
     */
    DeleteBuilder orderBy(String orderBy);
    
    /**
     * Add a RETURNING clause to the query, if supported by the database.
     *
     * @param columns The columns to return
     * @return This DeleteBuilder for chaining
     */
    DeleteBuilder returning(String... columns);
    
    /**
     * Execute the DELETE query and return the number of affected rows.
     * This is a safe version that catches and logs exceptions.
     *
     * @param logger The logger to use for error logging
     * @return The number of rows deleted, or 0 if the deletion fails
     */
    default int executeUpdateSafe(java.util.logging.Logger logger) {
        try {
            return executeUpdate();
        } catch (java.sql.SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Delete failed: " + getSQL(), e);
            return 0;
        }
    }
}