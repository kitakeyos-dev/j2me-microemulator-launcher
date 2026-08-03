package me.kitakeyos.j2me.domain.emulator.service.cleanup;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.service.InstanceCleanupStep;
import me.kitakeyos.j2me.domain.speed.service.SpeedService;

/**
 * Drops the instance's speed multiplier entry.
 */
public class SpeedCleanupStep implements InstanceCleanupStep {

    private final SpeedService speedService;

    public SpeedCleanupStep(SpeedService speedService) {
        this.speedService = speedService;
    }

    @Override
    public String name() {
        return "speed";
    }

    @Override
    public void clean(EmulatorInstance instance) {
        speedService.removeInstance(instance.getInstanceId());
    }
}
