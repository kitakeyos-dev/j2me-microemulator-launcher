package me.kitakeyos.j2me.domain.network.model;

import java.net.Proxy;

/**
 * Represents a proxy rule for socket connections.
 */
public class ProxyRule {

    public static final int ALL_INSTANCES = -1;

    public enum ProxyType {
        SOCKS,
        HTTP
    }

    private final ProxyType proxyType;
    private final String proxyHost;
    private final int proxyPort;
    private final int instanceId; // -1 means apply to all instances
    private final String username; // nullable, for authentication
    private final String password; // nullable, for authentication
    private boolean enabled;

    public ProxyRule(ProxyType proxyType, String proxyHost, int proxyPort, int instanceId) {
        this(proxyType, proxyHost, proxyPort, instanceId, null, null);
    }

    public ProxyRule(ProxyType proxyType, String proxyHost, int proxyPort, int instanceId, String username,
            String password) {
        this.proxyType = proxyType;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.instanceId = instanceId;
        this.username = username;
        this.password = password;
        this.enabled = true;
    }

    public ProxyType getProxyType() {
        return proxyType;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public int getInstanceId() {
        return instanceId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean hasAuthentication() {
        return username != null && !username.isEmpty();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Check if this rule applies to the given instance
     */
    public boolean appliesTo(int instanceId) {
        if (!enabled)
            return false;
        return this.instanceId == ALL_INSTANCES || this.instanceId == instanceId;
    }

    /**
     * Get the Java Proxy.Type for this rule
     */
    public Proxy.Type getJavaProxyType() {
        return proxyType == ProxyType.SOCKS ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
    }

    @Override
    public String toString() {
        String scope = instanceId == ALL_INSTANCES ? "ALL" : "#" + instanceId;
        String auth = hasAuthentication() ? " (auth)" : "";
        return String.format("[%s] %s %s:%d%s", scope, proxyType, proxyHost, proxyPort, auth);
    }
}
