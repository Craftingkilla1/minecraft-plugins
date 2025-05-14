// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/callback/DatabaseCallback.java
package com.minecraft.sqlbridge.api.callback;

/**
 * Simple callback interface for database operations.
 * This interface is used for operations that don't return a result.
 */
public interface DatabaseCallback {
    
    /**
     * Called when an operation completes successfully.
     */
    void onSuccess();
    
    /**
     * Called when an operation fails.
     *
     * @param e The exception that caused the failure
     */
    void onError(Exception e);
}