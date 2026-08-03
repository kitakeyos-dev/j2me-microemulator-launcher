package me.kitakeyos.j2me.infrastructure.network;

import me.kitakeyos.j2me.domain.network.model.SocketRoute;
import me.kitakeyos.j2me.domain.network.repository.SocketOpener;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Socket;

/**
 * Opens real TCP connections with {@code java.net}.
 * <p>
 * Proxy credentials go through {@link Authenticator#setDefault}, which is
 * process-global; confining that side effect here keeps it out of the domain,
 * where it would have been impossible to test around.
 */
public class PlainSocketOpener implements SocketOpener {

    @Override
    public Socket open(SocketRoute route) throws IOException {
        if (!route.hasProxy()) {
            return new Socket(route.getTargetHost(), route.getTargetPort());
        }

        if (route.hasProxyCredentials()) {
            installCredentials(route.getProxyUsername(), route.getProxyPassword());
        }

        Socket socket = new Socket(route.getProxy());
        socket.connect(new InetSocketAddress(route.getTargetHost(), route.getTargetPort()));
        return socket;
    }

    private void installCredentials(String username, String password) {
        final char[] secret = password != null ? password.toCharArray() : new char[0];
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, secret);
            }
        });
    }
}
