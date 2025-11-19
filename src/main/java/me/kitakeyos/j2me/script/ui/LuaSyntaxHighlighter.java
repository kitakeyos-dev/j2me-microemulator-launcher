package me.kitakeyos.j2me.script.ui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optimized Lua Syntax Highlighter with basic color scheme and improved performance
 */
public class LuaSyntaxHighlighter {

    // Core Lua keywords
    private static final Set<String> LUA_KEYWORDS = new HashSet<>(Arrays.asList(
            "and", "break", "do", "else", "elseif", "end", "false", "for", "function",
            "if", "in", "local", "nil", "not", "or", "repeat", "return", "then",
            "true", "until", "while", "goto"
    ));

    // Essential built-in functions
    private static final Set<String> LUA_BUILTIN = new HashSet<>(Arrays.asList(
            "print", "type", "tostring", "tonumber", "pairs", "ipairs",
            "require", "string", "table", "math", "io", "os"
    ));

    private StyledDocument doc;
    private boolean isDarkMode;
    private Timer highlightTimer;
    private boolean highlightPending = false;

    // Basic style attributes
    private SimpleAttributeSet keywordStyle;
    private SimpleAttributeSet builtinStyle;
    private SimpleAttributeSet stringStyle;
    private SimpleAttributeSet commentStyle;
    private SimpleAttributeSet numberStyle;
    private SimpleAttributeSet normalStyle;
    private SimpleAttributeSet importStyle;

    // Pre-compiled patterns for performance
    private static final Pattern COMMENT_PATTERN = Pattern.compile("--.*$", Pattern.MULTILINE);
    private static final Pattern STRING_DOUBLE_PATTERN = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
    private static final Pattern STRING_SINGLE_PATTERN = Pattern.compile("'([^'\\\\]|\\\\.)*'");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");

    public LuaSyntaxHighlighter(JTextPane textPane, boolean isDarkMode) {
        this.doc = textPane.getStyledDocument();
        this.isDarkMode = isDarkMode;
        initializeStyles();

        // Timer for debounced highlighting
        highlightTimer = new Timer(200, e -> {
            if (highlightPending) {
                performHighlighting();
                highlightPending = false;
            }
        });
        highlightTimer.setRepeats(false);
    }

    public void setDarkMode(boolean isDarkMode) {
        this.isDarkMode = isDarkMode;
        initializeStyles();
        scheduleHighlight();
    }

    private void initializeStyles() {
        // Normal text
        normalStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(normalStyle, Color.BLACK);

        // Keywords - blue and bold
        keywordStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(keywordStyle, new Color(0, 0, 255));
        StyleConstants.setBold(keywordStyle, true);

        // Built-in functions - purple
        builtinStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(builtinStyle, new Color(128, 0, 128));

        // Strings - green
        stringStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(stringStyle, new Color(0, 128, 0));

        // Comments - gray and italic
        commentStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(commentStyle, new Color(128, 128, 128));
        StyleConstants.setItalic(commentStyle, true);

        // Numbers - red
        numberStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(numberStyle, new Color(255, 0, 0));

        // Import statements - brown
        importStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(importStyle, new Color(139, 69, 19));
        StyleConstants.setBold(importStyle, true);
    }

    public SimpleAttributeSet getNormalStyle() {
        return normalStyle;
    }

    public void highlightAll() {
        scheduleHighlight();
    }

    private void scheduleHighlight() {
        highlightPending = true;
        highlightTimer.restart();
    }

    private void performHighlighting() {
        SwingUtilities.invokeLater(() -> {
            try {
                String text = doc.getText(0, doc.getLength());

                // Skip highlighting for very large documents
                if (text.length() > 15000) {
                    return;
                }

                // Clear all attributes first
                doc.setCharacterAttributes(0, doc.getLength(), normalStyle, true);

                // Highlight in order of priority
                highlightComments(text);
                highlightStrings(text);
                highlightNumbers(text);
                highlightKeywords(text);
                highlightBuiltins(text);

            } catch (BadLocationException e) {
                // Document changed during highlighting, ignore
            }
        });
    }

    private void highlightComments(String text) throws BadLocationException {
        Matcher matcher = COMMENT_PATTERN.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            int length = matcher.end() - matcher.start();
            doc.setCharacterAttributes(start, length, commentStyle, false);
        }
    }

    private void highlightStrings(String text) throws BadLocationException {
        // Double quoted strings
        Matcher matcher = STRING_DOUBLE_PATTERN.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            int length = matcher.end() - matcher.start();
            doc.setCharacterAttributes(start, length, stringStyle, false);
        }

        // Single quoted strings
        matcher = STRING_SINGLE_PATTERN.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            int length = matcher.end() - matcher.start();
            doc.setCharacterAttributes(start, length, stringStyle, false);
        }
    }

    private void highlightNumbers(String text) throws BadLocationException {
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            int length = matcher.end() - matcher.start();
            doc.setCharacterAttributes(start, length, numberStyle, false);
        }
    }

    private void highlightKeywords(String text) throws BadLocationException {
        String keywordPattern = "\\b(" + String.join("|", LUA_KEYWORDS) + ")\\b";
        Pattern pattern = Pattern.compile(keywordPattern);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            int start = matcher.start();
            int length = matcher.end() - matcher.start();
            doc.setCharacterAttributes(start, length, keywordStyle, false);
        }
    }

    private void highlightBuiltins(String text) throws BadLocationException {
        String builtinPattern = "\\b(" + String.join("|", LUA_BUILTIN) + ")\\b";
        Pattern pattern = Pattern.compile(builtinPattern);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            int start = matcher.start();
            int length = matcher.end() - matcher.start();
            doc.setCharacterAttributes(start, length, builtinStyle, false);
        }
    }

    // Simplified document update handling - only trigger for small changes
    public void handleDocumentUpdate(int offset, int length) {
        try {
            if (doc.getLength() < 10000) {
                scheduleHighlight();
            }
        } catch (Exception e) {
            // Ignore
        }
    }
}