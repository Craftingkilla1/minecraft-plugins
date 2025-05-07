package com.minecraft.sqlbridge.test;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.connection.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Tests for the database connection functionality.
 * Tests connection pooling, concurrent connections, and connection timeout.
 */
public class ConnectionTest extends DatabaseTest {

    private final SqlBridgePlugin plugin;
    
    /**
     * Create a new connection test
     *
     * @param plugin The SQL-Bridge plugin instance
     */
    public ConnectionTest(SqlBridgePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }
    
    /**
     * Run all connection tests
     */
    public void runAllTests() {
        // Set up the test environment
        if (!setupTest()) {
            LogUtil.severe("Failed to set up test environment for ConnectionTest");
            return;
        }
        
        try {
            // Run individual tests
            testBasicConnection();
            testConnectionPooling();
            testConcurrentConnections();
            testConnectionTimeout();
            testConnectionReleasing();
            testTransactionRollback();
            
            // Log test results
            logResults();
        } finally {
            // Clean up test environment
            cleanup();
        }
    }
    
    /**
     * Test basic database connection
     */
    private void testBasicConnection() {
        runTest("Basic Connection", database -> {
            try {
                // Test that we can connect to the database
                Connection connection = database.getConnection();
                if (connection == null || !connection.isValid(1)) {
                    throw new RuntimeException("Failed to establish basic database connection");
                }
                
                // Test executing a simple query
                PreparedStatement statement = connection.prepareStatement("SELECT 1");
                ResultSet resultSet = statement.executeQuery();
                
                if (!resultSet.next() || resultSet.getInt(1) != 1) {
                    throw new RuntimeException("Failed to execute simple query");
                }
                
                // Close resources
                resultSet.close();
                statement.close();
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException("SQL error in basic connection test: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Test connection pooling
     */
    private void testConnectionPooling() {
        runTest("Connection Pooling", database -> {
            try {
                // Get the first connection
                Connection conn1 = database.getConnection();
                
                // Create a unique identifier for this connection
                String id1 = String.valueOf(System.identityHashCode(conn1));
                
                // Close the connection (returns to pool)
                conn1.close();
                
                // Get another connection (should be the same physical connection)
                Connection conn2 = database.getConnection();
                String id2 = String.valueOf(System.identityHashCode(conn2));
                
                // The connections should have the same identity hash code if pooling works
                if (!id1.equals(id2)) {
                    throw new RuntimeException("Connection pooling test failed: Got different connection from pool");
                }
                
                // Close the second connection
                conn2.close();
            } catch (SQLException e) {
                throw new RuntimeException("SQL error in connection pooling test: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Test concurrent connections
     */
    private void testConcurrentConnections() {
        runTest("Concurrent Connections", database -> {
            int numThreads = 10;
            int queriesPerThread = 5;
            
            // Create a thread pool
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            
            // Create a list of tasks
            List<Callable<Boolean>> tasks = new ArrayList<>();
            
            // Create tasks for each thread
            for (int i = 0; i < numThreads; i++) {
                tasks.add(() -> {
                    try {
                        for (int j = 0; j < queriesPerThread; j++) {
                            // Get a connection
                            Connection connection = database.getConnection();
                            
                            // Execute a query
                            PreparedStatement statement = connection.prepareStatement("SELECT 1");
                            ResultSet resultSet = statement.executeQuery();
                            resultSet.next();
                            
                            // Close resources
                            resultSet.close();
                            statement.close();
                            connection.close();
                            
                            // Add some random sleep to simulate work
                            Thread.sleep((long) (Math.random() * 50));
                        }
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                });
            }
            
            // Execute all tasks
            List<Future<Boolean>> results;
            try {
                results = executor.invokeAll(tasks);
            } catch (InterruptedException e) {
                throw new RuntimeException("Concurrent connections test interrupted", e);
            }
            
            // Shutdown the executor
            executor.shutdown();
            
            // Check results
            for (Future<Boolean> result : results) {
                try {
                    if (!result.get()) {
                        throw new RuntimeException("Concurrent connections test failed");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error checking concurrent connection results", e);
                }
            }
        });
    }
    
    /**
     * Test connection timeout
     */
    private void testConnectionTimeout() {
        runTest("Connection Timeout", database -> {
            try {
                // Get a connection
                Connection connection = database.getConnection();
                
                // Test that isValid times out correctly
                long start = System.currentTimeMillis();
                boolean valid = connection.isValid(1); // 1 second timeout
                long end = System.currentTimeMillis();
                
                // The isValid method should return quickly
                if (!valid || (end - start) > 2000) {
                    throw new RuntimeException("Connection timeout test failed: isValid took too long or failed");
                }
                
                // Close the connection
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException("SQL error in connection timeout test: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Test that connections are properly released back to the pool
     */
    private void testConnectionReleasing() {
        runTest("Connection Releasing", database -> {
            List<Connection> connections = new ArrayList<>();
            
            try {
                // Get several connections
                for (int i = 0; i < 5; i++) {
                    connections.add(database.getConnection());
                }
                
                // Close all connections
                for (Connection connection : connections) {
                    connection.close();
                }
                
                // Get a connection again - should succeed
                Connection connection = database.getConnection();
                
                // Test that the connection is valid
                if (!connection.isValid(1)) {
                    throw new RuntimeException("Connection releasing test failed: Got invalid connection");
                }
                
                // Close the connection
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException("SQL error in connection releasing test: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Test transaction rollback
     */
    private void testTransactionRollback() {
        runTest("Transaction Rollback", database -> {
            try {
                // Get a connection
                Connection connection = database.getConnection();
                
                // Start a transaction
                connection.setAutoCommit(false);
                
                try {
                    // Create a test user
                    PreparedStatement insertStatement = connection.prepareStatement(
                            "INSERT INTO test_users (username, email) VALUES (?, ?)");
                    insertStatement.setString(1, "testuser");
                    insertStatement.setString(2, "test@example.com");
                    insertStatement.executeUpdate();
                    
                    // Rollback the transaction
                    connection.rollback();
                    
                    // Check that the user was not inserted
                    PreparedStatement selectStatement = connection.prepareStatement(
                            "SELECT COUNT(*) FROM test_users WHERE username = ?");
                    selectStatement.setString(1, "testuser");
                    ResultSet resultSet = selectStatement.executeQuery();
                    resultSet.next();
                    int count = resultSet.getInt(1);
                    
                    if (count > 0) {
                        throw new RuntimeException("Transaction rollback test failed: Data was not rolled back");
                    }
                    
                    // Close resources
                    resultSet.close();
                    selectStatement.close();
                    insertStatement.close();
                } finally {
                    // Restore auto-commit and close connection
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                throw new RuntimeException("SQL error in transaction rollback test: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Log test results
     */
    private void logResults() {
        LogUtil.info("==== Connection Test Results ====");
        
        for (TestResult result : getResults()) {
            String status = result.isSuccess() ? "PASSED" : "FAILED";
            LogUtil.info(status + ": " + result.getTestName());
            
            if (!result.isSuccess() && result.getErrorMessage() != null) {
                LogUtil.warning("  Error: " + result.getErrorMessage());
            }
        }
        
        // Log summary
        Map<String, Object> summary = getResultSummary();
        LogUtil.info("=== Summary ===");
        LogUtil.info("Total tests: " + summary.get("totalTests"));
        LogUtil.info("Passed: " + summary.get("passed"));
        LogUtil.info("Failed: " + summary.get("failed"));
        LogUtil.info("Pass rate: " + String.format("%.2f%%", summary.get("passRate")));
    }
}