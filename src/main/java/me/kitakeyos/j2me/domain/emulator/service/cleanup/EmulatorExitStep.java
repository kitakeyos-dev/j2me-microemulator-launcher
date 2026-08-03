package me.kitakeyos.j2me.domain.emulator.service.cleanup;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.service.InstanceCleanupStep;

import java.awt.event.ActionListener;

/**
 * Fires MicroEmulator's own exit listener so the MIDlet gets a chance to shut
 * down cleanly, then pauses briefly to let the emulator settle its internal
 * state before the harder cleanup steps run.
 */
public class EmulatorExitStep implements InstanceCleanupStep {

    private static final long SETTLE_MILLIS = 100L;

    @Override
    public String name() {
        return "emulator-exit";
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public void clean(EmulatorInstance instance) {
        ActionListener exitListener = instance.getMenuExitListener();
        if (exitListener == null) {
            return;
        }
        exitListener.actionPerformed(null);

        try {
            Thread.sleep(SETTLE_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
