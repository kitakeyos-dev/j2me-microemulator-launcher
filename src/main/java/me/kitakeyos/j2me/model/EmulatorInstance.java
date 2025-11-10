package me.kitakeyos.j2me.model;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * Configuration for an emulator instance
 */
public class EmulatorInstance {
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

    public boolean isRunning() {
        return state == InstanceState.RUNNING;
    }

    public boolean canRun() {
        return state == InstanceState.CREATED || state == InstanceState.STOPPED;
    }

    public void shutdown() {
        state = InstanceState.STOPPED;
        if (menuExitListener != null) {
            menuExitListener.actionPerformed(null);
        }
    }
}