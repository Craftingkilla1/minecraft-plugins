package com.minecraft.sqlbridge.impl;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.api.BatchOperations;
import com.minecraft.sqlbridge.api.QueryBuilder;
import com.minecraft.sqlbridge.connection.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * Implementation of batch operations for the DefaultDatabase.
 */
public class DefaultDatabaseBatchImpl implements BatchOperations {

    private final DefaultDatabase database;
    private final ConnectionManager connectionManager;
    private final int batchSize;

    /**
     * Create a new batch operations implementation
     *
     * @param database The parent database
     * @param connectionManager The connection manager
     * @param batchSize The maximum batch size
     */
    public DefaultDatabaseBatchImpl(DefaultDatabase database, ConnectionManager connectionManager, int batchSize) {
        this.database = database;
        this.connectionManager = connectionManager;
        this.batchSize = batchSize;
    }

    @Override
    public int[] batchInsert(String sql, List<Object[]> batchParams) {
        if (batchParams == null || batchParams.isEmpty()) {
            return new int[0];
        }

        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = connectionManager.getConnection();
            statement = connection.prepareStatement(sql);
            
            // Disable auto-commit for batch operations
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            
            int totalBatches = (int) Math.ceil((double) batchParams.size() / batchSize);
            int[] results = new int[batchParams.size()];
            
            try {
                int batchCount = 0;
                int resultIndex = 0;
                
                // Process in batch chunks to avoid OutOfMemory errors with very large batches
                for (Object[] params : batchParams) {
                    // Set parameters for this batch item
                    for (int i = 0; i < params.length; i++) {
                        statement.setObject(i + 1, params[i]);
                    }
                    
                    // Add to the batch
                    statement.addBatch();
                    batchCount++;
                    
                    // Execute if we've reached the batch size or this is the last item
                    if (batchCount >= batchSize || resultIndex == batchParams.size() - 1) {
                        int[] batchResults = statement.executeBatch();
                        
                        // Copy the results to the final results array
                        System.arraycopy(batchResults, 0, results, resultIndex - batchCount + 1, batchResults.length);
                        
                        // Clear batch for the next chunk
                        statement.clearBatch();
                        batchCount = 0;
                    }
                    
                    resultIndex++;
                }
                
                // Commit the transaction
                connection.commit();
                
                return results;
            } catch (SQLException e) {
                // Rollback on error
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    LogUtil.severe("Error rolling back batch insert: " + rollbackEx.getMessage());
                }
                
                throw e;
            } finally {
                // Restore original auto-commit setting
                connection.setAutoCommit(originalAutoCommit);
            }
            
        } catch (SQLException e) {
            LogUtil.severe("Error executing batch insert: " + e.getMessage());
            LogUtil.severe("SQL: " + sql);
            e.printStackTrace();
            return new int[0];
        } finally {
            closeResources(statement, connection);
        }
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> batchParams) {
        // Implementation is the same as batchInsert
        return batchInsert(sql, batchParams);
    }

    @Override
    public boolean executeBatch(List<String> statements) {
        if (statements == null || statements.isEmpty()) {
            return true;
        }

        Connection connection = null;
        Statement statement = null;
        
        try {
            connection = connectionManager.getConnection();
            statement = connection.createStatement();
            
            // Disable auto-commit for batch operations
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            
            try {
                // Add all statements to the batch
                for (String sql : statements) {
                    statement.addBatch(sql);
                }
                
                // Execute all statements
                statement.executeBatch();
                
                // Commit the transaction
                connection.commit();
                
                return true;
            } catch (SQLException e) {
                // Rollback on error
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    LogUtil.severe("Error rolling back batch execution: " + rollbackEx.getMessage());
                }
                
                throw e;
            } finally {
                // Restore original auto-commit setting
                connection.setAutoCommit(originalAutoCommit);
            }
            
        } catch (SQLException e) {
            LogUtil.severe("Error executing batch statements: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            closeResources(statement, connection);
        }
    }

    @Override
    public <T> int[] batchInsertObjects(String tableName, String[] columns, List<T> objects, Function<T, Object[]> mapper) {
        if (objects == null || objects.isEmpty() || columns == null || columns.length == 0) {
            return new int[0];
        }
        
        // Build the SQL insert statement
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");
        
        StringJoiner columnsJoiner = new StringJoiner(", ");
        for (String column : columns) {
            columnsJoiner.add(column);
        }
        
        sql.append(columnsJoiner.toString());
        sql.append(") VALUES (");
        
        StringJoiner valuesJoiner = new StringJoiner(", ");
        for (int i = 0; i < columns.length; i++) {
            valuesJoiner.add("?");
        }
        
        sql.append(valuesJoiner.toString());
        sql.append(")");
        
        // Map objects to parameter arrays
        List<Object[]> batchParams = new ArrayList<>(objects.size());
        for (T object : objects) {
            batchParams.add(mapper.apply(object));
        }
        
        // Execute the batch insert
        return batchInsert(sql.toString(), batchParams);
    }

    @Override
    public <T> int[] batchUpdateObjects(String tableName, String[] columns, String idColumn, List<T> objects, Function<T, Object[]> mapper) {
        if (objects == null || objects.isEmpty() || columns == null || columns.length == 0) {
            return new int[0];
        }
        
        // Build the SQL update statement
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(tableName).append(" SET ");
        
        StringJoiner columnsJoiner = new StringJoiner(", ");
        for (String column : columns) {
            columnsJoiner.add(column + " = ?");
        }
        
        sql.append(columnsJoiner.toString());
        sql.append(" WHERE ").append(idColumn).append(" = ?");
        
        // Map objects to parameter arrays
        List<Object[]> batchParams = new ArrayList<>(objects.size());
        for (T object : objects) {
            batchParams.add(mapper.apply(object));
        }
        
        // Execute the batch update
        return batchUpdate(sql.toString(), batchParams);
    }

    @Override
    public <T> List<List<T>> batchQuery(String sql, List<Object[]> batchParams, Function<Map<String, Object>, T> mapper) {
        if (batchParams == null || batchParams.isEmpty()) {
            return new ArrayList<>();
        }

        List<List<T>> results = new ArrayList<>(batchParams.size());
        
        // Execute each query individually
        // Note: JDBC doesn't support batch SELECT operations directly
        for (Object[] params : batchParams) {
            List<T> queryResult = database.query(sql, mapper, params);
            results.add(queryResult);
        }
        
        return results;
    }
    
    /**
     * Close database resources
     *
     * @param statement The statement to close
     * @param connection The connection to close
     */
    private void closeResources(Statement statement, Connection connection) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                // Ignore
            }
        }
        
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // Ignore
            }
        }
    }
}