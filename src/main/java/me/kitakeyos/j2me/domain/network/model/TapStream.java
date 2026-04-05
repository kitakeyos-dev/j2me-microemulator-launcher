package me.kitakeyos.j2me.domain.network.model;

import java.io.IOException;
import java.io.InputStream;

/**
 * An expandable-buffer backed InputStream that captures a copy of socket data.
 * Writers (MonitoredInputStream/OutputStream) push data in; readers (injection code) consume it.
 *
 * Key properties:
 * - Thread-safe: writer and reader can be on different threads
 * - Non-blocking write: buffer grows as needed (no data loss)
 * - Blocking read: reader blocks when no data is available
 * - Supports peek (non-destructive) and drain (consume all)
 */
public class TapStream extends InputStream {

    private byte[] buffer;
    private int writePos = 0;
    private int readPos = 0;
    private volatile boolean closed = false;

    private static final int INITIAL_CAPACITY = 4096;
    private static final int MAX_CAPACITY = 16 * 1024 * 1024; // 16 MB safety limit

    public TapStream() {
        this.buffer = new byte[INITIAL_CAPACITY];
    }

    /**
     * Write data into the tap buffer. Called by MonitoredInputStream/OutputStream.
     * Buffer grows automatically if needed.
     */
    public synchronized void push(byte[] data, int offset, int length) {
        if (closed) return;

        ensureCapacity(length);
        System.arraycopy(data, offset, buffer, writePos, length);
        writePos += length;
        notifyAll();
    }

    private void ensureCapacity(int additional) {
        int required = writePos + additional;
        if (required <= buffer.length) return;

        // Compact first: shift unread data to beginning
        if (readPos > 0) {
            int unread = writePos - readPos;
            System.arraycopy(buffer, readPos, buffer, 0, unread);
            writePos = unread;
            readPos = 0;
        }

        // Grow if still not enough
        required = writePos + additional;
        if (required > buffer.length) {
            int newCapacity = Math.min(Math.max(buffer.length * 2, required), MAX_CAPACITY);
            if (newCapacity < required) {
                // At max capacity - drop oldest data to make room
                int toDrop = required - newCapacity + writePos;
                if (toDrop > 0 && toDrop < writePos) {
                    int remaining = writePos - toDrop;
                    System.arraycopy(buffer, toDrop, buffer, 0, remaining);
                    writePos = remaining;
                }
                return;
            }
            byte[] newBuffer = new byte[newCapacity];
            System.arraycopy(buffer, 0, newBuffer, 0, writePos);
            buffer = newBuffer;
        }
    }

    @Override
    public synchronized int read() throws IOException {
        while (readPos >= writePos) {
            if (closed) return -1;
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted", e);
            }
        }
        return buffer[readPos++] & 0xFF;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        while (readPos >= writePos) {
            if (closed) return -1;
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted", e);
            }
        }

        int available = writePos - readPos;
        int toRead = Math.min(len, available);
        System.arraycopy(buffer, readPos, b, off, toRead);
        readPos += toRead;
        compactIfNeeded();
        return toRead;
    }

    @Override
    public synchronized int available() {
        return writePos - readPos;
    }

    /**
     * Read all currently available data without blocking.
     * Returns empty array if no data available.
     */
    public synchronized byte[] drain() {
        int available = writePos - readPos;
        if (available == 0) return new byte[0];
        byte[] data = new byte[available];
        System.arraycopy(buffer, readPos, data, 0, available);
        readPos = 0;
        writePos = 0;
        return data;
    }

    /**
     * Peek at all available data without consuming it.
     */
    public synchronized byte[] peek() {
        int available = writePos - readPos;
        if (available == 0) return new byte[0];
        byte[] data = new byte[available];
        System.arraycopy(buffer, readPos, data, 0, available);
        return data;
    }

    /**
     * Compact buffer when read position is past halfway to avoid unbounded growth.
     */
    private void compactIfNeeded() {
        if (readPos > buffer.length / 2) {
            int unread = writePos - readPos;
            System.arraycopy(buffer, readPos, buffer, 0, unread);
            writePos = unread;
            readPos = 0;
        }
    }

    /**
     * Get the current buffer usage in bytes.
     */
    public synchronized int getBufferSize() {
        return writePos - readPos;
    }

    @Override
    public synchronized void close() {
        closed = true;
        notifyAll();
    }

    public boolean isClosed() {
        return closed;
    }
}
