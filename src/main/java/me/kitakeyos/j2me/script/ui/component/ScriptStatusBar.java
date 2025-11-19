package me.kitakeyos.j2me.script.ui.component;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Status bar component for the Lua Script Manager.
 * Displays current status, script count, and mode information.
 */
public class ScriptStatusBar extends JPanel {

    private JLabel statusLabel;
    private JLabel scriptCountLabel;
    private JLabel modeLabel;

    public ScriptStatusBar() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 10, 5, 10));

        statusLabel = new JLabel("Ready");
        add(statusLabel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        scriptCountLabel = new JLabel("Scripts: 0");
        modeLabel = new JLabel("Mode: Light");

        rightPanel.add(scriptCountLabel);
        rightPanel.add(new JSeparator(SwingConstants.VERTICAL));
        rightPanel.add(modeLabel);
        add(rightPanel, BorderLayout.EAST);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
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

    public void setSuccess(String message) {
        setStatus(message);
        statusLabel.setForeground(new Color(0, 128, 0));

        Timer timer = new Timer(3000, e -> {
            setReady();
            statusLabel.setForeground(UIManager.getColor("Label.foreground"));
        });
        timer.setRepeats(false);
        timer.start();
    }

    public void setError(String error) {
        setStatus("Error: " + error);
        statusLabel.setForeground(Color.RED);
    }


    public void setInfo(String error) {
        setStatus("Info: " + error);
        statusLabel.setForeground(Color.BLUE);
    }

    public void setWarning(String warning) {
        setStatus("Warning: " + warning);
        statusLabel.setForeground(new Color(255, 140, 0));
    }

    public void showBusy(String message) {
        setStatus(message + "...");
    }
}