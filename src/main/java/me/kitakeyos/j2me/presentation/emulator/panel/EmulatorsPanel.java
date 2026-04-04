package me.kitakeyos.j2me.presentation.emulator.panel;

import me.kitakeyos.j2me.application.MainApplication;
import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.domain.application.service.ApplicationService;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorConfig;
import me.kitakeyos.j2me.infrastructure.persistence.emulator.EmulatorConfigRepositoryImpl;
import me.kitakeyos.j2me.presentation.common.component.BaseTabPanel;
import me.kitakeyos.j2me.presentation.common.component.ToastNotification;
import me.kitakeyos.j2me.presentation.common.dialog.ConfirmDialog;
import me.kitakeyos.j2me.presentation.common.dialog.MessageDialog;
import me.kitakeyos.j2me.presentation.common.i18n.Messages;

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

    private EmulatorConfigRepositoryImpl emulatorConfigRepository;

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
            ApplicationService applicationService, EmulatorConfigRepositoryImpl emulatorConfigRepository) {
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
        formPanel.setBorder(BorderFactory.createTitledBorder(Messages.get("emu.config.title")));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel(Messages.get("emu.name")), gbc);
        nameField = new JTextField();
        nameField.setPreferredSize(new Dimension(200, nameField.getPreferredSize().height));
        nameField.setToolTipText(Messages.get("emu.name.tooltip"));
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        formPanel.add(nameField, gbc);

        // JAR Path
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel(Messages.get("emu.jarPath")), gbc);
        jarPathField = new JTextField();
        jarPathField.setPreferredSize(new Dimension(200, jarPathField.getPreferredSize().height));
        jarPathField.setToolTipText(Messages.get("emu.jarPath.tooltip"));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(jarPathField, gbc);
        JButton browseButton = new JButton(Messages.get("common.browse"));
        browseButton.setToolTipText(Messages.get("emu.browse.tooltip"));
        browseButton.addActionListener(e -> browseJarFile());
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        formPanel.add(browseButton, gbc);

        // Default Display Size
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel(Messages.get("emu.defaultSize")), gbc);

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

        addButton = new JButton(Messages.get("emu.addButton"));
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

        clearButton = new JButton(Messages.get("emu.clearForm"));
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

        emptyLabel = new JLabel(Messages.get("emu.empty"));
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setForeground(Color.GRAY);
        emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        emulatorsListPanel.add(emptyLabel);

        JScrollPane scrollPane = new JScrollPane(emulatorsListPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                Messages.get("emu.list.title"),
                TitledBorder.LEFT,
                TitledBorder.TOP));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        return scrollPane;
    }

    @Override
    protected void onInitialized() {
        refreshEmulatorsList();
    }

    private void browseJarFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(Messages.get("emu.browseDialog.title"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileNameExtensionFilter(Messages.get("emu.jarFilter"), "jar"));

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
            MessageDialog.showError(this, Messages.get("common.error"), Messages.get("emu.error.nameEmpty"));
            return;
        }
        if (jarPath.isEmpty()) {
            MessageDialog.showError(this, Messages.get("common.error"), Messages.get("emu.error.jarEmpty"));
            return;
        }

        int width = (Integer) defaultWidthSpinner.getValue();
        int height = (Integer) defaultHeightSpinner.getValue();

        // Clone JAR to data/emulators/ directory
        String clonedPath;
        try {
            clonedPath = emulatorConfigRepository.cloneJarFile(jarPath);
        } catch (java.io.IOException ex) {
            MessageDialog.showError(this, Messages.get("common.error"), Messages.get("emu.error.cloneFailed", ex.getMessage()));
            return;
        }

        EmulatorConfig config = new EmulatorConfig(name, clonedPath, width, height);
        emulatorConfigRepository.save(config);
        refreshEmulatorsList();
        clearForm();

        ToastNotification.showSuccess(Messages.get("emu.added", name));
        statusBar.setSuccess(Messages.get("emu.addedStatus", name));
    }

    private void updateEmulator() {
        if (editingConfig == null)
            return;

        String name = nameField.getText().trim();
        String jarPath = jarPathField.getText().trim();

        if (name.isEmpty()) {
            MessageDialog.showError(this, Messages.get("common.error"), Messages.get("emu.error.nameEmpty"));
            return;
        }
        if (jarPath.isEmpty()) {
            MessageDialog.showError(this, Messages.get("common.error"), Messages.get("emu.error.jarEmpty"));
            return;
        }

        editingConfig.setName(name);
        editingConfig.setDefaultDisplayWidth((Integer) defaultWidthSpinner.getValue());
        editingConfig.setDefaultDisplayHeight((Integer) defaultHeightSpinner.getValue());

        // If JAR path changed, clone the new JAR
        if (!jarPath.equals(editingConfig.getJarPath())) {
            try {
                String clonedPath = emulatorConfigRepository.cloneJarFile(jarPath);
                editingConfig.setJarPath(clonedPath);
            } catch (java.io.IOException ex) {
                MessageDialog.showError(this, Messages.get("common.error"), Messages.get("emu.error.cloneFailed", ex.getMessage()));
                return;
            }
        }

        emulatorConfigRepository.save(editingConfig);
        refreshEmulatorsList();
        clearForm();

        ToastNotification.showSuccess(Messages.get("emu.updated", name));
        statusBar.setSuccess(Messages.get("emu.updatedStatus", name));
    }

    private void removeEmulator(EmulatorConfig config) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        boolean confirmed = ConfirmDialog.showConfirm(owner, Messages.get("emu.remove.title"),
                Messages.get("emu.remove.confirm", config.getName()));
        if (confirmed) {
            emulatorConfigRepository.remove(config.getId());
            refreshEmulatorsList();

            // Clear form if we were editing this one
            if (editingConfig != null && editingConfig.getId().equals(config.getId())) {
                clearForm();
            }

            ToastNotification.showInfo(Messages.get("emu.removed", config.getName()));
            statusBar.setInfo(Messages.get("emu.removedStatus", config.getName()));
        }
    }

    private void editEmulator(EmulatorConfig config) {
        editingConfig = config;
        nameField.setText(config.getName());
        jarPathField.setText(config.getJarPath());
        defaultWidthSpinner.setValue(config.getDefaultDisplayWidth());
        defaultHeightSpinner.setValue(config.getDefaultDisplayHeight());

        addButton.setText(Messages.get("emu.saveChanges"));
        clearButton.setText(Messages.get("emu.cancelEdit"));

        statusBar.setInfo(Messages.get("emu.editing", config.getName()));
    }

    private void clearForm() {
        editingConfig = null;
        nameField.setText("");
        jarPathField.setText("");
        defaultWidthSpinner.setValue(240);
        defaultHeightSpinner.setValue(320);

        addButton.setText(Messages.get("emu.addButton"));
        clearButton.setText(Messages.get("emu.clearForm"));
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
        statusBar.setInfo(Messages.get("emu.status.count", configs.size()));
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
            detailsLabel.setToolTipText(Messages.get("emu.jarNotFound"));
        }
        infoPanel.add(detailsLabel);

        row.add(infoPanel, BorderLayout.CENTER);

        // Status indicator
        if (!valid) {
            JLabel warningLabel = new JLabel("\u26a0");
            warningLabel.setForeground(Color.RED);
            warningLabel.setToolTipText(Messages.get("emu.jarNotFoundShort"));
            row.add(warningLabel, BorderLayout.WEST);
        }

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);

        JButton editButton = new JButton(Messages.get("common.edit"));
        editButton.setPreferredSize(new Dimension(70, 26));
        editButton.addActionListener(e -> editEmulator(config));

        JButton removeButton = new JButton(Messages.get("common.remove"));
        removeButton.setPreferredSize(new Dimension(80, 26));
        removeButton.addActionListener(e -> removeEmulator(config));

        buttonPanel.add(editButton);
        buttonPanel.add(removeButton);

        row.add(buttonPanel, BorderLayout.EAST);

        return row;
    }
}
