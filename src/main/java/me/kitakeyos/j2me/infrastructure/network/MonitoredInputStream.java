package me.kitakeyos.j2me.infrastructure.network;

import me.kitakeyos.j2me.domain.network.model.PacketLog;
import me.kitakeyos.j2me.domain.network.model.SocketTap;
import me.kitakeyos.j2me.domain.network.service.NetworkService;


import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapped InputStream that monitors all read operations.
 * Data is both logged as PacketLog and pushed to the SocketTap for streaming access.
 */
public class MonitoredInputStream extends InputStream {

    private final InputStream wrapped;
    private final int instanceId;
    private final int socketId;
    private final String host;
    private final int port;

    public MonitoredInputStream(InputStream wrapped, int instanceId, int socketId, String host, int port) {
        this.wrapped = wrapped;
        this.instanceId = instanceId;
        this.socketId = socketId;
        this.host = host;
        this.port = port;
    }

    @Override
    public int read() throws IOException {
        int b = wrapped.read();
        if (b != -1) {
            byte[] data = new byte[]{(byte) b};
            onDataReceived(data, 0, 1);
        }
        return b;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int len = wrapped.read(b);
        if (len > 0) {
            onDataReceived(b, 0, len);
        }
        return len;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = wrapped.read(b, off, len);
        if (bytesRead > 0) {
            onDataReceived(b, off, bytesRead);
        }
        return bytesRead;
    }

    private void onDataReceived(byte[] data, int offset, int length) {
        NetworkService ns = NetworkService.getInstance();
        logPacket(data, offset, length);
        SocketTap tap = ns.getOrCreateTap(socketId, instanceId, host, port);
        if (tap != null) {
            tap.getReceivedStream().push(data, offset, length);
        }
    }

    private void logPacket(byte[] data, int offset, int length) {
        PacketLog log = new PacketLog(instanceId, socketId, PacketLog.Direction.IN, host, port, data, offset, length);
        NetworkService.getInstance().addPacketLog(log);
    }

    @Override
    public int available() throws IOException {
        return wrapped.available();
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        wrapped.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        wrapped.reset();
    }

    @Override
    public boolean markSupported() {
        return wrapped.markSupported();
    }

    @Override
    public long skip(long n) throws IOException {
        return wrapped.skip(n);
    }
}
