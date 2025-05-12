// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/result/ResultRow.java
package com.minecraft.sqlbridge.api.result;

import java.sql.SQLException;

/**
 * Interface representing a row of data from a database query result.
 * This abstraction allows for easier mapping of database results to Java objects.
 */
public interface ResultRow {
    
    /**
     * Get a string value from the result row.
     *
     * @param columnName The name of the column
     * @return The string value
     * @throws SQLException If an error occurs while accessing the value
     */
    String getString(String columnName) throws SQLException;
    
    /**
     * Get an integer value from the result row.
     *
     * @param columnName The name of the column
     * @return The integer value
     * @throws SQLException If an error occurs while accessing the value
     */
    int getInt(String columnName) throws SQLException;
    
    /**
     * Get a long value from the result row.
     *
     * @param columnName The name of the column
     * @return The long value
     * @throws SQLException If an error occurs while accessing the value
     */
    long getLong(String columnName) throws SQLException;
    
    /**
     * Get a double value from the result row.
     *
     * @param columnName The name of the column
     * @return The double value
     * @throws SQLException If an error occurs while accessing the value
     */
    double getDouble(String columnName) throws SQLException;
    
    /**
     * Get a boolean value from the result row.
     *
     * @param columnName The name of the column
     * @return The boolean value
     * @throws SQLException If an error occurs while accessing the value
     */
    boolean getBoolean(String columnName) throws SQLException;
    
    /**
     * Get a byte array from the result row.
     *
     * @param columnName The name of the column
     * @return The byte array
     * @throws SQLException If an error occurs while accessing the value
     */
    byte[] getBytes(String columnName) throws SQLException;
    
    /**
     * Get a date value from the result row.
     *
     * @param columnName The name of the column
     * @return The date value
     * @throws SQLException If an error occurs while accessing the value
     */
    java.sql.Date getDate(String columnName) throws SQLException;
    
    /**
     * Get a timestamp value from the result row.
     *
     * @param columnName The name of the column
     * @return The timestamp value
     * @throws SQLException If an error occurs while accessing the value
     */
    java.sql.Timestamp getTimestamp(String columnName) throws SQLException;
    
    /**
     * Get an object value from the result row.
     *
     * @param columnName The name of the column
     * @return The object value
     * @throws SQLException If an error occurs while accessing the value
     */
    Object getObject(String columnName) throws SQLException;
    
    /**
     * Check if a column value is null.
     *
     * @param columnName The name of the column
     * @return true if the value is null, false otherwise
     * @throws SQLException If an error occurs while accessing the value
     */
    boolean isNull(String columnName) throws SQLException;
}