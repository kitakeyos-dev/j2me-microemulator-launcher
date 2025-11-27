package me.kitakeyos.j2me.presentation.common.component;

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

    protected final JLabel messageLabel;

    public StatusBar() {
        setLayout(new BorderLayout());

        // Message label
        messageLabel = new JLabel();
        add(messageLabel, BorderLayout.WEST);
    }

    /**
     * Set status message with type
     */
    public void setStatus(String message, StatusType type) {
        messageLabel.setText(message);
        switch (type) {
            case INFO:
                messageLabel.setForeground(Color.BLUE);
                break;
            case SUCCESS:
                messageLabel.setForeground(new Color(0, 128, 0));
                break;
            case ERROR:
                messageLabel.setForeground(Color.RED);
                break;
            case WARNING:
                messageLabel.setForeground(new Color(255, 140, 0));
                break;
        }
    }

    /**
     * Set info status
     */
    public void setInfo(String message) {
        setStatus(message, StatusType.INFO);
    }

    /**
     * Set success status
     */
    public void setSuccess(String message) {
        setStatus(message, StatusType.SUCCESS);
    }

    /**
     * Set warning status
     */
    public void setWarning(String message) {
        setStatus(message, StatusType.WARNING);
    }

    /**
     * Set error status
     */
    public void setError(String message) {
        setStatus(message, StatusType.ERROR);
    }
}
