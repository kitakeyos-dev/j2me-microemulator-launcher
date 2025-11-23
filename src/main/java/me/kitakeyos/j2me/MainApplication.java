package me.kitakeyos.j2me;

import me.kitakeyos.j2me.config.ApplicationConfig;
import me.kitakeyos.j2me.model.EmulatorInstance;
import me.kitakeyos.j2me.model.J2meApplication;
import me.kitakeyos.j2me.script.ui.LuaScriptManager;
import me.kitakeyos.j2me.service.EmulatorInstanceManager;
import me.kitakeyos.j2me.service.J2meApplicationManager;
import me.kitakeyos.j2me.ui.panel.ApplicationsPanel;
import me.kitakeyos.j2me.ui.panel.InstancesPanel;
import me.kitakeyos.j2me.ui.dialog.SettingsDialog;
import javax.swing.*;

/**
 * Main J2ME Launcher application
 */
public class MainApplication extends JFrame {

    public static final MainApplication INSTANCE = new MainApplication();

    private final ApplicationConfig applicationConfig;
    private final J2meApplicationManager j2meApplicationManager;
    private InstancesPanel instancesPanel;
    public EmulatorInstanceManager emulatorInstanceManager;

    public MainApplication() {
        setTitle("J2ME MicroEmulator Launcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Initialize managers
        applicationConfig = new ApplicationConfig();
        j2meApplicationManager = new J2meApplicationManager();

        initializeComponents();
    }

    private void initializeComponents() {
        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: Applications
        ApplicationsPanel applicationsPanel = new ApplicationsPanel(j2meApplicationManager);
        tabbedPane.addTab("Applications", applicationsPanel);

        // Tab 2: Instances
        instancesPanel = new InstancesPanel(applicationConfig, j2meApplicationManager);
        instancesPanel.setSettingsDialogOpener(this::openSettingsDialog);
        emulatorInstanceManager = instancesPanel.emulatorInstanceManager;
        tabbedPane.addTab("Instances", instancesPanel);

        // Tab 3: Scripts
        JPanel scriptsPanel = new LuaScriptManager();
        tabbedPane.addTab("Scripts", scriptsPanel);

        add(tabbedPane);

        // Listen for application changes to update combo box in instances panel
        j2meApplicationManager.addApplicationChangeListener(new J2meApplicationManager.ApplicationChangeListener() {
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

    private void openSettingsDialog() {
        SettingsDialog settingsDialog = new SettingsDialog(this, applicationConfig);
        settingsDialog.setVisible(true);
        // Note: InstancesPanel will reload configuration after this returns
    }

    /**
     * Delegate method for compatibility - runs a single emulator instance
     */
    public void runSingleInstance(EmulatorInstance emulatorInstance) {
        instancesPanel.runSingleInstance(emulatorInstance);
    }

    /**
     * Delegate method for compatibility - adds an emulator instance tab
     */
    public void addEmulatorInstanceTab(EmulatorInstance emulatorInstance) {
        instancesPanel.addEmulatorInstanceTab(emulatorInstance);
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
