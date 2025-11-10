package me.kitakeyos.j2me.ui.builder;

import me.kitakeyos.j2me.model.EmulatorInstance;
import me.kitakeyos.j2me.model.EmulatorInstance.InstanceState;

import javax.swing.*;
import java.awt.*;

/**
 * Builds UI for emulator instances
 */
public class EmulatorInstanceUIBuilder {

    public static JPanel buildEmulatorInstancePanel(EmulatorInstance emulatorInstance,
                                                    Runnable onMoveUp, Runnable onMoveDown,
                                                    Runnable onRun, Runnable onStop, Runnable onRemove) {
        JPanel itemPanel = new JPanel(new BorderLayout(10, 10));
        itemPanel.setBackground(Color.WHITE);
        itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        Color borderColor = getBorderColorByState(emulatorInstance.state);
        itemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JPanel infoPanel = createEmulatorInstanceInfoPanel(emulatorInstance);
        JPanel buttonPanel = createEmulatorInstanceButtonPanel(
                emulatorInstance, onMoveUp, onMoveDown, onRun, onStop, onRemove
        );

        itemPanel.add(infoPanel, BorderLayout.CENTER);
        itemPanel.add(buttonPanel, BorderLayout.EAST);

        return itemPanel;
    }

    private static Color getBorderColorByState(InstanceState state) {
        switch (state) {
            case CREATED:
                return new Color(100, 150, 200);
            // Blue - Created
            case STARTING:
                return new Color(255, 165, 0);
            // Orange - Starting
            case RUNNING:
                return new Color(34, 139, 34);
            // Green - Running
            case STOPPED:
                return new Color(200, 50, 50);    // Red - Stopped

            default:
                throw new IllegalArgumentException();
        }
    }

    private static String getStateDisplayText(InstanceState state) {
        switch (state) {
            case CREATED:
                return "<span style='color:#6495ED'>Created</span>";
            case STARTING:
                return "<span style='color:#FFA500'>Starting...</span>";
            case RUNNING:
                return "<span style='color:#228B22'>Running</span>";
            case STOPPED:
                return "<span style='color:#DC143C'>Stopped</span>";
            default:
                throw new IllegalArgumentException();
        }
    }

    private static JPanel createEmulatorInstanceInfoPanel(EmulatorInstance emulatorInstance) {
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("<html><b>Instance #" + emulatorInstance.instanceId +
                "</b> - Status: " + getStateDisplayText(emulatorInstance.state) + "</html>");
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        String fileName = new java.io.File(emulatorInstance.j2meFilePath).getName();
        JLabel fileLabel = new JLabel("File: " + fileName);
        fileLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        fileLabel.setForeground(Color.DARK_GRAY);

        JLabel pathLabel = new JLabel("Path: " + emulatorInstance.j2meFilePath);
        pathLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        pathLabel.setForeground(Color.GRAY);

        infoPanel.add(titleLabel);
        infoPanel.add(fileLabel);
        infoPanel.add(pathLabel);

        return infoPanel;
    }

    private static JPanel createEmulatorInstanceButtonPanel(
            EmulatorInstance instance,
            Runnable onMoveUp, Runnable onMoveDown,
            Runnable onRun, Runnable onStop, Runnable onRemove) {

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        buttonPanel.setOpaque(false);

        // Move buttons
        JButton moveUpButton = new JButton("↑");
        moveUpButton.setPreferredSize(new Dimension(40, 30));
        moveUpButton.setToolTipText("Move Up");
        moveUpButton.addActionListener(e -> onMoveUp.run());

        JButton moveDownButton = new JButton("↓");
        moveDownButton.setPreferredSize(new Dimension(40, 30));
        moveDownButton.setToolTipText("Move Down");
        moveDownButton.addActionListener(e -> onMoveDown.run());

        // Action buttons based on state
        if (instance.state == InstanceState.CREATED || instance.state == InstanceState.STOPPED) {
            JButton runButton = new JButton("Run");
            runButton.setBackground(new Color(34, 139, 34));
            runButton.setFont(new Font("Arial", Font.BOLD, 11));
            runButton.setPreferredSize(new Dimension(70, 30));
            runButton.setToolTipText("Run this instance");
            runButton.addActionListener(e -> onRun.run());
            buttonPanel.add(runButton);

            JButton removeButton = new JButton("Remove");
            removeButton.setBackground(new Color(220, 20, 60));
            removeButton.setFont(new Font("Arial", Font.BOLD, 11));
            removeButton.setPreferredSize(new Dimension(80, 30));
            removeButton.setToolTipText("Remove this instance");
            removeButton.addActionListener(e -> onRemove.run());
            buttonPanel.add(removeButton);
        } else if (instance.state == InstanceState.RUNNING) {
            JButton stopButton = new JButton("Stop");
            stopButton.setBackground(new Color(220, 20, 60));
            stopButton.setFont(new Font("Arial", Font.BOLD, 11));
            stopButton.setPreferredSize(new Dimension(70, 30));
            stopButton.setToolTipText("Stop this instance");
            stopButton.addActionListener(e -> onStop.run());
            buttonPanel.add(stopButton);
        } else if (instance.state == InstanceState.STARTING) {
            JLabel startingLabel = new JLabel("Starting...");
            startingLabel.setFont(new Font("Arial", Font.ITALIC, 11));
            startingLabel.setForeground(new Color(255, 165, 0));
            buttonPanel.add(startingLabel);
        }

        buttonPanel.add(moveUpButton);
        buttonPanel.add(moveDownButton);

        return buttonPanel;
    }

    /**
     * Update UI panel for an existing instance
     */
    public static void updateInstancePanel(EmulatorInstance instance,
                                           Runnable onMoveUp, Runnable onMoveDown,
                                           Runnable onRun, Runnable onStop, Runnable onRemove) {
        if (instance.uiPanel != null) {
            instance.uiPanel.removeAll();

            JPanel newPanel = buildEmulatorInstancePanel(instance, onMoveUp, onMoveDown, onRun, onStop, onRemove);
            instance.uiPanel.setLayout(new BorderLayout());

            // Add components with correct BorderLayout constraints
            // Assuming consistent order: infoPanel at index 0 (CENTER), buttonPanel at index 1 (EAST)
            Component[] components = newPanel.getComponents();
            if (components.length >= 2) {
                instance.uiPanel.add(components[0], BorderLayout.CENTER);
                instance.uiPanel.add(components[1], BorderLayout.EAST);
            }

            instance.uiPanel.setBorder(newPanel.getBorder());
            instance.uiPanel.setBackground(newPanel.getBackground());
            instance.uiPanel.setMaximumSize(newPanel.getMaximumSize());  // Reapply max size for consistency
            instance.uiPanel.revalidate();
            instance.uiPanel.repaint();
        }
    }
}