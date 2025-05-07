package com.minecraft.sqlbridge.query;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.Query;

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
 * Default implementation of the Query interface.
 */
public class DefaultQuery implements Query {

    private final String sql;
    private final Database database;
    private Object[] params;

    /**
     * Create a new query
     *
     * @param sql The SQL statement
     * @param database The database to execute the query on
     */
    public DefaultQuery(String sql, Database database) {
        this.sql = sql;
        this.database = database;
        this.params = new Object[0];
    }

    @Override
    public Query params(Object... params) {
        this.params = params;
        return this;
    }

    @Override
    public <T> List<T> executeQuery(Function<Map<String, Object>, T> mapper) {
        List<T> results = new ArrayList<>();
        
        try (Connection connection = database.getConnection();
             PreparedStatement statement = prepareStatement(connection);
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
            LogUtil.debug("Parameters: " + String.join(", ", convertParamsToStrings()));
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public <T> Optional<T> executeQueryFirst(Function<Map<String, Object>, T> mapper) {
        List<T> results = executeQuery(mapper);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public int executeUpdate() {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = prepareStatement(connection)) {
            
            return statement.executeUpdate();
        } catch (SQLException e) {
            LogUtil.severe("Error executing update: " + e.getMessage());
            LogUtil.severe("SQL: " + sql);
            LogUtil.debug("Parameters: " + String.join(", ", convertParamsToStrings()));
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public long executeInsert() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet generatedKeys = null;
        
        try {
            connection = database.getConnection();
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
                return -1;
            }
        } catch (SQLException e) {
            LogUtil.severe("Error executing insert: " + e.getMessage());
            LogUtil.severe("SQL: " + sql);
            LogUtil.debug("Parameters: " + String.join(", ", convertParamsToStrings()));
            e.printStackTrace();
            return -1;
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
    public String getSql() {
        return sql;
    }

    @Override
    public Object[] getParams() {
        return params;
    }

    /**
     * Prepare a statement with the current parameters
     *
     * @param connection The database connection
     * @return A prepared statement
     * @throws SQLException If there's an error preparing the statement
     */
    private PreparedStatement prepareStatement(Connection connection) throws SQLException {
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
     * @return Array of parameter strings
     */
    private String[] convertParamsToStrings() {
        String[] strings = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            strings[i] = params[i] == null ? "null" : params[i].toString();
        }
        return strings;
    }
}