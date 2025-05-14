// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/dialect/Dialect.java
package com.minecraft.sqlbridge.dialect;

/**
 * Interface for database dialects.
 * This provides methods for handling database-specific SQL syntax.
 */
public interface Dialect {
    
    /**
     * Get the name of the database type.
     *
     * @return The database type name (e.g., "mysql", "sqlite")
     */
    String getDatabaseType();
    
    /**
     * Format a table name according to the dialect's rules.
     *
     * @param tableName The raw table name
     * @return The formatted table name
     */
    String formatTableName(String tableName);
    
    /**
     * Format a column name according to the dialect's rules.
     *
     * @param columnName The raw column name
     * @return The formatted column name
     */
    String formatColumnName(String columnName);
    
    /**
     * Get the SQL syntax for a RETURNING clause.
     *
     * @param columns The columns to return, properly formatted
     * @return The SQL for the RETURNING clause, or null if not supported
     */
    String getReturningClause(String columns);
    
    /**
     * Get the SQL syntax for a LIMIT clause.
     *
     * @param limit The limit value
     * @param offset The offset value, can be null
     * @return The SQL for the LIMIT clause
     */
    String getLimitClause(Integer limit, Integer offset);
    
    /**
     * Check if the dialect supports a specific feature.
     *
     * @param feature The feature to check
     * @return true if the feature is supported, false otherwise
     */
    boolean supportsFeature(DialectFeature feature);
}