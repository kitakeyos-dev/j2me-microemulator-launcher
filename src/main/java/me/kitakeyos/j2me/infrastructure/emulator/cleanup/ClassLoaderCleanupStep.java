package me.kitakeyos.j2me.infrastructure.emulator.cleanup;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.service.InstanceCleanupStep;

import java.io.Closeable;

/**
 * Closes the instance's classloader to release JAR handles and its loaded
 * classes, then nulls both classloader references so the graph can be
 * collected.
 * <p>
 * Lives in infrastructure because closing is a concern of the concrete
 * {@code EmulatorClassLoader}; the domain only ever sees a {@link ClassLoader}.
 */
public class ClassLoaderCleanupStep implements InstanceCleanupStep {

    @Override
    public String name() {
        return "classloader";
    }

    @Override
    public void clean(EmulatorInstance instance) {
        ClassLoader emulatorClassLoader = instance.getEmulatorClassLoader();
        try {
            if (emulatorClassLoader instanceof Closeable) {
                ((Closeable) emulatorClassLoader).close();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to close EmulatorClassLoader", e);
        } finally {
            // Drop the references even if closing failed, otherwise the whole
            // class graph stays reachable for the rest of the session.
            instance.setAppClassLoader(null);
            instance.setEmulatorClassLoader(null);
        }
    }
}
