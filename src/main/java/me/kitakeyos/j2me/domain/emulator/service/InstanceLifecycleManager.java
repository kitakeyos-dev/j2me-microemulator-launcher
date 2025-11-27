package me.kitakeyos.j2me.domain.emulator.service;

import me.kitakeyos.j2me.application.MainApplication;
import me.kitakeyos.j2me.infrastructure.classloader.InstanceContext;
import me.kitakeyos.j2me.infrastructure.resource.ResourceManager;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;

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
     * This method is idempotent - calling it multiple times on the same instance is safe
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

        MainApplication.INSTANCE.emulatorInstanceManager.removeInstance(instance);

        // Clean up managed resources (threads and sockets)
        cleanupResources(instance);

        // Trigger emulator exit
        triggerEmulatorExit(instance);

        // Clean up UI components
        cleanupUIComponents(instance);

        // Clear ThreadLocal context if needed
        clearThreadLocalContext(instance);

        logger.info("Instance #" + instance.getInstanceId() + " shutdown completed");

        // Suggest garbage collection
        System.gc();
    }

    /**
     * Clean up all resources (threads and sockets) managed by the instance
     */
    private static void cleanupResources(EmulatorInstance instance) {
        try {
            ResourceManager resourceManager = instance.getResourceManager();
            if (resourceManager != null) {
                resourceManager.cleanupAll();
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
     * Clear ThreadLocal context if this thread is associated with this instance
     */
    private static void clearThreadLocalContext(EmulatorInstance instance) {
        try {
            if (InstanceContext.isSet() && InstanceContext.getInstanceId() == instance.getInstanceId()) {
                InstanceContext.clear();
                logger.fine("ThreadLocal context cleared for instance #" + instance.getInstanceId());
            }
        } catch (Exception e) {
            logger.warning("Error clearing InstanceContext: " + e.getMessage());
        }
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
        clearThreadLocalContext(instance);
    }
}
