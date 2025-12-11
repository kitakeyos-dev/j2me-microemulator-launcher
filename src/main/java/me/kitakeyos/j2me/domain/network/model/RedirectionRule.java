package me.kitakeyos.j2me.domain.network.model;

/**
 * Represents a redirection rule for socket connections.
 * When a connection matches originalHost:originalPort, it will be redirected to
 * targetHost:targetPort.
 */
public class RedirectionRule {

    public static final int ALL_INSTANCES = -1;

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

    public int getInstanceId() {
        return instanceId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
    public String toString() {
        String scope = instanceId == ALL_INSTANCES ? "ALL" : "#" + instanceId;
        return String.format("[%s] %s:%d -> %s:%d", scope, originalHost, originalPort, targetHost, targetPort);
    }
}
