package me.kitakeyos.j2me.infrastructure.resource;

import me.kitakeyos.j2me.domain.application.model.J2meApplication;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.UUID;

/**
 * Utility class to read manifest information from JAR/JAD files
 */
public class ManifestReader {

    /**
     * Read application information from a JAR or JAD file
     */
    public static J2meApplication readApplicationInfo(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new FileNotFoundException("File not found: " + file);
        }

        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".jar")) {
            return readFromJar(file);
        } else if (fileName.endsWith(".jad")) {
            return readFromJad(file);
        } else {
            throw new IllegalArgumentException("Unsupported file type. Only JAR and JAD files are supported.");
        }
    }

    /**
     * Read application info from JAR file
     */
    private static J2meApplication readFromJar(File jarFile) throws IOException {
        J2meApplication app = new J2meApplication();
        app.setId(UUID.randomUUID().toString());
        app.setFilePath(jarFile.getAbsolutePath());
        app.setFileSize(jarFile.length());

        try (JarFile jar = new JarFile(jarFile)) {
            Manifest manifest = jar.getManifest();
            if (manifest != null) {
                Attributes attrs = manifest.getMainAttributes();

                // Read MIDlet name
                String midletName = attrs.getValue("MIDlet-Name");
                if (midletName == null || midletName.trim().isEmpty()) {
                    midletName = jarFile.getName().replaceAll("\\.(jar|JAR)$", "");
                }
                app.setName(midletName);

                // Read vendor
                String vendor = attrs.getValue("MIDlet-Vendor");
                app.setVendor(vendor);

                // Read version
                String version = attrs.getValue("MIDlet-Version");
                app.setVersion(version);

                // Read icon
                String midlet1 = attrs.getValue("MIDlet-1");
                if (midlet1 != null) {
                    String iconName = extractIconName(midlet1);
                    if (iconName != null && !iconName.isEmpty()) {
                        Image icon = loadIconFromJar(jar, iconName);
                        if (icon != null) {
                            app.setIcon(icon);
                            app.setIconPath(iconName);
                        }
                    }
                }
            } else {
                // No manifest, use filename
                app.setName(jarFile.getName().replaceAll("\\.(jar|JAR)$", ""));
            }
        }

        return app;
    }

    /**
     * Read application info from JAD file
     */
    private static J2meApplication readFromJad(File jadFile) throws IOException {
        J2meApplication app = new J2meApplication();
        app.setId(UUID.randomUUID().toString());
        app.setFilePath(jadFile.getAbsolutePath());
        app.setFileSize(jadFile.length());

        try (BufferedReader reader = new BufferedReader(new FileReader(jadFile))) {
            String line;
            String midletName = null;
            String vendor = null;
            String version = null;
            String iconName = null;
            String jarUrl = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    switch (key) {
                        case "MIDlet-Name":
                            midletName = value;
                            break;
                        case "MIDlet-Vendor":
                            vendor = value;
                            break;
                        case "MIDlet-Version":
                            version = value;
                            break;
                        case "MIDlet-1":
                            iconName = extractIconName(value);
                            break;
                        case "MIDlet-Jar-URL":
                            jarUrl = value;
                            break;
                    }
                }
            }

            if (midletName == null || midletName.isEmpty()) {
                midletName = jadFile.getName().replaceAll("\\.(jad|JAD)$", "");
            }
            app.setName(midletName);
            app.setVendor(vendor);
            app.setVersion(version);

            // Try to load icon from associated JAR file
            if (iconName != null && jarUrl != null) {
                File jarFile = new File(jadFile.getParent(), jarUrl);
                if (jarFile.exists()) {
                    try (JarFile jar = new JarFile(jarFile)) {
                        Image icon = loadIconFromJar(jar, iconName);
                        if (icon != null) {
                            app.setIcon(icon);
                            app.setIconPath(iconName);
                        }
                    } catch (IOException e) {
                        // Ignore icon loading errors
                    }
                }
            }
        }

        return app;
    }

    /**
     * Extract icon name from MIDlet-1 attribute
     * Format: name, icon, class
     * Example: "MyApp, /icon.png, com.example.MyApp"
     */
    private static String extractIconName(String midlet1) {
        if (midlet1 == null) {
            return null;
        }

        String[] parts = midlet1.split(",");
        if (parts.length >= 2) {
            String iconPath = parts[1].trim();
            if (!iconPath.isEmpty() && !iconPath.equals("null")) {
                // Remove leading slash if present
                if (iconPath.startsWith("/")) {
                    iconPath = iconPath.substring(1);
                }
                return iconPath;
            }
        }
        return null;
    }

    /**
     * Load icon image from JAR file
     */
    private static Image loadIconFromJar(JarFile jar, String iconPath) {
        try {
            JarEntry entry = jar.getJarEntry(iconPath);
            if (entry == null) {
                // Try with leading slash
                entry = jar.getJarEntry("/" + iconPath);
            }
            if (entry == null) {
                return null;
            }

            try (InputStream is = jar.getInputStream(entry)) {
                return ImageIO.read(is);
            }
        } catch (IOException e) {
            // Ignore icon loading errors
            return null;
        }
    }

    /**
     * Check if a file is a valid J2ME application file
     */
    public static boolean isValidJ2meFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }

        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".jar") || fileName.endsWith(".jad");
    }
}
