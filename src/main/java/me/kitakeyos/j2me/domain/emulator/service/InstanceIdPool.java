package me.kitakeyos.j2me.domain.emulator.service;

import java.util.PriorityQueue;

/**
 * ID Pool for managing instance IDs with reuse capability
 * Uses a min-heap to efficiently track and reuse released IDs
 * Thread-safe implementation for concurrent access
 */
public class InstanceIdPool {
    // Min heap to keep track of available IDs in sorted order
    private final PriorityQueue<Integer> availableIds;
    // Track the next new ID to generate when pool is empty
    private int nextNewId;

    public InstanceIdPool() {
        this.availableIds = new PriorityQueue<>();
        this.nextNewId = 1;
    }

    /**
     * Acquire an ID from the pool
     * Returns the smallest available ID, or generates a new one if pool is empty
     * @return Available instance ID
     */
    public synchronized int acquireId() {
        if (!availableIds.isEmpty()) {
            return availableIds.poll();
        }
        return nextNewId++;
    }

    /**
     * Release an ID back to the pool for reuse
     * @param id The ID to release
     */
    public synchronized void releaseId(int id) {
        if (id > 0 && !availableIds.contains(id)) {
            availableIds.offer(id);
        }
    }

    /**
     * Reset the pool to initial state
     * Clears all available IDs and resets the counter
     */
    public synchronized void reset() {
        availableIds.clear();
        nextNewId = 1;
    }

    /**
     * Get the size of available IDs in pool
     * @return Number of reusable IDs
     */
    public synchronized int getAvailableCount() {
        return availableIds.size();
    }

    /**
     * Get the next ID that will be generated (if pool is empty)
     * @return Next new ID value
     */
    public synchronized int getNextNewId() {
        return nextNewId;
    }

    /**
     * Get statistics about the ID pool
     * @return Statistics string
     */
    public synchronized String getStatistics() {
        return String.format("ID Pool: %d available IDs, next new ID: %d",
            availableIds.size(), nextNewId);
    }
}
