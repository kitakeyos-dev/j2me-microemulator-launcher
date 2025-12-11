package me.kitakeyos.j2me.application.script;

import me.kitakeyos.j2me.application.script.state.EditorState;
import me.kitakeyos.j2me.application.script.state.EditorStateManager;
import me.kitakeyos.j2me.domain.script.executor.LuaScriptExecutor;
import me.kitakeyos.j2me.domain.script.model.LuaScript;
import me.kitakeyos.j2me.infrastructure.persistence.script.ScriptFileManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Application layer service for managing Lua scripts.
 * Coordinates between Domain (LuaScriptExecutor) and Infrastructure
 * (ScriptFileManager).
 * Presentation layer should depend on this service, not on Infrastructure
 * directly.
 */
public class LuaScriptService {

    private final ScriptFileManager fileManager;
    private final EditorStateManager stateManager;
    private final LuaScriptExecutor scriptExecutor;
    private Map<String, LuaScript> scripts;
    private LuaScript currentScript;

    public LuaScriptService(ScriptFileManager fileManager,
            Consumer<String> outputConsumer,
            Consumer<String> errorConsumer,
            Consumer<String> successConsumer,
            Consumer<String> infoConsumer) {
        this.fileManager = fileManager;
        this.stateManager = new EditorStateManager();
        this.scriptExecutor = new LuaScriptExecutor(
                fileManager.getScriptsDirectory(),
                outputConsumer,
                errorConsumer,
                successConsumer,
                infoConsumer);
    }

    /**
     * Load all scripts from disk
     */
    public void loadScripts() {
        scripts = fileManager.loadScripts();

        // Initialize states for all scripts
        for (Map.Entry<String, LuaScript> entry : scripts.entrySet()) {
            stateManager.initializeState(entry.getKey(), entry.getValue().getCode());
        }
    }

    /**
     * Get all scripts
     */
    public Map<String, LuaScript> getScripts() {
        return scripts;
    }

    /**
     * Get all folder paths
     */
    public List<String> getAllFolders() {
        return fileManager.getAllFolders();
    }

    /**
     * Get script count
     */
    public int getScriptCount() {
        return scripts != null ? scripts.size() : 0;
    }

    /**
     * Get current script
     */
    public LuaScript getCurrentScript() {
        return currentScript;
    }

    /**
     * Set current script
     */
    public void setCurrentScript(LuaScript script) {
        this.currentScript = script;
        if (script != null) {
            String key = getScriptKey(script);
            stateManager.setCurrentScriptName(key);
        }
    }

    /**
     * Get script by key (relative path)
     */
    public LuaScript getScript(String key) {
        return scripts != null ? scripts.get(key) : null;
    }

    /**
     * Create a new script
     */
    public LuaScript createScript(String folderPath, String name) {
        String fullPath = folderPath.isEmpty() ? name : folderPath + "/" + name;
        if (!fullPath.endsWith(".lua")) {
            fullPath += ".lua";
        }

        if (scripts.containsKey(fullPath)) {
            return null; // Already exists
        }

        String templateCode = generateTemplate(name);
        Path scriptPath = fileManager.resolvePath(fullPath);
        LuaScript script = new LuaScript(name, templateCode, scriptPath);

        // Save immediately to ensure file exists
        fileManager.saveScriptToFile(script);

        // Update local map
        scripts.put(fullPath, script);
        stateManager.initializeState(fullPath, templateCode);

        return script;
    }

    /**
     * Create a new folder
     */
    public boolean createFolder(String parentPath, String name) {
        String fullPath = parentPath.isEmpty() ? name : parentPath + "/" + name;
        return fileManager.createFolder(fullPath);
    }

    /**
     * Save a script
     */
    public void saveScript(LuaScript script, String code) {
        script.setCode(code);

        String scriptKey = getScriptKey(script);
        EditorState state = stateManager.getState(scriptKey);
        if (state != null) {
            state.setCode(code);
            state.setModified(false);
        }

        fileManager.saveScriptToFile(script);
    }

    /**
     * Delete a script or folder
     */
    public boolean deletePath(String path, boolean isFolder) {
        if (!fileManager.deletePath(path)) {
            return false;
        }

        if (isFolder) {
            // Remove all scripts in this folder from map
            scripts.entrySet().removeIf(entry -> entry.getKey().equals(path) || entry.getKey().startsWith(path + "/"));
        } else {
            scripts.remove(path);
            stateManager.removeState(path);
        }

        // Clear current script if it was deleted
        if (currentScript != null) {
            String currentKey = getScriptKey(currentScript);
            if (currentKey != null && (currentKey.equals(path) || currentKey.startsWith(path + "/"))) {
                currentScript = null;
            }
        }

        return true;
    }

    /**
     * Rename a script or folder
     */
    public boolean renamePath(String oldPath, String newPath, boolean isFolder) {
        if (!fileManager.renamePath(oldPath, newPath)) {
            return false;
        }

        if (isFolder) {
            // Update all scripts in map that start with old path
            List<String> keysToUpdate = new ArrayList<>();
            for (String key : scripts.keySet()) {
                if (key.startsWith(oldPath + "/")) {
                    keysToUpdate.add(key);
                }
            }

            for (String oldKey : keysToUpdate) {
                LuaScript script = scripts.remove(oldKey);
                String newKey = newPath + oldKey.substring(oldPath.length());
                Path newScriptPath = fileManager.resolvePath(newKey);
                script.setPath(newScriptPath);
                scripts.put(newKey, script);

                // Update state manager
                EditorState state = stateManager.getState(oldKey);
                if (state != null) {
                    stateManager.removeState(oldKey);
                    stateManager.initializeState(newKey, state.getCode());
                    if (state.isModified()) {
                        stateManager.setModified(newKey, true);
                    }
                }
            }
        } else {
            // Single script rename
            LuaScript script = scripts.remove(oldPath);
            if (script != null) {
                Path newScriptPath = fileManager.resolvePath(newPath);
                script.setPath(newScriptPath);
                String newName = newPath.contains("/")
                        ? newPath.substring(newPath.lastIndexOf("/") + 1)
                        : newPath;
                script.setName(newName);
                scripts.put(newPath, script);

                // Update state manager
                EditorState state = stateManager.getState(oldPath);
                if (state != null) {
                    stateManager.removeState(oldPath);
                    stateManager.initializeState(newPath, state.getCode());
                    if (state.isModified()) {
                        stateManager.setModified(newPath, true);
                    }
                }
            }
        }

        return true;
    }

    /**
     * Execute a script
     */
    public void executeScript(LuaScript script) {
        scriptExecutor.executeScript(script.getPath());
    }

    /**
     * Set ClassLoader for script execution
     */
    public void setInstanceClassLoader(ClassLoader classLoader) {
        scriptExecutor.setInstanceClassLoader(classLoader);
    }

    /**
     * Get the state manager for editor state management
     */
    public EditorStateManager getStateManager() {
        return stateManager;
    }

    /**
     * Resolve a relative path to absolute
     */
    public Path resolvePath(String relativePath) {
        return fileManager.resolvePath(relativePath);
    }

    /**
     * Get list of modified scripts
     */
    public List<String> getModifiedScripts() {
        return stateManager.getModifiedScripts();
    }

    /**
     * Check if script is modified
     */
    public boolean isModified(String scriptKey) {
        return stateManager.isModified(scriptKey);
    }

    /**
     * Set modified flag
     */
    public void setModified(String scriptKey, boolean modified) {
        stateManager.setModified(scriptKey, modified);
    }

    /**
     * Get or create editor state
     */
    public EditorState getOrCreateState(String scriptKey) {
        return stateManager.getOrCreateState(scriptKey);
    }

    /**
     * Get editor state
     */
    public EditorState getState(String scriptKey) {
        return stateManager.getState(scriptKey);
    }

    /**
     * Get the key (relative path) for a script object
     */
    public String getScriptKey(LuaScript script) {
        if (script == null || scripts == null)
            return null;

        for (Map.Entry<String, LuaScript> entry : scripts.entrySet()) {
            if (entry.getValue() == script) {
                return entry.getKey();
            }
        }
        // Fallback: if we can't find the exact object, try by path equality
        for (Map.Entry<String, LuaScript> entry : scripts.entrySet()) {
            if (entry.getValue().getPath().equals(script.getPath())) {
                return entry.getKey();
            }
        }
        return script.getName(); // Fallback to name if all else fails
    }

    private String generateTemplate(String name) {
        return "-- " + name + "\n" +
                "-- Created: " + java.time.LocalDate.now() + "\n\n" +
                "-- Your Lua code here\n" +
                "print(\"Hello from " + name + "!\")\n";
    }
}
