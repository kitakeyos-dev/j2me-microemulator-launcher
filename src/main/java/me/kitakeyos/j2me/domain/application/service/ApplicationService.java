package me.kitakeyos.j2me.domain.application.service;

import me.kitakeyos.j2me.domain.application.model.J2meApplication;
import me.kitakeyos.j2me.domain.application.repository.ApplicationRepository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing installed J2ME applications.
 * This is a Domain service that uses the ApplicationRepository interface
 * for persistence operations (Dependency Inversion Principle).
 */
public class ApplicationService {

    private final List<J2meApplication> applications;
    private final ApplicationRepository repository;
    private final List<ApplicationChangeListener> listeners;

    public ApplicationService(ApplicationRepository repository) {
        this.applications = new ArrayList<>();
        this.repository = repository;
        this.listeners = new ArrayList<>();

        loadApplications();
    }

    /**
     * Add a new application from file
     */
    public J2meApplication addApplication(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new FileNotFoundException("File not found: " + file);
        }

        // Read application info via repository
        J2meApplication app = repository.readApplicationInfo(file);

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

        // Copy/clone the JAR/JAD file to apps directory via repository
        String fileExtension = fileName.substring(fileName.lastIndexOf('.'));
        String clonedFileName = app.getId() + fileExtension;

        try {
            File clonedFile = repository.copyFileToAppsDirectory(file, clonedFileName);
            app.setFilePath(clonedFile.getAbsolutePath());
        } catch (IOException e) {
            throw new IOException("Failed to copy application file: " + e.getMessage(), e);
        }

        // Save icon to disk if available via repository
        try {
            repository.saveIcon(app);
        } catch (IOException e) {
            // Ignore icon save errors
            app.setIconPath(null);
        }

        applications.add(app);
        saveApplications();
        notifyApplicationAdded(app);

        return app;
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

            // Delete files via repository
            repository.deleteApplicationFiles(application);

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
     * Save applications via repository
     */
    private void saveApplications() {
        repository.saveAllApplications(applications);
    }

    /**
     * Load applications via repository
     */
    private void loadApplications() {
        List<J2meApplication> loaded = repository.loadAllApplications();
        applications.clear();
        applications.addAll(loaded);
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
     * Listener interface for application changes
     */
    public interface ApplicationChangeListener {
        void onApplicationAdded(J2meApplication app);

        void onApplicationRemoved(J2meApplication app);
    }
}
