package me.kitakeyos.j2me.script.ui.component;

import me.kitakeyos.j2me.ui.component.StatusBar;

import javax.swing.*;
import java.awt.*;

/**
 * Status bar component for the Lua Script Manager.
 * Displays current status, script count, and mode information.
 */
public class ScriptStatusBar extends StatusBar {

    private JLabel scriptCountLabel;
    private JLabel modeLabel;

    public ScriptStatusBar() {
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        scriptCountLabel = new JLabel("Scripts: 0");
        modeLabel = new JLabel("Mode: Light");

        rightPanel.add(scriptCountLabel);
        rightPanel.add(new JSeparator(SwingConstants.VERTICAL));
        rightPanel.add(modeLabel);
        add(rightPanel, BorderLayout.EAST);
    }

    public void setStatus(String status) {
        messageLabel.setText(status);
    }

    public void setScriptCount(int count) {
        scriptCountLabel.setText("Scripts: " + count);
    }

    public void setMode(String mode) {
        modeLabel.setText("Mode: " + mode);
    }

    public void setReady() {
        setStatus("Ready");
    }

    public void showBusy(String message) {
        setStatus(message + "...");
    }
}