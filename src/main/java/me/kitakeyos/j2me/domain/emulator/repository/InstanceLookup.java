package me.kitakeyos.j2me.domain.emulator.repository;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;

import java.util.List;

/**
 * Read-only view over the live emulator instances.
 * <p>
 * Split from {@link InstanceRegistrar} so collaborators that only need to
 * look instances up (speed propagation, bytecode entry points) do not gain
 * the ability to mutate the instance list.
 */
public interface InstanceLookup {

    /**
     * Find a running or created instance by its id.
     *
     * @param instanceId the instance id
     * @return the instance, or {@code null} when no instance holds that id
     */
    EmulatorInstance findInstance(int instanceId);

    /**
     * @return every instance currently in the RUNNING state
     */
    List<EmulatorInstance> getRunningInstances();
}
