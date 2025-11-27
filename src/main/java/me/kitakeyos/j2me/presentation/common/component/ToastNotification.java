package me.kitakeyos.j2me.presentation.common.component;

import me.kitakeyos.j2me.application.MainApplication;

import javax.swing.*;
import java.awt.*;

/**
 * Basic toast notification that appears at the bottom-right corner and auto-dismisses
 */
public class ToastNotification extends JWindow {

    public enum ToastType {
        SUCCESS,
        ERROR,
        WARNING,
        INFO
    }

    private static final int TOAST_WIDTH = 300;
    private static final int TOAST_HEIGHT = 60;
    private static final int DISPLAY_TIME = 2000; // 2 seconds

    private Window owner;
    private ToastType toastType;

    public ToastNotification(Window owner, String message, ToastType toastType) {
        super(owner);
        this.owner = owner;
        this.toastType = toastType;
        initComponents(message);
        positionToast();
        scheduleAutoClose();
    }

    private void initComponents(String message) {
        setSize(TOAST_WIDTH, TOAST_HEIGHT);
        setAlwaysOnTop(true);

        // Get colors based on toast type
        Color borderColor = getBorderColor();
        Color backgroundColor = getBackgroundColor();
        Color textColor = getTextColor();

        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(backgroundColor);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 2),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        // Message label
        JLabel messageLabel = new JLabel(message);
        messageLabel.setForeground(textColor);
        messageLabel.setFont(messageLabel.getFont().deriveFont(12f));
        mainPanel.add(messageLabel, BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    private Color getBorderColor() {
        switch (toastType) {
            case SUCCESS:
                return new Color(40, 167, 69); // Green
            case ERROR:
                return new Color(220, 53, 69); // Red
            case WARNING:
                return new Color(255, 193, 7); // Yellow/Amber
            case INFO:
                return new Color(23, 162, 184); // Blue
            default:
                return Color.GRAY;
        }
    }

    private Color getBackgroundColor() {
        switch (toastType) {
            case SUCCESS:
                return new Color(212, 237, 218); // Light green
            case ERROR:
                return new Color(248, 215, 218); // Light red
            case WARNING:
                return new Color(255, 243, 205); // Light yellow
            case INFO:
                return new Color(209, 236, 241); // Light blue
            default:
                return Color.WHITE;
        }
    }

    private Color getTextColor() {
        switch (toastType) {
            case SUCCESS:
                return new Color(21, 87, 36); // Dark green
            case ERROR:
                return new Color(114, 28, 36); // Dark red
            case WARNING:
                return new Color(133, 100, 4); // Dark yellow/brown
            case INFO:
                return new Color(12, 84, 96); // Dark blue
            default:
                return Color.BLACK;
        }
    }

    private void positionToast() {
        if (owner != null) {
            // Position relative to owner window
            int ownerX = owner.getX();
            int ownerY = owner.getY();
            int ownerWidth = owner.getWidth();
            int ownerHeight = owner.getHeight();

            // Position at bottom-right corner of the owner window with some padding
            int x = ownerX + ownerWidth - TOAST_WIDTH - 30;
            int y = ownerY + ownerHeight - TOAST_HEIGHT - 30;

            setLocation(x, y);
        } else {
            // Fallback to screen positioning if no owner
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            DisplayMode mode = gd.getDisplayMode();

            int screenWidth = mode.getWidth();
            int screenHeight = mode.getHeight();

            // Position at bottom-right corner with some padding
            int x = screenWidth - TOAST_WIDTH - 30;
            int y = screenHeight - TOAST_HEIGHT - 30;

            setLocation(x, y);
        }
    }

    private void scheduleAutoClose() {
        Timer timer = new Timer(DISPLAY_TIME, e -> dispose());
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Show a success toast notification within the main application window
     */
    public static void showSuccess(String message) {
        SwingUtilities.invokeLater(() -> {
            ToastNotification toast = new ToastNotification(MainApplication.INSTANCE, message, ToastType.SUCCESS);
            toast.setVisible(true);
        });
    }

    /**
     * Show an error toast notification within the main application window
     */
    public static void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            ToastNotification toast = new ToastNotification(MainApplication.INSTANCE, message, ToastType.ERROR);
            toast.setVisible(true);
        });
    }

    /**
     * Show a warning toast notification within the main application window
     */
    public static void showWarning(String message) {
        SwingUtilities.invokeLater(() -> {
            ToastNotification toast = new ToastNotification(MainApplication.INSTANCE, message, ToastType.WARNING);
            toast.setVisible(true);
        });
    }

    /**
     * Show an info toast notification within the main application window
     */
    public static void showInfo(String message) {
        SwingUtilities.invokeLater(() -> {
            ToastNotification toast = new ToastNotification(MainApplication.INSTANCE, message, ToastType.INFO);
            toast.setVisible(true);
        });
    }
}
