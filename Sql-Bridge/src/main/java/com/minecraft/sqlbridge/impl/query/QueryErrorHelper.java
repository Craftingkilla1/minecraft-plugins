// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/impl/query/QueryErrorHelper.java
package com.minecraft.sqlbridge.impl.query;

import java.sql.SQLException;

/**
 * Helper class for providing more user-friendly error messages for common SQL-Bridge errors.
 */
public class QueryErrorHelper {
    
    /**
     * Enhance SQL exception messages with more helpful information.
     *
     * @param e The original SQLException
     * @param sql The SQL query that caused the exception
     * @return An enhanced SQLException with a more helpful message
     */
    public static SQLException enhanceException(SQLException e, String sql) {
        String message = e.getMessage();
        String enhancedMessage = null;
        
        // Check for common error patterns
        if (message.contains("ResultSet") && message.contains("ResultRow")) {
            enhancedMessage = "SQL-Bridge API Error: Your mapper is using java.sql.ResultSet instead of com.minecraft.sqlbridge.api.result.ResultRow. " +
                    "Please modify your mapper method to accept ResultRow instead of ResultSet. Example: " +
                    "ResultMapper<YourType> mapper = (ResultRow row) -> { ... };";
        } else if (message.contains("UUID cannot be converted")) {
            enhancedMessage = "SQL-Bridge API Error: UUID values need to be converted to strings for SQL storage. " +
                    "Use uuid.toString() when passing UUIDs as parameters. Example: " +
                    "database.queryFirst(\"SELECT * FROM table WHERE uuid = ?\", mapper, uuid.toString());";
        } else if (message.contains("executeQueryAsync") || message.contains("method not found")) {
            enhancedMessage = "SQL-Bridge API Error: Method name or signature mismatch. " +
                    "Check that you're using the correct method names: query, queryAsync, queryFirst, queryFirstAsync, update, updateAsync.";
        } else if (message.contains("method reference")) {
            enhancedMessage = "SQL-Bridge API Error: Invalid method reference. " +
                    "Your mapper method signature doesn't match what SQL-Bridge expects. " +
                    "Make sure your method accepts ResultRow, not ResultSet, or use a lambda expression instead.";
        }
        
        if (enhancedMessage != null) {
            SQLException enhancedException = new SQLException(enhancedMessage, e.getSQLState(), e.getErrorCode());
            enhancedException.setNextException(e);
            return enhancedException;
        }
        
        return e;
    }
}