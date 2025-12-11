package me.kitakeyos.j2me.infrastructure.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

/**
 * Monitored socket that wraps a real socket and provides monitored I/O streams.
 * All data read/written is logged for packet capture.
 */
public class MonitoredSocket extends Socket {

    private final Socket wrapped;
    private final int instanceId;
    private final String host;
    private final int port;

    private MonitoredInputStream monitoredInputStream;
    private MonitoredOutputStream monitoredOutputStream;

    public MonitoredSocket(Socket wrapped, int instanceId, String host, int port) {
        this.wrapped = wrapped;
        this.instanceId = instanceId;
        this.host = host;
        this.port = port;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (monitoredInputStream == null) {
            monitoredInputStream = new MonitoredInputStream(wrapped.getInputStream(), instanceId, host, port);
        }
        return monitoredInputStream;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (monitoredOutputStream == null) {
            monitoredOutputStream = new MonitoredOutputStream(wrapped.getOutputStream(), instanceId, host, port);
        }
        return monitoredOutputStream;
    }

    // Delegate all other methods to wrapped socket

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        wrapped.connect(endpoint);
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        wrapped.connect(endpoint, timeout);
    }

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        wrapped.bind(bindpoint);
    }

    @Override
    public InetAddress getInetAddress() {
        return wrapped.getInetAddress();
    }

    @Override
    public InetAddress getLocalAddress() {
        return wrapped.getLocalAddress();
    }

    @Override
    public int getPort() {
        return wrapped.getPort();
    }

    @Override
    public int getLocalPort() {
        return wrapped.getLocalPort();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return wrapped.getRemoteSocketAddress();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return wrapped.getLocalSocketAddress();
    }

    @Override
    public SocketChannel getChannel() {
        return wrapped.getChannel();
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        wrapped.setTcpNoDelay(on);
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return wrapped.getTcpNoDelay();
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
        wrapped.setSoLinger(on, linger);
    }

    @Override
    public int getSoLinger() throws SocketException {
        return wrapped.getSoLinger();
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
        wrapped.sendUrgentData(data);
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
        wrapped.setOOBInline(on);
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return wrapped.getOOBInline();
    }

    @Override
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        wrapped.setSoTimeout(timeout);
    }

    @Override
    public synchronized int getSoTimeout() throws SocketException {
        return wrapped.getSoTimeout();
    }

    @Override
    public synchronized void setSendBufferSize(int size) throws SocketException {
        wrapped.setSendBufferSize(size);
    }

    @Override
    public synchronized int getSendBufferSize() throws SocketException {
        return wrapped.getSendBufferSize();
    }

    @Override
    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        wrapped.setReceiveBufferSize(size);
    }

    @Override
    public synchronized int getReceiveBufferSize() throws SocketException {
        return wrapped.getReceiveBufferSize();
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
        wrapped.setKeepAlive(on);
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return wrapped.getKeepAlive();
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
        wrapped.setTrafficClass(tc);
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return wrapped.getTrafficClass();
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        wrapped.setReuseAddress(on);
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return wrapped.getReuseAddress();
    }

    @Override
    public synchronized void close() throws IOException {
        wrapped.close();
    }

    @Override
    public void shutdownInput() throws IOException {
        wrapped.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        wrapped.shutdownOutput();
    }

    @Override
    public String toString() {
        return "MonitoredSocket[" + wrapped.toString() + "]";
    }

    @Override
    public boolean isConnected() {
        return wrapped.isConnected();
    }

    @Override
    public boolean isBound() {
        return wrapped.isBound();
    }

    @Override
    public boolean isClosed() {
        return wrapped.isClosed();
    }

    @Override
    public boolean isInputShutdown() {
        return wrapped.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return wrapped.isOutputShutdown();
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        wrapped.setPerformancePreferences(connectionTime, latency, bandwidth);
    }
}
