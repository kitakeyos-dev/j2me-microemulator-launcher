package me.kitakeyos.j2me.infrastructure.persistence.emulator;

import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorConfig;
import me.kitakeyos.j2me.domain.emulator.repository.EmulatorConfigRepository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
    private static final String EMULATORS_DIR = "emulators";

    private final String configFilePath;
    private final File dataDir;
    private final File emulatorsDir;
    private final List<EmulatorConfig> configs;
    private final List<EmulatorConfigChangeListener> listeners = new ArrayList<>();

    public EmulatorConfigRepositoryImpl(ApplicationConfig applicationConfig) {
        this.dataDir = applicationConfig.getDataDirectory();
        this.configFilePath = new File(dataDir, CONFIG_FILE).getPath();
        this.emulatorsDir = new File(dataDir, EMULATORS_DIR);
        if (!emulatorsDir.exists()) {
            emulatorsDir.mkdirs();
        }
        this.configs = new ArrayList<>();
        loadConfigs();

        // Auto-create default emulator if none exist
        if (configs.isEmpty()) {
            String defaultPath = applicationConfig.getMicroemulatorPath();
            if (defaultPath != null && !defaultPath.isEmpty()) {
                try {
                    String clonedPath = cloneJarFile(defaultPath);
                    EmulatorConfig defaultConfig = new EmulatorConfig(
                            "MicroEmulator Default", clonedPath, 240, 320);
                    save(defaultConfig);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Cannot clone default emulator JAR: " + e.getMessage());
                    // Fallback: use original path
                    EmulatorConfig defaultConfig = new EmulatorConfig(
                            "MicroEmulator Default", defaultPath, 240, 320);
                    save(defaultConfig);
                }
            }
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
        // Delete cloned JAR file if it lives in our emulators directory
        for (EmulatorConfig config : configs) {
            if (config.getId().equals(id)) {
                File jarFile = new File(config.getJarPath());
                if (jarFile.exists() && jarFile.getParentFile().equals(emulatorsDir)) {
                    jarFile.delete();
                }
                break;
            }
        }
        configs.removeIf(c -> c.getId().equals(id));
        saveConfigs();
        notifyListeners();
    }

    /**
     * Clone a JAR file into the data/emulators/ directory.
     * Returns the path to the cloned file.
     */
    public String cloneJarFile(String sourcePath) throws IOException {
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            throw new IOException("Source JAR file does not exist: " + sourcePath);
        }

        String fileName = sourceFile.getName();
        File targetFile = new File(emulatorsDir, fileName);

        // If file already exists with same name, add a suffix
        int counter = 1;
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String extension = fileName.substring(fileName.lastIndexOf('.'));
        while (targetFile.exists()) {
            targetFile = new File(emulatorsDir, baseName + "_" + counter + extension);
            counter++;
        }

        Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        return targetFile.getAbsolutePath();
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
                // Resolve relative path to absolute
                String resolvedPath = resolveJarPath(jarPath);
                configs.add(new EmulatorConfig(id, name, resolvedPath, displayWidth, displayHeight));
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
            // Save as relative path if inside data directory
            props.setProperty(prefix + "jarPath", toRelativePath(config.getJarPath()));
            props.setProperty(prefix + "displayWidth", String.valueOf(config.getDefaultDisplayWidth()));
            props.setProperty(prefix + "displayHeight", String.valueOf(config.getDefaultDisplayHeight()));
        }

        try (FileOutputStream fos = new FileOutputStream(configFilePath)) {
            props.store(fos, "Emulator Configurations");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Cannot save emulator configs: " + e.getMessage());
        }
    }

    /**
     * Convert absolute path to relative (relative to data dir) if possible.
     */
    private String toRelativePath(String absolutePath) {
        try {
            java.nio.file.Path dataPath = dataDir.toPath().toAbsolutePath().normalize();
            java.nio.file.Path filePath = new File(absolutePath).toPath().toAbsolutePath().normalize();
            if (filePath.startsWith(dataPath)) {
                return dataPath.relativize(filePath).toString().replace('\\', '/');
            }
        } catch (Exception ignored) {
        }
        return absolutePath;
    }

    /**
     * Resolve a path (relative or absolute) to an absolute path.
     */
    private String resolveJarPath(String storedPath) {
        File file = new File(storedPath);
        if (file.isAbsolute()) {
            return storedPath;
        }
        // Resolve relative to data directory
        return new File(dataDir, storedPath).getAbsolutePath();
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
