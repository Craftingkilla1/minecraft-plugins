package com.minecraft.sqlbridge.dialect;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringJoiner;

/**
 * SQLite-specific implementation of the SQL dialect.
 */
public class SQLiteDialect implements Dialect {

    @Override
    public String createTable(String tableName, String... columns) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sql.append(tableName).append(" (");
        
        StringJoiner joiner = new StringJoiner(", ");
        for (String column : columns) {
            joiner.add(column);
        }
        
        sql.append(joiner.toString());
        sql.append(")");
        
        return sql.toString();
    }

    @Override
    public String dropTable(String tableName, boolean ifExists) {
        if (ifExists) {
            return "DROP TABLE IF EXISTS " + tableName;
        } else {
            return "DROP TABLE " + tableName;
        }
    }

    @Override
    public String createIndex(String indexName, String tableName, boolean unique, String... columns) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE ");
        
        if (unique) {
            sql.append("UNIQUE ");
        }
        
        sql.append("INDEX IF NOT EXISTS ").append(indexName).append(" ON ").append(tableName).append(" (");
        
        StringJoiner joiner = new StringJoiner(", ");
        for (String column : columns) {
            joiner.add(column);
        }
        
        sql.append(joiner.toString());
        sql.append(")");
        
        return sql.toString();
    }

    @Override
    public String tableExists(String tableName) {
        return "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = '" + tableName + "' LIMIT 1";
    }

    @Override
    public String getIntegerType(boolean autoIncrement) {
        if (autoIncrement) {
            return "INTEGER PRIMARY KEY AUTOINCREMENT";
        } else {
            return "INTEGER";
        }
    }

    @Override
    public String getLongType(boolean autoIncrement) {
        // SQLite doesn't have a separate BIGINT type, it uses INTEGER for all integer types
        if (autoIncrement) {
            return "INTEGER PRIMARY KEY AUTOINCREMENT";
        } else {
            return "INTEGER";
        }
    }

    @Override
    public String getStringType(int length) {
        // SQLite doesn't enforce length restrictions on TEXT
        return "TEXT";
    }

    @Override
    public String getBooleanType() {
        // SQLite doesn't have a boolean type, use INTEGER (0/1)
        return "INTEGER";
    }

    @Override
    public String getTimestampType() {
        // SQLite doesn't have a dedicated timestamp type
        return "TEXT";
    }

    @Override
    public String getBlobType() {
        return "BLOB";
    }

    @Override
    public String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof String) {
            String str = (String) value;
            return "'" + str.replace("'", "''") + "'";
        } else if (value instanceof Date) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return "'" + format.format((Date) value) + "'";
        } else if (value instanceof Boolean) {
            return (Boolean) value ? "1" : "0";
        } else {
            return value.toString();
        }
    }

    @Override
    public String caseInsensitiveLike(String column, String value) {
        // SQLite's LIKE is case-insensitive by default
        return column + " LIKE " + formatValue(value);
    }

    @Override
    public String paginate(int limit, int offset) {
        StringBuilder sql = new StringBuilder();
        
        if (limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }
        
        if (offset > 0) {
            sql.append(" OFFSET ").append(offset);
        }
        
        return sql.toString();
    }

    @Override
    public String getLastInsertId() {
        return "SELECT last_insert_rowid()";
    }

    @Override
    public String formatDate(String column, String format) {
        // SQLite has limited date formatting functions
        // This implementation uses the built-in strftime function
        return "strftime(" + formatValue(convertMySQLToSQLiteFormat(format)) + ", " + column + ")";
    }

    @Override
    public String concat(String... parts) {
        // SQLite uses || operator for concatenation
        if (parts.length == 0) {
            return "''";
        }
        
        StringBuilder sql = new StringBuilder("(");
        
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sql.append(" || ");
            }
            sql.append(parts[i]);
        }
        
        sql.append(")");
        
        return sql.toString();
    }

    @Override
    public String getName() {
        return "SQLite";
    }
    
    /**
     * Convert MySQL date format to SQLite format
     * This is a simple implementation that handles common format patterns
     * 
     * @param mysqlFormat The MySQL format pattern
     * @return The SQLite format pattern
     */
    private String convertMySQLToSQLiteFormat(String mysqlFormat) {
        // This is a simplified conversion and doesn't handle all format specifiers
        String sqliteFormat = mysqlFormat
            .replace("%Y", "%Y")       // Year (4 digits)
            .replace("%y", "%y")       // Year (2 digits)
            .replace("%m", "%m")       // Month (01-12)
            .replace("%d", "%d")       // Day (01-31)
            .replace("%H", "%H")       // Hour (00-23)
            .replace("%h", "%I")       // Hour (01-12)
            .replace("%i", "%M")       // Minute (00-59)
            .replace("%s", "%S")       // Second (00-59)
            .replace("%W", "%W")       // Weekday name (Sunday-Saturday)
            .replace("%M", "%m")       // Month name (January-December)
            .replace("%D", "%d");      // Day with suffix (1st, 2nd, 3rd...)
            
        return sqliteFormat;
    }
}