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

    public EmulatorInstanceManager(JPanel instancesPanel) {
        this.instances = new ArrayList<>();
        this.instancesPanel = instancesPanel;
        this.idPool = new InstanceIdPool();
    }

    public void addInstance(EmulatorInstance instance) {
        instances.add(instance);
    }

    public void removeInstance(EmulatorInstance instance) {
        instances.remove(instance);

        // Return the ID back to pool for reuse
        idPool.releaseId(instance.getInstanceId());

        if (instance.getUIPanel() != null) {
            instancesPanel.remove(instance.getUIPanel());
        }
    }

    public void moveInstanceUp(EmulatorInstance instance) {
        int currentIndex = instances.indexOf(instance);
        if (currentIndex > 0) {
            Collections.swap(instances, currentIndex, currentIndex - 1);
            updatePanelOrder();
        }
    }

    public void moveInstanceDown(EmulatorInstance instance) {
        int currentIndex = instances.indexOf(instance);
        if (currentIndex < instances.size() - 1) {
            Collections.swap(instances, currentIndex, currentIndex + 1);
            updatePanelOrder();
        }
    }

    public void updatePanelOrder() {
        instancesPanel.removeAll();
        for (EmulatorInstance instance : instances) {
            if (instance.getUIPanel() != null) {
                instancesPanel.add(instance.getUIPanel());
            }
        }
        instancesPanel.revalidate();
        instancesPanel.repaint();
    }

    public void stopAllInstances() {
        for (EmulatorInstance instance : new ArrayList<>(instances)) {
            if (instance.getState() == InstanceState.RUNNING) {
                instance.shutdown();
            }
        }
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
}
