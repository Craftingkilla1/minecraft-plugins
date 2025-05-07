package com.minecraft.sqlbridge.query;

import com.minecraft.sqlbridge.api.Query;
import com.minecraft.sqlbridge.api.QueryBuilder;
import com.minecraft.sqlbridge.dialect.Dialect;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Default implementation of the QueryBuilder interface.
 * Provides a fluent API for building SQL queries in a database-agnostic way.
 */
public class DefaultQueryBuilder implements QueryBuilder {

    private final Dialect dialect;
    private final StringBuilder query = new StringBuilder();
    private final List<Object> parameters = new ArrayList<>();
    
    private String queryType;
    private String tableName;
    private List<String> columns = new ArrayList<>();
    private List<String> joins = new ArrayList<>();
    private List<String> whereConditions = new ArrayList<>();
    private List<String> groupByColumns = new ArrayList<>();
    private List<String> havingConditions = new ArrayList<>();
    private List<String> orderByColumns = new ArrayList<>();
    private int limitValue = -1;
    private int offsetValue = -1;
    private boolean distinct = false;
    private List<Object[]> batchValues = new ArrayList<>();
    private List<String> setValues = new ArrayList<>();

    /**
     * Create a new query builder with the specified dialect
     *
     * @param dialect The SQL dialect to use
     */
    public DefaultQueryBuilder(Dialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public QueryBuilder select(String... columns) {
        this.queryType = "SELECT";
        this.distinct = false;
        
        for (String column : columns) {
            this.columns.add(column);
        }
        
        return this;
    }

    @Override
    public QueryBuilder selectDistinct(String... columns) {
        this.queryType = "SELECT";
        this.distinct = true;
        
        for (String column : columns) {
            this.columns.add(column);
        }
        
        return this;
    }

    @Override
    public QueryBuilder from(String table) {
        this.tableName = table;
        return this;
    }

    @Override
    public QueryBuilder join(String table, String condition) {
        joins.add("JOIN " + table + " ON " + condition);
        return this;
    }

    @Override
    public QueryBuilder leftJoin(String table, String condition) {
        joins.add("LEFT JOIN " + table + " ON " + condition);
        return this;
    }

    @Override
    public QueryBuilder rightJoin(String table, String condition) {
        joins.add("RIGHT JOIN " + table + " ON " + condition);
        return this;
    }

    @Override
    public QueryBuilder where(String condition) {
        whereConditions.add(condition);
        return this;
    }

    @Override
    public QueryBuilder where(String column, String operator, Object value) {
        whereConditions.add(column + " " + operator + " ?");
        parameters.add(value);
        return this;
    }

    @Override
    public QueryBuilder and(String condition) {
        if (!whereConditions.isEmpty()) {
            whereConditions.add("AND " + condition);
        } else {
            whereConditions.add(condition);
        }
        return this;
    }

    @Override
    public QueryBuilder and(String column, String operator, Object value) {
        if (!whereConditions.isEmpty()) {
            whereConditions.add("AND " + column + " " + operator + " ?");
        } else {
            whereConditions.add(column + " " + operator + " ?");
        }
        parameters.add(value);
        return this;
    }

    @Override
    public QueryBuilder or(String condition) {
        if (!whereConditions.isEmpty()) {
            whereConditions.add("OR " + condition);
        } else {
            whereConditions.add(condition);
        }
        return this;
    }

    @Override
    public QueryBuilder or(String column, String operator, Object value) {
        if (!whereConditions.isEmpty()) {
            whereConditions.add("OR " + column + " " + operator + " ?");
        } else {
            whereConditions.add(column + " " + operator + " ?");
        }
        parameters.add(value);
        return this;
    }

    @Override
    public QueryBuilder groupBy(String... columns) {
        for (String column : columns) {
            groupByColumns.add(column);
        }
        return this;
    }

    @Override
    public QueryBuilder having(String condition) {
        havingConditions.add(condition);
        return this;
    }

    @Override
    public QueryBuilder orderBy(String column, String direction) {
        orderByColumns.add(column + " " + direction);
        return this;
    }

    @Override
    public QueryBuilder limit(int limit) {
        this.limitValue = limit;
        return this;
    }

    @Override
    public QueryBuilder offset(int offset) {
        this.offsetValue = offset;
        return this;
    }

    @Override
    public QueryBuilder insertInto(String table) {
        this.queryType = "INSERT";
        this.tableName = table;
        return this;
    }

    @Override
    public QueryBuilder columns(String... cols) {
        for (String col : cols) {
            this.columns.add(col);
        }
        return this;
    }

    @Override
    public QueryBuilder values(Object... values) {
        for (Object value : values) {
            this.parameters.add(value);
        }
        return this;
    }

    @Override
    public QueryBuilder update(String table) {
        this.queryType = "UPDATE";
        this.tableName = table;
        return this;
    }

    @Override
    public QueryBuilder set(String column, Object value) {
        setValues.add(column + " = ?");
        parameters.add(value);
        return this;
    }

    @Override
    public QueryBuilder deleteFrom(String table) {
        this.queryType = "DELETE";
        this.tableName = table;
        return this;
    }

    @Override
    public QueryBuilder addBatch(List<Object[]> values) {
        this.batchValues.addAll(values);
        return this;
    }

    @Override
    public Object[] getParameters() {
        return parameters.toArray();
    }

    @Override
    public String build() {
        query.setLength(0); // Clear any previous query
        
        if ("SELECT".equals(queryType)) {
            buildSelectQuery();
        } else if ("INSERT".equals(queryType)) {
            buildInsertQuery();
        } else if ("UPDATE".equals(queryType)) {
            buildUpdateQuery();
        } else if ("DELETE".equals(queryType)) {
            buildDeleteQuery();
        }
        
        return query.toString();
    }

    @Override
    public Query buildQuery() {
        String sql = build();
        // Return a new Query object with the built SQL and parameters
        // Note: This assumes there's a Database instance available
        // Implementation may vary depending on how Query objects are created
        return new DefaultQuery(sql, null);
    }
    
    /**
     * Build a SELECT query
     */
    private void buildSelectQuery() {
        query.append("SELECT ");
        
        if (distinct) {
            query.append("DISTINCT ");
        }
        
        // Add columns
        if (columns.isEmpty()) {
            query.append("*");
        } else {
            StringJoiner columnJoiner = new StringJoiner(", ");
            for (String column : columns) {
                columnJoiner.add(column);
            }
            query.append(columnJoiner.toString());
        }
        
        // Add table
        if (tableName != null) {
            query.append(" FROM ").append(tableName);
        }
        
        // Add joins
        for (String join : joins) {
            query.append(" ").append(join);
        }
        
        // Add where conditions
        if (!whereConditions.isEmpty()) {
            query.append(" WHERE ");
            StringJoiner whereJoiner = new StringJoiner(" ");
            for (String condition : whereConditions) {
                whereJoiner.add(condition);
            }
            query.append(whereJoiner.toString());
        }
        
        // Add group by
        if (!groupByColumns.isEmpty()) {
            query.append(" GROUP BY ");
            StringJoiner groupByJoiner = new StringJoiner(", ");
            for (String column : groupByColumns) {
                groupByJoiner.add(column);
            }
            query.append(groupByJoiner.toString());
        }
        
        // Add having
        if (!havingConditions.isEmpty()) {
            query.append(" HAVING ");
            StringJoiner havingJoiner = new StringJoiner(" AND ");
            for (String condition : havingConditions) {
                havingJoiner.add(condition);
            }
            query.append(havingJoiner.toString());
        }
        
        // Add order by
        if (!orderByColumns.isEmpty()) {
            query.append(" ORDER BY ");
            StringJoiner orderByJoiner = new StringJoiner(", ");
            for (String column : orderByColumns) {
                orderByJoiner.add(column);
            }
            query.append(orderByJoiner.toString());
        }
        
        // Add limit and offset
        if (limitValue > 0 || offsetValue > 0) {
            query.append(dialect.paginate(limitValue, offsetValue));
        }
    }
    
    /**
     * Build an INSERT query
     */
    private void buildInsertQuery() {
        query.append("INSERT INTO ").append(tableName);
        
        // Add columns
        if (!columns.isEmpty()) {
            query.append(" (");
            StringJoiner columnJoiner = new StringJoiner(", ");
            for (String column : columns) {
                columnJoiner.add(column);
            }
            query.append(columnJoiner.toString());
            query.append(")");
        }
        
        // Add values
        query.append(" VALUES ");
        
        if (batchValues.isEmpty()) {
            // Single row insert
            query.append("(");
            StringJoiner valueJoiner = new StringJoiner(", ");
            for (int i = 0; i < columns.size(); i++) {
                valueJoiner.add("?");
            }
            query.append(valueJoiner.toString());
            query.append(")");
        } else {
            // Batch insert
            StringJoiner batchJoiner = new StringJoiner(", ");
            for (Object[] row : batchValues) {
                StringJoiner valueJoiner = new StringJoiner(", ", "(", ")");
                for (int i = 0; i < row.length; i++) {
                    valueJoiner.add("?");
                    parameters.add(row[i]);
                }
                batchJoiner.add(valueJoiner.toString());
            }
            query.append(batchJoiner.toString());
        }
    }
    
    /**
     * Build an UPDATE query
     */
    private void buildUpdateQuery() {
        query.append("UPDATE ").append(tableName);
        
        // Add set values
        if (!setValues.isEmpty()) {
            query.append(" SET ");
            StringJoiner setJoiner = new StringJoiner(", ");
            for (String setValue : setValues) {
                setJoiner.add(setValue);
            }
            query.append(setJoiner.toString());
        }
        
        // Add where conditions
        if (!whereConditions.isEmpty()) {
            query.append(" WHERE ");
            StringJoiner whereJoiner = new StringJoiner(" ");
            for (String condition : whereConditions) {
                whereJoiner.add(condition);
            }
            query.append(whereJoiner.toString());
        }
    }
    
    /**
     * Build a DELETE query
     */
    private void buildDeleteQuery() {
        query.append("DELETE FROM ").append(tableName);
        
        // Add where conditions
        if (!whereConditions.isEmpty()) {
            query.append(" WHERE ");
            StringJoiner whereJoiner = new StringJoiner(" ");
            for (String condition : whereConditions) {
                whereJoiner.add(condition);
            }
            query.append(whereJoiner.toString());
        }
    }
}