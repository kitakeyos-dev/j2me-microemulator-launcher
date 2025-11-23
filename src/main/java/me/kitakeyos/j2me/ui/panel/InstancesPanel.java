package me.kitakeyos.j2me.ui.panel;

import me.kitakeyos.j2me.config.ApplicationConfig;
import me.kitakeyos.j2me.model.EmulatorInstance;
import me.kitakeyos.j2me.model.EmulatorInstance.InstanceState;
import me.kitakeyos.j2me.model.J2meApplication;
import me.kitakeyos.j2me.service.EmulatorInstanceManager;
import me.kitakeyos.j2me.service.EmulatorLauncher;
import me.kitakeyos.j2me.service.J2meApplicationManager;
import me.kitakeyos.j2me.ui.builder.ConfigurationPanelBuilder;
import me.kitakeyos.j2me.ui.component.StatusBar;
import me.kitakeyos.j2me.ui.component.ToastNotification;
import me.kitakeyos.j2me.ui.dialog.MessageDialog;
import me.kitakeyos.j2me.ui.layout.SimpleFlowLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Instances tab panel for managing emulator instances.
 * Allows users to create, configure, and manage multiple emulator instances.
 */
public class InstancesPanel extends BaseTabPanel {

    // UI Components
    private JComboBox<J2meApplication> applicationComboBox;
    private JTextField microemulatorPathField;
    private JSpinner instanceCountSpinner;
    private JSpinner displayWidthSpinner;
    private JSpinner displayHeightSpinner;
    private JCheckBox syncInputCheckBox;
    private JCheckBox fullDisplayModeCheckBox;
    private JPanel runningInstancesPanel;
    private JLabel instancesEmptyLabel;
    private StatusBar statusBar;

    // Services and managers
    public EmulatorInstanceManager emulatorInstanceManager;

    // Callback for opening settings dialog
    private Runnable settingsDialogOpener;

    public InstancesPanel(ApplicationConfig applicationConfig, J2meApplicationManager j2meApplicationManager) {
        super(applicationConfig, j2meApplicationManager);
    }

    /**
     * Set the callback for opening settings dialog
     */
    public void setSettingsDialogOpener(Runnable opener) {
        this.settingsDialogOpener = opener;
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
                this::openSettingsDialog);
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
        // Running instances panel in center (using SimpleFlowLayout for auto-wrapping)
        runningInstancesPanel = new JPanel();
        runningInstancesPanel.setLayout(new SimpleFlowLayout(FlowLayout.LEFT, 10, 10));
        emulatorInstanceManager = new EmulatorInstanceManager(runningInstancesPanel);

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
            javax.swing.border.TitledBorder.TOP
        ));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Add component listener to handle window resize
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                runningInstancesPanel.revalidate();
            }
        });

        return scrollPane;
    }

    @Override
    protected JComponent createStatusBar() {
        statusBar = new StatusBar();
        statusBar.setInfoStatus("Ready");
        return statusBar;
    }

    @Override
    protected void onInitialized() {
        // Load configuration
        loadApplicationConfiguration();
    }

    private JPanel createActionButtonsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton createButton = new JButton("Create & Run");
        createButton.setToolTipText("Create and automatically start instances");
        createButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createButton.setPreferredSize(new Dimension(130, 35));
        createButton.setMaximumSize(new Dimension(130, 35));
        createButton.addActionListener(e -> createEmulatorInstances());

        JButton stopAllButton = new JButton("Stop All");
        stopAllButton.setToolTipText("Stop all running instances");
        stopAllButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        stopAllButton.setPreferredSize(new Dimension(130, 35));
        stopAllButton.setMaximumSize(new Dimension(130, 35));
        stopAllButton.addActionListener(e -> stopAllInstances());

        panel.add(createButton);
        panel.add(Box.createVerticalStrut(8));
        panel.add(stopAllButton);

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
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

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
                statusBar.setInfoStatus(message);
            }
        });

        // Full display mode option
        fullDisplayModeCheckBox = new JCheckBox("Full Display Mode");
        fullDisplayModeCheckBox.setToolTipText("Show emulator with full interface (menubar, toolbar) instead of simple device panel only");
        fullDisplayModeCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(syncInputCheckBox);
        panel.add(Box.createVerticalStrut(5));
        panel.add(fullDisplayModeCheckBox);

        return panel;
    }

    private void loadApplicationConfiguration() {
        microemulatorPathField.setText(applicationConfig.getMicroemulatorPath());

        // Pre-warm classloader in background if microemulator path is valid
        if (applicationConfig.isMicroemulatorPathValid()) {
            EmulatorLauncher.prewarmClassLoader(applicationConfig.getMicroemulatorPath());
        }
    }

    private void openSettingsDialog() {
        if (settingsDialogOpener != null) {
            settingsDialogOpener.run();
            // Reload configuration after settings dialog closes
            loadApplicationConfiguration();
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
            EmulatorInstance emulatorInstance = new EmulatorInstance(instanceId, microemulatorPath, j2meFilePath, displayWidth, displayHeight, fullDisplayMode);

            emulatorInstanceManager.addInstance(emulatorInstance);
            // Automatically start the instance
            runSingleInstance(emulatorInstance);
        }

        String message = "Starting " + numberOfInstances + " instance(s) for '" + selectedApp.getName() + "'";
        showToast(message, ToastNotification.ToastType.SUCCESS);
        statusBar.setSuccessStatus(message);
    }

    /**
     * Run a single instance
     */
    public void runSingleInstance(EmulatorInstance emulatorInstance) {
        if (!emulatorInstance.canRun()) {
            showErrorMessage("Instance #" + emulatorInstance.getInstanceId() + " cannot be run in current state.");
            return;
        }

        new Thread(() -> EmulatorLauncher.startEmulatorInstance(
                emulatorInstance,
                // onComplete callback
                () -> SwingUtilities.invokeLater(() -> {
                    if (emulatorInstance.getState() == InstanceState.RUNNING) {
                        addEmulatorInstanceTab(emulatorInstance);
                    }
                }),
                // onStarted callback
                null
        )).start();
    }

    /**
     * Stop all running instances
     */
    private void stopAllInstances() {
        java.util.List<EmulatorInstance> runningInstances = emulatorInstanceManager.getRunningInstances();

        if (runningInstances.isEmpty()) {
            showInfoMessage("No running instances to stop.");
            statusBar.setInfoStatus("No running instances to stop");
            return;
        }

        // Note: removeEmulatorInstanceTab already calls notifyInstanceStopping
        for (EmulatorInstance instance : runningInstances) {
            removeEmulatorInstanceTab(instance);
            instance.shutdown();
        }

        String message = "Stopped " + runningInstances.size() + " instance(s)";
        showToast(message, ToastNotification.ToastType.INFO);
        statusBar.setInfoStatus(message);
    }

    /**
     * Update empty state visibility based on number of running instances
     */
    private void updateInstancesEmptyState() {
        boolean hasInstances = runningInstancesPanel.getComponentCount() > 1; // More than just the empty label
        instancesEmptyLabel.setVisible(!hasInstances);
        if (hasInstances) {
            int count = runningInstancesPanel.getComponentCount() - 1; // Exclude empty label
            statusBar.setInfoStatus(count + " instance(s) running");
        } else {
            statusBar.setInfoStatus("No instances running");
        }
    }

    /**
     * Add emulator display to running instances panel
     * Instances are arranged using SimpleFlowLayout with auto-wrapping
     * Instances are sorted by instanceId in ascending order
     */
    public void addEmulatorInstanceTab(EmulatorInstance emulatorInstance) {
        if (emulatorInstance.getEmulatorDisplay() != null) {
            // Create wrapper panel with BorderLayout
            JPanel wrapperPanel = new JPanel(new BorderLayout());

            // Create header panel with title and stop button
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            // Title label
            JLabel titleLabel = new JLabel("Instance #" + emulatorInstance.getInstanceId());
            headerPanel.add(titleLabel, BorderLayout.WEST);

            // Stop button
            JButton stopButton = new JButton("Stop");
            stopButton.setPreferredSize(new Dimension(70, 25));
            stopButton.addActionListener(e -> {
                removeEmulatorInstanceTab(emulatorInstance);
                emulatorInstance.shutdown();
                showToast("Stopped Instance #" + emulatorInstance.getInstanceId(), ToastNotification.ToastType.INFO);
            });
            headerPanel.add(stopButton, BorderLayout.EAST);

            // Add header and display to wrapper
            wrapperPanel.add(headerPanel, BorderLayout.NORTH);
            wrapperPanel.add(emulatorInstance.getEmulatorDisplay(), BorderLayout.CENTER);
            wrapperPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

            // Store wrapper panel reference and instanceId for later removal and sorting
            emulatorInstance.getEmulatorDisplay().putClientProperty("wrapperPanel", wrapperPanel);
            wrapperPanel.putClientProperty("instanceId", emulatorInstance.getInstanceId());

            // Find correct position to insert based on instanceId (sorted in ascending order)
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
}
