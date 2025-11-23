package me.kitakeyos.j2me.ui.component;

import me.kitakeyos.j2me.MainApplication;

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

    public ToastNotification(Window owner, String message, ToastType toastType) {
        super(owner);
        this.owner = owner;
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
        if (owner != null) {
            // Position relative to owner window
            int ownerX = owner.getX();
            int ownerY = owner.getY();
            int ownerWidth = owner.getWidth();
            int ownerHeight = owner.getHeight();

            // Position at bottom-right corner of the owner window with some padding
            int x = ownerX + ownerWidth - TOAST_WIDTH - 20;
            int y = ownerY + ownerHeight - TOAST_HEIGHT - 60;

            setLocation(x, y);
        } else {
            // Fallback to screen positioning if no owner
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
