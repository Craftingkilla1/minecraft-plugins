// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/dialect/Dialect.java
package com.minecraft.sqlbridge.dialect;

/**
 * Interface for database-specific SQL dialects.
 * This allows the plugin to adapt SQL queries for different database systems.
 */
public interface Dialect {
    
    /**
     * Get the database type for this dialect.
     *
     * @return The database type
     */
    String getDatabaseType();
    
    /**
     * Check if the dialect supports a specific feature.
     *
     * @param feature The feature to check
     * @return true if the feature is supported, false otherwise
     */
    boolean supportsFeature(DialectFeature feature);
    
    /**
     * Format a table name according to the dialect rules.
     *
     * @param tableName The table name to format
     * @return The formatted table name
     */
    String formatTableName(String tableName);
    
    /**
     * Format a column name according to the dialect rules.
     *
     * @param columnName The column name to format
     * @return The formatted column name
     */
    String formatColumnName(String columnName);
    
    /**
     * Get the SQL for a LIMIT clause.
     *
     * @param limit The limit value
     * @param offset The offset value, or null for no offset
     * @return The SQL for the LIMIT clause
     */
    String getLimitClause(int limit, Integer offset);
    
    /**
     * Get the SQL for creating a database.
     *
     * @param databaseName The name of the database to create
     * @return The SQL for creating the database
     */
    String getCreateDatabaseSQL(String databaseName);
    
    /**
     * Get the SQL for creating a table with an auto-increment primary key.
     *
     * @param tableName The name of the table
     * @param primaryKeyColumn The name of the primary key column
     * @return The SQL for creating the table
     */
    String getCreateTableWithAutoIncrementSQL(String tableName, String primaryKeyColumn);
    
    /**
     * Get the SQL for a pagination query.
     *
     * @param innerQuery The inner query to paginate
     * @param page The page number (1-based)
     * @param pageSize The page size
     * @return The SQL for the pagination query
     */
    String getPaginationSQL(String innerQuery, int page, int pageSize);
    
    /**
     * Get the SQL for a case-insensitive LIKE clause.
     *
     * @param column The column to search
     * @param pattern The search pattern
     * @return The SQL for the case-insensitive LIKE clause
     */
    String getCaseInsensitiveLikeSQL(String column, String pattern);
    
    /**
     * Get the placeholder for a parameter at the specified index.
     * Most dialects use "?", but some might have different syntax.
     *
     * @param index The parameter index (1-based)
     * @return The parameter placeholder
     */
    String getParameterPlaceholder(int index);
    
    /**
     * Get the SQL for the current timestamp.
     *
     * @return The SQL for the current timestamp
     */
    String getCurrentTimestampSQL();
    
    /**
     * Get the SQL for a random ordering.
     *
     * @return The SQL for random ordering
     */
    String getRandomOrderSQL();
    
    /**
     * Get the SQL for an "IF EXISTS" condition for dropping objects.
     *
     * @return The SQL for the IF EXISTS condition
     */
    String getIfExistsSQL();
    
    /**
     * Get the SQL for a batch insert.
     *
     * @param tableName The table name
     * @param columns The column names
     * @param batchSize The number of rows to insert
     * @return The SQL for the batch insert
     */
    String getBatchInsertSQL(String tableName, String[] columns, int batchSize);
    
    /**
     * Get the SQL for a RETURNING clause (if supported).
     *
     * @param columnName The column name to return
     * @return The SQL for the RETURNING clause, or null if not supported
     */
    String getReturningClause(String columnName);
}