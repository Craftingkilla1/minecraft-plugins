// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/query/AbstractQueryBuilder.java
package com.minecraft.sqlbridge.api.query;

import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.result.ResultMapper;
import com.minecraft.sqlbridge.error.DatabaseException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for query builders.
 * Provides common functionality for all query builders.
 */
public abstract class AbstractQueryBuilder implements QueryBuilder {
    
    protected final Database database;
    protected final List<Object> parameters;
    
    /**
     * Constructor for AbstractQueryBuilder.
     *
     * @param database The database to use for executing queries
     */
    public AbstractQueryBuilder(Database database) {
        this.database = database;
        this.parameters = new ArrayList<>();
    }
    
    /**
     * Add parameters to the parameter list.
     *
     * @param params The parameters to add
     */
    protected void addParameters(Object... params) {
        parameters.addAll(Arrays.asList(params));
    }
    
    @Override
    public Object[] getParameters() {
        return parameters.toArray();
    }
    
    @Override
    public <T> List<T> executeQuery(ResultMapper<T> mapper) throws SQLException {
        String sql = getSQL();
        return database.query(sql, mapper, getParameters());
    }
    
    @Override
    public <T> CompletableFuture<List<T>> executeQueryAsync(ResultMapper<T> mapper) {
        String sql = getSQL();
        return database.queryAsync(sql, mapper, getParameters());
    }
    
    @Override
    public <T> Optional<T> executeQueryFirst(ResultMapper<T> mapper) throws SQLException {
        String sql = getSQL();
        return database.queryFirst(sql, mapper, getParameters());
    }
    
    @Override
    public <T> CompletableFuture<Optional<T>> executeQueryFirstAsync(ResultMapper<T> mapper) {
        String sql = getSQL();
        return database.queryFirstAsync(sql, mapper, getParameters());
    }
    
    @Override
    public int executeUpdate() throws SQLException {
        String sql = getSQL();
        return database.update(sql, getParameters());
    }
    
    @Override
    public CompletableFuture<Integer> executeUpdateAsync() {
        String sql = getSQL();
        return database.updateAsync(sql, getParameters());
    }
    
    /**
     * Execute the query and return the number of affected rows with safe error handling.
     *
     * @param logger The logger to use for error logging
     * @return The number of rows affected, or 0 if the update fails
     */
    public int executeUpdateSafe(java.util.logging.Logger logger) {
        try {
            return executeUpdate();
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Query execution failed: " + getSQL(), e);
            return 0;
        }
    }
}