package me.kitakeyos.j2me.script.ui;

import me.kitakeyos.j2me.script.core.LuaScriptExecutor;
import me.kitakeyos.j2me.script.model.LuaScript;
import me.kitakeyos.j2me.script.storage.ScriptFileManager;
import me.kitakeyos.j2me.script.ui.components.OutputPanelComponent;
import me.kitakeyos.j2me.script.ui.components.ScriptListComponent;
import me.kitakeyos.j2me.script.ui.components.StatusBarComponent;
import me.kitakeyos.j2me.script.ui.components.ToolbarComponent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Clean, organized Lua Script Manager using component architecture
 */
public class LuaScriptManager extends JPanel
        implements ToolbarComponent.ToolbarActions, ScriptListComponent.ScriptSelectionListener {

    // UI Components
    private ToolbarComponent toolbar;
    private ScriptListComponent scriptList;
    private LuaCodeEditor codeEditor;
    private OutputPanelComponent outputPanel;
    private StatusBarComponent statusBar;

    // Data and services
    private Map<String, LuaScript> scripts;
    private LuaScriptExecutor scriptExecutor;
    private ScriptFileManager fileManager;

    // State
    private boolean isDarkMode = false;
    private boolean syntaxHighlightEnabled = true;

    public LuaScriptManager() {
        this.scripts = new HashMap<>();
        this.fileManager = new ScriptFileManager();

        initializeComponents();
        initializeServices();
        layoutComponents();
        loadScripts();
    }

    private void initializeComponents() {
        // Create all UI components
        toolbar = new ToolbarComponent(this);
        scriptList = new ScriptListComponent(this);
        outputPanel = new OutputPanelComponent();
        statusBar = new StatusBarComponent();

        // Create code editor with document listener
        codeEditor = new LuaCodeEditor(isDarkMode, syntaxHighlightEnabled,
                new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        handleEditorUpdate(e);
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        handleEditorUpdate(e);
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        handleEditorUpdate(e);
                    }
                });
    }

    private void initializeServices() {
        scriptExecutor = new LuaScriptExecutor(
                outputPanel::appendNormal,
                outputPanel::appendError,
                outputPanel::appendSuccess,
                outputPanel::appendInfo
        );
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(300);
        mainSplit.setBorder(new EmptyBorder(5, 10, 5, 10));

        mainSplit.setLeftComponent(scriptList);

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplit.setDividerLocation(450);
        rightSplit.setTopComponent(codeEditor.getEditorPanel());
        rightSplit.setBottomComponent(outputPanel);

        mainSplit.setRightComponent(rightSplit);
        add(mainSplit, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        updateTheme();
    }

    private void updateTheme() {
        statusBar.setMode(isDarkMode ? "Dark" : "Light");
        outputPanel.setDarkMode(isDarkMode);
        if (codeEditor != null) {
            codeEditor.setDarkMode(isDarkMode);
        }
    }

    private void handleEditorUpdate(DocumentEvent e) {
        if (syntaxHighlightEnabled && codeEditor != null) {
            codeEditor.getSyntaxHighlighter().handleDocumentUpdate(e.getOffset(), e.getLength());
        }
    }

    // ToolbarActions Implementation
    @Override
    public void onNewScript() {
        String name = JOptionPane.showInputDialog(this, "Enter script name:");
        if (name != null && !name.trim().isEmpty()) {
            name = name.trim();
            if (scripts.containsKey(name)) {
                JOptionPane.showMessageDialog(this, "Script name already exists!");
                return;
            }

            LuaScript script = new LuaScript(name, generateTemplate(name));
            scripts.put(name, script);
            scriptList.addScript(name);
            statusBar.setScriptCount(scripts.size());
            statusBar.setSuccess("Created: " + name);
        }
    }

    @Override
    public void onSaveScript() {
        String selected = scriptList.getSelectedScriptName();
        if (selected == null) {
            statusBar.setWarning("No script selected");
            return;
        }

        LuaScript script = scripts.get(selected);
        if (script != null) {
            script.setCode(codeEditor.getText());
            fileManager.saveScriptToFile(script);
            statusBar.setSuccess("Saved: " + selected);
        }
    }

    @Override
    public void onDeleteScript() {
        String selected = scriptList.getSelectedScriptName();
        if (selected == null) {
            statusBar.setWarning("No script selected");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete '" + selected + "'?", "Confirm", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            scripts.remove(selected);
            scriptList.removeScript(selected);
            codeEditor.setText("");
            fileManager.deleteScriptFiles(selected);
            statusBar.setScriptCount(scripts.size());
            statusBar.setSuccess("Deleted script");
        }
    }

    @Override
    public void onRunScript() {
        String selected = scriptList.getSelectedScriptName();
        if (selected == null) {
            outputPanel.appendError("No script selected");
            return;
        }

        LuaScript script = scripts.get(selected);
        if (script != null) {
            script.setCode(codeEditor.getText());
            outputPanel.clearOutput();
            outputPanel.appendInfo("Executing: " + selected);
            statusBar.showBusy("Executing script");

            SwingUtilities.invokeLater(() -> {
                try {
                    onSaveScript();
                    scriptExecutor.executeScript(selected);
                    statusBar.setSuccess("Execution completed");
                } catch (Exception e) {
                    outputPanel.appendError("Execution failed: " + e.getMessage());
                    statusBar.setError("Execution failed");
                }
            });
        }
    }

    // ScriptSelectionListener Implementation
    @Override
    public void onScriptSelected(String scriptName) {
        if (scriptName != null) {
            LuaScript script = scripts.get(scriptName);
            if (script != null && codeEditor != null) {
                codeEditor.setText(script.getCode());
                statusBar.setStatus("Selected: " + scriptName);
            }
        } else {
            if (codeEditor != null) {
                codeEditor.setText("");
            }
            statusBar.setReady();
        }
    }

    // Utility methods
    private void loadScripts() {
        statusBar.showBusy("Loading scripts");
        scripts.clear();
        scripts = fileManager.loadScripts();
        scriptList.loadScripts(scripts);
        statusBar.setScriptCount(scripts.size());
        outputPanel.appendInfo("Loaded " + scripts.size() + " scripts");

        if (!scripts.isEmpty()) {
            scriptList.selectFirstScript();
        }
        statusBar.setReady();
    }

    private String generateTemplate(String name) {
        return "-- " + name + "\nprint(\"Hello from " + name + "!\")\n";
    }

    // Public API
    public void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        updateTheme();
        statusBar.setSuccess("Switched to " + (isDarkMode ? "dark" : "light") + " mode");
    }

    public void toggleSyntaxHighlighting() {
        syntaxHighlightEnabled = !syntaxHighlightEnabled;
        if (codeEditor != null) {
            codeEditor.toggleSyntaxHighlighting();
        }
        statusBar.setSuccess("Syntax highlighting " + (syntaxHighlightEnabled ? "enabled" : "disabled"));
    }
}