// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/impl/DefaultDatabase.java
package com.minecraft.sqlbridge.impl;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.query.QueryBuilder;
import com.minecraft.sqlbridge.api.query.SelectBuilder;
import com.minecraft.sqlbridge.api.query.InsertBuilder;
import com.minecraft.sqlbridge.api.query.UpdateBuilder;
import com.minecraft.sqlbridge.api.query.DeleteBuilder;
import com.minecraft.sqlbridge.api.result.ResultMapper;
import com.minecraft.sqlbridge.api.result.ResultRow;
import com.minecraft.sqlbridge.api.transaction.Transaction;
import com.minecraft.sqlbridge.error.DatabaseException;
import com.minecraft.sqlbridge.monitoring.QueryStatistics;
import com.minecraft.sqlbridge.security.QueryValidator;
import com.minecraft.sqlbridge.impl.query.DefaultDeleteBuilder;
import com.minecraft.sqlbridge.dialect.Dialect;
import com.minecraft.sqlbridge.dialect.MySQLDialect;
import com.minecraft.sqlbridge.dialect.SQLiteDialect;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of the Database interface.
 */
public class DefaultDatabase implements Database {

    private final SqlBridgePlugin plugin;
    private final DataSource dataSource;
    private final QueryValidator queryValidator;
    private final QueryStatistics queryStatistics;
    private final Executor asyncExecutor;
    private final Dialect dialect;

    /**
     * Constructor for DefaultDatabase.
     *
     * @param plugin The SQL-Bridge plugin instance
     * @param dataSource The data source for database connections
     */
    public DefaultDatabase(SqlBridgePlugin plugin, DataSource dataSource) {
        this.plugin = plugin;
        this.dataSource = dataSource;
        this.queryValidator = new QueryValidator(plugin);
        this.queryStatistics = new QueryStatistics(plugin);
        this.asyncExecutor = Executors.newFixedThreadPool(
                plugin.getPluginConfig().getDatabaseThreadPoolSize(),
                r -> {
                    Thread thread = new Thread(r, "SqlBridge-AsyncExecutor");
                    thread.setDaemon(true);
                    return thread;
                });
                
        // Determine the dialect based on the configured database type
        String dbType = plugin.getPluginConfig().getDatabaseType().toLowerCase();
        if (dbType.equals("mysql")) {
            this.dialect = new MySQLDialect();
        } else {
            this.dialect = new SQLiteDialect();
        }
    }

    @Override
    public <T> List<T> query(String sql, ResultMapper<T> mapper, Object... params) throws SQLException {
        long startTime = System.currentTimeMillis();
        List<T> results = new ArrayList<>();
        
        // Validate query for security
        queryValidator.validateQuery(sql);
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = prepareStatement(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                ResultRow resultRow = new DefaultResultRow(rs);
                results.add(mapper.map(resultRow));
            }
            
            // Record query statistics
            long endTime = System.currentTimeMillis();
            queryStatistics.recordQuery(sql, endTime - startTime);
            
            return results;
        } catch (SQLException e) {
            LogUtil.severe("SQL error executing query: " + e.getMessage());
            queryStatistics.recordFailedQuery(sql);
            throw e;
        }
    }

    @Override
    public <T> CompletableFuture<List<T>> queryAsync(String sql, ResultMapper<T> mapper, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return query(sql, mapper, params);
            } catch (SQLException e) {
                throw new DatabaseException("Error executing async query", e);
            }
        }, asyncExecutor);
    }

    @Override
    public <T> Optional<T> queryFirst(String sql, ResultMapper<T> mapper, Object... params) throws SQLException {
        List<T> results = query(sql, mapper, params);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public <T> CompletableFuture<Optional<T>> queryFirstAsync(String sql, ResultMapper<T> mapper, Object... params) {
        return queryAsync(sql, mapper, params)
                .thenApply(results -> results.isEmpty() ? Optional.empty() : Optional.of(results.get(0)));
    }

    @Override
    public int update(String sql, Object... params) throws SQLException {
        long startTime = System.currentTimeMillis();
        
        // Validate query for security
        queryValidator.validateQuery(sql);
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = prepareStatement(conn, sql, params)) {
            
            int rowsAffected = stmt.executeUpdate();
            
            // Record query statistics
            long endTime = System.currentTimeMillis();
            queryStatistics.recordUpdate(sql, endTime - startTime);
            
            return rowsAffected;
        } catch (SQLException e) {
            LogUtil.severe("SQL error executing update: " + e.getMessage());
            queryStatistics.recordFailedUpdate(sql);
            throw e;
        }
    }

    @Override
    public CompletableFuture<Integer> updateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return update(sql, params);
            } catch (SQLException e) {
                throw new DatabaseException("Error executing async update", e);
            }
        }, asyncExecutor);
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> parameterSets) throws SQLException {
        long startTime = System.currentTimeMillis();
        
        // Validate query for security
        queryValidator.validateQuery(sql);
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (Object[] params : parameterSets) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                stmt.addBatch();
            }
            
            int[] results = stmt.executeBatch();
            
            // Record query statistics
            long endTime = System.currentTimeMillis();
            queryStatistics.recordBatchUpdate(sql, parameterSets.size(), endTime - startTime);
            
            return results;
        } catch (SQLException e) {
            LogUtil.severe("SQL error executing batch update: " + e.getMessage());
            queryStatistics.recordFailedBatchUpdate(sql);
            throw e;
        }
    }

    @Override
    public CompletableFuture<int[]> batchUpdateAsync(String sql, List<Object[]> parameterSets) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return batchUpdate(sql, parameterSets);
            } catch (SQLException e) {
                throw new DatabaseException("Error executing async batch update", e);
            }
        }, asyncExecutor);
    }

    @Override
    public SelectBuilder select() {
        return new DefaultSelectBuilder(this);
    }

    @Override
    public InsertBuilder insertInto(String table) {
        return new DefaultInsertBuilder(this, table);
    }

    @Override
    public UpdateBuilder update(String table) {
        return new DefaultUpdateBuilder(this, table);
    }

    @Override
    public QueryBuilder createQuery() {
        return new DefaultQueryBuilder(this);
    }
    
    @Override
    public DeleteBuilder deleteFrom(String table) {
        return new DefaultDeleteBuilder(this, dialect).from(table);
    }

    @Override
    public <T> T executeTransaction(Transaction<T> transactionFunction) throws SQLException {
        Connection conn = null;
        boolean originalAutoCommit = true;
        
        try {
            conn = getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            T result = transactionFunction.execute(conn);
            
            conn.commit();
            return result;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    LogUtil.severe("Error rolling back transaction: " + rollbackEx.getMessage());
                }
            }
            
            if (e instanceof SQLException) {
                throw (SQLException) e;
            } else {
                throw new DatabaseException("Error executing transaction", e);
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                    conn.close();
                } catch (SQLException closeEx) {
                    LogUtil.warning("Error closing connection: " + closeEx.getMessage());
                }
            }
        }
    }

    @Override
    public <T> CompletableFuture<T> executeTransactionAsync(Transaction<T> transactionFunction) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeTransaction(transactionFunction);
            } catch (SQLException e) {
                throw new DatabaseException("Error executing async transaction", e);
            }
        }, asyncExecutor);
    }

    @Override
    public void executeQuery(String sql, Consumer<ResultRow> resultConsumer, Object... params) throws SQLException {
        long startTime = System.currentTimeMillis();
        
        // Validate query for security
        queryValidator.validateQuery(sql);
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = prepareStatement(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                ResultRow resultRow = new DefaultResultRow(rs);
                resultConsumer.accept(resultRow);
            }
            
            // Record query statistics
            long endTime = System.currentTimeMillis();
            queryStatistics.recordQuery(sql, endTime - startTime);
        } catch (SQLException e) {
            LogUtil.severe("SQL error executing query with consumer: " + e.getMessage());
            queryStatistics.recordFailedQuery(sql);
            throw e;
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            LogUtil.severe("Failed to get database connection: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean isConnectionValid() {
        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(5);
        } catch (SQLException e) {
            LogUtil.warning("Connection validity check failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.putAll(queryStatistics.getStatistics());
        return stats;
    }
    
    @Override
    public <T> List<T> querySafe(String sql, ResultMapper<T> mapper, Logger logger, Object... params) {
        try {
            // Use existing implementation
            return query(sql, mapper, params);
        } catch (SQLException e) {
            // Optimized error handling
            logger.log(Level.SEVERE, "Query failed: " + sql, e);
            // Use this.plugin.getErrorHandler() to track the error
            plugin.getErrorHandler().handleQueryError(sql, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public <T> Optional<T> queryFirstSafe(String sql, ResultMapper<T> mapper, Logger logger, Object... params) {
        try {
            return queryFirst(sql, mapper, params);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Query first failed: " + sql, e);
            plugin.getErrorHandler().handleQueryError(sql, e);
            return Optional.empty();
        }
    }
    
    @Override
    public int updateSafe(String sql, Logger logger, Object... params) {
        try {
            return update(sql, params);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Update failed: " + sql, e);
            plugin.getErrorHandler().handleQueryError(sql, e);
            return 0;
        }
    }
    
    @Override
    public int[] batchUpdateSafe(String sql, List<Object[]> parameterSets, Logger logger) {
        try {
            return batchUpdate(sql, parameterSets);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Batch update failed: " + sql, e);
            plugin.getErrorHandler().handleQueryError(sql, e);
            return new int[0];
        }
    }
    
    @Override
    public boolean tableExists(String tableName) throws SQLException {
        try (Connection conn = getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }
    
    @Override
    public boolean tableExistsSafe(String tableName, Logger logger) {
        try {
            return tableExists(tableName);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking if table exists: " + tableName, e);
            plugin.getErrorHandler().handleQueryError("CHECK TABLE " + tableName, e);
            return false;
        }
    }
    
    @Override
    public boolean createTableIfNotExists(String tableName, String createTableSQL) throws SQLException {
        if (tableExists(tableName)) {
            return true;
        }
        
        // Execute the SQL directly as this is a DDL statement
        return update(createTableSQL, new Object[0]) >= 0;
    }
    
    @Override
    public boolean createTableIfNotExistsSafe(String tableName, String createTableSQL, Logger logger) {
        try {
            return createTableIfNotExists(tableName, createTableSQL);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating table: " + tableName, e);
            plugin.getErrorHandler().handleQueryError(createTableSQL, e);
            return false;
        }
    }

    /**
     * Prepare a statement with parameters.
     *
     * @param conn The database connection
     * @param sql The SQL query
     * @param params The parameters for the query
     * @return A prepared statement
     * @throws SQLException If an error occurs during preparation
     */
    private PreparedStatement prepareStatement(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);
        
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        
        return stmt;
    }

    /**
     * Close the database and free resources.
     * This should be called when the plugin is disabled.
     */
    public void close() {
        // The connection pool will be closed by the ConnectionManager
        LogUtil.info("Database closed.");
    }

    /**
     * Default implementation of ResultRow.
     */
    private static class DefaultResultRow implements ResultRow {
        private final ResultSet resultSet;

        public DefaultResultRow(ResultSet resultSet) {
            this.resultSet = resultSet;
        }

        @Override
        public String getString(String columnName) throws SQLException {
            return resultSet.getString(columnName);
        }

        @Override
        public int getInt(String columnName) throws SQLException {
            return resultSet.getInt(columnName);
        }

        @Override
        public long getLong(String columnName) throws SQLException {
            return resultSet.getLong(columnName);
        }

        @Override
        public double getDouble(String columnName) throws SQLException {
            return resultSet.getDouble(columnName);
        }

        @Override
        public boolean getBoolean(String columnName) throws SQLException {
            return resultSet.getBoolean(columnName);
        }

        @Override
        public byte[] getBytes(String columnName) throws SQLException {
            return resultSet.getBytes(columnName);
        }

        @Override
        public java.sql.Date getDate(String columnName) throws SQLException {
            return resultSet.getDate(columnName);
        }

        @Override
        public java.sql.Timestamp getTimestamp(String columnName) throws SQLException {
            return resultSet.getTimestamp(columnName);
        }

        @Override
        public Object getObject(String columnName) throws SQLException {
            return resultSet.getObject(columnName);
        }

        @Override
        public boolean isNull(String columnName) throws SQLException {
            return resultSet.getObject(columnName) == null;
        }
    }

    /**
     * Default implementation of SelectBuilder.
     * This is a placeholder - the actual implementation would be more complex.
     */
    private class DefaultSelectBuilder implements SelectBuilder {
        private final Database database;
        
        public DefaultSelectBuilder(Database database) {
            this.database = database;
        }
        
        // Implementation would go here
        // This is just a stub for now
        
        @Override
        public SelectBuilder columns(String... columns) {
            return this;
        }
        
        @Override
        public SelectBuilder from(String table) {
            return this;
        }
        
        @Override
        public SelectBuilder where(String condition, Object... parameters) {
            return this;
        }
        
        @Override
        public SelectBuilder and(String condition, Object... parameters) {
            return this;
        }
        
        @Override
        public SelectBuilder or(String condition, Object... parameters) {
            return this;
        }
        
        @Override
        public SelectBuilder join(String type, String table, String condition) {
            return this;
        }
        
        @Override
        public SelectBuilder innerJoin(String table, String condition) {
            return this;
        }
        
        @Override
        public SelectBuilder leftJoin(String table, String condition) {
            return this;
        }
        
        @Override
        public SelectBuilder rightJoin(String table, String condition) {
            return this;
        }
        
        @Override
        public SelectBuilder groupBy(String... columns) {
            return this;
        }
        
        @Override
        public SelectBuilder having(String condition, Object... parameters) {
            return this;
        }
        
        @Override
        public SelectBuilder orderBy(String orderBy) {
            return this;
        }
        
        @Override
        public SelectBuilder limit(int limit) {
            return this;
        }
        
        @Override
        public SelectBuilder offset(int offset) {
            return this;
        }
        
        @Override
        public SelectBuilder forUpdate() {
            return this;
        }
        
        @Override
        public String getSQL() {
            return "";
        }
        
        @Override
        public Object[] getParameters() {
            return new Object[0];
        }
        
        @Override
        public <T> List<T> executeQuery(ResultMapper<T> mapper) throws SQLException {
            return new ArrayList<>();
        }
        
        @Override
        public <T> CompletableFuture<List<T>> executeQueryAsync(ResultMapper<T> mapper) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        @Override
        public <T> Optional<T> executeQueryFirst(ResultMapper<T> mapper) throws SQLException {
            return Optional.empty();
        }
        
        @Override
        public <T> CompletableFuture<Optional<T>> executeQueryFirstAsync(ResultMapper<T> mapper) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        
        @Override
        public int executeUpdate() throws SQLException {
            return 0;
        }
        
        @Override
        public CompletableFuture<Integer> executeUpdateAsync() {
            return CompletableFuture.completedFuture(0);
        }
    }

    /**
     * Default implementation of InsertBuilder.
     * This is a placeholder - the actual implementation would be more complex.
     */
    private class DefaultInsertBuilder implements InsertBuilder {
        private final Database database;
        private final String table;
        
        public DefaultInsertBuilder(Database database, String table) {
            this.database = database;
            this.table = table;
        }
        
        // Implementation would go here
        // This is just a stub for now
        
        @Override
        public InsertBuilder columns(String... columns) {
            return this;
        }
        
        @Override
        public InsertBuilder values(Object... values) {
            return this;
        }
        
        @Override
        public InsertBuilder addRow(Object... values) {
            return this;
        }
        
        @Override
        public InsertBuilder columnValues(Map<String, Object> columnValues) {
            return this;
        }
        
        @Override
        public InsertBuilder onDuplicateKeyUpdate(String column, Object value) {
            return this;
        }
        
        @Override
        public InsertBuilder onDuplicateKeyUpdate(Map<String, Object> columnValues) {
            return this;
        }
        
        @Override
        public InsertBuilder select(SelectBuilder selectBuilder) {
            return this;
        }
        
        @Override
        public InsertBuilder returning(String... columns) {
            return this;
        }
        
        @Override
        public String getSQL() {
            return "";
        }
        
        @Override
        public Object[] getParameters() {
            return new Object[0];
        }
        
        @Override
        public <T> List<T> executeQuery(ResultMapper<T> mapper) throws SQLException {
            return new ArrayList<>();
        }
        
        @Override
        public <T> CompletableFuture<List<T>> executeQueryAsync(ResultMapper<T> mapper) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        @Override
        public <T> Optional<T> executeQueryFirst(ResultMapper<T> mapper) throws SQLException {
            return Optional.empty();
        }
        
        @Override
        public <T> CompletableFuture<Optional<T>> executeQueryFirstAsync(ResultMapper<T> mapper) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        
        @Override
        public int executeUpdate() throws SQLException {
            return 0;
        }
        
        @Override
        public CompletableFuture<Integer> executeUpdateAsync() {
            return CompletableFuture.completedFuture(0);
        }
    }

    /**
     * Default implementation of UpdateBuilder.
     * This is a placeholder - the actual implementation would be more complex.
     */
    private class DefaultUpdateBuilder implements UpdateBuilder {
        private final Database database;
        private final String table;
        
        public DefaultUpdateBuilder(Database database, String table) {
            this.database = database;
            this.table = table;
        }
        
        // Implementation would go here
        // This is just a stub for now
        
        @Override
        public UpdateBuilder set(String column, Object value) {
            return this;
        }
        
        @Override
        public UpdateBuilder set(Map<String, Object> columnValues) {
            return this;
        }
        
        @Override
        public UpdateBuilder where(String condition, Object... parameters) {
            return this;
        }
        
        @Override
        public UpdateBuilder and(String condition, Object... parameters) {
            return this;
        }
        
        @Override
        public UpdateBuilder or(String condition, Object... parameters) {
            return this;
        }
        
        @Override
        public UpdateBuilder limit(int limit) {
            return this;
        }
        
        @Override
        public UpdateBuilder orderBy(String orderBy) {
            return this;
        }
        
        @Override
        public UpdateBuilder returning(String... columns) {
            return this;
        }
        
        @Override
        public String getSQL() {
            return "";
        }
        
        @Override
        public Object[] getParameters() {
            return new Object[0];
        }
        
        @Override
        public <T> List<T> executeQuery(ResultMapper<T> mapper) throws SQLException {
            return new ArrayList<>();
        }
        
        @Override
        public <T> CompletableFuture<List<T>> executeQueryAsync(ResultMapper<T> mapper) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        @Override
        public <T> Optional<T> executeQueryFirst(ResultMapper<T> mapper) throws SQLException {
            return Optional.empty();
        }
        
        @Override
        public <T> CompletableFuture<Optional<T>> executeQueryFirstAsync(ResultMapper<T> mapper) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        
        @Override
        public int executeUpdate() throws SQLException {
            return 0;
        }
        
        @Override
        public CompletableFuture<Integer> executeUpdateAsync() {
            return CompletableFuture.completedFuture(0);
        }
    }

    /**
     * Default implementation of QueryBuilder.
     * This is a placeholder - the actual implementation would be more complex.
     */
    private class DefaultQueryBuilder implements QueryBuilder {
        private final Database database;
        
        public DefaultQueryBuilder(Database database) {
            this.database = database;
        }
        
        // Implementation would go here
        // This is just a stub for now
        
        @Override
        public String getSQL() {
            return "";
        }
        
        @Override
        public Object[] getParameters() {
            return new Object[0];
        }
        
        @Override
        public <T> List<T> executeQuery(ResultMapper<T> mapper) throws SQLException {
            return new ArrayList<>();
        }
        
        @Override
        public <T> CompletableFuture<List<T>> executeQueryAsync(ResultMapper<T> mapper) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        @Override
        public <T> Optional<T> executeQueryFirst(ResultMapper<T> mapper) throws SQLException {
            return Optional.empty();
        }
        
        @Override
        public <T> CompletableFuture<Optional<T>> executeQueryFirstAsync(ResultMapper<T> mapper) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        
        @Override
        public int executeUpdate() throws SQLException {
            return 0;
        }
        
        @Override
        public CompletableFuture<Integer> executeUpdateAsync() {
            return CompletableFuture.completedFuture(0);
        }
    }
}