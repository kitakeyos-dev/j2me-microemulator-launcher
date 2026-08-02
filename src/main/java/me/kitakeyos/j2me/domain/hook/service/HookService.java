package me.kitakeyos.j2me.domain.hook.service;

import me.kitakeyos.j2me.domain.hook.model.MethodInterceptor;
import me.kitakeyos.j2me.domain.hook.model.MethodSignature;
import me.kitakeyos.j2me.domain.hook.repository.HookLookup;
import me.kitakeyos.j2me.domain.hook.repository.HookRegistrar;

/**
 * Task-shaped facade over {@link HookRegistrar} for callers that install hooks
 * (UI, injection scripts).
 *
 * <p>Depends on the port, never on the storage implementation — wire it in
 * {@code MainApplication} with whichever registry the app uses.
 */
public class HookService {

    private final HookRegistrar registrar;

    public HookService(HookRegistrar registrar) {
        if (registrar == null) {
            throw new IllegalArgumentException("registrar must not be null");
        }
        this.registrar = registrar;
    }

    /**
     * Hook one specific overload of one instance's method.
     *
     * @param instanceId  target instance id
     * @param className   target class, dotted or slashed
     * @param methodName  target method name
     * @param descriptor  JVM descriptor, e.g. {@code (I)V}
     * @param interceptor behaviour to run in place of the original
     */
    public void hook(int instanceId, String className, String methodName, String descriptor,
            MethodInterceptor interceptor) {
        registrar.register(instanceId, MethodSignature.of(className, methodName, descriptor), interceptor);
    }

    /**
     * Hook every overload of a method name.
     */
    public void hookAnyOverload(int instanceId, String className, String methodName,
            MethodInterceptor interceptor) {
        registrar.register(instanceId, MethodSignature.ofAnyOverload(className, methodName), interceptor);
    }

    /**
     * Hook a method across every instance, present and future.
     */
    public void hookAllInstances(String className, String methodName, String descriptor,
            MethodInterceptor interceptor) {
        hook(HookLookup.ALL_INSTANCES, className, methodName, descriptor, interceptor);
    }

    public boolean unhook(int instanceId, String className, String methodName, String descriptor) {
        return registrar.unregister(instanceId, MethodSignature.of(className, methodName, descriptor));
    }

    /**
     * Drop every hook belonging to one instance.
     */
    public void clear(int instanceId) {
        registrar.clear(instanceId);
    }
}
