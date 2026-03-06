package me.kitakeyos.j2me.domain.emulator.repository;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorConfig;

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
}
