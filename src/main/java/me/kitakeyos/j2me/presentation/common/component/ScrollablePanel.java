package me.kitakeyos.j2me.presentation.common.component;

import javax.swing.*;
import java.awt.*;

/**
 * A JPanel that implements Scrollable interface to support proper resizing
 * inside a JScrollPane. This is essential for wrapping layouts like
 * SimpleFlowLayout
 * to work correctly when the viewport is resized.
 */
public class ScrollablePanel extends JPanel implements Scrollable {

    public ScrollablePanel() {
        super();
    }

    public ScrollablePanel(LayoutManager layout) {
        super(layout);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true; // Force width to match viewport, enabling wrapping
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false; // Allow height to grow as needed
    }
}
