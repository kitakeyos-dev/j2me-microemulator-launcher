package me.kitakeyos.j2me.domain.application.model;

import java.awt.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a J2ME application installed in the launcher
 */
public class J2meApplication implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;              // Unique identifier
    private String name;            // Application name from manifest
    private String vendor;          // Application vendor
    private String version;         // Application version
    private String filePath;        // Path to JAR/JAD file
    private String iconPath;        // Path to icon image (extracted from JAR)
    private transient Image icon;   // Icon image (loaded on demand)
    private long installedDate;     // Installation timestamp
    private long fileSize;          // File size in bytes

    public J2meApplication() {
        this.installedDate = System.currentTimeMillis();
    }

    public J2meApplication(String id, String name, String filePath) {
        this.id = id;
        this.name = name;
        this.filePath = filePath;
        this.installedDate = System.currentTimeMillis();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    public Image getIcon() {
        return icon;
    }

    public void setIcon(Image icon) {
        this.icon = icon;
    }

    public long getInstalledDate() {
        return installedDate;
    }

    public void setInstalledDate(long installedDate) {
        this.installedDate = installedDate;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        J2meApplication that = (J2meApplication) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + (version != null ? " v" + version : "") +
               (vendor != null ? " (" + vendor + ")" : "");
    }
}
