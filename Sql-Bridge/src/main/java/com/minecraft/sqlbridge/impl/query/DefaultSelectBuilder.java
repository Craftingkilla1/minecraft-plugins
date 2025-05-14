// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/impl/query/DefaultSelectBuilder.java
package com.minecraft.sqlbridge.impl.query;

import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.query.AbstractQueryBuilder;
import com.minecraft.sqlbridge.api.query.SelectBuilder;
import com.minecraft.sqlbridge.dialect.Dialect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of SelectBuilder.
 */
public class DefaultSelectBuilder extends AbstractQueryBuilder implements SelectBuilder {

    private final Dialect dialect;
    private final List<String> columns;
    private String table;
    private final List<String> whereConditions;
    private final List<String> joinClauses;
    private final List<String> groupByColumns;
    private String havingClause;
    private String orderBy;
    private Integer limit;
    private Integer offset;
    private boolean forUpdate;

    /**
     * Constructor for DefaultSelectBuilder.
     *
     * @param database The database to use for executing queries
     * @param dialect The SQL dialect to use
     */
    public DefaultSelectBuilder(Database database, Dialect dialect) {
        super(database);
        this.dialect = dialect;
        this.columns = new ArrayList<>();
        this.whereConditions = new ArrayList<>();
        this.joinClauses = new ArrayList<>();
        this.groupByColumns = new ArrayList<>();
    }

    @Override
    public SelectBuilder columns(String... columns) {
        this.columns.clear();
        this.columns.addAll(Arrays.asList(columns));
        return this;
    }

    @Override
    public SelectBuilder from(String table) {
        this.table = table;
        return this;
    }

    @Override
    public SelectBuilder where(String condition, Object... parameters) {
        whereConditions.clear();
        whereConditions.add(condition);
        addParameters(parameters);
        return this;
    }

    @Override
    public SelectBuilder and(String condition, Object... parameters) {
        if (!whereConditions.isEmpty()) {
            whereConditions.add(condition);
            addParameters(parameters);
        } else {
            where(condition, parameters);
        }
        return this;
    }

    @Override
    public SelectBuilder or(String condition, Object... parameters) {
        if (!whereConditions.isEmpty()) {
            whereConditions.add("OR " + condition);
            addParameters(parameters);
        } else {
            where(condition, parameters);
        }
        return this;
    }

    @Override
    public SelectBuilder join(String type, String table, String condition) {
        joinClauses.add(type + " JOIN " + dialect.formatTableName(table) + " ON " + condition);
        return this;
    }

    @Override
    public SelectBuilder innerJoin(String table, String condition) {
        return join("INNER", table, condition);
    }

    @Override
    public SelectBuilder leftJoin(String table, String condition) {
        return join("LEFT", table, condition);
    }

    @Override
    public SelectBuilder rightJoin(String table, String condition) {
        return join("RIGHT", table, condition);
    }

    @Override
    public SelectBuilder groupBy(String... columns) {
        this.groupByColumns.clear();
        this.groupByColumns.addAll(Arrays.asList(columns));
        return this;
    }

    @Override
    public SelectBuilder having(String condition, Object... parameters) {
        this.havingClause = condition;
        addParameters(parameters);
        return this;
    }

    @Override
    public SelectBuilder orderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    @Override
    public SelectBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public SelectBuilder offset(int offset) {
        this.offset = offset;
        return this;
    }

    @Override
    public SelectBuilder forUpdate() {
        this.forUpdate = true;
        return this;
    }

    @Override
    public String getSQL() {
        if (table == null || table.isEmpty()) {
            throw new IllegalStateException("SELECT query must specify a table");
        }

        StringBuilder sql = new StringBuilder("SELECT ");
        
        // Columns
        if (columns.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(columns.stream()
                    .map(dialect::formatColumnName)
                    .collect(Collectors.joining(", ")));
        }
        
        // FROM clause
        sql.append(" FROM ").append(dialect.formatTableName(table));
        
        // JOIN clauses
        if (!joinClauses.isEmpty()) {
            sql.append(" ").append(String.join(" ", joinClauses));
        }
        
        // WHERE clause
        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ");
            boolean first = true;
            for (String condition : whereConditions) {
                if (!first) {
                    if (condition.startsWith("OR ")) {
                        sql.append(" OR ");
                        sql.append(condition.substring(3));
                    } else {
                        sql.append(" AND ");
                        sql.append(condition);
                    }
                } else {
                    sql.append(condition);
                    first = false;
                }
            }
        }
        
        // GROUP BY clause
        if (!groupByColumns.isEmpty()) {
            sql.append(" GROUP BY ").append(groupByColumns.stream()
                    .map(dialect::formatColumnName)
                    .collect(Collectors.joining(", ")));
        }
        
        // HAVING clause
        if (havingClause != null) {
            sql.append(" HAVING ").append(havingClause);
        }
        
        // ORDER BY clause
        if (orderBy != null) {
            sql.append(" ORDER BY ").append(orderBy);
        }
        
        // LIMIT and OFFSET
        if (limit != null) {
            sql.append(" ").append(dialect.getLimitClause(limit, offset));
        }
        
        // FOR UPDATE
        if (forUpdate) {
            sql.append(" FOR UPDATE");
        }
        
        return sql.toString();
    }
}