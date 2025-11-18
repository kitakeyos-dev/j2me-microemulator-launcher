package me.kitakeyos.j2me.ui.builder;

import javax.swing.*;
import java.awt.*;

/**
 * Builds configuration panel for the application
 */
public class ConfigurationPanelBuilder {

    /**
     * Create configuration panel with JComboBox for application selection
     */
    public static JPanel createConfigurationPanel(JComboBox<?> applicationComboBox, JSpinner instanceCountSpinner,
                                           JTextField microemulatorPathField, Runnable onOpenSettings) {
        return createConfigurationPanel(applicationComboBox, instanceCountSpinner, microemulatorPathField, null, null, onOpenSettings);
    }

    /**
     * Create configuration panel with display size configuration
     */
    public static JPanel createConfigurationPanel(JComboBox<?> applicationComboBox, JSpinner instanceCountSpinner,
                                           JTextField microemulatorPathField, JSpinner displayWidthSpinner,
                                           JSpinner displayHeightSpinner, Runnable onOpenSettings) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // J2ME Application
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        panel.add(new JLabel("J2ME Application:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        applicationComboBox.setPreferredSize(new Dimension(300, applicationComboBox.getPreferredSize().height));
        panel.add(applicationComboBox, gbc);

        // Number of Instances
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Number of Instances:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.3;
        panel.add(instanceCountSpinner, gbc);

        // Display Size (Width x Height)
        if (displayWidthSpinner != null && displayHeightSpinner != null) {
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 1;
            gbc.weightx = 0.0;
            panel.add(new JLabel("Display Size (W x H):"), gbc);

            // Create a panel to hold both width and height spinners
            JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            sizePanel.add(displayWidthSpinner);
            sizePanel.add(new JLabel("x"));
            sizePanel.add(displayHeightSpinner);

            gbc.gridx = 1;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            panel.add(sizePanel, gbc);
        }

        // MicroEmulator Path
        gbc.gridx = 0;
        gbc.gridy = displayWidthSpinner != null ? 3 : 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("MicroEmulator Path:"), gbc);
        microemulatorPathField.setText("microemulator.jar");
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(microemulatorPathField, gbc);
        JButton settingsButton = new JButton("Settings");
        settingsButton.setToolTipText("Open settings to configure MicroEmulator path");
        settingsButton.addActionListener(e -> onOpenSettings.run());
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        panel.add(settingsButton, gbc);

        return panel;
    }
}
