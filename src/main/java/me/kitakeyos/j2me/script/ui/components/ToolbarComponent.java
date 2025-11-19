package me.kitakeyos.j2me.script.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Reusable toolbar component for script manager
 */
public class ToolbarComponent extends JPanel {

    public interface ToolbarActions {
        void onNewScript();
        void onSaveScript();
        void onDeleteScript();
        void onRunScript();
    }

    private JButton newBtn;
    private JButton saveBtn;
    private JButton deleteBtn;
    private JButton runBtn;

    public ToolbarComponent(ToolbarActions actions) {
        initializeUI();
        setupActions(actions);
    }

    private void initializeUI() {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setBorder(new EmptyBorder(5, 10, 5, 10));

        newBtn = new JButton("New");
        saveBtn = new JButton("Save");
        deleteBtn = new JButton("Delete");
        runBtn = new JButton("Run");

        add(newBtn);
        add(saveBtn);
        add(deleteBtn);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(runBtn);
    }

    private void setupActions(ToolbarActions actions) {
        if (actions != null) {
            newBtn.addActionListener(e -> actions.onNewScript());
            saveBtn.addActionListener(e -> actions.onSaveScript());
            deleteBtn.addActionListener(e -> actions.onDeleteScript());
            runBtn.addActionListener(e -> actions.onRunScript());
        }
    }

    public void setButtonEnabled(String buttonName, boolean enabled) {
        switch (buttonName.toLowerCase()) {
            case "new":
                newBtn.setEnabled(enabled);
                break;
            case "save":
                saveBtn.setEnabled(enabled);
                break;
            case "delete":
                deleteBtn.setEnabled(enabled);
                break;
            case "run":
                runBtn.setEnabled(enabled);
                break;
        }
    }

    public void setAllButtonsEnabled(boolean enabled) {
        newBtn.setEnabled(enabled);
        saveBtn.setEnabled(enabled);
        deleteBtn.setEnabled(enabled);
        runBtn.setEnabled(enabled);
    }
}