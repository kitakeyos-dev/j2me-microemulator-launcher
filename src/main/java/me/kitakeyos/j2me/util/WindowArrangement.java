package me.kitakeyos.j2me.util;

import me.kitakeyos.j2me.model.EmulatorInstance;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Arranges instance windows
 */
public class WindowArrangement {

    public static void arrangeInstances(List<EmulatorInstance> instances) {
        if (instances.isEmpty()) {
            return;
        }

        // Get screen size
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;

        EmulatorInstance first = instances.get(0);
        int windowWidth = first.emulatorWindow.getWidth();
        int windowHeight = first.emulatorWindow.getHeight();

        // Spacing between windows
        int spacingX = 20;
        int spacingY = 20;

        // Starting position (top-left corner)
        int startX = 50;
        int startY = 50;

        // Calculate maximum columns based on screen size
        int maxCols = calculateMaximumColumns(screenWidth, windowWidth, spacingX, startX);

        int index = 0;
        for (EmulatorInstance emulatorInstance : instances) {
            JFrame frame = emulatorInstance.emulatorWindow;
            int col = index % maxCols;
            int row = index / maxCols;

            int x = startX + col * (windowWidth + spacingX);
            int y = startY + row * (windowHeight + spacingY);

            frame.setLocation(x, y);
            frame.setSize(windowWidth, windowHeight);

            index++;
        }
    }

    /**
     * Calculate maximum columns based on screen size
     */
    private static int calculateMaximumColumns(int screenWidth, int windowWidth, int spacingX, int startX) {
        // Calculate remaining space after subtracting starting position
        int availableWidth = screenWidth - startX;

        // Calculate number of columns that can fit
        // Formula: (availableWidth + spacingX) / (windowWidth + spacingX)
        int maxCols = (availableWidth + spacingX) / (windowWidth + spacingX);

        // Ensure at least 1 column
        return Math.max(1, maxCols);
    }

    /**
     * Calculate maximum columns for info message (using current screen size)
     */
    private static int calculateColumnGridForDisplay(List<EmulatorInstance> instances) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;


        EmulatorInstance first = instances.get(0);
        int windowWidth = first.emulatorWindow.getWidth();

        int spacingX = 20;
        int startX = 50;

        return calculateMaximumColumns(screenWidth, windowWidth, spacingX, startX);
    }

    public static String getGridInfo(List<EmulatorInstance> instances) {
        int instanceCount = instances.size();
        int maxCols = calculateColumnGridForDisplay(instances);
        int cols = Math.min(instanceCount, maxCols);
        int rows = (int) Math.ceil((double) instanceCount / maxCols);
        return "Arranged " + cols + " columns x " + rows + " rows (left to right, top to bottom)";
    }
}
