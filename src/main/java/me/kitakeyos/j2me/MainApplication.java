package me.kitakeyos.j2me;

import me.kitakeyos.j2me.config.ApplicationConfig;
import me.kitakeyos.j2me.config.EmulatorInstance;
import me.kitakeyos.j2me.config.EmulatorInstance.InstanceState;
import me.kitakeyos.j2me.config.J2meApplication;
import me.kitakeyos.j2me.launcher.EmulatorLauncher;
import me.kitakeyos.j2me.manager.EmulatorInstanceManager;
import me.kitakeyos.j2me.manager.J2meApplicationManager;
import me.kitakeyos.j2me.ui.ApplicationsPanel;
import me.kitakeyos.j2me.ui.ConfigurationPanelBuilder;
import me.kitakeyos.j2me.ui.EmulatorInstanceUIBuilder;
import me.kitakeyos.j2me.ui.SettingsDialog;
import me.kitakeyos.j2me.ui.WindowArrangement;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

/**
 * Main J2ME Launcher application
 */
public class MainApplication extends JFrame {

    public static final MainApplication INSTANCE = new MainApplication();

    private JComboBox<J2meApplication> applicationComboBox;
    private JTextField microemulatorPathField;
    private JSpinner instanceCountSpinner;
    private JPanel emulatorInstancesPanel;
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

        JButton arrangeButton = createStyledButton("Arrange", new Color(255, 140, 0), this::arrangeEmulatorInstances);
        arrangeButton.setToolTipText("Arrange running instance windows in a grid");

        panel.add(createButton);
        panel.add(runAllButton);
        panel.add(stopAllButton);
        panel.add(clearAllButton);
        panel.add(arrangeButton);

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

        showInfoMessage("Created " + numberOfInstances + " instance(s) for '" + selectedApp.getName() + "'. Click 'Run' on each instance or 'Run All' to start them.");
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
                        autoArrangeEmulatorInstances();
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

        showInfoMessage("Stopped " + runningInstances.size() + " instance(s).");
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
    }

    private void arrangeEmulatorInstances() {
        java.util.List<EmulatorInstance> runningInstances = emulatorInstanceManager.getRunningInstances();

        if (runningInstances.isEmpty()) {
            showInfoMessage("No running instances to arrange");
            return;
        }

        WindowArrangement.arrangeInstances(runningInstances);
        String gridInfo = WindowArrangement.getGridInfo(runningInstances);
        showInfoMessage("Windows arranged: " + gridInfo);
    }

    /**
     * Auto-arrange running instances
     */
    private void autoArrangeEmulatorInstances() {
        java.util.List<EmulatorInstance> runningInstances = emulatorInstanceManager.getRunningInstances();
        if (!runningInstances.isEmpty()) {
            WindowArrangement.arrangeInstances(runningInstances);
        }
    }

    private void clearAllEmulatorInstances() {
        int count = emulatorInstanceManager.getInstanceCount();
        if (count == 0) {
            showInfoMessage("No instances to clear.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to clear all " + count + " instance(s)?",
                "Confirm Clear All",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            emulatorInstanceManager.clearAllInstances();
            emulatorInstanceManager.resetInstanceIdCounter();
            showInfoMessage("Cleared all instances.");
        }
    }

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfoMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
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