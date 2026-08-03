package me.kitakeyos.j2me.domain.emulator.service.cleanup;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.service.InstanceCleanupStep;
import me.kitakeyos.j2me.domain.graphics.service.GraphicsOptimizationService;

/**
 * Restores the original DisplayAccess by removing the graphics proxy.
 * Must run before the classloader is closed — the proxy holds references
 * resolved through it.
 */
public class GraphicsCleanupStep implements InstanceCleanupStep {

    private final GraphicsOptimizationService graphicsService;

    public GraphicsCleanupStep(GraphicsOptimizationService graphicsService) {
        this.graphicsService = graphicsService;
    }

    @Override
    public String name() {
        return "graphics";
    }

    @Override
    public void clean(EmulatorInstance instance) {
        graphicsService.removeInstance(instance);
    }
}
