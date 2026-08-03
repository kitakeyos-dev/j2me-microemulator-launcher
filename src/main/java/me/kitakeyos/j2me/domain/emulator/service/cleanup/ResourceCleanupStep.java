package me.kitakeyos.j2me.domain.emulator.service.cleanup;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.resource.ResourceManager;
import me.kitakeyos.j2me.domain.emulator.service.InstanceCleanupStep;

/**
 * Force-stops the instance's remaining threads and closes its sockets.
 */
public class ResourceCleanupStep implements InstanceCleanupStep {

    @Override
    public String name() {
        return "resources";
    }

    @Override
    public void clean(EmulatorInstance instance) {
        ResourceManager resourceManager = instance.getResourceManager();
        if (resourceManager != null) {
            resourceManager.cleanupAll();
        }
    }
}
