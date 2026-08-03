package me.kitakeyos.j2me.domain.emulator.repository;

/**
 * Full read/write access to the live instance list. Implemented by
 * {@code InstanceManager}; consumers should depend on the narrowest of
 * {@link InstanceLookup} / {@link InstanceRegistrar} that covers their needs.
 */
public interface InstanceRegistry extends InstanceLookup, InstanceRegistrar {
}
