package me.kitakeyos.j2me.presentation.emulator.panel;

import me.kitakeyos.j2me.application.MainApplication;
import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.domain.application.model.J2meApplication;
import me.kitakeyos.j2me.domain.application.service.ApplicationService;
import me.kitakeyos.j2me.application.emulator.EmulatorLauncher;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorConfig;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance.InstanceState;
import me.kitakeyos.j2me.domain.emulator.repository.EmulatorConfigRepository;
import me.kitakeyos.j2me.domain.emulator.service.InstanceManager;
import me.kitakeyos.j2me.infrastructure.input.InputSynchronizerImpl;
import me.kitakeyos.j2me.presentation.common.builder.ConfigurationPanelBuilder;
import me.kitakeyos.j2me.presentation.common.component.BaseTabPanel;
import me.kitakeyos.j2me.presentation.common.component.ScrollablePanel;
import me.kitakeyos.j2me.presentation.common.component.ToastNotification;
import me.kitakeyos.j2me.presentation.common.dialog.MessageDialog;
import me.kitakeyos.j2me.presentation.common.i18n.Messages;
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
    private JComboBox<EmulatorConfig> emulatorComboBox;
    private JSpinner instanceCountSpinner;
    private JSpinner displayWidthSpinner;
    private JSpinner displayHeightSpinner;
    private JButton syncInputButton;
    private JCheckBox scaleInputBySizeCheckBox;
    private JCheckBox fullDisplayModeCheckBox;
    private JCheckBox disableGraphicsCheckBox; // New global toggle
    private JComboBox<String> defaultSpeedComboBox;
    private ScrollablePanel runningInstancesPanel;
    private JLabel instancesEmptyLabel;

    // Speed options
    private static final String[] SPEED_OPTIONS = { "0.5x", "1x", "2x", "3x", "5x", "10x", "20x" };
    private static final double[] SPEED_VALUES = { 0.5, 1.0, 2.0, 3.0, 5.0, 10.0, 20.0 };

    // Services and managers
    public InstanceManager emulatorInstanceManager;

    // Emulator config repository
    private EmulatorConfigRepository emulatorConfigRepository;

    // Thread pool for launching emulator instances
    private final ExecutorService instanceLauncherPool = Executors.newCachedThreadPool();

    public InstancesPanel(MainApplication mainApplication, ApplicationConfig applicationConfig,
            ApplicationService j2meApplicationManager) {
        super(mainApplication, applicationConfig, j2meApplicationManager);
    }

    /**
     * Set the emulator config repository and refresh the combo box.
     * Called after construction since the repository may not be available at
     * constructor time.
     */
    public void setEmulatorConfigRepository(EmulatorConfigRepository repository) {
        this.emulatorConfigRepository = repository;
        refreshEmulatorComboBox();
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
        applicationComboBox.setToolTipText(Messages.get("inst.app.tooltip"));
        refreshApplicationComboBox();

        // Emulator selector dropdown
        emulatorComboBox = new JComboBox<>();
        emulatorComboBox.setToolTipText(Messages.get("inst.emu.tooltip"));
        emulatorComboBox.addActionListener(e -> onEmulatorSelectionChanged());

        instanceCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        instanceCountSpinner.setToolTipText(Messages.get("inst.count.tooltip"));

        // Display size spinners (default 240x320)
        displayWidthSpinner = new JSpinner(new SpinnerNumberModel(240, 128, 800, 1));
        displayWidthSpinner.setToolTipText(Messages.get("inst.width.tooltip"));
        displayWidthSpinner.setPreferredSize(new Dimension(80, displayWidthSpinner.getPreferredSize().height));

        displayHeightSpinner = new JSpinner(new SpinnerNumberModel(320, 128, 1000, 1));
        displayHeightSpinner.setToolTipText(Messages.get("inst.height.tooltip"));
        displayHeightSpinner.setPreferredSize(new Dimension(80, displayHeightSpinner.getPreferredSize().height));

        // Default speed option
        defaultSpeedComboBox = new JComboBox<>(SPEED_OPTIONS);
        defaultSpeedComboBox.setSelectedIndex(1); // Default 1x
        defaultSpeedComboBox.setToolTipText(Messages.get("inst.speed.tooltip"));

        JPanel configurationPanel = ConfigurationPanelBuilder.createConfigurationPanel(
                applicationComboBox, instanceCountSpinner, emulatorComboBox,
                displayWidthSpinner, displayHeightSpinner, defaultSpeedComboBox);
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
        instancesEmptyLabel = new JLabel(Messages.get("inst.empty"));
        instancesEmptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        instancesEmptyLabel.setForeground(Color.GRAY);
        runningInstancesPanel.add(instancesEmptyLabel);

        // Resize empty label to full width whenever panel is laid out
        runningInstancesPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (instancesEmptyLabel.isVisible()) {
                    int w = runningInstancesPanel.getWidth() - 30;
                    instancesEmptyLabel.setPreferredSize(new Dimension(Math.max(w, 100), 40));
                    runningInstancesPanel.revalidate();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(runningInstancesPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                Messages.get("inst.running.title"),
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        return scrollPane;
    }

    @Override
    protected void onInitialized() {
        // Wire InputSynchronizer implementation
        emulatorInstanceManager.setInputSynchronizer(new InputSynchronizerImpl(emulatorInstanceManager));

        updateInstancesEmptyState();
    }

    private JPanel createActionButtonsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton createButton = new JButton(Messages.get("inst.createButton"));
        createButton.setToolTipText(Messages.get("inst.createButton.tooltip"));
        createButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createButton.setPreferredSize(new Dimension(130, 30));
        createButton.setMaximumSize(new Dimension(130, 30));
        createButton.addActionListener(e -> createEmulatorInstances());

        JButton stopAllButton = new JButton(Messages.get("inst.stopAllButton"));
        stopAllButton.setToolTipText(Messages.get("inst.stopAllButton.tooltip"));
        stopAllButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        stopAllButton.setPreferredSize(new Dimension(130, 30));
        stopAllButton.setMaximumSize(new Dimension(130, 30));
        stopAllButton.addActionListener(e -> stopAllInstances());

        JButton networkMonitorButton = new JButton(Messages.get("inst.networkMonitor"));
        networkMonitorButton.setToolTipText(Messages.get("inst.networkMonitor.tooltip"));
        networkMonitorButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        networkMonitorButton.setPreferredSize(new Dimension(130, 30));
        networkMonitorButton.setMaximumSize(new Dimension(130, 30));
        networkMonitorButton.addActionListener(e -> openNetworkMonitor());

        JButton systemMonitorButton = new JButton(Messages.get("inst.systemMonitor"));
        systemMonitorButton.setToolTipText(Messages.get("inst.systemMonitor.tooltip"));
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
                BorderFactory.createTitledBorder(Messages.get("inst.options.title")),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        // Input synchronization button - opens selection dialog
        syncInputButton = new JButton(Messages.get("inst.syncInput"));
        syncInputButton.setToolTipText(Messages.get("inst.syncInput.tooltip"));
        syncInputButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        syncInputButton.addActionListener(e -> showSyncSelectionDialog());

        // Scale input by size option
        scaleInputBySizeCheckBox = new JCheckBox(Messages.get("inst.scaleInput"));
        scaleInputBySizeCheckBox.setToolTipText(Messages.get("inst.scaleInput.tooltip"));
        scaleInputBySizeCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        scaleInputBySizeCheckBox.addActionListener(e -> {
            boolean enabled = scaleInputBySizeCheckBox.isSelected();
            if (emulatorInstanceManager != null) {
                emulatorInstanceManager.setInputScaleBySize(enabled);
                String message = Messages.get(enabled ? "inst.scaleInput.enabled" : "inst.scaleInput.disabled");
                showToast(message, ToastNotification.ToastType.INFO);
                statusBar.setInfo(message);
            }
        });

        // Full display mode option
        fullDisplayModeCheckBox = new JCheckBox(Messages.get("inst.fullDisplay"));
        fullDisplayModeCheckBox.setToolTipText(Messages.get("inst.fullDisplay.tooltip"));
        fullDisplayModeCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Disable Graphics Toggle
        disableGraphicsCheckBox = new JCheckBox(Messages.get("inst.disableGraphics"));
        disableGraphicsCheckBox.setToolTipText(Messages.get("inst.disableGraphics.tooltip"));
        disableGraphicsCheckBox.addActionListener(e -> {
            boolean disable = disableGraphicsCheckBox.isSelected();
            if (emulatorInstanceManager != null) {
                // !disable because service takes "enabled"
                emulatorInstanceManager.setGlobalGraphicsEnabled(!disable);
                statusBar.setInfo(Messages.get(disable ? "inst.graphicsDisabledAll" : "inst.graphicsEnabledAll"));
            }
        });

        // Max paint FPS spinner (caps SwingDisplayComponent.repaintRequest)
        JPanel fpsRow = new JPanel();
        fpsRow.setLayout(new BoxLayout(fpsRow, BoxLayout.X_AXIS));
        fpsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel fpsLabel = new JLabel("Max paint FPS: ");
        int currentFps = applicationConfig.getMaxPaintFps();
        JSpinner fpsSpinner = new JSpinner(new SpinnerNumberModel(currentFps, 5, 60, 5));
        fpsSpinner.setMaximumSize(new Dimension(80, 25));
        fpsSpinner.setToolTipText("Cap MicroEmulator paint rate. Game logic speed is unaffected.");
        fpsSpinner.addChangeListener(e -> {
            int fps = (Integer) fpsSpinner.getValue();
            applicationConfig.setMaxPaintFps(fps);
            applicationConfig.saveConfiguration();
            me.kitakeyos.j2me.infrastructure.bytecode.PaintThrottleConfig.setFps(fps);
        });
        fpsRow.add(fpsLabel);
        fpsRow.add(fpsSpinner);

        // Idle sleep spinner — drops paints entirely after N seconds idle.
        JPanel idleRow = new JPanel();
        idleRow.setLayout(new BoxLayout(idleRow, BoxLayout.X_AXIS));
        idleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel idleLabel = new JLabel("Idle sleep (sec, 0=off): ");
        int currentIdle = applicationConfig.getIdleSleepSeconds();
        JSpinner idleSpinner = new JSpinner(new SpinnerNumberModel(currentIdle, 0, 3600, 10));
        idleSpinner.setMaximumSize(new Dimension(80, 25));
        idleSpinner.setToolTipText("After this many seconds without mouse/keyboard activity, stop rendering all MIDlet canvases. 0 disables.");
        idleSpinner.addChangeListener(e -> {
            int sec = (Integer) idleSpinner.getValue();
            applicationConfig.setIdleSleepSeconds(sec);
            applicationConfig.saveConfiguration();
            me.kitakeyos.j2me.infrastructure.bytecode.PaintThrottleConfig.setIdleTimeoutSeconds(sec);
        });
        idleRow.add(idleLabel);
        idleRow.add(idleSpinner);

        panel.add(syncInputButton);
        panel.add(Box.createVerticalStrut(5));
        panel.add(scaleInputBySizeCheckBox);
        panel.add(Box.createVerticalStrut(5));
        panel.add(fullDisplayModeCheckBox);
        panel.add(Box.createVerticalStrut(5));
        panel.add(disableGraphicsCheckBox);
        panel.add(Box.createVerticalStrut(5));
        panel.add(fpsRow);
        panel.add(Box.createVerticalStrut(5));
        panel.add(idleRow);

        return panel;
    }

    /**
     * When emulator selection changes, auto-populate display size spinners
     */
    private void onEmulatorSelectionChanged() {
        EmulatorConfig selected = (EmulatorConfig) emulatorComboBox.getSelectedItem();
        if (selected != null) {
            displayWidthSpinner.setValue(selected.getDefaultDisplayWidth());
            displayHeightSpinner.setValue(selected.getDefaultDisplayHeight());
        }
    }

    /**
     * Refresh the emulator combo box with latest emulator configs
     */
    public void refreshEmulatorComboBox() {
        if (emulatorConfigRepository == null)
            return;
        EmulatorConfig previousSelection = (EmulatorConfig) emulatorComboBox.getSelectedItem();
        emulatorComboBox.removeAllItems();
        for (EmulatorConfig config : emulatorConfigRepository.getAll()) {
            emulatorComboBox.addItem(config);
        }
        // Restore previous selection if still available
        if (previousSelection != null) {
            for (int i = 0; i < emulatorComboBox.getItemCount(); i++) {
                if (emulatorComboBox.getItemAt(i).getId().equals(previousSelection.getId())) {
                    emulatorComboBox.setSelectedIndex(i);
                    break;
                }
            }
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
            showErrorMessage(Messages.get("inst.error.noApp"));
            return;
        }

        EmulatorConfig selectedEmulator = (EmulatorConfig) emulatorComboBox.getSelectedItem();
        if (selectedEmulator == null) {
            showErrorMessage(Messages.get("inst.error.noEmulator"));
            return;
        }

        if (!selectedEmulator.isValid()) {
            showErrorMessage(Messages.get("inst.error.invalidEmulator", selectedEmulator.getJarPath()));
            return;
        }

        int numberOfInstances = (Integer) instanceCountSpinner.getValue();
        String microemulatorPath = selectedEmulator.getJarPath();
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

        String message = Messages.get("inst.starting", numberOfInstances, selectedApp.getName());
        showToast(message, ToastNotification.ToastType.SUCCESS);
        statusBar.setSuccess(message);
    }

    /**
     * Run a single instance
     */
    public void runSingleInstance(EmulatorInstance emulatorInstance) {
        if (!emulatorInstance.canRun()) {
            showErrorMessage(Messages.get("inst.error.cannotRun", emulatorInstance.getInstanceId()));
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
     * Stop all running instances.
     * UI tabs are removed immediately on EDT, then shutdowns run in parallel on background threads.
     */
    private void stopAllInstances() {
        java.util.List<EmulatorInstance> runningInstances = emulatorInstanceManager.getRunningInstances();

        if (runningInstances.isEmpty()) {
            showInfoMessage(Messages.get("inst.noRunning"));
            statusBar.setInfo(Messages.get("inst.noRunningStatus"));
            return;
        }

        // Remove UI tabs immediately so the user sees instant feedback
        for (EmulatorInstance instance : runningInstances) {
            removeEmulatorInstanceTab(instance);
        }

        String message = Messages.get("inst.stopping", runningInstances.size());
        showToast(message, ToastNotification.ToastType.INFO);
        statusBar.setInfo(message);

        // Shutdown all instances in parallel on background threads
        int count = runningInstances.size();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(count);
        for (EmulatorInstance instance : runningInstances) {
            new Thread(() -> {
                try {
                    instance.shutdown();
                } finally {
                    latch.countDown();
                }
            }, "shutdown-instance-" + instance.getInstanceId()).start();
        }

        // Notify completion asynchronously without blocking EDT
        new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            SwingUtilities.invokeLater(() -> {
                String doneMessage = Messages.get("inst.stopped", count);
                showToast(doneMessage, ToastNotification.ToastType.INFO);
                statusBar.setInfo(doneMessage);
            });
        }, "shutdown-wait").start();
    }

    /**
     * Update empty state visibility based on number of running instances
     */
    private void updateInstancesEmptyState() {
        boolean hasInstances = runningInstancesPanel.getComponentCount() > 1; // More than just the empty label
        instancesEmptyLabel.setVisible(!hasInstances);
        if (hasInstances) {
            int count = runningInstancesPanel.getComponentCount() - 1; // Exclude empty label
            statusBar.setInfo(Messages.get("inst.status.running", count));
        } else {
            statusBar.setInfo(Messages.get("inst.status.noRunning"));
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
        JMenuBar menuBar = new JMenuBar() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                if (isOpaque() && getBackground() != null) {
                    g.setColor(getBackground());
                    g.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    super.paintComponent(g);
                }
            }
        };

        // Instance title/info as a non-clickable label
        JLabel titleLabel = new JLabel("  " + Messages.get("inst.instance.title", emulatorInstance.getInstanceId()) + "  ");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        menuBar.add(titleLabel);

        // Push Actions menu to the right
        menuBar.add(Box.createHorizontalGlue());

        // Actions menu (contains all instance controls)
        JMenu actionsMenu = new JMenu(Messages.get("inst.actions"));
        actionsMenu.setToolTipText(Messages.get("inst.actions.tooltip"));

        // Speed submenu - find initial text based on defaultSpeed
        String initialSpeedText = Messages.get("inst.speed");
        for (int i = 0; i < SPEED_VALUES.length; i++) {
            if (Math.abs(SPEED_VALUES[i] - defaultSpeed) < 0.01) {
                initialSpeedText = Messages.get("inst.speed.format", SPEED_OPTIONS[i]);
                break;
            }
        }
        JMenu speedSubmenu = new JMenu(initialSpeedText);
        speedSubmenu.setToolTipText(Messages.get("inst.speed.submenu.tooltip"));

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
                speedSubmenu.setText(Messages.get("inst.speed.format", SPEED_OPTIONS[index]));
                showToast(Messages.get("inst.speed.set", emulatorInstance.getInstanceId(), SPEED_OPTIONS[index]),
                        ToastNotification.ToastType.INFO);
            });

            speedGroup.add(item);
            speedSubmenu.add(item);
        }
        actionsMenu.add(speedSubmenu);

        // Graphics optimization (Stop Painting)
        JCheckBoxMenuItem graphicsItem = new JCheckBoxMenuItem(Messages.get("inst.disableGraphicsItem"));
        graphicsItem.setToolTipText(Messages.get("inst.disableGraphicsItem.tooltip"));
        graphicsItem.addActionListener(e -> {
            boolean disableGraphics = graphicsItem.isSelected();
            me.kitakeyos.j2me.domain.graphics.service.GraphicsOptimizationService.getInstance()
                    .setGraphicsEnabled(emulatorInstance, !disableGraphics);

            String status = disableGraphics ? "DISABLED" : "ENABLED";
            showToast(Messages.get("inst.graphics.status", status, emulatorInstance.getInstanceId()),
                    ToastNotification.ToastType.INFO);
        });
        actionsMenu.add(graphicsItem);

        // Packet capture toggle
        JCheckBoxMenuItem packetCaptureItem = new JCheckBoxMenuItem(Messages.get("inst.packetCapture"));
        packetCaptureItem.setToolTipText(Messages.get("inst.packetCapture.tooltip"));
        packetCaptureItem.addActionListener(e -> {
            me.kitakeyos.j2me.domain.network.service.NetworkService ns =
                    me.kitakeyos.j2me.domain.network.service.NetworkService.getInstance();
            if (packetCaptureItem.isSelected()) {
                ns.enableTapping(emulatorInstance.getInstanceId());
            } else {
                ns.disableTapping(emulatorInstance.getInstanceId());
            }
            String status = packetCaptureItem.isSelected() ? "ON" : "OFF";
            showToast(Messages.get("inst.packetCapture.status", status, emulatorInstance.getInstanceId()),
                    ToastNotification.ToastType.INFO);
        });
        actionsMenu.add(packetCaptureItem);

        actionsMenu.addSeparator();

        // Stop instance
        JMenuItem stopItem = new JMenuItem(Messages.get("inst.stopInstance"));
        stopItem.addActionListener(e -> {
            removeEmulatorInstanceTab(emulatorInstance);
            new Thread(() -> {
                emulatorInstance.shutdown();
                SwingUtilities.invokeLater(() ->
                    showToast(Messages.get("inst.stoppedSingle", emulatorInstance.getInstanceId()),
                            ToastNotification.ToastType.INFO));
            }, "shutdown-instance-" + emulatorInstance.getInstanceId()).start();
        });
        actionsMenu.add(stopItem);

        actionsMenu.addSeparator();

        // Placeholder for future features
        JMenuItem screenshotItem = new JMenuItem(Messages.get("inst.takeScreenshot"));
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
                // Update empty state visibility
                updateInstancesEmptyState();
            }
        }
    }

    // Utility methods for dialogs and toast notifications
    private void showErrorMessage(String message) {
        MessageDialog.showError(this, Messages.get("common.error"), message);
    }

    private void showInfoMessage(String message) {
        MessageDialog.showInfo(this, Messages.get("common.info"), message);
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
     * Show dialog to select which instances participate in input sync.
     */
    private void showSyncSelectionDialog() {
        java.util.List<EmulatorInstance> running = emulatorInstanceManager.getRunningInstances();
        if (running.isEmpty()) {
            showInfoMessage(Messages.get("inst.noRunning"));
            return;
        }

        java.util.Set<Integer> currentSynced = emulatorInstanceManager.getSyncedInstanceIds();

        // Build checkbox grid — scale columns with instance count so dialog
        // stays compact when there are many instances.
        int columns = running.size() > 60 ? 5 : running.size() > 20 ? 3 : 1;
        JPanel listPanel = new JPanel(new java.awt.GridLayout(0, columns, 8, 2));

        java.util.List<JCheckBox> checkBoxes = new java.util.ArrayList<>();
        for (EmulatorInstance instance : running) {
            JCheckBox cb = new JCheckBox(
                    Messages.get("inst.instance.title", instance.getInstanceId()),
                    currentSynced.contains(instance.getInstanceId()));
            checkBoxes.add(cb);
            listPanel.add(cb);
        }

        // Cap the dialog height; scroll when it overflows.
        JScrollPane scrollPane = new JScrollPane(listPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        int rowHeight = 24;
        int preferredRows = Math.min((int) Math.ceil(running.size() / (double) columns), 18);
        scrollPane.setPreferredSize(new Dimension(
                columns * 130 + 24,
                preferredRows * rowHeight + 8));

        // Select All / Deselect All buttons
        JPanel buttonRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));
        JButton selectAllBtn = new JButton(Messages.get("inst.sync.selectAll"));
        selectAllBtn.addActionListener(e -> checkBoxes.forEach(cb -> cb.setSelected(true)));
        JButton deselectAllBtn = new JButton(Messages.get("inst.sync.deselectAll"));
        deselectAllBtn.addActionListener(e -> checkBoxes.forEach(cb -> cb.setSelected(false)));
        buttonRow.add(selectAllBtn);
        buttonRow.add(deselectAllBtn);

        JPanel dialogPanel = new JPanel(new BorderLayout(0, 10));
        dialogPanel.add(buttonRow, BorderLayout.NORTH);
        dialogPanel.add(scrollPane, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                this, dialogPanel, Messages.get("inst.syncInput"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            java.util.Set<Integer> selected = new java.util.HashSet<>();
            for (int i = 0; i < checkBoxes.size(); i++) {
                if (checkBoxes.get(i).isSelected()) {
                    selected.add(running.get(i).getInstanceId());
                }
            }

            emulatorInstanceManager.setSyncedInstanceIds(selected);

            if (selected.isEmpty()) {
                emulatorInstanceManager.setInputSynchronizationEnabled(false);
                statusBar.setInfo(Messages.get("inst.syncInput.disabled"));
            } else {
                emulatorInstanceManager.setInputSynchronizationEnabled(true);
                statusBar.setInfo(Messages.get("inst.sync.selected", selected.size()));
            }

            updateSyncHighlights(selected);
        }
    }

    private static final Color SYNC_BG = new Color(0, 120, 215);
    private static final Color SYNC_FG = Color.WHITE;

    /**
     * Update menu bar background to show which instances are synced.
     * Custom paintComponent on JMenuBar allows overriding LAF background.
     */
    private void updateSyncHighlights(java.util.Set<Integer> syncedIds) {
        for (Component comp : runningInstancesPanel.getComponents()) {
            if (!(comp instanceof JPanel)) continue;
            JPanel panel = (JPanel) comp;
            Integer id = (Integer) panel.getClientProperty("instanceId");
            if (id == null) continue;

            Component first = panel.getComponent(0);
            if (!(first instanceof JMenuBar)) continue;
            JMenuBar menuBar = (JMenuBar) first;

            boolean synced = syncedIds.contains(id);
            menuBar.setOpaque(synced);
            menuBar.setBackground(synced ? SYNC_BG : null);

            for (Component child : menuBar.getComponents()) {
                if (child instanceof JLabel) {
                    ((JLabel) child).setForeground(synced ? SYNC_FG : null);
                } else if (child instanceof JMenu) {
                    ((JMenu) child).setForeground(synced ? SYNC_FG : null);
                }
            }
        }
        runningInstancesPanel.repaint();
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
