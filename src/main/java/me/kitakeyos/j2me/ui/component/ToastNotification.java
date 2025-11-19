package me.kitakeyos.j2me.ui.component;

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

    public ToastNotification(String message, ToastType toastType) {
        initComponents(message);
        positionToast();
        scheduleAutoClose();
    }

    private void initComponents(String message) {
        setSize(TOAST_WIDTH, TOAST_HEIGHT);
        setAlwaysOnTop(true);

        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Message label
        JLabel messageLabel = new JLabel(message);
        mainPanel.add(messageLabel, BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    private void positionToast() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        DisplayMode mode = gd.getDisplayMode();

        int screenWidth = mode.getWidth();
        int screenHeight = mode.getHeight();

        // Position at bottom-right corner with some padding
        int x = screenWidth - TOAST_WIDTH - 20;
        int y = screenHeight - TOAST_HEIGHT - 60;

        setLocation(x, y);
    }

    private void scheduleAutoClose() {
        Timer timer = new Timer(DISPLAY_TIME, e -> dispose());
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Show a success toast notification
     */
    public static void showSuccess(String message) {
        SwingUtilities.invokeLater(() -> {
            ToastNotification toast = new ToastNotification(message, ToastType.SUCCESS);
            toast.setVisible(true);
        });
    }

    /**
     * Show an error toast notification
     */
    public static void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            ToastNotification toast = new ToastNotification(message, ToastType.ERROR);
            toast.setVisible(true);
        });
    }

    /**
     * Show a warning toast notification
     */
    public static void showWarning(String message) {
        SwingUtilities.invokeLater(() -> {
            ToastNotification toast = new ToastNotification(message, ToastType.WARNING);
            toast.setVisible(true);
        });
    }

    /**
     * Show an info toast notification
     */
    public static void showInfo(String message) {
        SwingUtilities.invokeLater(() -> {
            ToastNotification toast = new ToastNotification(message, ToastType.INFO);
            toast.setVisible(true);
        });
    }
}
