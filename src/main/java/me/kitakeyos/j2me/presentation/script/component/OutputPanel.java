package me.kitakeyos.j2me.presentation.script.component;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Output panel component for displaying script execution results.
 * Supports color-coded output for info, error, success, and normal messages.
 */
public class OutputPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(OutputPanel.class.getName());

    private JTextPane outputArea;

    public OutputPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Output"));

        JPanel headerPanel = new JPanel(new BorderLayout());
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> clearOutput());
        headerPanel.add(clearBtn, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        outputArea = new JTextPane();
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setEditable(false);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);
    }

    private void appendToOutput(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = outputArea.getStyledDocument();
                SimpleAttributeSet style = new SimpleAttributeSet();
                StyleConstants.setForeground(style, color);
                StyleConstants.setFontFamily(style, "Monospaced");
                StyleConstants.setFontSize(style, 12);

                doc.insertString(doc.getLength(), text, style);
                outputArea.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                logger.log(Level.SEVERE, "Failed to append text to output panel: " + e.getMessage());
            }
        });
    }

    public void appendInfo(String text) {
        appendToOutput("[INFO] " + text + "\n", Color.BLUE);
    }

    public void appendError(String text) {
        appendToOutput("[ERROR] " + text + "\n", Color.RED);
    }

    public void appendSuccess(String text) {
        appendToOutput("[SUCCESS] " + text + "\n", new Color(0, 128, 0));
    }

    public void appendNormal(String text) {
        appendToOutput(text + "\n", Color.BLACK);
    }

    public void clearOutput() {
        outputArea.setText("");
        outputArea.setCaretPosition(0);
    }

    public void setDarkMode(boolean isDarkMode) {
        if (isDarkMode) {
            outputArea.setBackground(new Color(43, 43, 43));
            outputArea.setForeground(Color.WHITE);
        } else {
            outputArea.setBackground(Color.WHITE);
            outputArea.setForeground(Color.BLACK);
        }
    }
}