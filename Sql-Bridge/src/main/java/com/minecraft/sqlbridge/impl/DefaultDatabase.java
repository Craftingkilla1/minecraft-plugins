package com.minecraft.sqlbridge.impl;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.api.BatchOperations;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.DatabaseType;
import com.minecraft.sqlbridge.api.Query;
import com.minecraft.sqlbridge.api.QueryBuilder;
import com.minecraft.sqlbridge.api.Transaction;
import com.minecraft.sqlbridge.connection.ConnectionManager;
import com.minecraft.sqlbridge.query.DefaultQueryBuilder;
import com.minecraft.sqlbridge.query.DefaultQuery;
import com.minecraft.sqlbridge.stats.QueryStatistics;

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
import java.util.Optional;
import java.util.function.Function;

/**
 * Default implementation of the Database interface.
 * Provides database connectivity and operations for the SQL-Bridge plugin.
 */
public class DefaultDatabase implements Database, BatchOperations {

    private final SqlBridgePlugin plugin;
    private final ConnectionManager connectionManager;
    private final DefaultDatabaseBatchImpl batchOperations;
    private final int defaultBatchSize = 100;

    /**
     * Create a new default database
     *
     * @param plugin The plugin instance
     * @param connectionManager The connection manager
     */
    public DefaultDatabase(SqlBridgePlugin plugin, ConnectionManager connectionManager) {
        this.plugin = plugin;
        this.connectionManager = connectionManager;
        this.batchOperations = new DefaultDatabaseBatchImpl(this, connectionManager, 
                plugin.getConfig().getInt("batch.size", defaultBatchSize));
    }

    @Override
    public <T> List<T> query(String sql, Function<Map<String, Object>, T> mapper, Object... params) {
        List<T> results = new ArrayList<>();
        
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = prepareStatement(connection, sql, params);
             ResultSet resultSet = statement.executeQuery()) {
            
            // Process the results
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                
                // Map column names to values
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                
                // Apply mapper function
                results.add(mapper.apply(row));
            }
            
            return results;
        } catch (SQLException e) {
            LogUtil.severe("Error executing query: " + e.getMessage());
            LogUtil.severe("SQL: " + sql);
            LogUtil.debug("Parameters: " + String.join(", ", convertParamsToStrings(params)));
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public <T> Optional<T> queryFirst(String sql, Function<Map<String, Object>, T> mapper, Object... params) {
        List<T> results = query(sql, mapper, params);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public int update(String sql, Object... params) {
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = prepareStatement(connection, sql, params)) {
            
            return statement.executeUpdate();
        } catch (SQLException e) {
            LogUtil.severe("Error executing update: " + e.getMessage());
            LogUtil.severe("SQL: " + sql);
            LogUtil.debug("Parameters: " + String.join(", ", convertParamsToStrings(params)));
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public long insert(String sql, Object... params) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet generatedKeys = null;
        
        try {
            connection = connectionManager.getConnection();
            statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            // Set parameters
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            
            // Execute the insert
            statement.executeUpdate();
            
            // Get the generated key
            generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getLong(1);
            } else {
                // Try to get the last insert ID using dialect-specific SQL
                try (Statement idStatement = connection.createStatement();
                     ResultSet idResult = idStatement.executeQuery(
                         connectionManager.getDialect().getLastInsertId())) {
                    
                    if (idResult.next()) {
                        return idResult.getLong(1);
                    }
                }
                
                return -1;
            }
        } catch (SQLException e) {
            LogUtil.severe("Error executing insert: " + e.getMessage());
            LogUtil.severe("SQL: " + sql);
            LogUtil.debug("Parameters: " + String.join(", ", convertParamsToStrings(params)));
            throw e;
        } finally {
            // Close resources
            if (generatedKeys != null) {
                try {
                    generatedKeys.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
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

    @Override
    public <T> T transaction(Transaction<T> transaction) {
        Connection connection = null;
        boolean originalAutoCommit = false;
        
        try {
            // Get a connection and disable auto-commit
            connection = connectionManager.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            
            // Execute the transaction
            T result = transaction.execute(connection);
            
            // Commit the transaction
            connection.commit();
            
            return result;
        } catch (SQLException e) {
            // Rollback the transaction
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    LogUtil.severe("Error rolling back transaction: " + rollbackEx.getMessage());
                    rollbackEx.printStackTrace();
                }
            }
            
            LogUtil.severe("Error executing transaction: " + e.getMessage());
            e.printStackTrace();
            
            // Rethrow as runtime exception
            throw new RuntimeException("Transaction failed", e);
        } finally {
            // Restore auto-commit and close the connection
            if (connection != null) {
                try {
                    connection.setAutoCommit(originalAutoCommit);
                    connection.close();
                } catch (SQLException e) {
                    LogUtil.warning("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connectionManager.getConnection();
    }

    @Override
    public boolean isConnectionValid() {
        try (Connection connection = connectionManager.getConnection()) {
            return connection != null && connection.isValid(3);
        } catch (SQLException e) {
            LogUtil.warning("Connection validation failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public QueryBuilder createQueryBuilder() {
        return new DefaultQueryBuilder(connectionManager.getDialect());
    }

    @Override
    public DatabaseType getType() {
        return connectionManager.getType();
    }

    /**
     * Create a prepared statement with parameters
     *
     * @param connection The database connection
     * @param sql The SQL statement
     * @param params The parameters
     * @return A prepared statement
     * @throws SQLException If there's an error preparing the statement
     */
    private PreparedStatement prepareStatement(Connection connection, String sql, Object... params) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        
        // Set parameters
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
        
        return statement;
    }

    /**
     * Convert query parameters to strings for logging
     *
     * @param params The parameters
     * @return Array of parameter strings
     */
    private String[] convertParamsToStrings(Object... params) {
        String[] strings = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            strings[i] = params[i] == null ? "null" : params[i].toString();
        }
        return strings;
    }
    
    // BatchOperations implementation
    
    @Override
    public int[] batchInsert(String sql, List<Object[]> batchParams) {
        return batchOperations.batchInsert(sql, batchParams);
    }
    
    @Override
    public int[] batchUpdate(String sql, List<Object[]> batchParams) {
        return batchOperations.batchUpdate(sql, batchParams);
    }
    
    @Override
    public boolean executeBatch(List<String> statements) {
        return batchOperations.executeBatch(statements);
    }
    
    @Override
    public <T> int[] batchInsertObjects(String tableName, String[] columns, List<T> objects, Function<T, Object[]> mapper) {
        return batchOperations.batchInsertObjects(tableName, columns, objects, mapper);
    }
    
    @Override
    public <T> int[] batchUpdateObjects(String tableName, String[] columns, String idColumn, List<T> objects, Function<T, Object[]> mapper) {
        return batchOperations.batchUpdateObjects(tableName, columns, idColumn, objects, mapper);
    }
    
    @Override
    public <T> List<List<T>> batchQuery(String sql, List<Object[]> batchParams, Function<Map<String, Object>, T> mapper) {
        return batchOperations.batchQuery(sql, batchParams, mapper);
    }
}