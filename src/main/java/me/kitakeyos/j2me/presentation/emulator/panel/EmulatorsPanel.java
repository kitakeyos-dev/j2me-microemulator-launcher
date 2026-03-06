package me.kitakeyos.j2me.presentation.emulator.panel;

import me.kitakeyos.j2me.application.MainApplication;
import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.domain.application.service.ApplicationService;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorConfig;
import me.kitakeyos.j2me.domain.emulator.repository.EmulatorConfigRepository;
import me.kitakeyos.j2me.presentation.common.component.BaseTabPanel;
import me.kitakeyos.j2me.presentation.common.component.ToastNotification;
import me.kitakeyos.j2me.presentation.common.dialog.ConfirmDialog;
import me.kitakeyos.j2me.presentation.common.dialog.MessageDialog;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

/**
 * Tab panel for managing installed emulator configurations.
 * Allows users to add, edit, and remove emulator JAR configurations.
 */
public class EmulatorsPanel extends BaseTabPanel {

    private EmulatorConfigRepository emulatorConfigRepository;

    // Form fields
    private JTextField nameField;
    private JTextField jarPathField;
    private JSpinner defaultWidthSpinner;
    private JSpinner defaultHeightSpinner;
    private JButton addButton;
    private JButton clearButton;

    // List
    private JPanel emulatorsListPanel;
    private JLabel emptyLabel;

    // Currently editing
    private EmulatorConfig editingConfig;

    public EmulatorsPanel(MainApplication mainApplication, ApplicationConfig applicationConfig,
            ApplicationService applicationService, EmulatorConfigRepository emulatorConfigRepository) {
        super(mainApplication, applicationConfig, applicationService);
        // Assign after super() — onInitialized() will skip refresh due to null guard
        this.emulatorConfigRepository = emulatorConfigRepository;
        // Now manually refresh the list
        refreshEmulatorsList();
    }

    @Override
    protected JComponent createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Emulator Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Name:"), gbc);
        nameField = new JTextField();
        nameField.setPreferredSize(new Dimension(200, nameField.getPreferredSize().height));
        nameField.setToolTipText("Display name for this emulator");
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        formPanel.add(nameField, gbc);

        // JAR Path
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("JAR Path:"), gbc);
        jarPathField = new JTextField();
        jarPathField.setPreferredSize(new Dimension(200, jarPathField.getPreferredSize().height));
        jarPathField.setToolTipText("Path to the emulator JAR file");
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(jarPathField, gbc);
        JButton browseButton = new JButton("Browse");
        browseButton.setToolTipText("Browse for JAR file");
        browseButton.addActionListener(e -> browseJarFile());
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        formPanel.add(browseButton, gbc);

        // Default Display Size
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Default Size (W x H):"), gbc);

        defaultWidthSpinner = new JSpinner(new SpinnerNumberModel(240, 128, 800, 1));
        defaultWidthSpinner.setPreferredSize(new Dimension(80, defaultWidthSpinner.getPreferredSize().height));
        defaultHeightSpinner = new JSpinner(new SpinnerNumberModel(320, 128, 1000, 1));
        defaultHeightSpinner.setPreferredSize(new Dimension(80, defaultHeightSpinner.getPreferredSize().height));

        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        sizePanel.add(defaultWidthSpinner);
        sizePanel.add(new JLabel("x"));
        sizePanel.add(defaultHeightSpinner);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        formPanel.add(sizePanel, gbc);

        headerPanel.add(formPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        addButton = new JButton("Add Emulator");
        addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addButton.setPreferredSize(new Dimension(140, 30));
        addButton.setMaximumSize(new Dimension(140, 30));
        addButton.addActionListener(e -> {
            if (editingConfig != null) {
                updateEmulator();
            } else {
                addEmulator();
            }
        });

        clearButton = new JButton("Clear Form");
        clearButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearButton.setPreferredSize(new Dimension(140, 30));
        clearButton.setMaximumSize(new Dimension(140, 30));
        clearButton.addActionListener(e -> clearForm());

        buttonsPanel.add(addButton);
        buttonsPanel.add(Box.createVerticalStrut(8));
        buttonsPanel.add(clearButton);

        headerPanel.add(buttonsPanel, BorderLayout.EAST);

        return headerPanel;
    }

    @Override
    protected JComponent createContent() {
        emulatorsListPanel = new JPanel();
        emulatorsListPanel.setLayout(new BoxLayout(emulatorsListPanel, BoxLayout.Y_AXIS));

        emptyLabel = new JLabel("No emulators configured. Add one above.");
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setForeground(Color.GRAY);
        emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        emulatorsListPanel.add(emptyLabel);

        JScrollPane scrollPane = new JScrollPane(emulatorsListPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                "Installed Emulators",
                TitledBorder.LEFT,
                TitledBorder.TOP));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        return scrollPane;
    }

    @Override
    protected void onInitialized() {
        refreshEmulatorsList();
    }

    private void browseJarFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Emulator JAR File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileNameExtensionFilter("JAR Files (*.jar)", "jar"));

        // Set current directory from existing path
        String currentPath = jarPathField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
                fileChooser.setCurrentDirectory(currentFile.getParentFile());
            }
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            jarPathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void addEmulator() {
        String name = nameField.getText().trim();
        String jarPath = jarPathField.getText().trim();

        if (name.isEmpty()) {
            MessageDialog.showError(this, "Error", "Please enter a name for the emulator.");
            return;
        }
        if (jarPath.isEmpty()) {
            MessageDialog.showError(this, "Error", "Please specify the JAR file path.");
            return;
        }

        int width = (Integer) defaultWidthSpinner.getValue();
        int height = (Integer) defaultHeightSpinner.getValue();

        EmulatorConfig config = new EmulatorConfig(name, jarPath, width, height);
        emulatorConfigRepository.save(config);
        refreshEmulatorsList();
        clearForm();

        ToastNotification.showSuccess("Emulator '" + name + "' added successfully");
        statusBar.setSuccess("Added emulator: " + name);
    }

    private void updateEmulator() {
        if (editingConfig == null)
            return;

        String name = nameField.getText().trim();
        String jarPath = jarPathField.getText().trim();

        if (name.isEmpty()) {
            MessageDialog.showError(this, "Error", "Please enter a name for the emulator.");
            return;
        }
        if (jarPath.isEmpty()) {
            MessageDialog.showError(this, "Error", "Please specify the JAR file path.");
            return;
        }

        editingConfig.setName(name);
        editingConfig.setJarPath(jarPath);
        editingConfig.setDefaultDisplayWidth((Integer) defaultWidthSpinner.getValue());
        editingConfig.setDefaultDisplayHeight((Integer) defaultHeightSpinner.getValue());

        emulatorConfigRepository.save(editingConfig);
        refreshEmulatorsList();
        clearForm();

        ToastNotification.showSuccess("Emulator '" + name + "' updated successfully");
        statusBar.setSuccess("Updated emulator: " + name);
    }

    private void removeEmulator(EmulatorConfig config) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        boolean confirmed = ConfirmDialog.showConfirm(owner, "Remove Emulator",
                "Are you sure you want to remove '" + config.getName() + "'?");
        if (confirmed) {
            emulatorConfigRepository.remove(config.getId());
            refreshEmulatorsList();

            // Clear form if we were editing this one
            if (editingConfig != null && editingConfig.getId().equals(config.getId())) {
                clearForm();
            }

            ToastNotification.showInfo("Emulator '" + config.getName() + "' removed");
            statusBar.setInfo("Removed emulator: " + config.getName());
        }
    }

    private void editEmulator(EmulatorConfig config) {
        editingConfig = config;
        nameField.setText(config.getName());
        jarPathField.setText(config.getJarPath());
        defaultWidthSpinner.setValue(config.getDefaultDisplayWidth());
        defaultHeightSpinner.setValue(config.getDefaultDisplayHeight());

        addButton.setText("Save Changes");
        clearButton.setText("Cancel");

        statusBar.setInfo("Editing: " + config.getName());
    }

    private void clearForm() {
        editingConfig = null;
        nameField.setText("");
        jarPathField.setText("");
        defaultWidthSpinner.setValue(240);
        defaultHeightSpinner.setValue(320);

        addButton.setText("Add Emulator");
        clearButton.setText("Clear Form");
    }

    private void refreshEmulatorsList() {
        emulatorsListPanel.removeAll();

        if (emulatorConfigRepository == null)
            return;
        java.util.List<EmulatorConfig> configs = emulatorConfigRepository.getAll();
        if (configs.isEmpty()) {
            emulatorsListPanel.add(emptyLabel);
            emptyLabel.setVisible(true);
        } else {
            for (EmulatorConfig config : configs) {
                emulatorsListPanel.add(createEmulatorRow(config));
                emulatorsListPanel.add(Box.createVerticalStrut(4));
            }
        }

        emulatorsListPanel.revalidate();
        emulatorsListPanel.repaint();

        // Update status
        statusBar.setInfo(configs.size() + " emulator(s) configured");
    }

    private JPanel createEmulatorRow(EmulatorConfig config) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // Info section
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(config.getName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        infoPanel.add(nameLabel);

        boolean valid = config.isValid();
        String details = config.getJarPath() + "  |  " +
                config.getDefaultDisplayWidth() + "x" + config.getDefaultDisplayHeight();
        JLabel detailsLabel = new JLabel(details);
        detailsLabel.setFont(detailsLabel.getFont().deriveFont(Font.PLAIN, 11f));
        detailsLabel.setForeground(valid ? Color.GRAY : Color.RED);
        if (!valid) {
            detailsLabel.setToolTipText("JAR file not found or invalid");
        }
        infoPanel.add(detailsLabel);

        row.add(infoPanel, BorderLayout.CENTER);

        // Status indicator
        if (!valid) {
            JLabel warningLabel = new JLabel("⚠");
            warningLabel.setForeground(Color.RED);
            warningLabel.setToolTipText("JAR file not found");
            row.add(warningLabel, BorderLayout.WEST);
        }

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);

        JButton editButton = new JButton("Edit");
        editButton.setPreferredSize(new Dimension(70, 26));
        editButton.addActionListener(e -> editEmulator(config));

        JButton removeButton = new JButton("Remove");
        removeButton.setPreferredSize(new Dimension(80, 26));
        removeButton.addActionListener(e -> removeEmulator(config));

        buttonPanel.add(editButton);
        buttonPanel.add(removeButton);

        row.add(buttonPanel, BorderLayout.EAST);

        return row;
    }
}
