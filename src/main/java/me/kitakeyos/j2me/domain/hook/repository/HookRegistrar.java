package me.kitakeyos.j2me.domain.hook.repository;

import me.kitakeyos.j2me.domain.hook.model.MethodInterceptor;
import me.kitakeyos.j2me.domain.hook.model.MethodSignature;

/**
 * Write side of the hook store, used by whoever installs hooks (UI, injection
 * JAR scanner, tests).
 *
 * <p>Registrations only affect classes loaded <em>after</em> them: the rewrite
 * happens once, at class-load time. Hooking a class an instance has already
 * loaded requires restarting that instance.
 */
public interface HookRegistrar {

    /**
     * @param instanceId  target instance, or {@link HookLookup#ALL_INSTANCES}
     * @param signature   method to intercept; a wildcard descriptor covers all overloads
     * @param interceptor behaviour to run in place of the original method
     */
    void register(int instanceId, MethodSignature signature, MethodInterceptor interceptor);

    /**
     * @return {@code true} if a registration was removed
     */
    boolean unregister(int instanceId, MethodSignature signature);

    /**
     * Drop every hook registered for one instance.
     */
    void clear(int instanceId);
}
