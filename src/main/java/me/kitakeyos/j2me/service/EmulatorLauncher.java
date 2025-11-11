package me.kitakeyos.j2me.service;

import me.kitakeyos.j2me.MainApplication;
import me.kitakeyos.j2me.core.classloader.EmulatorClassLoader;
import me.kitakeyos.j2me.core.classloader.InstrumentedClassCache;
import me.kitakeyos.j2me.model.EmulatorInstance;
import me.kitakeyos.j2me.model.EmulatorInstance.InstanceState;
import me.kitakeyos.j2me.util.ReflectionHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Launches emulator instances
 */
public class EmulatorLauncher {

    private static final Logger logger = Logger.getLogger(EmulatorLauncher.class.getName());

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

                // List of important classes to pre-load and cache
                String[] classesToPreload = {
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

                int successCount = 0;
                int failCount = 0;

                // Load each class to trigger instrumentation and caching
                for (String className : classesToPreload) {
                    try {
                        Class<?> clazz = tempClassLoader.loadClass(className);
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

    public static EmulatorClassLoader initializeEmulatorClassLoader(int instanceId, String microemulatorJarPath) throws IOException {
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

    public static void startEmulatorInstance(EmulatorInstance instance, Runnable onComplete, Runnable onStarted) {
        // Set state to STARTING
        instance.state = InstanceState.STARTING;
        if (onStarted != null) {
            SwingUtilities.invokeLater(onStarted);
        }

        long instanceStartTime = System.currentTimeMillis();
        EmulatorClassLoader emulatorClassLoader = null;

        try {
            emulatorClassLoader = initializeEmulatorClassLoader(
                    instance.instanceId,
                    instance.microemulatorPath
            );

            // Ensure emulator runs with its own context ClassLoader
            Thread.currentThread().setContextClassLoader(emulatorClassLoader);

            List<String> params = new ArrayList<>();
            params.add(instance.j2meFilePath);
            params.add("--resizableDevice");
            params.add("240");
            params.add("320");
            JFrame frame = launchMicroEmulator(instance, params, emulatorClassLoader);
            instance.menuExitListener = ReflectionHelper.getFieldValue(frame, "menuExitListener", ActionListener.class);
            instance.emulatorDisplay = ReflectionHelper.getFieldValue(frame, "devicePanel", JPanel.class);
            frame.setResizable(false);
            frame.setTitle("Instance " + instance.instanceId);
            // Set state to RUNNING after successful configuration
            instance.state = InstanceState.RUNNING;

            long instanceDuration = System.currentTimeMillis() - instanceStartTime;
            logger.info(String.format("Instance #%d started in %d ms", instance.instanceId, instanceDuration));
            logger.info(emulatorClassLoader.getStatistics());
            logger.info("Global " + InstrumentedClassCache.getStatistics());
        } catch (Exception e) {
            instance.errorMessage = e.getMessage();
            instance.state = InstanceState.STOPPED;
            e.printStackTrace();
        } finally {
            if (onComplete != null) {
                SwingUtilities.invokeLater(onComplete);
            }
        }
    }

    public static JFrame launchMicroEmulator(EmulatorInstance instance, List<String> params, ClassLoader classLoader)
            throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {

        // Create instance of Main class
        JFrame app = (JFrame) ReflectionHelper.createInstance(classLoader, "org.microemu.app.Main");

        // Get required fields
        Object common = ReflectionHelper.getFieldValue(app, "common");
        Object selectDevicePanel = ReflectionHelper.getFieldValue(app, "selectDevicePanel");

        // Get selected device entry
        Object deviceEntry = ReflectionHelper.invokeMethod(selectDevicePanel, "getSelectedDeviceEntry");

        // Call common.initParams(params, deviceEntry, J2SEDevice.class)
        Class<?> j2seDeviceClass = ReflectionHelper.loadClass(classLoader, "org.microemu.device.j2se.J2SEDevice");
        boolean initResult = (boolean) ReflectionHelper.invokeMethod(
                common,
                "initParams",
                new Class<?>[]{List.class, deviceEntry.getClass(), Class.class},
                params, deviceEntry, j2seDeviceClass
        );

        if (initResult) {
            // Set deviceEntry field
            ReflectionHelper.setFieldValue(app, "deviceEntry", deviceEntry);

            // Get DeviceDisplayImpl
            Class<?> deviceFactoryClass = ReflectionHelper.loadClass(classLoader, "org.microemu.device.DeviceFactory");
            Object device = ReflectionHelper.invokeStaticMethod(deviceFactoryClass, "getDevice", new Class<?>[]{});
            Object deviceDisplay = ReflectionHelper.invokeMethod(device, "getDeviceDisplay");

            // Check if resizable
            boolean isResizable = (boolean) ReflectionHelper.invokeMethod(deviceDisplay, "isResizable");

            if (isResizable) {
                // Get device entry display size
                Class<?> configClass = ReflectionHelper.loadClass(classLoader, "org.microemu.app.Config");
                Object size = ReflectionHelper.invokeStaticMethod(
                        configClass,
                        "getDeviceEntryDisplaySize",
                        new Class<?>[]{Object.class},
                        deviceEntry
                );

                if (size != null) {
                    // Set display rectangle
                    ReflectionHelper.invokeMethod(
                            deviceDisplay,
                            "setDisplayRectangle",
                            new Class<?>[]{Rectangle.class},
                            size
                    );
                }
            }
        }

        // Update device
        ReflectionHelper.invokeDeclaredMethod(app, "updateDevice");

        // Validate frame
        ReflectionHelper.invokeMethod(app, "validate");

        // Don't show the frame window - the display panel will be added to a tab instead
        // ReflectionHelper.invokeMethod(app, "setVisible", new Class<?>[]{boolean.class}, true);
        // app.addWindowListener(new WindowAdapter() {
        //     @Override
        //     public void windowClosed(WindowEvent e) {
        //         MainApplication.INSTANCE.stopEmulatorInstance(instance);
        //     }
        // });

        // Initialize MIDlet
        ReflectionHelper.invokeMethod(common, "initMIDlet", new Class<?>[]{boolean.class}, true);

        // Add component listener
        Object componentListener = ReflectionHelper.getFieldValue(app, "componentListener");
        ReflectionHelper.invokeMethod(
                app,
                "addComponentListener",
                new Class<?>[]{ComponentListener.class},
                componentListener
        );

        // Notify state changed
        Object responseInterfaceListener = ReflectionHelper.getFieldValue(app, "responseInterfaceListener");
        ReflectionHelper.invokeDeclaredMethod(
                responseInterfaceListener,
                "stateChanged",
                new Class<?>[]{boolean.class},
                true
        );

        return app;
    }
}