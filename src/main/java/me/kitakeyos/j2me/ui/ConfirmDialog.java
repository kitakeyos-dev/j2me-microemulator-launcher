package me.kitakeyos.j2me.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern styled confirmation dialog with gradient backgrounds and smooth styling
 */
public class ConfirmDialog extends JDialog {

    private static final int DIALOG_WIDTH = 450;
    private static final int DIALOG_HEIGHT = 200;
    private static final int CORNER_RADIUS = 20;

    private boolean confirmed = false;
    private final String message;
    private final String title;
    private final String yesText;
    private final String noText;

    private ConfirmDialog(Frame parent, String title, String message, String yesText, String noText) {
        super(parent, true);
        this.title = title;
        this.message = message;
        this.yesText = yesText;
        this.noText = noText;

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

                // Warning gradient colors (orange/yellow)
                Color topColor = new Color(251, 146, 60);
                Color bottomColor = new Color(249, 115, 22);
                Color borderColor = new Color(234, 88, 12);

                GradientPaint gradient = new GradientPaint(
                    0, 0, topColor,
                    0, getHeight(), bottomColor
                );

                // Draw rounded rectangle with gradient
                g2d.setPaint(gradient);
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS));

                // Draw border
                g2d.setColor(borderColor);
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

        JLabel iconLabel = new JLabel("⚠");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        iconLabel.setForeground(Color.WHITE);
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

        JButton noButton = createModernButton(noText, false);
        noButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton yesButton = createModernButton(yesText, true);
        yesButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        buttonPanel.add(noButton);
        buttonPanel.add(yesButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // Add shadow effect (simulated with rounded shape)
        setShape(new RoundRectangle2D.Float(0, 0, DIALOG_WIDTH, DIALOG_HEIGHT, CORNER_RADIUS, CORNER_RADIUS));
    }

    private JButton createModernButton(String text, boolean isPrimary) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color buttonColor;
                if (isPrimary) {
                    // Primary button (Yes/OK) - white with orange text
                    if (getModel().isPressed()) {
                        buttonColor = new Color(255, 255, 255, 180);
                    } else if (getModel().isRollover()) {
                        buttonColor = new Color(255, 255, 255, 220);
                    } else {
                        buttonColor = new Color(255, 255, 255, 200);
                    }
                } else {
                    // Secondary button (No/Cancel) - semi-transparent
                    if (getModel().isPressed()) {
                        buttonColor = new Color(255, 255, 255, 100);
                    } else if (getModel().isRollover()) {
                        buttonColor = new Color(255, 255, 255, 140);
                    } else {
                        buttonColor = new Color(255, 255, 255, 80);
                    }
                }

                g2d.setColor(buttonColor);
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

                // Draw text
                g2d.setColor(getForeground());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x, y);
            }
        };

        button.setPreferredSize(new Dimension(90, 35));
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(isPrimary ? new Color(234, 88, 12) : Color.WHITE);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    /**
     * Show a confirmation dialog with Yes/No options
     *
     * @param parent Parent frame
     * @param title Dialog title
     * @param message Confirmation message
     * @return true if user clicked Yes, false otherwise
     */
    public static boolean showConfirm(Frame parent, String title, String message) {
        return showConfirm(parent, title, message, "Yes", "No");
    }

    /**
     * Show a confirmation dialog with custom button texts
     *
     * @param parent Parent frame
     * @param title Dialog title
     * @param message Confirmation message
     * @param yesText Text for confirmation button
     * @param noText Text for cancel button
     * @return true if user clicked confirmation button, false otherwise
     */
    public static boolean showConfirm(Frame parent, String title, String message, String yesText, String noText) {
        ConfirmDialog dialog = new ConfirmDialog(parent, title, message, yesText, noText);
        dialog.setVisible(true);
        return dialog.confirmed;
    }

    /**
     * Show a confirmation dialog compatible with JOptionPane return values
     *
     * @param parent Parent frame
     * @param title Dialog title
     * @param message Confirmation message
     * @return JOptionPane.YES_OPTION if confirmed, JOptionPane.NO_OPTION otherwise
     */
    public static int showConfirmDialog(Frame parent, String title, String message) {
        boolean result = showConfirm(parent, title, message);
        return result ? JOptionPane.YES_OPTION : JOptionPane.NO_OPTION;
    }
}
