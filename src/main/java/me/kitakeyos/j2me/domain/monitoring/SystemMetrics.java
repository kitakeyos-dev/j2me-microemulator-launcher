package me.kitakeyos.j2me.domain.monitoring;

/**
 * Read-only view of JVM and host resource usage.
 * <p>
 * Declared here so the monitor dialog can render metrics without importing the
 * JMX-backed implementation, and so a fake can drive the dialog in a test.
 * All values are sampled at call time.
 */
public interface SystemMetrics {

    /**
     * @return heap bytes currently in use
     */
    long getUsedHeapMemory();

    /**
     * @return heap bytes the JVM will grow to at most
     */
    long getMaxHeapMemory();

    /**
     * @return non-heap bytes in use (metaspace, code cache)
     */
    long getUsedNonHeapMemory();

    /**
     * @return host-wide CPU load in 0..1, or a negative value when unavailable
     */
    double getSystemCpuLoad();

    /**
     * @return this process's CPU load in 0..1, or a negative value when
     *         unavailable
     */
    double getProcessCpuLoad();

    /**
     * @return live thread count
     */
    int getThreadCount();

    /**
     * @return the OS load average, or a negative value when unavailable
     */
    double getSystemLoadAverage();

    /**
     * @return total physical RAM in bytes, or 0 when unavailable
     */
    long getTotalPhysicalMemory();

    /**
     * @return free physical RAM in bytes, or 0 when unavailable
     */
    long getFreePhysicalMemory();
}
