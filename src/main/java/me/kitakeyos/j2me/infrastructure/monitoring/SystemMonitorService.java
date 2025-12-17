package me.kitakeyos.j2me.infrastructure.monitoring;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

/**
 * Service to retrieve system performance metrics
 */
@SuppressWarnings("restriction")
public class SystemMonitorService {

    private final MemoryMXBean memoryBean;
    private final OperatingSystemMXBean osBean;
    private final ThreadMXBean threadBean;

    public SystemMonitorService() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
    }

    public long getUsedHeapMemory() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return heapUsage.getUsed();
    }

    public long getMaxHeapMemory() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return heapUsage.getMax();
    }

    public long getUsedNonHeapMemory() {
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        return nonHeapUsage.getUsed();
    }

    public double getSystemCpuLoad() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad() * 100;
        }
        return -1;
    }

    public double getProcessCpuLoad() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad() * 100;
        }
        return -1;
    }

    public int getThreadCount() {
        return threadBean.getThreadCount();
    }

    public double getSystemLoadAverage() {
        return osBean.getSystemLoadAverage();
    }

    public long getTotalPhysicalMemory() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getTotalPhysicalMemorySize();
        }
        return -1;
    }

    public long getFreePhysicalMemory() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getFreePhysicalMemorySize();
        }
        return -1;
    }
}
