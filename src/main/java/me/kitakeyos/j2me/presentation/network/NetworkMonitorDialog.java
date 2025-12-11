package me.kitakeyos.j2me.presentation.network;

import me.kitakeyos.j2me.domain.network.model.ConnectionLog;
import me.kitakeyos.j2me.domain.network.model.PacketLog;
import me.kitakeyos.j2me.domain.network.model.ProxyRule;
import me.kitakeyos.j2me.domain.network.model.RedirectionRule;
import me.kitakeyos.j2me.domain.network.service.NetworkService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Dialog to monitor network connections, manage redirection rules, and proxy
 * settings.
 */
public class NetworkMonitorDialog extends JDialog implements NetworkService.NetworkChangeListener {

    private final NetworkService networkService;

    // Logs tab
    private DefaultTableModel logsTableModel;
    private JTable logsTable;
    private java.util.List<ConnectionLog> logsList = new java.util.ArrayList<>();

    // Redirection rules tab
    private DefaultTableModel redirectionTableModel;
    private JTable redirectionTable;

    // Proxy rules tab
    private DefaultTableModel proxyTableModel;
    private JTable proxyTable;

    // Packet logs tab
    private DefaultTableModel packetLogsTableModel;
    private JTable packetLogsTable;
    private JLabel packetStatsLabel;

    public NetworkMonitorDialog(Frame owner) {
        super(owner, "Network Monitor", false);
        this.networkService = NetworkService.getInstance();

        initComponents();
        loadData();

        networkService.addListener(this);

        setSize(800, 500);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();

        // Logs tab
        tabbedPane.addTab("Connection Logs", createLogsPanel());

        // Redirection rules tab
        tabbedPane.addTab("Redirection Rules", createRedirectionPanel());

        // Proxy rules tab
        tabbedPane.addTab("Proxy Rules", createProxyPanel());

        // Packet logs tab
        tabbedPane.addTab("Packet Logs", createPacketLogsPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createLogsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = { "Time", "Instance", "Original", "Actual", "Proxy", "Status" };
        logsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        logsTable = new JTable(logsTableModel);
        logsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        logsTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        logsTable.getColumnModel().getColumn(1).setPreferredWidth(60);

        JScrollPane scrollPane = new JScrollPane(logsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton createRedirectButton = new JButton("Create Redirect Rule");
        createRedirectButton.setToolTipText("Create a redirect rule from selected log entry");
        createRedirectButton.addActionListener(e -> createRedirectFromSelectedLog());

        JButton clearButton = new JButton("Clear Logs");
        clearButton.addActionListener(e -> {
            networkService.clearConnectionLogs();
            logsTableModel.setRowCount(0);
            logsList.clear();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(createRedirectButton);
        buttonPanel.add(clearButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createRedirectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = { "Enabled", "Instance", "Original Host", "Port", "Target Host", "Port" };
        redirectionTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // Only enabled column is editable
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }
        };
        redirectionTable = new JTable(redirectionTableModel);
        redirectionTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        redirectionTable.getColumnModel().getColumn(1).setPreferredWidth(60);

        // Handle enable/disable toggle
        redirectionTableModel.addTableModelListener(e -> {
            if (e.getColumn() == 0) {
                int row = e.getFirstRow();
                Boolean enabled = (Boolean) redirectionTableModel.getValueAt(row, 0);
                List<RedirectionRule> rules = networkService.getRedirectionRules();
                if (row < rules.size()) {
                    rules.get(row).setEnabled(enabled);
                    networkService.saveRules();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(redirectionTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Double-click to edit
        redirectionTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedRedirectionRule();
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton addButton = new JButton("Add Rule");
        addButton.addActionListener(e -> showAddRedirectionDialog());

        JButton editButton = new JButton("Edit");
        editButton.addActionListener(e -> editSelectedRedirectionRule());

        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeSelectedRedirectionRule());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(removeButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createProxyPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = { "Enabled", "Instance", "Type", "Proxy Host", "Port", "Auth" };
        proxyTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }
        };
        proxyTable = new JTable(proxyTableModel);
        proxyTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        proxyTable.getColumnModel().getColumn(1).setPreferredWidth(60);

        // Handle enable/disable toggle
        proxyTableModel.addTableModelListener(e -> {
            if (e.getColumn() == 0) {
                int row = e.getFirstRow();
                Boolean enabled = (Boolean) proxyTableModel.getValueAt(row, 0);
                List<ProxyRule> rules = networkService.getProxyRules();
                if (row < rules.size()) {
                    rules.get(row).setEnabled(enabled);
                    networkService.saveRules();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(proxyTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Double-click to edit
        proxyTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedProxyRule();
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton addButton = new JButton("Add Proxy");
        addButton.addActionListener(e -> showAddProxyDialog());

        JButton editButton = new JButton("Edit");
        editButton.addActionListener(e -> editSelectedProxyRule());

        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeSelectedProxyRule());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(removeButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPacketLogsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = { "Time", "Inst:Socket", "Dir", "Host:Port", "Size", "Data Preview" };
        packetLogsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        packetLogsTable = new JTable(packetLogsTableModel);
        packetLogsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        packetLogsTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        packetLogsTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        packetLogsTable.getColumnModel().getColumn(2).setPreferredWidth(40);
        packetLogsTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        packetLogsTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        packetLogsTable.getColumnModel().getColumn(5).setPreferredWidth(200);

        // Set monospace font for data preview
        packetLogsTable.getColumnModel().getColumn(5).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                c.setFont(new Font("Monospaced", Font.PLAIN, 12));
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(packetLogsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        packetStatsLabel = new JLabel("Sent: 0 B | Received: 0 B");
        packetStatsLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        JButton clearButton = new JButton("Clear Packets");
        clearButton.addActionListener(e -> {
            networkService.clearPacketLogs();
            packetLogsTableModel.setRowCount(0);
            updatePacketStats();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearButton);

        bottomPanel.add(packetStatsLabel, BorderLayout.WEST);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadData() {
        // Load logs
        for (ConnectionLog log : networkService.getConnectionLogs()) {
            logsList.add(log);
            addLogToTable(log);
        }

        // Load redirection rules
        for (RedirectionRule rule : networkService.getRedirectionRules()) {
            addRedirectionRuleToTable(rule);
        }

        // Load proxy rules
        for (ProxyRule rule : networkService.getProxyRules()) {
            addProxyRuleToTable(rule);
        }

        // Load packet logs
        for (PacketLog log : networkService.getPacketLogs()) {
            addPacketLogToTable(log);
        }
        updatePacketStats();
    }

    private void addLogToTable(ConnectionLog log) {
        String original = log.getOriginalHost() + ":" + log.getOriginalPort();
        String actual = log.getActualHost() + ":" + log.getActualPort();
        String proxy = log.getProxyInfo() != null ? log.getProxyInfo() : "-";
        String status = log.isSuccess() ? "OK" : "FAILED";

        logsTableModel.addRow(new Object[] {
                log.getFormattedTimestamp(),
                "#" + log.getInstanceId(),
                original,
                actual,
                proxy,
                status
        });

        // Scroll to bottom
        SwingUtilities.invokeLater(() -> {
            int lastRow = logsTable.getRowCount() - 1;
            if (lastRow >= 0) {
                logsTable.scrollRectToVisible(logsTable.getCellRect(lastRow, 0, true));
            }
        });
    }

    private void addRedirectionRuleToTable(RedirectionRule rule) {
        String instanceStr = rule.getInstanceId() == RedirectionRule.ALL_INSTANCES ? "ALL" : "#" + rule.getInstanceId();
        redirectionTableModel.addRow(new Object[] {
                rule.isEnabled(),
                instanceStr,
                rule.getOriginalHost(),
                rule.getOriginalPort(),
                rule.getTargetHost(),
                rule.getTargetPort()
        });
    }

    private void addProxyRuleToTable(ProxyRule rule) {
        String instanceStr = rule.getInstanceId() == ProxyRule.ALL_INSTANCES ? "ALL" : "#" + rule.getInstanceId();
        String authStr = rule.hasAuthentication() ? "Yes" : "No";
        proxyTableModel.addRow(new Object[] {
                rule.isEnabled(),
                instanceStr,
                rule.getProxyType().name(),
                rule.getProxyHost(),
                rule.getProxyPort(),
                authStr
        });
    }

    private void addPacketLogToTable(PacketLog log) {
        String dir = log.getDirection() == PacketLog.Direction.IN ? "<<" : ">>";
        packetLogsTableModel.addRow(new Object[] {
                log.getFormattedTimestamp(),
                "#" + log.getInstanceId() + ":" + log.getSocketId(),
                dir,
                log.getHost() + ":" + log.getPort(),
                log.getLength() + " B",
                log.getAsciiPreview()
        });

        // Scroll to bottom
        SwingUtilities.invokeLater(() -> {
            int lastRow = packetLogsTable.getRowCount() - 1;
            if (lastRow >= 0) {
                packetLogsTable.scrollRectToVisible(packetLogsTable.getCellRect(lastRow, 0, true));
            }
        });
    }

    private void updatePacketStats() {
        packetStatsLabel.setText(networkService.getFormattedStats());
    }

    private void showAddRedirectionDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));

        JTextField instanceField = new JTextField("-1");
        JTextField origHostField = new JTextField();
        JTextField origPortField = new JTextField("80");
        JTextField targetHostField = new JTextField();
        JTextField targetPortField = new JTextField("80");

        panel.add(new JLabel("Instance ID (-1 for all):"));
        panel.add(instanceField);
        panel.add(new JLabel("Original Host:"));
        panel.add(origHostField);
        panel.add(new JLabel("Original Port:"));
        panel.add(origPortField);
        panel.add(new JLabel("Target Host:"));
        panel.add(targetHostField);
        panel.add(new JLabel("Target Port:"));
        panel.add(targetPortField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Redirection Rule",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int instanceId = Integer.parseInt(instanceField.getText().trim());
                String origHost = origHostField.getText().trim();
                int origPort = Integer.parseInt(origPortField.getText().trim());
                String targetHost = targetHostField.getText().trim();
                int targetPort = Integer.parseInt(targetPortField.getText().trim());

                if (origHost.isEmpty() || targetHost.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Host cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                RedirectionRule rule = new RedirectionRule(origHost, origPort, targetHost, targetPort, instanceId);
                networkService.addRedirectionRule(rule);
                addRedirectionRuleToTable(rule);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid number format", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAddProxyDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));

        JTextField instanceField = new JTextField("-1");
        JComboBox<ProxyRule.ProxyType> typeCombo = new JComboBox<>(ProxyRule.ProxyType.values());
        JTextField hostField = new JTextField();
        JTextField portField = new JTextField("1080");
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        panel.add(new JLabel("Instance ID (-1 for all):"));
        panel.add(instanceField);
        panel.add(new JLabel("Proxy Type:"));
        panel.add(typeCombo);
        panel.add(new JLabel("Proxy Host:"));
        panel.add(hostField);
        panel.add(new JLabel("Proxy Port:"));
        panel.add(portField);
        panel.add(new JLabel("Username (optional):"));
        panel.add(usernameField);
        panel.add(new JLabel("Password (optional):"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Proxy Rule",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int instanceId = Integer.parseInt(instanceField.getText().trim());
                ProxyRule.ProxyType type = (ProxyRule.ProxyType) typeCombo.getSelectedItem();
                String host = hostField.getText().trim();
                int port = Integer.parseInt(portField.getText().trim());
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());

                if (host.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Host cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                ProxyRule rule;
                if (username.isEmpty()) {
                    rule = new ProxyRule(type, host, port, instanceId);
                } else {
                    rule = new ProxyRule(type, host, port, instanceId, username, password);
                }
                networkService.addProxyRule(rule);
                addProxyRuleToTable(rule);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid number format", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeSelectedRedirectionRule() {
        int selectedRow = redirectionTable.getSelectedRow();
        if (selectedRow >= 0) {
            List<RedirectionRule> rules = networkService.getRedirectionRules();
            if (selectedRow < rules.size()) {
                networkService.removeRedirectionRule(rules.get(selectedRow));
                redirectionTableModel.removeRow(selectedRow);
            }
        }
    }

    private void removeSelectedProxyRule() {
        int selectedRow = proxyTable.getSelectedRow();
        if (selectedRow >= 0) {
            List<ProxyRule> rules = networkService.getProxyRules();
            if (selectedRow < rules.size()) {
                networkService.removeProxyRule(rules.get(selectedRow));
                proxyTableModel.removeRow(selectedRow);
            }
        }
    }

    /**
     * Edit the selected redirection rule
     */
    private void editSelectedRedirectionRule() {
        int selectedRow = redirectionTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a rule to edit", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<RedirectionRule> rules = networkService.getRedirectionRules();
        if (selectedRow >= rules.size())
            return;

        RedirectionRule oldRule = rules.get(selectedRow);

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));

        JTextField instanceField = new JTextField(String.valueOf(oldRule.getInstanceId()));
        JTextField origHostField = new JTextField(oldRule.getOriginalHost());
        JTextField origPortField = new JTextField(String.valueOf(oldRule.getOriginalPort()));
        JTextField targetHostField = new JTextField(oldRule.getTargetHost());
        JTextField targetPortField = new JTextField(String.valueOf(oldRule.getTargetPort()));

        panel.add(new JLabel("Instance ID (-1 for all):"));
        panel.add(instanceField);
        panel.add(new JLabel("Original Host:"));
        panel.add(origHostField);
        panel.add(new JLabel("Original Port:"));
        panel.add(origPortField);
        panel.add(new JLabel("Target Host:"));
        panel.add(targetHostField);
        panel.add(new JLabel("Target Port:"));
        panel.add(targetPortField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Redirection Rule",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int instanceId = Integer.parseInt(instanceField.getText().trim());
                String origHost = origHostField.getText().trim();
                int origPort = Integer.parseInt(origPortField.getText().trim());
                String targetHost = targetHostField.getText().trim();
                int targetPort = Integer.parseInt(targetPortField.getText().trim());

                if (origHost.isEmpty() || targetHost.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Host cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Remove old and add new
                networkService.removeRedirectionRule(oldRule);
                RedirectionRule newRule = new RedirectionRule(origHost, origPort, targetHost, targetPort, instanceId);
                networkService.addRedirectionRule(newRule);

                // Update table
                String instanceStr = instanceId == RedirectionRule.ALL_INSTANCES ? "ALL" : "#" + instanceId;
                redirectionTableModel.setValueAt(newRule.isEnabled(), selectedRow, 0);
                redirectionTableModel.setValueAt(instanceStr, selectedRow, 1);
                redirectionTableModel.setValueAt(origHost, selectedRow, 2);
                redirectionTableModel.setValueAt(origPort, selectedRow, 3);
                redirectionTableModel.setValueAt(targetHost, selectedRow, 4);
                redirectionTableModel.setValueAt(targetPort, selectedRow, 5);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid number format", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Edit the selected proxy rule
     */
    private void editSelectedProxyRule() {
        int selectedRow = proxyTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a rule to edit", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<ProxyRule> rules = networkService.getProxyRules();
        if (selectedRow >= rules.size())
            return;

        ProxyRule oldRule = rules.get(selectedRow);

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));

        JTextField instanceField = new JTextField(String.valueOf(oldRule.getInstanceId()));
        JComboBox<ProxyRule.ProxyType> typeCombo = new JComboBox<>(ProxyRule.ProxyType.values());
        typeCombo.setSelectedItem(oldRule.getProxyType());
        JTextField hostField = new JTextField(oldRule.getProxyHost());
        JTextField portField = new JTextField(String.valueOf(oldRule.getProxyPort()));
        JTextField usernameField = new JTextField(oldRule.getUsername() != null ? oldRule.getUsername() : "");
        JPasswordField passwordField = new JPasswordField(oldRule.getPassword() != null ? oldRule.getPassword() : "");

        panel.add(new JLabel("Instance ID (-1 for all):"));
        panel.add(instanceField);
        panel.add(new JLabel("Proxy Type:"));
        panel.add(typeCombo);
        panel.add(new JLabel("Proxy Host:"));
        panel.add(hostField);
        panel.add(new JLabel("Proxy Port:"));
        panel.add(portField);
        panel.add(new JLabel("Username (optional):"));
        panel.add(usernameField);
        panel.add(new JLabel("Password (optional):"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Proxy Rule",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int instanceId = Integer.parseInt(instanceField.getText().trim());
                ProxyRule.ProxyType type = (ProxyRule.ProxyType) typeCombo.getSelectedItem();
                String host = hostField.getText().trim();
                int port = Integer.parseInt(portField.getText().trim());
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());

                if (host.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Host cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Remove old and add new
                networkService.removeProxyRule(oldRule);
                ProxyRule newRule;
                if (username.isEmpty()) {
                    newRule = new ProxyRule(type, host, port, instanceId);
                } else {
                    newRule = new ProxyRule(type, host, port, instanceId, username, password);
                }
                networkService.addProxyRule(newRule);

                // Update table
                String instanceStr = instanceId == ProxyRule.ALL_INSTANCES ? "ALL" : "#" + instanceId;
                String authStr = newRule.hasAuthentication() ? "Yes" : "No";
                proxyTableModel.setValueAt(newRule.isEnabled(), selectedRow, 0);
                proxyTableModel.setValueAt(instanceStr, selectedRow, 1);
                proxyTableModel.setValueAt(type.name(), selectedRow, 2);
                proxyTableModel.setValueAt(host, selectedRow, 3);
                proxyTableModel.setValueAt(port, selectedRow, 4);
                proxyTableModel.setValueAt(authStr, selectedRow, 5);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid number format", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // NetworkChangeListener implementation
    @Override
    public void onLogAdded(ConnectionLog log) {
        SwingUtilities.invokeLater(() -> {
            logsList.add(log);
            addLogToTable(log);
        });
    }

    @Override
    public void onLogsCleared() {
        SwingUtilities.invokeLater(() -> {
            logsTableModel.setRowCount(0);
            logsList.clear();
        });
    }

    @Override
    public void onPacketLogAdded(PacketLog log) {
        SwingUtilities.invokeLater(() -> {
            addPacketLogToTable(log);
            updatePacketStats();
        });
    }

    @Override
    public void onPacketLogsCleared() {
        SwingUtilities.invokeLater(() -> {
            packetLogsTableModel.setRowCount(0);
            updatePacketStats();
        });
    }

    /**
     * Create a redirect rule from the selected log entry
     */
    private void createRedirectFromSelectedLog() {
        int selectedRow = logsTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= logsList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a log entry first", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ConnectionLog log = logsList.get(selectedRow);
        showAddRedirectionDialogWithValues(
                log.getInstanceId(),
                log.getOriginalHost(),
                log.getOriginalPort());
    }

    /**
     * Show add redirection dialog with pre-filled values
     */
    private void showAddRedirectionDialogWithValues(int instanceId, String origHost, int origPort) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));

        JTextField instanceField = new JTextField(String.valueOf(instanceId));
        JTextField origHostField = new JTextField(origHost);
        JTextField origPortField = new JTextField(String.valueOf(origPort));
        JTextField targetHostField = new JTextField();
        JTextField targetPortField = new JTextField(String.valueOf(origPort));

        panel.add(new JLabel("Instance ID (-1 for all):"));
        panel.add(instanceField);
        panel.add(new JLabel("Original Host:"));
        panel.add(origHostField);
        panel.add(new JLabel("Original Port:"));
        panel.add(origPortField);
        panel.add(new JLabel("Target Host:"));
        panel.add(targetHostField);
        panel.add(new JLabel("Target Port:"));
        panel.add(targetPortField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Create Redirection Rule from Log",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int instId = Integer.parseInt(instanceField.getText().trim());
                String oHost = origHostField.getText().trim();
                int oPort = Integer.parseInt(origPortField.getText().trim());
                String targetHost = targetHostField.getText().trim();
                int targetPort = Integer.parseInt(targetPortField.getText().trim());

                if (oHost.isEmpty() || targetHost.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Host cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                RedirectionRule rule = new RedirectionRule(oHost, oPort, targetHost, targetPort, instId);
                networkService.addRedirectionRule(rule);
                addRedirectionRuleToTable(rule);
                JOptionPane.showMessageDialog(this, "Redirection rule created successfully", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid number format", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void dispose() {
        networkService.removeListener(this);
        super.dispose();
    }
}
