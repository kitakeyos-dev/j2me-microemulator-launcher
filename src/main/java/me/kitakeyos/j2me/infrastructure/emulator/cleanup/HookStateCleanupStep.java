package me.kitakeyos.j2me.infrastructure.emulator.cleanup;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.service.InstanceCleanupStep;
import me.kitakeyos.j2me.infrastructure.hook.HookDispatcher;

/**
 * Drops the hook dispatcher's cached reflection state so the instance's
 * classloader becomes collectable.
 * <p>
 * Registered hooks themselves are intentionally kept: restarting the instance
 * should re-apply them.
 */
public class HookStateCleanupStep implements InstanceCleanupStep {

    @Override
    public String name() {
        return "hook-state";
    }

    @Override
    public void clean(EmulatorInstance instance) {
        HookDispatcher.release(instance.getInstanceId());
    }
}
