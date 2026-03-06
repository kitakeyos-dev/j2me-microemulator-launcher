package me.kitakeyos.j2me.domain.emulator.model;

import java.util.UUID;

/**
 * Represents an installed emulator configuration.
 * Each config holds the emulator JAR path and default display settings.
 */
public class EmulatorConfig {

    private final String id;
    private String name;
    private String jarPath;
    private int defaultDisplayWidth;
    private int defaultDisplayHeight;

    public EmulatorConfig(String name, String jarPath, int defaultDisplayWidth, int defaultDisplayHeight) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.jarPath = jarPath;
        this.defaultDisplayWidth = defaultDisplayWidth;
        this.defaultDisplayHeight = defaultDisplayHeight;
    }

    public EmulatorConfig(String id, String name, String jarPath, int defaultDisplayWidth, int defaultDisplayHeight) {
        this.id = id;
        this.name = name;
        this.jarPath = jarPath;
        this.defaultDisplayWidth = defaultDisplayWidth;
        this.defaultDisplayHeight = defaultDisplayHeight;
    }

    // === Getters ===

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getJarPath() {
        return jarPath;
    }

    public int getDefaultDisplayWidth() {
        return defaultDisplayWidth;
    }

    public int getDefaultDisplayHeight() {
        return defaultDisplayHeight;
    }

    // === Setters ===

    public void setName(String name) {
        this.name = name;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public void setDefaultDisplayWidth(int defaultDisplayWidth) {
        this.defaultDisplayWidth = defaultDisplayWidth;
    }

    public void setDefaultDisplayHeight(int defaultDisplayHeight) {
        this.defaultDisplayHeight = defaultDisplayHeight;
    }

    /**
     * Check if the JAR file exists and is valid
     */
    public boolean isValid() {
        if (jarPath == null || jarPath.isEmpty()) {
            return false;
        }
        java.io.File file = new java.io.File(jarPath);
        return file.exists() && file.isFile() && jarPath.toLowerCase().endsWith(".jar");
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        EmulatorConfig that = (EmulatorConfig) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
