package me.kitakeyos.j2me.infrastructure.persistence.script;

import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.domain.script.model.LuaScript;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * ScriptFileManager - Handles loading and saving of Lua scripts and their
 * metadata using java.nio.file API.
 * Supports nested folder structure for organizing scripts.
 */
public class ScriptFileManager {
    private static final Logger logger = Logger.getLogger(ScriptFileManager.class.getName());

    private final Path scriptsDirectory;

    public ScriptFileManager(ApplicationConfig config) {
        this.scriptsDirectory = Paths.get(config.getDataDirectory().getAbsolutePath(), ApplicationConfig.SCRIPTS_DIR);
        try {
            if (!Files.exists(scriptsDirectory)) {
                Files.createDirectories(scriptsDirectory);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create scripts directory", e);
        }
    }

    public Path getScriptsDirectory() {
        return scriptsDirectory;
    }

    /**
     * Loads all Lua scripts recursively from the scripts directory and
     * subdirectories
     *
     * @return Map of script paths (relative string) to LuaScript objects
     */
    public Map<String, LuaScript> loadScripts() {
        Map<String, LuaScript> scripts = new HashMap<>();
        if (!Files.exists(scriptsDirectory)) {
            return scripts;
        }

        try (Stream<Path> stream = Files.walk(scriptsDirectory)) {
            stream.filter(p -> !Files.isDirectory(p) && p.toString().endsWith(".lua"))
                    .forEach(p -> {
                        try {
                            String code = new String(Files.readAllBytes(p));
                            String relativePath = getRelativePathString(p);
                            // Use filename as name, but store full relative path as key
                            scripts.put(relativePath, new LuaScript(p.getFileName().toString(), code, p));
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "Failed to load script: " + p, e);
                        }
                    });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to walk scripts directory", e);
        }
        return scripts;
    }

    /**
     * Get all folders in the scripts directory (recursively)
     * 
     * @return List of relative folder paths
     */
    public List<String> getAllFolders() {
        List<String> folders = new ArrayList<>();
        if (!Files.exists(scriptsDirectory)) {
            return folders;
        }

        try (Stream<Path> stream = Files.walk(scriptsDirectory)) {
            stream.filter(Files::isDirectory)
                    .filter(p -> !p.equals(scriptsDirectory))
                    .forEach(p -> folders.add(getRelativePathString(p)));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to walk scripts directory for folders", e);
        }
        return folders;
    }

    /**
     * Saves a Lua script to file, creating subdirectories if needed
     *
     * @param script The LuaScript object to save
     */
    public void saveScriptToFile(LuaScript script) {
        Path path = script.getPath();
        if (path == null) {
            // Should not happen if properly initialized, but fallback
            path = scriptsDirectory.resolve(script.getName());
        }

        try {
            Files.createDirectories(path.getParent());
            Files.write(path, script.getCode().getBytes());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save script file " + path, e);
        }
    }

    /**
     * Deletes a script file or folder
     *
     * @param relativePath The relative path of the script/folder to delete
     * @return true if successful
     */
    public boolean deletePath(String relativePath) {
        Path target = scriptsDirectory.resolve(relativePath);
        if (!Files.exists(target)) {
            // Try appending .lua if it's a script and not found
            if (!relativePath.endsWith(".lua")) {
                Path withExt = scriptsDirectory.resolve(relativePath + ".lua");
                if (Files.exists(withExt)) {
                    target = withExt;
                }
            }
        }

        if (!Files.exists(target))
            return false;

        try {
            if (Files.isDirectory(target)) {
                deleteDirectoryRecursive(target);
            } else {
                deleteFileWithRetry(target);
            }
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to delete path: " + target, e);
            return false;
        }
    }

    private void deleteFileWithRetry(Path file) throws IOException {
        try {
            Files.delete(file);
        } catch (IOException e) {
            // Retry logic for Windows file locks
            try {
                System.gc();
                Thread.sleep(50);
                Files.delete(file);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted during deletion retry", ie);
            } catch (IOException ex) {
                logger.warning("Failed to delete file after retry: " + file);
                throw ex;
            }
        }
    }

    private void deleteDirectoryRecursive(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                deleteFileWithRetry(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null)
                    throw exc;
                deleteFileWithRetry(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Create a new folder in the scripts directory
     *
     * @param folderPath Relative path for the new folder
     * @return true if folder was created successfully
     */
    public boolean createFolder(String folderPath) {
        Path target = scriptsDirectory.resolve(folderPath);
        try {
            Files.createDirectories(target);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create folder: " + target, e);
            return false;
        }
    }

    /**
     * Rename a script or folder
     *
     * @param oldRelPath Old relative path
     * @param newRelPath New relative path
     * @return true if rename was successful
     */
    public boolean renamePath(String oldRelPath, String newRelPath) {
        Path source = scriptsDirectory.resolve(oldRelPath);
        Path target = scriptsDirectory.resolve(newRelPath);

        // Handle implicit .lua extension for source if needed
        if (!Files.exists(source) && !oldRelPath.endsWith(".lua")) {
            Path withExt = scriptsDirectory.resolve(oldRelPath + ".lua");
            if (Files.exists(withExt)) {
                source = withExt;
                // If source had implicit extension, target probably should too unless specified
                if (!newRelPath.endsWith(".lua")) {
                    target = scriptsDirectory.resolve(newRelPath + ".lua");
                }
            }
        }

        if (!Files.exists(source))
            return false;

        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to rename path from " + source + " to " + target, e);
            return false;
        }
    }

    /**
     * Resolve a relative path string to an absolute Path object
     */
    public Path resolvePath(String relativePath) {
        return scriptsDirectory.resolve(relativePath);
    }

    private String getRelativePathString(Path path) {
        return scriptsDirectory.relativize(path).toString().replace("\\", "/");
    }
}