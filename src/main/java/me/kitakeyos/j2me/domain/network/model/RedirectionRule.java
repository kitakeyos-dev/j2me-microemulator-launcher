package me.kitakeyos.j2me.domain.network.model;

/**
 * Represents a redirection rule for socket connections.
 * When a connection matches originalHost:originalPort, it will be redirected to
 * targetHost:targetPort.
 */
public class RedirectionRule implements SocketRule {

    /**
     * Group and order for every address-rewriting rule. Redirection runs before
     * proxy selection so the proxy sees the final destination.
     */
    public static final String GROUP = "address";
    public static final int ORDER = 100;

    private final String originalHost;
    private final int originalPort;
    private final String targetHost;
    private final int targetPort;
    private final int instanceId; // -1 means apply to all instances
    private boolean enabled;

    public RedirectionRule(String originalHost, int originalPort, String targetHost, int targetPort, int instanceId) {
        this.originalHost = originalHost;
        this.originalPort = originalPort;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.instanceId = instanceId;
        this.enabled = true;
    }

    public String getOriginalHost() {
        return originalHost;
    }

    public int getOriginalPort() {
        return originalPort;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public int getTargetPort() {
        return targetPort;
    }

    @Override
    public int getInstanceId() {
        return instanceId;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String group() {
        return GROUP;
    }

    @Override
    public int order() {
        return ORDER;
    }

    /**
     * Check if this rule applies to the given instance and host:port
     */
    public boolean matches(int instanceId, String host, int port) {
        if (!enabled)
            return false;
        if (this.instanceId != ALL_INSTANCES && this.instanceId != instanceId)
            return false;
        return originalHost.equalsIgnoreCase(host) && originalPort == port;
    }

    @Override
    public boolean matches(SocketRoute route) {
        // Matched against the requested address, so chained redirections cannot
        // bounce a connection through a second hop.
        return matches(route.getInstanceId(), route.getRequestedHost(), route.getRequestedPort());
    }

    @Override
    public void apply(SocketRoute route) {
        route.redirectTo(targetHost, targetPort);
    }

    @Override
    public String toString() {
        String scope = instanceId == ALL_INSTANCES ? "ALL" : "#" + instanceId;
        return String.format("[%s] %s:%d -> %s:%d", scope, originalHost, originalPort, targetHost, targetPort);
    }
}
