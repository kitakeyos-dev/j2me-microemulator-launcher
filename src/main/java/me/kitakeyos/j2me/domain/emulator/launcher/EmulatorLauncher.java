package me.kitakeyos.j2me.domain.emulator.launcher;

import me.kitakeyos.j2me.infrastructure.classloader.EmulatorClassLoader;
import me.kitakeyos.j2me.infrastructure.classloader.InstrumentedClassCache;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance.InstanceState;
import me.kitakeyos.j2me.util.reflection.EmulatorReflectionHelper;
import me.kitakeyos.j2me.util.reflection.ReflectionHelper;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Launches emulator instances
 * Refactored to separate concerns and improve maintainability
 */
public class EmulatorLauncher {

    private static final Logger logger = Logger.getLogger(EmulatorLauncher.class.getName());

    // Common classes to pre-load for better performance
    private static final String[] PRELOAD_CLASSES = {
            "org.microemu.app.Main",
            "org.microemu.app.Config",
            "org.microemu.app.Common",
            "org.microemu.device.DeviceFactory",
            "org.microemu.device.j2se.J2SEDevice",
            "org.microemu.device.j2se.J2SEDeviceDisplay",
            "org.microemu.device.j2se.J2SEInputMethod",
            "org.microemu.MIDletBridge",
            "org.microemu.DisplayAccess",
            "org.microemu.MicroEmulator"
    };

    /**
     * Pre-warm the emulator classloader by loading and caching common classes.
     * This should be called once at application startup to improve instance launch performance.
     *
     * @param microemulatorJarPath Path to the microemulator JAR file
     */
    public static void prewarmClassLoader(String microemulatorJarPath) {
        // Check if file exists first
        File microemulatorJar = new File(microemulatorJarPath);
        if (!microemulatorJar.exists()) {
            logger.warning("Cannot prewarm classloader: MicroEmulator JAR not found at " + microemulatorJarPath);
            return;
        }

        // Run in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                logger.info("Pre-warming emulator classloader...");

                // Create a temporary classloader with dummy instanceId = 0
                EmulatorClassLoader tempClassLoader = initializeEmulatorClassLoader(0, microemulatorJarPath);

                int successCount = 0;
                int failCount = 0;

                // Load each class to trigger instrumentation and caching
                for (String className : PRELOAD_CLASSES) {
                    try {
                        tempClassLoader.loadClass(className);
                        successCount++;
                        logger.fine("Pre-loaded class: " + className);
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        failCount++;
                        logger.fine("Could not pre-load class: " + className + " - " + e.getMessage());
                    }
                }

                long duration = System.currentTimeMillis() - startTime;
                logger.info(String.format(
                        "Classloader pre-warming completed in %d ms. Loaded: %d, Failed: %d. %s",
                        duration, successCount, failCount, InstrumentedClassCache.getStatistics()
                ));

            } catch (Exception e) {
                logger.warning("Failed to prewarm classloader: " + e.getMessage());
            }
        }, "ClassLoader-PreWarmer").start();
    }

    /**
     * Initialize an emulator classloader for the given instance
     *
     * @param instanceId           Instance ID
     * @param microemulatorJarPath Path to microemulator JAR
     * @return Initialized classloader
     */
    public static EmulatorClassLoader initializeEmulatorClassLoader(int instanceId, String microemulatorJarPath)
            throws IOException {
        List<URL> urls = new ArrayList<>();
        File microemulatorJar = new File(microemulatorJarPath);

        if (microemulatorJar.exists()) {
            urls.add(microemulatorJar.toURI().toURL());
        }

        if (urls.isEmpty()) {
            throw new IOException("No JAR files found in " + microemulatorJar);
        }

        return new EmulatorClassLoader(instanceId, urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
    }

    /**
     * Start an emulator instance
     *
     * @param instance   The instance to start
     * @param onComplete Callback when instance startup completes (success or failure)
     */
    public static void startEmulatorInstance(EmulatorInstance instance, Runnable onComplete) {
        // Set state to STARTING
        instance.setState(InstanceState.STARTING);

        long instanceStartTime = System.currentTimeMillis();

        try {
            EmulatorClassLoader emulatorClassLoader = initializeEmulatorClassLoader(
                    instance.getInstanceId(),
                    instance.getMicroemulatorPath()
            );

            // Ensure emulator runs with its own context ClassLoader
            Thread.currentThread().setContextClassLoader(emulatorClassLoader);

            // Build parameters
            List<String> params = buildEmulatorParameters(instance);

            // Launch the emulator
            JFrame frame = launchMicroEmulator(params, emulatorClassLoader);

            // Extract and store UI components
            configureInstanceComponents(instance, frame, emulatorClassLoader);

            // Set state to RUNNING after successful configuration
            instance.setState(InstanceState.RUNNING);

            long instanceDuration = System.currentTimeMillis() - instanceStartTime;
            logger.info(String.format("Instance #%d started in %d ms", instance.getInstanceId(), instanceDuration));
            logger.info(emulatorClassLoader.getStatistics());
            logger.info("Global " + InstrumentedClassCache.getStatistics());

        } catch (Exception e) {
            instance.setErrorMessage(e.getMessage());
            instance.setState(InstanceState.STOPPED);
            logger.severe("Failed to start instance #" + instance.getInstanceId() + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (onComplete != null) {
                SwingUtilities.invokeLater(onComplete);
            }
        }
    }

    /**
     * Build emulator launch parameters
     */
    private static List<String> buildEmulatorParameters(EmulatorInstance instance) {
        List<String> params = new ArrayList<>();
        params.add(instance.getJ2meFilePath());
        params.add("--resizableDevice");
        params.add(String.valueOf(instance.getDisplayWidth()));//Width
        params.add(String.valueOf(instance.getDisplayHeight()));//Height
        return params;
    }

    /**
     * Configure instance components after successful launch
     */
    private static void configureInstanceComponents(EmulatorInstance instance, JFrame frame, EmulatorClassLoader emulatorClassLoader) throws Exception {
        ActionListener exitListener = ReflectionHelper.getFieldValue(frame, "menuExitListener", ActionListener.class);
        JPanel devicePanel = ReflectionHelper.getFieldValue(frame, "devicePanel", JPanel.class);

        instance.setMenuExitListener(exitListener);

        // Set emulator display based on display mode
        if (instance.isFullDisplayMode()) {
            instance.setEmulatorDisplay(frame.getRootPane());
        } else {
            instance.setEmulatorDisplay(devicePanel);
        }
        instance.setDevicePanel(devicePanel);

        Class<?> mIDletResourceLoader = ReflectionHelper.loadClass(emulatorClassLoader, "org.microemu.app.util.MIDletResourceLoader");
        ClassLoader classLoader = (ClassLoader) ReflectionHelper.getStaticFieldValue(mIDletResourceLoader, "classLoader");
        instance.setAppClassLoader(classLoader);

        frame.setResizable(false);
        frame.setTitle("Instance " + instance.getInstanceId());
    }

    /**
     * Launch MicroEmulator using reflection
     * Refactored to use EmulatorReflectionHelper for cleaner code
     *
     * @param params      Launch parameters
     * @param classLoader ClassLoader to use
     * @return Configured JFrame
     */
    public static JFrame launchMicroEmulator(List<String> params, ClassLoader classLoader)
            throws Exception {

        // Create instance of Main class
        JFrame app = (JFrame) ReflectionHelper.createInstance(classLoader, "org.microemu.app.Main");

        // Initialize parameters
        Object deviceEntry = EmulatorReflectionHelper.initializeEmulatorParams(app, params, classLoader);

        // Configure display size
        EmulatorReflectionHelper.configureDisplaySize(deviceEntry, classLoader);

        // Update device
        EmulatorReflectionHelper.updateDevice(app);

        // Validate frame
        app.validate();

        // Get common object for MIDlet initialization
        Object common = ReflectionHelper.getFieldValue(app, "common");

        // Initialize MIDlet
        EmulatorReflectionHelper.initializeMIDlet(common);

        // Setup component listener
        EmulatorReflectionHelper.setupComponentListener(app);

        // Notify state changed
        EmulatorReflectionHelper.notifyStateChanged(app);

        return app;
    }
}
