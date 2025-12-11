package me.kitakeyos.j2me.infrastructure.persistence.application;

import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.domain.application.model.J2meApplication;
import me.kitakeyos.j2me.domain.application.repository.ApplicationRepository;
import me.kitakeyos.j2me.infrastructure.resource.ManifestReader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Infrastructure implementation of ApplicationRepository.
 * Handles all file I/O operations for J2ME applications.
 */
public class ApplicationRepositoryImpl implements ApplicationRepository {
    private static final Logger logger = Logger.getLogger(ApplicationRepositoryImpl.class.getName());

    private final File dataDirectory;
    private final File appsDirectory;
    private final File iconsDirectory;
    private final File configFile;

    public ApplicationRepositoryImpl(ApplicationConfig config) {
        this.dataDirectory = config.getDataDirectory();
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }

        this.appsDirectory = new File(dataDirectory, ApplicationConfig.APPS_DIR);
        if (!appsDirectory.exists()) {
            appsDirectory.mkdirs();
        }

        this.iconsDirectory = new File(dataDirectory, ApplicationConfig.ICONS_DIR);
        if (!iconsDirectory.exists()) {
            iconsDirectory.mkdirs();
        }

        this.configFile = new File(dataDirectory, ApplicationConfig.APPS_CONFIG_FILE);
    }

    @Override
    public J2meApplication readApplicationInfo(File file) throws IOException {
        return ManifestReader.readApplicationInfo(file);
    }

    @Override
    public File copyFileToAppsDirectory(File source, String targetFileName) throws IOException {
        File targetFile = new File(appsDirectory, targetFileName);
        copyFile(source, targetFile);
        return targetFile;
    }

    @Override
    public void saveIcon(J2meApplication app) throws IOException {
        if (app.getIcon() != null) {
            String iconFileName = app.getId() + ".png";
            File iconFile = new File(iconsDirectory, iconFileName);
            ImageIO.write(toBufferedImage(app.getIcon()), "png", iconFile);
            app.setIconPath(iconFile.getAbsolutePath());
        }
    }

    @Override
    public void deleteApplicationFiles(J2meApplication app) {
        // Delete JAR/JAD file
        File appFile = new File(app.getFilePath());
        if (appFile.exists()) {
            appFile.delete();
        }

        // Delete icon file
        if (app.getIconPath() != null) {
            File iconFile = new File(app.getIconPath());
            if (iconFile.exists()) {
                iconFile.delete();
            }
        }
    }

    @Override
    public List<J2meApplication> loadAllApplications() {
        List<J2meApplication> applications = new ArrayList<>();

        if (!configFile.exists()) {
            return applications;
        }

        Properties props = new Properties();
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(configFile))) {
            props.load(bis);

            int count = Integer.parseInt(props.getProperty("app.count", "0"));

            for (int i = 0; i < count; i++) {
                String prefix = "app." + i + ".";

                String id = props.getProperty(prefix + "id");
                String name = props.getProperty(prefix + "name");
                String relativeFilePath = props.getProperty(prefix + "filePath");

                if (id == null || name == null || relativeFilePath == null) {
                    continue;
                }

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
                    String absoluteIconPath = getAbsolutePath(relativeIconPath);
                    app.setIconPath(absoluteIconPath);
                    loadIconForApplication(app, absoluteIconPath);
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
            logger.log(Level.SEVERE, "Error loading applications: " + e.getMessage());
        }

        return applications;
    }

    @Override
    public void saveAllApplications(List<J2meApplication> applications) {
        Properties props = new Properties();

        props.setProperty("app.count", String.valueOf(applications.size()));

        for (int i = 0; i < applications.size(); i++) {
            J2meApplication app = applications.get(i);
            String prefix = "app." + i + ".";

            props.setProperty(prefix + "id", app.getId());
            props.setProperty(prefix + "name", app.getName());
            props.setProperty(prefix + "filePath", getRelativePath(app.getFilePath()));

            if (app.getVendor() != null) {
                props.setProperty(prefix + "vendor", app.getVendor());
            }
            if (app.getVersion() != null) {
                props.setProperty(prefix + "version", app.getVersion());
            }
            if (app.getIconPath() != null) {
                props.setProperty(prefix + "iconPath", getRelativePath(app.getIconPath()));
            }

            props.setProperty(prefix + "installedDate", String.valueOf(app.getInstalledDate()));
            props.setProperty(prefix + "fileSize", String.valueOf(app.getFileSize()));
        }

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(configFile))) {
            props.store(bos, "J2ME Applications Configuration");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error saving applications: " + e.getMessage());
        }
    }

    @Override
    public boolean isValidJ2meFile(File file) {
        return ManifestReader.isValidJ2meFile(file);
    }

    // ========== Private Helper Methods ==========

    private void copyFile(File source, File dest) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(source));
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    private void loadIconForApplication(J2meApplication app, String iconPath) {
        File iconFile = new File(iconPath);
        if (iconFile.exists()) {
            try {
                Image icon = ImageIO.read(iconFile);
                app.setIcon(icon);
            } catch (IOException e) {
                // Ignore icon loading errors
            }
        }
    }

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

    private String getRelativePath(String absolutePath) {
        if (absolutePath == null) {
            return null;
        }

        try {
            File absoluteFile = new File(absolutePath);
            File baseDir = dataDirectory.getCanonicalFile();
            File targetFile = absoluteFile.getCanonicalFile();

            String relativePath = baseDir.toURI().relativize(targetFile.toURI()).getPath();

            if (relativePath.endsWith("/")) {
                relativePath = relativePath.substring(0, relativePath.length() - 1);
            }

            return relativePath;
        } catch (IOException e) {
            return absolutePath;
        }
    }

    private String getAbsolutePath(String relativePath) {
        if (relativePath == null) {
            return null;
        }

        File file = new File(relativePath);
        if (file.isAbsolute()) {
            return relativePath;
        }

        return new File(dataDirectory, relativePath).getAbsolutePath();
    }
}
