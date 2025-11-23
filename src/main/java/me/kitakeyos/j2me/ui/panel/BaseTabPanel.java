package me.kitakeyos.j2me.ui.panel;

import me.kitakeyos.j2me.config.ApplicationConfig;
import me.kitakeyos.j2me.service.J2meApplicationManager;
import me.kitakeyos.j2me.ui.component.StatusBar;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Abstract base class for all tab panels in the application.
 * Provides standardized layout structure and consistent spacing across tabs.
 *
 * Layout structure:
 * - NORTH: Header/toolbar area (implemented by subclasses)
 * - CENTER: Main content area (implemented by subclasses)
 * - SOUTH: Status bar (standardized)
 *
 * All tabs have:
 * - BorderLayout(10, 10) with 10px horizontal and vertical gaps
 * - EmptyBorder(10, 10, 10, 10) for consistent margins
 * - Status bar with top border line
 */
public abstract class BaseTabPanel extends JPanel {

    protected StatusBar statusBar;
    protected final ApplicationConfig applicationConfig;
    protected final J2meApplicationManager applicationManager;

    public BaseTabPanel(ApplicationConfig applicationConfig, J2meApplicationManager applicationManager) {
        this.applicationConfig = applicationConfig;
        this.applicationManager = applicationManager;
        // Standardized layout: BorderLayout with 10px gaps
        setLayout(new BorderLayout(10, 10));

        // Standardized border: 10px margin on all sides
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Initialize UI components
        initializeComponents();
    }

    /**
     * Initialize all components and build the UI.
     * Called from constructor.
     */
    private void initializeComponents() {
        // Create header/toolbar (implemented by subclass)
        JComponent header = createHeader();
        if (header != null) {
            add(header, BorderLayout.NORTH);
        }

        // Create main content (implemented by subclass)
        JComponent content = createContent();
        if (content != null) {
            add(content, BorderLayout.CENTER);
        }

        // Create standardized status bar
        statusBar = new StatusBar();
        statusBar.setInfoStatus("Ready");
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(statusBar, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Post-initialization hook for subclasses
        onInitialized();
    }

    /**
     * Create the header/toolbar component for the NORTH section.
     * Subclasses should override this to provide their custom header.
     *
     * @return The header component, or null if no header is needed
     */
    protected abstract JComponent createHeader();

    /**
     * Create the main content component for the CENTER section.
     * Subclasses must override this to provide their main content.
     *
     * @return The content component (must not be null)
     */
    protected abstract JComponent createContent();

    /**
     * Called after all components are initialized.
     * Subclasses can override this to perform additional initialization.
     */
    protected void onInitialized() {
        // Default: do nothing
    }

    /**
     * Get the status bar instance for updating status messages.
     *
     * @return The status bar instance
     */
    public StatusBar getStatusBar() {
        return statusBar;
    }

    /**
     * Convenience method to update status bar with info message.
     *
     * @param message The message to display
     */
    protected void setStatusInfo(String message) {
        statusBar.setInfoStatus(message);
    }

    /**
     * Convenience method to update status bar with success message.
     *
     * @param message The message to display
     */
    protected void setStatusSuccess(String message) {
        statusBar.setSuccessStatus(message);
    }

    /**
     * Convenience method to update status bar with warning message.
     *
     * @param message The message to display
     */
    protected void setStatusWarning(String message) {
        statusBar.setWarningStatus(message);
    }

    /**
     * Convenience method to update status bar with error message.
     *
     * @param message The message to display
     */
    protected void setStatusError(String message) {
        statusBar.setErrorStatus(message);
    }

    /**
     * Convenience method to clear status bar to default "Ready" state.
     */
    protected void clearStatus() {
        statusBar.clearStatus();
    }
}
