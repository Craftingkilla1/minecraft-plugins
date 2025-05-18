// ./Example-Plugin/src/main/java/com/minecraft/example/sql/models/Achievement.java
package com.minecraft.example.sql.models;

import java.sql.Timestamp;

/**
 * Represents an achievement in the database.
 * Used with SQL-Bridge's result mapping.
 */
public class Achievement {
    
    private int id;
    private String identifier;
    private String name;
    private String description;
    private String iconMaterial;
    private Timestamp createdAt;
    
    /**
     * Default constructor.
     */
    public Achievement() {
    }
    
    /**
     * Creates a new achievement with the specified details.
     *
     * @param identifier The unique identifier
     * @param name The display name
     * @param description The description
     */
    public Achievement(String identifier, String name, String description) {
        this.identifier = identifier;
        this.name = name;
        this.description = description;
        this.iconMaterial = "DIAMOND";
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }
    
    /**
     * Creates a new achievement with the specified details.
     *
     * @param identifier The unique identifier
     * @param name The display name
     * @param description The description
     * @param iconMaterial The icon material name
     */
    public Achievement(String identifier, String name, String description, String iconMaterial) {
        this.identifier = identifier;
        this.name = name;
        this.description = description;
        this.iconMaterial = iconMaterial;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Gets the achievement's database ID.
     *
     * @return The achievement's ID
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the achievement's database ID.
     *
     * @param id The achievement's ID
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets the achievement's unique identifier.
     *
     * @return The identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the achievement's unique identifier.
     *
     * @param identifier The identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Gets the achievement's display name.
     *
     * @return The display name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the achievement's display name.
     *
     * @param name The display name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the achievement's description.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the achievement's description.
     *
     * @param description The description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the achievement's icon material name.
     *
     * @return The icon material name
     */
    public String getIconMaterial() {
        return iconMaterial;
    }

    /**
     * Sets the achievement's icon material name.
     *
     * @param iconMaterial The icon material name
     */
    public void setIconMaterial(String iconMaterial) {
        this.iconMaterial = iconMaterial;
    }

    /**
     * Gets the timestamp when the achievement was created.
     *
     * @return The creation timestamp
     */
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the timestamp when the achievement was created.
     *
     * @param createdAt The creation timestamp
     */
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Achievement that = (Achievement) o;
        return identifier.equals(that.identifier);
    }
    
    @Override
    public int hashCode() {
        return identifier.hashCode();
    }
    
    @Override
    public String toString() {
        return "Achievement{" +
                "id=" + id +
                ", identifier='" + identifier + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", iconMaterial='" + iconMaterial + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}