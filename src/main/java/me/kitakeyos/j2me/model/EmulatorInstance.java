package me.kitakeyos.j2me.model;

import me.kitakeyos.j2me.core.classloader.InstanceContext;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

/**
 * Configuration for an emulator instance
 */
public class EmulatorInstance {
    private static final Logger logger = Logger.getLogger(EmulatorInstance.class.getName());

    public enum InstanceState {
        CREATED,    // Instance created but not running
        STARTING,   // Instance is starting
        RUNNING,    // Instance is running
        STOPPED     // Instance has been stopped
    }

    public int instanceId;
    public String microemulatorPath;
    public String j2meFilePath;
    public InstanceState state;
    public String errorMessage;
    public JPanel uiPanel;
    public JFrame emulatorWindow;
    public ActionListener menuExitListener;
    public JPanel emulatorDisplay;

    public EmulatorInstance(int instanceId, String microemulatorPath, String j2meFilePath) {
        this.instanceId = instanceId;
        this.microemulatorPath = microemulatorPath;
        this.j2meFilePath = j2meFilePath;
        this.state = InstanceState.CREATED;
    }

    public boolean canRun() {
        return state == InstanceState.CREATED || state == InstanceState.STOPPED;
    }

    /**
     * Shutdown the instance and release all resources for garbage collection
     */
    public void shutdown() {
        logger.info("Shutting down instance #" + instanceId + " and releasing resources...");

        // Set state to stopped first
        state = InstanceState.STOPPED;

        // Trigger emulator exit if running
        if (menuExitListener != null) {
            try {
                menuExitListener.actionPerformed(null);
            } catch (Exception e) {
                logger.warning("Error during menu exit: " + e.getMessage());
            }
        }

        // Dispose window to release native resources
        if (emulatorWindow != null) {
            try {
                emulatorWindow.setVisible(false);
                emulatorWindow.dispose();
            } catch (Exception e) {
                logger.warning("Error disposing window: " + e.getMessage());
            }
        }

        // Clear client properties from display panel
        if (emulatorDisplay != null) {
            try {
                // Remove wrapper panel reference
                emulatorDisplay.putClientProperty("wrapperPanel", null);
                // Remove all components
                emulatorDisplay.removeAll();
            } catch (Exception e) {
                logger.warning("Error cleaning display panel: " + e.getMessage());
            }
        }

        // Clear UI panel
        if (uiPanel != null) {
            try {
                uiPanel.removeAll();
            } catch (Exception e) {
                logger.warning("Error cleaning UI panel: " + e.getMessage());
            }
        }

        // Clear ThreadLocal if this thread is associated with this instance
        try {
            if (InstanceContext.isSet() && InstanceContext.getInstanceId() == this.instanceId) {
                InstanceContext.clear();
            }
        } catch (Exception e) {
            logger.warning("Error clearing InstanceContext: " + e.getMessage());
        }

        // Null out all references to help garbage collector
        emulatorWindow = null;
        emulatorDisplay = null;
        menuExitListener = null;
        uiPanel = null;
        errorMessage = null;

        logger.info("Instance #" + instanceId + " resources released");

        // Suggest garbage collection (JVM decides when to actually run it)
        System.gc();
    }
}