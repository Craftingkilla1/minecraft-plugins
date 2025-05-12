// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/dialect/DialectFeature.java
package com.minecraft.sqlbridge.dialect;

/**
 * Enumeration of database dialect features.
 */
public enum DialectFeature {
    /**
     * The RETURNING clause for getting values from inserts.
     */
    RETURNING_CLAUSE,
    
    /**
     * Common Table Expressions (WITH clause).
     */
    COMMON_TABLE_EXPRESSIONS,
    
    /**
     * Window functions (OVER clause).
     */
    WINDOW_FUNCTIONS,
    
    /**
     * JSON data type and operations.
     */
    JSON_SUPPORT,
    
    /**
     * Multiple-row insert syntax.
     */
    MULTI_ROW_INSERT,
    
    /**
     * UPSERT (INSERT ... ON DUPLICATE KEY UPDATE).
     */
    UPSERT,
    
    /**
     * CREATE INDEX ... IF NOT EXISTS.
     */
    CREATE_IF_NOT_EXISTS,
    
    /**
     * DROP ... IF EXISTS.
     */
    DROP_IF_EXISTS,
    
    /**
     * Batch parameter binding.
     */
    BATCH_PARAMETERS,
    
    /**
     * LIMIT with OFFSET.
     */
    LIMIT_OFFSET,
    
    /**
     * Generated/computed columns.
     */
    GENERATED_COLUMNS,
    
    /**
     * Foreign key constraints.
     */
    FOREIGN_KEYS,
    
    /**
     * Transactional DDL.
     */
    TRANSACTIONAL_DDL,
    
    /**
     * RENAME TABLE.
     */
    RENAME_TABLE,
    
    /**
     * Full-text search.
     */
    FULLTEXT_SEARCH
}