package me.kitakeyos.j2me.domain.network.service;

import me.kitakeyos.j2me.domain.network.model.ConnectionLog;
import me.kitakeyos.j2me.domain.network.model.PacketLog;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Fan-out of network events to registered listeners.
 * <p>
 * Extracted so the rule store and the activity log can publish without either
 * of them owning listener bookkeeping. A throwing listener is logged and the
 * remaining listeners still run.
 */
public class NetworkEvents {

    private static final Logger logger = Logger.getLogger(NetworkEvents.class.getName());

    private final List<NetworkChangeListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(NetworkChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(NetworkChangeListener listener) {
        listeners.remove(listener);
    }

    public void fireRulesChanged() {
        fire(NetworkChangeListener::onRulesChanged);
    }

    public void fireLogAdded(ConnectionLog log) {
        fire(listener -> listener.onLogAdded(log));
    }

    public void fireLogsCleared() {
        fire(NetworkChangeListener::onLogsCleared);
    }

    public void firePacketLogAdded(PacketLog log) {
        fire(listener -> listener.onPacketLogAdded(log));
    }

    public void firePacketLogsCleared() {
        fire(NetworkChangeListener::onPacketLogsCleared);
    }

    private void fire(Consumer<NetworkChangeListener> event) {
        for (NetworkChangeListener listener : listeners) {
            try {
                event.accept(listener);
            } catch (Exception e) {
                logger.warning("Network listener failed: " + e);
            }
        }
    }
}
