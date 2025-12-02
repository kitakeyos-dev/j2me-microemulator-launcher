package me.kitakeyos.j2me.infrastructure.resource;

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
    private final List<Thread> threads;
    private final List<Socket> sockets;

    public ResourceManager(int instanceId) {
        this.instanceId = instanceId;
        this.threads = Collections.synchronizedList(new ArrayList<>());
        this.sockets = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Add a thread to be managed by this instance
     */
    public void addThread(Thread thread) {
        threads.add(thread);
        logger.info("Added thread to instance " + instanceId + ": " + thread.getName());
    }

    /**
     * Remove a thread from be managed by this instance
     */
    public void removeThread(Thread thread) {
        threads.remove(thread);
        logger.info("Removed thread from instance " + instanceId + ": " + thread.getName());
    }

    /**
     * Add a socket to be managed by this instance
     */
    public void addSocket(Socket socket) {
        sockets.add(socket);
        logger.info("Added socket to instance " + instanceId);
    }

    /**
     * Get all managed threads
     */
    public List<Thread> getThreads() {
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

        for (Thread thread : threads) {
            if (thread.isAlive() && thread != Thread.currentThread()) {
                try {
                    thread.stop();
                    logger.info("Stopped thread: " + thread.getName());
                } catch (Exception e) {
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
                    logger.info("Closed socket: " + socket);
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
