package me.kitakeyos.j2me.domain.network.service;

import me.kitakeyos.j2me.domain.network.model.ConnectionLog;
import me.kitakeyos.j2me.domain.network.model.PacketLog;
import me.kitakeyos.j2me.domain.network.model.ProxyRule;
import me.kitakeyos.j2me.domain.network.model.RedirectionRule;
import me.kitakeyos.j2me.domain.network.model.SocketRoute;
import me.kitakeyos.j2me.domain.network.model.SocketRule;
import me.kitakeyos.j2me.domain.network.model.SocketTap;
import me.kitakeyos.j2me.domain.network.repository.NetworkRuleRepository;
import me.kitakeyos.j2me.domain.network.repository.SocketOpener;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Entry point for network interception.
 * <p>
 * This used to hold rules, logs, taps, statistics, socket construction and
 * properties-file I/O in one class. Those now live in {@link NetworkRuleStore},
 * {@link NetworkActivityLog}, {@link SocketTapRegistry} and
 * {@link SocketConnector}; what remains here is composition plus the delegating
 * surface that the instrumented bytecode and the monitor UI call.
 * <p>
 * Still a singleton because ASM-injected call sites reach it statically. It
 * starts with no persistence and a null-safe opener, and the composition root
 * calls {@link #configure} to supply the real ones — so nothing here depends on
 * the launcher window.
 */
public class NetworkService {

    private static final Logger logger = Logger.getLogger(NetworkService.class.getName());

    private static final NetworkService INSTANCE = new NetworkService();

    private final NetworkEvents events = new NetworkEvents();
    private final NetworkActivityLog activityLog = new NetworkActivityLog(events);
    private final SocketTapRegistry tapRegistry = new SocketTapRegistry();

    private volatile NetworkRuleStore ruleStore;
    private volatile SocketConnector connector;

    private NetworkService() {
        // Usable before configure(): rules are in memory only and connections
        // go straight out, so an un-wired test or early startup still works.
        configure(new InMemoryRuleRepository(), new DirectSocketOpener());
    }

    public static NetworkService getInstance() {
        return INSTANCE;
    }

    /**
     * Install the real persistence and socket implementations, then load the
     * stored rules. Called once by the composition root.
     */
    public void configure(NetworkRuleRepository repository, SocketOpener opener) {
        this.ruleStore = new NetworkRuleStore(repository, events);
        this.connector = new SocketConnector(ruleStore, activityLog, opener);
        try {
            ruleStore.load();
        } catch (RuntimeException e) {
            logger.severe("Failed to load network rules: " + e.getMessage());
        }
    }

    // === Collaborators, for callers that want the narrow surface ===

    public NetworkRuleStore rules() {
        return ruleStore;
    }

    public NetworkActivityLog activity() {
        return activityLog;
    }

    public SocketTapRegistry taps() {
        return tapRegistry;
    }

    // === Redirection Rules ===

    public void addRedirectionRule(RedirectionRule rule) {
        ruleStore.addRedirectionRule(rule);
    }

    public void removeRedirectionRule(RedirectionRule rule) {
        ruleStore.removeRedirectionRule(rule);
    }

    public List<RedirectionRule> getRedirectionRules() {
        return ruleStore.getRedirectionRules();
    }

    public void clearRedirectionRules() {
        ruleStore.clearRedirectionRules();
    }

    // === Proxy Rules ===

    public void addProxyRule(ProxyRule rule) {
        ruleStore.addProxyRule(rule);
    }

    public void removeProxyRule(ProxyRule rule) {
        ruleStore.removeProxyRule(rule);
    }

    public List<ProxyRule> getProxyRules() {
        return ruleStore.getProxyRules();
    }

    public void clearProxyRules() {
        ruleStore.clearProxyRules();
    }

    public void saveRules() {
        ruleStore.save();
    }

    // === Connection Logs ===

    public void addConnectionLog(ConnectionLog log) {
        activityLog.addConnectionLog(log);
    }

    public List<ConnectionLog> getConnectionLogs() {
        return activityLog.getConnectionLogs();
    }

    public void clearConnectionLogs() {
        activityLog.clearConnectionLogs();
    }

    // === Tapping Control (per-instance on/off) ===

    public void enableTapping(int instanceId) {
        tapRegistry.enableTapping(instanceId);
    }

    public void disableTapping(int instanceId) {
        tapRegistry.disableTapping(instanceId);
    }

    public boolean isTappingEnabled(int instanceId) {
        return tapRegistry.isTappingEnabled(instanceId);
    }

    // === Packet Logs ===

    /**
     * Record a packet, but only for instances the user has enabled capture on.
     */
    public void addPacketLog(PacketLog log) {
        if (!tapRegistry.isTappingEnabled(log.getInstanceId())) {
            return;
        }
        activityLog.addPacketLog(log);
    }

    public List<PacketLog> getPacketLogs() {
        return activityLog.getPacketLogs();
    }

    public void clearPacketLogs() {
        activityLog.clearPacketLogs();
    }

    public long getTotalBytesSent() {
        return activityLog.getTotalBytesSent();
    }

    public long getTotalBytesReceived() {
        return activityLog.getTotalBytesReceived();
    }

    // === Socket Taps (stream-based data access) ===

    public SocketTap getOrCreateTap(int socketId, int instanceId, String host, int port) {
        return tapRegistry.getOrCreateTap(socketId, instanceId, host, port);
    }

    public SocketTap getTap(int socketId) {
        return tapRegistry.getTap(socketId);
    }

    public List<SocketTap> getTapsByInstance(int instanceId) {
        return tapRegistry.getTapsByInstance(instanceId);
    }

    public Map<Integer, SocketTap> getAllTaps() {
        return tapRegistry.getAllTaps();
    }

    public void removeTap(int socketId) {
        tapRegistry.removeTap(socketId);
    }

    /**
     * Remove all logs and taps associated with a specific instance.
     * Called during instance shutdown to prevent memory leaks.
     */
    public void removeInstanceData(int instanceId) {
        activityLog.removeInstanceData(instanceId);
        tapRegistry.closeTapsOf(instanceId);
        logger.info("Cleaned up network data for instance #" + instanceId);
    }

    // === Socket Creation ===

    /**
     * Create a socket with redirection and proxy support.
     * This method is called by SystemCallHandler.
     */
    public Socket createSocket(int instanceId, String host, int port) throws IOException {
        return connector.connect(instanceId, host, port);
    }

    /**
     * Resolve what a connection would do without opening it.
     */
    public SocketRoute resolveRoute(int instanceId, String host, int port) {
        return connector.resolve(instanceId, host, port);
    }

    // === Listeners ===

    public void addListener(NetworkChangeListener listener) {
        events.addListener(listener);
    }

    public void removeListener(NetworkChangeListener listener) {
        events.removeListener(listener);
    }

    /**
     * Placeholder persistence used until {@link #configure} runs.
     */
    private static class InMemoryRuleRepository implements NetworkRuleRepository {
        @Override
        public List<SocketRule> loadAll() {
            return Collections.emptyList();
        }

        @Override
        public void saveAll(List<SocketRule> rules) {
        }
    }

    /**
     * Placeholder opener used until {@link #configure} runs: honours the
     * resolved target but knows nothing about proxies.
     */
    private static class DirectSocketOpener implements SocketOpener {
        @Override
        public Socket open(SocketRoute route) throws IOException {
            return new Socket(route.getTargetHost(), route.getTargetPort());
        }
    }
}
