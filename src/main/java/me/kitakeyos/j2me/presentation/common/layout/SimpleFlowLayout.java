package me.kitakeyos.j2me.presentation.common.layout;

import java.awt.*;

/**
 * Simple flow layout that wraps components and works properly with JScrollPane
 * This is a lightweight alternative to complex wrap layouts
 */
public class SimpleFlowLayout extends FlowLayout {

    public SimpleFlowLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);
            int nmembers = target.getComponentCount();
            int maxWidth = target.getWidth();

            if (maxWidth == 0) {
                maxWidth = Integer.MAX_VALUE;
            }

            int x = 0;
            int y = getVgap();
            int rowHeight = 0;

            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = m.getPreferredSize();

                    // Check if we need to wrap to next row
                    if (x > 0 && x + d.width > maxWidth) {
                        // Move to next row
                        x = 0;
                        y += rowHeight + getVgap();
                        rowHeight = 0;
                    }

                    if (x == 0) {
                        x = getHgap();
                    }

                    x += d.width + getHgap();
                    rowHeight = Math.max(rowHeight, d.height);

                    dim.width = Math.max(dim.width, x);
                }
            }

            dim.height = y + rowHeight + getVgap();

            Insets insets = target.getInsets();
            dim.width += insets.left + insets.right;
            dim.height += insets.top + insets.bottom;

            return dim;
        }
    }
}
