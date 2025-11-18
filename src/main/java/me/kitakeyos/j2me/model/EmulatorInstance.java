package me.kitakeyos.j2me.model;

import me.kitakeyos.j2me.core.lifecycle.InstanceLifecycleManager;
import me.kitakeyos.j2me.core.resource.ResourceManager;
import me.kitakeyos.j2me.core.thread.XThread;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.net.Socket;

/**
 * Represents an emulator instance with its configuration and state
 * This class now follows proper encapsulation and Single Responsibility Principle
 */
public class EmulatorInstance {

    /**
     * Possible states for an emulator instance
     */
    public enum InstanceState {
        CREATED,    // Instance created but not running
        STARTING,   // Instance is starting
        RUNNING,    // Instance is running
        STOPPED     // Instance has been stopped
    }

    // Core configuration (immutable)
    private final int instanceId;
    private final String microemulatorPath;
    private final String j2meFilePath;
    private final int displayWidth;
    private final int displayHeight;

    // State management
    private InstanceState state;
    private String errorMessage;

    // UI components
    private JPanel uiPanel;
    private JPanel emulatorDisplay;
    private ActionListener menuExitListener;

    // Resource management
    private final ResourceManager resourceManager;

    public EmulatorInstance(int instanceId, String microemulatorPath, String j2meFilePath) {
        this(instanceId, microemulatorPath, j2meFilePath, 240, 320);
    }

    public EmulatorInstance(int instanceId, String microemulatorPath, String j2meFilePath, int displayWidth, int displayHeight) {
        this.instanceId = instanceId;
        this.microemulatorPath = microemulatorPath;
        this.j2meFilePath = j2meFilePath;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        this.state = InstanceState.CREATED;
        this.resourceManager = new ResourceManager(instanceId);
    }

    // === Getters ===

    public int getInstanceId() {
        return instanceId;
    }

    public String getMicroemulatorPath() {
        return microemulatorPath;
    }

    public String getJ2meFilePath() {
        return j2meFilePath;
    }

    public int getDisplayWidth() {
        return displayWidth;
    }

    public int getDisplayHeight() {
        return displayHeight;
    }

    public InstanceState getState() {
        return state;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public JPanel getUIPanel() {
        return uiPanel;
    }

    public JPanel getEmulatorDisplay() {
        return emulatorDisplay;
    }

    public ActionListener getMenuExitListener() {
        return menuExitListener;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    // === Setters ===

    public void setState(InstanceState state) {
        this.state = state;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setUIPanel(JPanel uiPanel) {
        this.uiPanel = uiPanel;
    }

    public void setEmulatorDisplay(JPanel emulatorDisplay) {
        this.emulatorDisplay = emulatorDisplay;
    }

    public void setMenuExitListener(ActionListener menuExitListener) {
        this.menuExitListener = menuExitListener;
    }

    // === Business Logic ===

    /**
     * Check if this instance can be run
     */
    public boolean canRun() {
        return state == InstanceState.CREATED || state == InstanceState.STOPPED;
    }

    /**
     * Add a thread to this instance's resource manager
     * Deprecated: Use getResourceManager().addThread() instead
     */
    @Deprecated
    public void addThread(XThread thread) {
        resourceManager.addThread(thread);
    }

    /**
     * Add a socket to this instance's resource manager
     * Deprecated: Use getResourceManager().addSocket() instead
     */
    @Deprecated
    public void addSocket(Socket socket) {
        resourceManager.addSocket(socket);
    }

    /**
     * Shutdown the instance and release all resources
     * Delegates to InstanceLifecycleManager for proper cleanup
     */
    public void shutdown() {
        InstanceLifecycleManager.shutdown(this);
    }

    @Override
    public String toString() {
        return String.format("EmulatorInstance{id=%d, state=%s, path=%s}",
            instanceId, state, j2meFilePath);
    }
}
