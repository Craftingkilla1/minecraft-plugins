// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/impl/query/DefaultDeleteBuilder.java
package com.minecraft.sqlbridge.impl.query;

import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.query.AbstractQueryBuilder;
import com.minecraft.sqlbridge.api.query.DeleteBuilder;
import com.minecraft.sqlbridge.dialect.Dialect;
import com.minecraft.sqlbridge.dialect.DialectFeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of DeleteBuilder.
 */
public class DefaultDeleteBuilder extends AbstractQueryBuilder implements DeleteBuilder {

    private final Dialect dialect;
    private String table;
    private final List<String> whereConditions;
    private String orderBy;
    private Integer limit;
    private String[] returningColumns;

    /**
     * Constructor for DefaultDeleteBuilder.
     *
     * @param database The database to use for executing queries
     * @param dialect The SQL dialect to use
     */
    public DefaultDeleteBuilder(Database database, Dialect dialect) {
        super(database);
        this.dialect = dialect;
        this.whereConditions = new ArrayList<>();
    }

    @Override
    public DeleteBuilder from(String table) {
        this.table = table;
        return this;
    }

    @Override
    public DeleteBuilder where(String condition, Object... parameters) {
        whereConditions.clear();
        whereConditions.add(condition);
        addParameters(parameters);
        return this;
    }

    @Override
    public DeleteBuilder and(String condition, Object... parameters) {
        if (!whereConditions.isEmpty()) {
            whereConditions.add(condition);
            addParameters(parameters);
        } else {
            where(condition, parameters);
        }
        return this;
    }

    @Override
    public DeleteBuilder or(String condition, Object... parameters) {
        if (!whereConditions.isEmpty()) {
            whereConditions.add("OR " + condition);
            addParameters(parameters);
        } else {
            where(condition, parameters);
        }
        return this;
    }

    @Override
    public DeleteBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public DeleteBuilder orderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    @Override
    public DeleteBuilder returning(String... columns) {
        this.returningColumns = columns;
        return this;
    }

    @Override
    public String getSQL() {
        if (table == null || table.isEmpty()) {
            throw new IllegalStateException("DELETE query must specify a table");
        }

        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(dialect.formatTableName(table));
        
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
        
        // ORDER BY clause - only supported by some dialects for DELETE
        if (orderBy != null) {
            if (dialect.getDatabaseType().equals("mysql")) {
                sql.append(" ORDER BY ").append(orderBy);
            }
        }
        
        // LIMIT clause - only supported by some dialects for DELETE
        if (limit != null) {
            if (dialect.getDatabaseType().equals("mysql")) {
                sql.append(" LIMIT ").append(limit);
            }
        }
        
        // RETURNING clause if supported
        if (returningColumns != null && 
            returningColumns.length > 0 && 
            dialect.supportsFeature(DialectFeature.RETURNING_CLAUSE)) {
            
            String returningClause = dialect.getReturningClause(
                    Arrays.stream(returningColumns)
                          .map(dialect::formatColumnName)
                          .collect(Collectors.joining(", ")));
            
            if (returningClause != null) {
                sql.append(" ").append(returningClause);
            }
        }
        
        return sql.toString();
    }
}