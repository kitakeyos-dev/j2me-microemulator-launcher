package me.kitakeyos.j2me.service;

import me.kitakeyos.j2me.core.classloader.EmulatorClassLoader;
import me.kitakeyos.j2me.model.EmulatorInstance;
import me.kitakeyos.j2me.model.EmulatorInstance.InstanceState;
import me.kitakeyos.j2me.util.ReflectionHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Launches emulator instances
 */
public class EmulatorLauncher {

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

        try {
            EmulatorClassLoader emulatorClassLoader = initializeEmulatorClassLoader(
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
            instance.emulatorWindow = frame;
            instance.menuExitListener = ReflectionHelper.getFieldValue(frame, "menuExitListener", ActionListener.class);
            instance.emulatorDisplay = ReflectionHelper.getFieldValue(frame, "devicePanel", JPanel.class);
            frame.setResizable(false);
            frame.setTitle("Instance " + instance.instanceId);
            // Set state to RUNNING after successful configuration
            instance.state = InstanceState.RUNNING;
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