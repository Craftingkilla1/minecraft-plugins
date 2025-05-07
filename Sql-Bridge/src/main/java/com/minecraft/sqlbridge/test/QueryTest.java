package com.minecraft.sqlbridge.test;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.SqlBridgePlugin;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.QueryBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the database query functionality.
 * Tests basic CRUD operations, batch operations, and query builder.
 */
public class QueryTest extends DatabaseTest {

    private final SqlBridgePlugin plugin;
    
    /**
     * Create a new query test
     *
     * @param plugin The SQL-Bridge plugin instance
     */
    public QueryTest(SqlBridgePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }
    
    /**
     * Run all query tests
     */
    public void runAllTests() {
        // Set up the test environment
        if (!setupTest()) {
            LogUtil.severe("Failed to set up test environment for QueryTest");
            return;
        }
        
        try {
            // Insert test data
            insertTestData();
            
            // Run individual tests
            testBasicQuery();
            testQueryFirst();
            testUpdate();
            testInsert();
            testTransaction();
            testBatchOperations();
            testQueryBuilder();
            
            // Log test results
            logResults();
        } finally {
            // Clean up test environment
            cleanup();
        }
    }
    
    /**
     * Insert test data for the tests
     */
    private void insertTestData() {
        runTest("Insert Test Data", database -> {
            // Insert test users
            database.update(
                    "INSERT INTO test_users (username, email) VALUES (?, ?)",
                    "user1", "user1@example.com");
            
            database.update(
                    "INSERT INTO test_users (username, email) VALUES (?, ?)",
                    "user2", "user2@example.com");
            
            database.update(
                    "INSERT INTO test_users (username, email) VALUES (?, ?)",
                    "user3", "user3@example.com");
            
            // Get user IDs
            List<Map<String, Object>> users = database.query(
                    "SELECT id FROM test_users ORDER BY id",
                    row -> row);
            
            int user1Id = ((Number) users.get(0).get("id")).intValue();
            int user2Id = ((Number) users.get(1).get("id")).intValue();
            
            // Insert test posts
            database.update(
                    "INSERT INTO test_posts (user_id, title, content) VALUES (?, ?, ?)",
                    user1Id, "First Post", "This is the first post");
            
            database.update(
                    "INSERT INTO test_posts (user_id, title, content) VALUES (?, ?, ?)",
                    user1Id, "Second Post", "This is the second post");
            
            database.update(
                    "INSERT INTO test_posts (user_id, title, content) VALUES (?, ?, ?)",
                    user2Id, "Hello World", "Hello, world!");
        });
    }
    
    /**
     * Test basic query execution
     */
    private void testBasicQuery() {
        runTest("Basic Query", database -> {
            // Query all users
            List<Map<String, Object>> users = database.query(
                    "SELECT * FROM test_users ORDER BY id",
                    row -> row);
            
            // Check that we got the expected number of users
            if (users.size() != 3) {
                throw new RuntimeException("Expected 3 users, got " + users.size());
            }
            
            // Check that the first user has the expected username
            String username = (String) users.get(0).get("username");
            if (!"user1".equals(username)) {
                throw new RuntimeException("Expected username 'user1', got '" + username + "'");
            }
            
            // Query posts with join
            List<Map<String, Object>> posts = database.query(
                    "SELECT p.*, u.username FROM test_posts p " +
                    "JOIN test_users u ON p.user_id = u.id " +
                    "ORDER BY p.id",
                    row -> row);
            
            // Check that we got the expected number of posts
            if (posts.size() != 3) {
                throw new RuntimeException("Expected 3 posts, got " + posts.size());
            }
            
            // Check that the first post has the expected title and username
            String title = (String) posts.get(0).get("title");
            String postUsername = (String) posts.get(0).get("username");
            
            if (!"First Post".equals(title)) {
                throw new RuntimeException("Expected title 'First Post', got '" + title + "'");
            }
            
            if (!"user1".equals(postUsername)) {
                throw new RuntimeException("Expected username 'user1', got '" + postUsername + "'");
            }
        });
    }
    
    /**
     * Test queryFirst method
     */
    private void testQueryFirst() {
        runTest("Query First", database -> {
            // Query a single user
            Map<String, Object> user = database.queryFirst(
                    "SELECT * FROM test_users WHERE username = ?",
                    row -> row,
                    "user2").orElse(null);
            
            // Check that we got a user
            if (user == null) {
                throw new RuntimeException("Expected to find user 'user2', but got null");
            }
            
            // Check that the user has the expected email
            String email = (String) user.get("email");
            if (!"user2@example.com".equals(email)) {
                throw new RuntimeException("Expected email 'user2@example.com', got '" + email + "'");
            }
            
            // Query a non-existent user
            Map<String, Object> nonExistentUser = database.queryFirst(
                    "SELECT * FROM test_users WHERE username = ?",
                    row -> row,
                    "nonexistent").orElse(null);
            
            // Check that we didn't get a user
            if (nonExistentUser != null) {
                throw new RuntimeException("Expected null for non-existent user, got a result");
            }
        });
    }
    
    /**
     * Test update method
     */
    private void testUpdate() {
        runTest("Update", database -> {
            // Update a user's email
            int updatedRows = database.update(
                    "UPDATE test_users SET email = ? WHERE username = ?",
                    "newemail@example.com", "user3");
            
            // Check that we updated one row
            if (updatedRows != 1) {
                throw new RuntimeException("Expected to update 1 row, updated " + updatedRows);
            }
            
            // Query the user to check the update
            Map<String, Object> user = database.queryFirst(
                    "SELECT * FROM test_users WHERE username = ?",
                    row -> row,
                    "user3").orElse(null);
            
            // Check that the user's email was updated
            String email = (String) user.get("email");
            if (!"newemail@example.com".equals(email)) {
                throw new RuntimeException("Expected email 'newemail@example.com', got '" + email + "'");
            }
        });
    }
    
    /**
     * Test insert method
     */
    private void testInsert() {
        runTest("Insert", database -> {
            // Insert a new user
            long userId = 0;
            try {
                userId = database.insert(
                        "INSERT INTO test_users (username, email) VALUES (?, ?)",
                        "user4", "user4@example.com");
            } catch (SQLException e) {
                throw new RuntimeException("Error inserting user: " + e.getMessage(), e);
            }
            
            // Check that we got a valid ID
            if (userId <= 0) {
                throw new RuntimeException("Expected a positive user ID, got " + userId);
            }
            
            // Query the user to check the insert
            Map<String, Object> user = database.queryFirst(
                    "SELECT * FROM test_users WHERE id = ?",
                    row -> row,
                    userId).orElse(null);
            
            // Check that the user was inserted correctly
            if (user == null) {
                throw new RuntimeException("Expected to find user with ID " + userId + ", but got null");
            }
            
            String username = (String) user.get("username");
            String email = (String) user.get("email");
            
            if (!"user4".equals(username)) {
                throw new RuntimeException("Expected username 'user4', got '" + username + "'");
            }
            
            if (!"user4@example.com".equals(email)) {
                throw new RuntimeException("Expected email 'user4@example.com', got '" + email + "'");
            }
        });
    }
    
    /**
     * Test transaction method
     */
    private void testTransaction() {
        runTest("Transaction", database -> {
            // Execute a transaction that inserts a user and a post
            database.transaction(connection -> {
                // Insert a user
                try (PreparedStatement userStatement = connection.prepareStatement(
                        "INSERT INTO test_users (username, email) VALUES (?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    
                    userStatement.setString(1, "user5");
                    userStatement.setString(2, "user5@example.com");
                    userStatement.executeUpdate();
                    
                    // Get the user ID
                    ResultSet rs = userStatement.getGeneratedKeys();
                    if (!rs.next()) {
                        throw new SQLException("Failed to get generated user ID");
                    }
                    
                    int userId = rs.getInt(1);
                    
                    // Insert a post for the user
                    try (PreparedStatement postStatement = connection.prepareStatement(
                            "INSERT INTO test_posts (user_id, title, content) VALUES (?, ?, ?)")) {
                        
                        postStatement.setInt(1, userId);
                        postStatement.setString(2, "Transaction Test");
                        postStatement.setString(3, "This post was created in a transaction");
                        postStatement.executeUpdate();
                    }
                }
                
                return true;
            });
            
            // Query to check that both the user and post were inserted
            Map<String, Object> user = database.queryFirst(
                    "SELECT * FROM test_users WHERE username = ?",
                    row -> row,
                    "user5").orElse(null);
            
            if (user == null) {
                throw new RuntimeException("Transaction failed: User was not inserted");
            }
            
            int userId = ((Number) user.get("id")).intValue();
            
            Map<String, Object> post = database.queryFirst(
                    "SELECT * FROM test_posts WHERE user_id = ? AND title = ?",
                    row -> row,
                    userId, "Transaction Test").orElse(null);
            
            if (post == null) {
                throw new RuntimeException("Transaction failed: Post was not inserted");
            }
        });
    }
    
    /**
     * Test batch operations
     */
    private void testBatchOperations() {
        runTest("Batch Operations", database -> {
            // Create batch parameters for inserting multiple users
            List<Object[]> batchParams = new ArrayList<>();
            batchParams.add(new Object[] { "batch1", "batch1@example.com" });
            batchParams.add(new Object[] { "batch2", "batch2@example.com" });
            batchParams.add(new Object[] { "batch3", "batch3@example.com" });
            
            // Execute batch insert
            int[] results = database.batchUpdate(
                    "INSERT INTO test_users (username, email) VALUES (?, ?)",
                    batchParams);
            
            // Check that all inserts were successful
            if (results.length != 3) {
                throw new RuntimeException("Expected 3 batch results, got " + results.length);
            }
            
            for (int i = 0; i < results.length; i++) {
                if (results[i] != 1) {
                    throw new RuntimeException("Batch insert " + i + " failed: Expected 1 row, got " + results[i]);
                }
            }
            
            // Query to check that all users were inserted
            List<Map<String, Object>> users = database.query(
                    "SELECT * FROM test_users WHERE username LIKE 'batch%' ORDER BY username",
                    row -> row);
            
            if (users.size() != 3) {
                throw new RuntimeException("Expected 3 batch users, got " + users.size());
            }
            
            for (int i = 0; i < users.size(); i++) {
                String username = (String) users.get(i).get("username");
                String expectedUsername = "batch" + (i + 1);
                
                if (!expectedUsername.equals(username)) {
                    throw new RuntimeException("Expected username '" + expectedUsername + "', got '" + username + "'");
                }
            }
        });
    }
    
    /**
     * Test query builder
     */
    private void testQueryBuilder() {
        runTest("Query Builder", database -> {
            // Create a query builder
            QueryBuilder builder = database.createQueryBuilder();
            
            // Build a SELECT query
            String sql = builder
                    .select("p.*", "u.username")
                    .from("test_posts p")
                    .join("test_users u", "p.user_id = u.id")
                    .where("u.username", "=", "user1")
                    .orderBy("p.id", "ASC")
                    .build();
            
            // Execute the query
            List<Map<String, Object>> posts = database.query(
                    sql,
                    row -> row,
                    builder.getParameters());
            
            // Check that we got the expected posts
            if (posts.size() != 2) {
                throw new RuntimeException("Expected 2 posts for user1, got " + posts.size());
            }
            
            // Build an INSERT query
            builder = database.createQueryBuilder();
            sql = builder
                    .insertInto("test_users")
                    .columns("username", "email")
                    .values("builder_user", "builder@example.com")
                    .build();
            
            // Execute the insert
            int result = database.update(sql, builder.getParameters());
            
            // Check that the insert was successful
            if (result != 1) {
                throw new RuntimeException("Insert with query builder failed: Expected 1 row, got " + result);
            }
            
            // Build an UPDATE query
            builder = database.createQueryBuilder();
            sql = builder
                    .update("test_users")
                    .set("email", "updated@example.com")
                    .where("username", "=", "builder_user")
                    .build();
            
            // Execute the update
            result = database.update(sql, builder.getParameters());
            
            // Check that the update was successful
            if (result != 1) {
                throw new RuntimeException("Update with query builder failed: Expected 1 row, got " + result);
            }
            
            // Build a DELETE query
            builder = database.createQueryBuilder();
            sql = builder
                    .deleteFrom("test_users")
                    .where("username", "=", "builder_user")
                    .build();
            
            // Execute the delete
            result = database.update(sql, builder.getParameters());
            
            // Check that the delete was successful
            if (result != 1) {
                throw new RuntimeException("Delete with query builder failed: Expected 1 row, got " + result);
            }
        });
    }
    
    /**
     * Log test results
     */
    private void logResults() {
        LogUtil.info("==== Query Test Results ====");
        
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