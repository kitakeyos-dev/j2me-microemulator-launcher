package me.kitakeyos.j2me.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Builds configuration panel for the application
 */
public class ConfigurationPanelBuilder {

    public static JPanel createConfigurationPanel(JTextField j2meFilePathField, JSpinner instanceCountSpinner,
                                           JTextField microemulatorPathField, Runnable onBrowseJ2meFile, Runnable onOpenSettings) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // J2ME File
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("J2ME File (JAR/JAD):"), gbc);
        j2meFilePathField.setEditable(false);
        gbc.gridx = 1;
        panel.add(j2meFilePathField, gbc);
        JButton browseJ2meButton = new JButton("Browse...");
        browseJ2meButton.addActionListener(e -> onBrowseJ2meFile.run());
        gbc.gridx = 2;
        panel.add(browseJ2meButton, gbc);

        // Number of Instances
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Number of Instances:"), gbc);
        gbc.gridx = 1;
        panel.add(instanceCountSpinner, gbc);

        // MicroEmulator Path
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("MicroEmulator Path:"), gbc);
        microemulatorPathField.setText("microemulator.jar");
        gbc.gridx = 1;
        panel.add(microemulatorPathField, gbc);
        JButton settingsButton = new JButton("Settings");
        settingsButton.addActionListener(e -> onOpenSettings.run());
        gbc.gridx = 2;
        panel.add(settingsButton, gbc);

        return panel;
    }
}
