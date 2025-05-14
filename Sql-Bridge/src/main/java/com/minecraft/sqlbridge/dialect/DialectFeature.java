// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/dialect/DialectFeature.java
package com.minecraft.sqlbridge.dialect;

/**
 * Enum for database dialect features.
 * This allows checking if a specific dialect supports certain features.
 */
public enum DialectFeature {
    /**
     * Support for returning values in INSERT, UPDATE, or DELETE statements.
     */
    RETURNING_CLAUSE,
    
    /**
     * Support for LIMIT in DELETE statements.
     */
    DELETE_LIMIT,
    
    /**
     * Support for ORDER BY in DELETE statements.
     */
    DELETE_ORDER_BY,
    
    /**
     * Support for UPSERT operations.
     */
    UPSERT,
    
    /**
     * Support for multi-row INSERT statements.
     */
    MULTI_ROW_INSERT,
    
    /**
     * Support for batch operations.
     */
    BATCH_OPERATIONS
}