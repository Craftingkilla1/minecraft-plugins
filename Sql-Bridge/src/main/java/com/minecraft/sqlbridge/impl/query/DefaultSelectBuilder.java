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
    private final List<JoinClause> joins;
    private final List<String> groupByColumns;
    private String havingCondition;
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
        this.joins = new ArrayList<>();
        this.groupByColumns = new ArrayList<>();
    }
    
    @Override
    public SelectBuilder columns(String... columns) {
        this.columns.clear();
        for (String column : columns) {
            this.columns.add(column);
        }
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
        joins.add(new JoinClause(type, table, condition));
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
        groupByColumns.clear();
        groupByColumns.addAll(Arrays.asList(columns));
        return this;
    }
    
    @Override
    public SelectBuilder having(String condition, Object... parameters) {
        this.havingCondition = condition;
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
        StringBuilder sql = new StringBuilder("SELECT ");
        
        // Columns
        if (columns.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(String.join(", ", columns));
        }
        
        // Table
        sql.append(" FROM ");
        if (table != null) {
            sql.append(dialect.formatTableName(table));
        } else {
            sql.append("(SELECT 1) as dummy");
        }
        
        // Joins
        for (JoinClause join : joins) {
            sql.append(" ").append(join.getType()).append(" JOIN ")
               .append(dialect.formatTableName(join.getTable()))
               .append(" ON ").append(join.getCondition());
        }
        
        // Where conditions
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
        
        // Group By
        if (!groupByColumns.isEmpty()) {
            sql.append(" GROUP BY ");
            sql.append(String.join(", ", groupByColumns));
        }
        
        // Having
        if (havingCondition != null) {
            sql.append(" HAVING ").append(havingCondition);
        }
        
        // Order By
        if (orderBy != null) {
            sql.append(" ORDER BY ").append(orderBy);
        }
        
        // Limit and Offset
        if (limit != null) {
            sql.append(" ").append(dialect.getLimitClause(limit, offset));
        }
        
        // For Update
        if (forUpdate) {
            sql.append(" FOR UPDATE");
        }
        
        return sql.toString();
    }
    
    /**
     * Represents a JOIN clause in a SELECT query.
     */
    private static class JoinClause {
        private final String type;
        private final String table;
        private final String condition;
        
        /**
         * Constructor for JoinClause.
         *
         * @param type The join type (INNER, LEFT, RIGHT, etc.)
         * @param table The table to join
         * @param condition The join condition
         */
        public JoinClause(String type, String table, String condition) {
            this.type = type;
            this.table = table;
            this.condition = condition;
        }
        
        /**
         * Get the join type.
         *
         * @return The join type
         */
        public String getType() {
            return type;
        }
        
        /**
         * Get the table to join.
         *
         * @return The table name
         */
        public String getTable() {
            return table;
        }
        
        /**
         * Get the join condition.
         *
         * @return The join condition
         */
        public String getCondition() {
            return condition;
        }
    }
}