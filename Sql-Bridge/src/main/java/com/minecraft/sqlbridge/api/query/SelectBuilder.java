// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/api/query/SelectBuilder.java
package com.minecraft.sqlbridge.api.query;

/**
 * Interface for building SELECT queries.
 */
public interface SelectBuilder extends QueryBuilder {
    
    /**
     * Specify the columns to select.
     *
     * @param columns The columns to select
     * @return This SelectBuilder for chaining
     */
    SelectBuilder columns(String... columns);
    
    /**
     * Specify the table to select from.
     *
     * @param table The table name
     * @return This SelectBuilder for chaining
     */
    SelectBuilder from(String table);
    
    /**
     * Add a WHERE clause to the query.
     *
     * @param condition The WHERE condition
     * @param parameters The parameters for the condition
     * @return This SelectBuilder for chaining
     */
    SelectBuilder where(String condition, Object... parameters);
    
    /**
     * Add an AND condition to the WHERE clause.
     *
     * @param condition The AND condition
     * @param parameters The parameters for the condition
     * @return This SelectBuilder for chaining
     */
    SelectBuilder and(String condition, Object... parameters);
    
    /**
     * Add an OR condition to the WHERE clause.
     *
     * @param condition The OR condition
     * @param parameters The parameters for the condition
     * @return This SelectBuilder for chaining
     */
    SelectBuilder or(String condition, Object... parameters);
    
    /**
     * Add a JOIN clause to the query.
     *
     * @param type The join type (INNER, LEFT, RIGHT, etc.)
     * @param table The table to join
     * @param condition The join condition
     * @return This SelectBuilder for chaining
     */
    SelectBuilder join(String type, String table, String condition);
    
    /**
     * Add an INNER JOIN clause to the query.
     *
     * @param table The table to join
     * @param condition The join condition
     * @return This SelectBuilder for chaining
     */
    SelectBuilder innerJoin(String table, String condition);
    
    /**
     * Add a LEFT JOIN clause to the query.
     *
     * @param table The table to join
     * @param condition The join condition
     * @return This SelectBuilder for chaining
     */
    SelectBuilder leftJoin(String table, String condition);
    
    /**
     * Add a RIGHT JOIN clause to the query.
     *
     * @param table The table to join
     * @param condition The join condition
     * @return This SelectBuilder for chaining
     */
    SelectBuilder rightJoin(String table, String condition);
    
    /**
     * Add a GROUP BY clause to the query.
     *
     * @param columns The columns to group by
     * @return This SelectBuilder for chaining
     */
    SelectBuilder groupBy(String... columns);
    
    /**
     * Add a HAVING clause to the query.
     *
     * @param condition The HAVING condition
     * @param parameters The parameters for the condition
     * @return This SelectBuilder for chaining
     */
    SelectBuilder having(String condition, Object... parameters);
    
    /**
     * Add an ORDER BY clause to the query.
     *
     * @param orderBy The ORDER BY expression
     * @return This SelectBuilder for chaining
     */
    SelectBuilder orderBy(String orderBy);
    
    /**
     * Add a LIMIT clause to the query.
     *
     * @param limit The maximum number of rows to return
     * @return This SelectBuilder for chaining
     */
    SelectBuilder limit(int limit);
    
    /**
     * Add an OFFSET clause to the query.
     *
     * @param offset The number of rows to skip
     * @return This SelectBuilder for chaining
     */
    SelectBuilder offset(int offset);
    
    /**
     * Add a FOR UPDATE clause to the query.
     *
     * @return This SelectBuilder for chaining
     */
    SelectBuilder forUpdate();
}