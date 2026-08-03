package me.kitakeyos.j2me.domain.network.repository;

import me.kitakeyos.j2me.domain.network.model.SocketRoute;

import java.io.IOException;
import java.net.Socket;

/**
 * Opens the actual TCP connection described by a fully-resolved route.
 * <p>
 * Keeps {@code java.net} socket construction — and the JVM-global authenticator
 * side effect proxy auth requires — out of the domain, and lets the connector
 * be exercised without touching the network.
 */
public interface SocketOpener {

    /**
     * Dial the route's target, through its proxy when one is set.
     *
     * @return a connected socket
     * @throws IOException if the connection cannot be established
     */
    Socket open(SocketRoute route) throws IOException;
}
