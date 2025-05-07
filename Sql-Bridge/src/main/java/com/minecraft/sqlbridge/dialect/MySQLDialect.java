package com.minecraft.sqlbridge.dialect;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringJoiner;

/**
 * MySQL-specific implementation of the SQL dialect.
 */
public class MySQLDialect implements Dialect {

    @Override
    public String createTable(String tableName, String... columns) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sql.append(tableName).append(" (");
        
        StringJoiner joiner = new StringJoiner(", ");
        for (String column : columns) {
            joiner.add(column);
        }
        
        sql.append(joiner.toString());
        sql.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        
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
        
        sql.append("INDEX ").append(indexName).append(" ON ").append(tableName).append(" (");
        
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
        return "SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '" + tableName + "' LIMIT 1";
    }

    @Override
    public String getIntegerType(boolean autoIncrement) {
        if (autoIncrement) {
            return "INT NOT NULL AUTO_INCREMENT";
        } else {
            return "INT";
        }
    }

    @Override
    public String getLongType(boolean autoIncrement) {
        if (autoIncrement) {
            return "BIGINT NOT NULL AUTO_INCREMENT";
        } else {
            return "BIGINT";
        }
    }

    @Override
    public String getStringType(int length) {
        if (length <= 0) {
            return "TEXT";
        } else if (length <= 255) {
            return "VARCHAR(" + length + ")";
        } else if (length <= 65535) {
            return "TEXT";
        } else if (length <= 16777215) {
            return "MEDIUMTEXT";
        } else {
            return "LONGTEXT";
        }
    }

    @Override
    public String getBooleanType() {
        return "TINYINT(1)";
    }

    @Override
    public String getTimestampType() {
        return "TIMESTAMP";
    }

    @Override
    public String getBlobType() {
        return "LONGBLOB";
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
        return column + " LIKE " + formatValue(value) + " COLLATE utf8mb4_unicode_ci";
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
        return "SELECT LAST_INSERT_ID()";
    }

    @Override
    public String formatDate(String column, String format) {
        return "DATE_FORMAT(" + column + ", " + formatValue(format) + ")";
    }

    @Override
    public String concat(String... parts) {
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
        return "MySQL";
    }
}