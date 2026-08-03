package me.kitakeyos.j2me.presentation.common.format;

/**
 * Renders byte counts for display.
 * <p>
 * Lives in Presentation: how a number reads to a user is not a domain concern,
 * and keeping it here means the network services stay free of formatting.
 */
public final class ByteFormat {

    private static final long KB = 1024L;
    private static final long MB = KB * 1024L;

    private ByteFormat() {
    }

    /**
     * @return the count as B, KB or MB, whichever keeps it readable
     */
    public static String humanReadable(long bytes) {
        if (bytes < KB) {
            return bytes + " B";
        }
        if (bytes < MB) {
            return String.format("%.1f KB", bytes / (double) KB);
        }
        return String.format("%.2f MB", bytes / (double) MB);
    }

    /**
     * @return a "Sent: x | Received: y" summary line
     */
    public static String trafficSummary(long bytesSent, long bytesReceived) {
        return String.format("Sent: %s | Received: %s",
                humanReadable(bytesSent), humanReadable(bytesReceived));
    }
}
