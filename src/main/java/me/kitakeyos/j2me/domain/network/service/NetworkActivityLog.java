package me.kitakeyos.j2me.domain.network.service;

import me.kitakeyos.j2me.domain.network.model.ConnectionLog;
import me.kitakeyos.j2me.domain.network.model.PacketLog;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded history of connection attempts and captured packets, plus the
 * running byte totals derived from them.
 * <p>
 * Totals are kept as atomics because packets are logged from MIDlet threads
 * while the monitor UI reads them from the EDT.
 */
public class NetworkActivityLog {

    private static final int MAX_LOG_SIZE = 1000;
    private static final int MAX_PACKET_LOG_SIZE = 5000;

    private final List<ConnectionLog> connectionLogs = new CopyOnWriteArrayList<>();
    private final List<PacketLog> packetLogs = new CopyOnWriteArrayList<>();

    private final AtomicLong totalBytesSent = new AtomicLong();
    private final AtomicLong totalBytesReceived = new AtomicLong();

    private final NetworkEvents events;

    public NetworkActivityLog(NetworkEvents events) {
        this.events = events;
    }

    // === Connection logs ===

    public void addConnectionLog(ConnectionLog log) {
        connectionLogs.add(log);
        trim(connectionLogs, MAX_LOG_SIZE);
        events.fireLogAdded(log);
    }

    public List<ConnectionLog> getConnectionLogs() {
        return Collections.unmodifiableList(connectionLogs);
    }

    public void clearConnectionLogs() {
        connectionLogs.clear();
        events.fireLogsCleared();
    }

    // === Packet logs ===

    /**
     * Record a captured packet and fold its length into the running totals.
     * Callers are responsible for checking that capture is enabled.
     */
    public void addPacketLog(PacketLog log) {
        packetLogs.add(log);
        counterFor(log).addAndGet(log.getLength());
        trim(packetLogs, MAX_PACKET_LOG_SIZE);
        events.firePacketLogAdded(log);
    }

    public List<PacketLog> getPacketLogs() {
        return Collections.unmodifiableList(packetLogs);
    }

    public void clearPacketLogs() {
        packetLogs.clear();
        totalBytesSent.set(0);
        totalBytesReceived.set(0);
        events.firePacketLogsCleared();
    }

    public long getTotalBytesSent() {
        return totalBytesSent.get();
    }

    public long getTotalBytesReceived() {
        return totalBytesReceived.get();
    }

    /**
     * Forget everything recorded for one instance, rolling its bytes back out
     * of the totals so the displayed figures stay consistent.
     */
    public void removeInstanceData(int instanceId) {
        connectionLogs.removeIf(log -> log.getInstanceId() == instanceId);

        for (PacketLog log : packetLogs) {
            if (log.getInstanceId() == instanceId) {
                counterFor(log).addAndGet(-log.getLength());
            }
        }
        packetLogs.removeIf(log -> log.getInstanceId() == instanceId);
    }

    private AtomicLong counterFor(PacketLog log) {
        return log.getDirection() == PacketLog.Direction.OUT ? totalBytesSent : totalBytesReceived;
    }

    private static void trim(List<?> log, int maxSize) {
        while (log.size() > maxSize) {
            log.remove(0);
        }
    }
}
