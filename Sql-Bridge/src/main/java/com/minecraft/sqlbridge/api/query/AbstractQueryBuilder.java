// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/query/AbstractQueryBuilder.java
package com.minecraft.sqlbridge.api.query;

import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.result.ResultMapper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract implementation of QueryBuilder that provides common functionality.
 */
public abstract class AbstractQueryBuilder implements QueryBuilder {
    
    protected final Database database;
    protected final List<Object> parameters;
    
    /**
     * Constructor for AbstractQueryBuilder.
     *
     * @param database The database to use for executing queries
     */
    protected AbstractQueryBuilder(Database database) {
        this.database = database;
        this.parameters = new ArrayList<>();
    }
    
    @Override
    public Object[] getParameters() {
        return parameters.toArray();
    }
    
    @Override
    public <T> List<T> executeQuery(ResultMapper<T> mapper) throws SQLException {
        return database.query(getSQL(), mapper, getParameters());
    }
    
    @Override
    public <T> CompletableFuture<List<T>> executeQueryAsync(ResultMapper<T> mapper) {
        return database.queryAsync(getSQL(), mapper, getParameters());
    }
    
    @Override
    public <T> Optional<T> executeQueryFirst(ResultMapper<T> mapper) throws SQLException {
        return database.queryFirst(getSQL(), mapper, getParameters());
    }
    
    @Override
    public <T> CompletableFuture<Optional<T>> executeQueryFirstAsync(ResultMapper<T> mapper) {
        return database.queryFirstAsync(getSQL(), mapper, getParameters());
    }
    
    @Override
    public int executeUpdate() throws SQLException {
        return database.update(getSQL(), getParameters());
    }
    
    @Override
    public CompletableFuture<Integer> executeUpdateAsync() {
        return database.updateAsync(getSQL(), getParameters());
    }
    
    /**
     * Add parameters to the parameter list.
     *
     * @param newParameters The parameters to add
     */
    protected void addParameters(Object... newParameters) {
        for (Object param : newParameters) {
            parameters.add(param);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SQL: ").append(getSQL()).append("\n");
        sb.append("Parameters: [");
        
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parameters.get(i));
        }
        
        sb.append("]");
        return sb.toString();
    }
}