package me.kitakeyos.j2me.ui.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Modern status bar with icons and smooth color transitions
 */
public class StatusBar extends JPanel {

    public enum StatusType {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    private final JLabel iconLabel;
    private final JLabel messageLabel;
    private StatusType currentType = StatusType.INFO;
    private Color targetColor;
    private Timer colorTransitionTimer;
    private int transitionStep = 0;
    private static final int TRANSITION_STEPS = 10;

    public StatusBar() {
        setLayout(new BorderLayout(10, 0));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        setBackground(new Color(245, 245, 245));

        // Icon label
        iconLabel = new JLabel();
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        add(iconLabel, BorderLayout.WEST);

        // Message label
        messageLabel = new JLabel(" ");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        messageLabel.setForeground(new Color(100, 100, 100));
        add(messageLabel, BorderLayout.CENTER);

        setStatus("Ready", StatusType.INFO);
    }

    /**
     * Set status message with type
     */
    public void setStatus(String message, StatusType type) {
        currentType = type;
        messageLabel.setText(message);
        iconLabel.setText(getIconForType(type));

        // Animate color transition
        targetColor = getColorForType(type);
        animateColorTransition();
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

    private void animateColorTransition() {
        if (colorTransitionTimer != null && colorTransitionTimer.isRunning()) {
            colorTransitionTimer.stop();
        }

        transitionStep = 0;
        final Color startColor = messageLabel.getForeground();

        colorTransitionTimer = new Timer(30, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                transitionStep++;
                float ratio = (float) transitionStep / TRANSITION_STEPS;

                int red = (int) (startColor.getRed() + (targetColor.getRed() - startColor.getRed()) * ratio);
                int green = (int) (startColor.getGreen() + (targetColor.getGreen() - startColor.getGreen()) * ratio);
                int blue = (int) (startColor.getBlue() + (targetColor.getBlue() - startColor.getBlue()) * ratio);

                messageLabel.setForeground(new Color(red, green, blue));
                iconLabel.setForeground(new Color(red, green, blue));

                if (transitionStep >= TRANSITION_STEPS) {
                    ((Timer) e.getSource()).stop();
                }
            }
        });
        colorTransitionTimer.start();
    }

    private String getIconForType(StatusType type) {
        switch (type) {
            case SUCCESS:
                return "✓";
            case ERROR:
                return "✕";
            case WARNING:
                return "⚠";
            case INFO:
            default:
                return "ℹ";
        }
    }

    private Color getColorForType(StatusType type) {
        switch (type) {
            case SUCCESS:
                return new Color(34, 139, 34);
            case ERROR:
                return new Color(220, 53, 69);
            case WARNING:
                return new Color(255, 140, 0);
            case INFO:
            default:
                return new Color(59, 130, 246);
        }
    }
}
