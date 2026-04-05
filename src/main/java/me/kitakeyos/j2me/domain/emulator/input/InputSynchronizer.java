package me.kitakeyos.j2me.domain.emulator.input;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;

import java.util.Set;

/**
 * Interface for synchronizing input across selected emulator instances.
 * This interface lives in Domain layer; implementations live in Infrastructure.
 */
public interface InputSynchronizer {

    /**
     * Enable or disable input synchronization
     */
    void setEnabled(boolean enabled);

    /**
     * Check if synchronization is enabled
     */
    boolean isEnabled();

    /**
     * Enable or disable scaling by device panel size
     */
    void setScaleBySize(boolean scaleBySize);

    /**
     * Check if scaling by size is enabled
     */
    boolean isScaleBySize();

    /**
     * Set which instance IDs should participate in sync.
     * Only these instances will send/receive synchronized input.
     * An empty set means no instances are synced.
     */
    void setSyncedInstanceIds(Set<Integer> instanceIds);

    /**
     * Get the set of instance IDs currently participating in sync.
     */
    Set<Integer> getSyncedInstanceIds();

    /**
     * Attach listeners to a new instance (only if it's in the synced set)
     */
    void attachListenersToInstance(EmulatorInstance instance);

    /**
     * Detach listeners from an instance
     */
    void detachListenersFromInstance(EmulatorInstance instance);
}
