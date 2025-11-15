package me.kitakeyos.j2me.util;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.prefs.Preferences;

/**
 * Helper class for JFileChooser with persistent directory preferences
 */
public class FileChooserHelper {
    private static final String PREF_LAST_APP_DIR = "lastApplicationDirectory";
    private static final String PREF_LAST_EMULATOR_DIR = "lastEmulatorDirectory";
    private static final Preferences prefs = Preferences.userNodeForPackage(FileChooserHelper.class);

    /**
     * Create a file chooser for J2ME application files (JAR/JAD)
     * with the last used directory
     */
    public static JFileChooser createApplicationFileChooser() {
        String lastDir = prefs.get(PREF_LAST_APP_DIR, System.getProperty("user.home"));
        JFileChooser fileChooser = new JFileChooser(lastDir);

        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "J2ME Files (JAR, JAD)", "jar", "jad"
        );
        fileChooser.setFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false);

        return fileChooser;
    }

    /**
     * Create a file chooser for MicroEmulator JAR file
     * with the last used directory
     */
    public static JFileChooser createEmulatorFileChooser() {
        String lastDir = prefs.get(PREF_LAST_EMULATOR_DIR, System.getProperty("user.home"));
        JFileChooser fileChooser = new JFileChooser(lastDir);

        fileChooser.setDialogTitle("Select MicroEmulator File");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("JAR Files (*.jar)", "jar");
        fileChooser.setFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false);

        return fileChooser;
    }

    /**
     * Save the directory of application file selection
     */
    public static void saveApplicationDirectory(File file) {
        if (file != null && file.getParent() != null) {
            prefs.put(PREF_LAST_APP_DIR, file.getParent());
        }
    }

    /**
     * Save the directory of emulator file selection
     */
    public static void saveEmulatorDirectory(File file) {
        if (file != null && file.getParent() != null) {
            prefs.put(PREF_LAST_EMULATOR_DIR, file.getParent());
        }
    }

    /**
     * Get the last used application directory
     */
    public static String getLastApplicationDirectory() {
        return prefs.get(PREF_LAST_APP_DIR, System.getProperty("user.home"));
    }

    /**
     * Get the last used emulator directory
     */
    public static String getLastEmulatorDirectory() {
        return prefs.get(PREF_LAST_EMULATOR_DIR, System.getProperty("user.home"));
    }
}
