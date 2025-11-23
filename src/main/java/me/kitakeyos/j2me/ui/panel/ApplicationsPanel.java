package me.kitakeyos.j2me.ui.panel;

import me.kitakeyos.j2me.config.ApplicationConfig;
import me.kitakeyos.j2me.model.J2meApplication;
import me.kitakeyos.j2me.service.J2meApplicationManager;
import me.kitakeyos.j2me.ui.component.ToastNotification;
import me.kitakeyos.j2me.ui.dialog.MessageDialog;
import me.kitakeyos.j2me.ui.dialog.ConfirmDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Panel for managing installed J2ME applications
 */
public class ApplicationsPanel extends BaseTabPanel implements J2meApplicationManager.ApplicationChangeListener {
    private JPanel applicationsListPanel;

    public ApplicationsPanel(ApplicationConfig applicationConfig, J2meApplicationManager applicationManager) {
        super(applicationConfig, applicationManager);
        this.applicationManager.addApplicationChangeListener(this);
    }

    @Override
    protected JComponent createHeader() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));

        JButton addButton = new JButton("Add Application");
        addButton.setToolTipText("Add new J2ME application from JAR or JAD file");
        addButton.addActionListener(e -> addApplication());
        panel.add(addButton, BorderLayout.EAST);

        return panel;
    }

    @Override
    protected JComponent createContent() {
        applicationsListPanel = new JPanel();
        applicationsListPanel.setLayout(new BoxLayout(applicationsListPanel, BoxLayout.Y_AXIS));
        applicationsListPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(applicationsListPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                "Applications",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        return scrollPane;
    }

    @Override
    protected void onInitialized() {
        // Load applications after UI is initialized
        refreshApplicationsList();
    }

    private void refreshApplicationsList() {
        applicationsListPanel.removeAll();

        java.util.List<J2meApplication> apps = applicationManager.getApplications();

        if (apps.isEmpty()) {
            JLabel emptyLabel = new JLabel("No applications installed. Click 'Add Application' to install one.");
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
            applicationsListPanel.add(emptyLabel);
        } else {
            for (J2meApplication app : apps) {
                JPanel appPanel = createApplicationPanel(app);
                applicationsListPanel.add(appPanel);
                applicationsListPanel.add(Box.createVerticalStrut(5));
            }
        }

        updateStatus();
        applicationsListPanel.revalidate();
        applicationsListPanel.repaint();
    }

    private JPanel createApplicationPanel(J2meApplication app) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                new EmptyBorder(10, 10, 10, 10)
        ));
        panel.setBackground(Color.WHITE);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        // Icon
        JLabel iconLabel = new JLabel();
        if (app.getIcon() != null) {
            Image scaledIcon = app.getIcon().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
            iconLabel.setIcon(new ImageIcon(scaledIcon));
        } else {
            iconLabel.setIcon(createDefaultIcon());
        }
        iconLabel.setPreferredSize(new Dimension(48, 48));
        panel.add(iconLabel, BorderLayout.WEST);

        // Info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);

        JLabel nameLabel = new JLabel(app.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        infoPanel.add(nameLabel);

        if (app.getVendor() != null) {
            JLabel vendorLabel = new JLabel("Vendor: " + app.getVendor());
            vendorLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            vendorLabel.setForeground(Color.GRAY);
            infoPanel.add(vendorLabel);
        }

        if (app.getVersion() != null) {
            JLabel versionLabel = new JLabel("Version: " + app.getVersion());
            versionLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            versionLabel.setForeground(Color.GRAY);
            infoPanel.add(versionLabel);
        }

        JLabel pathLabel = new JLabel("Path: " + app.getFilePath());
        pathLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        pathLabel.setForeground(Color.DARK_GRAY);
        infoPanel.add(pathLabel);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        JLabel dateLabel = new JLabel("Installed: " + dateFormat.format(new Date(app.getInstalledDate())));
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        dateLabel.setForeground(Color.DARK_GRAY);
        infoPanel.add(dateLabel);

        panel.add(infoPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setBackground(Color.WHITE);

        JButton removeButton = new JButton("Remove");
        removeButton.setToolTipText("Remove this application from the list");
        removeButton.addActionListener(e -> removeApplication(app));
        removeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonsPanel.add(removeButton);

        panel.add(buttonsPanel, BorderLayout.EAST);

        return panel;
    }

    private ImageIcon createDefaultIcon() {
        // Create a simple default icon
        int size = 48;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Draw a simple phone icon
        g2d.setColor(Color.GRAY);
        g2d.fillRoundRect(10, 5, size - 20, size - 10, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(15, 10, size - 30, size - 25);
        g2d.setColor(Color.GRAY);
        g2d.fillOval(size / 2 - 3, size - 10, 6, 6);

        g2d.dispose();
        return new ImageIcon(image);
    }

    private void addApplication() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "J2ME Files (JAR, JAD)", "jar", "jad"
        );
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                J2meApplication app = applicationManager.addApplication(selectedFile);
                statusBar.setSuccess("Application added successfully: " + app.getName());
                ToastNotification.showSuccess("Added: " + app.getName());
            } catch (Exception e) {
                statusBar.setError("Error: " + e.getMessage());
                MessageDialog.showError(
                    SwingUtilities.getWindowAncestor(this) instanceof Frame
                        ? (Frame) SwingUtilities.getWindowAncestor(this)
                        : null,
                    "Error",
                    "Failed to add application: " + e.getMessage()
                );
            }
        }
    }

    private void removeApplication(J2meApplication app) {
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        boolean confirm = ConfirmDialog.showConfirm(
                parentFrame,
                "Confirm Remove",
                "Are you sure you want to remove '" + app.getName() + "'?"
        );

        if (confirm) {
            boolean success = applicationManager.removeApplication(app.getId());
            if (success) {
                statusBar.setSuccess("Application removed: " + app.getName());
                ToastNotification.showSuccess("Removed: " + app.getName());
            } else {
                statusBar.setError("Failed to remove application");
            }
        }
    }

    private void updateStatus() {
        int count = applicationManager.getApplicationCount();
        if (count == 0) {
            statusBar.setInfo("No applications installed");
        } else {
            statusBar.setInfo(count + " application(s) installed");
        }
    }

    @Override
    public void onApplicationAdded(J2meApplication app) {
        refreshApplicationsList();
    }

    @Override
    public void onApplicationRemoved(J2meApplication app) {
        refreshApplicationsList();
    }

    public J2meApplicationManager getApplicationManager() {
        return applicationManager;
    }
}
