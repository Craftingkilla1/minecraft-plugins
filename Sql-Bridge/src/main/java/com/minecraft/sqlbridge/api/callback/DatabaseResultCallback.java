// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/callback/DatabaseResultCallback.java
package com.minecraft.sqlbridge.api.callback;

/**
 * Callback interface for database operations that return a result.
 *
 * @param <T> The type of result returned by the operation
 */
public interface DatabaseResultCallback<T> {
    
    /**
     * Called when an operation completes successfully.
     *
     * @param result The result of the operation
     */
    void onSuccess(T result);
    
    /**
     * Called when an operation fails.
     *
     * @param e The exception that caused the failure
     */
    void onError(Exception e);
}