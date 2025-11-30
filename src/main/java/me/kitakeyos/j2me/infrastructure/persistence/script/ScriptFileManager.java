package me.kitakeyos.j2me.infrastructure.persistence.script;

import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.domain.script.model.LuaScript;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * ScriptFileManager - Handles loading and saving of Lua scripts and their
 * metadata
 * Supports nested folder structure for organizing scripts
 */
public class ScriptFileManager {
    private final File scriptsDirectory;

    public ScriptFileManager(ApplicationConfig config) {
        this.scriptsDirectory = new File(config.getDataDirectory(), ApplicationConfig.SCRIPTS_DIR);
        if (!this.scriptsDirectory.exists()) {
            this.scriptsDirectory.mkdirs();
        }
    }

    public File getScriptsDirectory() {
        return scriptsDirectory;
    }

    /**
     * Loads all Lua scripts recursively from the scripts directory and
     * subdirectories
     *
     * @return Map of script paths (relative) to LuaScript objects
     */
    public Map<String, LuaScript> loadScripts() {
        Map<String, LuaScript> scripts = new HashMap<>();

        if (!scriptsDirectory.exists()) {
            return scripts;
        }

        loadScriptsRecursive(scriptsDirectory, "", scripts);
        return scripts;
    }

    /**
     * Recursively load scripts from directory and subdirectories
     */
    private void loadScriptsRecursive(File directory, String relativePath, Map<String, LuaScript> scripts) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // Recurse into subdirectory
                String subPath = relativePath.isEmpty() ? file.getName() : relativePath + "/" + file.getName();
                loadScriptsRecursive(file, subPath, scripts);
            } else if (file.getName().endsWith(".lua")) {
                // Load Lua script
                String scriptName = file.getName();
                String scriptPath = relativePath.isEmpty() ? scriptName : relativePath + "/" + scriptName;

                String code = readFileContent(file);
                LuaScript script = new LuaScript(scriptPath, code);
                scripts.put(scriptPath, script);
            }
        }
    }

    /**
     * Get all folders in the scripts directory (recursively)
     * 
     * @return List of relative folder paths
     */
    public java.util.List<String> getAllFolders() {
        java.util.List<String> folders = new java.util.ArrayList<>();
        if (scriptsDirectory.exists()) {
            getAllFoldersRecursive(scriptsDirectory, "", folders);
        }
        return folders;
    }

    private void getAllFoldersRecursive(File directory, String relativePath, java.util.List<String> folders) {
        File[] files = directory.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isDirectory()) {
                String subPath = relativePath.isEmpty() ? file.getName() : relativePath + "/" + file.getName();
                folders.add(subPath);
                getAllFoldersRecursive(file, subPath, folders);
            }
        }
    }

    /**
     * Read file content as string
     */
    private String readFileContent(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder codeBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                codeBuilder.append(line).append("\n");
            }
            return codeBuilder.toString();
        } catch (IOException e) {
            System.err.println("Failed to load script file " + file.getName() + ": " + e.getMessage());
            return "";
        }
    }

    /**
     * Saves a Lua script to file, creating subdirectories if needed
     *
     * @param script The LuaScript object to save
     */
    public void saveScriptToFile(LuaScript script) {
        File luaFile = getScriptFile(script.getName());

        // Create parent directories if they don't exist
        File parentDir = luaFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(luaFile))) {
            writer.print(script.getCode());
        } catch (IOException e) {
            System.err.println("Failed to save script file " + script.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Deletes a script file
     *
     * @param scriptPath The relative path of the script to delete (e.g.,
     *                   "folder/scriptname")
     */
    public void deleteScriptFiles(String scriptPath) {
        File scriptFile = getScriptFile(scriptPath);
        if (scriptFile.exists()) {
            scriptFile.delete();

            // Clean up empty parent directories
            cleanupEmptyDirectories(scriptFile.getParentFile());
        }
    }

    /**
     * Get the actual File object for a script path
     */
    private File getScriptFile(String scriptPath) {
        return new File(scriptsDirectory, scriptPath + ".lua");
    }

    /**
     * Remove empty directories up to the scripts root
     */
    private void cleanupEmptyDirectories(File directory) {
        while (directory != null && !directory.equals(scriptsDirectory)) {
            File[] files = directory.listFiles();
            if (files == null || files.length == 0) {
                directory.delete();
                directory = directory.getParentFile();
            } else {
                break;
            }
        }
    }

    /**
     * Create a new folder in the scripts directory
     *
     * @param folderPath Relative path for the new folder
     * @return true if folder was created successfully
     */
    public boolean createFolder(String folderPath) {
        File folder = new File(scriptsDirectory, folderPath);
        return folder.mkdirs();
    }

    /**
     * Delete a folder and all its contents
     *
     * @param folderPath Relative path of the folder to delete
     * @return true if folder was deleted successfully
     */
    public boolean deleteFolder(String folderPath) {
        File folder = new File(scriptsDirectory, folderPath);
        return deleteRecursive(folder);
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return file.delete();
    }

    /**
     * Rename a script or move it to a different path
     *
     * @param oldPath Old relative path
     * @param newPath New relative path
     * @return true if rename was successful
     */
    public boolean renameScript(String oldPath, String newPath) {
        File oldFile = getScriptFile(oldPath);
        File newFile = getScriptFile(newPath);

        if (!oldFile.exists()) {
            return false;
        }

        // Create parent directories for new location if needed
        File newParent = newFile.getParentFile();
        if (!newParent.exists()) {
            newParent.mkdirs();
        }

        boolean success = oldFile.renameTo(newFile);

        if (success) {
            // Clean up empty directories from old location
            cleanupEmptyDirectories(oldFile.getParentFile());
        }

        return success;
    }
}