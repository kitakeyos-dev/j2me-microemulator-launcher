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
import me.kitakeyos.j2me.ui.builder.EmulatorInstanceUIBuilder;
import me.kitakeyos.j2me.ui.dialog.ConfirmDialog;
import me.kitakeyos.j2me.ui.dialog.MessageDialog;
import me.kitakeyos.j2me.ui.dialog.SettingsDialog;
import me.kitakeyos.j2me.ui.component.ToastNotification;

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
    private JPanel emulatorInstancesPanel;
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
            // Recalculate layout when window state changes (e.g., maximized/restored)
            SwingUtilities.invokeLater(this::recalculateGridLayout);
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

        // Tab 2: Instances
        JPanel instancesPanel = createInstancesPanel();
        tabbedPane.addTab("Instances", instancesPanel);

        // Tab 3: Running Instances
        JPanel runningInstancesPanel = createRunningInstancesPanel();
        tabbedPane.addTab("Running Instances", runningInstancesPanel);

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

    private JPanel createRunningInstancesPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Create panel to hold all running instances in a grid
        runningInstancesPanel = new JPanel();
        // GridLayout(rows, cols, hgap, vgap): 0 rows = unlimited rows (auto-expand), columns will be calculated dynamically
        runningInstancesPanel.setLayout(new GridLayout(0, 1, 10, 10)); // Start with 1 column
        runningInstancesPanel.setBackground(new Color(240, 240, 240));

        JScrollPane scrollPane = new JScrollPane(runningInstancesPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Running Instances"));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Add component listener to handle window resize
        mainPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                recalculateGridLayout();
            }
        });

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel createInstancesPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Application combo box
        applicationComboBox = new JComboBox<>();
        applicationComboBox.setToolTipText("Select J2ME application to create instances");
        refreshApplicationComboBox();

        microemulatorPathField = new JTextField();
        microemulatorPathField.setEditable(false);
        microemulatorPathField.setBackground(new Color(240, 240, 240));
        microemulatorPathField.setToolTipText("Path to MicroEmulator JAR (configure in Settings)");
        instanceCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        instanceCountSpinner.setToolTipText("Number of instances to create (1-20)");

        JPanel configurationPanel = ConfigurationPanelBuilder.createConfigurationPanel(
                applicationComboBox, instanceCountSpinner, microemulatorPathField,
                this::openSettingsDialog);
        mainPanel.add(configurationPanel, BorderLayout.NORTH);

        emulatorInstancesPanel = new JPanel();
        emulatorInstancesPanel.setLayout(new BoxLayout(emulatorInstancesPanel, BoxLayout.Y_AXIS));
        emulatorInstancesPanel.setBackground(new Color(240, 240, 240));
        emulatorInstanceManager = new EmulatorInstanceManager(emulatorInstancesPanel);

        JScrollPane scrollPane = new JScrollPane(emulatorInstancesPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Instances"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = createActionButtonsPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

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

        JButton createButton = createStyledButton("Create Instances", new Color(70, 130, 180), this::createEmulatorInstances);
        createButton.setToolTipText("Create instances from selected application (not started yet)");

        JButton runAllButton = createStyledButton("Run All", new Color(34, 139, 34), this::runAllInstances);
        runAllButton.setToolTipText("Run all created instances");

        JButton stopAllButton = createStyledButton("Stop All", new Color(220, 20, 60), this::stopAllInstances);
        stopAllButton.setToolTipText("Stop all running instances");

        JButton clearAllButton = createStyledButton("Clear All", new Color(169, 169, 169), this::clearAllEmulatorInstances);
        clearAllButton.setToolTipText("Remove all instances (including running ones)");

        panel.add(createButton);
        panel.add(runAllButton);
        panel.add(stopAllButton);
        panel.add(clearAllButton);

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
    }

    private void openSettingsDialog() {
        SettingsDialog settingsDialog = new SettingsDialog(this, applicationConfig);
        settingsDialog.setVisible(true);

        if (settingsDialog.isSettingsModified()) {
            loadApplicationConfiguration();
        }
    }


    /**
     * Create instances without running them
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

        for (int i = 0; i < numberOfInstances; i++) {
            int instanceId = emulatorInstanceManager.getNextInstanceId();
            EmulatorInstance emulatorInstance = new EmulatorInstance(instanceId, microemulatorPath, j2meFilePath);

            emulatorInstanceManager.addInstance(emulatorInstance);
            addEmulatorInstanceToPanel(emulatorInstance);
        }

        emulatorInstancesPanel.revalidate();
        emulatorInstancesPanel.repaint();

        showToast("Created " + numberOfInstances + " instance(s) for '" + selectedApp.getName() + "'", ToastNotification.ToastType.SUCCESS);
    }

    /**
     * Run all created instances
     */
    private void runAllInstances() {
        java.util.List<EmulatorInstance> runnableInstances = emulatorInstanceManager.getRunnableInstances();

        if (runnableInstances.isEmpty()) {
            showInfoMessage("No instances available to run. Create instances first.");
            return;
        }

        for (EmulatorInstance instance : runnableInstances) {
            runSingleInstance(instance);
        }
    }

    /**
     * Run a single instance
     */
    public void runSingleInstance(EmulatorInstance emulatorInstance) {
        if (!emulatorInstance.canRun()) {
            showErrorMessage("Instance #" + emulatorInstance.instanceId + " cannot be run in current state.");
            return;
        }

        new Thread(() -> EmulatorLauncher.startEmulatorInstance(
                emulatorInstance,
                // onComplete callback
                () -> SwingUtilities.invokeLater(() -> {
                    updateInstanceUI(emulatorInstance);
                    if (emulatorInstance.state == InstanceState.RUNNING) {
                        addEmulatorInstanceTab(emulatorInstance);
                        // No need to arrange windows since instances are displayed in tabs
                        // autoArrangeEmulatorInstances();
                    }
                }),
                // onStarted callback
                () -> SwingUtilities.invokeLater(() -> updateInstanceUI(emulatorInstance))
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

        for (EmulatorInstance instance : runningInstances) {
            instance.shutdown();
            updateInstanceUI(instance);
        }

        showToast("Stopped " + runningInstances.size() + " instance(s)", ToastNotification.ToastType.INFO);
    }

    public void addEmulatorInstanceToPanel(EmulatorInstance emulatorInstance) {
        JPanel panel = EmulatorInstanceUIBuilder.buildEmulatorInstancePanel(
                emulatorInstance,
                () -> emulatorInstanceManager.moveInstanceUp(emulatorInstance),
                () -> emulatorInstanceManager.moveInstanceDown(emulatorInstance),
                () -> runSingleInstance(emulatorInstance),
                () -> {
                    emulatorInstance.shutdown();
                    updateInstanceUI(emulatorInstance);
                },
                () -> {
                    emulatorInstanceManager.removeInstance(emulatorInstance);
                    emulatorInstancesPanel.revalidate();
                    emulatorInstancesPanel.repaint();
                }
        );

        emulatorInstance.uiPanel = panel;
        emulatorInstancesPanel.add(panel);
    }

    /**
     * Update UI for an instance
     */
    private void updateInstanceUI(EmulatorInstance instance) {
        EmulatorInstanceUIBuilder.updateInstancePanel(
                instance,
                () -> emulatorInstanceManager.moveInstanceUp(instance),
                () -> emulatorInstanceManager.moveInstanceDown(instance),
                () -> runSingleInstance(instance),
                () -> {
                    instance.shutdown();
                    updateInstanceUI(instance);
                },
                () -> {
                    emulatorInstanceManager.removeInstance(instance);
                    emulatorInstancesPanel.revalidate();
                    emulatorInstancesPanel.repaint();
                }
        );
    }

    public void stopEmulatorInstance(EmulatorInstance emulatorInstance) {
        emulatorInstance.state = InstanceState.STOPPED;
        updateInstanceUI(emulatorInstance);
        removeEmulatorInstanceTab(emulatorInstance);
    }

    /**
     * Recalculate grid layout based on panel size and emulator display dimensions
     * Automatically adjusts the number of instances per row based on available width
     */
    private void recalculateGridLayout() {
        if (runningInstancesPanel == null || runningInstancesPanel.getComponentCount() == 0) {
            return;
        }

        // Get parent container (ScrollPane viewport) for accurate available width
        Container parent = runningInstancesPanel.getParent();
        int availableWidth;

        if (parent instanceof JViewport) {
            // Get viewport width for accurate calculation (excludes scrollbar)
            availableWidth = ((JViewport) parent).getWidth();
        } else {
            availableWidth = runningInstancesPanel.getWidth();
        }

        if (availableWidth <= 0) {
            return;
        }

        // Subtract padding to account for panel margins
        int panelPadding = 20; // Account for potential scrollbar and padding
        availableWidth = Math.max(100, availableWidth - panelPadding);

        // Get the actual width of emulatorDisplay from first wrapper panel
        Component firstWrapperPanel = runningInstancesPanel.getComponent(0);
        int instanceWidth = 0;
        int instanceHeight = 0;

        if (firstWrapperPanel instanceof JPanel) {
            JPanel wrapper = (JPanel) firstWrapperPanel;
            // Get the emulatorDisplay from wrapper
            if (wrapper.getComponentCount() > 0) {
                Component display = wrapper.getComponent(0);

                // Try to get actual dimensions from the display component
                if (display.getWidth() > 0 && display.getHeight() > 0) {
                    instanceWidth = display.getWidth();
                    instanceHeight = display.getHeight();
                } else if (display.getPreferredSize().width > 0 && display.getPreferredSize().height > 0) {
                    instanceWidth = display.getPreferredSize().width;
                    instanceHeight = display.getPreferredSize().height;
                }

                // Add border insets to both width and height
                if (wrapper.getBorder() != null) {
                    Insets insets = wrapper.getBorder().getBorderInsets(wrapper);
                    if (instanceWidth > 0) {
                        instanceWidth += insets.left + insets.right;
                    }
                    if (instanceHeight > 0) {
                        instanceHeight += insets.top + insets.bottom;
                    }
                }
            }
        }

        // Fallback to wrapper's preferred size if display size not available
        if (instanceWidth <= 0 || instanceHeight <= 0) {
            Dimension wrapperSize = firstWrapperPanel.getPreferredSize();
            if (instanceWidth <= 0 && wrapperSize.width > 0) {
                instanceWidth = wrapperSize.width;
            }
            if (instanceHeight <= 0 && wrapperSize.height > 0) {
                instanceHeight = wrapperSize.height;
            }
        }

        // Use default size if still not available
        if (instanceWidth <= 0) {
            // Default emulator display width (240px) + border (around 20-30px)
            instanceWidth = 270;
        }
        if (instanceHeight <= 0) {
            // Default emulator display height (320px) + border
            instanceHeight = 350;
        }

        // Calculate optimal number of columns based on available width and instance width
        int hgap = 10; // Horizontal gap between instances
        int columns = Math.max(1, (availableWidth + hgap) / (instanceWidth + hgap));

        // Update grid layout (0 rows means unlimited rows, auto-expand)
        GridLayout layout = (GridLayout) runningInstancesPanel.getLayout();
        int currentColumns = layout.getColumns();

        // Only update if columns changed to avoid unnecessary reflows
        if (currentColumns != columns) {
            layout.setColumns(columns);
            runningInstancesPanel.revalidate();
            runningInstancesPanel.repaint();
        }
    }

    /**
     * Add emulator display to running instances panel
     */
    public void addEmulatorInstanceTab(EmulatorInstance emulatorInstance) {
        if (emulatorInstance.emulatorDisplay != null) {
            // Add title border to the display panel
            JPanel wrapperPanel = new JPanel(new BorderLayout());
            wrapperPanel.setBorder(BorderFactory.createTitledBorder("Instance #" + emulatorInstance.instanceId));
            wrapperPanel.add(emulatorInstance.emulatorDisplay, BorderLayout.CENTER);

            // Store wrapper panel reference for later removal
            emulatorInstance.emulatorDisplay.putClientProperty("wrapperPanel", wrapperPanel);

            runningInstancesPanel.add(wrapperPanel);
            runningInstancesPanel.revalidate();
            runningInstancesPanel.repaint();

            // Recalculate grid layout after component is fully rendered
            // Use invokeLater twice to ensure component has been laid out
            SwingUtilities.invokeLater(() -> {
                SwingUtilities.invokeLater(this::recalculateGridLayout);
            });
        }
    }

    /**
     * Remove emulator display from running instances panel
     */
    public void removeEmulatorInstanceTab(EmulatorInstance emulatorInstance) {
        if (emulatorInstance.emulatorDisplay != null) {
            // Get wrapper panel and remove it
            JPanel wrapperPanel = (JPanel) emulatorInstance.emulatorDisplay.getClientProperty("wrapperPanel");
            if (wrapperPanel != null) {
                runningInstancesPanel.remove(wrapperPanel);
                runningInstancesPanel.revalidate();
                runningInstancesPanel.repaint();

                // Recalculate grid layout after removing instance
                SwingUtilities.invokeLater(this::recalculateGridLayout);
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