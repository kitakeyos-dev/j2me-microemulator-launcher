package me.kitakeyos.j2me.launcher;

import me.kitakeyos.j2me.classloader.EmulatorClassLoader;
import me.kitakeyos.j2me.config.EmulatorInstance;
import me.kitakeyos.j2me.config.EmulatorInstance.InstanceState;
import me.kitakeyos.j2me.ui.MessageDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

    public static void startEmulatorInstance(EmulatorInstance emulatorInstance, Runnable onComplete, Runnable onStarted) {
        // Set state to STARTING
        emulatorInstance.state = InstanceState.STARTING;
        if (onStarted != null) {
            SwingUtilities.invokeLater(onStarted);
        }

        try {
            EmulatorClassLoader emulatorClassLoader = initializeEmulatorClassLoader(
                    emulatorInstance.instanceId,
                    emulatorInstance.microemulatorPath
            );

            // Ensure emulator runs with its own context ClassLoader
            Thread.currentThread().setContextClassLoader(emulatorClassLoader);

            List<String> params = new ArrayList<>();
            params.add(emulatorInstance.j2meFilePath);
            params.add("--resizableDevice");
            params.add("240");
            params.add("320");
            JFrame frame = launchMicroEmulator(params, emulatorClassLoader);
            if (frame == null) {
                // Hiện thông báo lỗi không thể khởi động
                return;
            }
            configure(emulatorInstance, frame);
            // Set state to RUNNING after successful configuration
            emulatorInstance.state = InstanceState.RUNNING;
        } catch (Exception e) {
            emulatorInstance.errorMessage = e.getMessage();
            emulatorInstance.state = InstanceState.STOPPED;
            e.printStackTrace();
        } finally {
            if (onComplete != null) {
                SwingUtilities.invokeLater(onComplete);
            }
        }
    }

    private static void configure(EmulatorInstance instance, JFrame frame) {
        instance.emulatorWindow = frame;
        frame.setResizable(false);
        instance.menuExitListener = getFieldValue(frame, "menuExitListener", ActionListener.class);
        instance.emulatorDisplay = getFieldValue(frame, "devicePanel", JPanel.class);
        frame.setTitle("Instance " + instance.instanceId);
    }

    public static <T> T getFieldValue(Object obj, String fieldName, Class<T> type) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return type.cast(field.get(obj));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    public static JFrame launchMicroEmulator(List<String> params, ClassLoader classLoader) {
        try {
            // Tạo instance của Main class
            JFrame app = (JFrame) classLoader.loadClass("org.microemu.app.Main").getDeclaredConstructor().newInstance();

            // Lấy các field cần thiết
            Field commonField = app.getClass().getDeclaredField("common");
            commonField.setAccessible(true);
            Object common = commonField.get(app);

            Field selectDevicePanelField = app.getClass().getDeclaredField("selectDevicePanel");
            selectDevicePanelField.setAccessible(true);
            Object selectDevicePanel = selectDevicePanelField.get(app);

            // Gọi getSelectedDeviceEntry()
            Method getSelectedDeviceEntryMethod = selectDevicePanel.getClass().getMethod("getSelectedDeviceEntry");
            Object deviceEntry = getSelectedDeviceEntryMethod.invoke(selectDevicePanel);

            // Gọi common.initParams(params, deviceEntry, J2SEDevice.class)
            Class<?> j2seDeviceClass = classLoader.loadClass("org.microemu.device.j2se.J2SEDevice");
            Method initParamsMethod = common.getClass().getMethod("initParams", List.class, deviceEntry.getClass(), Class.class);
            boolean initResult = (boolean) initParamsMethod.invoke(common, params, deviceEntry, j2seDeviceClass);

            if (initResult) {
                // Set deviceEntry field
                Field deviceEntryField = app.getClass().getDeclaredField("deviceEntry");
                deviceEntryField.setAccessible(true);
                deviceEntryField.set(app, deviceEntry);

                // Lấy DeviceDisplayImpl
                Class<?> deviceFactoryClass = classLoader.loadClass("org.microemu.device.DeviceFactory");
                Method getDeviceMethod = deviceFactoryClass.getMethod("getDevice");
                Object device = getDeviceMethod.invoke(null);

                Method getDeviceDisplayMethod = device.getClass().getMethod("getDeviceDisplay");
                Object deviceDisplay = getDeviceDisplayMethod.invoke(device);

                // Kiểm tra isResizable()
                Method isResizableMethod = deviceDisplay.getClass().getMethod("isResizable");
                boolean isResizable = (boolean) isResizableMethod.invoke(deviceDisplay);

                if (isResizable) {
                    // Gọi Config.getDeviceEntryDisplaySize(deviceEntry)
                    Class<?> configClass = classLoader.loadClass("org.microemu.app.Config");
                    Method getDeviceEntryDisplaySizeMethod = configClass.getMethod("getDeviceEntryDisplaySize", Object.class);
                    Object size = getDeviceEntryDisplaySizeMethod.invoke(null, deviceEntry);

                    if (size != null) {
                        // Gọi deviceDisplay.setDisplayRectangle(size)
                        Method setDisplayRectangleMethod = deviceDisplay.getClass().getMethod("setDisplayRectangle", Rectangle.class);
                        setDisplayRectangleMethod.invoke(deviceDisplay, size);
                    }
                }
            }

            // Gọi app.updateDevice()
            Method updateDeviceMethod = app.getClass().getDeclaredMethod("updateDevice");
            updateDeviceMethod.setAccessible(true);
            updateDeviceMethod.invoke(app);

            // Gọi app.validate()
            Method validateMethod = app.getClass().getMethod("validate");
            validateMethod.invoke(app);

//            // Gọi app.setVisible(true)
//            Method setVisibleMethod = app.getClass().getMethod("setVisible", boolean.class);
//            setVisibleMethod.invoke(app, true);

            // Gọi app.common.initMIDlet(false)
            Method initMIDletMethod = common.getClass().getMethod("initMIDlet", boolean.class);
            initMIDletMethod.invoke(common, true);

            // Lấy componentListener và add vào app
            Field componentListenerField = app.getClass().getDeclaredField("componentListener");
            componentListenerField.setAccessible(true);
            Object componentListener = componentListenerField.get(app);

            Method addComponentListenerMethod = app.getClass().getMethod("addComponentListener", ComponentListener.class);
            addComponentListenerMethod.invoke(app, componentListener);

            // Gọi responseInterfaceListener.stateChanged(true)
            Field responseInterfaceListenerField = app.getClass().getDeclaredField("responseInterfaceListener");
            responseInterfaceListenerField.setAccessible(true);
            Object responseInterfaceListener = responseInterfaceListenerField.get(app);

            Method stateChangedMethod = responseInterfaceListener.getClass().getMethod("stateChanged", boolean.class);
            stateChangedMethod.setAccessible(true);
            stateChangedMethod.invoke(responseInterfaceListener, true);
            return app;
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            System.err.println("Method not found: " + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            System.err.println("Field not found: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            System.err.println("Illegal access: " + e.getMessage());
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.err.println("Instantiation error: " + e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            System.err.println("Invocation target exception: " + e.getMessage());
            e.printStackTrace();
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
                e.getCause().printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}