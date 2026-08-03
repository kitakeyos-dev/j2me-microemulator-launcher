package me.kitakeyos.j2me.domain.emulator.repository;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorConfig;

import java.io.IOException;
import java.util.List;

/**
 * Repository interface for managing emulator configurations.
 */
public interface EmulatorConfigRepository {

    /**
     * Get all installed emulator configurations
     */
    List<EmulatorConfig> getAll();

    /**
     * Save or update an emulator configuration
     */
    void save(EmulatorConfig config);

    /**
     * Remove an emulator configuration by ID
     */
    void remove(String id);

    /**
     * Take the launcher's own copy of an emulator JAR, so the configuration
     * keeps working after the user moves or deletes the original.
     *
     * @param sourcePath path of the JAR to copy
     * @return path of the stored copy
     * @throws IOException if the copy fails
     */
    String cloneJarFile(String sourcePath) throws IOException;

    /**
     * Register a listener notified after any save or remove.
     * <p>
     * Declared on the port rather than the implementation so callers that need
     * change notifications can still depend on this interface.
     */
    void addChangeListener(EmulatorConfigChangeListener listener);

    /**
     * Remove a previously registered listener.
     */
    void removeChangeListener(EmulatorConfigChangeListener listener);

    /**
     * Notified when the stored emulator configurations change.
     */
    interface EmulatorConfigChangeListener {
        void onEmulatorConfigsChanged();
    }
}
