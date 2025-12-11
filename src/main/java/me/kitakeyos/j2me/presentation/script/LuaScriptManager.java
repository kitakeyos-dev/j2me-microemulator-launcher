package me.kitakeyos.j2me.presentation.script;

import me.kitakeyos.j2me.application.MainApplication;
import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.application.script.LuaScriptService;
import me.kitakeyos.j2me.application.script.state.EditorState;
import me.kitakeyos.j2me.domain.application.service.ApplicationService;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
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
 * 
 * This class is now focused on UI concerns only.
 * Business logic is delegated to LuaScriptService.
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

    // Application Service (business logic)
    private LuaScriptService scriptService;

    // UI state
    private boolean isLoadingScript = false;

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
        // Initialize the application service with output panel callbacks
        ScriptFileManager fileManager = new ScriptFileManager(applicationConfig);
        scriptService = new LuaScriptService(
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
        LuaScript currentScript = scriptService.getCurrentScript();
        if (currentScript != null && !isLoadingScript) {
            String scriptName = scriptService.getScriptKey(currentScript);
            scriptService.setModified(scriptName, true);
            updateTitleWithModifiedIndicator();
        }
    }

    private void updateTitleWithModifiedIndicator() {
        LuaScript currentScript = scriptService.getCurrentScript();
        if (currentScript != null) {
            String scriptName = scriptService.getScriptKey(currentScript);
            boolean modified = scriptService.isModified(scriptName);
            String title = modified ? scriptName + " *" : scriptName;
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
        LuaScript currentScript = scriptService.getCurrentScript();
        if (currentScript == null) {
            statusBar.setWarning("No script selected");
            return;
        }

        String code = codeEditor.getText();
        String scriptName = scriptService.getScriptKey(currentScript);

        scriptService.saveScript(currentScript, code);

        codeEditor.setModified(false);
        statusBar.setSuccess("Saved: " + scriptName);
        updateTitleWithModifiedIndicator();
    }

    @Override
    public void onDeleteScript() {
        String selectedPath = scriptList.getSelectedPath();
        boolean isFolder = scriptList.isSelectedFolder();

        if (selectedPath == null) {
            LuaScript currentScript = scriptService.getCurrentScript();
            if (currentScript != null) {
                onDelete(scriptService.getScriptKey(currentScript), false);
            } else {
                statusBar.setWarning("No script or folder selected");
            }
            return;
        }

        onDelete(selectedPath, isFolder);
    }

    @Override
    public void onRunScript() {
        LuaScript currentScript = scriptService.getCurrentScript();
        if (currentScript == null) {
            outputPanel.appendError("No script selected");
            return;
        }

        // Auto-save before running
        currentScript.setCode(codeEditor.getText());

        outputPanel.clearOutput();
        outputPanel.appendInfo("Executing: " + currentScript.getName());
        statusBar.showBusy("Executing script");

        SwingUtilities.invokeLater(() -> {
            try {
                onSaveScript();
                scriptService.executeScript(currentScript);
                statusBar.setSuccess("Execution completed");
            } catch (Exception e) {
                outputPanel.appendError("Execution failed: " + e.getMessage());
                statusBar.setError("Execution failed");
            }
        });
    }

    // ========== ScriptActionListener Implementation ==========

    @Override
    public void onScriptSelected(String scriptName) {
        if (isLoadingScript) {
            return;
        }

        LuaScript currentScript = scriptService.getCurrentScript();
        if (currentScript != null && scriptName != null
                && scriptName.equals(scriptService.getScriptKey(currentScript))) {
            return;
        }

        // Save state of current script BEFORE switching
        saveCurrentScriptState();

        if (scriptName != null) {
            LuaScript script = scriptService.getScript(scriptName);
            if (script != null) {
                scriptService.setCurrentScript(script);

                if (codeEditor != null) {
                    isLoadingScript = true;
                    try {
                        EditorState state = scriptService.getOrCreateState(scriptName);

                        if (state.getCode() == null || state.getCode().isEmpty()) {
                            state.setCode(script.getCode());
                        }

                        codeEditor.restoreFromState(state);
                        updateTitleWithModifiedIndicator();
                    } finally {
                        isLoadingScript = false;
                    }
                }
            }
        } else {
            scriptService.setCurrentScript(null);
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
            if (!fullPath.endsWith(".lua")) {
                fullPath += ".lua";
            }

            if (scriptService.getScript(fullPath) != null) {
                JOptionPane.showMessageDialog(this, "Script name already exists!");
                return;
            }

            saveCurrentScriptState();

            LuaScript script = scriptService.createScript(folderPath, name);
            if (script != null) {
                scriptList.addScript(fullPath);
                onScriptSelected(fullPath);

                statusBar.setScriptCount(scriptService.getScriptCount());
                statusBar.setSuccess("Created: " + fullPath);
            }
        }
    }

    @Override
    public void onNewFolder(String parentPath) {
        String name = JOptionPane.showInputDialog(this, "Enter folder name:");
        if (name != null && !name.trim().isEmpty()) {
            name = name.trim();
            String fullPath = parentPath.isEmpty() ? name : parentPath + "/" + name;

            if (scriptService.createFolder(parentPath, name)) {
                scriptList.addFolder(fullPath);
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
                LuaScript currentScript = scriptService.getCurrentScript();
                if (currentScript != null) {
                    String currentKey = scriptService.getScriptKey(currentScript);
                    if (currentKey != null && (currentKey.equals(path) || currentKey.startsWith(path + "/"))) {
                        codeEditor.clearEditor();
                    }
                }

                if (scriptService.deletePath(path, true)) {
                    scriptList.removeFolder(path);
                    statusBar.setScriptCount(scriptService.getScriptCount());
                    statusBar.setSuccess("Deleted folder: " + path);
                } else {
                    statusBar.setError("Failed to delete folder: " + path);
                }
            }
        } else {
            if (scriptService.isModified(path)) {
                int saveFirst = JOptionPane.showConfirmDialog(this,
                        "'" + path + "' has unsaved changes. Save before deleting?",
                        "Unsaved Changes",
                        JOptionPane.YES_NO_CANCEL_OPTION);

                if (saveFirst == JOptionPane.CANCEL_OPTION) {
                    return;
                }
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete '" + path + "'?", "Confirm", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                LuaScript currentScript = scriptService.getCurrentScript();
                if (currentScript != null && path.equals(scriptService.getScriptKey(currentScript))) {
                    codeEditor.clearEditor();
                }

                if (scriptService.deletePath(path, false)) {
                    scriptList.removeScript(path);
                    statusBar.setScriptCount(scriptService.getScriptCount());
                    statusBar.setSuccess("Deleted: " + path);
                } else {
                    statusBar.setError("Failed to delete: " + path);
                }
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

            LuaScript currentScript = scriptService.getCurrentScript();
            String oldCurrentKey = currentScript != null ? scriptService.getScriptKey(currentScript) : null;

            if (scriptService.renamePath(path, newPath, isFolder)) {
                scriptList.renameNode(path, newPath);

                if (oldCurrentKey != null) {
                    if (!isFolder && oldCurrentKey.equals(path)) {
                        onScriptSelected(newPath);
                    } else if (isFolder && oldCurrentKey.startsWith(path + "/")) {
                        String newKey = newPath + oldCurrentKey.substring(path.length());
                        onScriptSelected(newKey);
                    }
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
        LuaScript currentScript = scriptService.getCurrentScript();
        if (currentScript != null && codeEditor != null && !isLoadingScript) {
            String scriptName = scriptService.getScriptKey(currentScript);
            EditorState state = scriptService.getOrCreateState(scriptName);
            codeEditor.saveToState(state);
            currentScript.setCode(state.getCode());
        }
    }

    // ========== Utility Methods ==========

    private void loadScripts() {
        statusBar.showBusy("Loading scripts");
        scriptService.loadScripts();

        Map<String, LuaScript> scripts = scriptService.getScripts();
        List<String> folders = scriptService.getAllFolders();

        scriptList.loadScripts(scripts);
        scriptList.loadFolders(folders);

        statusBar.setScriptCount(scriptService.getScriptCount());
        outputPanel.appendInfo("Loaded " + scriptService.getScriptCount() + " scripts");

        if (scripts.isEmpty()) {
            codeEditor.setText("-- No scripts available. Click 'New' to create one.");
            statusBar.setInfo("No scripts available");
        } else {
            scriptList.selectFirstScript();
        }
        statusBar.setReady();
    }

    /**
     * Checks if there are unsaved changes and prompts user
     *
     * @return true if it's okay to proceed, false if user cancelled
     */
    public boolean checkUnsavedChanges() {
        saveCurrentScriptState();

        List<String> modifiedScripts = scriptService.getModifiedScripts();
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
            for (String scriptName : modifiedScripts) {
                LuaScript script = scriptService.getScript(scriptName);
                EditorState state = scriptService.getState(scriptName);
                if (script != null && state != null) {
                    scriptService.saveScript(script, state.getCode());
                }
            }
            return true;
        } else if (result == JOptionPane.NO_OPTION) {
            return true;
        } else {
            return false;
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
            scriptService.setInstanceClassLoader(null);
            statusBar.setInfo("Using default ClassLoader");
            return;
        }

        try {
            int instanceId = Integer.parseInt(selected.replace("Instance #", ""));
            EmulatorInstance instance = findInstanceById(instanceId);

            if (instance != null && instance.getAppClassLoader() != null) {
                scriptService.setInstanceClassLoader(instance.getAppClassLoader());
                statusBar.setSuccess("Using ClassLoader from Instance #" + instanceId);
            } else {
                statusBar.setWarning("Instance #" + instanceId + " has no ClassLoader");
                scriptService.setInstanceClassLoader(null);
            }
        } catch (NumberFormatException e) {
            statusBar.setError("Invalid instance selection");
            scriptService.setInstanceClassLoader(null);
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
    public LuaScript getCurrentScript() {
        return scriptService.getCurrentScript();
    }

    /**
     * Gets the code editor
     */
    public LuaCodeEditor getCodeEditor() {
        return codeEditor;
    }
}