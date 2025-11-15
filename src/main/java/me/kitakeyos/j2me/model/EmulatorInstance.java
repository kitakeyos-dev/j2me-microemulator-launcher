package me.kitakeyos.j2me.model;

import me.kitakeyos.j2me.MainApplication;
import me.kitakeyos.j2me.core.classloader.InstanceContext;
import me.kitakeyos.j2me.core.thread.XThread;
import me.kitakeyos.j2me.service.EmulatorInstanceManager;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
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
    public ActionListener menuExitListener;
    public JPanel emulatorDisplay;
    private List<Socket> sockets;
    private List<XThread> threads;

    public EmulatorInstance(int instanceId, String microemulatorPath, String j2meFilePath) {
        this.instanceId = instanceId;
        this.microemulatorPath = microemulatorPath;
        this.j2meFilePath = j2meFilePath;
        this.sockets = new ArrayList<>();
        this.threads = new ArrayList<>();
        this.state = InstanceState.CREATED;
    }

    public boolean canRun() {
        return state == InstanceState.CREATED || state == InstanceState.STOPPED;
    }

    public void addThread(XThread thread) {
        threads.add(thread);
        System.out.println("Add Thread to instance " + instanceId);
    }

    public void addSocket(Socket socket) {
        this.sockets.add(socket);
    }

    /**
     * Shutdown the instance and release all resources for garbage collection
     */
    public void shutdown() {
        if (state == InstanceState.STOPPED) {
            return;
        }
        logger.info("Shutting down instance #" + instanceId + " and releasing resources...");

        // Set state to stopped first
        state = InstanceState.STOPPED;

        EmulatorInstanceManager manager = MainApplication.INSTANCE.emulatorInstanceManager;
        manager.removeInstance(this);

        for (XThread thread : threads) {
            if (thread.isAlive()) {
                try {
                    thread.interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        threads.clear();

        for (Socket socket : sockets) {
            if (!socket.isClosed()) {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        sockets.clear();

        // Trigger emulator exit if running
        if (menuExitListener != null) {
            try {
                menuExitListener.actionPerformed(null);
            } catch (Exception e) {
                logger.warning("Error during menu exit: " + e.getMessage());
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

        // Clear ThreadLocal if this thread is associated with this instance
        try {
            if (InstanceContext.isSet() && InstanceContext.getInstanceId() == this.instanceId) {
                InstanceContext.clear();
            }
        } catch (Exception e) {
            logger.warning("Error clearing InstanceContext: " + e.getMessage());
        }

        // Null out all references to help garbage collector
        emulatorDisplay = null;
        menuExitListener = null;
        errorMessage = null;

        logger.info("Instance #" + instanceId + " resources released");

        // Suggest garbage collection (JVM decides when to actually run it)
        System.gc();
    }
}