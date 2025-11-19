package me.kitakeyos.j2me.ui.component;

import javax.swing.*;
import java.awt.*;

/**
 * Basic status bar for displaying status messages
 */
public class StatusBar extends JPanel {

    public enum StatusType {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    private final JLabel messageLabel;

    public StatusBar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // Message label
        messageLabel = new JLabel("Ready");
        add(messageLabel, BorderLayout.CENTER);
    }

    /**
     * Set status message with type
     */
    public void setStatus(String message, StatusType type) {
        messageLabel.setText(message);
    }

    /**
     * Set info status
     */
    public void setInfoStatus(String message) {
        setStatus(message, StatusType.INFO);
    }

    /**
     * Set success status
     */
    public void setSuccessStatus(String message) {
        setStatus(message, StatusType.SUCCESS);
    }

    /**
     * Set warning status
     */
    public void setWarningStatus(String message) {
        setStatus(message, StatusType.WARNING);
    }

    /**
     * Set error status
     */
    public void setErrorStatus(String message) {
        setStatus(message, StatusType.ERROR);
    }

    /**
     * Clear status to default
     */
    public void clearStatus() {
        setStatus("Ready", StatusType.INFO);
    }
}
