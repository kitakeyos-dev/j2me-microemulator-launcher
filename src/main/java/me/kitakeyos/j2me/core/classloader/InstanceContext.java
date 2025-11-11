package me.kitakeyos.j2me.core.classloader;

import java.util.logging.Logger;

/**
 * Thread-local context for storing the current emulator instance ID.
 * This allows instrumented bytecode to dynamically retrieve the instance ID
 * without having it baked into the bytecode itself, enabling bytecode sharing
 * across multiple instances.
 */
public class InstanceContext {
    private static final Logger logger = Logger.getLogger(InstanceContext.class.getName());

    // ThreadLocal to store the current instance ID for each thread
    private static final ThreadLocal<Integer> currentInstanceId = new ThreadLocal<>();

    /**
     * Set the instance ID for the current thread
     * @param instanceId The instance ID to set
     */
    public static void setInstanceId(int instanceId) {
        currentInstanceId.set(instanceId);
        logger.fine("Set instance ID for thread " + Thread.currentThread().getName() + ": " + instanceId);
    }

    /**
     * Get the instance ID for the current thread
     * @return The instance ID, or -1 if not set
     */
    public static int getInstanceId() {
        Integer instanceId = currentInstanceId.get();
        if (instanceId == null) {
            logger.warning("Instance ID not set for thread " + Thread.currentThread().getName());
            return -1;
        }
        return instanceId;
    }

    /**
     * Clear the instance ID for the current thread
     */
    public static void clear() {
        int instanceId = getInstanceId();
        currentInstanceId.remove();
        logger.fine("Cleared instance ID for thread " + Thread.currentThread().getName() + ": " + instanceId);
    }

    /**
     * Check if instance ID is set for the current thread
     * @return true if set, false otherwise
     */
    public static boolean isSet() {
        return currentInstanceId.get() != null;
    }
}
