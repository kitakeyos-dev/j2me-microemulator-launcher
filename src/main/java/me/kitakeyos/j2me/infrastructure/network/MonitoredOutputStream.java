package me.kitakeyos.j2me.infrastructure.network;

import me.kitakeyos.j2me.domain.network.model.PacketLog;
import me.kitakeyos.j2me.domain.network.model.SocketTap;
import me.kitakeyos.j2me.domain.network.service.NetworkService;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrapped OutputStream that monitors all write operations.
 * Data is both logged as PacketLog and pushed to the SocketTap for streaming access.
 */
public class MonitoredOutputStream extends OutputStream {

    private final OutputStream wrapped;
    private final int instanceId;
    private final int socketId;
    private final String host;
    private final int port;

    public MonitoredOutputStream(OutputStream wrapped, int instanceId, int socketId, String host, int port) {
        this.wrapped = wrapped;
        this.instanceId = instanceId;
        this.socketId = socketId;
        this.host = host;
        this.port = port;
    }

    @Override
    public void write(int b) throws IOException {
        wrapped.write(b);
        byte[] data = new byte[]{(byte) b};
        onDataSent(data, 0, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        wrapped.write(b);
        onDataSent(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        wrapped.write(b, off, len);
        onDataSent(b, off, len);
    }

    private void onDataSent(byte[] data, int offset, int length) {
        NetworkService ns = NetworkService.getInstance();
        logPacket(data, offset, length);
        SocketTap tap = ns.getOrCreateTap(socketId, instanceId, host, port);
        if (tap != null) {
            tap.getSentStream().push(data, offset, length);
        }
    }

    private void logPacket(byte[] data, int offset, int length) {
        PacketLog log = new PacketLog(instanceId, socketId, PacketLog.Direction.OUT, host, port, data, offset, length);
        NetworkService.getInstance().addPacketLog(log);
    }

    @Override
    public void flush() throws IOException {
        wrapped.flush();
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }
}
