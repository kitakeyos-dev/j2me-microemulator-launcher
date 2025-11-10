package me.kitakeyos.j2me.launcher;

import me.kitakeyos.j2me.MainApplication;
import me.kitakeyos.j2me.classloader.EmulatorClassLoader;
import me.kitakeyos.j2me.config.EmulatorInstance;
import me.kitakeyos.j2me.config.EmulatorInstance.InstanceState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.io.File;
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
            Class<?> microemulatorMainClass = emulatorClassLoader.loadClass("org.microemu.app.Main");
            Method mainMethod = microemulatorMainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[]{
                    emulatorInstance.j2meFilePath,
                    "--resizableDevice",
                    "240",
                    "320"
            });

            Window[] windows = JFrame.getWindows();
            for (Window window : windows) {
                if (window instanceof JFrame) {
                    JFrame frame = (JFrame) window;
                    if (frame.getClass().getClassLoader() == emulatorClassLoader) {
                        configure(emulatorInstance, frame);
                        // Set state to RUNNING after successful configuration
                        emulatorInstance.state = InstanceState.RUNNING;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            emulatorInstance.errorMessage = e.getMessage();
            emulatorInstance.state = InstanceState.STOPPED;
            e.printStackTrace();

            // Show error notification to user
            SwingUtilities.invokeLater(() -> {
                String errorMessage = "Failed to launch J2ME emulator for instance " + emulatorInstance.instanceId + ":\n" +
                                     (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                JOptionPane.showMessageDialog(
                    null,
                    errorMessage,
                    "Emulator Launch Error",
                    JOptionPane.ERROR_MESSAGE
                );
            });
        } finally {
            if (onComplete != null) {
                SwingUtilities.invokeLater(onComplete);
            }
        }
    }

    private static void configure(EmulatorInstance instance, JFrame frame) {
        instance.emulatorWindow = frame;
        frame.setResizable(false);
        instance.menuExitListener = ReflectionHelper.getFieldValue(frame, "menuExitListener", ActionListener.class);
        frame.setTitle("Instance " + instance.instanceId);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                MainApplication.INSTANCE.stopEmulatorInstance(instance);
            }
        });
    }
}