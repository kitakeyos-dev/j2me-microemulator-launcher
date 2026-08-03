package me.kitakeyos.j2me.domain.network.service;

import me.kitakeyos.j2me.domain.network.model.SocketTap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Tracks which instances have packet capture switched on and owns the live
 * {@link SocketTap} per socket.
 * <p>
 * Capture is off by default: taps are only created for instances the user has
 * explicitly enabled, so an untapped session pays nothing.
 */
public class SocketTapRegistry {

    private static final Logger logger = Logger.getLogger(SocketTapRegistry.class.getName());

    private final Map<Integer, SocketTap> socketTaps = new ConcurrentHashMap<>();
    private final Set<Integer> tappingEnabledInstances = ConcurrentHashMap.newKeySet();

    /**
     * Enable packet logging and stream tapping for an instance.
     * Must be called before any data will be captured.
     */
    public void enableTapping(int instanceId) {
        tappingEnabledInstances.add(instanceId);
        logger.info("Tapping enabled for instance #" + instanceId);
    }

    /**
     * Disable tapping for an instance and close all of its taps.
     */
    public void disableTapping(int instanceId) {
        tappingEnabledInstances.remove(instanceId);
        closeTapsOf(instanceId);
        logger.info("Tapping disabled for instance #" + instanceId);
    }

    public boolean isTappingEnabled(int instanceId) {
        return tappingEnabledInstances.contains(instanceId);
    }

    /**
     * Get or create the tap for a socket.
     *
     * @return the tap, or {@code null} when tapping is disabled for the instance
     */
    public SocketTap getOrCreateTap(int socketId, int instanceId, String host, int port) {
        if (!isTappingEnabled(instanceId)) {
            return null;
        }
        return socketTaps.computeIfAbsent(socketId, k -> new SocketTap(socketId, instanceId, host, port));
    }

    /**
     * @return the tap for this socket, or {@code null} if none exists
     */
    public SocketTap getTap(int socketId) {
        return socketTaps.get(socketId);
    }

    public List<SocketTap> getTapsByInstance(int instanceId) {
        List<SocketTap> result = new ArrayList<>();
        for (SocketTap tap : socketTaps.values()) {
            if (tap.getInstanceId() == instanceId) {
                result.add(tap);
            }
        }
        return result;
    }

    public Map<Integer, SocketTap> getAllTaps() {
        return Collections.unmodifiableMap(socketTaps);
    }

    /**
     * Remove and close the tap for a specific socket.
     */
    public void removeTap(int socketId) {
        SocketTap tap = socketTaps.remove(socketId);
        if (tap != null) {
            tap.close();
        }
    }

    /**
     * Close every tap belonging to an instance. Called on shutdown to keep
     * stopped instances from leaking their buffers.
     */
    public void closeTapsOf(int instanceId) {
        List<Integer> toRemove = new ArrayList<>();
        for (Map.Entry<Integer, SocketTap> entry : socketTaps.entrySet()) {
            if (entry.getValue().getInstanceId() == instanceId) {
                toRemove.add(entry.getKey());
            }
        }
        for (int socketId : toRemove) {
            removeTap(socketId);
        }
    }
}
