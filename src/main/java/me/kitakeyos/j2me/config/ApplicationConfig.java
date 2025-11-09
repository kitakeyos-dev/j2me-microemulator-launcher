package me.kitakeyos.j2me.config;

import java.io.*;
import java.util.Properties;

/**
 * Manages application configuration, including microemulator path and other settings
 */
public class ApplicationConfig {
    private static final String CONFIG_FILE = "j2me_launcher.properties";
    private static final String MICROEMULATOR_PATH_KEY = "microemulator.path";
    private static final String DEFAULT_MICROEMULATOR_PATH = "microemulator.jar";
    
    private Properties properties;
    private String userConfigFilePath;
    
    public ApplicationConfig() {
        this.properties = new Properties();
        this.userConfigFilePath = System.getProperty("user.home") + File.separator + CONFIG_FILE;
        loadConfiguration();
    }
    
    /**
     * Load configuration from file
     */
    public void loadConfiguration() {
        File configFile = new File(userConfigFilePath);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Cannot load configuration file: " + e.getMessage());
                loadDefaultConfiguration();
            }
        } else {
            loadDefaultConfiguration();
        }
    }
    
    /**
     * Save configuration to file
     */
    public void saveConfiguration() {
        try (FileOutputStream fos = new FileOutputStream(userConfigFilePath)) {
            properties.store(fos, "J2ME Launcher Configuration");
        } catch (IOException e) {
            System.err.println("Cannot save configuration file: " + e.getMessage());
        }
    }
    
    /**
     * Load default configuration
     */
    private void loadDefaultConfiguration() {
        properties.setProperty(MICROEMULATOR_PATH_KEY, DEFAULT_MICROEMULATOR_PATH);
    }
    
    /**
     * Get microemulator path
     */
    public String getMicroemulatorPath() {
        return properties.getProperty(MICROEMULATOR_PATH_KEY, DEFAULT_MICROEMULATOR_PATH);
    }
    
    /**
     * Set microemulator path
     */
    public void setMicroemulatorPath(String path) {
        properties.setProperty(MICROEMULATOR_PATH_KEY, path);
    }
    
    /**
     * Check if microemulator path is valid
     */
    public boolean isMicroemulatorPathValid() {
        String path = getMicroemulatorPath();
        File file = new File(path);
        return file.exists() && file.isFile() && path.toLowerCase().endsWith(".jar");
    }
    
    /**
     * Get configuration file path
     */
    public String getUserConfigFilePath() {
        return userConfigFilePath;
    }
}
