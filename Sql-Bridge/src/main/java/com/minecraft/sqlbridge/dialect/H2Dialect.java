package com.minecraft.sqlbridge.dialect;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringJoiner;

/**
 * H2-specific implementation of the SQL dialect.
 */
public class H2Dialect implements Dialect {

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
        return "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = '" + tableName.toUpperCase() + "' LIMIT 1";
    }

    @Override
    public String getIntegerType(boolean autoIncrement) {
        if (autoIncrement) {
            return "INT AUTO_INCREMENT";
        } else {
            return "INT";
        }
    }

    @Override
    public String getLongType(boolean autoIncrement) {
        if (autoIncrement) {
            return "BIGINT AUTO_INCREMENT";
        } else {
            return "BIGINT";
        }
    }

    @Override
    public String getStringType(int length) {
        if (length <= 0) {
            return "VARCHAR";
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
        return "TIMESTAMP";
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
            return (Boolean) value ? "TRUE" : "FALSE";
        } else {
            return value.toString();
        }
    }

    @Override
    public String caseInsensitiveLike(String column, String value) {
        return "LOWER(" + column + ") LIKE LOWER(" + formatValue(value) + ")";
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
        return "SELECT SCOPE_IDENTITY()";
    }

    @Override
    public String formatDate(String column, String format) {
        // H2 uses the FORMATDATETIME function for date formatting
        return "FORMATDATETIME(" + column + ", " + formatValue(convertMySQLToH2Format(format)) + ")";
    }

    @Override
    public String concat(String... parts) {
        if (parts.length == 0) {
            return "''";
        }
        
        // H2 supports CONCAT function or || operator
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
        return "H2";
    }
    
    /**
     * Convert MySQL date format to H2 format
     * This is a simple implementation that handles common format patterns
     * 
     * @param mysqlFormat The MySQL format pattern
     * @return The H2 format pattern
     */
    private String convertMySQLToH2Format(String mysqlFormat) {
        // H2's date format is similar to Java's SimpleDateFormat
        String h2Format = mysqlFormat
            .replace("%Y", "yyyy")     // Year (4 digits)
            .replace("%y", "yy")       // Year (2 digits)
            .replace("%m", "MM")       // Month (01-12)
            .replace("%d", "dd")       // Day (01-31)
            .replace("%H", "HH")       // Hour (00-23)
            .replace("%h", "hh")       // Hour (01-12)
            .replace("%i", "mm")       // Minute (00-59)
            .replace("%s", "ss")       // Second (00-59)
            .replace("%W", "EEEE")     // Weekday name (Sunday-Saturday)
            .replace("%M", "MMMM")     // Month name (January-December)
            .replace("%D", "dd");      // Day with suffix (1st, 2nd, 3rd...)
            
        return h2Format;
    }
}