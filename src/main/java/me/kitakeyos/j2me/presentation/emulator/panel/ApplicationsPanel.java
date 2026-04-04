package me.kitakeyos.j2me.presentation.emulator.panel;

import me.kitakeyos.j2me.application.MainApplication;
import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.domain.application.model.J2meApplication;
import me.kitakeyos.j2me.domain.application.service.ApplicationService;
import me.kitakeyos.j2me.presentation.common.component.BaseTabPanel;
import me.kitakeyos.j2me.presentation.common.component.ToastNotification;
import me.kitakeyos.j2me.presentation.common.dialog.ConfirmDialog;
import me.kitakeyos.j2me.presentation.common.dialog.MessageDialog;
import me.kitakeyos.j2me.presentation.common.i18n.Messages;

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
public class ApplicationsPanel extends BaseTabPanel implements ApplicationService.ApplicationChangeListener {
    private JPanel applicationsListPanel;

    public ApplicationsPanel(MainApplication mainApplication, ApplicationConfig applicationConfig, ApplicationService applicationManager) {
        super(mainApplication, applicationConfig, applicationManager);
        this.applicationManager.addApplicationChangeListener(this);
    }

    @Override
    protected JComponent createHeader() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));

        JButton addButton = new JButton(Messages.get("apps.addButton"));
        addButton.setToolTipText(Messages.get("apps.addButton.tooltip"));
        addButton.addActionListener(e -> addApplication());
        panel.add(addButton, BorderLayout.EAST);

        return panel;
    }

    @Override
    protected JComponent createContent() {
        applicationsListPanel = new JPanel();
        applicationsListPanel.setLayout(new BoxLayout(applicationsListPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(applicationsListPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                Messages.get("apps.list.title"),
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

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
            JLabel emptyLabel = new JLabel(Messages.get("apps.empty"));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
            applicationsListPanel.add(emptyLabel);
        } else {
            for (J2meApplication app : apps) {
                JPanel appPanel = createApplicationPanel(app);
                applicationsListPanel.add(appPanel);
                applicationsListPanel.add(Box.createVerticalStrut(4));
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
                new EmptyBorder(8, 12, 8, 12)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

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
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(app.getName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        infoPanel.add(nameLabel);

        if (app.getVendor() != null) {
            JLabel vendorLabel = new JLabel(Messages.get("apps.vendor", app.getVendor()));
            vendorLabel.setFont(vendorLabel.getFont().deriveFont(Font.PLAIN, 11f));
            vendorLabel.setForeground(Color.GRAY);
            infoPanel.add(vendorLabel);
        }

        if (app.getVersion() != null) {
            JLabel versionLabel = new JLabel(Messages.get("apps.version", app.getVersion()));
            versionLabel.setFont(versionLabel.getFont().deriveFont(Font.PLAIN, 11f));
            versionLabel.setForeground(Color.GRAY);
            infoPanel.add(versionLabel);
        }

        JLabel pathLabel = new JLabel(Messages.get("apps.path", app.getFilePath()));
        pathLabel.setFont(pathLabel.getFont().deriveFont(Font.PLAIN, 10f));
        pathLabel.setForeground(Color.DARK_GRAY);
        infoPanel.add(pathLabel);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        JLabel dateLabel = new JLabel(Messages.get("apps.installed", dateFormat.format(new Date(app.getInstalledDate()))));
        dateLabel.setFont(dateLabel.getFont().deriveFont(Font.PLAIN, 10f));
        dateLabel.setForeground(Color.DARK_GRAY);
        infoPanel.add(dateLabel);

        panel.add(infoPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setOpaque(false);

        JButton removeButton = new JButton(Messages.get("common.remove"));
        removeButton.setToolTipText(Messages.get("apps.remove.tooltip"));
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
                Messages.get("apps.fileFilter"), "jar", "jad"
        );
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                J2meApplication app = applicationManager.addApplication(selectedFile);
                statusBar.setSuccess(Messages.get("apps.added", app.getName()));
                ToastNotification.showSuccess(Messages.get("apps.addedShort", app.getName()));
            } catch (Exception e) {
                statusBar.setError("Error: " + e.getMessage());
                MessageDialog.showError(
                    SwingUtilities.getWindowAncestor(this) instanceof Frame
                        ? (Frame) SwingUtilities.getWindowAncestor(this)
                        : null,
                    Messages.get("common.error"),
                    Messages.get("apps.addFailed", e.getMessage())
                );
            }
        }
    }

    private void removeApplication(J2meApplication app) {
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        boolean confirm = ConfirmDialog.showConfirm(
                parentFrame,
                Messages.get("apps.remove.confirm.title"),
                Messages.get("apps.remove.confirm.message", app.getName())
        );

        if (confirm) {
            boolean success = applicationManager.removeApplication(app.getId());
            if (success) {
                statusBar.setSuccess(Messages.get("apps.removed", app.getName()));
                ToastNotification.showSuccess(Messages.get("apps.removedShort", app.getName()));
            } else {
                statusBar.setError(Messages.get("apps.removeFailed"));
            }
        }
    }

    private void updateStatus() {
        int count = applicationManager.getApplicationCount();
        if (count == 0) {
            statusBar.setInfo(Messages.get("apps.status.none"));
        } else {
            statusBar.setInfo(Messages.get("apps.status.count", count));
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

    public ApplicationService getApplicationManager() {
        return applicationManager;
    }
}
