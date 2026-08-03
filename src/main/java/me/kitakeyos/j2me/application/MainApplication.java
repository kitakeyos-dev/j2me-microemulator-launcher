package me.kitakeyos.j2me.application;

import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.domain.application.model.J2meApplication;
import me.kitakeyos.j2me.domain.application.repository.ApplicationRepository;
import me.kitakeyos.j2me.domain.application.service.ApplicationService;
import me.kitakeyos.j2me.domain.emulator.input.InputSynchronizerFactory;
import me.kitakeyos.j2me.domain.emulator.repository.EmulatorConfigRepository;
import me.kitakeyos.j2me.domain.emulator.service.EmulatorRuntime;
import me.kitakeyos.j2me.domain.emulator.service.InstanceCleanupStep;
import me.kitakeyos.j2me.domain.emulator.service.InstanceLifecycleManager;
import me.kitakeyos.j2me.domain.emulator.service.InstanceManager;
import me.kitakeyos.j2me.domain.emulator.service.cleanup.EmulatorExitStep;
import me.kitakeyos.j2me.domain.emulator.service.cleanup.GraphicsCleanupStep;
import me.kitakeyos.j2me.domain.emulator.service.cleanup.NetworkCleanupStep;
import me.kitakeyos.j2me.domain.emulator.service.cleanup.RegistryRemovalStep;
import me.kitakeyos.j2me.domain.emulator.service.cleanup.ResourceCleanupStep;
import me.kitakeyos.j2me.domain.emulator.service.cleanup.SpeedCleanupStep;
import me.kitakeyos.j2me.domain.emulator.service.cleanup.UiComponentCleanupStep;
import me.kitakeyos.j2me.domain.graphics.service.GraphicsOptimizationService;
import me.kitakeyos.j2me.domain.hook.repository.HookProvider;
import me.kitakeyos.j2me.domain.hook.repository.HookRegistry;
import me.kitakeyos.j2me.domain.hook.service.HookService;
import me.kitakeyos.j2me.domain.monitoring.SystemMetrics;
import me.kitakeyos.j2me.domain.network.repository.NetworkRuleCodec;
import me.kitakeyos.j2me.domain.network.service.NetworkService;
import me.kitakeyos.j2me.domain.speed.service.SpeedService;
import me.kitakeyos.j2me.infrastructure.emulator.cleanup.BytecodeCacheEvictionStep;
import me.kitakeyos.j2me.infrastructure.emulator.cleanup.ClassLoaderCleanupStep;
import me.kitakeyos.j2me.infrastructure.emulator.cleanup.HookStateCleanupStep;
import me.kitakeyos.j2me.infrastructure.emulator.cleanup.TransformedJarCleanupStep;
import me.kitakeyos.j2me.infrastructure.hook.HookJarLoader;
import me.kitakeyos.j2me.infrastructure.hook.InMemoryHookRegistry;
import me.kitakeyos.j2me.infrastructure.input.InputSynchronizerImpl;
import me.kitakeyos.j2me.infrastructure.monitoring.SystemMonitorService;
import me.kitakeyos.j2me.infrastructure.network.PlainSocketOpener;
import me.kitakeyos.j2me.infrastructure.persistence.application.ApplicationRepositoryImpl;
import me.kitakeyos.j2me.infrastructure.persistence.emulator.EmulatorConfigRepositoryImpl;
import me.kitakeyos.j2me.infrastructure.persistence.network.ProxyRuleCodec;
import me.kitakeyos.j2me.infrastructure.persistence.network.PropertiesNetworkRuleRepository;
import me.kitakeyos.j2me.infrastructure.persistence.network.RedirectionRuleCodec;
import me.kitakeyos.j2me.presentation.emulator.panel.ApplicationsPanel;
import me.kitakeyos.j2me.presentation.emulator.panel.EmulatorsPanel;
import me.kitakeyos.j2me.presentation.emulator.panel.InstancesPanel;
import me.kitakeyos.j2me.presentation.injection.panel.InjectionPanel;

import me.kitakeyos.j2me.presentation.common.dialog.SettingsDialog;
import me.kitakeyos.j2me.presentation.common.i18n.Messages;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main J2ME Launcher application.
 * <p>
 * Also the composition root: it is the only place that names both a port and
 * its implementation. Everything below it — domain services, cleanup steps,
 * panels — receives what it needs rather than reaching for a global.
 */
public class MainApplication extends JFrame {

    static {
        // Set system Look and Feel BEFORE any Swing component is created
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    public static final MainApplication INSTANCE = new MainApplication();

    private final ApplicationConfig applicationConfig;
    private final ApplicationRepository applicationRepository;
    private final ApplicationService applicationService;
    private final EmulatorConfigRepository emulatorConfigRepository;
    private final HookProvider hookProvider;
    private final HookService hookService;
    private final InputSynchronizerFactory inputSynchronizerFactory = InputSynchronizerImpl::new;
    private final SystemMetrics systemMetrics = new SystemMonitorService();

    private InstancesPanel instancesPanel;
    private EmulatorsPanel emulatorsPanel;
    public InstanceManager emulatorInstanceManager;
    public ApplicationsPanel applicationsPanel;
    private InjectionPanel injectionPanel;

    public MainApplication() {
        // Initialize config first and load language
        applicationConfig = new ApplicationConfig();
        Messages.loadBundle(applicationConfig.getLanguage());
        me.kitakeyos.j2me.infrastructure.bytecode.PaintThrottleConfig.setFps(
                applicationConfig.getMaxPaintFps());

        setTitle(Messages.get("app.title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1090, 800);
        setLocationRelativeTo(null);
        loadAppIcons();

        // Drop every MicroEmulator paint while the launcher is minimized to
        // the taskbar — no point rendering canvases the user cannot see.
        addWindowStateListener(e -> {
            boolean iconified = (e.getNewState() & java.awt.Frame.ICONIFIED) != 0;
            me.kitakeyos.j2me.infrastructure.bytecode.PaintThrottleConfig.windowMinimized = iconified;
        });

        // Track user activity for idle-based paint sleeping. Writes happen
        // on the EDT, reads happen from MIDlet threads — both via volatile.
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(
                evt -> me.kitakeyos.j2me.infrastructure.bytecode.PaintThrottleConfig.lastActivityMs
                        = System.currentTimeMillis(),
                java.awt.AWTEvent.MOUSE_EVENT_MASK
                        | java.awt.AWTEvent.MOUSE_MOTION_EVENT_MASK
                        | java.awt.AWTEvent.KEY_EVENT_MASK);
        me.kitakeyos.j2me.infrastructure.bytecode.PaintThrottleConfig.setIdleTimeoutSeconds(
                applicationConfig.getIdleSleepSeconds());

        // Initialize managers with dependency injection
        applicationRepository = new ApplicationRepositoryImpl(applicationConfig);
        applicationService = new ApplicationService(applicationRepository);
        emulatorConfigRepository = new EmulatorConfigRepositoryImpl(applicationConfig);

        configureNetworkService();

        // The bytecode pipeline reads this same registry through
        // InMemoryHookRegistry.getInstance(); the UI goes through the ports.
        HookRegistry hookRegistry = InMemoryHookRegistry.getInstance();
        hookService = new HookService(hookRegistry);
        hookProvider = new HookJarLoader(hookRegistry);

        createPanels();
        bindEmulatorRuntime();

        initializeComponents();
    }

    /**
     * Give the network stack its persistence and its socket implementation.
     * Until this runs, NetworkService keeps rules in memory only.
     */
    private void configureNetworkService() {
        List<NetworkRuleCodec<?>> codecs = Arrays.asList(
                new RedirectionRuleCodec(),
                new ProxyRuleCodec());
        NetworkService.getInstance().configure(
                new PropertiesNetworkRuleRepository(applicationConfig, codecs),
                new PlainSocketOpener());
    }

    /**
     * Publish the live instance registry, the tab controller and the shutdown
     * pipeline on {@link EmulatorRuntime}.
     * <p>
     * Must be re-run after {@link #rebuildUI()}, which replaces the panels and
     * therefore the instance manager behind them.
     */
    private void bindEmulatorRuntime() {
        EmulatorRuntime.bind(
                emulatorInstanceManager,
                instancesPanel,
                new InstanceLifecycleManager(buildCleanupPipeline()));
    }

    /**
     * The teardown sequence, in execution order.
     * <p>
     * Ordering constraints worth preserving: graphics must be restored before
     * the classloader closes, the emulator's own exit must fire before threads
     * are force-stopped, and the registry entry must outlive the exit step so
     * threads spawned during shutdown can still find their instance.
     */
    private List<InstanceCleanupStep> buildCleanupPipeline() {
        List<InstanceCleanupStep> steps = new ArrayList<>();
        steps.add(new GraphicsCleanupStep(GraphicsOptimizationService.getInstance()));
        steps.add(new SpeedCleanupStep(SpeedService.getInstance()));
        steps.add(new EmulatorExitStep());
        steps.add(new ResourceCleanupStep());
        steps.add(new ClassLoaderCleanupStep());
        steps.add(new TransformedJarCleanupStep());
        steps.add(new NetworkCleanupStep(NetworkService.getInstance()));
        steps.add(new HookStateCleanupStep());
        steps.add(new RegistryRemovalStep(emulatorInstanceManager));
        steps.add(new UiComponentCleanupStep());
        steps.add(new BytecodeCacheEvictionStep(emulatorInstanceManager));
        return steps;
    }

    /**
     * Build the four tab panels and expose the instance manager they share.
     */
    private void createPanels() {
        applicationsPanel = new ApplicationsPanel(this, applicationConfig, applicationService);
        emulatorsPanel = new EmulatorsPanel(this, applicationConfig, applicationService, emulatorConfigRepository);
        instancesPanel = new InstancesPanel(this, applicationConfig, applicationService, inputSynchronizerFactory,
                systemMetrics);
        injectionPanel = new InjectionPanel(this, applicationConfig, applicationService, hookProvider, hookService);

        instancesPanel.setEmulatorConfigRepository(emulatorConfigRepository);
        emulatorInstanceManager = instancesPanel.emulatorInstanceManager;
    }

    /**
     * Listener for application changes - kept as field to allow removal on rebuild.
     */
    private ApplicationService.ApplicationChangeListener appChangeListener;

    /**
     * Emulator config listener - kept as a field so rebuildUI() can detach the
     * one bound to the previous panels instead of stacking duplicates.
     */
    private EmulatorConfigRepository.EmulatorConfigChangeListener emulatorConfigChangeListener;

    private void initializeComponents() {
        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab(Messages.get("tab.applications"), applicationsPanel);
        tabbedPane.addTab(Messages.get("tab.emulators"), emulatorsPanel);
        tabbedPane.addTab(Messages.get("tab.instances"), instancesPanel);
        tabbedPane.addTab(Messages.get("tab.injection"), injectionPanel);

        add(tabbedPane);

        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu(Messages.get("settings.title"));
        JMenuItem settingsItem = new JMenuItem(Messages.get("settings.title"));
        settingsItem.addActionListener(e -> SettingsDialog.show(this, applicationConfig));
        fileMenu.add(settingsItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Store listeners as fields so they can be removed on rebuild
        appChangeListener = new ApplicationService.ApplicationChangeListener() {
            @Override
            public void onApplicationAdded(J2meApplication app) {
                instancesPanel.refreshApplicationComboBox();
            }

            @Override
            public void onApplicationRemoved(J2meApplication app) {
                instancesPanel.refreshApplicationComboBox();
            }
        };
        applicationService.addApplicationChangeListener(appChangeListener);

        emulatorConfigChangeListener = () -> instancesPanel.refreshEmulatorComboBox();
        emulatorConfigRepository.addChangeListener(emulatorConfigChangeListener);

        Runnable instanceChangeListener = () -> injectionPanel.refreshInstanceList();
        emulatorInstanceManager.addInstanceChangeListener(instanceChangeListener);
    }

    /**
     * Rebuild the entire UI after language change.
     * Stops all running instances first, then recreates all panels and menus.
     */
    public void rebuildUI() {
        // Stop all running instances (their display components are tied to old panels)
        if (emulatorInstanceManager != null) {
            emulatorInstanceManager.clearAllInstances();
        }

        // Remove old listeners to prevent duplicates
        if (appChangeListener != null) {
            applicationService.removeApplicationChangeListener(appChangeListener);
        }
        applicationService.removeApplicationChangeListener(applicationsPanel);
        if (emulatorConfigChangeListener != null) {
            emulatorConfigRepository.removeChangeListener(emulatorConfigChangeListener);
        }

        // Remove all content
        getContentPane().removeAll();
        setJMenuBar(null);

        createPanels();
        // The registry and tab controller are new objects; re-publish them.
        bindEmulatorRuntime();

        // Rebuild UI
        setTitle(Messages.get("app.title"));
        initializeComponents();

        revalidate();
        repaint();
    }

    /**
     * Load application icons for the window title bar and taskbar.
     */
    private void loadAppIcons() {
        int[] sizes = {16, 32, 48, 64, 128, 256};
        java.util.List<java.awt.Image> icons = new java.util.ArrayList<>();
        for (int size : sizes) {
            java.net.URL url = getClass().getClassLoader().getResource("icons/icon_" + size + ".png");
            if (url != null) {
                icons.add(new javax.swing.ImageIcon(url).getImage());
            }
        }
        if (!icons.isEmpty()) {
            setIconImages(icons);
        }
    }

    /**
     * Get application configuration
     */
    public ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> INSTANCE.setVisible(true));
    }
}
