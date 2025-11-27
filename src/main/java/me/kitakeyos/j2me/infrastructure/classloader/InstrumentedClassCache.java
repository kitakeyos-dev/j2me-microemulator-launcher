package me.kitakeyos.j2me.infrastructure.classloader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Thread-safe cache for instrumented bytecode.
 * Allows sharing of instrumented classes across multiple EmulatorClassLoader instances
 * to avoid repeated instrumentation overhead.
 */
public class InstrumentedClassCache {
    private static final Logger logger = Logger.getLogger(InstrumentedClassCache.class.getName());

    // Cache key: className -> instrumented bytecode
    private static final ConcurrentHashMap<String, byte[]> cache = new ConcurrentHashMap<>();

    // Statistics
    private static volatile long hits = 0;
    private static volatile long misses = 0;

    /**
     * Get instrumented bytecode from cache
     * @param className The fully qualified class name
     * @return The instrumented bytecode, or null if not in cache
     */
    public static byte[] get(String className) {
        byte[] bytecode = cache.get(className);
        if (bytecode != null) {
            hits++;
            logger.fine("Cache HIT for class: " + className);
        } else {
            misses++;
            logger.fine("Cache MISS for class: " + className);
        }
        return bytecode;
    }

    /**
     * Put instrumented bytecode into cache
     * @param className The fully qualified class name
     * @param bytecode The instrumented bytecode
     */
    public static void put(String className, byte[] bytecode) {
        cache.put(className, bytecode);
        logger.fine("Cached instrumented class: " + className);
    }

    /**
     * Check if a class is already in cache
     * @param className The fully qualified class name
     * @return true if cached, false otherwise
     */
    public static boolean contains(String className) {
        return cache.containsKey(className);
    }

    /**
     * Clear the entire cache
     */
    public static void clear() {
        cache.clear();
        hits = 0;
        misses = 0;
        logger.info("Instrumented class cache cleared");
    }

    /**
     * Get cache statistics
     * @return String with cache hit/miss statistics
     */
    public static String getStatistics() {
        long total = hits + misses;
        double hitRate = total > 0 ? (hits * 100.0 / total) : 0.0;
        return String.format("Cache statistics - Size: %d, Hits: %d, Misses: %d, Hit rate: %.2f%%",
                cache.size(), hits, misses, hitRate);
    }

    /**
     * Get the current cache size
     * @return Number of cached classes
     */
    public static int size() {
        return cache.size();
    }
}
