package me.kitakeyos.j2me.domain.application.service;

import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.domain.application.model.J2meApplication;
import me.kitakeyos.j2me.infrastructure.resource.ManifestReader;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.*;
import java.util.*;

/**
 * Manager for installed J2ME applications
 */
public class ApplicationService {

    private final List<J2meApplication> applications;
    private final File dataDirectory;
    private final File appsDirectory;
    private final File iconsDirectory;
    private final File configFile;
    private final List<ApplicationChangeListener> listeners;

    public ApplicationService() {
        this.applications = new ArrayList<>();
        this.listeners = new ArrayList<>();

        // Use application directory instead of user home
        this.dataDirectory = new File(ApplicationConfig.DATA_DIR);
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }

        // Create apps directory for storing cloned JAR/JAD files
        this.appsDirectory = new File(dataDirectory, ApplicationConfig.APPS_DIR);
        if (!appsDirectory.exists()) {
            appsDirectory.mkdirs();
        }

        // Create icons directory
        this.iconsDirectory = new File(dataDirectory, ApplicationConfig.ICONS_DIR);
        if (!iconsDirectory.exists()) {
            iconsDirectory.mkdirs();
        }

        this.configFile = new File(dataDirectory, ApplicationConfig.APPS_CONFIG_FILE);

        loadApplications();
    }

    /**
     * Add a new application from file
     */
    public J2meApplication addApplication(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new FileNotFoundException("File not found: " + file);
        }

        // Read application info from original file
        J2meApplication app = ManifestReader.readApplicationInfo(file);

        // Check if already installed (by file name and size)
        String fileName = file.getName();
        long fileSize = file.length();
        Optional<J2meApplication> existing = applications.stream()
                .filter(a -> {
                    File existingFile = new File(a.getFilePath());
                    return existingFile.getName().equals(fileName) && a.getFileSize() == fileSize;
                })
                .findFirst();

        if (existing.isPresent()) {
            throw new IllegalArgumentException("Application already installed: " + existing.get().getName());
        }

        // Copy/clone the JAR/JAD file to apps directory
        String fileExtension = fileName.substring(fileName.lastIndexOf('.'));
        String clonedFileName = app.getId() + fileExtension;
        File clonedFile = new File(appsDirectory, clonedFileName);

        try {
            copyFile(file, clonedFile);
            app.setFilePath(clonedFile.getAbsolutePath());
        } catch (IOException e) {
            throw new IOException("Failed to copy application file: " + e.getMessage(), e);
        }

        // Save icon to disk if available
        if (app.getIcon() != null) {
            String iconFileName = app.getId() + ".png";
            File iconFile = new File(iconsDirectory, iconFileName);
            try {
                ImageIO.write(toBufferedImage(app.getIcon()), "png", iconFile);
                app.setIconPath(iconFile.getAbsolutePath());
            } catch (IOException e) {
                // Ignore icon save errors
                app.setIconPath(null);
            }
        }

        applications.add(app);
        saveApplications();
        notifyApplicationAdded(app);

        return app;
    }

    /**
     * Copy file from source to destination
     */
    private void copyFile(File source, File dest) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    /**
     * Remove an application
     */
    public boolean removeApplication(String appId) {
        Optional<J2meApplication> app = applications.stream()
                .filter(a -> a.getId().equals(appId))
                .findFirst();

        if (app.isPresent()) {
            J2meApplication application = app.get();

            // Delete cloned JAR/JAD file
            File appFile = new File(application.getFilePath());
            if (appFile.exists()) {
                appFile.delete();
            }

            // Delete icon file
            if (application.getIconPath() != null) {
                File iconFile = new File(application.getIconPath());
                if (iconFile.exists()) {
                    iconFile.delete();
                }
            }

            applications.remove(application);
            saveApplications();
            notifyApplicationRemoved(application);
            return true;
        }

        return false;
    }

    /**
     * Get all installed applications
     */
    public List<J2meApplication> getApplications() {
        return new ArrayList<>(applications);
    }

    /**
     * Get application by ID
     */
    public J2meApplication getApplicationById(String id) {
        return applications.stream()
                .filter(app -> app.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get application count
     */
    public int getApplicationCount() {
        return applications.size();
    }

    /**
     * Save applications to config file
     */
    private void saveApplications() {
        Properties props = new Properties();

        // Save application count
        props.setProperty("app.count", String.valueOf(applications.size()));

        // Save each application
        for (int i = 0; i < applications.size(); i++) {
            J2meApplication app = applications.get(i);
            String prefix = "app." + i + ".";

            props.setProperty(prefix + "id", app.getId());
            props.setProperty(prefix + "name", app.getName());

            // Save relative path instead of absolute path
            props.setProperty(prefix + "filePath", getRelativePath(app.getFilePath()));

            if (app.getVendor() != null) {
                props.setProperty(prefix + "vendor", app.getVendor());
            }
            if (app.getVersion() != null) {
                props.setProperty(prefix + "version", app.getVersion());
            }
            if (app.getIconPath() != null) {
                // Save relative path for icon as well
                props.setProperty(prefix + "iconPath", getRelativePath(app.getIconPath()));
            }

            props.setProperty(prefix + "installedDate", String.valueOf(app.getInstalledDate()));
            props.setProperty(prefix + "fileSize", String.valueOf(app.getFileSize()));
        }

        // Write to file
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "J2ME Applications Configuration");
        } catch (IOException e) {
            System.err.println("Error saving applications: " + e.getMessage());
        }
    }

    /**
     * Load applications from config file
     */
    private void loadApplications() {
        if (!configFile.exists()) {
            return;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);

            int count = Integer.parseInt(props.getProperty("app.count", "0"));

            for (int i = 0; i < count; i++) {
                String prefix = "app." + i + ".";

                String id = props.getProperty(prefix + "id");
                String name = props.getProperty(prefix + "name");
                String relativeFilePath = props.getProperty(prefix + "filePath");

                if (id == null || name == null || relativeFilePath == null) {
                    continue;
                }

                // Convert relative path to absolute path
                String absoluteFilePath = getAbsolutePath(relativeFilePath);
                File file = new File(absoluteFilePath);
                if (!file.exists()) {
                    continue;
                }

                J2meApplication app = new J2meApplication(id, name, absoluteFilePath);
                app.setVendor(props.getProperty(prefix + "vendor"));
                app.setVersion(props.getProperty(prefix + "version"));

                String relativeIconPath = props.getProperty(prefix + "iconPath");
                if (relativeIconPath != null) {
                    // Convert relative path to absolute path
                    String absoluteIconPath = getAbsolutePath(relativeIconPath);
                    app.setIconPath(absoluteIconPath);
                    // Load icon if it exists
                    File iconFile = new File(absoluteIconPath);
                    if (iconFile.exists()) {
                        try {
                            Image icon = ImageIO.read(iconFile);
                            app.setIcon(icon);
                        } catch (IOException e) {
                            // Ignore icon loading errors
                        }
                    }
                }

                // Update file size if not set
                File appFile = new File(absoluteFilePath);
                if (appFile.exists() && app.getFileSize() == 0) {
                    app.setFileSize(appFile.length());
                }

                try {
                    app.setInstalledDate(Long.parseLong(props.getProperty(prefix + "installedDate", "0")));
                    app.setFileSize(Long.parseLong(props.getProperty(prefix + "fileSize", "0")));
                } catch (NumberFormatException e) {
                    // Ignore parsing errors
                }

                applications.add(app);
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading applications: " + e.getMessage());
        }
    }

    /**
     * Convert Image to BufferedImage
     */
    private static java.awt.image.BufferedImage toBufferedImage(Image img) {
        if (img instanceof java.awt.image.BufferedImage) {
            return (java.awt.image.BufferedImage) img;
        }

        java.awt.image.BufferedImage bimage = new java.awt.image.BufferedImage(
                img.getWidth(null), img.getHeight(null), java.awt.image.BufferedImage.TYPE_INT_ARGB);

        java.awt.Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        return bimage;
    }

    // Listener management

    public void addApplicationChangeListener(ApplicationChangeListener listener) {
        listeners.add(listener);
    }

    public void removeApplicationChangeListener(ApplicationChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyApplicationAdded(J2meApplication app) {
        for (ApplicationChangeListener listener : listeners) {
            listener.onApplicationAdded(app);
        }
    }

    private void notifyApplicationRemoved(J2meApplication app) {
        for (ApplicationChangeListener listener : listeners) {
            listener.onApplicationRemoved(app);
        }
    }

    /**
     * Convert absolute path to relative path (relative to data directory)
     */
    private String getRelativePath(String absolutePath) {
        if (absolutePath == null) {
            return null;
        }

        try {
            File absoluteFile = new File(absolutePath);
            File baseDir = dataDirectory.getCanonicalFile();
            File targetFile = absoluteFile.getCanonicalFile();

            // Get relative path
            String relativePath = baseDir.toURI().relativize(targetFile.toURI()).getPath();

            // Remove trailing slash if present
            if (relativePath.endsWith("/")) {
                relativePath = relativePath.substring(0, relativePath.length() - 1);
            }

            return relativePath;
        } catch (IOException e) {
            // If conversion fails, return the original path
            return absolutePath;
        }
    }

    /**
     * Convert relative path to absolute path (relative to data directory)
     */
    private String getAbsolutePath(String relativePath) {
        if (relativePath == null) {
            return null;
        }

        // If already absolute, return as-is
        File file = new File(relativePath);
        if (file.isAbsolute()) {
            return relativePath;
        }

        // Convert relative to absolute
        return new File(dataDirectory, relativePath).getAbsolutePath();
    }

    /**
     * Listener interface for application changes
     */
    public interface ApplicationChangeListener {
        void onApplicationAdded(J2meApplication app);
        void onApplicationRemoved(J2meApplication app);
    }
}
