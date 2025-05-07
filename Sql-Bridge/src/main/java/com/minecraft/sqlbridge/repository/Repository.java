package com.minecraft.sqlbridge.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository interface for database operations.
 *
 * @param <T> The entity type
 * @param <ID> The ID type
 */
public interface Repository<T, ID> {

    /**
     * Save an entity
     *
     * @param entity The entity to save
     * @return The saved entity
     */
    T save(T entity);

    /**
     * Find an entity by ID
     *
     * @param id The entity ID
     * @return Optional containing the entity, or empty if not found
     */
    Optional<T> findById(ID id);

    /**
     * Find all entities
     *
     * @return List of all entities
     */
    List<T> findAll();

    /**
     * Delete an entity
     *
     * @param entity The entity to delete
     * @return True if the entity was deleted, false otherwise
     */
    boolean delete(T entity);

    /**
     * Delete an entity by ID
     *
     * @param id The entity ID
     * @return True if the entity was deleted, false otherwise
     */
    boolean deleteById(ID id);

    /**
     * Check if an entity exists
     *
     * @param id The entity ID
     * @return True if the entity exists, false otherwise
     */
    boolean exists(ID id);

    /**
     * Count all entities
     *
     * @return The number of entities
     */
    long count();
}