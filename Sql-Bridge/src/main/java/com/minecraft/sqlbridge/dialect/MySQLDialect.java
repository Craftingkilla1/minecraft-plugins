// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/dialect/MySQLDialect.java
package com.minecraft.sqlbridge.dialect;

import java.util.EnumSet;
import java.util.Set;

/**
 * MySQL dialect implementation.
 */
public class MySQLDialect implements Dialect {
    
    private static final Set<DialectFeature> SUPPORTED_FEATURES = EnumSet.of(
            DialectFeature.DELETE_LIMIT,
            DialectFeature.DELETE_ORDER_BY,
            DialectFeature.MULTI_ROW_INSERT,
            DialectFeature.BATCH_OPERATIONS,
            DialectFeature.UPSERT
    );
    
    @Override
    public String getDatabaseType() {
        return "mysql";
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
    public String getReturningClause(String columns) {
        // MySQL doesn't support RETURNING, but some versions 
        // have a non-standard way using SELECT LAST_INSERT_ID()
        // For simplicity, we'll return null here
        return null;
    }
    
    @Override
    public String getLimitClause(Integer limit, Integer offset) {
        if (limit == null) {
            return "";
        }
        
        if (offset != null && offset > 0) {
            return "LIMIT " + offset + ", " + limit;
        } else {
            return "LIMIT " + limit;
        }
    }
    
    @Override
    public boolean supportsFeature(DialectFeature feature) {
        return SUPPORTED_FEATURES.contains(feature);
    }
}