package me.kitakeyos.j2me.util.reflection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentListener;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Helper class for reflection operations specific to MicroEmulator
 * Encapsulates complex reflection logic for launching emulator instances
 */
public class EmulatorReflectionHelper {

    /**
     * Initialize parameters for the MicroEmulator
     *
     * @param app         The Main application instance
     * @param params      Parameters list
     * @param classLoader ClassLoader to use
     * @return Device entry object
     */
    public static Object initializeEmulatorParams(JFrame app, List<String> params, ClassLoader classLoader)
            throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {

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

        // Only set deviceEntry if initialization succeeded
        if (initResult) {
            // Set deviceEntry field
            ReflectionHelper.setFieldValue(app, "deviceEntry", deviceEntry);
        }

        return deviceEntry;
    }

    /**
     * Configure display size if the device is resizable
     *
     * @param deviceEntry Device entry object
     * @param classLoader ClassLoader to use
     */
    public static void configureDisplaySize(Object deviceEntry, ClassLoader classLoader)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

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
                    new Class<?>[]{deviceEntry.getClass()},
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

    /**
     * Initialize MIDlet with the given common object
     *
     * @param common Common object from Main application
     */
    public static void initializeMIDlet(Object common)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ReflectionHelper.invokeMethod(common, "initMIDlet", new Class<?>[]{boolean.class}, true);
    }

    /**
     * Setup component listener for the application frame
     *
     * @param app The Main application JFrame
     */
    public static void setupComponentListener(JFrame app)
            throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object componentListener = ReflectionHelper.getFieldValue(app, "componentListener");
        ReflectionHelper.invokeMethod(
                app,
                "addComponentListener",
                new Class<?>[]{ComponentListener.class},
                componentListener
        );
    }

    /**
     * Notify state changed through response interface listener
     *
     * @param app The Main application JFrame
     */
    public static void notifyStateChanged(JFrame app)
            throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object responseInterfaceListener = ReflectionHelper.getFieldValue(app, "responseInterfaceListener");
        ReflectionHelper.invokeDeclaredMethod(
                responseInterfaceListener,
                "stateChanged",
                new Class<?>[]{boolean.class},
                true
        );
    }

    /**
     * Update device display
     *
     * @param app The Main application JFrame
     */
    public static void updateDevice(JFrame app)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ReflectionHelper.invokeDeclaredMethod(app, "updateDevice");
    }
}
