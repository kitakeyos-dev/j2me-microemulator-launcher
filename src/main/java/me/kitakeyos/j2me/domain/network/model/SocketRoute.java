package me.kitakeyos.j2me.domain.network.model;

import java.net.Proxy;

/**
 * The mutable plan for one outbound connection.
 * <p>
 * Starts out pointing at whatever the MIDlet asked for, then each matching
 * {@link SocketRule} rewrites part of it. Because rules only ever touch this
 * object, a new kind of rule needs no change to the connector.
 */
public class SocketRoute {

    private final int instanceId;
    private final String requestedHost;
    private final int requestedPort;

    private String targetHost;
    private int targetPort;

    private Proxy proxy;
    private String proxyDescription;
    private String proxyUsername;
    private String proxyPassword;

    public SocketRoute(int instanceId, String requestedHost, int requestedPort) {
        this.instanceId = instanceId;
        this.requestedHost = requestedHost;
        this.requestedPort = requestedPort;
        this.targetHost = requestedHost;
        this.targetPort = requestedPort;
    }

    public int getInstanceId() {
        return instanceId;
    }

    /**
     * @return the host the MIDlet originally asked for, never rewritten
     */
    public String getRequestedHost() {
        return requestedHost;
    }

    /**
     * @return the port the MIDlet originally asked for, never rewritten
     */
    public int getRequestedPort() {
        return requestedPort;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public int getTargetPort() {
        return targetPort;
    }

    /**
     * Point the connection somewhere else.
     */
    public void redirectTo(String host, int port) {
        this.targetHost = host;
        this.targetPort = port;
    }

    public Proxy getProxy() {
        return proxy;
    }

    /**
     * @return true once a rule has installed a proxy on this route
     */
    public boolean hasProxy() {
        return proxy != null;
    }

    public String getProxyDescription() {
        return proxyDescription;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public boolean hasProxyCredentials() {
        return proxyUsername != null && !proxyUsername.isEmpty();
    }

    /**
     * Route the connection through a proxy.
     *
     * @param proxy       the proxy to dial through
     * @param description human-readable form used in the connection log
     */
    public void useProxy(Proxy proxy, String description) {
        this.proxy = proxy;
        this.proxyDescription = description;
    }

    /**
     * Attach credentials for the proxy installed by {@link #useProxy}.
     */
    public void useProxyCredentials(String username, String password) {
        this.proxyUsername = username;
        this.proxyPassword = password;
    }

    /**
     * @return true when a rule pointed this route away from what was requested
     */
    public boolean isRedirected() {
        return targetPort != requestedPort || !targetHost.equals(requestedHost);
    }

    @Override
    public String toString() {
        return String.format("SocketRoute{#%d %s:%d -> %s:%d%s}",
                instanceId, requestedHost, requestedPort, targetHost, targetPort,
                hasProxy() ? " via " + proxyDescription : "");
    }
}
