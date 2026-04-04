package me.kitakeyos.j2me.application.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages application configuration, including microemulator path and other
 * settings
 */
public class ApplicationConfig {
    private static final Logger logger = Logger.getLogger(ApplicationConfig.class.getName());

    private static final String CONFIG_FILE = "j2me_launcher.properties";
    private static final String MICROEMULATOR_PATH_KEY = "microemulator.path";
    private static final String DEFAULT_MICROEMULATOR_PATH = "microemulator.jar";
    private static final String TOAST_NOTIFICATIONS_KEY = "ui.toastNotifications";
    private static final String LANGUAGE_KEY = "ui.language";
    public static final String DATA_DIR = "data";
    public static final String APPS_DIR = "apps";
    public static final String ICONS_DIR = "icons";
    public static final String RMS_DIR = "rms";
    public static final String APPS_CONFIG_FILE = "j2me_apps.properties";

    private final Properties properties;
    private final String configFilePath;
    private final File dataDirectory;

    public ApplicationConfig() {
        this.properties = new Properties();

        // Use application directory instead of user home
        this.dataDirectory = new File(DATA_DIR);
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }

        this.configFilePath = new File(dataDirectory, CONFIG_FILE).getPath();
        loadConfiguration();
    }

    /**
     * Load configuration from file
     */
    public void loadConfiguration() {
        File configFile = new File(configFilePath);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Cannot load configuration file: " + e.getMessage());
                loadDefaultConfiguration();
            }
        } else {
            loadDefaultConfiguration();
            saveConfiguration();
        }
    }

    /**
     * Save configuration to file
     */
    public void saveConfiguration() {
        try (FileOutputStream fos = new FileOutputStream(configFilePath)) {
            properties.store(fos, "J2ME Launcher Configuration");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Cannot save configuration file: " + e.getMessage());
        }
    }

    /**
     * Load default configuration
     */
    private void loadDefaultConfiguration() {
        properties.setProperty(MICROEMULATOR_PATH_KEY, DEFAULT_MICROEMULATOR_PATH);
        properties.setProperty(TOAST_NOTIFICATIONS_KEY, "true");
    }

    /**
     * Check if toast notifications are enabled
     */
    public boolean isToastNotificationsEnabled() {
        return Boolean.parseBoolean(properties.getProperty(TOAST_NOTIFICATIONS_KEY, "false"));
    }

    /**
     * Set toast notifications enabled
     */
    public void setToastNotificationsEnabled(boolean enabled) {
        properties.setProperty(TOAST_NOTIFICATIONS_KEY, String.valueOf(enabled));
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
     * Get UI language
     */
    public String getLanguage() {
        return properties.getProperty(LANGUAGE_KEY, "en");
    }

    /**
     * Set UI language
     */
    public void setLanguage(String language) {
        properties.setProperty(LANGUAGE_KEY, language);
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
    public String getConfigFilePath() {
        return configFilePath;
    }

    /**
     * Get configuration directory
     */
    public File getDataDirectory() {
        return dataDirectory;
    }
}
