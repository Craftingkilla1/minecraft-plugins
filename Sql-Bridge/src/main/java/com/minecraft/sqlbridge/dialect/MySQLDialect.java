// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/dialect/MySQLDialect.java
package com.minecraft.sqlbridge.dialect;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MySQL database dialect implementation.
 */
public class MySQLDialect implements Dialect {
    
    private static final Set<DialectFeature> SUPPORTED_FEATURES = new HashSet<>(Arrays.asList(
        DialectFeature.MULTI_ROW_INSERT,
        DialectFeature.UPSERT,
        DialectFeature.DROP_IF_EXISTS,
        DialectFeature.BATCH_PARAMETERS,
        DialectFeature.LIMIT_OFFSET,
        DialectFeature.FOREIGN_KEYS,
        DialectFeature.RENAME_TABLE,
        DialectFeature.FULLTEXT_SEARCH,
        DialectFeature.JSON_SUPPORT
    ));
    
    @Override
    public String getDatabaseType() {
        return "mysql";
    }
    
    @Override
    public boolean supportsFeature(DialectFeature feature) {
        return SUPPORTED_FEATURES.contains(feature);
    }
    
    @Override
    public String formatTableName(String tableName) {
        // MySQL uses backticks for table names
        return "`" + tableName + "`";
    }
    
    @Override
    public String formatColumnName(String columnName) {
        // MySQL uses backticks for column names
        return "`" + columnName + "`";
    }
    
    @Override
    public String getLimitClause(int limit, Integer offset) {
        if (offset != null) {
            return "LIMIT " + offset + ", " + limit;
        } else {
            return "LIMIT " + limit;
        }
    }
    
    @Override
    public String getCreateDatabaseSQL(String databaseName) {
        return "CREATE DATABASE IF NOT EXISTS `" + databaseName + 
               "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
    }
    
    @Override
    public String getCreateTableWithAutoIncrementSQL(String tableName, String primaryKeyColumn) {
        return "CREATE TABLE IF NOT EXISTS " + formatTableName(tableName) + " (" +
               formatColumnName(primaryKeyColumn) + " INT AUTO_INCREMENT PRIMARY KEY)";
    }
    
    @Override
    public String getPaginationSQL(String innerQuery, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return innerQuery + " LIMIT " + offset + ", " + pageSize;
    }
    
    @Override
    public String getCaseInsensitiveLikeSQL(String column, String pattern) {
        return column + " LIKE " + pattern + " COLLATE utf8mb4_unicode_ci";
    }
    
    @Override
    public String getParameterPlaceholder(int index) {
        return "?";
    }
    
    @Override
    public String getCurrentTimestampSQL() {
        return "NOW()";
    }
    
    @Override
    public String getRandomOrderSQL() {
        return "RAND()";
    }
    
    @Override
    public String getIfExistsSQL() {
        return "IF EXISTS";
    }
    
    @Override
    public String getBatchInsertSQL(String tableName, String[] columns, int batchSize) {
        String columnList = Arrays.stream(columns)
                                 .map(this::formatColumnName)
                                 .collect(Collectors.joining(", "));
        
        StringBuilder sql = new StringBuilder("INSERT INTO ")
                              .append(formatTableName(tableName))
                              .append(" (").append(columnList).append(") VALUES ");
        
        // Create placeholders for each row
        String rowPlaceholders = "(" + 
                                 Arrays.stream(columns)
                                       .map(c -> "?")
                                       .collect(Collectors.joining(", ")) + 
                                 ")";
        
        // Add placeholders for each batch row
        for (int i = 0; i < batchSize; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(rowPlaceholders);
        }
        
        return sql.toString();
    }
    
    @Override
    public String getReturningClause(String columnName) {
        // MySQL doesn't support RETURNING clause
        return null;
    }
}