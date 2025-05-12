// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/result/ResultMapper.java
package com.minecraft.sqlbridge.api.result;

import java.sql.SQLException;

/**
 * Functional interface for mapping database results to Java objects.
 * This interface is used to convert a database result row into a strongly-typed object.
 *
 * @param <T> The type of object to map the result to
 */
@FunctionalInterface
public interface ResultMapper<T> {
    
    /**
     * Maps a database result row to an object of type T.
     *
     * @param row The database result row
     * @return The mapped object
     * @throws SQLException If an error occurs while mapping the result
     */
    T map(ResultRow row) throws SQLException;
}