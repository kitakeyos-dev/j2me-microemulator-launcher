package me.kitakeyos.j2me.domain.emulator.service;

import me.kitakeyos.j2me.domain.emulator.input.InputSynchronizer;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance.InstanceState;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages emulator instances with ID pool for efficient ID reuse.
 * Now uses InputSynchronizer interface (Dependency Inversion Principle).
 */
public class InstanceManager {
    private final List<EmulatorInstance> instances;
    private final JPanel instancesPanel;
    private final InstanceIdPool idPool;
    private InputSynchronizer inputSynchronizer;

    public InstanceManager(JPanel instancesPanel) {
        this.instances = new ArrayList<>();
        this.instancesPanel = instancesPanel;
        this.idPool = new InstanceIdPool();
    }

    /**
     * Set the input synchronizer (dependency injection)
     */
    public void setInputSynchronizer(InputSynchronizer inputSynchronizer) {
        this.inputSynchronizer = inputSynchronizer;
    }

    public void addInstance(EmulatorInstance instance) {
        instances.add(instance);
    }

    public void removeInstance(EmulatorInstance instance) {
        instances.remove(instance);
        idPool.releaseId(instance.getInstanceId());
    }

    public void clearAllInstances() {
        for (EmulatorInstance instance : new ArrayList<>(instances)) {
            if (instance.getState() == InstanceState.RUNNING) {
                instance.shutdown();
            }
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

    public int getNextInstanceId() {
        return idPool.acquireId();
    }

    public void resetInstanceIdCounter() {
        idPool.reset();
    }

    public String getIdPoolStatistics() {
        return idPool.getStatistics();
    }

    public InputSynchronizer getInputSynchronizer() {
        return inputSynchronizer;
    }

    public void setInputSynchronizationEnabled(boolean enabled) {
        if (inputSynchronizer != null) {
            inputSynchronizer.setEnabled(enabled);
        }
    }

    public boolean isInputSynchronizationEnabled() {
        return inputSynchronizer != null && inputSynchronizer.isEnabled();
    }

    public void setInputScaleBySize(boolean scaleBySize) {
        if (inputSynchronizer != null) {
            inputSynchronizer.setScaleBySize(scaleBySize);
        }
    }

    public boolean isInputScaleBySize() {
        return inputSynchronizer != null && inputSynchronizer.isScaleBySize();
    }

    public void notifyInstanceStarted(EmulatorInstance instance) {
        if (inputSynchronizer != null && inputSynchronizer.isEnabled()) {
            inputSynchronizer.attachListenersToInstance(instance);
        }
    }

    public void notifyInstanceStopping(EmulatorInstance instance) {
        if (inputSynchronizer != null && inputSynchronizer.isEnabled()) {
            inputSynchronizer.detachListenersFromInstance(instance);
        }
    }

    /**
     * Enable or disable graphics rendering for all running instances.
     */
    public void setGlobalGraphicsEnabled(boolean enabled) {
        me.kitakeyos.j2me.domain.graphics.service.GraphicsOptimizationService service = me.kitakeyos.j2me.domain.graphics.service.GraphicsOptimizationService
                .getInstance();
        for (EmulatorInstance instance : getRunningInstances()) {
            service.setGraphicsEnabled(instance, enabled);
        }
    }
}
