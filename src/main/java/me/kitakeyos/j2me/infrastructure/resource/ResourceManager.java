package me.kitakeyos.j2me.infrastructure.resource;

import me.kitakeyos.j2me.infrastructure.thread.XThread;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages resources (threads and sockets) for an emulator instance
 * Provides thread-safe operations for adding and cleaning up resources
 */
public class ResourceManager {
    private static final Logger logger = Logger.getLogger(ResourceManager.class.getName());

    private final int instanceId;
    private final List<XThread> threads;
    private final List<Socket> sockets;

    public ResourceManager(int instanceId) {
        this.instanceId = instanceId;
        this.threads = Collections.synchronizedList(new ArrayList<>());
        this.sockets = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Add a thread to be managed by this instance
     */
    public void addThread(XThread thread) {
        threads.add(thread);
        logger.fine("Added thread to instance " + instanceId + ": " + thread.getName());
    }

    /**
     * Add a socket to be managed by this instance
     */
    public void addSocket(Socket socket) {
        sockets.add(socket);
        logger.fine("Added socket to instance " + instanceId);
    }

    /**
     * Get all managed threads
     */
    public List<XThread> getThreads() {
        return new ArrayList<>(threads);
    }

    /**
     * Get all managed sockets
     */
    public List<Socket> getSockets() {
        return new ArrayList<>(sockets);
    }

    /**
     * Clean up all threads - interrupt and clear
     */
    public void cleanupThreads() {
        logger.info("Cleaning up " + threads.size() + " threads for instance " + instanceId);

        for (XThread thread : threads) {
            if (thread.isAlive()) {
                try {
                    thread.interrupt();
                    logger.fine("Interrupted thread: " + thread.getName());
                } catch (Exception e) {
                    logger.warning("Error interrupting thread: " + e.getMessage());
                }
            }
        }
        threads.clear();
    }

    /**
     * Clean up all sockets - close and clear
     */
    public void cleanupSockets() {
        logger.info("Cleaning up " + sockets.size() + " sockets for instance " + instanceId);

        for (Socket socket : sockets) {
            if (socket.isConnected() && !socket.isClosed()) {
                try {
                    socket.close();
                    logger.fine("Closed socket: " + socket);
                } catch (Exception e) {
                    logger.warning("Error closing socket: " + e.getMessage());
                }
            }
        }
        sockets.clear();
    }

    /**
     * Clean up all resources (threads and sockets)
     */
    public void cleanupAll() {
        cleanupThreads();
        cleanupSockets();
        logger.info("All resources cleaned up for instance " + instanceId);
    }

    /**
     * Get resource statistics
     */
    public String getStatistics() {
        return String.format("Instance #%d Resources: %d threads, %d sockets",
            instanceId, threads.size(), sockets.size());
    }
}
