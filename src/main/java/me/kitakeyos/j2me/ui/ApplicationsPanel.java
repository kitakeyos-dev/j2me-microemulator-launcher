package me.kitakeyos.j2me.ui;

import me.kitakeyos.j2me.config.J2meApplication;
import me.kitakeyos.j2me.manager.J2meApplicationManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Panel for managing installed J2ME applications
 */
public class ApplicationsPanel extends JPanel implements J2meApplicationManager.ApplicationChangeListener {
    private J2meApplicationManager applicationManager;
    private JPanel applicationsListPanel;
    private JLabel statusLabel;

    public ApplicationsPanel(J2meApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
        this.applicationManager.addApplicationChangeListener(this);

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top panel with title and add button
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // Center panel with applications list (scrollable)
        JScrollPane scrollPane = createApplicationsListScrollPane();
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with status
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);

        // Load applications
        refreshApplicationsList();
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));

        JLabel titleLabel = new JLabel("Installed J2ME Applications");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.WEST);

        JButton addButton = new JButton("Add Application");
        addButton.setToolTipText("Thêm ứng dụng J2ME mới từ file JAR hoặc JAD");
        addButton.addActionListener(e -> addApplication());
        panel.add(addButton, BorderLayout.EAST);

        return panel;
    }

    private JScrollPane createApplicationsListScrollPane() {
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

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.BLUE);
        panel.add(statusLabel, BorderLayout.WEST);
        return panel;
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
        removeButton.setToolTipText("Xóa ứng dụng này khỏi danh sách");
        removeButton.addActionListener(e -> removeApplication(app));
        removeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonsPanel.add(removeButton);

        panel.add(buttonsPanel, BorderLayout.EAST);

        // Add hover effect
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBackground(new Color(240, 240, 255));
                infoPanel.setBackground(new Color(240, 240, 255));
                buttonsPanel.setBackground(new Color(240, 240, 255));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBackground(Color.WHITE);
                infoPanel.setBackground(Color.WHITE);
                buttonsPanel.setBackground(Color.WHITE);
            }
        });

        return panel;
    }

    private ImageIcon createDefaultIcon() {
        // Create a simple default icon
        int size = 48;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Draw a simple phone icon
        g2d.setColor(new Color(100, 100, 200));
        g2d.fillRoundRect(10, 5, size - 20, size - 10, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(15, 10, size - 30, size - 25);
        g2d.setColor(new Color(100, 100, 200));
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
                statusLabel.setText("Application added successfully: " + app.getName());
                statusLabel.setForeground(new Color(0, 150, 0));
            } catch (Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
                statusLabel.setForeground(Color.RED);
                JOptionPane.showMessageDialog(this,
                        "Failed to add application: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeApplication(J2meApplication app) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove '" + app.getName() + "'?",
                "Confirm Remove",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = applicationManager.removeApplication(app.getId());
            if (success) {
                statusLabel.setText("Application removed: " + app.getName());
                statusLabel.setForeground(new Color(0, 150, 0));
            } else {
                statusLabel.setText("Failed to remove application");
                statusLabel.setForeground(Color.RED);
            }
        }
    }

    private void updateStatus() {
        int count = applicationManager.getApplicationCount();
        if (count == 0) {
            statusLabel.setText("No applications installed");
        } else {
            statusLabel.setText(count + " application(s) installed");
        }
        statusLabel.setForeground(Color.BLUE);
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
