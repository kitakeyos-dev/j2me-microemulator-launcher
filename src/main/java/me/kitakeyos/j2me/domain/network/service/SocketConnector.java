package me.kitakeyos.j2me.domain.network.service;

import me.kitakeyos.j2me.domain.network.model.ConnectionLog;
import me.kitakeyos.j2me.domain.network.model.SocketRoute;
import me.kitakeyos.j2me.domain.network.model.SocketRule;
import me.kitakeyos.j2me.domain.network.repository.SocketOpener;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves an outbound connection through the rule pipeline, opens it, and
 * records the attempt.
 * <p>
 * The pipeline is derived from the rules themselves — groups are applied in
 * ascending order and the first match inside a group wins — so this class has
 * no knowledge of redirection or proxying specifically and does not change
 * when a rule type is added.
 */
public class SocketConnector {

    private final NetworkRuleStore ruleStore;
    private final NetworkActivityLog activityLog;
    private final SocketOpener opener;

    public SocketConnector(NetworkRuleStore ruleStore, NetworkActivityLog activityLog, SocketOpener opener) {
        this.ruleStore = ruleStore;
        this.activityLog = activityLog;
        this.opener = opener;
    }

    /**
     * Open a connection for a MIDlet, applying every configured rule.
     * <p>
     * The attempt is logged whether it succeeds or fails.
     *
     * @throws IOException if the connection cannot be established
     */
    public Socket connect(int instanceId, String host, int port) throws IOException {
        SocketRoute route = resolve(instanceId, host, port);

        boolean success = false;
        String errorMessage = null;
        try {
            Socket socket = opener.open(route);
            success = true;
            return socket;
        } catch (IOException e) {
            errorMessage = e.getMessage();
            throw e;
        } finally {
            activityLog.addConnectionLog(new ConnectionLog(
                    instanceId, host, port,
                    route.getTargetHost(), route.getTargetPort(),
                    route.getProxyDescription(), success, errorMessage));
        }
    }

    /**
     * Run the rule pipeline over a fresh route without opening anything.
     * Exposed for tests and for previewing what a connection would do.
     */
    public SocketRoute resolve(int instanceId, String host, int port) {
        SocketRoute route = new SocketRoute(instanceId, host, port);

        List<SocketRule> ordered = new ArrayList<>(ruleStore.getRules());
        ordered.sort(Comparator.comparingInt(SocketRule::order));

        Set<String> settledGroups = new HashSet<>();
        for (SocketRule rule : ordered) {
            if (settledGroups.contains(rule.group())) {
                continue;
            }
            if (rule.matches(route)) {
                rule.apply(route);
                settledGroups.add(rule.group());
            }
        }
        return route;
    }
}
