package me.kitakeyos.j2me.domain.network.service;

import me.kitakeyos.j2me.domain.network.model.ConnectionLog;
import me.kitakeyos.j2me.domain.network.model.PacketLog;

/**
 * Listener interface for network state changes.
 * <p>
 * Every method is a default no-op so a listener only overrides the events it
 * actually cares about.
 */
public interface NetworkChangeListener {

    default void onRulesChanged() {
    }

    default void onLogAdded(ConnectionLog log) {
    }

    default void onLogsCleared() {
    }

    default void onPacketLogAdded(PacketLog log) {
    }

    default void onPacketLogsCleared() {
    }
}
