package me.kitakeyos.j2me.domain.emulator.resource;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages resources (threads and sockets) for an emulator instance.
 * This is a Domain concept - each EmulatorInstance owns its resources.
 * Provides thread-safe operations for adding and cleaning up resources.
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
     * Total deadline for all threads to finish gracefully, regardless of thread count.
     * After this deadline, remaining threads are force-stopped.
     */
    private static final long THREAD_CLEANUP_DEADLINE_MS = 1000;

    /**
     * Clean up all threads - interrupt first, then join with shared deadline, force stop as last resort.
     * Sockets are closed BEFORE interrupting threads so that threads blocked on I/O get unblocked.
     * Total wait time is bounded by THREAD_CLEANUP_DEADLINE_MS regardless of thread count.
     */
    @SuppressWarnings("deprecation")
    public void cleanupThreads() {
        logger.info("Cleaning up " + threads.size() + " threads for instance " + instanceId);

        List<Thread> aliveThreads = new ArrayList<>();

        // Phase 1: Interrupt all threads
        for (Thread thread : threads) {
            if (thread.isAlive() && thread != Thread.currentThread()) {
                thread.interrupt();
                aliveThreads.add(thread);
            }
        }

        // Phase 2: Wait with a shared deadline (not per-thread timeout)
        long deadline = System.currentTimeMillis() + THREAD_CLEANUP_DEADLINE_MS;
        for (Thread thread : aliveThreads) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                break;
            }
            try {
                thread.join(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Phase 3: Force stop threads that didn't respond to interrupt
        for (Thread thread : aliveThreads) {
            if (thread.isAlive()) {
                try {
                    thread.stop();
                    logger.warning("Force-stopped unresponsive thread: " + thread.getName());
                } catch (Exception e) {
                    logger.warning("Failed to stop thread " + thread.getName() + ": " + e.getMessage());
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
            if (!socket.isClosed()) {
                try {
                    socket.close();
                } catch (Exception e) {
                    logger.warning("Error closing socket: " + e.getMessage());
                }
            }
        }
        sockets.clear();
    }

    /**
     * Clean up all resources.
     * Sockets are closed FIRST to unblock threads waiting on I/O,
     * then threads are interrupted and joined.
     */
    public void cleanupAll() {
        cleanupSockets();
        cleanupThreads();
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
