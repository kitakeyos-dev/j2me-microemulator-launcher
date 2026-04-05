package me.kitakeyos.j2me.domain.network.model;

/**
 * Represents a tap on a monitored socket connection.
 * Holds two TapStreams: one for sent data, one for received data.
 * Injection code can read from these streams like normal InputStreams.
 *
 * Usage from injection code:
 *   SocketTap tap = NetworkService.getInstance().getTap(socketId);
 *   InputStream sent = tap.getSentStream();       // data app sent to server
 *   InputStream received = tap.getReceivedStream(); // data app received from server
 *   byte[] data = tap.drainSent();                // non-blocking: get all buffered sent data
 */
public class SocketTap {

    private final int socketId;
    private final int instanceId;
    private final String host;
    private final int port;
    private final TapStream sentStream;
    private final TapStream receivedStream;

    public SocketTap(int socketId, int instanceId, String host, int port) {
        this.socketId = socketId;
        this.instanceId = instanceId;
        this.host = host;
        this.port = port;
        this.sentStream = new TapStream();
        this.receivedStream = new TapStream();
    }

    public int getSocketId() {
        return socketId;
    }

    public int getInstanceId() {
        return instanceId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    /**
     * Get the stream of data SENT by the app (app → server).
     */
    public TapStream getSentStream() {
        return sentStream;
    }

    /**
     * Get the stream of data RECEIVED by the app (server → app).
     */
    public TapStream getReceivedStream() {
        return receivedStream;
    }

    /**
     * Non-blocking: drain all buffered sent data.
     */
    public byte[] drainSent() {
        return sentStream.drain();
    }

    /**
     * Non-blocking: drain all buffered received data.
     */
    public byte[] drainReceived() {
        return receivedStream.drain();
    }

    /**
     * Non-blocking: peek at buffered sent data without consuming.
     */
    public byte[] peekSent() {
        return sentStream.peek();
    }

    /**
     * Non-blocking: peek at buffered received data without consuming.
     */
    public byte[] peekReceived() {
        return receivedStream.peek();
    }

    /**
     * Close both tap streams.
     */
    public void close() {
        sentStream.close();
        receivedStream.close();
    }

    /**
     * Check if this tap is closed.
     */
    public boolean isClosed() {
        return sentStream.isClosed() && receivedStream.isClosed();
    }

    @Override
    public String toString() {
        return String.format("SocketTap[#%d:%d → %s:%d]", instanceId, socketId, host, port);
    }
}
