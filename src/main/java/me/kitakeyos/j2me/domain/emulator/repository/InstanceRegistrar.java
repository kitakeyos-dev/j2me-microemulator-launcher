package me.kitakeyos.j2me.domain.emulator.repository;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;

/**
 * Write side of the instance registry.
 *
 * @see InstanceLookup for the read side
 */
public interface InstanceRegistrar {

    /**
     * Register a newly created instance.
     */
    void addInstance(EmulatorInstance instance);

    /**
     * Deregister an instance and release its id back to the pool.
     */
    void removeInstance(EmulatorInstance instance);
}
