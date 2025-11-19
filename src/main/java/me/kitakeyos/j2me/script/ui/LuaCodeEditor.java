package me.kitakeyos.j2me.script.ui;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Optimized LuaCodeEditor with basic interface and improved performance
 */
public class LuaCodeEditor {
    private JPanel editorPanel;
    private JTextPane textPane;
    private LineNumberPanel lineNumberPanel;
    private JScrollPane scrollPane;
    private LuaSyntaxHighlighter syntaxHighlighter;
    private CodeCompletionProvider completionProvider;
    private JPopupMenu completionPopup;
    private JList<String> completionList;
    private UndoManager undoManager;
    private boolean isDarkMode;
    private boolean syntaxHighlightEnabled;

    public LuaCodeEditor(boolean isDarkMode, boolean syntaxHighlightEnabled, DocumentListener documentListener) {
        this.isDarkMode = isDarkMode;
        this.syntaxHighlightEnabled = syntaxHighlightEnabled;

        undoManager = new UndoManager();

        initializeUI();
        initializeCodeCompletion();
        setupUndoRedo();

        textPane.getDocument().addDocumentListener(documentListener);

        // Add document listener to update line numbers
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    lineNumberPanel.updateLineNumbers();
                    lineNumberPanel.repaint();
                });
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    lineNumberPanel.updateLineNumbers();
                    lineNumberPanel.repaint();
                });
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    lineNumberPanel.updateLineNumbers();
                    lineNumberPanel.repaint();
                });
            }
        });
    }

    private void setupUndoRedo() {
        textPane.getDocument().addUndoableEditListener(undoManager);

        InputMap inputMap = textPane.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = textPane.getActionMap();

        // Ctrl+Z for undo
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                    lineNumberPanel.updateLineNumbers();
                    lineNumberPanel.repaint();
                    if (syntaxHighlightEnabled) {
                        SwingUtilities.invokeLater(() -> syntaxHighlighter.highlightAll());
                    }
                }
            }
        });

        // Ctrl+Y for redo
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK), "redo");
        actionMap.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo()) {
                    undoManager.redo();
                    lineNumberPanel.updateLineNumbers();
                    lineNumberPanel.repaint();
                    if (syntaxHighlightEnabled) {
                        SwingUtilities.invokeLater(() -> syntaxHighlighter.highlightAll());
                    }
                }
            }
        });
    }

    private void initializeUI() {
        editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBorder(BorderFactory.createTitledBorder("Code Editor"));

        textPane = new JTextPane();
        textPane.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Create line number panel
        lineNumberPanel = new LineNumberPanel(textPane);

        scrollPane = new JScrollPane(textPane);
        scrollPane.setRowHeaderView(lineNumberPanel);

        // Add scroll listener to synchronize line numbers
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            lineNumberPanel.repaint();
        });

        editorPanel.add(scrollPane, BorderLayout.CENTER);

        syntaxHighlighter = new LuaSyntaxHighlighter(textPane, isDarkMode);
        if (syntaxHighlightEnabled) {
            syntaxHighlighter.highlightAll();
        }
    }

    private void initializeCodeCompletion() {
        completionProvider = new CodeCompletionProvider();
        completionPopup = new JPopupMenu();
        completionList = new JList<>();
        completionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        completionList.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JScrollPane completionScrollPane = new JScrollPane(completionList);
        completionScrollPane.setPreferredSize(new Dimension(200, 120));
        completionPopup.add(completionScrollPane);

        // Handle selection from completion list
        completionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    insertSelectedCompletion();
                }
            }
        });

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

        // Handle key events for code completion
        textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_SPACE) {
                    e.consume();
                    showCompletionPopup();
                } else if (completionPopup.isVisible()) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP) {
                        completionList.dispatchEvent(e);
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB) {
                        e.consume();
                        insertSelectedCompletion();
                    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        hideCompletionPopup();
                    }
                }
                if (shouldHidePopup(e.getKeyCode())) {
                    hideCompletionPopup();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (c == '.' || c == ':') {
                    SwingUtilities.invokeLater(() -> showCompletionPopup());
                } else if (Character.isLetterOrDigit(c) || c == '_') {
                    SwingUtilities.invokeLater(() -> {
                        if (completionPopup.isVisible()) {
                            updateCompletionPopup();
                        } else {
                            // Show popup after typing 2+ characters
                            String text = textPane.getText();
                            int caretOffset = textPane.getCaretPosition();
                            String partialWord = completionProvider.getPartialWord(text.substring(0, caretOffset) + c);
                            if (partialWord.length() >= 2) {
                                showCompletionPopup();
                            }
                        }
                    });
                } else if (shouldHidePopupOnChar(c)) {
                    hideCompletionPopup();
                }
            }
        });

        // Hide popup when focus is lost
        textPane.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (!completionList.hasFocus()) {
                    hideCompletionPopup();
                }
            }
        });

        textPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (completionPopup.isVisible()) {
                    hideCompletionPopup();
                }
            }
        });
    }

    private boolean shouldHidePopup(int keyCode) {
        return keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT ||
                keyCode == KeyEvent.VK_HOME || keyCode == KeyEvent.VK_END ||
                keyCode == KeyEvent.VK_PAGE_UP || keyCode == KeyEvent.VK_PAGE_DOWN;
    }

    private boolean shouldHidePopupOnChar(char c) {
        return c == ' ' || c == '\n' || c == '\t' || c == '(' || c == ')' ||
                c == '{' || c == '}' || c == '[' || c == ']' || c == ';' ||
                c == ',' || c == '+' || c == '-' || c == '*' || c == '/';
    }

    private void hideCompletionPopup() {
        if (completionPopup.isVisible()) {
            completionPopup.setVisible(false);
            textPane.requestFocus();
        }
    }

    private void showCompletionPopup() {
        try {
            String text = textPane.getText();
            int caretOffset = textPane.getCaretPosition();
            List<String> suggestions = completionProvider.getSuggestions(text, caretOffset);

            if (!suggestions.isEmpty()) {
                completionList.setListData(suggestions.toArray(new String[0]));
                completionList.setSelectedIndex(0);

                Rectangle caretRect = textPane.modelToView(caretOffset);
                Point popupLocation = new Point(caretRect.x, caretRect.y + caretRect.height);
                SwingUtilities.convertPointToScreen(popupLocation, textPane);

                // Adjust popup position if it goes off screen
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                if (popupLocation.x + 200 > screenSize.width) {
                    popupLocation.x = screenSize.width - 200;
                }
                if (popupLocation.y + 120 > screenSize.height) {
                    popupLocation.y = popupLocation.y - caretRect.height - 120;
                }

                completionPopup.setLocation(popupLocation);
                completionPopup.setVisible(true);

                SwingUtilities.invokeLater(() -> completionList.requestFocus());
            } else {
                hideCompletionPopup();
            }
        } catch (BadLocationException e) {
            System.err.println("Failed to show completion popup: " + e.getMessage());
        }
    }

    private void updateCompletionPopup() {
        if (!completionPopup.isVisible()) return;

        try {
            String text = textPane.getText();
            int caretOffset = textPane.getCaretPosition();
            List<String> suggestions = completionProvider.getSuggestions(text, caretOffset);

            if (!suggestions.isEmpty()) {
                completionList.setListData(suggestions.toArray(new String[0]));
                completionList.setSelectedIndex(0);
            } else {
                hideCompletionPopup();
            }
        } catch (Exception e) {
            hideCompletionPopup();
        }
    }

    private void insertSelectedCompletion() {
        String selected = completionList.getSelectedValue();
        if (selected != null) {
            try {
                int caretOffset = textPane.getCaretPosition();
                String text = textPane.getText();
                String prefix = text.substring(0, caretOffset);

                String currentLine = getCurrentLine(prefix);
                if (currentLine.trim().startsWith("import ")) {
                    insertImportCompletion(selected, caretOffset, text, currentLine);
                } else {
                    insertNormalCompletion(selected, caretOffset, text, prefix);
                }

                hideCompletionPopup();
            } catch (BadLocationException e) {
                System.err.println("Failed to insert completion: " + e.getMessage());
            }
        }
    }

    private String getCurrentLine(String textUpToCaret) {
        int lastNewline = textUpToCaret.lastIndexOf('\n');
        if (lastNewline == -1) {
            return textUpToCaret;
        }
        return textUpToCaret.substring(lastNewline + 1);
    }

    private void insertImportCompletion(String selected, int caretOffset, String text, String currentLine)
            throws BadLocationException {
        String prefix = text.substring(0, caretOffset);
        int lineStart = prefix.lastIndexOf('\n') + 1;

        String importPrefix = "import ";
        int importStartInLine = currentLine.indexOf(importPrefix);
        if (importStartInLine == -1) return;

        int importStart = lineStart + importStartInLine + importPrefix.length();
        String alreadyTyped = text.substring(importStart, caretOffset).trim();

        int replaceStart = importStart;
        int replaceLength = caretOffset - importStart;

        if (!alreadyTyped.isEmpty()) {
            textPane.getDocument().remove(replaceStart, replaceLength);
            textPane.getDocument().insertString(replaceStart, selected, null);
        } else {
            textPane.getDocument().insertString(caretOffset, selected, null);
        }
    }

    private void insertNormalCompletion(String selected, int caretOffset, String text, String prefix)
            throws BadLocationException {
        String partialWord = completionProvider.getPartialWord(prefix);

        int start = caretOffset - partialWord.length();
        textPane.getDocument().remove(start, partialWord.length());
        textPane.getDocument().insertString(start, selected, null);

        if (selected.endsWith("()")) {
            textPane.setCaretPosition(start + selected.length() - 1);
        }
    }

    // Public methods
    public JPanel getEditorPanel() {
        return editorPanel;
    }

    public void setDarkMode(boolean isDarkMode) {
        this.isDarkMode = isDarkMode;
        syntaxHighlighter.setDarkMode(isDarkMode);
        if (syntaxHighlightEnabled) {
            syntaxHighlighter.highlightAll();
        }
    }

    public void setText(String text) {
        undoManager.discardAllEdits();
        hideCompletionPopup();

        textPane.setText(text);
        lineNumberPanel.updateLineNumbers();
        lineNumberPanel.repaint();
        if (syntaxHighlightEnabled) {
            syntaxHighlighter.highlightAll();
        }
    }

    public String getText() {
        return textPane.getText();
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
                System.err.println("Failed to reset syntax highlighting: " + e.getMessage());
            }
        }
    }

    public LuaSyntaxHighlighter getSyntaxHighlighter() {
        return syntaxHighlighter;
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public void undo() {
        if (undoManager.canUndo()) {
            hideCompletionPopup();
            undoManager.undo();
            lineNumberPanel.updateLineNumbers();
            lineNumberPanel.repaint();
            if (syntaxHighlightEnabled) {
                SwingUtilities.invokeLater(() -> syntaxHighlighter.highlightAll());
            }
        }
    }

    public void redo() {
        if (undoManager.canRedo()) {
            hideCompletionPopup();
            undoManager.redo();
            lineNumberPanel.updateLineNumbers();
            lineNumberPanel.repaint();
            if (syntaxHighlightEnabled) {
                SwingUtilities.invokeLater(() -> syntaxHighlighter.highlightAll());
            }
        }
    }

    public boolean canUndo() {
        return undoManager.canUndo();
    }

    public boolean canRedo() {
        return undoManager.canRedo();
    }
}