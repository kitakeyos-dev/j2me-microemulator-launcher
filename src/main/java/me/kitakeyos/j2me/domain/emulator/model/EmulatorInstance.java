package me.kitakeyos.j2me.domain.emulator.model;

import me.kitakeyos.j2me.domain.emulator.resource.ResourceManager;
import me.kitakeyos.j2me.domain.emulator.service.InstanceLifecycleManager;
import me.kitakeyos.j2me.domain.speed.service.SpeedService;
import me.kitakeyos.j2me.infrastructure.classloader.EmulatorClassLoader;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.net.Socket;

/**
 * Represents an emulator instance with its configuration and state
 * This class now follows proper encapsulation and Single Responsibility
 * Principle
 */
public class EmulatorInstance {

    /**
     * Possible states for an emulator instance
     */
    public enum InstanceState {
        CREATED, // Instance created but not running
        STARTING, // Instance is starting
        RUNNING, // Instance is running
        STOPPED // Instance has been stopped
    }

    // Core configuration (immutable)
    private final int instanceId;
    private final String microemulatorPath;
    private final String j2meFilePath;
    private final int displayWidth;
    private final int displayHeight;
    private final boolean fullDisplayMode;

    // State management
    private InstanceState state;

    // UI components
    private JPanel devicePanel;
    private JComponent emulatorDisplay;
    private ActionListener menuExitListener;
    private ClassLoader appClassLoader;
    private EmulatorClassLoader emulatorClassLoader;
    private java.nio.file.Path transformedJarPath; // Speed control: transformed JAR
    private boolean graphicsEnabled = true; // Graphics optimization flag

    // Resource management
    private final ResourceManager resourceManager;

    public EmulatorInstance(int instanceId, String microemulatorPath, String j2meFilePath, int displayWidth,
            int displayHeight, boolean fullDisplayMode) {
        this.instanceId = instanceId;
        this.microemulatorPath = microemulatorPath;
        this.j2meFilePath = j2meFilePath;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        this.fullDisplayMode = fullDisplayMode;
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

    public boolean isFullDisplayMode() {
        return fullDisplayMode;
    }

    public InstanceState getState() {
        return state;
    }

    public JPanel getDevicePanel() {
        return devicePanel;
    }

    public JComponent getEmulatorDisplay() {
        return emulatorDisplay;
    }

    public ActionListener getMenuExitListener() {
        return menuExitListener;
    }

    public ClassLoader getAppClassLoader() {
        return appClassLoader;
    }

    public EmulatorClassLoader getEmulatorClassLoader() {
        return emulatorClassLoader;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    // === Setters ===

    public void setState(InstanceState state) {
        this.state = state;
    }

    public void setDevicePanel(JPanel devicePanel) {
        this.devicePanel = devicePanel;
    }

    public void setEmulatorDisplay(JComponent emulatorDisplay) {
        this.emulatorDisplay = emulatorDisplay;
    }

    public void setMenuExitListener(ActionListener menuExitListener) {
        this.menuExitListener = menuExitListener;
    }

    public void setAppClassLoader(ClassLoader appClassLoader) {
        this.appClassLoader = appClassLoader;
    }

    public void setEmulatorClassLoader(EmulatorClassLoader emulatorClassLoader) {
        this.emulatorClassLoader = emulatorClassLoader;
    }

    public java.nio.file.Path getTransformedJarPath() {
        return transformedJarPath;
    }

    public void setTransformedJarPath(java.nio.file.Path transformedJarPath) {
        this.transformedJarPath = transformedJarPath;
    }

    // === Speed Control ===

    /**
     * Get the current speed multiplier for this instance.
     * 
     * @return Speed multiplier (1.0 = normal, 2.0 = 2x faster)
     */
    public double getSpeedMultiplier() {
        return SpeedService.getInstance()
                .getSpeedMultiplier(instanceId);
    }

    /**
     * Set speed multiplier for this instance.
     * Takes effect immediately for Thread.sleep() calls.
     * 
     * @param multiplier Speed multiplier (1.0 = normal, 2.0 = 2x faster)
     */
    public void setSpeedMultiplier(double multiplier) {
        SpeedService.getInstance()
                .setSpeedMultiplier(instanceId, multiplier);
    }

    // === Graphics Optimization ===
    public boolean isGraphicsEnabled() {
        return graphicsEnabled;
    }

    public void setGraphicsEnabled(boolean graphicsEnabled) {
        this.graphicsEnabled = graphicsEnabled;
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
     */
    public void addThread(Thread thread) {
        resourceManager.addThread(thread);
    }

    /**
     * Remove a thread from this instance's resource manager
     */
    public void removeThread(Thread thread) {
        resourceManager.removeThread(thread);
    }

    /**
     * Add a socket to this instance's resource manager
     */
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
