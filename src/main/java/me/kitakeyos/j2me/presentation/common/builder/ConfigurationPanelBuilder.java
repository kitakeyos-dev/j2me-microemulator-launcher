package me.kitakeyos.j2me.presentation.common.builder;

import me.kitakeyos.j2me.presentation.common.i18n.Messages;

import javax.swing.*;
import java.awt.*;

/**
 * Builds configuration panel for the application
 */
public class ConfigurationPanelBuilder {

    /**
     * Create configuration panel with emulator selector and display size
     * configuration
     */
    public static JPanel createConfigurationPanel(JComboBox<?> applicationComboBox, JSpinner instanceCountSpinner,
            JComboBox<?> emulatorComboBox, JSpinner displayWidthSpinner,
            JSpinner displayHeightSpinner, JComboBox<String> defaultSpeedComboBox) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(Messages.get("config.title")));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // J2ME Application
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        panel.add(new JLabel(Messages.get("config.j2meApp")), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 0.0;
        applicationComboBox.setPreferredSize(new Dimension(200, applicationComboBox.getPreferredSize().height));
        panel.add(applicationComboBox, gbc);

        // Number of Instances
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel(Messages.get("config.instanceCount")), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.3;
        panel.add(instanceCountSpinner, gbc);

        // Display Size (Width x Height)
        if (displayWidthSpinner != null && displayHeightSpinner != null) {
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 1;
            gbc.weightx = 0.0;
            panel.add(new JLabel(Messages.get("config.displaySize")), gbc);

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

        // Emulator selector
        gbc.gridx = 0;
        gbc.gridy = displayWidthSpinner != null ? 3 : 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel(Messages.get("config.emulator")), gbc);
        emulatorComboBox.setPreferredSize(new Dimension(200, emulatorComboBox.getPreferredSize().height));
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        panel.add(emulatorComboBox, gbc);

        // Default Speed
        gbc.gridy++;
        gbc.gridwidth = 1;
        JLabel speedLabel = new JLabel(Messages.get("config.defaultSpeed") + " ");
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        panel.add(speedLabel, gbc);

        gbc.gridx = 1;
        panel.add(defaultSpeedComboBox, gbc);

        return panel;
    }
}
