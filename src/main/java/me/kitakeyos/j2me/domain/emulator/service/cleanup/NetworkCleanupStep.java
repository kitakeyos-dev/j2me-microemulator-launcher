package me.kitakeyos.j2me.domain.emulator.service.cleanup;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.service.InstanceCleanupStep;
import me.kitakeyos.j2me.domain.network.service.NetworkService;

/**
 * Removes the instance's connection logs, packet logs and socket taps.
 */
public class NetworkCleanupStep implements InstanceCleanupStep {

    private final NetworkService networkService;

    public NetworkCleanupStep(NetworkService networkService) {
        this.networkService = networkService;
    }

    @Override
    public String name() {
        return "network";
    }

    @Override
    public void clean(EmulatorInstance instance) {
        networkService.removeInstanceData(instance.getInstanceId());
    }
}
