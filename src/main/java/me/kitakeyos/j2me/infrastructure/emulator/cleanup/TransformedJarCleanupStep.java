package me.kitakeyos.j2me.infrastructure.emulator.cleanup;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.service.InstanceCleanupStep;
import me.kitakeyos.j2me.infrastructure.bytecode.JarTransformer;

import java.nio.file.Path;

/**
 * Deletes the instrumented copy of the MIDlet JAR written for this instance.
 */
public class TransformedJarCleanupStep implements InstanceCleanupStep {

    @Override
    public String name() {
        return "transformed-jar";
    }

    @Override
    public void clean(EmulatorInstance instance) {
        Path transformedJar = instance.getTransformedJarPath();
        if (transformedJar == null) {
            return;
        }
        JarTransformer.cleanupTransformedJar(transformedJar);
        instance.setTransformedJarPath(null);
    }
}
