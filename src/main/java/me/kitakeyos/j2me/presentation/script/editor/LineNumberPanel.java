package me.kitakeyos.j2me.presentation.script.editor;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import java.awt.*;

/**
 * A panel that displays line numbers alongside a JTextPane.
 * This component automatically updates line numbers as the document changes
 * and efficiently renders only visible line numbers.
 */
public class LineNumberPanel extends JPanel {
    private JTextPane textPane;
    private int lineCount = 1;

    /**
     * Creates a new LineNumberPanel for the specified text pane.
     *
     * @param textPane the text pane to display line numbers for
     */
    public LineNumberPanel(JTextPane textPane) {
        this.textPane = textPane;
        setPreferredSize(new Dimension(40, Integer.MAX_VALUE));
        setBackground(new Color(240, 240, 240));
        updateLineNumbers();
    }

    /**
     * Updates the line count and adjusts panel width based on maximum line number.
     * This method should be called when the document content changes.
     */
    public void updateLineNumbers() {
        Document doc = textPane.getDocument();
        Element root = doc.getDefaultRootElement();
        lineCount = root.getElementCount();

        String maxLineStr = String.valueOf(lineCount);
        FontMetrics fm = getFontMetrics(textPane.getFont());
        if (fm != null) {
            int width = fm.stringWidth(maxLineStr) + 15;
            setPreferredSize(new Dimension(Math.max(40, width), Integer.MAX_VALUE));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setFont(textPane.getFont());
        g.setColor(Color.GRAY);

        FontMetrics fm = g.getFontMetrics();
        int lineHeight = fm.getHeight();

        Rectangle visibleRect = textPane.getVisibleRect();

        try {
            int startOffset = textPane.viewToModel(new Point(0, visibleRect.y));
            int endOffset = textPane.viewToModel(new Point(0, visibleRect.y + visibleRect.height));

            Document doc = textPane.getDocument();
            Element root = doc.getDefaultRootElement();

            int startLine = root.getElementIndex(startOffset);
            int endLine = root.getElementIndex(endOffset);

            for (int line = startLine; line <= endLine && line < lineCount; line++) {
                try {
                    Element lineElement = root.getElement(line);
                    int lineStartOffset = lineElement.getStartOffset();
                    Rectangle lineRect = textPane.modelToView(lineStartOffset);

                    if (lineRect != null) {
                        int y = lineRect.y - visibleRect.y + fm.getAscent();
                        String lineNumber = String.valueOf(line + 1);
                        int x = getWidth() - fm.stringWidth(lineNumber) - 5;
                        g.drawString(lineNumber, x, y);
                    }
                } catch (BadLocationException e) {
                    // Skip this line if there's an error
                    continue;
                }
            }
        } catch (Exception e) {
            // Fallback to simple numbering
            for (int i = 0; i < lineCount; i++) {
                String lineNumber = String.valueOf(i + 1);
                int x = getWidth() - fm.stringWidth(lineNumber) - 5;
                int y = (i + 1) * lineHeight;
                if (y > visibleRect.y && y < visibleRect.y + visibleRect.height) {
                    g.drawString(lineNumber, x, y);
                }
            }
        }
    }
}
