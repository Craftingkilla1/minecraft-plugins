// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/dialect/SQLiteDialect.java
package com.minecraft.sqlbridge.dialect;

import java.util.EnumSet;
import java.util.Set;

/**
 * SQLite dialect implementation.
 */
public class SQLiteDialect implements Dialect {
    
    private static final Set<DialectFeature> SUPPORTED_FEATURES = EnumSet.of(
            DialectFeature.BATCH_OPERATIONS,
            DialectFeature.RETURNING_CLAUSE
    );
    
    @Override
    public String getDatabaseType() {
        return "sqlite";
    }
    
    @Override
    public String formatTableName(String tableName) {
        // SQLite doesn't require quotes for standard table names,
        // but for consistency and to handle names with spaces or special chars,
        // we'll use double quotes
        return "\"" + tableName + "\"";
    }
    
    @Override
    public String formatColumnName(String columnName) {
        // SQLite doesn't require quotes for standard column names,
        // but for consistency and to handle names with spaces or special chars,
        // we'll use double quotes
        return "\"" + columnName + "\"";
    }
    
    @Override
    public String getReturningClause(String columns) {
        // SQLite supports RETURNING since version 3.35.0 (2021-03-12)
        return "RETURNING " + columns;
    }
    
    @Override
    public String getLimitClause(Integer limit, Integer offset) {
        if (limit == null) {
            return "";
        }
        
        StringBuilder clause = new StringBuilder("LIMIT " + limit);
        
        if (offset != null && offset > 0) {
            clause.append(" OFFSET ").append(offset);
        }
        
        return clause.toString();
    }
    
    @Override
    public boolean supportsFeature(DialectFeature feature) {
        return SUPPORTED_FEATURES.contains(feature);
    }
}