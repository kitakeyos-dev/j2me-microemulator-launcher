package me.kitakeyos.j2me.infrastructure.monitoring;

import me.kitakeyos.j2me.domain.monitoring.SystemMetrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

/**
 * Service to retrieve system performance metrics
 */
@SuppressWarnings("restriction")
public class SystemMonitorService implements SystemMetrics {

    private final MemoryMXBean memoryBean;
    private final OperatingSystemMXBean osBean;
    private final ThreadMXBean threadBean;

    public SystemMonitorService() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
    }

    @Override
    public long getUsedHeapMemory() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return heapUsage.getUsed();
    }

    @Override
    public long getMaxHeapMemory() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return heapUsage.getMax();
    }

    @Override
    public long getUsedNonHeapMemory() {
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        return nonHeapUsage.getUsed();
    }

    @Override
    public double getSystemCpuLoad() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad() * 100;
        }
        return -1;
    }

    @Override
    public double getProcessCpuLoad() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad() * 100;
        }
        return -1;
    }

    @Override
    public int getThreadCount() {
        return threadBean.getThreadCount();
    }

    @Override
    public double getSystemLoadAverage() {
        return osBean.getSystemLoadAverage();
    }

    @Override
    public long getTotalPhysicalMemory() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getTotalPhysicalMemorySize();
        }
        return -1;
    }

    @Override
    public long getFreePhysicalMemory() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getFreePhysicalMemorySize();
        }
        return -1;
    }
}
