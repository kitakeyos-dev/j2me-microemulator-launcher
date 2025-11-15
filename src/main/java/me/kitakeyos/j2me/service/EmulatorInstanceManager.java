package me.kitakeyos.j2me.service;

import me.kitakeyos.j2me.model.EmulatorInstance;
import me.kitakeyos.j2me.model.EmulatorInstance.InstanceState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import javax.swing.JPanel;

/**
 * Manages emulator instances with ID pool for efficient ID reuse
 */
public class EmulatorInstanceManager {
    private final List<EmulatorInstance> instances;
    private final JPanel instancesPanel;
    private final IdPool idPool;

    public EmulatorInstanceManager(JPanel instancesPanel) {
        this.instances = new ArrayList<>();
        this.instancesPanel = instancesPanel;
        this.idPool = new IdPool();
    }

    public void addInstance(EmulatorInstance instance) {
        instances.add(instance);
    }

    public void removeInstance(EmulatorInstance instance) {
        instances.remove(instance);

        // Return the ID back to pool for reuse
        idPool.releaseId(instance.instanceId);

        if (instance.uiPanel != null) {
            instancesPanel.remove(instance.uiPanel);
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
            if (instance.uiPanel != null) {
                instancesPanel.add(instance.uiPanel);
            }
        }
        instancesPanel.revalidate();
        instancesPanel.repaint();
    }

    public void stopAllInstances() {
        for (EmulatorInstance instance : new ArrayList<>(instances)) {
            if (instance.state == InstanceState.RUNNING) {
                instance.shutdown();
            }
        }
    }

    public void clearAllInstances() {
        for (EmulatorInstance instance : new ArrayList<>(instances)) {
            if (instance.state == InstanceState.RUNNING) {
                instance.shutdown();
            }
            // Release all IDs back to pool
            idPool.releaseId(instance.instanceId);
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
            if (instance.state == InstanceState.RUNNING) {
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
            if (instance.instanceId == instanceId) {
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
     * ID Pool for managing instance IDs with reuse capability
     */
    private static class IdPool {
        // Min heap to keep track of available IDs in sorted order
        private final PriorityQueue<Integer> availableIds;
        // Track the next new ID to generate when pool is empty
        private int nextNewId;

        public IdPool() {
            this.availableIds = new PriorityQueue<>();
            this.nextNewId = 1;
        }

        /**
         * Acquire an ID from the pool
         * Returns the smallest available ID, or generates a new one if pool is empty
         * @return Available instance ID
         */
        public int acquireId() {
            if (!availableIds.isEmpty()) {
                return availableIds.poll();
            }
            return nextNewId++;
        }

        /**
         * Release an ID back to the pool for reuse
         * @param id The ID to release
         */
        public void releaseId(int id) {
            if (id > 0 && !availableIds.contains(id)) {
                availableIds.offer(id);
            }
        }

        /**
         * Reset the pool to initial state
         */
        public void reset() {
            availableIds.clear();
            nextNewId = 1;
        }

        /**
         * Get the size of available IDs in pool
         * @return Number of reusable IDs
         */
        public int getAvailableCount() {
            return availableIds.size();
        }
    }
}