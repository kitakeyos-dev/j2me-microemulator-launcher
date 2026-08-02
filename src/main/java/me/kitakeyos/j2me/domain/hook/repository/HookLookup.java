package me.kitakeyos.j2me.domain.hook.repository;

import me.kitakeyos.j2me.domain.hook.model.MethodInterceptor;
import me.kitakeyos.j2me.domain.hook.model.MethodSignature;

/**
 * Read side of the hook store.
 *
 * <p>Kept separate from {@link HookRegistrar} so the bytecode pipeline — which
 * must never mutate hooks — depends on queries only.
 */
public interface HookLookup {

    /** Registration scope matching every emulator instance. */
    int ALL_INSTANCES = -1;

    /**
     * Fast bail-out for the class visitor: does this instance have any hook at
     * all? Answering {@code false} lets the transform skip a whole class.
     */
    boolean hasAnyHooks(int instanceId);

    /**
     * Does this instance hook anything declared by {@code ownerInternalName}?
     */
    boolean hasHooksForClass(int instanceId, String ownerInternalName);

    /**
     * Interceptor for a concrete call, or {@code null} if none.
     *
     * <p>An exact-descriptor registration wins over a wildcard one; a hook
     * registered for {@link #ALL_INSTANCES} is the last fallback.
     */
    MethodInterceptor find(int instanceId, MethodSignature signature);
}
