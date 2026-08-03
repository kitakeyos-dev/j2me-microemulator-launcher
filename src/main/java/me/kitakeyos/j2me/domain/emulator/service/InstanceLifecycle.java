package me.kitakeyos.j2me.domain.emulator.service;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;

/**
 * Shuts an instance down and releases everything it holds.
 */
public interface InstanceLifecycle {

    /**
     * Shut the instance down. Idempotent: a STOPPED instance is left alone.
     */
    void shutdown(EmulatorInstance instance);

    /**
     * Run every cleanup step regardless of the instance's current state.
     * Used by bulk teardown paths that must not skip already-stopped instances.
     */
    void forceShutdown(EmulatorInstance instance);
}
