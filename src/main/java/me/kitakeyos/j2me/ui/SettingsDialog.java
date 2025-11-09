package me.kitakeyos.j2me.ui;

import me.kitakeyos.j2me.config.ApplicationConfig;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Configuration dialog allowing users to set microemulator path
 */
public class SettingsDialog extends JDialog {
    private ApplicationConfig applicationConfig;
    private JTextField microemulatorPathField;
    private JButton browseButton;
    private JButton saveButton;
    private JButton cancelButton;
    private boolean isSettingsModified = false;
    
    public SettingsDialog(Frame parent, ApplicationConfig applicationConfig) {
        super(parent, "Settings", true);
        this.applicationConfig = applicationConfig;
        initComponents();
        loadCurrentSettings();
    }
    
    private void initComponents() {
        setSize(600, 250);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Panel cấu hình
        JPanel configPanel = createConfigPanel();
        mainPanel.add(configPanel, BorderLayout.CENTER);
        
        // Panel nút bấm
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        panel.setBorder(BorderFactory.createTitledBorder("MicroEmulator Configuration"));
        panel.setBackground(Color.WHITE);
        
        // Label and field for microemulator path
        JLabel pathLabel = new JLabel("MicroEmulator Path:");
        pathLabel.setFont(new Font("Arial", Font.BOLD, 12));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 5, 10);
        panel.add(pathLabel, gbc);
        
        // Panel containing text field and button
        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        pathPanel.setBackground(Color.WHITE);
        
        microemulatorPathField = new JTextField();
        microemulatorPathField.setFont(new Font("Arial", Font.PLAIN, 11));
        microemulatorPathField.setPreferredSize(new Dimension(400, 25));
        microemulatorPathField.setToolTipText("Enter full path to microemulator.jar file");
        pathPanel.add(microemulatorPathField, BorderLayout.CENTER);

        browseButton = new JButton("Browse...");
        browseButton.setFont(new Font("Arial", Font.BOLD, 11));
        browseButton.setPreferredSize(new Dimension(80, 25));
        browseButton.setToolTipText("Browse and select microemulator.jar file");
        browseButton.addActionListener(this::selectMicroemulatorFile);
        pathPanel.add(browseButton, BorderLayout.EAST);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 10, 10, 10);
        panel.add(pathPanel, gbc);
        
        // Status information
        JLabel statusLabel = new JLabel("Select microemulator.jar file to run J2ME applications");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        statusLabel.setForeground(Color.GRAY);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 10, 10);
        panel.add(statusLabel, gbc);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        
        saveButton = new JButton("Save");
        saveButton.setFont(new Font("Arial", Font.BOLD, 12));
        saveButton.setPreferredSize(new Dimension(80, 30));
        saveButton.setToolTipText("Save configuration and close dialog");
        saveButton.addActionListener(this::saveSettings);

        cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Arial", Font.BOLD, 12));
        cancelButton.setPreferredSize(new Dimension(80, 30));
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(e -> dispose());
        
        panel.add(saveButton);
        panel.add(cancelButton);
        
        return panel;
    }
    
    private void selectMicroemulatorFile(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select MicroEmulator File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JAR Files (*.jar)", "jar"));
        
        // Set default directory
        String currentPath = microemulatorPathField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            if (currentFile.getParent() != null) {
                fileChooser.setCurrentDirectory(new File(currentFile.getParent()));
            }
        }
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            microemulatorPathField.setText(selectedFile.getAbsolutePath());
        }
    }
    
    private void saveSettings(ActionEvent e) {
        String path = microemulatorPathField.getText().trim();
        
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please select MicroEmulator path!", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            JOptionPane.showMessageDialog(this, 
                "File does not exist or is invalid!", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!path.toLowerCase().endsWith(".jar")) {
            JOptionPane.showMessageDialog(this, 
                "Please select a JAR file!", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        applicationConfig.setMicroemulatorPath(path);
        applicationConfig.saveConfiguration();
        isSettingsModified = true;
        
        JOptionPane.showMessageDialog(this, 
            "Configuration saved successfully!", 
            "Success", 
            JOptionPane.INFORMATION_MESSAGE);
        
        dispose();
    }
    
    private void loadCurrentSettings() {
        microemulatorPathField.setText(applicationConfig.getMicroemulatorPath());
    }
    
    /**
     * Check if settings have changed
     */
    public boolean isSettingsModified() {
        return isSettingsModified;
    }
}
