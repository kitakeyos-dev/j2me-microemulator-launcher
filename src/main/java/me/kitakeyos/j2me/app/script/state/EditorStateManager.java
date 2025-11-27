package me.kitakeyos.j2me.app.script.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages editor states for multiple scripts.
 * Provides functionality to save, restore, and track modifications
 * across different script files.
 */
public class EditorStateManager {
    private final Map<String, EditorState> scriptStates;
    private String currentScriptName;

    public EditorStateManager() {
        this.scriptStates = new HashMap<>();
        this.currentScriptName = null;
    }

    /**
     * Gets or creates an editor state for the specified script
     */
    public EditorState getOrCreateState(String scriptName) {
        if (scriptName == null) {
            return null;
        }
        return scriptStates.computeIfAbsent(scriptName, k -> new EditorState());
    }

    /**
     * Gets the state for a script if it exists
     */
    public EditorState getState(String scriptName) {
        return scriptStates.get(scriptName);
    }

    /**
     * Saves the current editor state
     */
    public void saveState(String scriptName, String code, int caretPosition, int scrollPosition) {
        if (scriptName == null) {
            return;
        }

        EditorState state = getOrCreateState(scriptName);
        state.setCode(code);
        state.setCaretPosition(caretPosition);
        state.setScrollPosition(scrollPosition);
    }

    /**
     * Marks a script as modified
     */
    public void setModified(String scriptName, boolean modified) {
        EditorState state = getState(scriptName);
        if (state != null) {
            state.setModified(modified);
        }
    }

    /**
     * Checks if a script has been modified
     */
    public boolean isModified(String scriptName) {
        EditorState state = getState(scriptName);
        return state != null && state.isModified();
    }

    /**
     * Removes the state for a script (when deleted)
     */
    public void removeState(String scriptName) {
        if (scriptName != null) {
            EditorState state = scriptStates.remove(scriptName);
            if (state != null) {
                state.clearUndoHistory();
            }
        }
    }

    /**
     * Gets the current script name
     */
    public String getCurrentScriptName() {
        return currentScriptName;
    }

    /**
     * Sets the current script name
     */
    public void setCurrentScriptName(String scriptName) {
        this.currentScriptName = scriptName;
    }

    /**
     * Gets all script names with saved states
     */
    public Set<String> getAllScriptNames() {
        return scriptStates.keySet();
    }

    /**
     * Checks if there are any unsaved modifications
     */
    public boolean hasUnsavedChanges() {
        return scriptStates.values().stream().anyMatch(EditorState::isModified);
    }

    /**
     * Gets a list of scripts with unsaved changes
     */
    public java.util.List<String> getModifiedScripts() {
        return scriptStates.entrySet().stream()
                .filter(e -> e.getValue().isModified())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Clears all states (for cleanup)
     */
    public void clearAll() {
        scriptStates.values().forEach(EditorState::clearUndoHistory);
        scriptStates.clear();
        currentScriptName = null;
    }

    /**
     * Initializes state for a script with initial code
     */
    public void initializeState(String scriptName, String initialCode) {
        if (scriptName == null) {
            return;
        }

        EditorState state = new EditorState(initialCode);
        scriptStates.put(scriptName, state);
    }

    /**
     * Renames a script's state
     */
    public void renameScript(String oldName, String newName) {
        if (oldName == null || newName == null || oldName.equals(newName)) {
            return;
        }

        EditorState state = scriptStates.remove(oldName);
        if (state != null) {
            scriptStates.put(newName, state);
        }

        if (oldName.equals(currentScriptName)) {
            currentScriptName = newName;
        }
    }
}