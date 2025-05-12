// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/dialect/SQLiteDialect.java
package com.minecraft.sqlbridge.dialect;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SQLite database dialect implementation.
 */
public class SQLiteDialect implements Dialect {
    
    private static final Set<DialectFeature> SUPPORTED_FEATURES = new HashSet<>(Arrays.asList(
        DialectFeature.DROP_IF_EXISTS,
        DialectFeature.LIMIT_OFFSET,
        DialectFeature.FOREIGN_KEYS
    ));
    
    @Override
    public String getDatabaseType() {
        return "sqlite";
    }
    
    @Override
    public boolean supportsFeature(DialectFeature feature) {
        return SUPPORTED_FEATURES.contains(feature);
    }
    
    @Override
    public String formatTableName(String tableName) {
        // SQLite uses double quotes for table names
        return "\"" + tableName + "\"";
    }
    
    @Override
    public String formatColumnName(String columnName) {
        // SQLite uses double quotes for column names
        return "\"" + columnName + "\"";
    }
    
    @Override
    public String getLimitClause(int limit, Integer offset) {
        if (offset != null) {
            return "LIMIT " + limit + " OFFSET " + offset;
        } else {
            return "LIMIT " + limit;
        }
    }
    
    @Override
    public String getCreateDatabaseSQL(String databaseName) {
        // SQLite doesn't have a CREATE DATABASE command
        // Databases are created by connecting to a file
        return "";
    }
    
    @Override
    public String getCreateTableWithAutoIncrementSQL(String tableName, String primaryKeyColumn) {
        return "CREATE TABLE IF NOT EXISTS " + formatTableName(tableName) + " (" +
               formatColumnName(primaryKeyColumn) + " INTEGER PRIMARY KEY AUTOINCREMENT)";
    }
    
    @Override
    public String getPaginationSQL(String innerQuery, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return innerQuery + " LIMIT " + pageSize + " OFFSET " + offset;
    }
    
    @Override
    public String getCaseInsensitiveLikeSQL(String column, String pattern) {
        return column + " LIKE " + pattern + " COLLATE NOCASE";
    }
    
    @Override
    public String getParameterPlaceholder(int index) {
        return "?";
    }
    
    @Override
    public String getCurrentTimestampSQL() {
        return "CURRENT_TIMESTAMP";
    }
    
    @Override
    public String getRandomOrderSQL() {
        return "RANDOM()";
    }
    
    @Override
    public String getIfExistsSQL() {
        return "IF EXISTS";
    }
    
    @Override
    public String getBatchInsertSQL(String tableName, String[] columns, int batchSize) {
        // SQLite doesn't support multi-row INSERT syntax
        // We'll create a single-row insert statement that can be executed multiple times
        String columnList = Arrays.stream(columns)
                                 .map(this::formatColumnName)
                                 .collect(Collectors.joining(", "));
        
        return "INSERT INTO " + formatTableName(tableName) + 
               " (" + columnList + ") VALUES (" + 
               Arrays.stream(columns).map(c -> "?").collect(Collectors.joining(", ")) + 
               ")";
    }
    
    @Override
    public String getReturningClause(String columnName) {
        // SQLite doesn't support RETURNING clause
        return null;
    }
}