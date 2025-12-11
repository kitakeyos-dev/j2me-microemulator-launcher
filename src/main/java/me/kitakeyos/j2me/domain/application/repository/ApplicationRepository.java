package me.kitakeyos.j2me.domain.application.repository;

import me.kitakeyos.j2me.domain.application.model.J2meApplication;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Repository interface for J2meApplication persistence.
 * This interface lives in the Domain layer; implementations live in
 * Infrastructure.
 * Following the Dependency Inversion Principle (DIP).
 */
public interface ApplicationRepository {

    /**
     * Read application information from a JAR or JAD file.
     *
     * @param file the JAR or JAD file
     * @return the application information
     * @throws IOException if reading fails
     */
    J2meApplication readApplicationInfo(File file) throws IOException;

    /**
     * Copy a file to the applications directory.
     *
     * @param source         the source file
     * @param targetFileName the target file name
     * @return the copied file
     * @throws IOException if copying fails
     */
    File copyFileToAppsDirectory(File source, String targetFileName) throws IOException;

    /**
     * Save the application icon to disk.
     *
     * @param app the application with icon to save
     * @throws IOException if saving fails
     */
    void saveIcon(J2meApplication app) throws IOException;

    /**
     * Delete the application file and icon from disk.
     *
     * @param app the application to delete files for
     */
    void deleteApplicationFiles(J2meApplication app);

    /**
     * Load all applications from the configuration file.
     *
     * @return list of loaded applications
     */
    List<J2meApplication> loadAllApplications();

    /**
     * Save all applications to the configuration file.
     *
     * @param applications the applications to save
     */
    void saveAllApplications(List<J2meApplication> applications);

    /**
     * Check if a file is a valid J2ME application file.
     *
     * @param file the file to check
     * @return true if valid, false otherwise
     */
    boolean isValidJ2meFile(File file);
}
