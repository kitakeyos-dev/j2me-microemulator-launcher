package me.kitakeyos.j2me.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern styled message dialog with gradient backgrounds and smooth animations
 */
public class MessageDialog extends JDialog {

    public enum MessageType {
        SUCCESS,
        ERROR,
        WARNING,
        INFO
    }

    private static final int DIALOG_WIDTH = 400;
    private static final int DIALOG_HEIGHT = 200;
    private static final int CORNER_RADIUS = 20;

    private final MessageType messageType;
    private final String message;
    private final String title;

    public MessageDialog(Frame parent, String title, String message, MessageType messageType) {
        super(parent, true);
        this.title = title;
        this.message = message;
        this.messageType = messageType;

        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setUndecorated(true);
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);

        // Create main panel with gradient
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Get gradient colors based on message type
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
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setOpaque(false);

        // Header panel with icon and title
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(getIconForType());
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        headerPanel.add(iconLabel);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Message panel
        JTextArea messageArea = new JTextArea(message);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageArea.setForeground(new Color(255, 255, 255, 240));
        messageArea.setOpaque(false);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFocusable(false);

        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);
        messagePanel.add(messageArea, BorderLayout.CENTER);
        mainPanel.add(messagePanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton okButton = createModernButton("OK");
        okButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // Add shadow effect (simulated with multiple dialogs)
        setShape(new RoundRectangle2D.Float(0, 0, DIALOG_WIDTH, DIALOG_HEIGHT, CORNER_RADIUS, CORNER_RADIUS));
    }

    private JButton createModernButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(new Color(255, 255, 255, 180));
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(255, 255, 255, 220));
                } else {
                    g2d.setColor(new Color(255, 255, 255, 200));
                }

                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

                g2d.setColor(getForeground());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x, y);
            }
        };

        button.setPreferredSize(new Dimension(80, 35));
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(getButtonTextColor());
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    private Color getButtonTextColor() {
        switch (messageType) {
            case SUCCESS:
                return new Color(34, 139, 34);
            case ERROR:
                return new Color(220, 53, 69);
            case WARNING:
                return new Color(255, 140, 0);
            case INFO:
            default:
                return new Color(13, 110, 253);
        }
    }

    private Color[] getGradientColors() {
        switch (messageType) {
            case SUCCESS:
                return new Color[]{
                    new Color(34, 197, 94),      // Top - Green
                    new Color(22, 163, 74),      // Bottom - Darker Green
                    new Color(21, 128, 61)       // Border - Even Darker Green
                };
            case ERROR:
                return new Color[]{
                    new Color(239, 68, 68),      // Top - Red
                    new Color(220, 38, 38),      // Bottom - Darker Red
                    new Color(185, 28, 28)       // Border - Even Darker Red
                };
            case WARNING:
                return new Color[]{
                    new Color(251, 146, 60),     // Top - Orange
                    new Color(249, 115, 22),     // Bottom - Darker Orange
                    new Color(234, 88, 12)       // Border - Even Darker Orange
                };
            case INFO:
            default:
                return new Color[]{
                    new Color(59, 130, 246),     // Top - Blue
                    new Color(37, 99, 235),      // Bottom - Darker Blue
                    new Color(29, 78, 216)       // Border - Even Darker Blue
                };
        }
    }

    private String getIconForType() {
        switch (messageType) {
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
     * Show a success message dialog
     */
    public static void showSuccess(Frame parent, String title, String message) {
        MessageDialog dialog = new MessageDialog(parent, title, message, MessageType.SUCCESS);
        dialog.setVisible(true);
    }

    /**
     * Show an error message dialog
     */
    public static void showError(Frame parent, String title, String message) {
        MessageDialog dialog = new MessageDialog(parent, title, message, MessageType.ERROR);
        dialog.setVisible(true);
    }

    /**
     * Show a warning message dialog
     */
    public static void showWarning(Frame parent, String title, String message) {
        MessageDialog dialog = new MessageDialog(parent, title, message, MessageType.WARNING);
        dialog.setVisible(true);
    }

    /**
     * Show an info message dialog
     */
    public static void showInfo(Frame parent, String title, String message) {
        MessageDialog dialog = new MessageDialog(parent, title, message, MessageType.INFO);
        dialog.setVisible(true);
    }
}
