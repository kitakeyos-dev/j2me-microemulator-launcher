package me.kitakeyos.j2me.presentation.monitor;

import me.kitakeyos.j2me.infrastructure.monitoring.SystemMonitorService;
import me.kitakeyos.j2me.presentation.common.i18n.Messages;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

public class SystemMonitorDialog extends JDialog {

    private final SystemMonitorService monitorService;
    private final Timer timer;

    private JLabel heapLabel;
    private JLabel nonHeapLabel;
    private JLabel threadLabel;
    private JLabel cpuLabel;
    private JLabel physicalMemLabel;

    private JProgressBar heapBar;
    private JProgressBar cpuBar;

    private final DecimalFormat df = new DecimalFormat("#.##");

    public SystemMonitorDialog(Frame owner) {
        super(owner, Messages.get("sysmon.title"), false);
        setResizable(false);
        this.monitorService = new SystemMonitorService();
        this.timer = new Timer(true);

        // Compact size
        setSize(350, 280);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        initComponents();
        startMonitoring();
    }

    private void initComponents() {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        // Row 0: Heap
        addLabel(contentPanel, Messages.get("sysmon.heapRam"), gbc, 0, 0);
        heapLabel = new JLabel(Messages.get("common.loading"));
        addControl(contentPanel, heapLabel, gbc, 1, 0);

        heapBar = new JProgressBar(0, 100);
        heapBar.setPreferredSize(new Dimension(100, 16));
        heapBar.setStringPainted(true);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        contentPanel.add(heapBar, gbc);

        // Row 2: Non-Heap
        gbc.gridwidth = 1;
        addLabel(contentPanel, Messages.get("sysmon.nonHeap"), gbc, 0, 2);
        nonHeapLabel = new JLabel(Messages.get("common.loading"));
        addControl(contentPanel, nonHeapLabel, gbc, 1, 2);

        // Row 3: CPU
        addLabel(contentPanel, Messages.get("sysmon.cpuLoad"), gbc, 0, 3);
        cpuLabel = new JLabel(Messages.get("common.loading"));
        addControl(contentPanel, cpuLabel, gbc, 1, 3);

        cpuBar = new JProgressBar(0, 100);
        cpuBar.setPreferredSize(new Dimension(100, 16));
        cpuBar.setStringPainted(true);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        contentPanel.add(cpuBar, gbc);

        // Row 5: Threads
        gbc.gridwidth = 1;
        addLabel(contentPanel, Messages.get("sysmon.threads"), gbc, 0, 5);
        threadLabel = new JLabel(Messages.get("common.loading"));
        addControl(contentPanel, threadLabel, gbc, 1, 5);

        // Row 6: Physical
        addLabel(contentPanel, Messages.get("sysmon.physicalRam"), gbc, 0, 6);
        physicalMemLabel = new JLabel(Messages.get("common.loading"));
        addControl(contentPanel, physicalMemLabel, gbc, 1, 6);

        // Footer: GC Button
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        JButton gcButton = new JButton(Messages.get("sysmon.gcButton"));
        gcButton.addActionListener(e -> {
            System.gc();
            JOptionPane.showMessageDialog(this, Messages.get("sysmon.gcRequested"), Messages.get("sysmon.system"),
                    JOptionPane.INFORMATION_MESSAGE);
        });
        footerPanel.add(gcButton);

        add(contentPanel, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private void addLabel(JPanel panel, String text, GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.weightx = 0.0;
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label, gbc);
    }

    private void addControl(JPanel panel, Component comp, GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.weightx = 1.0;
        panel.add(comp, gbc);
    }

    private void startMonitoring() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> updateMetrics());
            }
        }, 0, 1000); // 1s
    }

    private void updateMetrics() {
        // Heap
        long usedHeap = monitorService.getUsedHeapMemory();
        long maxHeap = monitorService.getMaxHeapMemory();
        long usedHeapMb = usedHeap / 1024 / 1024;
        long maxHeapMb = maxHeap / 1024 / 1024;

        heapLabel.setText(String.format("%d / %d MB", usedHeapMb, maxHeapMb));
        int heapPercent = (int) ((usedHeap * 100) / maxHeap);
        heapBar.setValue(heapPercent);
        heapBar.setString(heapPercent + "%");
        heapBar.setForeground(heapPercent > 80 ? Color.RED : new Color(34, 139, 34)); // Forest Green

        // Non-Heap
        long usedNonHeap = monitorService.getUsedNonHeapMemory();
        nonHeapLabel.setText((usedNonHeap / 1024 / 1024) + " MB");

        // CPU
        double cpuLoad = monitorService.getProcessCpuLoad();
        if (cpuLoad >= 0) {
            cpuLabel.setText(df.format(cpuLoad) + "%");
            cpuBar.setValue((int) cpuLoad);
            cpuBar.setString(df.format(cpuLoad) + "%");
            cpuBar.setForeground(cpuLoad > 80 ? Color.RED : new Color(34, 139, 34));
        } else {
            cpuLabel.setText(Messages.get("common.na"));
            cpuBar.setValue(0);
            cpuBar.setString(Messages.get("common.na"));
        }

        // Threads
        threadLabel.setText(String.valueOf(monitorService.getThreadCount()));

        // Physical
        long freePhys = monitorService.getFreePhysicalMemory();
        long totalPhys = monitorService.getTotalPhysicalMemory();
        if (totalPhys > 0) {
            double freeGb = freePhys / 1024.0 / 1024.0 / 1024.0;
            double totalGb = totalPhys / 1024.0 / 1024.0 / 1024.0;
            physicalMemLabel.setText(String.format("%.1f / %.1f GB", (totalGb - freeGb), totalGb));
        } else {
            physicalMemLabel.setText(Messages.get("common.na"));
        }
    }

    @Override
    public void dispose() {
        timer.cancel();
        super.dispose();
    }
}
