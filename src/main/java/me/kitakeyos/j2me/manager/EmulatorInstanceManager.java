package me.kitakeyos.j2me.manager;

import me.kitakeyos.j2me.config.EmulatorInstance;
import me.kitakeyos.j2me.config.EmulatorInstance.InstanceState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;

/**
 * Manages emulator instances
 */
public class EmulatorInstanceManager {
    private final List<EmulatorInstance> instances;
    private final JPanel instancesPanel;
    private int nextInstanceId = 1;

    public EmulatorInstanceManager(JPanel instancesPanel) {
        this.instances = new ArrayList<>();
        this.instancesPanel = instancesPanel;
    }

    public void addInstance(EmulatorInstance instance) {
        instances.add(instance);
    }

    public void removeInstance(EmulatorInstance instance) {
        instances.remove(instance);
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

    public int getNextInstanceId() {
        return nextInstanceId++;
    }

    public void resetInstanceIdCounter() {
        nextInstanceId = 1;
    }
}