package com.minecraft.sqlbridge.dialect;

/**
 * Interface for database-specific SQL dialects.
 * Provides methods for generating SQL statements in a database-specific format.
 */
public interface Dialect {

    /**
     * Get the SQL for creating a table
     *
     * @param tableName The table name
     * @param columns The column definitions
     * @return The SQL statement
     */
    String createTable(String tableName, String... columns);

    /**
     * Get the SQL for dropping a table
     *
     * @param tableName The table name
     * @param ifExists Whether to add IF EXISTS clause
     * @return The SQL statement
     */
    String dropTable(String tableName, boolean ifExists);

    /**
     * Get the SQL for creating an index
     *
     * @param indexName The index name
     * @param tableName The table name
     * @param unique Whether the index should be unique
     * @param columns The columns to include in the index
     * @return The SQL statement
     */
    String createIndex(String indexName, String tableName, boolean unique, String... columns);

    /**
     * Get the SQL for checking if a table exists
     *
     * @param tableName The table name
     * @return The SQL statement
     */
    String tableExists(String tableName);

    /**
     * Get the SQL type for an integer column
     *
     * @param autoIncrement Whether the column should auto-increment
     * @return The SQL type
     */
    String getIntegerType(boolean autoIncrement);

    /**
     * Get the SQL type for a long integer column
     *
     * @param autoIncrement Whether the column should auto-increment
     * @return The SQL type
     */
    String getLongType(boolean autoIncrement);

    /**
     * Get the SQL type for a string column
     *
     * @param length The maximum length of the string, or -1 for unlimited
     * @return The SQL type
     */
    String getStringType(int length);

    /**
     * Get the SQL type for a boolean column
     *
     * @return The SQL type
     */
    String getBooleanType();

    /**
     * Get the SQL type for a timestamp column
     *
     * @return The SQL type
     */
    String getTimestampType();

    /**
     * Get the SQL type for a blob column
     *
     * @return The SQL type
     */
    String getBlobType();

    /**
     * Format a value for use in SQL
     *
     * @param value The value to format
     * @return The formatted value
     */
    String formatValue(Object value);

    /**
     * Get the SQL for a LIKE clause that is case-insensitive
     *
     * @param column The column name
     * @param value The value to match
     * @return The SQL LIKE clause
     */
    String caseInsensitiveLike(String column, String value);

    /**
     * Get the SQL for a pagination clause
     *
     * @param limit The maximum number of rows to return, or -1 for unlimited
     * @param offset The number of rows to skip
     * @return The SQL pagination clause
     */
    String paginate(int limit, int offset);

    /**
     * Get the SQL for getting the last inserted ID
     *
     * @return The SQL statement
     */
    String getLastInsertId();

    /**
     * Get the SQL for a date formatting function
     *
     * @param column The column name or expression
     * @param format The date format pattern
     * @return The SQL function call
     */
    String formatDate(String column, String format);

    /**
     * Get the SQL for concatenating strings
     *
     * @param parts The parts to concatenate
     * @return The SQL function call
     */
    String concat(String... parts);

    /**
     * Get the name of the database type
     *
     * @return The database type name
     */
    String getName();
}