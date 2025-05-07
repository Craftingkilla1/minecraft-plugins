package com.minecraft.sqlbridge.api;

import java.util.List;

/**
 * Interface for building SQL queries in a type-safe and database-agnostic way.
 * Provides fluent methods for creating SELECT, INSERT, UPDATE, and DELETE queries.
 */
public interface QueryBuilder {

    /**
     * Start building a SELECT query
     *
     * @param columns The columns to select
     * @return The query builder for chaining
     */
    QueryBuilder select(String... columns);

    /**
     * Start building a SELECT DISTINCT query
     *
     * @param columns The columns to select
     * @return The query builder for chaining
     */
    QueryBuilder selectDistinct(String... columns);

    /**
     * Set the table to select from
     *
     * @param table The table name
     * @return The query builder for chaining
     */
    QueryBuilder from(String table);

    /**
     * Add a JOIN clause
     *
     * @param table The table to join
     * @param condition The join condition
     * @return The query builder for chaining
     */
    QueryBuilder join(String table, String condition);

    /**
     * Add a LEFT JOIN clause
     *
     * @param table The table to join
     * @param condition The join condition
     * @return The query builder for chaining
     */
    QueryBuilder leftJoin(String table, String condition);

    /**
     * Add a RIGHT JOIN clause
     *
     * @param table The table to join
     * @param condition The join condition
     * @return The query builder for chaining
     */
    QueryBuilder rightJoin(String table, String condition);

    /**
     * Add a WHERE clause
     *
     * @param condition The condition
     * @return The query builder for chaining
     */
    QueryBuilder where(String condition);

    /**
     * Add a WHERE clause with a parameter
     *
     * @param column The column name
     * @param operator The operator (=, <, >, etc.)
     * @param value The parameter value
     * @return The query builder for chaining
     */
    QueryBuilder where(String column, String operator, Object value);

    /**
     * Add an AND condition to the WHERE clause
     *
     * @param condition The condition
     * @return The query builder for chaining
     */
    QueryBuilder and(String condition);

    /**
     * Add an AND condition to the WHERE clause with a parameter
     *
     * @param column The column name
     * @param operator The operator (=, <, >, etc.)
     * @param value The parameter value
     * @return The query builder for chaining
     */
    QueryBuilder and(String column, String operator, Object value);

    /**
     * Add an OR condition to the WHERE clause
     *
     * @param condition The condition
     * @return The query builder for chaining
     */
    QueryBuilder or(String condition);

    /**
     * Add an OR condition to the WHERE clause with a parameter
     *
     * @param column The column name
     * @param operator The operator (=, <, >, etc.)
     * @param value The parameter value
     * @return The query builder for chaining
     */
    QueryBuilder or(String column, String operator, Object value);

    /**
     * Add a GROUP BY clause
     *
     * @param columns The columns to group by
     * @return The query builder for chaining
     */
    QueryBuilder groupBy(String... columns);

    /**
     * Add a HAVING clause
     *
     * @param condition The condition
     * @return The query builder for chaining
     */
    QueryBuilder having(String condition);

    /**
     * Add an ORDER BY clause
     *
     * @param column The column to order by
     * @param direction The sort direction (ASC or DESC)
     * @return The query builder for chaining
     */
    QueryBuilder orderBy(String column, String direction);

    /**
     * Add a LIMIT clause
     *
     * @param limit The maximum number of rows to return
     * @return The query builder for chaining
     */
    QueryBuilder limit(int limit);

    /**
     * Add an OFFSET clause
     *
     * @param offset The number of rows to skip
     * @return The query builder for chaining
     */
    QueryBuilder offset(int offset);

    /**
     * Start building an INSERT query
     *
     * @param table The table to insert into
     * @return The query builder for chaining
     */
    QueryBuilder insertInto(String table);

    /**
     * Set the columns for the INSERT query
     *
     * @param columns The columns to insert into
     * @return The query builder for chaining
     */
    QueryBuilder columns(String... columns);

    /**
     * Set the values for the INSERT query
     *
     * @param values The values to insert
     * @return The query builder for chaining
     */
    QueryBuilder values(Object... values);

    /**
     * Start building an UPDATE query
     *
     * @param table The table to update
     * @return The query builder for chaining
     */
    QueryBuilder update(String table);

    /**
     * Set a column=value pair for the UPDATE query
     *
     * @param column The column to update
     * @param value The new value
     * @return The query builder for chaining
     */
    QueryBuilder set(String column, Object value);

    /**
     * Start building a DELETE query
     *
     * @param table The table to delete from
     * @return The query builder for chaining
     */
    QueryBuilder deleteFrom(String table);

    /**
     * Add a batch of values for a batch INSERT
     *
     * @param values The batch of values
     * @return The query builder for chaining
     */
    QueryBuilder addBatch(List<Object[]> values);

    /**
     * Get the parameters for the query
     *
     * @return Array of parameters
     */
    Object[] getParameters();

    /**
     * Build the SQL query string
     *
     * @return The SQL query string
     */
    String build();

    /**
     * Build the Query object with the current state
     *
     * @return A Query object that can be executed
     */
    Query buildQuery();
}