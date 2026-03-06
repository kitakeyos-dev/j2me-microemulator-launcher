package me.kitakeyos.j2me.infrastructure.persistence.emulator;

import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorConfig;
import me.kitakeyos.j2me.domain.emulator.repository.EmulatorConfigRepository;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Properties-file based implementation of EmulatorConfigRepository.
 * Stores emulator configurations in data/emulators.properties
 */
public class EmulatorConfigRepositoryImpl implements EmulatorConfigRepository {

    private static final Logger logger = Logger.getLogger(EmulatorConfigRepositoryImpl.class.getName());
    private static final String CONFIG_FILE = "emulators.properties";

    private final String configFilePath;
    private final List<EmulatorConfig> configs;
    private final List<EmulatorConfigChangeListener> listeners = new ArrayList<>();

    public EmulatorConfigRepositoryImpl(ApplicationConfig applicationConfig) {
        this.configFilePath = new File(applicationConfig.getDataDirectory(), CONFIG_FILE).getPath();
        this.configs = new ArrayList<>();
        loadConfigs();

        // Auto-create default emulator if none exist
        if (configs.isEmpty()) {
            String defaultPath = applicationConfig.getMicroemulatorPath();
            EmulatorConfig defaultConfig = new EmulatorConfig(
                    "MicroEmulator Default", defaultPath, 240, 320);
            save(defaultConfig);
        }
    }

    @Override
    public List<EmulatorConfig> getAll() {
        return Collections.unmodifiableList(configs);
    }

    @Override
    public void save(EmulatorConfig config) {
        // Update existing or add new
        boolean found = false;
        for (int i = 0; i < configs.size(); i++) {
            if (configs.get(i).getId().equals(config.getId())) {
                configs.set(i, config);
                found = true;
                break;
            }
        }
        if (!found) {
            configs.add(config);
        }
        saveConfigs();
        notifyListeners();
    }

    @Override
    public void remove(String id) {
        configs.removeIf(c -> c.getId().equals(id));
        saveConfigs();
        notifyListeners();
    }

    private void loadConfigs() {
        File file = new File(configFilePath);
        if (!file.exists()) {
            return;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Cannot load emulator configs: " + e.getMessage());
            return;
        }

        int count = Integer.parseInt(props.getProperty("emulator.count", "0"));
        for (int i = 0; i < count; i++) {
            String prefix = "emulator." + i + ".";
            String id = props.getProperty(prefix + "id");
            String name = props.getProperty(prefix + "name");
            String jarPath = props.getProperty(prefix + "jarPath");
            int displayWidth = Integer.parseInt(props.getProperty(prefix + "displayWidth", "240"));
            int displayHeight = Integer.parseInt(props.getProperty(prefix + "displayHeight", "320"));

            if (id != null && name != null && jarPath != null) {
                configs.add(new EmulatorConfig(id, name, jarPath, displayWidth, displayHeight));
            }
        }
    }

    private void saveConfigs() {
        Properties props = new Properties();
        props.setProperty("emulator.count", String.valueOf(configs.size()));

        for (int i = 0; i < configs.size(); i++) {
            EmulatorConfig config = configs.get(i);
            String prefix = "emulator." + i + ".";
            props.setProperty(prefix + "id", config.getId());
            props.setProperty(prefix + "name", config.getName());
            props.setProperty(prefix + "jarPath", config.getJarPath());
            props.setProperty(prefix + "displayWidth", String.valueOf(config.getDefaultDisplayWidth()));
            props.setProperty(prefix + "displayHeight", String.valueOf(config.getDefaultDisplayHeight()));
        }

        try (FileOutputStream fos = new FileOutputStream(configFilePath)) {
            props.store(fos, "Emulator Configurations");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Cannot save emulator configs: " + e.getMessage());
        }
    }

    // === Listener support ===

    public interface EmulatorConfigChangeListener {
        void onEmulatorConfigsChanged();
    }

    public void addChangeListener(EmulatorConfigChangeListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (EmulatorConfigChangeListener listener : listeners) {
            listener.onEmulatorConfigsChanged();
        }
    }
}
