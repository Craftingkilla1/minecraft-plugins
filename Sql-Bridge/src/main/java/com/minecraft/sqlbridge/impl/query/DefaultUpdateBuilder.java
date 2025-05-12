// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/impl/query/DefaultUpdateBuilder.java
package com.minecraft.sqlbridge.impl.query;

import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.query.AbstractQueryBuilder;
import com.minecraft.sqlbridge.api.query.UpdateBuilder;
import com.minecraft.sqlbridge.dialect.Dialect;
import com.minecraft.sqlbridge.dialect.DialectFeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of UpdateBuilder.
 */
public class DefaultUpdateBuilder extends AbstractQueryBuilder implements UpdateBuilder {

    private final Dialect dialect;
    private final String table;
    private final Map<String, Object> setValues;
    private final List<String> whereConditions;
    private String orderBy;
    private Integer limit;
    private String[] returningColumns;

    /**
     * Constructor for DefaultUpdateBuilder.
     *
     * @param database The database to use for executing queries
     * @param dialect The SQL dialect to use
     * @param table The table to update
     */
    public DefaultUpdateBuilder(Database database, Dialect dialect, String table) {
        super(database);
        this.dialect = dialect;
        this.table = table;
        this.setValues = new LinkedHashMap<>();
        this.whereConditions = new ArrayList<>();
    }

    @Override
    public UpdateBuilder set(String column, Object value) {
        setValues.put(column, value);
        addParameters(value);
        return this;
    }

    @Override
    public UpdateBuilder set(Map<String, Object> columnValues) {
        setValues.putAll(columnValues);
        addParameters(columnValues.values().toArray());
        return this;
    }

    @Override
    public UpdateBuilder where(String condition, Object... parameters) {
        whereConditions.clear();
        whereConditions.add(condition);
        addParameters(parameters);
        return this;
    }

    @Override
    public UpdateBuilder and(String condition, Object... parameters) {
        if (!whereConditions.isEmpty()) {
            whereConditions.add(condition);
            addParameters(parameters);
        } else {
            where(condition, parameters);
        }
        return this;
    }

    @Override
    public UpdateBuilder or(String condition, Object... parameters) {
        if (!whereConditions.isEmpty()) {
            whereConditions.add("OR " + condition);
            addParameters(parameters);
        } else {
            where(condition, parameters);
        }
        return this;
    }

    @Override
    public UpdateBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public UpdateBuilder orderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    @Override
    public UpdateBuilder returning(String... columns) {
        this.returningColumns = columns;
        return this;
    }

    @Override
    public String getSQL() {
        if (setValues.isEmpty()) {
            throw new IllegalStateException("UPDATE query must have at least one SET clause");
        }

        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(dialect.formatTableName(table));
        
        // SET clause
        sql.append(" SET ");
        boolean first = true;
        for (String column : setValues.keySet()) {
            if (!first) {
                sql.append(", ");
            }
            sql.append(dialect.formatColumnName(column)).append(" = ?");
            first = false;
        }
        
        // WHERE clause
        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ");
            first = true;
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
        
        // ORDER BY clause - only supported by some dialects for UPDATE
        if (orderBy != null) {
            if (dialect.getDatabaseType().equals("mysql")) {
                sql.append(" ORDER BY ").append(orderBy);
            }
        }
        
        // LIMIT clause - only supported by some dialects for UPDATE
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
    
    @Override
    public Object[] getParameters() {
        List<Object> allParams = new ArrayList<>();
        
        // Add SET values
        allParams.addAll(setValues.values());
        
        // Add WHERE parameters from parent class
        allParams.addAll(parameters.subList(setValues.size(), parameters.size()));
        
        return allParams.toArray();
    }
}