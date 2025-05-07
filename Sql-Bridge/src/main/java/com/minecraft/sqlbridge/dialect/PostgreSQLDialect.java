package com.minecraft.sqlbridge.dialect;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringJoiner;

/**
 * PostgreSQL-specific implementation of the SQL dialect.
 */
public class PostgreSQLDialect implements Dialect {

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
        return "SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '" + tableName + "' LIMIT 1";
    }

    @Override
    public String getIntegerType(boolean autoIncrement) {
        if (autoIncrement) {
            return "SERIAL";
        } else {
            return "INTEGER";
        }
    }

    @Override
    public String getLongType(boolean autoIncrement) {
        if (autoIncrement) {
            return "BIGSERIAL";
        } else {
            return "BIGINT";
        }
    }

    @Override
    public String getStringType(int length) {
        if (length <= 0) {
            return "TEXT";
        } else {
            return "VARCHAR(" + length + ")";
        }
    }

    @Override
    public String getBooleanType() {
        return "BOOLEAN";
    }

    @Override
    public String getTimestampType() {
        return "TIMESTAMP WITH TIME ZONE";
    }

    @Override
    public String getBlobType() {
        return "BYTEA";
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
            return (Boolean) value ? "TRUE" : "FALSE";
        } else {
            return value.toString();
        }
    }

    @Override
    public String caseInsensitiveLike(String column, String value) {
        return column + " ILIKE " + formatValue(value);
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
        // PostgreSQL requires specifying the sequence name, which is typically tablename_columnname_seq
        // This is a placeholder; in practice, you'd need to provide the actual sequence name
        return "SELECT lastval()";
    }

    @Override
    public String formatDate(String column, String format) {
        // PostgreSQL uses the to_char function for date formatting
        return "to_char(" + column + ", " + formatValue(convertMySQLToPostgreSQLFormat(format)) + ")";
    }

    @Override
    public String concat(String... parts) {
        if (parts.length == 0) {
            return "''";
        }
        
        StringBuilder sql = new StringBuilder("CONCAT(");
        
        StringJoiner joiner = new StringJoiner(", ");
        for (String part : parts) {
            joiner.add(part);
        }
        
        sql.append(joiner.toString());
        sql.append(")");
        
        return sql.toString();
    }

    @Override
    public String getName() {
        return "PostgreSQL";
    }
    
    /**
     * Convert MySQL date format to PostgreSQL format
     * This is a simple implementation that handles common format patterns
     * 
     * @param mysqlFormat The MySQL format pattern
     * @return The PostgreSQL format pattern
     */
    private String convertMySQLToPostgreSQLFormat(String mysqlFormat) {
        // This is a simplified conversion and doesn't handle all format specifiers
        String postgresFormat = mysqlFormat
            .replace("%Y", "YYYY")     // Year (4 digits)
            .replace("%y", "YY")       // Year (2 digits)
            .replace("%m", "MM")       // Month (01-12)
            .replace("%d", "DD")       // Day (01-31)
            .replace("%H", "HH24")     // Hour (00-23)
            .replace("%h", "HH12")     // Hour (01-12)
            .replace("%i", "MI")       // Minute (00-59)
            .replace("%s", "SS")       // Second (00-59)
            .replace("%W", "Day")      // Weekday name (Sunday-Saturday)
            .replace("%M", "Month")    // Month name (January-December)
            .replace("%D", "DD");      // Day with suffix (1st, 2nd, 3rd...)
            
        return postgresFormat;
    }
}