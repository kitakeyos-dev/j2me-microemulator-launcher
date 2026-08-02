package me.kitakeyos.j2me.domain.hook.repository;

/**
 * Convenience union of the two hook ports for implementations that back both.
 *
 * <p>Collaborators should depend on {@link HookLookup} or {@link HookRegistrar},
 * whichever they actually need — not on this.
 */
public interface HookRegistry extends HookLookup, HookRegistrar {
}
