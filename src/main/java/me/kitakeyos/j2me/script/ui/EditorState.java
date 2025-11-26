package me.kitakeyos.j2me.script.ui;

import javax.swing.undo.UndoManager;

/**
 * Stores the editor state for a script including code content,
 * caret position, scroll position, and undo/redo history.
 * This allows preserving state when switching between scripts.
 */
public class EditorState {
    private String code;
    private int caretPosition;
    private int scrollPosition;
    private UndoManager undoManager;
    private boolean modified;

    public EditorState() {
        this.code = "";
        this.caretPosition = 0;
        this.scrollPosition = 0;
        this.undoManager = new UndoManager();
        this.undoManager.setLimit(100); // Limit undo history to 100 edits
        this.modified = false;
    }

    public EditorState(String code) {
        this();
        this.code = code != null ? code : "";
    }

    // Getters and setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code != null ? code : "";
    }

    public int getCaretPosition() {
        return caretPosition;
    }

    public void setCaretPosition(int caretPosition) {
        this.caretPosition = Math.max(0, caretPosition);
    }

    public int getScrollPosition() {
        return scrollPosition;
    }

    public void setScrollPosition(int scrollPosition) {
        this.scrollPosition = Math.max(0, scrollPosition);
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public void setUndoManager(UndoManager undoManager) {
        this.undoManager = undoManager;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    /**
     * Clears the undo history
     */
    public void clearUndoHistory() {
        if (undoManager != null) {
            undoManager.discardAllEdits();
        }
    }

    /**
     * Creates a deep copy of the current state
     */
    public EditorState copy() {
        EditorState copy = new EditorState(this.code);
        copy.setCaretPosition(this.caretPosition);
        copy.setScrollPosition(this.scrollPosition);
        copy.setModified(this.modified);
        // Note: UndoManager is not copied - each script gets its own
        return copy;
    }

    @Override
    public String toString() {
        return "EditorState{" +
                "codeLength=" + (code != null ? code.length() : 0) +
                ", caretPosition=" + caretPosition +
                ", scrollPosition=" + scrollPosition +
                ", modified=" + modified +
                ", canUndo=" + (undoManager != null && undoManager.canUndo()) +
                ", canRedo=" + (undoManager != null && undoManager.canRedo()) +
                '}';
    }
}