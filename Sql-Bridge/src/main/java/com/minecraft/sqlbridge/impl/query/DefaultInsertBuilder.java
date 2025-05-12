// ./Sql-Bridge/src/main/java/com/minecraft/sqlbridge/impl/query/DefaultInsertBuilder.java
package com.minecraft.sqlbridge.impl.query;

import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.query.AbstractQueryBuilder;
import com.minecraft.sqlbridge.api.query.InsertBuilder;
import com.minecraft.sqlbridge.api.query.SelectBuilder;
import com.minecraft.sqlbridge.dialect.Dialect;
import com.minecraft.sqlbridge.dialect.DialectFeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of InsertBuilder.
 */
public class DefaultInsertBuilder extends AbstractQueryBuilder implements InsertBuilder {

    private final Dialect dialect;
    private final String table;
    private final List<String> columns;
    private final List<List<Object>> rows;
    private Map<String, Object> onDuplicateKeyUpdateValues;
    private SelectBuilder selectBuilder;
    private String[] returningColumns;

    /**
     * Constructor for DefaultInsertBuilder.
     *
     * @param database The database to use for executing queries
     * @param dialect The SQL dialect to use
     * @param table The table to insert into
     */
    public DefaultInsertBuilder(Database database, Dialect dialect, String table) {
        super(database);
        this.dialect = dialect;
        this.table = table;
        this.columns = new ArrayList<>();
        this.rows = new ArrayList<>();
    }

    @Override
    public InsertBuilder columns(String... columns) {
        this.columns.clear();
        this.columns.addAll(Arrays.asList(columns));
        return this;
    }

    @Override
    public InsertBuilder values(Object... values) {
        List<Object> valuesList = new ArrayList<>(Arrays.asList(values));
        rows.clear();
        rows.add(valuesList);
        addParameters(values);
        return this;
    }

    @Override
    public InsertBuilder addRow(Object... values) {
        List<Object> valuesList = new ArrayList<>(Arrays.asList(values));
        rows.add(valuesList);
        addParameters(values);
        return this;
    }

    @Override
    public InsertBuilder columnValues(Map<String, Object> columnValues) {
        columns.clear();
        List<Object> values = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : columnValues.entrySet()) {
            columns.add(entry.getKey());
            values.add(entry.getValue());
        }
        
        rows.clear();
        rows.add(values);
        addParameters(values.toArray());
        return this;
    }

    @Override
    public InsertBuilder onDuplicateKeyUpdate(String column, Object value) {
        if (onDuplicateKeyUpdateValues == null) {
            onDuplicateKeyUpdateValues = new HashMap<>();
        }
        onDuplicateKeyUpdateValues.put(column, value);
        addParameters(value);
        return this;
    }

    @Override
    public InsertBuilder onDuplicateKeyUpdate(Map<String, Object> columnValues) {
        if (onDuplicateKeyUpdateValues == null) {
            onDuplicateKeyUpdateValues = new HashMap<>();
        }
        onDuplicateKeyUpdateValues.putAll(columnValues);
        addParameters(columnValues.values().toArray());
        return this;
    }

    @Override
    public InsertBuilder select(SelectBuilder selectBuilder) {
        this.selectBuilder = selectBuilder;
        return this;
    }

    @Override
    public InsertBuilder returning(String... columns) {
        this.returningColumns = columns;
        return this;
    }

    @Override
    public String getSQL() {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(dialect.formatTableName(table));
        
        // Columns
        if (!columns.isEmpty()) {
            sql.append(" (");
            sql.append(columns.stream()
                    .map(dialect::formatColumnName)
                    .collect(Collectors.joining(", ")));
            sql.append(")");
        }
        
        // Values
        if (selectBuilder != null) {
            // Insert from SELECT
            sql.append(" ").append(selectBuilder.getSQL());
        } else if (!rows.isEmpty()) {
            sql.append(" VALUES ");
            
            // Handle multiple rows if supported by the dialect
            if (rows.size() > 1 && dialect.supportsFeature(DialectFeature.MULTI_ROW_INSERT)) {
                for (int i = 0; i < rows.size(); i++) {
                    if (i > 0) {
                        sql.append(", ");
                    }
                    appendRowValues(sql, rows.get(i).size());
                }
            } else {
                // Single row
                appendRowValues(sql, rows.get(0).size());
            }
        } else {
            sql.append(" DEFAULT VALUES");
        }
        
        // ON DUPLICATE KEY UPDATE
        if (onDuplicateKeyUpdateValues != null && 
            !onDuplicateKeyUpdateValues.isEmpty() && 
            dialect.supportsFeature(DialectFeature.UPSERT)) {
            
            if (dialect.getDatabaseType().equals("mysql")) {
                sql.append(" ON DUPLICATE KEY UPDATE ");
                
                boolean first = true;
                for (Map.Entry<String, Object> entry : onDuplicateKeyUpdateValues.entrySet()) {
                    if (!first) {
                        sql.append(", ");
                    }
                    sql.append(dialect.formatColumnName(entry.getKey()));
                    sql.append(" = ?");
                    first = false;
                }
            } else if (dialect.getDatabaseType().equals("sqlite")) {
                // SQLite uses a different syntax for UPSERT
                sql.append(" ON CONFLICT(");
                
                // Use the primary key columns or first column as conflict target
                sql.append(columns.get(0)); // Simplified - should use actual PK columns
                
                sql.append(") DO UPDATE SET ");
                
                boolean first = true;
                for (Map.Entry<String, Object> entry : onDuplicateKeyUpdateValues.entrySet()) {
                    if (!first) {
                        sql.append(", ");
                    }
                    sql.append(dialect.formatColumnName(entry.getKey()));
                    sql.append(" = ?");
                    first = false;
                }
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
    
    /**
     * Append placeholders for a row of values.
     *
     * @param sql The SQL builder to append to
     * @param valueCount The number of values in the row
     */
    private void appendRowValues(StringBuilder sql, int valueCount) {
        sql.append("(");
        for (int i = 0; i < valueCount; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
        sql.append(")");
    }
    
    @Override
    public Object[] getParameters() {
        List<Object> allParams = new ArrayList<>();
        
        // Add row values
        if (selectBuilder == null) {
            for (List<Object> row : rows) {
                allParams.addAll(row);
            }
        } else {
            // Add parameters from the SELECT statement
            Object[] selectParams = selectBuilder.getParameters();
            allParams.addAll(Arrays.asList(selectParams));
        }
        
        // Add ON DUPLICATE KEY UPDATE parameters
        if (onDuplicateKeyUpdateValues != null) {
            allParams.addAll(onDuplicateKeyUpdateValues.values());
        }
        
        return allParams.toArray();
    }
}