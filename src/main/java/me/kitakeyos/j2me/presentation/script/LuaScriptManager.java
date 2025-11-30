package me.kitakeyos.j2me.presentation.script;

import me.kitakeyos.j2me.application.MainApplication;
import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.application.script.state.EditorState;
import me.kitakeyos.j2me.application.script.state.EditorStateManager;
import me.kitakeyos.j2me.domain.application.service.ApplicationService;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.script.executor.LuaScriptExecutor;
import me.kitakeyos.j2me.domain.script.model.LuaScript;
import me.kitakeyos.j2me.infrastructure.persistence.script.ScriptFileManager;
import me.kitakeyos.j2me.presentation.common.component.BaseTabPanel;
import me.kitakeyos.j2me.presentation.common.component.StatusBar;
import me.kitakeyos.j2me.presentation.script.component.OutputPanel;
import me.kitakeyos.j2me.presentation.script.component.ScriptList;
import me.kitakeyos.j2me.presentation.script.component.ScriptStatusBar;
import me.kitakeyos.j2me.presentation.script.component.ScriptToolbar;
import me.kitakeyos.j2me.presentation.script.editor.LuaCodeEditor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Enhanced Lua Script Manager with:
 * - State preservation when switching between scripts (code, caret, scroll,
 * undo history)
 * - Undo/Redo per script
 * - Keyboard shortcuts (Ctrl+S to save, Ctrl+Z/Y for undo/redo)
 * - Modified indicator
 * - Folder support
 */
public class LuaScriptManager extends BaseTabPanel
        implements ScriptToolbar.ToolbarActions, ScriptList.ScriptActionListener {

    // UI Components
    private ScriptToolbar toolbar;
    private ScriptList scriptList;
    private LuaCodeEditor codeEditor;
    private OutputPanel outputPanel;
    private ScriptStatusBar statusBar;
    private JComboBox<String> instanceSelector;

    // Data and services
    private Map<String, LuaScript> scripts;
    private LuaScriptExecutor scriptExecutor;
    private ScriptFileManager fileManager;

    // State management
    private EditorStateManager stateManager;
    private String currentScriptName;
    private boolean isLoadingScript = false; // Flag to avoid loop when loading script

    // Settings
    private boolean isDarkMode;
    private boolean syntaxHighlightEnabled;

    public LuaScriptManager(MainApplication mainApplication, ApplicationConfig applicationConfig,
            ApplicationService j2meApplicationManager) {
        super(mainApplication, applicationConfig, j2meApplicationManager);
        refreshInstanceList();
    }

    @Override
    protected JComponent createHeader() {
        toolbar = new ScriptToolbar(this);

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.add(toolbar, BorderLayout.CENTER);

        // Instance selector panel
        JPanel instancePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        instancePanel.add(new JLabel("Target Instance:"));

        instanceSelector = new JComboBox<>();
        instanceSelector.setPreferredSize(new Dimension(150, 25));
        instanceSelector.setToolTipText("Select running instance for script execution");
        instanceSelector.addActionListener(e -> onInstanceSelected());
        instancePanel.add(instanceSelector);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setToolTipText("Refresh instance list");
        refreshButton.addActionListener(e -> refreshInstanceList());
        instancePanel.add(refreshButton);

        topPanel.add(instancePanel, BorderLayout.EAST);

        return topPanel;
    }

    @Override
    protected JComponent createContent() {
        // Initialize state manager
        stateManager = new EditorStateManager();

        // Initialize script list
        scriptList = new ScriptList(this);

        // Initialize output panel
        outputPanel = new OutputPanel();

        this.isDarkMode = false;
        this.syntaxHighlightEnabled = true;

        // Create code editor with document listener to track modifications
        codeEditor = new LuaCodeEditor(isDarkMode, syntaxHighlightEnabled,
                new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        if (!isLoadingScript) {
                            handleEditorUpdate();
                        }
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        if (!isLoadingScript) {
                            handleEditorUpdate();
                        }
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        // Style changes, ignore
                    }
                });

        // Set up Ctrl+S callback
        codeEditor.setOnSaveCallback(this::onSaveScript);

        // Main split pane
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(300);
        mainSplit.setLeftComponent(scriptList);

        // Right split pane for editor and output
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplit.setDividerLocation(450);
        rightSplit.setTopComponent(codeEditor.getEditorPanel());
        rightSplit.setBottomComponent(outputPanel);

        mainSplit.setRightComponent(rightSplit);

        return mainSplit;
    }

    @Override
    protected StatusBar createStatusBar() {
        statusBar = new ScriptStatusBar();
        return statusBar;
    }

    @Override
    protected void onInitialized() {
        fileManager = new ScriptFileManager(applicationConfig);

        // Initialize services
        scriptExecutor = new LuaScriptExecutor(
                fileManager,
                outputPanel::appendNormal,
                outputPanel::appendError,
                outputPanel::appendSuccess,
                outputPanel::appendInfo);

        // Update theme
        updateTheme();

        // Load scripts
        loadScripts();
    }

    private void updateTheme() {
        statusBar.setMode(isDarkMode ? "Dark" : "Light");
        outputPanel.setDarkMode(isDarkMode);
        if (codeEditor != null) {
            codeEditor.setDarkMode(isDarkMode);
        }
    }

    /**
     * Called when editor content changes
     */
    private void handleEditorUpdate() {
        if (currentScriptName != null && !isLoadingScript) {
            stateManager.setModified(currentScriptName, true);
            updateTitleWithModifiedIndicator();
        }
    }

    private void updateTitleWithModifiedIndicator() {
        if (currentScriptName != null) {
            boolean modified = stateManager.isModified(currentScriptName);
            String title = modified ? currentScriptName + " *" : currentScriptName;
            statusBar.setStatus("Editing: " + title);
        }
    }

    // ========== ToolbarActions Implementation ==========

    @Override
    public void onNewScript() {
        String parentPath = scriptList.getSelectedFolderPath();
        onNewScript(parentPath);
    }

    @Override
    public void onNewFolder() {
        String parentPath = scriptList.getSelectedFolderPath();
        onNewFolder(parentPath);
    }

    @Override
    public void onSaveScript() {
        if (currentScriptName == null) {
            statusBar.setWarning("No script selected");
            return;
        }

        LuaScript script = scripts.get(currentScriptName);
        if (script != null) {
            // Get current code from editor
            String code = codeEditor.getText();
            script.setCode(code);

            // Update state
            EditorState state = stateManager.getState(currentScriptName);
            if (state != null) {
                state.setCode(code);
                state.setModified(false);
            }

            // Save to file
            fileManager.saveScriptToFile(script);

            codeEditor.setModified(false);
            statusBar.setSuccess("Saved: " + currentScriptName);
            updateTitleWithModifiedIndicator();
        }
    }

    @Override
    public void onDeleteScript() {
        String selectedPath = scriptList.getSelectedPath();
        boolean isFolder = scriptList.isSelectedFolder();

        if (selectedPath == null) {
            if (currentScriptName != null) {
                // Fallback to current script if nothing selected in tree
                onDelete(currentScriptName, false);
            } else {
                statusBar.setWarning("No script or folder selected");
            }
            return;
        }

        onDelete(selectedPath, isFolder);
    }

    @Override
    public void onRunScript() {
        if (currentScriptName == null) {
            outputPanel.appendError("No script selected");
            return;
        }

        LuaScript script = scripts.get(currentScriptName);
        if (script != null) {
            // Auto-save before running
            script.setCode(codeEditor.getText());

            outputPanel.clearOutput();
            outputPanel.appendInfo("Executing: " + currentScriptName);
            statusBar.showBusy("Executing script");

            SwingUtilities.invokeLater(() -> {
                try {
                    onSaveScript();
                    scriptExecutor.executeScript(currentScriptName);
                    statusBar.setSuccess("Execution completed");
                } catch (Exception e) {
                    outputPanel.appendError("Execution failed: " + e.getMessage());
                    statusBar.setError("Execution failed");
                }
            });
        }
    }

    // ========== ScriptActionListener Implementation ==========

    @Override
    public void onScriptSelected(String scriptName) {
        // Avoid processing if loading
        if (isLoadingScript) {
            return;
        }

        // If selecting the same script, do nothing
        if (scriptName != null && scriptName.equals(currentScriptName)) {
            return;
        }

        // IMPORTANT: Save state of current script BEFORE switching
        saveCurrentScriptState();

        // Update current script
        currentScriptName = scriptName;
        stateManager.setCurrentScriptName(scriptName);

        if (scriptName != null) {
            LuaScript script = scripts.get(scriptName);
            if (script != null && codeEditor != null) {
                isLoadingScript = true;
                try {
                    // Get or create state for this script
                    EditorState state = stateManager.getOrCreateState(scriptName);

                    // If state has no code, initialize from script
                    if (state.getCode() == null || state.getCode().isEmpty()) {
                        state.setCode(script.getCode());
                    }

                    // Restore editor from state (including code, caret, scroll, undo manager)
                    codeEditor.restoreFromState(state);

                    updateTitleWithModifiedIndicator();
                } finally {
                    isLoadingScript = false;
                }
            }
        } else {
            if (codeEditor != null) {
                codeEditor.clearEditor();
            }
            statusBar.setReady();
        }
    }

    @Override
    public void onNewScript(String folderPath) {
        String name = JOptionPane.showInputDialog(this, "Enter script name:");
        if (name != null && !name.trim().isEmpty()) {
            name = name.trim();

            String fullPath = folderPath.isEmpty() ? name : folderPath + "/" + name;

            if (scripts.containsKey(fullPath)) {
                JOptionPane.showMessageDialog(this, "Script name already exists!");
                return;
            }

            // Save current script state before creating new one
            saveCurrentScriptState();

            String templateCode = generateTemplate(name);
            LuaScript script = new LuaScript(fullPath, templateCode);
            scripts.put(fullPath, script);

            // Save immediately to ensure file exists
            fileManager.saveScriptToFile(script);

            // Reload to update tree
            loadScripts();

            // Select the new script
            onScriptSelected(fullPath);

            statusBar.setScriptCount(scripts.size());
            statusBar.setSuccess("Created: " + fullPath);
        }
    }

    @Override
    public void onNewFolder(String parentPath) {
        String name = JOptionPane.showInputDialog(this, "Enter folder name:");
        if (name != null && !name.trim().isEmpty()) {
            name = name.trim();
            String fullPath = parentPath.isEmpty() ? name : parentPath + "/" + name;

            if (fileManager.createFolder(fullPath)) {
                loadScripts();
                statusBar.setSuccess("Created folder: " + fullPath);
            } else {
                statusBar.setError("Failed to create folder: " + fullPath);
            }
        }
    }

    @Override
    public void onDelete(String path, boolean isFolder) {
        if (isFolder) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete folder '" + path + "' and all its contents?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                // If current script was in deleted folder, clear editor BEFORE deletion
                // to release any potential file locks (though editor shouldn't hold any)
                if (currentScriptName != null
                        && (currentScriptName.equals(path) || currentScriptName.startsWith(path + "/"))) {
                    currentScriptName = null;
                    codeEditor.clearEditor();
                }

                if (fileManager.deleteFolder(path)) {
                    loadScripts();
                    statusBar.setSuccess("Deleted folder: " + path);
                } else {
                    statusBar.setError("Failed to delete folder: " + path);
                }
            }
        } else {
            // Script deletion
            if (stateManager.isModified(path)) {
                int saveFirst = JOptionPane.showConfirmDialog(this,
                        "'" + path + "' has unsaved changes. Save before deleting?",
                        "Unsaved Changes",
                        JOptionPane.YES_NO_CANCEL_OPTION);

                if (saveFirst == JOptionPane.YES_OPTION) {
                    // Save logic here if needed, but we are deleting...
                } else if (saveFirst == JOptionPane.CANCEL_OPTION) {
                    return;
                }
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete '" + path + "'?", "Confirm", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                scripts.remove(path);
                stateManager.removeState(path);
                fileManager.deleteScriptFiles(path);

                if (path.equals(currentScriptName)) {
                    currentScriptName = null;
                    codeEditor.clearEditor();
                }

                loadScripts();
                statusBar.setSuccess("Deleted: " + path);
            }
        }
    }

    @Override
    public void onRename(String path, boolean isFolder) {
        String newName = JOptionPane.showInputDialog(this, "Enter new name:",
                path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path);

        if (newName != null && !newName.trim().isEmpty()) {
            newName = newName.trim();
            String parentPath = path.contains("/") ? path.substring(0, path.lastIndexOf("/")) : "";
            String newPath = parentPath.isEmpty() ? newName : parentPath + "/" + newName;

            if (path.equals(newPath))
                return;

            if (fileManager.renameScript(path, newPath)) {
                // Update state manager if script was renamed
                if (!isFolder) {
                    EditorState state = stateManager.getState(path);
                    if (state != null) {
                        stateManager.removeState(path);
                        // We need to re-initialize state for new path
                        // But loadScripts will handle it
                    }
                    if (path.equals(currentScriptName)) {
                        currentScriptName = newPath;
                    }
                } else {
                    // If folder renamed, we might need to update currentScriptName if it was inside
                    if (currentScriptName != null && currentScriptName.startsWith(path + "/")) {
                        currentScriptName = currentScriptName.replaceFirst(path, newPath);
                    }
                }

                loadScripts();

                // Reselect if needed
                if (currentScriptName != null) {
                    onScriptSelected(currentScriptName);
                }

                statusBar.setSuccess("Renamed to: " + newPath);
            } else {
                statusBar.setError("Failed to rename");
            }
        }
    }

    /**
     * Save state of current script
     */
    private void saveCurrentScriptState() {
        if (currentScriptName != null && codeEditor != null && !isLoadingScript) {
            EditorState state = stateManager.getOrCreateState(currentScriptName);
            codeEditor.saveToState(state);

            // Update code in LuaScript object to sync
            LuaScript script = scripts.get(currentScriptName);
            if (script != null) {
                script.setCode(state.getCode());
            }
        }
    }

    // ========== Utility Methods ==========

    private void loadScripts() {
        statusBar.showBusy("Loading scripts");
        scripts = fileManager.loadScripts();
        List<String> folders = fileManager.getAllFolders();

        scriptList.loadScripts(scripts);
        scriptList.loadFolders(folders);

        statusBar.setScriptCount(scripts.size());
        outputPanel.appendInfo("Loaded " + scripts.size() + " scripts");

        // Initialize states for all scripts
        for (Map.Entry<String, LuaScript> entry : scripts.entrySet()) {
            stateManager.initializeState(entry.getKey(), entry.getValue().getCode());
        }

        if (currentScriptName != null && scripts.containsKey(currentScriptName)) {
            // Keep selection
        } else if (!scripts.isEmpty()) {
            scriptList.selectFirstScript();
        } else {
            codeEditor.setText("-- No scripts available. Click 'New' to create one.");
            statusBar.setInfo("No scripts available");
        }
        statusBar.setReady();
    }

    private String generateTemplate(String name) {
        return "-- " + name + "\n" +
                "-- Created: " + java.time.LocalDate.now() + "\n\n" +
                "-- Your Lua code here\n" +
                "print(\"Hello from " + name + "!\")\n";
    }

    /**
     * Checks if there are unsaved changes and prompts user
     * 
     * @return true if it's okay to proceed, false if user cancelled
     */
    public boolean checkUnsavedChanges() {
        // Save current state first
        saveCurrentScriptState();

        List<String> modifiedScripts = stateManager.getModifiedScripts();
        if (modifiedScripts.isEmpty()) {
            return true;
        }

        String message = modifiedScripts.size() == 1
                ? "Script '" + modifiedScripts.get(0) + "' has unsaved changes."
                : modifiedScripts.size() + " scripts have unsaved changes.";

        int result = JOptionPane.showConfirmDialog(this,
                message + "\nDo you want to save before closing?",
                "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            // Save all modified scripts
            for (String scriptName : modifiedScripts) {
                LuaScript script = scripts.get(scriptName);
                EditorState state = stateManager.getState(scriptName);
                if (script != null && state != null) {
                    script.setCode(state.getCode());
                    fileManager.saveScriptToFile(script);
                }
            }
            return true;
        } else if (result == JOptionPane.NO_OPTION) {
            return true;
        } else {
            return false; // Cancel
        }
    }

    /**
     * Refresh the list of running instances
     */
    public void refreshInstanceList() {
        instanceSelector.removeAllItems();
        instanceSelector.addItem("None (default ClassLoader)");

        if (mainApplication.emulatorInstanceManager != null) {
            java.util.List<EmulatorInstance> runningInstances = mainApplication.emulatorInstanceManager
                    .getRunningInstances();

            for (EmulatorInstance instance : runningInstances) {
                instanceSelector.addItem("Instance #" + instance.getInstanceId());
            }

            if (!runningInstances.isEmpty()) {
                statusBar.setInfo("Found " + runningInstances.size() + " running instance(s)");
            } else {
                statusBar.setWarning("No running instances found");
            }
        }
    }

    private void onInstanceSelected() {
        String selected = (String) instanceSelector.getSelectedItem();
        if (selected == null || selected.equals("None (default ClassLoader)")) {
            scriptExecutor.setInstanceClassLoader(null);
            statusBar.setInfo("Using default ClassLoader");
            return;
        }

        try {
            int instanceId = Integer.parseInt(selected.replace("Instance #", ""));
            EmulatorInstance instance = findInstanceById(instanceId);

            if (instance != null && instance.getAppClassLoader() != null) {
                scriptExecutor.setInstanceClassLoader(instance.getAppClassLoader());
                statusBar.setSuccess("Using ClassLoader from Instance #" + instanceId);
            } else {
                statusBar.setWarning("Instance #" + instanceId + " has no ClassLoader");
                scriptExecutor.setInstanceClassLoader(null);
            }
        } catch (NumberFormatException e) {
            statusBar.setError("Invalid instance selection");
            scriptExecutor.setInstanceClassLoader(null);
        }
    }

    private EmulatorInstance findInstanceById(int instanceId) {
        if (mainApplication.emulatorInstanceManager != null) {
            java.util.List<EmulatorInstance> runningInstances = mainApplication.emulatorInstanceManager
                    .getRunningInstances();

            for (EmulatorInstance instance : runningInstances) {
                if (instance.getInstanceId() == instanceId) {
                    return instance;
                }
            }
        }
        return null;
    }

    /**
     * Gets the current script being edited
     */
    public String getCurrentScriptName() {
        return currentScriptName;
    }

    /**
     * Gets the editor state manager
     */
    public EditorStateManager getStateManager() {
        return stateManager;
    }

    /**
     * Gets the code editor
     */
    public LuaCodeEditor getCodeEditor() {
        return codeEditor;
    }
}