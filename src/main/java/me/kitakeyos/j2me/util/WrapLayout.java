package me.kitakeyos.j2me.util;

import java.awt.*;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * WrapLayout is a custom layout manager that arranges components in a row,
 * wrapping to the next row when the horizontal space is filled.
 * Unlike FlowLayout, this properly handles wrapping inside JScrollPane
 * and distributes extra space evenly between components with uniform spacing
 * calculated from the first row, ensuring vertical column alignment.
 */
public class WrapLayout extends FlowLayout {

    public WrapLayout() {
        super();
    }

    public WrapLayout(int align) {
        super(align);
    }

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int maxWidth = target.getWidth() - (insets.left + insets.right + getHgap() * 2);
            int nmembers = target.getComponentCount();
            int x = insets.left + getHgap();
            int y = insets.top + getVgap();
            int rowHeight = 0;
            int rowWidth = 0;
            int start = 0;

            boolean ltr = target.getComponentOrientation().isLeftToRight();

            // First pass: group components into rows and find the first full row
            java.util.List<RowInfo> rows = new java.util.ArrayList<>();

            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = m.getPreferredSize();
                    m.setSize(d);

                    if ((rowWidth + d.width > maxWidth) && rowWidth > 0) {
                        // Save row info
                        rows.add(new RowInfo(start, i, rowHeight));
                        rowHeight = 0;
                        rowWidth = 0;
                        start = i;
                    }

                    if (rowWidth != 0) {
                        rowWidth += getHgap();
                    }
                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }
            // Add last row
            rows.add(new RowInfo(start, nmembers, rowHeight));

            // Calculate uniform spacing from first row (which is typically the fullest)
            int uniformExtraGap = 0;
            if (!rows.isEmpty()) {
                RowInfo firstRow = rows.get(0);
                uniformExtraGap = calculateExtraGap(target, maxWidth, firstRow.start, firstRow.end);
            }

            // Second pass: layout all rows with uniform spacing
            y = insets.top + getVgap();
            for (RowInfo row : rows) {
                layoutRow(target, x, y, maxWidth, row.height, row.start, row.end, ltr, uniformExtraGap);
                y += row.height + getVgap();
            }
        }
    }

    private int calculateExtraGap(Container target, int maxWidth, int start, int end) {
        int totalWidth = 0;
        int visibleCount = 0;

        for (int i = start; i < end; i++) {
            Component m = target.getComponent(i);
            if (m.isVisible()) {
                totalWidth += m.getWidth();
                visibleCount++;
            }
        }

        if (visibleCount == 0) return 0;

        int totalGaps = (visibleCount - 1) * getHgap();
        int usedWidth = totalWidth + totalGaps;
        int extraSpace = maxWidth - usedWidth;

        // Calculate extra gap if row is reasonably full
        double fillRatio = (double) usedWidth / maxWidth;
        return (fillRatio > 0.7 && extraSpace > 0) ? extraSpace / (visibleCount + 1) : 0;
    }

    private static class RowInfo {
        int start;
        int end;
        int height;

        RowInfo(int start, int end, int height) {
            this.start = start;
            this.end = end;
            this.height = height;
        }
    }

    private void layoutRow(Container target, int x, int y, int maxWidth, int rowHeight, int start, int end, boolean ltr, int uniformExtraGap) {
        // Position components with uniform spacing from first row
        int currentX = x + uniformExtraGap;

        for (int i = start; i < end; i++) {
            Component m = target.getComponent(i);
            if (m.isVisible()) {
                int cy = y + (rowHeight - m.getHeight()) / 2;
                if (ltr) {
                    m.setLocation(currentX, cy);
                } else {
                    m.setLocation(target.getWidth() - currentX - m.getWidth(), cy);
                }
                currentX += m.getWidth() + getHgap() + uniformExtraGap;
            }
        }
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    /**
     * Calculate the layout size based on the target container
     */
    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            // Always get the width from parent viewport for accurate wrapping
            int targetWidth = 0;

            // First try to get width from parent viewport
            Container parent = target.getParent();
            if (parent != null) {
                targetWidth = parent.getWidth();
            }

            // If parent width is 0, use target's current width
            if (targetWidth == 0) {
                targetWidth = target.getWidth();
            }

            // If still 0, traverse up to find valid width
            if (targetWidth == 0) {
                Container container = target;
                while (container.getSize().width == 0 && container.getParent() != null) {
                    container = container.getParent();
                }
                targetWidth = container.getSize().width;
            }

            // Fallback to max value if no valid width found
            if (targetWidth == 0) {
                targetWidth = Integer.MAX_VALUE;
            }

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
            int maxWidth = targetWidth - horizontalInsetsAndGap;

            // Fit components into the width
            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            int nmembers = target.getComponentCount();

            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);

                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                    // Can't fit in current row, start new row
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    // Add component to current row
                    if (rowWidth != 0) {
                        rowWidth += hgap;
                    }

                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }

            addRow(dim, rowWidth, rowHeight);

            dim.width += horizontalInsetsAndGap;
            dim.height += insets.top + insets.bottom + vgap * 2;

            // When using a scroll pane or the DecoratedLookAndFeel we need to
            // make sure the preferred size is less than the size of the
            // target container so shrinking the container size works correctly.
            Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);

            if (scrollPane != null && target.isValid()) {
                dim.width -= (hgap + 1);
            }

            return dim;
        }
    }

    /**
     * Add a row to the overall dimension
     */
    private void addRow(Dimension dim, int rowWidth, int rowHeight) {
        dim.width = Math.max(dim.width, rowWidth);

        if (dim.height > 0) {
            dim.height += getVgap();
        }

        dim.height += rowHeight;
    }
}
