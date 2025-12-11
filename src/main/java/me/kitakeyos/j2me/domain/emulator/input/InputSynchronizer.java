package me.kitakeyos.j2me.domain.emulator.input;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;

/**
 * Interface for synchronizing input across multiple emulator instances.
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
     * Attach listeners to a new instance
     */
    void attachListenersToInstance(EmulatorInstance instance);

    /**
     * Detach listeners from an instance
     */
    void detachListenersFromInstance(EmulatorInstance instance);
}
