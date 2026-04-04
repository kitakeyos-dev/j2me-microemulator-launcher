package me.kitakeyos.j2me.application;

import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.domain.application.model.J2meApplication;
import me.kitakeyos.j2me.domain.application.repository.ApplicationRepository;
import me.kitakeyos.j2me.domain.application.service.ApplicationService;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.service.InstanceManager;
import me.kitakeyos.j2me.infrastructure.persistence.application.ApplicationRepositoryImpl;
import me.kitakeyos.j2me.infrastructure.persistence.emulator.EmulatorConfigRepositoryImpl;
import me.kitakeyos.j2me.presentation.emulator.panel.ApplicationsPanel;
import me.kitakeyos.j2me.presentation.emulator.panel.EmulatorsPanel;
import me.kitakeyos.j2me.presentation.emulator.panel.InstancesPanel;
import me.kitakeyos.j2me.presentation.injection.panel.InjectionPanel;

import me.kitakeyos.j2me.presentation.common.dialog.SettingsDialog;
import me.kitakeyos.j2me.presentation.common.i18n.Messages;

import javax.swing.*;

/**
 * Main J2ME Launcher application
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
    private final EmulatorConfigRepositoryImpl emulatorConfigRepository;
    private InstancesPanel instancesPanel;
    private EmulatorsPanel emulatorsPanel;
    public InstanceManager emulatorInstanceManager;
    public ApplicationsPanel applicationsPanel;
    private InjectionPanel injectionPanel;

    public MainApplication() {
        // Initialize config first and load language
        applicationConfig = new ApplicationConfig();
        Messages.loadBundle(applicationConfig.getLanguage());

        setTitle(Messages.get("app.title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1090, 800);
        setLocationRelativeTo(null);

        // Initialize managers with dependency injection
        applicationRepository = new ApplicationRepositoryImpl(applicationConfig);
        applicationService = new ApplicationService(applicationRepository);
        emulatorConfigRepository = new EmulatorConfigRepositoryImpl(applicationConfig);

        applicationsPanel = new ApplicationsPanel(this, applicationConfig, applicationService);
        emulatorsPanel = new EmulatorsPanel(this, applicationConfig, applicationService, emulatorConfigRepository);
        instancesPanel = new InstancesPanel(this, applicationConfig, applicationService);
        injectionPanel = new InjectionPanel(this, applicationConfig, applicationService);

        // Wire emulator config repository to instances panel
        instancesPanel.setEmulatorConfigRepository(emulatorConfigRepository);

        emulatorInstanceManager = instancesPanel.emulatorInstanceManager;

        initializeComponents();
    }

    /**
     * Listener for application changes - kept as field to allow removal on rebuild.
     */
    private ApplicationService.ApplicationChangeListener appChangeListener;
    private EmulatorConfigRepositoryImpl.EmulatorConfigChangeListener emulatorConfigChangeListener;
    private Runnable instanceChangeListener;

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


        instanceChangeListener = () -> injectionPanel.refreshInstanceList();
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

        // Remove all content
        getContentPane().removeAll();
        setJMenuBar(null);

        // Recreate panels
        applicationsPanel = new ApplicationsPanel(this, applicationConfig, applicationService);
        emulatorsPanel = new EmulatorsPanel(this, applicationConfig, applicationService, emulatorConfigRepository);
        instancesPanel = new InstancesPanel(this, applicationConfig, applicationService);
        injectionPanel = new InjectionPanel(this, applicationConfig, applicationService);

        instancesPanel.setEmulatorConfigRepository(emulatorConfigRepository);
        emulatorInstanceManager = instancesPanel.emulatorInstanceManager;

        // Rebuild UI
        setTitle(Messages.get("app.title"));
        initializeComponents();

        revalidate();
        repaint();
    }

    /**
     * Delegate method for compatibility - removes an emulator instance tab
     */
    public void removeEmulatorInstanceTab(EmulatorInstance emulatorInstance) {
        instancesPanel.removeEmulatorInstanceTab(emulatorInstance);
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
