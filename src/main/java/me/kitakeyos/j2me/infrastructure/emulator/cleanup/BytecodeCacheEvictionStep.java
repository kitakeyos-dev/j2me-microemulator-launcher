package me.kitakeyos.j2me.infrastructure.emulator.cleanup;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.repository.InstanceLookup;
import me.kitakeyos.j2me.domain.emulator.service.InstanceCleanupStep;
import me.kitakeyos.j2me.infrastructure.classloader.EmulatorClassLoader;

/**
 * Clears the shared transformed-bytecode cache once the last instance stops.
 * Keeping it warm while other instances run avoids re-instrumenting their
 * classes on restart.
 */
public class BytecodeCacheEvictionStep implements InstanceCleanupStep {

    private final InstanceLookup instances;

    public BytecodeCacheEvictionStep(InstanceLookup instances) {
        this.instances = instances;
    }

    @Override
    public String name() {
        return "bytecode-cache";
    }

    @Override
    public void clean(EmulatorInstance instance) {
        if (instances.getRunningInstances().isEmpty()) {
            EmulatorClassLoader.clearSharedCache();
        }
    }
}
