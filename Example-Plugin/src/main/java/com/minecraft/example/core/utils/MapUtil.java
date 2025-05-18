// ./Example-Plugin/src/main/java/com/minecraft/example/core/utils/MapUtil.java
package com.minecraft.example.core.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for creating and manipulating Maps in a Java 8 compatible way.
 */
public class MapUtil {
    
    /**
     * Creates a map with a single key-value pair.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @param key The key
     * @param value The value
     * @return A map containing the key-value pair
     */
    public static <K, V> Map<K, V> of(K key, V value) {
        Map<K, V> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
    
    /**
     * Creates a map with two key-value pairs.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @param k1 The first key
     * @param v1 The first value
     * @param k2 The second key
     * @param v2 The second value
     * @return A map containing the key-value pairs
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
    
    /**
     * Creates a map with three key-value pairs.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @param k1 The first key
     * @param v1 The first value
     * @param k2 The second key
     * @param v2 The second value
     * @param k3 The third key
     * @param v3 The third value
     * @return A map containing the key-value pairs
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }
    
    /**
     * Creates a map with four key-value pairs.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @param k1 The first key
     * @param v1 The first value
     * @param k2 The second key
     * @param v2 The second value
     * @param k3 The third key
     * @param v3 The third value
     * @param k4 The fourth key
     * @param v4 The fourth value
     * @return A map containing the key-value pairs
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }
    
    /**
     * Creates a map with five key-value pairs.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @param k1 The first key
     * @param v1 The first value
     * @param k2 The second key
     * @param v2 The second value
     * @param k3 The third key
     * @param v3 The third value
     * @param k4 The fourth key
     * @param v4 The fourth value
     * @param k5 The fifth key
     * @param v5 The fifth value
     * @return A map containing the key-value pairs
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        return map;
    }
    
    /**
     * Creates a map by adding entries to an existing map.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @param map The map to add entries to
     * @param key The key to add
     * @param value The value to add
     * @return The map with the added entry
     */
    public static <K, V> Map<K, V> with(Map<K, V> map, K key, V value) {
        map.put(key, value);
        return map;
    }
}