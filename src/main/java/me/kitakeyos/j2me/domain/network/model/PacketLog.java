package me.kitakeyos.j2me.domain.network.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a captured network packet for monitoring.
 */
public class PacketLog {

    public enum Direction {
        IN, // Data received
        OUT // Data sent
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final int instanceId;
    private final Direction direction;
    private final LocalDateTime timestamp;
    private final String host;
    private final int port;
    private final byte[] data;
    private final int length;

    public PacketLog(int instanceId, Direction direction, String host, int port, byte[] data, int offset, int length) {
        this.instanceId = instanceId;
        this.direction = direction;
        this.timestamp = LocalDateTime.now();
        this.host = host;
        this.port = port;
        this.length = length;

        // Copy only the relevant portion of data
        this.data = new byte[length];
        System.arraycopy(data, offset, this.data, 0, length);
    }

    public int getInstanceId() {
        return instanceId;
    }

    public Direction getDirection() {
        return direction;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return length;
    }

    /**
     * Get hex representation of data (first 32 bytes max)
     */
    public String getHexPreview() {
        StringBuilder sb = new StringBuilder();
        int previewLength = Math.min(data.length, 32);
        for (int i = 0; i < previewLength; i++) {
            sb.append(String.format("%02X ", data[i] & 0xFF));
        }
        if (data.length > 32) {
            sb.append("...");
        }
        return sb.toString().trim();
    }

    /**
     * Get ASCII representation of data (printable chars only)
     */
    public String getAsciiPreview() {
        StringBuilder sb = new StringBuilder();
        int previewLength = Math.min(data.length, 64);
        for (int i = 0; i < previewLength; i++) {
            char c = (char) (data[i] & 0xFF);
            sb.append(c >= 32 && c < 127 ? c : '.');
        }
        if (data.length > 64) {
            sb.append("...");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("[%s] #%d %s %s:%d (%d bytes)",
                getFormattedTimestamp(), instanceId,
                direction == Direction.IN ? "<<" : ">>",
                host, port, length);
    }
}
