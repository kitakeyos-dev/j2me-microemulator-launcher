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

import javax.swing.*;

/**
 * Main J2ME Launcher application
 */
public class MainApplication extends JFrame {

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
        setTitle("J2ME MicroEmulator Launcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1090, 800);
        setLocationRelativeTo(null);

        // Initialize managers with dependency injection
        applicationConfig = new ApplicationConfig();
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

    private void initializeComponents() {
        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: Applications
        tabbedPane.addTab("Applications", applicationsPanel);

        // Tab 2: Emulators
        tabbedPane.addTab("Emulators", emulatorsPanel);

        // Tab 3: Instances
        tabbedPane.addTab("Instances", instancesPanel);

        // Tab 4: Injection
        tabbedPane.addTab("Injection", injectionPanel);

        add(tabbedPane);

        // Listen for application changes to update combo box in instances panel
        applicationService.addApplicationChangeListener(new ApplicationService.ApplicationChangeListener() {
            @Override
            public void onApplicationAdded(J2meApplication app) {
                instancesPanel.refreshApplicationComboBox();
            }

            @Override
            public void onApplicationRemoved(J2meApplication app) {
                instancesPanel.refreshApplicationComboBox();
            }
        });

        // Listen for emulator config changes to update combo box in instances panel
        emulatorConfigRepository.addChangeListener(() -> instancesPanel.refreshEmulatorComboBox());

        // Auto-refresh instance combobox in Injection tab
        emulatorInstanceManager.addInstanceChangeListener(() -> {
            injectionPanel.refreshInstanceList();
        });
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
