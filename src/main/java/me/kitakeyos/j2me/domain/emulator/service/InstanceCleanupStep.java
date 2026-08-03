package me.kitakeyos.j2me.domain.emulator.service;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;

/**
 * One unit of instance teardown.
 * <p>
 * Steps are executed in registration order and are isolated from each other:
 * a step that throws is logged and the remaining steps still run. Adding a new
 * thing to clean up means registering another step at the composition root,
 * not editing the shutdown sequence.
 */
public interface InstanceCleanupStep {

    /**
     * Short name used in log messages when this step fails.
     */
    String name();

    /**
     * Release whatever this step owns for the given instance.
     */
    void clean(EmulatorInstance instance);

    /**
     * Whether the step may block for a short while (used only for ordering
     * documentation; the runner does not enforce a timeout).
     */
    default boolean isBlocking() {
        return false;
    }
}
