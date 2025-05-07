package com.minecraft.sqlbridge.repository;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.sqlbridge.api.Database;
import com.minecraft.sqlbridge.api.QueryBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Abstract base repository class that provides common CRUD operations.
 *
 * @param <T> The entity type
 * @param <ID> The ID type
 */
public abstract class AbstractRepository<T, ID> implements Repository<T, ID> {

    protected final Database database;
    protected final String tableName;
    protected final String idColumn;

    /**
     * Create a new abstract repository
     *
     * @param database The database
     * @param tableName The table name
     * @param idColumn The ID column name
     */
    protected AbstractRepository(Database database, String tableName, String idColumn) {
        this.database = database;
        this.tableName = tableName;
        this.idColumn = idColumn;
    }

    @Override
    public T save(T entity) {
        ID id = getId(entity);
        
        if (id != null && exists(id)) {
            // Update existing entity
            update(entity);
        } else {
            // Insert new entity
            id = insert(entity);
            setId(entity, id);
        }
        
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) {
        QueryBuilder queryBuilder = database.createQueryBuilder()
                .select("*")
                .from(tableName)
                .where(idColumn, "=", id);
        
        return database.queryFirst(queryBuilder.build(), this::mapRow, id);
    }

    @Override
    public List<T> findAll() {
        QueryBuilder queryBuilder = database.createQueryBuilder()
                .select("*")
                .from(tableName);
        
        return database.query(queryBuilder.build(), this::mapRow);
    }

    @Override
    public boolean delete(T entity) {
        ID id = getId(entity);
        return id != null && deleteById(id);
    }

    @Override
    public boolean deleteById(ID id) {
        QueryBuilder queryBuilder = database.createQueryBuilder()
                .deleteFrom(tableName)
                .where(idColumn, "=", id);
        
        int affected = database.update(queryBuilder.build(), id);
        return affected > 0;
    }

    @Override
    public boolean exists(ID id) {
        QueryBuilder queryBuilder = database.createQueryBuilder()
                .select("1")
                .from(tableName)
                .where(idColumn, "=", id);
        
        return database.queryFirst(queryBuilder.build(), row -> true, id).isPresent();
    }

    @Override
    public long count() {
        QueryBuilder queryBuilder = database.createQueryBuilder()
                .select("COUNT(*)")
                .from(tableName);
        
        return database.queryFirst(queryBuilder.build(), row -> {
            Object count = row.values().iterator().next();
            if (count instanceof Number) {
                return ((Number) count).longValue();
            }
            return 0L;
        }).orElse(0L);
    }

    /**
     * Update an existing entity
     *
     * @param entity The entity to update
     * @return True if the entity was updated, false otherwise
     */
    protected boolean update(T entity) {
        Map<String, Object> values = getUpdateValues(entity);
        ID id = getId(entity);
        
        if (values.isEmpty() || id == null) {
            return false;
        }
        
        QueryBuilder queryBuilder = database.createQueryBuilder()
                .update(tableName);
        
        // Add column=value pairs
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            queryBuilder.set(entry.getKey(), entry.getValue());
        }
        
        // Add where clause
        queryBuilder.where(idColumn, "=", id);
        
        // Execute the update
        List<Object> params = new ArrayList<>(values.size() + 1);
        params.addAll(values.values());
        params.add(id);
        
        int affected = database.update(queryBuilder.build(), params.toArray());
        return affected > 0;
    }

    /**
     * Insert a new entity
     *
     * @param entity The entity to insert
     * @return The generated ID
     */
    protected ID insert(T entity) {
        Map<String, Object> values = getInsertValues(entity);
        
        if (values.isEmpty()) {
            return null;
        }
        
        QueryBuilder queryBuilder = database.createQueryBuilder()
                .insertInto(tableName);
        
        String[] columns = values.keySet().toArray(new String[0]);
        Object[] params = values.values().toArray();
        
        queryBuilder.columns(columns);
        
        if (params.length == 1) {
            queryBuilder.values(params[0]);
        } else if (params.length > 1) {
            queryBuilder.values(params);
        }
        
        try {
            long generatedId = database.insert(queryBuilder.build(), params);
            return convertId(generatedId);
        } catch (Exception e) {
            LogUtil.severe("Error inserting entity: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Map a database row to an entity
     *
     * @param row The database row
     * @return The entity
     */
    protected abstract T mapRow(Map<String, Object> row);

    /**
     * Get the ID of an entity
     *
     * @param entity The entity
     * @return The ID
     */
    protected abstract ID getId(T entity);

    /**
     * Set the ID of an entity
     *
     * @param entity The entity
     * @param id The ID
     */
    protected abstract void setId(T entity, ID id);

    /**
     * Get the values for inserting a new entity
     *
     * @param entity The entity
     * @return Map of column names to values
     */
    protected abstract Map<String, Object> getInsertValues(T entity);

    /**
     * Get the values for updating an existing entity
     *
     * @param entity The entity
     * @return Map of column names to values
     */
    protected abstract Map<String, Object> getUpdateValues(T entity);

    /**
     * Convert a long ID to the ID type
     *
     * @param id The long ID
     * @return The converted ID
     */
    @SuppressWarnings("unchecked")
    protected ID convertId(long id) {
        // Default implementation assumes ID is a Number or String
        Object result;
        
        try {
            // Determine type of ID and convert accordingly
            Class<?> idClass = findIdClass();
            
            if (idClass == Integer.class) {
                result = (int) id;
            } else if (idClass == Long.class) {
                result = id;
            } else if (idClass == String.class) {
                result = String.valueOf(id);
            } else {
                // Default to Long
                result = id;
            }
            
            return (ID) result;
        } catch (Exception e) {
            LogUtil.warning("Error converting ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Find the class of the ID type
     *
     * @return The ID class
     */
    @SuppressWarnings("unchecked")
    protected Class<ID> findIdClass() {
        // This is a best-effort attempt to find the ID class
        // Subclasses can override this for more specific behavior
        return (Class<ID>) Long.class;
    }
}