package me.kitakeyos.j2me;

import me.kitakeyos.j2me.config.ApplicationConfig;
import me.kitakeyos.j2me.model.EmulatorInstance;
import me.kitakeyos.j2me.model.EmulatorInstance.InstanceState;
import me.kitakeyos.j2me.model.J2meApplication;
import me.kitakeyos.j2me.service.EmulatorLauncher;
import me.kitakeyos.j2me.service.EmulatorInstanceManager;
import me.kitakeyos.j2me.service.J2meApplicationManager;
import me.kitakeyos.j2me.ui.panel.ApplicationsPanel;
import me.kitakeyos.j2me.ui.builder.ConfigurationPanelBuilder;
import me.kitakeyos.j2me.ui.dialog.ConfirmDialog;
import me.kitakeyos.j2me.ui.dialog.MessageDialog;
import me.kitakeyos.j2me.ui.dialog.SettingsDialog;
import me.kitakeyos.j2me.ui.component.ToastNotification;
import me.kitakeyos.j2me.ui.layout.WrapLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Main J2ME Launcher application
 */
public class MainApplication extends JFrame {

    public static final MainApplication INSTANCE = new MainApplication();

    private JComboBox<J2meApplication> applicationComboBox;
    private JTextField microemulatorPathField;
    private JSpinner instanceCountSpinner;
    private JSpinner displayWidthSpinner;
    private JSpinner displayHeightSpinner;
    private JCheckBox syncInputCheckBox;
    private JPanel runningInstancesPanel;
    private final ApplicationConfig applicationConfig;
    private final J2meApplicationManager j2meApplicationManager;
    public EmulatorInstanceManager emulatorInstanceManager;

    public MainApplication() {
        setTitle("J2ME MicroEmulator Launcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        // Initialize managers
        applicationConfig = new ApplicationConfig();
        j2meApplicationManager = new J2meApplicationManager();

        // Add window state listener to detect full screen changes
        addWindowStateListener(e -> {
            // Revalidate layout when window state changes (e.g., maximized/restored)
            // WrapLayout will automatically rewrap instances to fill available width
            SwingUtilities.invokeLater(() -> {
                if (runningInstancesPanel != null) {
                    // Invalidate to force layout recalculation
                    runningInstancesPanel.invalidate();
                    runningInstancesPanel.revalidate();
                    runningInstancesPanel.repaint();
                }
            });
        });

        initializeComponents();
        loadApplicationConfiguration();
    }

    private void initializeComponents() {
        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: Applications
        ApplicationsPanel applicationsPanel = new ApplicationsPanel(j2meApplicationManager);
        tabbedPane.addTab("Applications", applicationsPanel);

        // Tab 2: Instances (merged from Instances and Running Instances)
        JPanel instancesPanel = createInstancesPanel();
        tabbedPane.addTab("Instances", instancesPanel);

        add(tabbedPane);

        // Listen for application changes to update combo box
        j2meApplicationManager.addApplicationChangeListener(new J2meApplicationManager.ApplicationChangeListener() {
            @Override
            public void onApplicationAdded(J2meApplication app) {
                refreshApplicationComboBox();
            }

            @Override
            public void onApplicationRemoved(J2meApplication app) {
                refreshApplicationComboBox();
            }
        });
    }


    private JPanel createInstancesPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top panel: Configuration + Action buttons
        JPanel topPanel = new JPanel(new BorderLayout(0, 10));

        // Configuration panel
        applicationComboBox = new JComboBox<>();
        applicationComboBox.setToolTipText("Select J2ME application to create instances");
        refreshApplicationComboBox();

        microemulatorPathField = new JTextField();
        microemulatorPathField.setEditable(false);
        microemulatorPathField.setBackground(new Color(240, 240, 240));
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
        topPanel.add(configurationPanel, BorderLayout.NORTH);

        // Action buttons
        JPanel buttonPanel = createActionButtonsPanel();
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Sync options panel
        JPanel syncPanel = createSyncOptionsPanel();
        topPanel.add(syncPanel, BorderLayout.CENTER);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Running instances panel in center (using WrapLayout)
        runningInstancesPanel = new JPanel();
        runningInstancesPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 10, 10));
        runningInstancesPanel.setBackground(new Color(240, 240, 240));
        emulatorInstanceManager = new EmulatorInstanceManager(runningInstancesPanel);

        JScrollPane scrollPane = new JScrollPane(runningInstancesPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Running Instances"));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Add component listener to handle window resize
        mainPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                runningInstancesPanel.invalidate();
                runningInstancesPanel.revalidate();
                runningInstancesPanel.repaint();
            }
        });

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        return mainPanel;
    }

    private void refreshApplicationComboBox() {
        applicationComboBox.removeAllItems();
        for (J2meApplication app : j2meApplicationManager.getApplications()) {
            applicationComboBox.addItem(app);
        }
    }

    private JPanel createActionButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton createButton = createStyledButton("Create & Run", new Color(34, 139, 34), this::createEmulatorInstances);
        createButton.setToolTipText("Create and automatically start instances");

        JButton stopAllButton = createStyledButton("Stop All", new Color(220, 20, 60), this::stopAllInstances);
        stopAllButton.setToolTipText("Stop all running instances");

        panel.add(createButton);
        panel.add(stopAllButton);

        return panel;
    }

    private JPanel createSyncOptionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Synchronization Options"));

        syncInputCheckBox = new JCheckBox("Sync Mouse & Keyboard Input");
        syncInputCheckBox.setToolTipText("Synchronize mouse clicks and keyboard input across all running instances");
        syncInputCheckBox.addActionListener(e -> {
            boolean enabled = syncInputCheckBox.isSelected();
            if (emulatorInstanceManager != null) {
                emulatorInstanceManager.setInputSynchronizationEnabled(enabled);
                String message = enabled ? "Input synchronization enabled" : "Input synchronization disabled";
                showToast(message, ToastNotification.ToastType.INFO);
            }
        });

        panel.add(syncInputCheckBox);

        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(140, 40));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void loadApplicationConfiguration() {
        microemulatorPathField.setText(applicationConfig.getMicroemulatorPath());

        // Pre-warm classloader in background if microemulator path is valid
        if (applicationConfig.isMicroemulatorPathValid()) {
            EmulatorLauncher.prewarmClassLoader(applicationConfig.getMicroemulatorPath());
        }
    }

    private void openSettingsDialog() {
        SettingsDialog settingsDialog = new SettingsDialog(this, applicationConfig);
        settingsDialog.setVisible(true);

        if (settingsDialog.isSettingsModified()) {
            loadApplicationConfiguration();
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

        for (int i = 0; i < numberOfInstances; i++) {
            int instanceId = emulatorInstanceManager.getNextInstanceId();
            EmulatorInstance emulatorInstance = new EmulatorInstance(instanceId, microemulatorPath, j2meFilePath, displayWidth, displayHeight);

            emulatorInstanceManager.addInstance(emulatorInstance);
            // Automatically start the instance
            runSingleInstance(emulatorInstance);
        }

        showToast("Starting " + numberOfInstances + " instance(s) for '" + selectedApp.getName() + "'", ToastNotification.ToastType.SUCCESS);
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
            return;
        }

        // Note: removeEmulatorInstanceTab already calls notifyInstanceStopping
        for (EmulatorInstance instance : runningInstances) {
            removeEmulatorInstanceTab(instance);
            instance.shutdown();
        }

        showToast("Stopped " + runningInstances.size() + " instance(s)", ToastNotification.ToastType.INFO);
    }


    /**
     * Add emulator display to running instances panel
     * Instances are automatically sorted by instanceId
     * WrapLayout automatically wraps instances to fill horizontal space
     */
    public void addEmulatorInstanceTab(EmulatorInstance emulatorInstance) {
        if (emulatorInstance.getEmulatorDisplay() != null) {
            // Create wrapper panel with BorderLayout
            JPanel wrapperPanel = new JPanel(new BorderLayout());

            // Create header panel with title and stop button
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            headerPanel.setBackground(new Color(230, 230, 250));

            // Title label
            JLabel titleLabel = new JLabel("Instance #" + emulatorInstance.getInstanceId());
            titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
            headerPanel.add(titleLabel, BorderLayout.WEST);

            // Stop button
            JButton stopButton = new JButton("Stop");
            stopButton.setFont(new Font("Arial", Font.BOLD, 11));
            stopButton.setPreferredSize(new Dimension(70, 25));
            stopButton.setBackground(new Color(220, 20, 60));
            stopButton.setForeground(Color.WHITE);
            stopButton.setFocusPainted(false);
            stopButton.setBorderPainted(false);
            stopButton.setOpaque(true);
            stopButton.addActionListener(e -> {
                removeEmulatorInstanceTab(emulatorInstance);
                emulatorInstance.shutdown();
                showToast("Stopped Instance #" + emulatorInstance.getInstanceId(), ToastNotification.ToastType.INFO);
            });
            headerPanel.add(stopButton, BorderLayout.EAST);

            // Add header and display to wrapper
            wrapperPanel.add(headerPanel, BorderLayout.NORTH);
            wrapperPanel.add(emulatorInstance.getEmulatorDisplay(), BorderLayout.CENTER);
            wrapperPanel.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 1));

            // Store wrapper panel reference for later removal
            emulatorInstance.getEmulatorDisplay().putClientProperty("wrapperPanel", wrapperPanel);
            // Store instanceId for sorting
            wrapperPanel.putClientProperty("instanceId", emulatorInstance.getInstanceId());

            // Find the correct position to insert based on instanceId
            int insertIndex = findInsertPosition(emulatorInstance.getInstanceId());
            runningInstancesPanel.add(wrapperPanel, insertIndex);

            // Invalidate and revalidate to trigger layout recalculation
            runningInstancesPanel.invalidate();
            runningInstancesPanel.revalidate();
            runningInstancesPanel.repaint();

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
                // Invalidate and revalidate to trigger layout recalculation
                runningInstancesPanel.invalidate();
                runningInstancesPanel.revalidate();
                runningInstancesPanel.repaint();
            }
        }
    }

    private void clearAllEmulatorInstances() {
        int count = emulatorInstanceManager.getInstanceCount();
        if (count == 0) {
            showInfoMessage("No instances to clear.");
            return;
        }

        boolean confirm = ConfirmDialog.showConfirm(
                this,
                "Confirm Clear All",
                "Are you sure you want to clear all " + count + " instance(s)?"
        );

        if (confirm) {
            emulatorInstanceManager.clearAllInstances();
            emulatorInstanceManager.resetInstanceIdCounter();
            // Clear running instances panel
            runningInstancesPanel.removeAll();
            runningInstancesPanel.revalidate();
            runningInstancesPanel.repaint();
            showToast("Cleared all instances", ToastNotification.ToastType.SUCCESS);
        }
    }

    private void showErrorMessage(String message) {
        MessageDialog.showError(this, "Error", message);
    }

    private void showInfoMessage(String message) {
        MessageDialog.showInfo(this, "Info", message);
    }

    private void showSuccessMessage(String message) {
        MessageDialog.showSuccess(this, "Success", message);
    }

    private void showWarningMessage(String message) {
        MessageDialog.showWarning(this, "Warning", message);
    }

    /**
     * Show toast notification for non-critical messages
     */
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            INSTANCE.setVisible(true);
        });
    }
}