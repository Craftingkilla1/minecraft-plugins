package com.minecraft.sqlbridge.connection;

import com.minecraft.core.utils.LogUtil;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Factory for creating database connections based on configuration.
 */
public class ConnectionFactory {

    private final String url;
    private final String driverClassName;
    private final String username;
    private final String password;
    private final Properties properties;

    /**
     * Create a new connection factory with the specified configuration
     *
     * @param url The JDBC URL
     * @param driverClassName The JDBC driver class name
     * @param username The database username
     * @param password The database password
     * @param properties Additional JDBC properties
     */
    private ConnectionFactory(String url, String driverClassName, String username, String password, Properties properties) {
        this.url = url;
        this.driverClassName = driverClassName;
        this.username = username;
        this.password = password;
        this.properties = properties;
        
        // Initialize the driver
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            LogUtil.severe("Database driver not found: " + driverClassName);
            throw new RuntimeException("Database driver not found: " + driverClassName, e);
        }
    }

    /**
     * Create a new database connection
     *
     * @return A database connection
     * @throws SQLException If a connection cannot be created
     */
    public Connection createConnection() throws SQLException {
        try {
            // Create connection with properties
            Properties connectionProps = new Properties();
            if (properties != null) {
                connectionProps.putAll(properties);
            }
            
            // Add username and password if provided
            if (username != null && !username.isEmpty()) {
                connectionProps.setProperty("user", username);
            }
            if (password != null) {
                connectionProps.setProperty("password", password);
            }
            
            // Get connection from driver manager
            return DriverManager.getConnection(url, connectionProps);
        } catch (SQLException e) {
            LogUtil.severe("Failed to create database connection: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Builder for creating connection factories with different configurations
     */
    public static class Builder {
        private String url;
        private String driverClassName;
        private String username;
        private String password;
        private Properties properties = new Properties();

        /**
         * Set the JDBC URL
         *
         * @param url The JDBC URL
         * @return The builder
         */
        public Builder withUrl(String url) {
            this.url = url;
            return this;
        }

        /**
         * Set the JDBC driver class name
         *
         * @param driverClassName The JDBC driver class name
         * @return The builder
         */
        public Builder withDriver(String driverClassName) {
            this.driverClassName = driverClassName;
            return this;
        }

        /**
         * Set the database username
         *
         * @param username The database username
         * @return The builder
         */
        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        /**
         * Set the database password
         *
         * @param password The database password
         * @return The builder
         */
        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        /**
         * Set additional JDBC properties
         *
         * @param properties The JDBC properties string (key=value&key2=value2)
         * @return The builder
         */
        public Builder withProperties(String properties) {
            if (properties != null && !properties.isEmpty()) {
                String[] props = properties.split("&");
                for (String prop : props) {
                    String[] keyValue = prop.split("=");
                    if (keyValue.length == 2) {
                        this.properties.setProperty(keyValue[0], keyValue[1]);
                    }
                }
            }
            return this;
        }

        /**
         * Add a single JDBC property
         *
         * @param key The property key
         * @param value The property value
         * @return The builder
         */
        public Builder withProperty(String key, String value) {
            this.properties.setProperty(key, value);
            return this;
        }

        /**
         * Build the connection factory
         *
         * @return A new connection factory
         */
        public ConnectionFactory build() {
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("Database URL is required");
            }
            if (driverClassName == null || driverClassName.isEmpty()) {
                throw new IllegalArgumentException("Database driver class name is required");
            }
            
            return new ConnectionFactory(url, driverClassName, username, password, properties);
        }
    }
}