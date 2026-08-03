package me.kitakeyos.j2me.domain.emulator.service.cleanup;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.repository.InstanceRegistrar;
import me.kitakeyos.j2me.domain.emulator.service.InstanceCleanupStep;

/**
 * Deregisters the instance.
 * <p>
 * Deliberately late in the pipeline: XThreads spawned while the emulator is
 * exiting still need to resolve their owning instance, so the registry entry
 * has to outlive the exit step.
 */
public class RegistryRemovalStep implements InstanceCleanupStep {

    private final InstanceRegistrar registrar;

    public RegistryRemovalStep(InstanceRegistrar registrar) {
        this.registrar = registrar;
    }

    @Override
    public String name() {
        return "registry";
    }

    @Override
    public void clean(EmulatorInstance instance) {
        registrar.removeInstance(instance);
    }
}
