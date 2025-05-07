package com.minecraft.sqlbridge.test;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.DatabaseType;
import com.minecraft.sqlbridge.connection.ConnectionFactory;
import com.minecraft.sqlbridge.connection.ConnectionManager;
import com.minecraft.sqlbridge.impl.DefaultDatabase;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Base class for integration tests of the database functionality.
 * Provides methods for setting up test databases and running test cases.
 */
public class DatabaseTest {

    private final SqlBridgePlugin plugin;
    private Database testDatabase;
    private ConnectionManager connectionManager;
    private final List<TestResult> results = new ArrayList<>();
    private boolean setupComplete = false;
    private final String testDatabaseName;

    /**
     * Create a new database test
     *
     * @param plugin The SQL-Bridge plugin instance
     */
    public DatabaseTest(SqlBridgePlugin plugin) {
        this.plugin = plugin;
        this.testDatabaseName = "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * Set up the test environment with an in-memory database
     *
     * @return True if setup was successful, false otherwise
     */
    public boolean setupTest() {
        try {
            // Create a test-specific configuration
            YamlConfiguration testConfig = new YamlConfiguration();
            testConfig.set("database.type", "H2");
            testConfig.set("database.name", testDatabaseName);
            
            // Create a test connection manager
            connectionManager = new ConnectionManager(plugin, DatabaseType.H2, "localhost", 0, 
                    testDatabaseName, "", "");
            
            // Initialize connection
            if (!connectionManager.initialize()) {
                LogUtil.severe("Failed to initialize test database connection");
                return false;
            }
            
            // Create the test database instance
            testDatabase = new DefaultDatabase(plugin, connectionManager);
            
            // Create test tables
            createTestTables();
            
            setupComplete = true;
            return true;
        } catch (Exception e) {
            LogUtil.severe("Error setting up test database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create tables for testing
     */
    private void createTestTables() throws SQLException {
        try (Connection connection = connectionManager.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Create a simple test table
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS test_users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(32) NOT NULL, " +
                    "email VARCHAR(255), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")"
            );
            
            // Create a second test table with relationships
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS test_posts (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "title VARCHAR(100) NOT NULL, " +
                    "content TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES test_users(id)" +
                    ")"
            );
        }
    }

    /**
     * Run a test case and record the result
     *
     * @param testName The name of the test
     * @param testCase The test case to run
     */
    public void runTest(String testName, Consumer<Database> testCase) {
        if (!setupComplete) {
            LogUtil.warning("Cannot run test: Test environment not set up");
            results.add(new TestResult(testName, false, "Test environment not set up"));
            return;
        }
        
        try {
            // Run the test case
            testCase.accept(testDatabase);
            
            // Record success
            results.add(new TestResult(testName, true, null));
        } catch (Exception e) {
            // Record failure
            LogUtil.warning("Test failed (" + testName + "): " + e.getMessage());
            results.add(new TestResult(testName, false, e.getMessage()));
        }
    }

    /**
     * Get the test results
     *
     * @return The test results
     */
    public List<TestResult> getResults() {
        return new ArrayList<>(results);
    }

    /**
     * Get a summary of the test results
     *
     * @return A map of result statistics
     */
    public Map<String, Object> getResultSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        int total = results.size();
        int passed = 0;
        int failed = 0;
        
        for (TestResult result : results) {
            if (result.isSuccess()) {
                passed++;
            } else {
                failed++;
            }
        }
        
        summary.put("totalTests", total);
        summary.put("passed", passed);
        summary.put("failed", failed);
        summary.put("passRate", total > 0 ? (double) passed / total * 100 : 0);
        
        return summary;
    }

    /**
     * Clean up the test environment
     */
    public void cleanup() {
        if (connectionManager != null) {
            try (Connection connection = connectionManager.getConnection();
                 Statement statement = connection.createStatement()) {
                
                // Drop test tables
                statement.executeUpdate("DROP TABLE IF EXISTS test_posts");
                statement.executeUpdate("DROP TABLE IF EXISTS test_users");
                
            } catch (SQLException e) {
                LogUtil.warning("Error cleaning up test database: " + e.getMessage());
            }
            
            // Close connections
            connectionManager.closeAllConnections();
        }
    }

    /**
     * Inner class representing a test result
     */
    public static class TestResult {
        private final String testName;
        private final boolean success;
        private final String errorMessage;

        public TestResult(String testName, boolean success, String errorMessage) {
            this.testName = testName;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public String getTestName() {
            return testName;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}