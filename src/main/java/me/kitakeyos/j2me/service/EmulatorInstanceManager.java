package me.kitakeyos.j2me.service;

import me.kitakeyos.j2me.model.EmulatorInstance;
import me.kitakeyos.j2me.model.EmulatorInstance.InstanceState;
import me.kitakeyos.j2me.util.InstanceIdPool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;

/**
 * Manages emulator instances with ID pool for efficient ID reuse
 * Refactored to use separate InstanceIdPool class
 */
public class EmulatorInstanceManager {
    private final List<EmulatorInstance> instances;
    private final JPanel instancesPanel;
    private final InstanceIdPool idPool;
    private final InputSynchronizer inputSynchronizer;

    public EmulatorInstanceManager(JPanel instancesPanel) {
        this.instances = new ArrayList<>();
        this.instancesPanel = instancesPanel;
        this.idPool = new InstanceIdPool();
        this.inputSynchronizer = new InputSynchronizer(this);
    }

    public void addInstance(EmulatorInstance instance) {
        instances.add(instance);
    }

    public void removeInstance(EmulatorInstance instance) {
        instances.remove(instance);

        // Return the ID back to pool for reuse
        idPool.releaseId(instance.getInstanceId());
    }

    public void clearAllInstances() {
        for (EmulatorInstance instance : new ArrayList<>(instances)) {
            if (instance.getState() == InstanceState.RUNNING) {
                instance.shutdown();
            }
            // Release all IDs back to pool
            idPool.releaseId(instance.getInstanceId());
        }
        instances.clear();
        instancesPanel.removeAll();
        instancesPanel.revalidate();
        instancesPanel.repaint();
    }

    public List<EmulatorInstance> getInstances() {
        return instances;
    }

    public List<EmulatorInstance> getRunningInstances() {
        List<EmulatorInstance> running = new ArrayList<>();
        for (EmulatorInstance instance : instances) {
            if (instance.getState() == InstanceState.RUNNING) {
                running.add(instance);
            }
        }
        return running;
    }

    public List<EmulatorInstance> getRunnableInstances() {
        List<EmulatorInstance> runnable = new ArrayList<>();
        for (EmulatorInstance instance : instances) {
            if (instance.canRun()) {
                runnable.add(instance);
            }
        }
        return runnable;
    }

    public EmulatorInstance findInstance(int instanceId) {
        for (EmulatorInstance instance : instances) {
            if (instance.getInstanceId() == instanceId) {
                return instance;
            }
        }
        return null;
    }

    public int getInstanceCount() {
        return instances.size();
    }

    /**
     * Get next available instance ID from the pool
     * @return Next available ID
     */
    public int getNextInstanceId() {
        return idPool.acquireId();
    }

    /**
     * Reset the ID pool (useful for testing or cleanup)
     */
    public void resetInstanceIdCounter() {
        idPool.reset();
    }

    /**
     * Get ID pool statistics
     * @return Statistics string
     */
    public String getIdPoolStatistics() {
        return idPool.getStatistics();
    }

    /**
     * Get the input synchronizer
     * @return InputSynchronizer instance
     */
    public InputSynchronizer getInputSynchronizer() {
        return inputSynchronizer;
    }

    /**
     * Enable or disable input synchronization across all instances
     * @param enabled true to enable, false to disable
     */
    public void setInputSynchronizationEnabled(boolean enabled) {
        inputSynchronizer.setEnabled(enabled);
    }

    /**
     * Check if input synchronization is enabled
     * @return true if enabled, false otherwise
     */
    public boolean isInputSynchronizationEnabled() {
        return inputSynchronizer.isEnabled();
    }

    /**
     * Enable or disable input scaling by device panel size
     * @param scaleBySize true to enable, false to disable
     */
    public void setInputScaleBySize(boolean scaleBySize) {
        inputSynchronizer.setScaleBySize(scaleBySize);
    }

    /**
     * Check if input scaling by size is enabled
     * @return true if enabled, false otherwise
     */
    public boolean isInputScaleBySize() {
        return inputSynchronizer.isScaleBySize();
    }

    /**
     * Notify the input synchronizer that an instance has been started
     * Should be called after instance display is ready
     */
    public void notifyInstanceStarted(EmulatorInstance instance) {
        if (inputSynchronizer.isEnabled()) {
            inputSynchronizer.attachListenersToInstance(instance);
        }
    }

    /**
     * Notify the input synchronizer that an instance is being removed
     * Should be called before instance is removed
     */
    public void notifyInstanceStopping(EmulatorInstance instance) {
        if (inputSynchronizer.isEnabled()) {
            inputSynchronizer.detachListenersFromInstance(instance);
        }
    }
}
