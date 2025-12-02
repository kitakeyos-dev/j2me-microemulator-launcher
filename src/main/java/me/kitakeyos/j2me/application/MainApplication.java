package me.kitakeyos.j2me.application;

import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.domain.application.model.J2meApplication;
import me.kitakeyos.j2me.domain.application.service.ApplicationService;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.service.InstanceManager;
import me.kitakeyos.j2me.presentation.emulator.panel.ApplicationsPanel;
import me.kitakeyos.j2me.presentation.emulator.panel.InstancesPanel;
import me.kitakeyos.j2me.presentation.script.LuaScriptManager;

import javax.swing.*;

/**
 * Main J2ME Launcher application
 */
public class MainApplication extends JFrame {

    public static final MainApplication INSTANCE = new MainApplication();

    private final ApplicationConfig applicationConfig;
    private final ApplicationService applicationService;
    private InstancesPanel instancesPanel;
    public InstanceManager emulatorInstanceManager;
    public LuaScriptManager luaScriptManager;
    public ApplicationsPanel applicationsPanel;

    public MainApplication() {
        setTitle("J2ME MicroEmulator Launcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1090, 800);
        setLocationRelativeTo(null);

        // Initialize managers
        applicationConfig = new ApplicationConfig();
        applicationService = new ApplicationService(applicationConfig);
        applicationsPanel = new ApplicationsPanel(this, applicationConfig, applicationService);
        instancesPanel = new InstancesPanel(this, applicationConfig, applicationService);
        luaScriptManager = new LuaScriptManager(this, applicationConfig, applicationService);

        emulatorInstanceManager = instancesPanel.emulatorInstanceManager;

        initializeComponents();
    }

    private void initializeComponents() {
        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: Applications
        tabbedPane.addTab("Applications", applicationsPanel);

        // Tab 2: Instances
        tabbedPane.addTab("Instances", instancesPanel);

        // Tab 3: Scripts
        tabbedPane.addTab("Scripts", luaScriptManager);

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
    }

    /**
     * Delegate method for compatibility - removes an emulator instance tab
     */
    public void removeEmulatorInstanceTab(EmulatorInstance emulatorInstance) {
        instancesPanel.removeEmulatorInstanceTab(emulatorInstance);
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
