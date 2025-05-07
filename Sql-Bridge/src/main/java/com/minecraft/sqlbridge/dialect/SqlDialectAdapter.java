package com.minecraft.sqlbridge.dialect;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.api.DatabaseType;

/**
 * Helper class to adapt SQL statements between different database dialects.
 * This helps prevent common errors when plugins use syntax specific to one database type.
 */
public class SqlDialectAdapter {

    /**
     * Adapt an SQL statement to be compatible with the target database type.
     * This method detects and converts common dialect-specific syntax.
     *
     * @param sql The original SQL statement
     * @param targetType The target database type
     * @return The adapted SQL statement
     */
    public static String adaptSql(String sql, DatabaseType targetType) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }

        // Convert to the target dialect
        switch (targetType) {
            case SQLITE:
                return convertToSqlite(sql);
            case MYSQL:
                return convertToMySql(sql);
            case POSTGRESQL:
                return convertToPostgreSql(sql);
            case H2:
                return convertToH2(sql);
            default:
                return sql;
        }
    }

    /**
     * Convert SQL to SQLite compatible syntax
     *
     * @param sql The original SQL statement
     * @return SQLite compatible SQL
     */
    private static String convertToSqlite(String sql) {
        // Replace AUTO_INCREMENT with AUTOINCREMENT (SQLite specific)
        sql = sql.replaceAll("(?i)INT\\s+AUTO_INCREMENT", "INTEGER PRIMARY KEY AUTOINCREMENT");
        sql = sql.replaceAll("(?i)BIGINT\\s+AUTO_INCREMENT", "INTEGER PRIMARY KEY AUTOINCREMENT");
        
        // Replace AUTO_INCREMENT PRIMARY KEY with AUTOINCREMENT (SQLite uses different syntax)
        sql = sql.replaceAll("(?i)AUTO_INCREMENT\\s+PRIMARY\\s+KEY", "PRIMARY KEY AUTOINCREMENT");
        
        // Remove ON UPDATE CURRENT_TIMESTAMP as SQLite doesn't support it
        sql = sql.replaceAll("(?i)\\s+ON\\s+UPDATE\\s+CURRENT_TIMESTAMP", "");
        
        // Fix UNIQUE KEY syntax for SQLite
        sql = sql.replaceAll("(?i)UNIQUE\\s+KEY\\s+(\\w+)\\s*\\(([^\\)]+)\\)", "UNIQUE($2)");
        
        // SQLite doesn't support multiple constraints in a single column definition
        sql = sql.replaceAll("(?i)VARCHAR\\s*\\([^\\)]+\\)\\s+NOT\\s+NULL\\s+UNIQUE", 
                "VARCHAR($1) NOT NULL");
        
        return sql;
    }

    /**
     * Convert SQL to MySQL compatible syntax
     *
     * @param sql The original SQL statement
     * @return MySQL compatible SQL
     */
    private static String convertToMySql(String sql) {
        // Replace SQLite's AUTOINCREMENT with MySQL's AUTO_INCREMENT
        sql = sql.replaceAll("(?i)INTEGER\\s+PRIMARY\\s+KEY\\s+AUTOINCREMENT", 
                "INT AUTO_INCREMENT PRIMARY KEY");
        
        return sql;
    }

    /**
     * Convert SQL to PostgreSQL compatible syntax
     *
     * @param sql The original SQL statement
     * @return PostgreSQL compatible SQL
     */
    private static String convertToPostgreSql(String sql) {
        // Replace AUTO_INCREMENT with SERIAL
        sql = sql.replaceAll("(?i)INT\\s+AUTO_INCREMENT", "SERIAL");
        sql = sql.replaceAll("(?i)BIGINT\\s+AUTO_INCREMENT", "BIGSERIAL");
        
        // Replace AUTO_INCREMENT PRIMARY KEY with SERIAL PRIMARY KEY
        sql = sql.replaceAll("(?i)INT\\s+AUTO_INCREMENT\\s+PRIMARY\\s+KEY", "SERIAL PRIMARY KEY");
        
        return sql;
    }

    /**
     * Convert SQL to H2 compatible syntax
     *
     * @param sql The original SQL statement
     * @return H2 compatible SQL
     */
    private static String convertToH2(String sql) {
        // H2 is mostly compatible with MySQL, but has some differences
        
        // For H2, both AUTO_INCREMENT and IDENTITY work
        return sql;
    }

    /**
     * Log SQL conversion for debugging
     *
     * @param original The original SQL
     * @param converted The converted SQL
     * @param targetType The target database type
     */
    public static void logConversion(String original, String converted, DatabaseType targetType) {
        if (!original.equals(converted)) {
            LogUtil.debug("SQL adapted for " + targetType + ":");
            LogUtil.debug("Original: " + original);
            LogUtil.debug("Converted: " + converted);
        }
    }
}