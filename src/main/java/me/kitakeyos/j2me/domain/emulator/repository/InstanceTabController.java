package me.kitakeyos.j2me.domain.emulator.repository;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;

/**
 * Port for detaching an instance's visual tab.
 * <p>
 * A MIDlet calling {@code System.exit()} is intercepted in the bytecode layer
 * and has to take its tab down with it. Declaring the capability here keeps
 * that call pointing inward at a domain port instead of reaching out to the
 * launcher window. Implemented by the presentation layer.
 */
public interface InstanceTabController {

    /**
     * Remove the instance's tab from the running-instances view.
     * Implementations must tolerate being called off the EDT.
     */
    void removeInstanceTab(EmulatorInstance instance);
}
