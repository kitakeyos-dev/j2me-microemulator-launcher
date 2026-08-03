package me.kitakeyos.j2me.domain.emulator.service.cleanup;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.service.InstanceCleanupStep;

import javax.swing.JComponent;

/**
 * Detaches the instance's Swing children and clears the references that would
 * otherwise pin the display component in memory.
 */
public class UiComponentCleanupStep implements InstanceCleanupStep {

    @Override
    public String name() {
        return "ui-components";
    }

    @Override
    public void clean(EmulatorInstance instance) {
        JComponent emulatorDisplay = instance.getEmulatorDisplay();
        if (emulatorDisplay != null) {
            emulatorDisplay.putClientProperty("wrapperPanel", null);
            emulatorDisplay.removeAll();
        }
        instance.setEmulatorDisplay(null);
    }
}
