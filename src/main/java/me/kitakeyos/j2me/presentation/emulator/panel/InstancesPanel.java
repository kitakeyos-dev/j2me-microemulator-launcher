package me.kitakeyos.j2me.presentation.emulator.panel;

import me.kitakeyos.j2me.application.MainApplication;
import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.domain.application.model.J2meApplication;
import me.kitakeyos.j2me.domain.application.service.ApplicationService;
import me.kitakeyos.j2me.application.emulator.EmulatorLauncher;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance.InstanceState;
import me.kitakeyos.j2me.domain.emulator.service.InstanceManager;
import me.kitakeyos.j2me.infrastructure.input.InputSynchronizerImpl;
import me.kitakeyos.j2me.presentation.common.builder.ConfigurationPanelBuilder;
import me.kitakeyos.j2me.presentation.common.component.BaseTabPanel;
import me.kitakeyos.j2me.presentation.common.component.ScrollablePanel;
import me.kitakeyos.j2me.presentation.common.component.ToastNotification;
import me.kitakeyos.j2me.presentation.common.dialog.MessageDialog;
import me.kitakeyos.j2me.presentation.monitor.SystemMonitorDialog;
import me.kitakeyos.j2me.presentation.network.NetworkMonitorDialog;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Instances tab panel for managing emulator instances.
 * Allows users to create, configure, and manage multiple emulator instances.
 */
public class InstancesPanel extends BaseTabPanel {

    private static final Logger logger = Logger.getLogger(InstancesPanel.class.getName());

    // UI Components
    private JComboBox<J2meApplication> applicationComboBox;
    private JTextField microemulatorPathField;
    private JSpinner instanceCountSpinner;
    private JSpinner displayWidthSpinner;
    private JSpinner displayHeightSpinner;
    private JCheckBox syncInputCheckBox;
    private JCheckBox scaleInputBySizeCheckBox;
    private JCheckBox fullDisplayModeCheckBox;
    private JComboBox<String> defaultSpeedComboBox;
    private ScrollablePanel runningInstancesPanel;
    private JLabel instancesEmptyLabel;

    // Speed options
    private static final String[] SPEED_OPTIONS = { "0.5x", "1x", "2x", "3x", "5x", "10x", "20x" };
    private static final double[] SPEED_VALUES = { 0.5, 1.0, 2.0, 3.0, 5.0, 10.0, 20.0 };

    // Services and managers
    public InstanceManager emulatorInstanceManager;

    // Thread pool for launching emulator instances
    private final ExecutorService instanceLauncherPool = Executors.newCachedThreadPool();

    public InstancesPanel(MainApplication mainApplication, ApplicationConfig applicationConfig,
            ApplicationService j2meApplicationManager) {
        super(mainApplication, applicationConfig, j2meApplicationManager);
    }

    @Override
    protected JComponent createHeader() {
        // Top panel: Horizontal layout with three sections
        // WEST: Configuration (compact, no stretch)
        // CENTER: Options panel
        // EAST: Action buttons
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));

        // Main Configuration (left side - compact)
        applicationComboBox = new JComboBox<>();
        applicationComboBox.setToolTipText("Select J2ME application to create instances");
        refreshApplicationComboBox();

        microemulatorPathField = new JTextField();
        microemulatorPathField.setEditable(false);
        microemulatorPathField.setToolTipText("Path to MicroEmulator JAR (configure in Settings)");
        instanceCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        instanceCountSpinner.setToolTipText("Number of instances to create (1-100)");

        // Display size spinners (default 240x320)
        displayWidthSpinner = new JSpinner(new SpinnerNumberModel(240, 128, 800, 1));
        displayWidthSpinner.setToolTipText("Display width in pixels (128-800)");
        displayWidthSpinner.setPreferredSize(new Dimension(80, displayWidthSpinner.getPreferredSize().height));

        displayHeightSpinner = new JSpinner(new SpinnerNumberModel(320, 128, 1000, 1));
        displayHeightSpinner.setToolTipText("Display height in pixels (128-1000)");
        displayHeightSpinner.setPreferredSize(new Dimension(80, displayHeightSpinner.getPreferredSize().height));

        JPanel configurationPanel = ConfigurationPanelBuilder.createConfigurationPanel(
                applicationComboBox, instanceCountSpinner, microemulatorPathField,
                displayWidthSpinner, displayHeightSpinner,
                this::browseMicroemulatorJar);
        topPanel.add(configurationPanel, BorderLayout.WEST);

        // Options panel in center
        JPanel optionsPanel = createOptionsPanel();
        topPanel.add(optionsPanel, BorderLayout.CENTER);

        // Action buttons on right
        JPanel buttonPanel = createActionButtonsPanel();
        topPanel.add(buttonPanel, BorderLayout.EAST);

        return topPanel;
    }

    @Override
    protected JComponent createContent() {
        // Running instances panel in center (using WrapLayout for auto-wrapping)
        // Use ScrollablePanel to ensure it tracks viewport width
        runningInstancesPanel = new ScrollablePanel(
                new me.kitakeyos.j2me.presentation.common.layout.WrapLayout(FlowLayout.LEFT, 10, 10));
        emulatorInstanceManager = new InstanceManager(runningInstancesPanel);

        // Create empty state label
        instancesEmptyLabel = new JLabel("No instances running. Click 'Create & Run' to start instances.");
        instancesEmptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        instancesEmptyLabel.setForeground(Color.GRAY);
        instancesEmptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        runningInstancesPanel.add(instancesEmptyLabel);

        JScrollPane scrollPane = new JScrollPane(runningInstancesPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                "Running Instances",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        return scrollPane;
    }

    @Override
    protected void onInitialized() {
        // Wire InputSynchronizer implementation
        emulatorInstanceManager.setInputSynchronizer(new InputSynchronizerImpl(emulatorInstanceManager));

        // Load configuration
        loadApplicationConfiguration();
        updateInstancesEmptyState();
    }

    private JPanel createActionButtonsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton createButton = new JButton("Create & Run");
        createButton.setToolTipText("Create and automatically start instances");
        createButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createButton.setPreferredSize(new Dimension(130, 30));
        createButton.setMaximumSize(new Dimension(130, 30));
        createButton.addActionListener(e -> createEmulatorInstances());

        JButton stopAllButton = new JButton("Stop All");
        stopAllButton.setToolTipText("Stop all running instances");
        stopAllButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        stopAllButton.setPreferredSize(new Dimension(130, 30));
        stopAllButton.setMaximumSize(new Dimension(130, 30));
        stopAllButton.addActionListener(e -> stopAllInstances());

        JButton networkMonitorButton = new JButton("Network Monitor");
        networkMonitorButton.setToolTipText("Open network monitor to view connections, redirection and proxy rules");
        networkMonitorButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        networkMonitorButton.setPreferredSize(new Dimension(130, 30));
        networkMonitorButton.setMaximumSize(new Dimension(130, 30));
        networkMonitorButton.addActionListener(e -> openNetworkMonitor());

        JButton systemMonitorButton = new JButton("System Monitor");
        systemMonitorButton.setToolTipText("Open system monitor to view RAM, CPU usage and Threads");
        systemMonitorButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        systemMonitorButton.setPreferredSize(new Dimension(130, 30));
        systemMonitorButton.setMaximumSize(new Dimension(130, 30));
        systemMonitorButton.addActionListener(e -> openSystemMonitor());

        panel.add(createButton);
        panel.add(Box.createVerticalStrut(8));
        panel.add(stopAllButton);
        panel.add(Box.createVerticalStrut(8));
        panel.add(networkMonitorButton);
        panel.add(Box.createVerticalStrut(8));
        panel.add(systemMonitorButton);

        return panel;
    }

    /**
     * Create options panel
     * Contains various runtime options like input synchronization
     */
    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Options"),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        // Input synchronization option
        syncInputCheckBox = new JCheckBox("Sync Mouse & Keyboard Input");
        syncInputCheckBox.setToolTipText("Synchronize mouse clicks and keyboard input across all running instances");
        syncInputCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        syncInputCheckBox.addActionListener(e -> {
            boolean enabled = syncInputCheckBox.isSelected();
            if (emulatorInstanceManager != null) {
                emulatorInstanceManager.setInputSynchronizationEnabled(enabled);
                String message = enabled ? "Input synchronization enabled" : "Input synchronization disabled";
                showToast(message, ToastNotification.ToastType.INFO);
                statusBar.setInfo(message);
            }
        });

        // Scale input by size option
        scaleInputBySizeCheckBox = new JCheckBox("Scale Input by Size");
        scaleInputBySizeCheckBox.setToolTipText(
                "Scale mouse coordinates based on device panel size (useful when instances have different sizes)");
        scaleInputBySizeCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        scaleInputBySizeCheckBox.addActionListener(e -> {
            boolean enabled = scaleInputBySizeCheckBox.isSelected();
            if (emulatorInstanceManager != null) {
                emulatorInstanceManager.setInputScaleBySize(enabled);
                String message = enabled ? "Input scaling by size enabled" : "Input scaling by size disabled";
                showToast(message, ToastNotification.ToastType.INFO);
                statusBar.setInfo(message);
            }
        });

        // Full display mode option
        fullDisplayModeCheckBox = new JCheckBox("Full Display Mode");
        fullDisplayModeCheckBox.setToolTipText(
                "Show emulator with full interface (menubar, toolbar) instead of simple device panel only");
        fullDisplayModeCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Default speed option
        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        speedPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel speedLabel = new JLabel("Default Speed: ");
        defaultSpeedComboBox = new JComboBox<>(SPEED_OPTIONS);
        defaultSpeedComboBox.setSelectedIndex(1); // Default 1x
        defaultSpeedComboBox.setToolTipText("Default speed for new instances");
        speedPanel.add(speedLabel);
        speedPanel.add(defaultSpeedComboBox);

        panel.add(syncInputCheckBox);
        panel.add(Box.createVerticalStrut(5));
        panel.add(scaleInputBySizeCheckBox);
        panel.add(Box.createVerticalStrut(5));
        panel.add(fullDisplayModeCheckBox);
        panel.add(Box.createVerticalStrut(5));
        panel.add(speedPanel);

        return panel;
    }

    private void loadApplicationConfiguration() {
        microemulatorPathField.setText(applicationConfig.getMicroemulatorPath());
    }

    /**
     * Browse for MicroEmulator JAR file
     */
    private void browseMicroemulatorJar() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select MicroEmulator JAR File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // Set file filter for JAR files
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(java.io.File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".jar");
            }

            @Override
            public String getDescription() {
                return "JAR Files (*.jar)";
            }
        });

        // Set current directory based on existing path if valid
        String currentPath = applicationConfig.getMicroemulatorPath();
        if (currentPath != null && !currentPath.isEmpty()) {
            java.io.File currentFile = new java.io.File(currentPath);
            if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
                fileChooser.setCurrentDirectory(currentFile.getParentFile());
            }
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            String selectedPath = selectedFile.getAbsolutePath();

            // Validate that it's a JAR file
            if (!selectedPath.toLowerCase().endsWith(".jar")) {
                showErrorMessage("Please select a valid JAR file.");
                return;
            }

            // Save to configuration
            applicationConfig.setMicroemulatorPath(selectedPath);
            applicationConfig.saveConfiguration();

            // Update UI
            microemulatorPathField.setText(selectedPath);

            // Show success message
            showToast("MicroEmulator path updated successfully", ToastNotification.ToastType.SUCCESS);
            statusBar.setSuccess("MicroEmulator path saved: " + selectedFile.getName());
        }
    }

    /**
     * Refresh the application combo box with latest applications
     */
    public void refreshApplicationComboBox() {
        applicationComboBox.removeAllItems();
        for (J2meApplication app : applicationManager.getApplications()) {
            applicationComboBox.addItem(app);
        }
    }

    /**
     * Create and automatically start instances
     */
    private void createEmulatorInstances() {
        J2meApplication selectedApp = (J2meApplication) applicationComboBox.getSelectedItem();
        if (selectedApp == null) {
            showErrorMessage("Please install and select a J2ME application from the Applications tab");
            return;
        }

        if (!applicationConfig.isMicroemulatorPathValid()) {
            showErrorMessage("MicroEmulator path is invalid. Please check settings.");
            return;
        }

        int numberOfInstances = (Integer) instanceCountSpinner.getValue();
        String microemulatorPath = applicationConfig.getMicroemulatorPath();
        String j2meFilePath = selectedApp.getFilePath();
        int displayWidth = (Integer) displayWidthSpinner.getValue();
        int displayHeight = (Integer) displayHeightSpinner.getValue();
        boolean fullDisplayMode = fullDisplayModeCheckBox.isSelected();

        for (int i = 0; i < numberOfInstances; i++) {
            int instanceId = emulatorInstanceManager.getNextInstanceId();
            EmulatorInstance emulatorInstance = new EmulatorInstance(instanceId, microemulatorPath, j2meFilePath,
                    displayWidth, displayHeight, fullDisplayMode);

            emulatorInstanceManager.addInstance(emulatorInstance);
            // Automatically start the instance
            runSingleInstance(emulatorInstance);
        }

        String message = "Starting " + numberOfInstances + " instance(s) for '" + selectedApp.getName() + "'";
        showToast(message, ToastNotification.ToastType.SUCCESS);
        statusBar.setSuccess(message);
    }

    /**
     * Run a single instance
     */
    public void runSingleInstance(EmulatorInstance emulatorInstance) {
        if (!emulatorInstance.canRun()) {
            showErrorMessage("Instance #" + emulatorInstance.getInstanceId() + " cannot be run in current state.");
            return;
        }

        // Get default speed from UI (capture before async call)
        final double defaultSpeed = getDefaultSpeed();

        instanceLauncherPool.submit(() -> {
            try {
                EmulatorLauncher.startEmulatorInstance(
                        emulatorInstance,
                        // onComplete callback
                        () -> SwingUtilities.invokeLater(() -> {
                            if (emulatorInstance.getState() == InstanceState.RUNNING) {
                                // Apply default speed
                                emulatorInstance.setSpeedMultiplier(defaultSpeed);
                                addEmulatorInstanceTab(emulatorInstance, defaultSpeed);
                                mainApplication.luaScriptManager.refreshInstanceList();
                            }
                        }));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> showErrorMessage(e.getMessage()));
                logger.severe(e.getMessage());
            }
        });
    }

    /**
     * Get default speed from UI selection
     */
    private double getDefaultSpeed() {
        int index = defaultSpeedComboBox.getSelectedIndex();
        if (index >= 0 && index < SPEED_VALUES.length) {
            return SPEED_VALUES[index];
        }
        return 1.0; // Default 1x
    }

    /**
     * Stop all running instances
     */
    private void stopAllInstances() {
        java.util.List<EmulatorInstance> runningInstances = emulatorInstanceManager.getRunningInstances();

        if (runningInstances.isEmpty()) {
            showInfoMessage("No running instances to stop.");
            statusBar.setInfo("No running instances to stop");
            return;
        }

        // Note: removeEmulatorInstanceTab already calls notifyInstanceStopping
        for (EmulatorInstance instance : runningInstances) {
            removeEmulatorInstanceTab(instance);
            instance.shutdown();
        }

        String message = "Stopped " + runningInstances.size() + " instance(s)";
        showToast(message, ToastNotification.ToastType.INFO);
        statusBar.setInfo(message);
    }

    /**
     * Update empty state visibility based on number of running instances
     */
    private void updateInstancesEmptyState() {
        boolean hasInstances = runningInstancesPanel.getComponentCount() > 1; // More than just the empty label
        instancesEmptyLabel.setVisible(!hasInstances);
        if (hasInstances) {
            int count = runningInstancesPanel.getComponentCount() - 1; // Exclude empty label
            statusBar.setInfo(count + " instance(s) running");
        } else {
            statusBar.setInfo("No instances running");
        }
    }

    /**
     * Add emulator display to running instances panel
     * Instances are arranged using SimpleFlowLayout with auto-wrapping
     * Instances are sorted by instanceId in ascending order
     * 
     * @param defaultSpeed Default speed multiplier to show in menu
     */
    public void addEmulatorInstanceTab(EmulatorInstance emulatorInstance, double defaultSpeed) {
        if (emulatorInstance.getEmulatorDisplay() != null) {
            // Create wrapper panel with BorderLayout
            JPanel wrapperPanel = new JPanel(new BorderLayout());

            // Create menu bar for instance controls
            JMenuBar menuBar = createInstanceMenuBar(emulatorInstance, defaultSpeed);

            // Add menu bar and display to wrapper
            wrapperPanel.add(menuBar, BorderLayout.NORTH);
            wrapperPanel.add(emulatorInstance.getEmulatorDisplay(), BorderLayout.CENTER);
            wrapperPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

            // Store wrapper panel reference and instanceId for later removal and sorting
            emulatorInstance.getEmulatorDisplay().putClientProperty("wrapperPanel", wrapperPanel);
            wrapperPanel.putClientProperty("instanceId", emulatorInstance.getInstanceId());

            // Find correct position to insert based on instanceId (sorted in ascending
            // order)
            int insertIndex = findInsertPosition(emulatorInstance.getInstanceId());
            runningInstancesPanel.add(wrapperPanel, insertIndex);

            // Revalidate to trigger layout recalculation
            runningInstancesPanel.revalidate();
            runningInstancesPanel.repaint();

            // Update empty state visibility
            updateInstancesEmptyState();

            // Notify instance manager that instance has been started (for input sync)
            emulatorInstanceManager.notifyInstanceStarted(emulatorInstance);
        }
    }

    /**
     * Create menu bar for instance controls
     * 
     * @param defaultSpeed Default speed to show as selected
     */
    private JMenuBar createInstanceMenuBar(EmulatorInstance emulatorInstance, double defaultSpeed) {
        JMenuBar menuBar = new JMenuBar();

        // Instance title/info as a non-clickable label
        JLabel titleLabel = new JLabel("  Instance #" + emulatorInstance.getInstanceId() + "  ");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        menuBar.add(titleLabel);

        // Push Actions menu to the right
        menuBar.add(Box.createHorizontalGlue());

        // Actions menu (contains all instance controls)
        JMenu actionsMenu = new JMenu("Actions");
        actionsMenu.setToolTipText("Instance actions");

        // Speed submenu - find initial text based on defaultSpeed
        String initialSpeedText = "Speed";
        for (int i = 0; i < SPEED_VALUES.length; i++) {
            if (Math.abs(SPEED_VALUES[i] - defaultSpeed) < 0.01) {
                initialSpeedText = "Speed (" + SPEED_OPTIONS[i] + ")";
                break;
            }
        }
        JMenu speedSubmenu = new JMenu(initialSpeedText);
        speedSubmenu.setToolTipText("Set emulator speed");

        ButtonGroup speedGroup = new ButtonGroup();
        for (int i = 0; i < SPEED_OPTIONS.length; i++) {
            final int index = i;
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(SPEED_OPTIONS[i]);
            // Select based on defaultSpeed
            if (Math.abs(SPEED_VALUES[i] - defaultSpeed) < 0.01) {
                item.setSelected(true);
            }

            item.addActionListener(e -> {
                double speed = SPEED_VALUES[index];
                emulatorInstance.setSpeedMultiplier(speed);
                speedSubmenu.setText("Speed (" + SPEED_OPTIONS[index] + ")");
                showToast("Instance #" + emulatorInstance.getInstanceId() + " speed: " + SPEED_OPTIONS[index],
                        ToastNotification.ToastType.INFO);
            });

            speedGroup.add(item);
            speedSubmenu.add(item);
        }
        actionsMenu.add(speedSubmenu);

        actionsMenu.addSeparator();

        // Stop instance
        JMenuItem stopItem = new JMenuItem("Stop Instance");
        stopItem.addActionListener(e -> {
            removeEmulatorInstanceTab(emulatorInstance);
            emulatorInstance.shutdown();
            showToast("Stopped Instance #" + emulatorInstance.getInstanceId(), ToastNotification.ToastType.INFO);
        });
        actionsMenu.add(stopItem);

        actionsMenu.addSeparator();

        // Placeholder for future features
        JMenuItem screenshotItem = new JMenuItem("Take Screenshot");
        screenshotItem.setEnabled(false); // TODO: Implement
        actionsMenu.add(screenshotItem);

        menuBar.add(actionsMenu);

        return menuBar;
    }

    /**
     * Find the correct position to insert an instance based on instanceId
     * Instances are sorted in ascending order by instanceId
     */
    private int findInsertPosition(int instanceId) {
        Component[] components = runningInstancesPanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof JPanel) {
                JPanel panel = (JPanel) components[i];
                Integer existingId = (Integer) panel.getClientProperty("instanceId");
                if (existingId != null && existingId > instanceId) {
                    return i; // Insert before this component
                }
            }
        }
        return components.length; // Insert at the end if not found
    }

    /**
     * Remove emulator display from running instances panel
     */
    public void removeEmulatorInstanceTab(EmulatorInstance emulatorInstance) {
        if (emulatorInstance.getEmulatorDisplay() != null) {
            // Notify instance manager that instance is stopping (for input sync)
            emulatorInstanceManager.notifyInstanceStopping(emulatorInstance);

            // Get wrapper panel and remove it
            JPanel wrapperPanel = (JPanel) emulatorInstance.getEmulatorDisplay().getClientProperty("wrapperPanel");
            if (wrapperPanel != null) {
                runningInstancesPanel.remove(wrapperPanel);
                // Revalidate to trigger layout recalculation
                runningInstancesPanel.revalidate();
                runningInstancesPanel.repaint();
                mainApplication.luaScriptManager.refreshInstanceList();
                // Update empty state visibility
                updateInstancesEmptyState();
            }
        }
    }

    // Utility methods for dialogs and toast notifications
    private void showErrorMessage(String message) {
        MessageDialog.showError(this, "Error", message);
    }

    private void showInfoMessage(String message) {
        MessageDialog.showInfo(this, "Info", message);
    }

    private void showToast(String message, ToastNotification.ToastType type) {
        switch (type) {
            case SUCCESS:
                ToastNotification.showSuccess(message);
                break;
            case ERROR:
                ToastNotification.showError(message);
                break;
            case WARNING:
                ToastNotification.showWarning(message);
                break;
            case INFO:
            default:
                ToastNotification.showInfo(message);
                break;
        }
    }

    /**
     * Open the Network Monitor dialog
     */
    private void openNetworkMonitor() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        NetworkMonitorDialog dialog = new NetworkMonitorDialog(owner);
        dialog.setVisible(true);
    }

    /**
     * Open the System Monitor dialog
     */
    private void openSystemMonitor() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        SystemMonitorDialog dialog = new SystemMonitorDialog(owner);
        dialog.setVisible(true);
    }
}
