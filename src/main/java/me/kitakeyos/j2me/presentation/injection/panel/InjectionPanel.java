package me.kitakeyos.j2me.presentation.injection.panel;

import me.kitakeyos.j2me.application.MainApplication;
import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.domain.application.service.ApplicationService;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.injection.model.InjectionLogger;
import me.kitakeyos.j2me.domain.injection.service.InjectionService;
import me.kitakeyos.j2me.presentation.common.component.BaseTabPanel;
import me.kitakeyos.j2me.presentation.common.component.ToastNotification;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Tab panel for Java class injection.
 * Allows developers to load compiled JAR files and execute
 * InjectionEntry implementations against running emulator instances.
 */
public class InjectionPanel extends BaseTabPanel {

    private final InjectionService injectionService;

    // Header components
    private JLabel jarPathLabel;
    private JComboBox<String> instanceComboBox;

    // Content components
    private JPanel entryListPanel;
    private JLabel emptyLabel;
    private JTextArea logTextArea;

    public InjectionPanel(MainApplication mainApplication, ApplicationConfig applicationConfig,
                          ApplicationService applicationService) {
        super(mainApplication, applicationConfig, applicationService);
        this.injectionService = new InjectionService();
    }

    @Override
    protected JComponent createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));

        // Left: JAR info + buttons
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("JAR File"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // JAR path label
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        leftPanel.add(new JLabel("Loaded:"), gbc);

        jarPathLabel = new JLabel("No JAR loaded");
        jarPathLabel.setForeground(Color.GRAY);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        leftPanel.add(jarPathLabel, gbc);

        // Target instance
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        leftPanel.add(new JLabel("Target Instance:"), gbc);

        instanceComboBox = new JComboBox<>();
        instanceComboBox.setToolTipText("Select running instance to inject into");
        instanceComboBox.setPreferredSize(new Dimension(200, instanceComboBox.getPreferredSize().height));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        leftPanel.add(instanceComboBox, gbc);

        headerPanel.add(leftPanel, BorderLayout.CENTER);

        // Right: Action buttons
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton loadButton = new JButton("Load JAR");
        loadButton.setToolTipText("Browse and load a JAR file containing InjectionEntry classes");
        loadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadButton.setPreferredSize(new Dimension(140, 30));
        loadButton.setMaximumSize(new Dimension(140, 30));
        loadButton.addActionListener(e -> browseAndLoadJar());

        JButton reloadButton = new JButton("Reload JAR");
        reloadButton.setToolTipText("Reload the current JAR file (hot-reload)");
        reloadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        reloadButton.setPreferredSize(new Dimension(140, 30));
        reloadButton.setMaximumSize(new Dimension(140, 30));
        reloadButton.addActionListener(e -> reloadJar());

        JButton refreshButton = new JButton("Refresh Instances");
        refreshButton.setToolTipText("Refresh the list of running instances");
        refreshButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        refreshButton.setPreferredSize(new Dimension(140, 30));
        refreshButton.setMaximumSize(new Dimension(140, 30));
        refreshButton.addActionListener(e -> refreshInstanceList());

        JButton clearLogButton = new JButton("Clear Log");
        clearLogButton.setToolTipText("Clear the log output");
        clearLogButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearLogButton.setPreferredSize(new Dimension(140, 30));
        clearLogButton.setMaximumSize(new Dimension(140, 30));
        clearLogButton.addActionListener(e -> logTextArea.setText(""));

        buttonsPanel.add(loadButton);
        buttonsPanel.add(Box.createVerticalStrut(8));
        buttonsPanel.add(reloadButton);
        buttonsPanel.add(Box.createVerticalStrut(8));
        buttonsPanel.add(refreshButton);
        buttonsPanel.add(Box.createVerticalStrut(8));
        buttonsPanel.add(clearLogButton);

        headerPanel.add(buttonsPanel, BorderLayout.EAST);

        return headerPanel;
    }

    @Override
    protected JComponent createContent() {
        // Top: Entry class list
        entryListPanel = new JPanel();
        entryListPanel.setLayout(new BoxLayout(entryListPanel, BoxLayout.Y_AXIS));

        emptyLabel = new JLabel("No JAR loaded. Click 'Load JAR' to import an injection JAR.");
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setForeground(Color.GRAY);
        emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        entryListPanel.add(emptyLabel);

        JScrollPane entryScrollPane = new JScrollPane(entryListPanel);
        entryScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                "Discovered Entry Classes",
                TitledBorder.LEFT,
                TitledBorder.TOP));
        entryScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        entryScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        entryScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Bottom: Log output
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logTextArea.setBackground(new Color(30, 30, 30));
        logTextArea.setForeground(new Color(200, 200, 200));
        logTextArea.setCaretColor(Color.WHITE);

        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                "Log Output",
                TitledBorder.LEFT,
                TitledBorder.TOP));
        logScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        logScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        logScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Split pane: entries on top, log on bottom
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, entryScrollPane, logScrollPane);
        splitPane.setResizeWeight(0.3);
        splitPane.setDividerLocation(150);

        return splitPane;
    }

    @Override
    protected void onInitialized() {
        refreshInstanceList();
    }

    private void browseAndLoadJar() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Injection JAR File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileNameExtensionFilter("JAR Files (*.jar)", "jar"));

        // Start from last loaded JAR location
        if (injectionService.getLoadedJarFile() != null) {
            fileChooser.setCurrentDirectory(injectionService.getLoadedJarFile().getParentFile());
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            loadJar(fileChooser.getSelectedFile());
        }
    }

    private void loadJar(File jarFile) {
        try {
            List<String> entries = injectionService.loadJar(jarFile);
            jarPathLabel.setText(jarFile.getName());
            jarPathLabel.setForeground(new Color(0, 128, 0));
            jarPathLabel.setToolTipText(jarFile.getAbsolutePath());

            refreshEntryList(entries);

            if (entries.isEmpty()) {
                statusBar.setWarning("JAR loaded but no InjectionEntry classes found");
                ToastNotification.showWarning("No InjectionEntry classes found in " + jarFile.getName());
            } else {
                statusBar.setSuccess("Loaded " + entries.size() + " entry class(es) from " + jarFile.getName());
                ToastNotification.showSuccess("Loaded " + entries.size() + " entry class(es)");
            }
        } catch (Exception e) {
            statusBar.setError("Failed to load JAR: " + e.getMessage());
            ToastNotification.showError("Failed to load: " + e.getMessage());
        }
    }

    private void reloadJar() {
        if (!injectionService.isJarLoaded() && injectionService.getLoadedJarFile() == null) {
            statusBar.setWarning("No JAR to reload");
            return;
        }

        try {
            List<String> entries = injectionService.reloadJar();
            refreshEntryList(entries);
            statusBar.setSuccess("Reloaded: " + entries.size() + " entry class(es)");
            ToastNotification.showSuccess("JAR reloaded");
        } catch (Exception e) {
            statusBar.setError("Reload failed: " + e.getMessage());
        }
    }

    private void refreshEntryList(List<String> entryClassNames) {
        entryListPanel.removeAll();

        if (entryClassNames.isEmpty()) {
            emptyLabel.setText("No InjectionEntry classes found in JAR.");
            entryListPanel.add(emptyLabel);
        } else {
            for (String className : entryClassNames) {
                entryListPanel.add(createEntryRow(className));
                entryListPanel.add(Box.createVerticalStrut(4));
            }
        }

        entryListPanel.revalidate();
        entryListPanel.repaint();
    }

    private JPanel createEntryRow(String className) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        // Class info
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        infoPanel.setOpaque(false);

        // Simple class name
        String simpleName = className.contains(".")
                ? className.substring(className.lastIndexOf('.') + 1)
                : className;
        JLabel nameLabel = new JLabel(simpleName);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        infoPanel.add(nameLabel);

        // Full class name
        JLabel fullNameLabel = new JLabel(className);
        fullNameLabel.setFont(fullNameLabel.getFont().deriveFont(Font.PLAIN, 11f));
        fullNameLabel.setForeground(Color.GRAY);
        infoPanel.add(fullNameLabel);

        row.add(infoPanel, BorderLayout.CENTER);

        // Execute button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);

        JButton executeButton = new JButton("Execute");
        executeButton.setPreferredSize(new Dimension(90, 26));
        executeButton.addActionListener(e -> executeEntry(className));
        buttonPanel.add(executeButton);

        row.add(buttonPanel, BorderLayout.EAST);

        return row;
    }

    private void executeEntry(String className) {
        EmulatorInstance targetInstance = getSelectedInstance();
        if (targetInstance == null) {
            statusBar.setWarning("No running instance selected");
            ToastNotification.showWarning("Select a running instance first");
            return;
        }

        ClassLoader appClassLoader = targetInstance.getAppClassLoader();
        if (appClassLoader == null) {
            statusBar.setError("Instance #" + targetInstance.getInstanceId() + " has no ClassLoader");
            return;
        }

        statusBar.setInfo("Executing " + className + " on Instance #" + targetInstance.getInstanceId() + "...");

        // Create logger that appends to the log text area
        InjectionLogger logger = new InjectionLogger(logEntry -> {
            SwingUtilities.invokeLater(() -> {
                logTextArea.append(logEntry.toString() + "\n");
                // Auto-scroll to bottom
                logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
            });
        });

        // Run in background thread to avoid blocking UI
        new Thread(() -> {
            logger.info("Executing " + className + " on Instance #" + targetInstance.getInstanceId());
            try {
                injectionService.execute(className, appClassLoader, logger);
                logger.success("Execution completed");
                SwingUtilities.invokeLater(() -> statusBar.setSuccess("Executed: " + className));
            } catch (Exception e) {
                logger.error("Execution failed", e);
                SwingUtilities.invokeLater(() -> statusBar.setError("Execution failed: " + e.getMessage()));
            }
        }, "injection-" + className).start();
    }

    private EmulatorInstance getSelectedInstance() {
        String selected = (String) instanceComboBox.getSelectedItem();
        if (selected == null || !selected.startsWith("Instance #")) {
            return null;
        }

        try {
            int instanceId = Integer.parseInt(selected.replace("Instance #", ""));
            if (mainApplication.emulatorInstanceManager != null) {
                for (EmulatorInstance instance : mainApplication.emulatorInstanceManager.getRunningInstances()) {
                    if (instance.getInstanceId() == instanceId) {
                        return instance;
                    }
                }
            }
        } catch (NumberFormatException ignored) {
        }

        return null;
    }

    /**
     * Refresh the instance combo box with running instances.
     */
    public void refreshInstanceList() {
        instanceComboBox.removeAllItems();

        if (mainApplication.emulatorInstanceManager != null) {
            List<EmulatorInstance> running = mainApplication.emulatorInstanceManager.getRunningInstances();
            for (EmulatorInstance instance : running) {
                instanceComboBox.addItem("Instance #" + instance.getInstanceId());
            }

            if (running.isEmpty()) {
                statusBar.setInfo("No running instances");
            } else {
                statusBar.setInfo(running.size() + " running instance(s)");
            }
        }
    }
}
