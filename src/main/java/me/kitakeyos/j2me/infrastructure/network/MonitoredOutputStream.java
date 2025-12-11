package me.kitakeyos.j2me.infrastructure.network;

import me.kitakeyos.j2me.domain.network.model.PacketLog;
import me.kitakeyos.j2me.domain.network.service.NetworkService;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrapped OutputStream that monitors all write operations for packet capture.
 */
public class MonitoredOutputStream extends OutputStream {

    private final OutputStream wrapped;
    private final int instanceId;
    private final String host;
    private final int port;

    public MonitoredOutputStream(OutputStream wrapped, int instanceId, String host, int port) {
        this.wrapped = wrapped;
        this.instanceId = instanceId;
        this.host = host;
        this.port = port;
    }

    @Override
    public void write(int b) throws IOException {
        wrapped.write(b);
        byte[] data = new byte[] { (byte) b };
        logPacket(data, 0, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        wrapped.write(b);
        logPacket(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        wrapped.write(b, off, len);
        logPacket(b, off, len);
    }

    private void logPacket(byte[] data, int offset, int length) {
        PacketLog log = new PacketLog(instanceId, PacketLog.Direction.OUT, host, port, data, offset, length);
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
