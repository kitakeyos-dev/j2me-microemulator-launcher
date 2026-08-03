package me.kitakeyos.j2me.domain.emulator.input;

import me.kitakeyos.j2me.domain.emulator.repository.InstanceLookup;

/**
 * Builds an {@link InputSynchronizer} bound to a given instance list.
 * <p>
 * The synchronizer can only be created once its instance manager exists, which
 * happens while the instances tab is laying itself out. Injecting this factory
 * instead of the finished object lets the panel defer construction without
 * naming the infrastructure implementation.
 */
public interface InputSynchronizerFactory {

    /**
     * @param instances the live instance list the synchronizer should read
     * @return a new synchronizer, initially disabled
     */
    InputSynchronizer create(InstanceLookup instances);
}
