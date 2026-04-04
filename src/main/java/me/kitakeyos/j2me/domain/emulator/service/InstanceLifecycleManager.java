package me.kitakeyos.j2me.domain.emulator.service;

import me.kitakeyos.j2me.application.MainApplication;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.resource.ResourceManager;
import me.kitakeyos.j2me.domain.graphics.service.GraphicsOptimizationService;
import me.kitakeyos.j2me.domain.network.service.NetworkService;
import me.kitakeyos.j2me.domain.speed.service.SpeedService;
import me.kitakeyos.j2me.infrastructure.classloader.EmulatorClassLoader;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

/**
 * Manages the lifecycle of emulator instances.
 * Handles shutdown, cleanup and resource disposal.
 * All cleanup steps use try-finally to ensure every step runs even if earlier steps fail.
 */
public class InstanceLifecycleManager {
    private static final Logger logger = Logger.getLogger(InstanceLifecycleManager.class.getName());

    /**
     * Shutdown an instance and release all its resources.
     * This method is idempotent - calling it multiple times on the same instance is safe.
     * Uses try-finally chain to guarantee all cleanup steps execute.
     *
     * @param instance The instance to shutdown
     */
    public static void shutdown(EmulatorInstance instance) {
        if (instance.getState() == EmulatorInstance.InstanceState.STOPPED) {
            logger.fine("Instance #" + instance.getInstanceId() + " already stopped, skipping shutdown");
            return;
        }

        logger.info("Shutting down instance #" + instance.getInstanceId() + " and releasing resources...");

        // Set state to stopped first to prevent concurrent shutdowns
        instance.setState(EmulatorInstance.InstanceState.STOPPED);

        try {
            // 1. Restore graphics proxy BEFORE classloader is closed
            cleanupGraphics(instance);
        } finally {
            try {
                // 2. Clean up speed settings
                SpeedService.getInstance().removeInstance(instance.getInstanceId());
            } finally {
                try {
                    // 3. Trigger emulator exit so MIDlet can cleanly shut down
                    triggerEmulatorExit(instance);

                    // Give emulator time to clean up its internal state
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } finally {
                    try {
                        // 4. Force-stop any remaining threads and close sockets
                        cleanupResources(instance);
                    } finally {
                        try {
                            // 5. Clean up network data for this instance
                            cleanupNetwork(instance);
                        } finally {
                            try {
                                // 6. Remove from instance manager (so XThreads created during exit are still tracked)
                                MainApplication.INSTANCE.emulatorInstanceManager.removeInstance(instance);
                            } finally {
                                try {
                                    // 7. Clean up UI components
                                    cleanupUIComponents(instance);
                                } finally {
                                    // 8. Clear bytecode cache if no more instances are running
                                    evictCacheIfEmpty();

                                    logger.info("Instance #" + instance.getInstanceId() + " shutdown completed");
                                    System.gc();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Clean up all resources (threads, sockets, and classloader) managed by the instance.
     */
    private static void cleanupResources(EmulatorInstance instance) {
        try {
            ResourceManager resourceManager = instance.getResourceManager();
            if (resourceManager != null) {
                resourceManager.cleanupAll();
            }
        } catch (Exception e) {
            logger.warning("Error cleaning up managed resources: " + e.getMessage());
        }

        try {
            // Close EmulatorClassLoader to release JAR handles and loaded classes
            EmulatorClassLoader emulatorClassLoader = instance.getEmulatorClassLoader();
            if (emulatorClassLoader != null) {
                emulatorClassLoader.close();
                logger.info("Closed EmulatorClassLoader for instance #" + instance.getInstanceId());
            }
        } catch (Exception e) {
            logger.warning("Error closing EmulatorClassLoader: " + e.getMessage());
        }

        // Nullify classloader references to allow GC
        instance.setAppClassLoader(null);
        instance.setEmulatorClassLoader(null);

        try {
            // Cleanup transformed JAR file
            java.nio.file.Path transformedJar = instance.getTransformedJarPath();
            if (transformedJar != null) {
                me.kitakeyos.j2me.infrastructure.bytecode.JarTransformer.cleanupTransformedJar(transformedJar);
                instance.setTransformedJarPath(null);
            }
        } catch (Exception e) {
            logger.warning("Error cleaning up transformed JAR: " + e.getMessage());
        }
    }

    /**
     * Restore original DisplayAccess by removing graphics proxy.
     * Must be called BEFORE classloader is closed.
     */
    private static void cleanupGraphics(EmulatorInstance instance) {
        try {
            GraphicsOptimizationService.getInstance().removeInstance(instance);
        } catch (Exception e) {
            logger.warning("Error cleaning up graphics for instance #" + instance.getInstanceId()
                    + ": " + e.getMessage());
        }
    }

    /**
     * Remove network logs and data pools for this instance.
     */
    private static void cleanupNetwork(EmulatorInstance instance) {
        try {
            NetworkService.getInstance().removeInstanceData(instance.getInstanceId());
        } catch (Exception e) {
            logger.warning("Error cleaning up network data for instance #" + instance.getInstanceId()
                    + ": " + e.getMessage());
        }
    }

    /**
     * Clear shared bytecode cache when no instances are running.
     */
    private static void evictCacheIfEmpty() {
        try {
            InstanceManager manager = MainApplication.INSTANCE.emulatorInstanceManager;
            if (manager.getRunningInstances().isEmpty()) {
                EmulatorClassLoader.clearSharedCache();
            }
        } catch (Exception e) {
            logger.warning("Error evicting bytecode cache: " + e.getMessage());
        }
    }

    /**
     * Trigger the emulator's exit listener if available.
     */
    private static void triggerEmulatorExit(EmulatorInstance instance) {
        ActionListener exitListener = instance.getMenuExitListener();
        if (exitListener != null) {
            try {
                exitListener.actionPerformed(null);
                logger.fine("Emulator exit listener triggered for instance #" + instance.getInstanceId());
            } catch (Exception e) {
                logger.warning("Error during menu exit: " + e.getMessage());
            }
        }
    }

    /**
     * Clean up UI components and their references.
     */
    private static void cleanupUIComponents(EmulatorInstance instance) {
        JComponent emulatorDisplay = instance.getEmulatorDisplay();
        if (emulatorDisplay != null) {
            try {
                emulatorDisplay.putClientProperty("wrapperPanel", null);
                emulatorDisplay.removeAll();
                logger.fine("UI components cleaned for instance #" + instance.getInstanceId());
            } catch (Exception e) {
                logger.warning("Error cleaning display panel: " + e.getMessage());
            }
        }

        // Clear UI references
        instance.setEmulatorDisplay(null);
    }

    /**
     * Force shutdown an instance even if it's already stopped.
     * Use this for cleanup operations that must run regardless of state.
     */
    public static void forceShutdown(EmulatorInstance instance) {
        logger.info("Force shutdown instance #" + instance.getInstanceId());
        instance.setState(EmulatorInstance.InstanceState.STOPPED);
        cleanupGraphics(instance);
        cleanupResources(instance);
        cleanupNetwork(instance);
        cleanupUIComponents(instance);
        evictCacheIfEmpty();
    }
}
