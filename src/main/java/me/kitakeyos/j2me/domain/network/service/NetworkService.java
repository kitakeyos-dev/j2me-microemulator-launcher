package me.kitakeyos.j2me.domain.network.service;

import me.kitakeyos.j2me.domain.network.model.ConnectionLog;
import me.kitakeyos.j2me.domain.network.model.PacketLog;
import me.kitakeyos.j2me.domain.network.model.ProxyRule;
import me.kitakeyos.j2me.domain.network.model.RedirectionRule;
import me.kitakeyos.j2me.domain.network.model.SocketTap;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Service to manage network redirection rules, proxy rules, and connection
 * logs.
 * This is a singleton class accessible by SystemCallHandler and UI.
 */
public class NetworkService {

    private static final Logger logger = Logger.getLogger(NetworkService.class.getName());
    private static final String NETWORK_RULES_FILE = "network_rules.properties";

    private static final NetworkService INSTANCE = new NetworkService();

    private final List<RedirectionRule> redirectionRules = new CopyOnWriteArrayList<>();
    private final List<ProxyRule> proxyRules = new CopyOnWriteArrayList<>();
    private final List<ConnectionLog> connectionLogs = new CopyOnWriteArrayList<>();
    private final List<NetworkChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final List<PacketLog> packetLogs = new CopyOnWriteArrayList<>();

    // Socket taps: socketId → SocketTap (stream-based data access)
    private final Map<Integer, SocketTap> socketTaps = new ConcurrentHashMap<>();

    // Instances with tapping/packet logging enabled (off by default)
    private final java.util.Set<Integer> tappingEnabledInstances = ConcurrentHashMap.newKeySet();

    private static final int MAX_LOG_SIZE = 1000;
    private static final int MAX_PACKET_LOG_SIZE = 5000;

    // Statistics
    private long totalBytesSent = 0;
    private long totalBytesReceived = 0;

    private NetworkService() {
        // Auto-load rules on startup
        loadRules();
    }

    public static NetworkService getInstance() {
        return INSTANCE;
    }

    // === Redirection Rules ===

    public void addRedirectionRule(RedirectionRule rule) {
        redirectionRules.add(rule);
        onRulesModified();
    }

    public void removeRedirectionRule(RedirectionRule rule) {
        redirectionRules.remove(rule);
        onRulesModified();
    }

    public List<RedirectionRule> getRedirectionRules() {
        return Collections.unmodifiableList(redirectionRules);
    }

    public void clearRedirectionRules() {
        redirectionRules.clear();
        // Don't auto-save on clear (used before load)
        notifyRulesChanged();
    }

    // === Proxy Rules ===

    public void addProxyRule(ProxyRule rule) {
        proxyRules.add(rule);
        onRulesModified();
    }

    public void removeProxyRule(ProxyRule rule) {
        proxyRules.remove(rule);
        onRulesModified();
    }

    public List<ProxyRule> getProxyRules() {
        return Collections.unmodifiableList(proxyRules);
    }

    public void clearProxyRules() {
        proxyRules.clear();
        // Don't auto-save on clear (used before load)
        notifyRulesChanged();
    }

    /**
     * Called when rules are modified - notifies listeners and auto-saves
     */
    private void onRulesModified() {
        notifyRulesChanged();
        saveRules();
    }

    // === Connection Logs ===

    public void addConnectionLog(ConnectionLog log) {
        connectionLogs.add(log);
        // Trim if too large
        while (connectionLogs.size() > MAX_LOG_SIZE) {
            connectionLogs.remove(0);
        }
        notifyLogAdded(log);
    }

    public List<ConnectionLog> getConnectionLogs() {
        return Collections.unmodifiableList(connectionLogs);
    }

    public void clearConnectionLogs() {
        connectionLogs.clear();
        notifyLogsCleared();
    }

    // === Tapping Control (per-instance on/off) ===

    /**
     * Enable packet logging and stream tapping for an instance.
     * Must be called before data will be captured.
     */
    public void enableTapping(int instanceId) {
        tappingEnabledInstances.add(instanceId);
        logger.info("Tapping enabled for instance #" + instanceId);
    }

    /**
     * Disable tapping for an instance and close all its taps.
     */
    public void disableTapping(int instanceId) {
        tappingEnabledInstances.remove(instanceId);
        // Close existing taps for this instance
        List<Integer> toRemove = new ArrayList<>();
        for (Map.Entry<Integer, SocketTap> entry : socketTaps.entrySet()) {
            if (entry.getValue().getInstanceId() == instanceId) {
                toRemove.add(entry.getKey());
            }
        }
        for (int socketId : toRemove) {
            removeTap(socketId);
        }
        logger.info("Tapping disabled for instance #" + instanceId);
    }

    /**
     * Check if tapping is enabled for an instance.
     */
    public boolean isTappingEnabled(int instanceId) {
        return tappingEnabledInstances.contains(instanceId);
    }

    // === Packet Logs ===

    /**
     * Add a packet log entry. Only logs if tapping is enabled for the instance.
     */
    public void addPacketLog(PacketLog log) {
        if (!tappingEnabledInstances.contains(log.getInstanceId())) {
            return;
        }

        packetLogs.add(log);

        // Update statistics
        if (log.getDirection() == PacketLog.Direction.OUT) {
            totalBytesSent += log.getLength();
        } else {
            totalBytesReceived += log.getLength();
        }

        // Trim if too large
        while (packetLogs.size() > MAX_PACKET_LOG_SIZE) {
            packetLogs.remove(0);
        }
        notifyPacketLogAdded(log);
    }

    public List<PacketLog> getPacketLogs() {
        return Collections.unmodifiableList(packetLogs);
    }

    public void clearPacketLogs() {
        packetLogs.clear();
        totalBytesSent = 0;
        totalBytesReceived = 0;
        notifyPacketLogsCleared();
    }

    // === Socket Taps (stream-based data access) ===

    /**
     * Get or create a SocketTap for the given socket.
     * Only creates if tapping is enabled for the instance.
     * Returns null if tapping is disabled.
     */
    public SocketTap getOrCreateTap(int socketId, int instanceId, String host, int port) {
        if (!tappingEnabledInstances.contains(instanceId)) {
            return null;
        }
        return socketTaps.computeIfAbsent(socketId, k -> new SocketTap(socketId, instanceId, host, port));
    }

    /**
     * Get an existing SocketTap by socketId.
     * Returns null if no tap exists for this socket.
     */
    public SocketTap getTap(int socketId) {
        return socketTaps.get(socketId);
    }

    /**
     * Get all active taps for a specific instance.
     */
    public List<SocketTap> getTapsByInstance(int instanceId) {
        List<SocketTap> result = new ArrayList<>();
        for (SocketTap tap : socketTaps.values()) {
            if (tap.getInstanceId() == instanceId) {
                result.add(tap);
            }
        }
        return result;
    }

    /**
     * Get all active taps.
     */
    public Map<Integer, SocketTap> getAllTaps() {
        return Collections.unmodifiableMap(socketTaps);
    }

    /**
     * Remove and close a tap for a specific socket.
     */
    public void removeTap(int socketId) {
        SocketTap tap = socketTaps.remove(socketId);
        if (tap != null) {
            tap.close();
        }
    }

    public long getTotalBytesSent() {
        return totalBytesSent;
    }

    public long getTotalBytesReceived() {
        return totalBytesReceived;
    }

    /**
     * Remove all logs and taps associated with a specific instance.
     * Called during instance shutdown to prevent memory leaks.
     */
    public void removeInstanceData(int instanceId) {
        // Remove connection logs for this instance
        connectionLogs.removeIf(log -> log.getInstanceId() == instanceId);

        // Remove packet logs for this instance and adjust statistics
        long sentReduction = 0;
        long receivedReduction = 0;
        for (PacketLog log : packetLogs) {
            if (log.getInstanceId() == instanceId) {
                if (log.getDirection() == PacketLog.Direction.OUT) {
                    sentReduction += log.getLength();
                } else {
                    receivedReduction += log.getLength();
                }
            }
        }
        packetLogs.removeIf(log -> log.getInstanceId() == instanceId);
        totalBytesSent -= sentReduction;
        totalBytesReceived -= receivedReduction;

        // Close and remove all taps for this instance
        List<Integer> tapIdsToRemove = new ArrayList<>();
        for (Map.Entry<Integer, SocketTap> entry : socketTaps.entrySet()) {
            if (entry.getValue().getInstanceId() == instanceId) {
                tapIdsToRemove.add(entry.getKey());
            }
        }
        for (int socketId : tapIdsToRemove) {
            removeTap(socketId);
        }

        logger.info("Cleaned up network data for instance #" + instanceId);
    }

    public String getFormattedStats() {
        return String.format("Sent: %s | Received: %s",
                formatBytes(totalBytesSent), formatBytes(totalBytesReceived));
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    // === Socket Creation with Redirection and Proxy ===

    /**
     * Create a socket with redirection and proxy support.
     * This method is called by SystemCallHandler.
     */
    public Socket createSocket(int instanceId, String host, int port) throws IOException {
        String actualHost = host;
        int actualPort = port;
        ProxyRule activeProxy = null;
        String proxyInfo = null;
        boolean success = false;
        String errorMessage = null;

        try {
            // Step 1: Check for redirection rules
            for (RedirectionRule rule : redirectionRules) {
                if (rule.matches(instanceId, host, port)) {
                    actualHost = rule.getTargetHost();
                    actualPort = rule.getTargetPort();
                    break;
                }
            }

            // Step 2: Check for proxy rules
            for (ProxyRule rule : proxyRules) {
                if (rule.appliesTo(instanceId)) {
                    activeProxy = rule;
                    break;
                }
            }

            // Step 3: Create socket
            Socket socket;
            if (activeProxy != null) {
                proxyInfo = activeProxy.getProxyType() + " " + activeProxy.getProxyHost() + ":"
                        + activeProxy.getProxyPort();
                if (activeProxy.hasAuthentication()) {
                    proxyInfo += " (auth)";
                }

                // Set up authentication if required
                if (activeProxy.hasAuthentication()) {
                    final String proxyUser = activeProxy.getUsername();
                    final String proxyPass = activeProxy.getPassword();
                    java.net.Authenticator.setDefault(new java.net.Authenticator() {
                        @Override
                        protected java.net.PasswordAuthentication getPasswordAuthentication() {
                            return new java.net.PasswordAuthentication(proxyUser, proxyPass.toCharArray());
                        }
                    });
                }

                Proxy proxy = new Proxy(
                        activeProxy.getJavaProxyType(),
                        new InetSocketAddress(activeProxy.getProxyHost(), activeProxy.getProxyPort()));
                socket = new Socket(proxy);
                socket.connect(new InetSocketAddress(actualHost, actualPort));
            } else {
                socket = new Socket(actualHost, actualPort);
            }

            success = true;
            return socket;

        } catch (IOException e) {
            errorMessage = e.getMessage();
            throw e;
        } finally {
            // Log the connection attempt
            ConnectionLog log = new ConnectionLog(
                    instanceId, host, port, actualHost, actualPort, proxyInfo, success, errorMessage);
            addConnectionLog(log);
        }
    }

    // === Listeners ===

    public void addListener(NetworkChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(NetworkChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyRulesChanged() {
        for (NetworkChangeListener listener : listeners) {
            listener.onRulesChanged();
        }
    }

    private void notifyLogAdded(ConnectionLog log) {
        for (NetworkChangeListener listener : listeners) {
            listener.onLogAdded(log);
        }
    }

    private void notifyLogsCleared() {
        for (NetworkChangeListener listener : listeners) {
            listener.onLogsCleared();
        }
    }

    private void notifyPacketLogAdded(PacketLog log) {
        for (NetworkChangeListener listener : listeners) {
            listener.onPacketLogAdded(log);
        }
    }

    private void notifyPacketLogsCleared() {
        for (NetworkChangeListener listener : listeners) {
            listener.onPacketLogsCleared();
        }
    }

    // === Persistence ===

    private java.io.File getConfigFile() {
        java.io.File dataDir = me.kitakeyos.j2me.application.MainApplication.INSTANCE.getApplicationConfig()
                .getDataDirectory();
        return new java.io.File(dataDir, NETWORK_RULES_FILE);
    }

    /**
     * Save all rules to properties file
     */
    /**
     * Save all rules to properties file
     */
    public void saveRules() {
        java.util.Properties props = new java.util.Properties();

        // Save redirection rules count
        props.setProperty("redirect.count", String.valueOf(redirectionRules.size()));
        for (int i = 0; i < redirectionRules.size(); i++) {
            RedirectionRule rule = redirectionRules.get(i);
            String prefix = "redirect." + i + ".";
            props.setProperty(prefix + "originalHost", rule.getOriginalHost());
            props.setProperty(prefix + "originalPort", String.valueOf(rule.getOriginalPort()));
            props.setProperty(prefix + "targetHost", rule.getTargetHost());
            props.setProperty(prefix + "targetPort", String.valueOf(rule.getTargetPort()));
            props.setProperty(prefix + "instanceId", String.valueOf(rule.getInstanceId()));
            props.setProperty(prefix + "enabled", String.valueOf(rule.isEnabled()));
        }

        // Save proxy rules count
        props.setProperty("proxy.count", String.valueOf(proxyRules.size()));
        for (int i = 0; i < proxyRules.size(); i++) {
            ProxyRule rule = proxyRules.get(i);
            String prefix = "proxy." + i + ".";
            props.setProperty(prefix + "type", rule.getProxyType().name());
            props.setProperty(prefix + "host", rule.getProxyHost());
            props.setProperty(prefix + "port", String.valueOf(rule.getProxyPort()));
            props.setProperty(prefix + "instanceId", String.valueOf(rule.getInstanceId()));
            props.setProperty(prefix + "enabled", String.valueOf(rule.isEnabled()));
            if (rule.getUsername() != null) {
                props.setProperty(prefix + "username", rule.getUsername());
            }
            if (rule.getPassword() != null) {
                props.setProperty(prefix + "password", rule.getPassword());
            }
        }

        // Write to file
        java.io.File configFile = getConfigFile();
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(configFile)) {
            props.store(fos, "Network Rules Configuration");
            logger.info("Network rules saved to " + configFile.getAbsolutePath());
        } catch (IOException e) {
            logger.severe("Failed to save network rules: " + e.getMessage());
        }
    }

    /**
     * Load all rules from properties file
     */
    private void loadRules() {
        java.io.File configFile = getConfigFile();
        if (!configFile.exists()) {
            return;
        }

        java.util.Properties props = new java.util.Properties();
        try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile)) {
            props.load(fis);

            // Load redirection rules
            int redirectCount = Integer.parseInt(props.getProperty("redirect.count", "0"));
            for (int i = 0; i < redirectCount; i++) {
                String prefix = "redirect." + i + ".";
                try {
                    String originalHost = props.getProperty(prefix + "originalHost");
                    int originalPort = Integer.parseInt(props.getProperty(prefix + "originalPort", "0"));
                    String targetHost = props.getProperty(prefix + "targetHost");
                    int targetPort = Integer.parseInt(props.getProperty(prefix + "targetPort", "0"));
                    int instanceId = Integer.parseInt(props.getProperty(prefix + "instanceId", "-1"));
                    boolean enabled = Boolean.parseBoolean(props.getProperty(prefix + "enabled", "true"));

                    if (originalHost != null && targetHost != null) {
                        RedirectionRule rule = new RedirectionRule(originalHost, originalPort, targetHost, targetPort,
                                instanceId);
                        rule.setEnabled(enabled);
                        redirectionRules.add(rule);
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid entry
                }
            }

            // Load proxy rules
            int proxyCount = Integer.parseInt(props.getProperty("proxy.count", "0"));
            for (int i = 0; i < proxyCount; i++) {
                String prefix = "proxy." + i + ".";
                try {
                    String typeStr = props.getProperty(prefix + "type");
                    String host = props.getProperty(prefix + "host");
                    int port = Integer.parseInt(props.getProperty(prefix + "port", "0"));
                    int instanceId = Integer.parseInt(props.getProperty(prefix + "instanceId", "-1"));
                    boolean enabled = Boolean.parseBoolean(props.getProperty(prefix + "enabled", "true"));
                    String username = props.getProperty(prefix + "username");
                    String password = props.getProperty(prefix + "password");

                    if (typeStr != null && host != null) {
                        ProxyRule.ProxyType type = ProxyRule.ProxyType.valueOf(typeStr);
                        ProxyRule rule;
                        if (username != null && !username.isEmpty()) {
                            rule = new ProxyRule(type, host, port, instanceId, username, password);
                        } else {
                            rule = new ProxyRule(type, host, port, instanceId);
                        }
                        rule.setEnabled(enabled);
                        proxyRules.add(rule);
                    }
                } catch (Exception e) {
                    // Skip invalid entry
                }
            }

            logger.info("Loaded " + redirectionRules.size() + " redirection rules and " + proxyRules.size()
                    + " proxy rules");
        } catch (IOException | NumberFormatException e) {
            logger.severe("Failed to load network rules: " + e.getMessage());
        }
    }

    /**
     * Listener interface for network state changes
     */
    public interface NetworkChangeListener {
        default void onRulesChanged() {
        }

        default void onLogAdded(ConnectionLog log) {
        }

        default void onLogsCleared() {
        }

        default void onPacketLogAdded(PacketLog log) {
        }

        default void onPacketLogsCleared() {
        }
    }
}
