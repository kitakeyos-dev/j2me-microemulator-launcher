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
 * Enhanced Lua Syntax Highlighter with:
 * - Proper dark mode support
 * - Better color schemes
 * - Optimized performance with debouncing
 * - Support for multi-line comments and strings
 */
public class LuaSyntaxHighlighter {

    // Lua keywords
    private static final Set<String> LUA_KEYWORDS = new HashSet<>(Arrays.asList(
            "and", "break", "do", "else", "elseif", "end", "false", "for", "function",
            "if", "in", "local", "nil", "not", "or", "repeat", "return", "then",
            "true", "until", "while", "goto"
    ));

    // Lua built-in functions and libraries
    private static final Set<String> LUA_BUILTIN = new HashSet<>(Arrays.asList(
            "print", "type", "tostring", "tonumber", "pairs", "ipairs", "next",
            "require", "assert", "error", "pcall", "xpcall", "select", "unpack",
            "setmetatable", "getmetatable", "rawget", "rawset", "rawequal",
            "collectgarbage", "dofile", "load", "loadfile", "loadstring",
            "string", "table", "math", "io", "os", "coroutine", "debug", "package"
    ));

    private StyledDocument doc;
    private boolean isDarkMode;
    private Timer highlightTimer;
    private boolean highlightPending = false;

    // Style attributes
    private SimpleAttributeSet keywordStyle;
    private SimpleAttributeSet builtinStyle;
    private SimpleAttributeSet stringStyle;
    private SimpleAttributeSet commentStyle;
    private SimpleAttributeSet numberStyle;
    private SimpleAttributeSet normalStyle;
    private SimpleAttributeSet operatorStyle;
    private SimpleAttributeSet functionDefStyle;

    // Pre-compiled patterns
    private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile("--(?!\\[\\[).*$", Pattern.MULTILINE);
    private static final Pattern MULTI_LINE_COMMENT = Pattern.compile("--\\[\\[.*?\\]\\]", Pattern.DOTALL);
    private static final Pattern STRING_DOUBLE = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
    private static final Pattern STRING_SINGLE = Pattern.compile("'([^'\\\\]|\\\\.)*'");
    private static final Pattern MULTI_LINE_STRING = Pattern.compile("\\[\\[.*?\\]\\]", Pattern.DOTALL);
    private static final Pattern NUMBER = Pattern.compile("\\b(0x[0-9a-fA-F]+|\\d+(\\.\\d+)?([eE][+-]?\\d+)?)\\b");
    private static final Pattern FUNCTION_DEF = Pattern.compile("\\bfunction\\s+([a-zA-Z_][a-zA-Z0-9_]*)");
    private static final Pattern OPERATOR = Pattern.compile("[+\\-*/%^#=<>~]+|\\.\\.\\.");

    public LuaSyntaxHighlighter(JTextPane textPane, boolean isDarkMode) {
        this.doc = textPane.getStyledDocument();
        this.isDarkMode = isDarkMode;
        initializeStyles();

        // Timer for debounced highlighting (150ms delay)
        highlightTimer = new Timer(150, e -> {
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
        if (isDarkMode) {
            initializeDarkStyles();
        } else {
            initializeLightStyles();
        }
    }

    private void initializeLightStyles() {
        // Normal text - black
        normalStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(normalStyle, new Color(0, 0, 0));

        // Keywords - blue, bold
        keywordStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(keywordStyle, new Color(0, 0, 200));
        StyleConstants.setBold(keywordStyle, true);

        // Built-in functions - dark magenta
        builtinStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(builtinStyle, new Color(136, 0, 136));

        // Strings - dark green
        stringStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(stringStyle, new Color(0, 128, 0));

        // Comments - gray, italic
        commentStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(commentStyle, new Color(128, 128, 128));
        StyleConstants.setItalic(commentStyle, true);

        // Numbers - dark red/brown
        numberStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(numberStyle, new Color(164, 0, 0));

        // Operators - dark gray
        operatorStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(operatorStyle, new Color(80, 80, 80));

        // Function definitions - teal
        functionDefStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(functionDefStyle, new Color(0, 128, 128));
        StyleConstants.setBold(functionDefStyle, true);
    }

    private void initializeDarkStyles() {
        // Normal text - light gray
        normalStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(normalStyle, new Color(187, 187, 187));

        // Keywords - light blue, bold
        keywordStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(keywordStyle, new Color(86, 156, 214));
        StyleConstants.setBold(keywordStyle, true);

        // Built-in functions - light magenta
        builtinStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(builtinStyle, new Color(220, 150, 220));

        // Strings - orange/salmon
        stringStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(stringStyle, new Color(206, 145, 120));

        // Comments - green, italic
        commentStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(commentStyle, new Color(106, 153, 85));
        StyleConstants.setItalic(commentStyle, true);

        // Numbers - light green
        numberStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(numberStyle, new Color(181, 206, 168));

        // Operators - light cyan
        operatorStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(operatorStyle, new Color(180, 180, 180));

        // Function definitions - gold/yellow
        functionDefStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(functionDefStyle, new Color(220, 220, 170));
        StyleConstants.setBold(functionDefStyle, true);
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
                if (text.length() > 20000) {
                    return;
                }

                // Clear all attributes first
                doc.setCharacterAttributes(0, doc.getLength(), normalStyle, true);

                // Highlight in order of priority (later styles override earlier ones)
                highlightNumbers(text);
                highlightOperators(text);
                highlightKeywords(text);
                highlightBuiltins(text);
                highlightFunctionDefs(text);
                highlightStrings(text);
                highlightComments(text);

            } catch (BadLocationException e) {
                // Document changed during highlighting, ignore
            }
        });
    }

    private void highlightPattern(String text, Pattern pattern, SimpleAttributeSet style) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            int length = matcher.end() - start;
            doc.setCharacterAttributes(start, length, style, false);
        }
    }

    private void highlightComments(String text) {
        // Multi-line comments first
        highlightPattern(text, MULTI_LINE_COMMENT, commentStyle);
        // Then single-line comments
        highlightPattern(text, SINGLE_LINE_COMMENT, commentStyle);
    }

    private void highlightStrings(String text) {
        highlightPattern(text, MULTI_LINE_STRING, stringStyle);
        highlightPattern(text, STRING_DOUBLE, stringStyle);
        highlightPattern(text, STRING_SINGLE, stringStyle);
    }

    private void highlightNumbers(String text) {
        highlightPattern(text, NUMBER, numberStyle);
    }

    private void highlightOperators(String text) {
        highlightPattern(text, OPERATOR, operatorStyle);
    }

    private void highlightKeywords(String text) {
        String keywordPattern = "\\b(" + String.join("|", LUA_KEYWORDS) + ")\\b";
        Pattern pattern = Pattern.compile(keywordPattern);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            int start = matcher.start();
            int length = matcher.end() - start;
            doc.setCharacterAttributes(start, length, keywordStyle, false);
        }
    }

    private void highlightBuiltins(String text) {
        String builtinPattern = "\\b(" + String.join("|", LUA_BUILTIN) + ")\\b";
        Pattern pattern = Pattern.compile(builtinPattern);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            int start = matcher.start();
            int length = matcher.end() - start;
            doc.setCharacterAttributes(start, length, builtinStyle, false);
        }
    }

    private void highlightFunctionDefs(String text) {
        Matcher matcher = FUNCTION_DEF.matcher(text);
        while (matcher.find()) {
            // Highlight just the function name
            int nameStart = matcher.start(1);
            int nameLength = matcher.end(1) - nameStart;
            doc.setCharacterAttributes(nameStart, nameLength, functionDefStyle, false);
        }
    }

    /**
     * Handles document updates - triggers re-highlighting with debounce
     */
    public void handleDocumentUpdate(int offset, int length) {
        try {
            // Only highlight if document is reasonably sized
            if (doc.getLength() < 20000) {
                scheduleHighlight();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Forces immediate highlighting without debounce
     */
    public void highlightImmediately() {
        highlightTimer.stop();
        highlightPending = false;
        performHighlighting();
    }

    /**
     * Gets whether dark mode is enabled
     */
    public boolean isDarkMode() {
        return isDarkMode;
    }
}