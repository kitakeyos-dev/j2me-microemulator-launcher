package me.kitakeyos.j2me.ui.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

/**
 * Toast notification that appears at the bottom-right corner and auto-dismisses
 */
public class ToastNotification extends JWindow {

    public enum ToastType {
        SUCCESS,
        ERROR,
        WARNING,
        INFO
    }

    private static final int TOAST_WIDTH = 350;
    private static final int TOAST_HEIGHT = 80;
    private static final int CORNER_RADIUS = 15;
    private static final int DISPLAY_TIME = 3000; // 3 seconds
    private static final int ANIMATION_STEPS = 20;
    private static final int ANIMATION_DELAY = 20;

    private final ToastType toastType;
    private final String message;
    private Timer fadeTimer;
    private float opacity = 0.0f;

    public ToastNotification(String message, ToastType toastType) {
        this.message = message;
        this.toastType = toastType;

        initComponents();
        positionToast();
        animateIn();
    }

    private void initComponents() {
        setSize(TOAST_WIDTH, TOAST_HEIGHT);
        setAlwaysOnTop(true);

        // Create main panel with gradient
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Set composite for opacity
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

                // Get gradient colors based on toast type
                Color[] colors = getGradientColors();
                GradientPaint gradient = new GradientPaint(
                    0, 0, colors[0],
                    0, getHeight(), colors[1]
                );

                // Draw rounded rectangle with gradient
                g2d.setPaint(gradient);
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS));

                // Draw border
                g2d.setColor(colors[2]);
                g2d.setStroke(new BasicStroke(2));
                g2d.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, CORNER_RADIUS, CORNER_RADIUS));
            }
        };
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        mainPanel.setOpaque(false);

        // Icon label
        JLabel iconLabel = new JLabel(getIconForType());
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        iconLabel.setForeground(Color.WHITE);
        mainPanel.add(iconLabel, BorderLayout.WEST);

        // Message label
        JLabel messageLabel = new JLabel("<html>" + message + "</html>");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageLabel.setForeground(Color.WHITE);
        mainPanel.add(messageLabel, BorderLayout.CENTER);

        setContentPane(mainPanel);
        setShape(new RoundRectangle2D.Float(0, 0, TOAST_WIDTH, TOAST_HEIGHT, CORNER_RADIUS, CORNER_RADIUS));
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

    private void animateIn() {
        Timer animationTimer = new Timer(ANIMATION_DELAY, new ActionListener() {
            private int step = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                step++;
                opacity = Math.min(1.0f, (float) step / ANIMATION_STEPS);
                repaint();

                if (step >= ANIMATION_STEPS) {
                    ((Timer) e.getSource()).stop();
                    scheduleAutoClose();
                }
            }
        });
        animationTimer.start();
    }

    private void animateOut() {
        Timer animationTimer = new Timer(ANIMATION_DELAY, new ActionListener() {
            private int step = ANIMATION_STEPS;

            @Override
            public void actionPerformed(ActionEvent e) {
                step--;
                opacity = Math.max(0.0f, (float) step / ANIMATION_STEPS);
                repaint();

                if (step <= 0) {
                    ((Timer) e.getSource()).stop();
                    dispose();
                }
            }
        });
        animationTimer.start();
    }

    private void scheduleAutoClose() {
        fadeTimer = new Timer(DISPLAY_TIME, e -> animateOut());
        fadeTimer.setRepeats(false);
        fadeTimer.start();
    }

    private Color[] getGradientColors() {
        switch (toastType) {
            case SUCCESS:
                return new Color[]{
                    new Color(34, 197, 94, 230),      // Top - Green
                    new Color(22, 163, 74, 230),      // Bottom - Darker Green
                    new Color(21, 128, 61, 230)       // Border
                };
            case ERROR:
                return new Color[]{
                    new Color(239, 68, 68, 230),      // Top - Red
                    new Color(220, 38, 38, 230),      // Bottom - Darker Red
                    new Color(185, 28, 28, 230)       // Border
                };
            case WARNING:
                return new Color[]{
                    new Color(251, 146, 60, 230),     // Top - Orange
                    new Color(249, 115, 22, 230),     // Bottom - Darker Orange
                    new Color(234, 88, 12, 230)       // Border
                };
            case INFO:
            default:
                return new Color[]{
                    new Color(59, 130, 246, 230),     // Top - Blue
                    new Color(37, 99, 235, 230),      // Bottom - Darker Blue
                    new Color(29, 78, 216, 230)       // Border
                };
        }
    }

    private String getIconForType() {
        switch (toastType) {
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
