package me.kitakeyos.j2me.presentation.script.editor;

import me.kitakeyos.j2me.presentation.script.syntax.LuaSyntaxHighlighter;
import me.kitakeyos.j2me.presentation.script.completion.CodeCompletionProvider;
import me.kitakeyos.j2me.application.script.state.EditorState;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Enhanced LuaCodeEditor with:
 * - Improved undo/redo with Ctrl+Z and Ctrl+Shift+Z (or Ctrl+Y)
 * - Better code completion with snippets
 * - State preservation when switching scripts
 * - Keyboard shortcuts (Ctrl+S for save, etc.)
 * - Auto-indent support
 */
public class LuaCodeEditor {

    // UI Components
    private JPanel editorPanel;
    private JTextPane textPane;
    private LineNumberPanel lineNumberPanel;
    private JScrollPane scrollPane;
    private LuaSyntaxHighlighter syntaxHighlighter;
    private CodeCompletionProvider completionProvider;
    private JPopupMenu completionPopup;
    private JList<String> completionList;
    private DefaultListModel<String> completionListModel;

    // Undo/Redo management
    private UndoManager currentUndoManager;
    private boolean isInternalChange = false;

    // State
    private boolean isDarkMode;
    private boolean syntaxHighlightEnabled;
    private boolean autoIndentEnabled = true;

    // Callback for save action
    private Runnable onSaveCallback;

    // Document modification tracking
    private boolean documentModified = false;
    private DocumentListener externalDocumentListener;

    public LuaCodeEditor(boolean isDarkMode, boolean syntaxHighlightEnabled, DocumentListener documentListener) {
        this.isDarkMode = isDarkMode;
        this.syntaxHighlightEnabled = syntaxHighlightEnabled;
        this.externalDocumentListener = documentListener;

        currentUndoManager = new UndoManager();
        currentUndoManager.setLimit(200);

        initializeUI();
        initializeCodeCompletion();
        setupKeyboardShortcuts();
        setupDocumentListeners();
    }

    private void initializeUI() {
        editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBorder(BorderFactory.createTitledBorder("Code Editor"));

        textPane = new JTextPane();
        textPane.setFont(new Font("Monospaced", Font.PLAIN, 13));

        // Enable tab key handling
        textPane.setFocusTraversalKeysEnabled(false);

        // Create line number panel
        lineNumberPanel = new LineNumberPanel(textPane);

        scrollPane = new JScrollPane(textPane);
        scrollPane.setRowHeaderView(lineNumberPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Sync line numbers on scroll
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            lineNumberPanel.repaint();
        });

        editorPanel.add(scrollPane, BorderLayout.CENTER);

        // Initialize syntax highlighter
        syntaxHighlighter = new LuaSyntaxHighlighter(textPane, isDarkMode);

        // Apply theme
        applyTheme();
    }

    private void setupDocumentListeners() {
        Document doc = textPane.getDocument();

        // Undo listener
        doc.addUndoableEditListener((UndoableEditEvent e) -> {
            if (!isInternalChange && currentUndoManager != null) {
                currentUndoManager.addEdit(e.getEdit());
            }
        });

        // Document change listener for external notification
        doc.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!isInternalChange) {
                    documentModified = true;
                    updateLineNumbers();
                    // Trigger syntax highlighting on text insertion
                    if (syntaxHighlightEnabled) {
                        syntaxHighlighter.handleDocumentUpdate(e.getOffset(), e.getLength());
                    }
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!isInternalChange) {
                    documentModified = true;
                    updateLineNumbers();
                    // Trigger syntax highlighting on text removal
                    if (syntaxHighlightEnabled) {
                        syntaxHighlighter.handleDocumentUpdate(e.getOffset(), e.getLength());
                    }
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Style changes, ignore
            }
        });

        // External listener
        if (externalDocumentListener != null) {
            doc.addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    if (!isInternalChange) {
                        externalDocumentListener.insertUpdate(e);
                    }
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    if (!isInternalChange) {
                        externalDocumentListener.removeUpdate(e);
                    }
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    if (!isInternalChange) {
                        externalDocumentListener.changedUpdate(e);
                    }
                }
            });
        }
    }

    private void updateLineNumbers() {
        SwingUtilities.invokeLater(() -> {
            lineNumberPanel.updateLineNumbers();
            lineNumberPanel.repaint();
        });
    }

    private void setupKeyboardShortcuts() {
        InputMap inputMap = textPane.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = textPane.getActionMap();

        // Xóa các binding mặc định có thể conflict
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "custom-undo");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                "custom-redo");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "custom-redo-alt");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "custom-save");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), "custom-duplicate");

        // Ctrl+Z for Undo
        actionMap.put("custom-undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        // Ctrl+Shift+Z for Redo
        actionMap.put("custom-redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });

        // Ctrl+Y for Redo (alternative)
        actionMap.put("custom-redo-alt", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });

        // Ctrl+S for Save
        actionMap.put("custom-save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }
            }
        });

        // Ctrl+D for Duplicate Line
        actionMap.put("custom-duplicate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                duplicateCurrentLine();
            }
        });

        // Key listener for Tab, Enter, and completion
        textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPressed(e);
            }

            @Override
            public void keyTyped(KeyEvent e) {
                handleKeyTyped(e);
            }
        });
    }

    private void handleKeyPressed(KeyEvent e) {
        // Code completion trigger: Ctrl+Space
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_SPACE) {
            e.consume();
            showCompletionPopup();
            return;
        }

        // Handle navigation in completion popup
        if (completionPopup != null && completionPopup.isVisible()) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_UP:
                    completionList.dispatchEvent(e);
                    e.consume();
                    return;
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_TAB:
                    e.consume();
                    insertSelectedCompletion();
                    return;
                case KeyEvent.VK_ESCAPE:
                    hideCompletionPopup();
                    e.consume();
                    return;
            }
        }

        // Tab key handling
        if (e.getKeyCode() == KeyEvent.VK_TAB && !e.isControlDown() && !e.isAltDown()) {
            e.consume();
            if (e.isShiftDown()) {
                unindentSelection();
            } else {
                indentOrInsertTab();
            }
            return;
        }

        // Enter key for auto-indent
        if (e.getKeyCode() == KeyEvent.VK_ENTER && autoIndentEnabled && !e.isControlDown() && !e.isShiftDown()) {
            e.consume();
            insertNewLineWithIndent();
            return;
        }

        // Hide popup on certain keys
        if (shouldHidePopup(e.getKeyCode())) {
            hideCompletionPopup();
        }
    }

    private void handleKeyTyped(KeyEvent e) {
        char c = e.getKeyChar();

        // Trigger completion on dot or colon
        if (c == '.' || c == ':') {
            SwingUtilities.invokeLater(this::showCompletionPopup);
        }
        // Update or show completion while typing
        else if (Character.isLetterOrDigit(c) || c == '_') {
            SwingUtilities.invokeLater(() -> {
                if (completionPopup != null && completionPopup.isVisible()) {
                    updateCompletionPopup();
                } else {
                    // Auto-show after 2 characters
                    try {
                        String text = textPane.getText();
                        int caretOffset = textPane.getCaretPosition();
                        if (caretOffset > 0 && caretOffset <= text.length()) {
                            String prefix = text.substring(0, caretOffset);
                            String partialWord = completionProvider.getPartialWord(prefix);
                            if (partialWord.length() >= 2) {
                                showCompletionPopup();
                            }
                        }
                    } catch (Exception ex) {
                        // Ignore
                    }
                }
            });
        }
        // Hide on space, newline, etc.
        else if (shouldHidePopupOnChar(c)) {
            hideCompletionPopup();
        }
    }

    private void initializeCodeCompletion() {
        completionProvider = new CodeCompletionProvider();
        completionPopup = new JPopupMenu();
        completionPopup.setFocusable(false);

        completionListModel = new DefaultListModel<>();
        completionList = new JList<>(completionListModel);
        completionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        completionList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        completionList.setVisibleRowCount(10);

        // Custom renderer for snippets
        completionList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                String item = (String) value;
                if (item.endsWith(" [snippet]")) {
                    setForeground(isSelected ? Color.WHITE : new Color(0, 128, 128));
                    setFont(getFont().deriveFont(Font.ITALIC));
                } else if (item.endsWith("()")) {
                    setForeground(isSelected ? Color.WHITE : new Color(128, 0, 128));
                }

                return this;
            }
        });

        JScrollPane completionScrollPane = new JScrollPane(completionList);
        completionScrollPane.setPreferredSize(new Dimension(250, 180));
        completionScrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        completionPopup.add(completionScrollPane);

        // Double-click to insert
        completionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    insertSelectedCompletion();
                }
            }
        });

        // Key handling in completion list
        completionList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB) {
                    e.consume();
                    insertSelectedCompletion();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hideCompletionPopup();
                }
            }
        });
    }

    private void showCompletionPopup() {
        try {
            String text = textPane.getText();
            int caretOffset = textPane.getCaretPosition();
            List<String> suggestions = completionProvider.getSuggestions(text, caretOffset);

            if (!suggestions.isEmpty()) {
                completionListModel.clear();
                for (String suggestion : suggestions) {
                    completionListModel.addElement(suggestion);
                }
                completionList.setSelectedIndex(0);

                Rectangle caretRect = textPane.modelToView(caretOffset);
                if (caretRect != null) {
                    Point popupLocation = new Point(caretRect.x, caretRect.y + caretRect.height);
                    SwingUtilities.convertPointToScreen(popupLocation, textPane);

                    // Adjust for screen bounds
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    if (popupLocation.x + 250 > screenSize.width) {
                        popupLocation.x = screenSize.width - 260;
                    }
                    if (popupLocation.y + 180 > screenSize.height) {
                        popupLocation.y = popupLocation.y - caretRect.height - 180;
                    }

                    completionPopup.setLocation(popupLocation);
                    completionPopup.setVisible(true);
                }
            } else {
                hideCompletionPopup();
            }
        } catch (BadLocationException e) {
            // Ignore
        }
    }

    private void updateCompletionPopup() {
        if (completionPopup == null || !completionPopup.isVisible())
            return;

        try {
            String text = textPane.getText();
            int caretOffset = textPane.getCaretPosition();
            List<String> suggestions = completionProvider.getSuggestions(text, caretOffset);

            if (!suggestions.isEmpty()) {
                String currentSelection = completionList.getSelectedValue();
                completionListModel.clear();
                int newSelectedIndex = 0;

                for (int i = 0; i < suggestions.size(); i++) {
                    String s = suggestions.get(i);
                    completionListModel.addElement(s);
                    if (s.equals(currentSelection)) {
                        newSelectedIndex = i;
                    }
                }

                completionList.setSelectedIndex(newSelectedIndex);
            } else {
                hideCompletionPopup();
            }
        } catch (Exception e) {
            hideCompletionPopup();
        }
    }

    private void hideCompletionPopup() {
        if (completionPopup != null && completionPopup.isVisible()) {
            completionPopup.setVisible(false);
            textPane.requestFocusInWindow();
        }
    }

    private void insertSelectedCompletion() {
        String selected = completionList.getSelectedValue();
        if (selected == null)
            return;

        try {
            int caretOffset = textPane.getCaretPosition();
            String text = textPane.getText();
            String prefix = text.substring(0, caretOffset);
            String currentLine = getCurrentLine(prefix);

            // Handle snippet insertion
            if (completionProvider.isSnippet(selected)) {
                insertSnippet(selected, caretOffset, prefix);
            }
            // Handle import completion
            else if (currentLine.trim().startsWith("import ")) {
                insertImportCompletion(selected, caretOffset, text, currentLine);
            }
            // Normal completion
            else {
                insertNormalCompletion(selected, caretOffset, prefix);
            }

            hideCompletionPopup();

            // Trigger syntax highlighting after completion
            if (syntaxHighlightEnabled) {
                syntaxHighlighter.highlightAll();
            }

        } catch (BadLocationException e) {
            // Ignore
        }
    }

    private void insertSnippet(String selected, int caretOffset, String prefix) throws BadLocationException {
        String snippet = completionProvider.getSnippet(selected);
        if (snippet == null)
            return;

        String partialWord = completionProvider.getPartialWord(prefix);
        int start = caretOffset - partialWord.length();

        // Get current indentation
        String currentLine = getCurrentLine(prefix);
        String indent = getLeadingWhitespace(currentLine);

        // Apply indentation to snippet
        String indentedSnippet = snippet.replace("\n", "\n" + indent);

        Document doc = textPane.getDocument();
        doc.remove(start, partialWord.length());
        doc.insertString(start, indentedSnippet, null);

        // Position cursor at first placeholder or after snippet
        int cursorPos = indentedSnippet.indexOf("name");
        if (cursorPos != -1) {
            textPane.setCaretPosition(start + cursorPos);
            textPane.select(start + cursorPos, start + cursorPos + 4);
        }
    }

    private void insertImportCompletion(String selected, int caretOffset, String text, String currentLine)
            throws BadLocationException {
        String prefix = text.substring(0, caretOffset);
        int lineStart = prefix.lastIndexOf('\n') + 1;

        String importPrefix = "import ";
        int importStartInLine = currentLine.indexOf(importPrefix);
        if (importStartInLine == -1)
            return;

        int importStart = lineStart + importStartInLine + importPrefix.length();
        int replaceLength = caretOffset - importStart;

        Document doc = textPane.getDocument();
        if (replaceLength > 0) {
            doc.remove(importStart, replaceLength);
        }
        doc.insertString(importStart, selected, null);
    }

    private void insertNormalCompletion(String selected, int caretOffset, String prefix)
            throws BadLocationException {
        String partialWord = completionProvider.getPartialWord(prefix);
        int start = caretOffset - partialWord.length();

        Document doc = textPane.getDocument();
        doc.remove(start, partialWord.length());
        doc.insertString(start, selected, null);

        // Position cursor inside parentheses for function calls
        if (selected.endsWith("()")) {
            textPane.setCaretPosition(start + selected.length() - 1);
        }
    }

    private String getCurrentLine(String textUpToCaret) {
        int lastNewline = textUpToCaret.lastIndexOf('\n');
        return lastNewline == -1 ? textUpToCaret : textUpToCaret.substring(lastNewline + 1);
    }

    private String getLeadingWhitespace(String line) {
        StringBuilder ws = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == ' ' || c == '\t') {
                ws.append(c);
            } else {
                break;
            }
        }
        return ws.toString();
    }

    private boolean shouldHidePopup(int keyCode) {
        return keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT
                || keyCode == KeyEvent.VK_HOME || keyCode == KeyEvent.VK_END;
    }

    private boolean shouldHidePopupOnChar(char c) {
        return c == ' ' || c == '\n' || c == '\r' || c == '(' || c == ')'
                || c == '{' || c == '}' || c == '[' || c == ']' || c == ';';
    }

    // ========== Auto-indent and Tab handling ==========

    private void insertNewLineWithIndent() {
        try {
            int caretPos = textPane.getCaretPosition();
            String text = textPane.getText();
            String prefix = text.substring(0, caretPos);
            String currentLine = getCurrentLine(prefix);
            String indent = getLeadingWhitespace(currentLine);

            String trimmedLine = currentLine.trim();

            // Increase indent after certain keywords
            if (trimmedLine.endsWith("then") || trimmedLine.endsWith("do")
                    || trimmedLine.endsWith("else") || trimmedLine.endsWith("function")
                    || trimmedLine.endsWith("{") || trimmedLine.endsWith("repeat")) {
                indent += "\t";
            }

            Document doc = textPane.getDocument();
            doc.insertString(caretPos, "\n" + indent, null);

        } catch (BadLocationException e) {
            // Fallback to simple newline
            try {
                textPane.getDocument().insertString(textPane.getCaretPosition(), "\n", null);
            } catch (BadLocationException ex) {
                // Ignore
            }
        }
    }

    private void indentOrInsertTab() {
        try {
            int selStart = textPane.getSelectionStart();
            int selEnd = textPane.getSelectionEnd();

            if (selStart == selEnd) {
                // No selection - insert tab
                textPane.getDocument().insertString(selStart, "\t", null);
            } else {
                // Selection - indent all selected lines
                indentSelection();
            }
        } catch (BadLocationException e) {
            // Ignore
        }
    }

    private void indentSelection() {
        try {
            String text = textPane.getText();
            int selStart = textPane.getSelectionStart();
            int selEnd = textPane.getSelectionEnd();

            // Find line boundaries
            int lineStart = text.lastIndexOf('\n', selStart - 1) + 1;
            int lineEnd = text.indexOf('\n', selEnd);
            if (lineEnd == -1)
                lineEnd = text.length();

            String selectedLines = text.substring(lineStart, lineEnd);
            String[] lines = selectedLines.split("\n", -1);

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                result.append("\t").append(lines[i]);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
            }

            Document doc = textPane.getDocument();
            doc.remove(lineStart, lineEnd - lineStart);
            doc.insertString(lineStart, result.toString(), null);

            textPane.setSelectionStart(lineStart);
            textPane.setSelectionEnd(lineStart + result.length());

        } catch (BadLocationException e) {
            // Ignore
        }
    }

    private void unindentSelection() {
        try {
            String text = textPane.getText();
            int selStart = textPane.getSelectionStart();
            int selEnd = textPane.getSelectionEnd();

            // Find line boundaries
            int lineStart = text.lastIndexOf('\n', selStart - 1) + 1;
            int lineEnd = text.indexOf('\n', selEnd);
            if (lineEnd == -1)
                lineEnd = text.length();

            String selectedLines = text.substring(lineStart, lineEnd);
            String[] lines = selectedLines.split("\n", -1);

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                // Remove one tab or up to 4 spaces
                if (line.startsWith("\t")) {
                    line = line.substring(1);
                } else if (line.startsWith("    ")) {
                    line = line.substring(4);
                } else if (line.startsWith(" ")) {
                    int spaces = 0;
                    while (spaces < line.length() && spaces < 4 && line.charAt(spaces) == ' ') {
                        spaces++;
                    }
                    line = line.substring(spaces);
                }
                result.append(line);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
            }

            Document doc = textPane.getDocument();
            doc.remove(lineStart, lineEnd - lineStart);
            doc.insertString(lineStart, result.toString(), null);

            textPane.setSelectionStart(lineStart);
            textPane.setSelectionEnd(lineStart + result.length());

        } catch (BadLocationException e) {
            // Ignore
        }
    }

    private void duplicateCurrentLine() {
        try {
            String text = textPane.getText();
            int caretPos = textPane.getCaretPosition();

            int lineStart = text.lastIndexOf('\n', caretPos - 1) + 1;
            int lineEnd = text.indexOf('\n', caretPos);
            if (lineEnd == -1)
                lineEnd = text.length();

            String currentLine = text.substring(lineStart, lineEnd);

            Document doc = textPane.getDocument();
            doc.insertString(lineEnd, "\n" + currentLine, null);

        } catch (BadLocationException e) {
            // Ignore
        }
    }

    // ========== Undo/Redo ==========

    public void undo() {
        if (currentUndoManager != null && currentUndoManager.canUndo()) {
            try {
                isInternalChange = true;
                hideCompletionPopup();
                currentUndoManager.undo();
            } catch (Exception e) {
                // Ignore undo errors
            } finally {
                isInternalChange = false;
            }
            // Refresh UI after undo
            refreshEditor();
        }
    }

    public void redo() {
        if (currentUndoManager != null && currentUndoManager.canRedo()) {
            try {
                isInternalChange = true;
                hideCompletionPopup();
                currentUndoManager.redo();
            } catch (Exception e) {
                // Ignore redo errors
            } finally {
                isInternalChange = false;
            }
            // Refresh UI after redo
            refreshEditor();
        }
    }

    /**
     * Refresh editor UI (line numbers + syntax highlighting)
     */
    private void refreshEditor() {
        SwingUtilities.invokeLater(() -> {
            lineNumberPanel.updateLineNumbers();
            lineNumberPanel.repaint();
            if (syntaxHighlightEnabled) {
                syntaxHighlighter.highlightAll();
            }
        });
    }

    public boolean canUndo() {
        return currentUndoManager != null && currentUndoManager.canUndo();
    }

    public boolean canRedo() {
        return currentUndoManager != null && currentUndoManager.canRedo();
    }

    // ========== State Management ==========

    /**
     * Saves the current editor state to an EditorState object
     */
    public void saveToState(EditorState state) {
        if (state == null)
            return;

        state.setCode(textPane.getText());
        state.setCaretPosition(textPane.getCaretPosition());
        state.setScrollPosition(scrollPane.getVerticalScrollBar().getValue());
        state.setUndoManager(currentUndoManager);
        state.setModified(documentModified);
    }

    /**
     * Restores the editor from an EditorState object.
     * This swaps the UndoManager to the one stored in the state.
     */
    public void restoreFromState(EditorState state) {
        if (state == null) {
            clearEditor();
            return;
        }

        hideCompletionPopup();

        // Swap undo manager TRƯỚC khi thay đổi text
        if (state.getUndoManager() != null) {
            currentUndoManager = state.getUndoManager();
        } else {
            currentUndoManager = new UndoManager();
            currentUndoManager.setLimit(200);
            state.setUndoManager(currentUndoManager);
        }

        // Set text without triggering undo recording or external listener
        isInternalChange = true;
        try {
            textPane.setText(state.getCode() != null ? state.getCode() : "");
        } finally {
            isInternalChange = false;
        }

        documentModified = state.isModified();

        // Restore caret, scroll position và apply highlighting
        final int savedCaretPos = state.getCaretPosition();
        final int savedScrollPos = state.getScrollPosition();

        SwingUtilities.invokeLater(() -> {
            // Set caret position
            try {
                String text = textPane.getText();
                int caretPos = Math.min(savedCaretPos, text.length());
                caretPos = Math.max(0, caretPos);
                textPane.setCaretPosition(caretPos);
            } catch (Exception e) {
                textPane.setCaretPosition(0);
            }

            // Update line numbers
            lineNumberPanel.updateLineNumbers();
            lineNumberPanel.repaint();

            // QUAN TRỌNG: Apply syntax highlighting SAU khi set text
            if (syntaxHighlightEnabled) {
                syntaxHighlighter.highlightAll();
            }

            // Set scroll position sau cùng (sau khi highlighting xong)
            SwingUtilities.invokeLater(() -> {
                scrollPane.getVerticalScrollBar().setValue(savedScrollPos);
            });
        });
    }

    /**
     * Clears the editor and creates a new UndoManager
     */
    public void clearEditor() {
        hideCompletionPopup();

        currentUndoManager = new UndoManager();
        currentUndoManager.setLimit(200);
        documentModified = false;

        isInternalChange = true;
        try {
            textPane.setText("");
        } finally {
            isInternalChange = false;
        }

        SwingUtilities.invokeLater(() -> {
            textPane.setCaretPosition(0);
            lineNumberPanel.updateLineNumbers();
            lineNumberPanel.repaint();
        });
    }

    // ========== Public API ==========

    public JPanel getEditorPanel() {
        return editorPanel;
    }

    /**
     * Sets text and resets undo history.
     * Use restoreFromState() if you want to preserve undo history.
     */
    public void setText(String text) {
        hideCompletionPopup();

        currentUndoManager.discardAllEdits();
        documentModified = false;

        isInternalChange = true;
        try {
            textPane.setText(text != null ? text : "");
        } finally {
            isInternalChange = false;
        }

        SwingUtilities.invokeLater(() -> {
            textPane.setCaretPosition(0);
            lineNumberPanel.updateLineNumbers();
            lineNumberPanel.repaint();

            // Apply syntax highlighting
            if (syntaxHighlightEnabled) {
                syntaxHighlighter.highlightAll();
            }
        });
    }

    public String getText() {
        return textPane.getText();
    }

    public void setDarkMode(boolean isDarkMode) {
        this.isDarkMode = isDarkMode;
        applyTheme();
        syntaxHighlighter.setDarkMode(isDarkMode);
        if (syntaxHighlightEnabled) {
            syntaxHighlighter.highlightAll();
        }
    }

    private void applyTheme() {
        if (isDarkMode) {
            textPane.setBackground(new Color(43, 43, 43));
            textPane.setForeground(new Color(187, 187, 187));
            textPane.setCaretColor(Color.WHITE);
            lineNumberPanel.setBackground(new Color(49, 51, 53));
        } else {
            textPane.setBackground(Color.WHITE);
            textPane.setForeground(Color.BLACK);
            textPane.setCaretColor(Color.BLACK);
            lineNumberPanel.setBackground(new Color(240, 240, 240));
        }
    }

    public void toggleSyntaxHighlighting() {
        syntaxHighlightEnabled = !syntaxHighlightEnabled;
        if (syntaxHighlightEnabled) {
            syntaxHighlighter.highlightAll();
        } else {
            try {
                textPane.getStyledDocument().setCharacterAttributes(
                        0, textPane.getText().length(),
                        syntaxHighlighter.getNormalStyle(), true);
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    public boolean isSyntaxHighlightEnabled() {
        return syntaxHighlightEnabled;
    }

    public void setAutoIndentEnabled(boolean enabled) {
        this.autoIndentEnabled = enabled;
    }

    public boolean isAutoIndentEnabled() {
        return autoIndentEnabled;
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public boolean isModified() {
        return documentModified;
    }

    public void setModified(boolean modified) {
        this.documentModified = modified;
    }

    public LuaSyntaxHighlighter getSyntaxHighlighter() {
        return syntaxHighlighter;
    }

    public CodeCompletionProvider getCompletionProvider() {
        return completionProvider;
    }

    public UndoManager getUndoManager() {
        return currentUndoManager;
    }

    public void requestFocus() {
        textPane.requestFocusInWindow();
    }

    public int getCaretPosition() {
        return textPane.getCaretPosition();
    }

    public void setCaretPosition(int position) {
        try {
            textPane.setCaretPosition(Math.min(position, textPane.getText().length()));
        } catch (Exception e) {
            textPane.setCaretPosition(0);
        }
    }

    public int getScrollPosition() {
        return scrollPane.getVerticalScrollBar().getValue();
    }

    public void setScrollPosition(int position) {
        scrollPane.getVerticalScrollBar().setValue(position);
    }

    /**
     * Force refresh syntax highlighting
     */
    public void refreshSyntaxHighlighting() {
        if (syntaxHighlightEnabled) {
            syntaxHighlighter.highlightAll();
        }
    }
}