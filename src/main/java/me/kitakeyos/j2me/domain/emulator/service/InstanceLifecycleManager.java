package me.kitakeyos.j2me.domain.emulator.service;

import me.kitakeyos.j2me.application.MainApplication;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.resource.ResourceManager;
import me.kitakeyos.j2me.infrastructure.classloader.EmulatorClassLoader;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

/**
 * Manages the lifecycle of emulator instances
 * Handles shutdown, cleanup and resource disposal
 */
public class InstanceLifecycleManager {
    private static final Logger logger = Logger.getLogger(InstanceLifecycleManager.class.getName());

    /**
     * Shutdown an instance and release all its resources
     * This method is idempotent - calling it multiple times on the same instance is
     * safe
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

        // Clean up speed settings
        me.kitakeyos.j2me.domain.speed.service.SpeedService.getInstance()
                .removeInstance(instance.getInstanceId());

        // 1. Trigger emulator exit FIRST so MIDlet can cleanly shut down
        triggerEmulatorExit(instance);

        // 2. Give emulator time to clean up its internal state
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 3. Force-stop any remaining threads and close sockets
        cleanupResources(instance);

        // 4. Remove from instance manager LAST (so XThreads created during exit are still tracked)
        MainApplication.INSTANCE.emulatorInstanceManager.removeInstance(instance);

        // 5. Clean up UI components
        cleanupUIComponents(instance);

        logger.info("Instance #" + instance.getInstanceId() + " shutdown completed");

        // Suggest garbage collection
        System.gc();
    }

    /**
     * Clean up all resources (threads, sockets, and classloader) managed by the
     * instance
     */
    private static void cleanupResources(EmulatorInstance instance) {
        try {
            ResourceManager resourceManager = instance.getResourceManager();
            if (resourceManager != null) {
                resourceManager.cleanupAll();
            }

            // Close EmulatorClassLoader to release JAR handles and loaded classes
            EmulatorClassLoader emulatorClassLoader = instance.getEmulatorClassLoader();
            if (emulatorClassLoader != null) {
                try {
                    emulatorClassLoader.close();
                    logger.info("Closed EmulatorClassLoader for instance #" + instance.getInstanceId());
                } catch (Exception e) {
                    logger.warning("Error closing EmulatorClassLoader: " + e.getMessage());
                }
            }

            // Nullify classloader references to allow GC
            instance.setAppClassLoader(null);
            instance.setEmulatorClassLoader(null);

            // Cleanup transformed JAR file
            java.nio.file.Path transformedJar = instance.getTransformedJarPath();
            if (transformedJar != null) {
                me.kitakeyos.j2me.infrastructure.bytecode.JarTransformer.cleanupTransformedJar(transformedJar);
                instance.setTransformedJarPath(null);
            }

        } catch (Exception e) {
            logger.warning("Error cleaning up resources: " + e.getMessage());
        }
    }

    /**
     * Trigger the emulator's exit listener if available
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
     * Clean up UI components and their references
     */
    private static void cleanupUIComponents(EmulatorInstance instance) {
        JComponent emulatorDisplay = instance.getEmulatorDisplay();
        if (emulatorDisplay != null) {
            try {
                // Remove wrapper panel reference
                emulatorDisplay.putClientProperty("wrapperPanel", null);
                // Remove all components
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
     * Force shutdown an instance even if it's already stopped
     * Use this for cleanup operations that must run regardless of state
     */
    public static void forceShutdown(EmulatorInstance instance) {
        logger.info("Force shutdown instance #" + instance.getInstanceId());
        instance.setState(EmulatorInstance.InstanceState.STOPPED);
        cleanupResources(instance);
        cleanupUIComponents(instance);
    }
}
