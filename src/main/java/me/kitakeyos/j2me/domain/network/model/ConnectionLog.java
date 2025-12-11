package me.kitakeyos.j2me.domain.network.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a logged socket connection attempt.
 */
public class ConnectionLog {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final int instanceId;
    private final String originalHost;
    private final int originalPort;
    private final String actualHost;
    private final int actualPort;
    private final String proxyInfo; // null if no proxy
    private final LocalDateTime timestamp;
    private final boolean success;
    private final String errorMessage;

    public ConnectionLog(int instanceId, String originalHost, int originalPort,
            String actualHost, int actualPort, String proxyInfo,
            boolean success, String errorMessage) {
        this.instanceId = instanceId;
        this.originalHost = originalHost;
        this.originalPort = originalPort;
        this.actualHost = actualHost;
        this.actualPort = actualPort;
        this.proxyInfo = proxyInfo;
        this.timestamp = LocalDateTime.now();
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public int getInstanceId() {
        return instanceId;
    }

    public String getOriginalHost() {
        return originalHost;
    }

    public int getOriginalPort() {
        return originalPort;
    }

    public String getActualHost() {
        return actualHost;
    }

    public int getActualPort() {
        return actualPort;
    }

    public String getProxyInfo() {
        return proxyInfo;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean wasRedirected() {
        return !originalHost.equals(actualHost) || originalPort != actualPort;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] #%d ", getFormattedTimestamp(), instanceId));
        sb.append(originalHost).append(":").append(originalPort);
        if (wasRedirected()) {
            sb.append(" -> ").append(actualHost).append(":").append(actualPort);
        }
        if (proxyInfo != null) {
            sb.append(" via ").append(proxyInfo);
        }
        sb.append(success ? " OK" : " FAILED: " + errorMessage);
        return sb.toString();
    }
}
